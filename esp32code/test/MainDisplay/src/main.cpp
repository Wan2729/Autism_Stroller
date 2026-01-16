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

// ───────── DISPLAY SETUP (Same as before) ─────────
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
    bus_cfg.freq_write = 14000000; // Try slightly higher if stable

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

// ───────── FAST FRAME FINDER ─────────
bool findJpegStart() {
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

  // 1. Calculate Centering Offsets
  offX = (800 - VIDEO_W) / 8;
  offY = (480 - VIDEO_H) / 4;

  // 2. Init LCD
  lcd.init();
  lcd.setRotation(0);
  lcd.setSwapBytes(true); // Keep this if colors are correct now
  lcd.fillScreen(TFT_BLACK);

  // 3. Init Memory
  mjpeg_buf = (uint8_t*)heap_caps_malloc(MJPEG_BUF_SIZE, MALLOC_CAP_SPIRAM);
  if (!mjpeg_buf) { Serial.println("Malloc failed"); while(1); }

  // 4. Init SD at 80MHz (Max speed for Sunton)
  SPI.begin(12, 13, 11, SD_CS); 
  if (!SD.begin(SD_CS, SPI, 80000000)) { // Increased to 80MHz
    Serial.println("SD Fail"); 
    // Fallback if 80MHz fails
    if(!SD.begin(SD_CS, SPI, 40000000)) while(true);
  }

  mjpegFile = SD.open("/anim2.mjpeg");
}

void loop() {
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
      
      // Optimization: Lock display bus once per frame
      lcd.startWrite(); 
      if (jpeg.openRAM(mjpeg_buf, frameSize, JPEGDraw)) {
        jpeg.decode(0, 0, 0); 
        jpeg.close();
      }
      lcd.endWrite();
      
    } else {
      mjpegFile.seek(0);
    }
  } else {
    mjpegFile.seek(0);
  }

  // Debug: Print how long decoding took
  // Serial.print("Time: "); Serial.println(millis() - t);

  // FPS Cap
  while(millis() - t < (1000/FPS)) { yield(); }
}