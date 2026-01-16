// #include <Arduino.h>
// #include <lv_conf.h>
// #include <lvgl.h>

// #include "gui/gui.h"

// void setup() {
//   gui_start();
// }

// void loop() {
//   lv_timer_handler();

//   delay(5);
// }
#include <LovyanGFX.hpp>
#include <TJpg_Decoder.h>
#include <SPIFFS.h>
#include "gfx/LGFX_ESP32S3_RGB_MakerfabsParallelTFTwithTouch70.h"

static LGFX lcd;
#define VIDEO_W 720
#define VIDEO_H 405
File mjpegFile;
static uint8_t* jpgBuf = nullptr;
static const size_t JPG_BUF_SIZE = 150 * 1024;

bool jpgDrawCallback(int16_t x, int16_t y,
                     uint16_t w, uint16_t h,
                     uint16_t* bitmap)
{
    static int drawX = (lcd.width()  - VIDEO_W) / 2;
    static int drawY = (lcd.height() - VIDEO_H) / 2;

    lcd.startWrite();
    lcd.pushImage(drawX + x, drawY + y, w, h, bitmap);
    lcd.endWrite();
    return true;
}

bool readNextJpeg() {
    if (!mjpegFile.available()) return false;

    // Find JPEG start
    while (mjpegFile.available()) {
        if (mjpegFile.read() == 0xFF && mjpegFile.peek() == 0xD8) {
            mjpegFile.seek(mjpegFile.position() - 1);
            break;
        }
    }

    int len = 0;

    while (mjpegFile.available() && len < JPG_BUF_SIZE) {
        jpgBuf[len++] = mjpegFile.read();
        if (len > 2 && jpgBuf[len - 2] == 0xFF && jpgBuf[len - 1] == 0xD9)
            break;
    }

    TJpgDec.drawJpg(0, 0, jpgBuf, len);
    return true;
}

void setup() {
    Serial.begin(115200);

    jpgBuf = (uint8_t*)heap_caps_malloc(JPG_BUF_SIZE, MALLOC_CAP_SPIRAM);
    if (!jpgBuf) {
        Serial.println("❌ PSRAM alloc failed");
        while (1);
    }

    lcd.init();
    lcd.setRotation(2);
    lcd.fillScreen(TFT_BLACK);

    SPIFFS.begin(true);

    TJpgDec.setJpgScale(1);
    TJpgDec.setSwapBytes(false);
    TJpgDec.setCallback(jpgDrawCallback);

    mjpegFile = SPIFFS.open("/loop.mjpeg", "r");
    if (!mjpegFile) {
        Serial.println("❌ Failed to open MJPEG file");
      } else {
        Serial.println("✅ MJPEG file opened");
        Serial.printf("File size: %d bytes\n", mjpegFile.size());
        mjpegFile.seek(2048);
    }
}

void loop() {
    if (!readNextJpeg()) {
        mjpegFile.seek(2048); // loop video
    }
}