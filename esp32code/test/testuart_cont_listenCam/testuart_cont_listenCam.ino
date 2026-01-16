#include <Arduino.h>

// Define the pins you are using for the Camera
#define CAM_RX_PIN 32
#define CAM_TX_PIN 33

void setup() {
  // 1. Start USB Serial (To talk to PC)
  Serial.begin(115200);
  Serial.println("--- Controller Listening Mode ---");
  Serial.println("Waiting for Camera...");

  // 2. Start Camera Serial (To talk to Camera)
  // We MUST match the Camera's speed (115200)
  Serial1.begin(115200, SERIAL_8N1, CAM_RX_PIN, CAM_TX_PIN);
}

void loop() {
  // If data comes from Camera (Serial1) -> Send to PC (Serial)
  if (Serial1.available()) {
    String msg = Serial1.readStringUntil('\n');
    Serial.print("RECEIVED: ");
    Serial.println(msg);
  }
}