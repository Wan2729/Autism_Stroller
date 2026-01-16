#include <Arduino.h>
#include "esp_camera.h"
#include <WiFi.h>
#include "esp_timer.h"
#include "img_converters.h"
#include "arduino.h"
#include "fb_gfx.h"
#include "soc/soc.h"           
#include "soc/rtc_cntl_reg.h"  
#include "esp_http_server.h"

// --- AI THINKER PIN DEFINITIONS ---
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27
#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22
#define LED_GPIO_NUM       4 // Large Flash LED

httpd_handle_t stream_httpd = NULL;

// --- STREAM HANDLER ---
static esp_err_t stream_handler(httpd_req_t *req) {
  camera_fb_t * fb = NULL;
  esp_err_t res = ESP_OK;
  size_t _jpg_buf_len = 0;
  uint8_t * _jpg_buf = NULL;
  char * part_buf[64];

  res = httpd_resp_set_type(req, "multipart/x-mixed-replace;boundary=frame");
  if(res != ESP_OK) return res;

  while(true){
    fb = esp_camera_fb_get();
    if (!fb) {
       // Frame failed, retry silently
    } else {
      if(fb->format != PIXFORMAT_JPEG){
        bool jpeg_converted = frame2jpg(fb, 80, &_jpg_buf, &_jpg_buf_len);
        esp_camera_fb_return(fb);
        fb = NULL;
        if(!jpeg_converted) res = ESP_FAIL;
      } else {
        _jpg_buf_len = fb->len;
        _jpg_buf = fb->buf;
      }
    }
    if(res == ESP_OK){
      size_t hlen = snprintf((char *)part_buf, 64, "Content-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n", _jpg_buf_len);
      res = httpd_resp_send_chunk(req, (const char *)part_buf, hlen);
    }
    if(res == ESP_OK) res = httpd_resp_send_chunk(req, (const char *)_jpg_buf, _jpg_buf_len);
    if(res == ESP_OK) res = httpd_resp_send_chunk(req, (const char *)"\r\n--frame\r\n", 12);
    
    if(fb){
      esp_camera_fb_return(fb);
      fb = NULL;
      _jpg_buf = NULL;
    } else if(_jpg_buf){
      free(_jpg_buf);
      _jpg_buf = NULL;
    }
    if(res != ESP_OK) break;
  }
  return res;
}

void startCameraServer(){
  httpd_config_t config = HTTPD_DEFAULT_CONFIG();
  config.server_port = 80;
  httpd_uri_t stream_uri = {
    .uri       = "/", 
    .method    = HTTP_GET,
    .handler   = stream_handler, 
    .user_ctx  = NULL
  };
  if (httpd_start(&stream_httpd, &config) == ESP_OK) {
    httpd_register_uri_handler(stream_httpd, &stream_uri);
  }
}

void setup() {
  WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0); // Disable brownout

  // 1. Setup Communication
  Serial.begin(115200); // Matches Controller
  
  // 2. Setup Flash LED
  pinMode(LED_GPIO_NUM, OUTPUT);
  digitalWrite(LED_GPIO_NUM, LOW); 

  // 3. Camera Configuration
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size = FRAMESIZE_QVGA;
  config.jpeg_quality = 12;
  config.fb_count = 2;

  if (esp_camera_init(&config) != ESP_OK) {
    // Error Blink: Fast and forever
    while(true) { digitalWrite(LED_GPIO_NUM, HIGH); delay(100); digitalWrite(LED_GPIO_NUM, LOW); delay(100); }
  }
}

void loop() {
  if (Serial.available()) {
    // DEBUG BLINK: Short blink = "I heard you"
    digitalWrite(LED_GPIO_NUM, HIGH);
    delay(10); 
    digitalWrite(LED_GPIO_NUM, LOW);

    String data = Serial.readStringUntil('\n');
    data.trim();

    if (data.startsWith("WIFI:")) {
      String credentials = data.substring(5);
      int commaIndex = credentials.indexOf(',');
      
      if (commaIndex > 0) {
        String ssid = credentials.substring(0, commaIndex);
        String pass = credentials.substring(commaIndex + 1);

        WiFi.begin(ssid.c_str(), pass.c_str());
        
        int retries = 0;
        while (WiFi.status() != WL_CONNECTED && retries < 20) {
          delay(500);
          retries++;
        }

        if (WiFi.status() == WL_CONNECTED) {
          startCameraServer();
          Serial.println("IP:" + WiFi.localIP().toString());
          // SUCCESS BLINK: Long blink = "Connected!"
          digitalWrite(LED_GPIO_NUM, HIGH); delay(500); digitalWrite(LED_GPIO_NUM, LOW);
        } else {
           Serial.println("ERR:ConnectFail");
        }
      }
    }
  }
}