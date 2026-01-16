#include "BLEInstance.hpp"
#include "BluetoothA2DPInstance.hpp"

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define WS_PIN 26
#define BCK_PIN 27
#define DATA_PIN 25
#define LED_PIN 2

BLEInstance BLE;
BluetoothA2DPInstance BluetoothA2DP;

void setup() {
  pinMode(LED_PIN, OUTPUT);
  Serial.begin(115200);

  // BLE Setup and Start
  BLE.initialize("ESP32_Control", SERVICE_UUID, CHARACTERISTIC_UUID);
  BLE.start();

  //a2dp Setup and Start
  BluetoothA2DP.initialize("ESP32_Speaker", BCK_PIN, WS_PIN, DATA_PIN);
  BluetoothA2DP.start();
}

void loop() {
  // BLE callbacks handle everything
  
}