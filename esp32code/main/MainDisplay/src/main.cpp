#include <Arduino.h>
#include <SPI.h>
#include <SD.h>
#include <JPEGDEC.h> 

#define LGFX_USE_V1
#include <LovyanGFX.hpp>
#include <lgfx/v1/platforms/esp32s3/Panel_RGB.hpp>
#include <lgfx/v1/platforms/esp32s3/Bus_RGB.hpp>

// ───────── USER CONFIG ─────────
#define SD_CS       10
#define FPS         20            // MUST match the FPS you used when creating the video!
#define VIDEO_W     480           // Width of your video file
#define VIDEO_H     272           // Height of your video file

#define MJPEG_BUF_SIZE (60 * 1024)

// ───────── DISPLAY SETUP ─────────
class LGFX : public lgfx::LGFX_Device {
public:
  lgfx::Bus_RGB     _bus;
  lgfx::Panel_RGB   _panel;
  lgfx::Light_PWM   _light;

  LGFX() {
    auto cfg = _panel.config();
    cfg.memory_width  = 800; cfg.memory_height = 480;
    cfg.panel_width   = 800; cfg.panel_height  = 480;
    cfg.offset_x      = 0;   cfg.offset_y      = 0;
    _panel.config(cfg);

    auto cfg_detail = _panel.config_detail();
    cfg_detail.use_psram = true;
    _panel.config_detail(cfg_detail);

    auto bus_cfg = _bus.config();
    bus_cfg.panel = &_panel;
    
    // Sunton 7-inch Pins
    bus_cfg.pin_d0 = 15; bus_cfg.pin_d1 = 7;  bus_cfg.pin_d2 = 6;  bus_cfg.pin_d3 = 5;
    bus_cfg.pin_d4 = 4;  bus_cfg.pin_d5 = 9;  bus_cfg.pin_d6 = 46; bus_cfg.pin_d7 = 3;
    bus_cfg.pin_d8 = 8;  bus_cfg.pin_d9 = 16; bus_cfg.pin_d10 = 1; bus_cfg.pin_d11 = 14;
    bus_cfg.pin_d12 = 21; bus_cfg.pin_d13 = 47; bus_cfg.pin_d14 = 48; bus_cfg.pin_d15 = 45;
    bus_cfg.pin_henable = 41; bus_cfg.pin_vsync = 40; bus_cfg.pin_hsync = 39; bus_cfg.pin_pclk = 42;
    bus_cfg.freq_write = 14000000; 

    bus_cfg.hsync_polarity = 0; bus_cfg.hsync_front_porch = 8; bus_cfg.hsync_pulse_width = 2; bus_cfg.hsync_back_porch = 43;
    bus_cfg.vsync_polarity = 0; bus_cfg.vsync_front_porch = 8; bus_cfg.vsync_pulse_width = 2; bus_cfg.vsync_back_porch = 12;
    bus_cfg.pclk_idle_high = 1;
    _bus.config(bus_cfg);
    _panel.setBus(&_bus);

    auto light_cfg = _light.config();
    light_cfg.pin_bl = 2;
    _light.config(light_cfg);
    _panel.light(&_light);
    setPanel(&_panel);
  }
};

static LGFX lcd;
JPEGDEC jpeg;
File mjpegFile;
uint8_t *mjpeg_buf;

// Calculate centering offsets
int offX = 0;
int offY = 0;

// ───────── DRAW CALLBACK (CENTERED) ─────────
int JPEGDraw(JPEGDRAW *pDraw) {
  // Apply the offset to center the image
  lcd.pushImage(pDraw->x + offX, pDraw->y + offY, pDraw->iWidth, pDraw->iHeight, (uint16_t *)pDraw->pPixels);
  return 1;
}

// ───────── HELPER FUNCTIONS ─────────

void stopPlayback() {
  if (mjpegFile) {
    mjpegFile.close();
  }
  lcd.fillScreen(TFT_BLACK);
  Serial.println("Playback Stopped");
}

void playVideoFile(int videoNum) {
  // Close existing file if open
  if (mjpegFile) mjpegFile.close();

  // Construct filename: /anim1.mjpeg, /anim2.mjpeg, etc.
  String filename = "/anim" + String(videoNum) + ".mjpeg";
  Serial.print("Loading: "); Serial.println(filename);

  mjpegFile = SD.open(filename);
  
  if (!mjpegFile) {
    Serial.println("File not found!");
    lcd.fillScreen(TFT_RED); // Visual error indicator
    delay(500);
    lcd.fillScreen(TFT_BLACK);
  } else {
    lcd.fillScreen(TFT_BLACK); // Clear screen before starting new video
  }
}

// ───────── FAST FRAME FINDER ─────────
bool findJpegStart() {
  if (!mjpegFile) return false; // Safety check

  const int READ_CHUNK = 1024;
  uint8_t tempBuf[READ_CHUNK];
  
  while (mjpegFile.available()) {
    long pos = mjpegFile.position();
    int bytesRead = mjpegFile.read(tempBuf, READ_CHUNK);
    
    for (int i = 0; i < bytesRead - 1; i++) {
      if (tempBuf[i] == 0xFF && tempBuf[i + 1] == 0xD8) {
        mjpegFile.seek(pos + i);
        return true;
      }
    }
    if (bytesRead == READ_CHUNK) mjpegFile.seek(pos + bytesRead - 1);
  }
  return false;
}

void setup() {
  Serial.begin(115200);
  
  // RX = 44, TX = 43
  Serial1.begin(9600, SERIAL_8N1, 44, 43);

  offX = (800 - VIDEO_W) / 8;
  offY = (480 - VIDEO_H) / 4;

  // 2. Init LCD
  lcd.init();
  lcd.setRotation(0);
  lcd.setSwapBytes(true); 
  lcd.fillScreen(TFT_BLACK);

  // 3. Init Memory
  mjpeg_buf = (uint8_t*)heap_caps_malloc(MJPEG_BUF_SIZE, MALLOC_CAP_SPIRAM);
  if (!mjpeg_buf) { Serial.println("Malloc failed"); while(1); }

  // 4. Init SD
  SPI.begin(12, 13, 11, SD_CS); 
  if (!SD.begin(SD_CS, SPI, 80000000)) { 
    Serial.println("SD Fail"); 
    if(!SD.begin(SD_CS, SPI, 40000000)) while(true);
  }

  Serial.println("System Ready. Send V:1, V:2... or STOP");
  
  // Optional: Auto-play anim1 on startup
  playVideoFile(1);
}

void loop() {
  // ─── SERIAL COMMAND HANDLING ───
  if (Serial1.available()) {
    String cmd = Serial1.readStringUntil('\n');
    cmd.trim(); 

    Serial.print("Received: "); Serial.println(cmd);

    if (cmd == "V:-1"){
      stopPlayback();
    }
    else if (cmd.startsWith("V:")) {
      String numStr = cmd.substring(2);
      int videoNum = numStr.toInt();
      playVideoFile(videoNum);
    }
  }
  
  // ─── VIDEO PLAYBACK LOGIC ───
  // Only run if a file is actually open
  if (mjpegFile) {
    unsigned long t = millis();

    if (findJpegStart()) {
      int bytesRead = mjpegFile.read(mjpeg_buf, MJPEG_BUF_SIZE);
      
      // Find End of Image (FF D9)
      int frameSize = bytesRead;
      bool foundEnd = false;
      for (int i = 0; i < bytesRead - 1; i++) {
        if (mjpeg_buf[i] == 0xFF && mjpeg_buf[i+1] == 0xD9) {
          frameSize = i + 2;
          foundEnd = true;
          break;
        }
      }

      if (foundEnd) {
        mjpegFile.seek(mjpegFile.position() - (bytesRead - frameSize));
        
        lcd.startWrite(); 
        if (jpeg.openRAM(mjpeg_buf, frameSize, JPEGDraw)) {
          jpeg.decode(0, 0, 0); 
          jpeg.close();
        }
        lcd.endWrite();
        
      } else {
        // If end not found in buffer, loop back to start (or handle error)
        mjpegFile.seek(0);
      }
    } else {
      // Loop video when it ends
      mjpegFile.seek(0);
    }

    // FPS Cap
    while(millis() - t < (1000/FPS)) { yield(); }
  }
}