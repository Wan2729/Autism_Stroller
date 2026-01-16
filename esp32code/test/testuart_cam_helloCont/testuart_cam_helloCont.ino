#include "soc/soc.h"
#include "soc/rtc_cntl_reg.h"

void setup() {
  WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0); // Keep power stable
  
  // Start Serial at 115200
  // On ESP32-CAM, this uses Pin 1 (TX) and Pin 3 (RX) automatically
  Serial.begin(115200); 
}

void loop() {
  Serial.println("Hello from Camera!");
  delay(5000);
}