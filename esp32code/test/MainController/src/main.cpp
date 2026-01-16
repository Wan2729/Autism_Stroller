#include <Arduino.h>
#include "BLEInstance.hpp"
#include "BluetoothA2DPInstance.hpp"
#include "LEDInstance.hpp"
#include "RemoteSensorInstance.hpp"
#include "CameraInstance.hpp"
#include "Utilities.hpp"

BLEInstance BLE;
BluetoothA2DPInstance BluetoothA2DP;
LEDInstance led(50);
RemoteSensorInstance RemoteSensors;
CameraInstance Camera;

int ledProg;
int hsv[3];
int animation;
char latestEvent;
std::string lastBleMessage = "";

// -- Timing variable
unsigned long lastUpdateListener;

void setup() {
  // -- UUID Constants
  const String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
  const String CHAR_WRITE_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
  const String CHAR_NOTIFY_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
  
  // -- Audio Pins
  const uint8_t WS_PIN = 26;
  const uint8_t BCK_PIN = 27;
  const uint8_t DATA_PIN = 25;

  Serial.begin(115200);
  Serial.println("--- System Started ---");

  // -- Camera Setup (Pins 32/33 @ 115200 baud)
  Camera.initialize(32, 33, 115200); 
  
  // -- Sensor Setup
  RemoteSensors.initialize(23, 22); 

  // -- BLE Setup
  BLE.initialize("Stroller_Control", SERVICE_UUID, CHAR_WRITE_UUID, CHAR_NOTIFY_UUID);
  BLE.start();

  // -- Audio Setup
  BluetoothA2DP.initialize("Stroller_Control", BCK_PIN, WS_PIN, DATA_PIN);
  BluetoothA2DP.start();

  // -- LED Setup
  led.initialize();
  ledProg = 0;

  lastUpdateListener = millis();
}

void loop() {
  // ------------------------------------------------
  // 1. ALWAYS RUN: Sensors & Camera Updates
  // ------------------------------------------------
  RemoteSensors.update();

  if (millis() - lastUpdateListener > 200) {
      lastUpdateListener = millis();
      String packet = "T:" + String(RemoteSensors.getTemp(), 1) + 
                      ",G:" + String(RemoteSensors.getGas()) + 
                      ",D:" + String(RemoteSensors.getDist(), 2);
      BLE.sendSensorData(packet);
  }

  Camera.update();
  if (Camera.hasNewIP()) {
      String ipMsg = Camera.getIP();
      BLE.sendSensorData(ipMsg); 
      Serial.println("SUCCESS: Camera Connected at " + ipMsg);
  }

  // ------------------------------------------------
  // 2. INPUT HANDLER: Only runs on NEW Data
  // (This updates the 'settings', but doesn't play the animation)
  // ------------------------------------------------
  std::string stdBleData = BLE.getData();

  // We keep this check to prevent WiFi SPAM, but we moved the LED playing out!
  if (stdBleData.length() > 0 && stdBleData != lastBleMessage) 
  {
    lastBleMessage = stdBleData;
    String bleData = String(stdBleData.c_str());
    char eventType = bleData.charAt(0);
    
    Serial.println("BLE Cmd: " + bleData);

    switch (eventType){
      case 'W': // WiFi Event
          {
            String rawCreds = bleData.substring(2);
            rawCreds.trim(); 
            int commaIndex = rawCreds.indexOf(',');
            if (commaIndex > 0) {
                String ssid = rawCreds.substring(0, commaIndex);
                String pass = rawCreds.substring(commaIndex + 1);
                ssid.trim(); pass.trim();
                Camera.sendWiFiCredentials(ssid, pass);
                Serial.println("-> Sent WiFi to Camera: " + ssid);
            }
          }
          break;

      case 'L': // LED Event -> JUST UPDATE VARIABLES
          // We parse the numbers, but we don't "play" it here.
          // We just update the global 'animation' and 'hsv' variables.
          splitToNumbers(stdBleData.substr(2), hsv[0], hsv[1], hsv[2], animation);
          
          // Optional: Reset progress when a new command arrives so animation restarts
          ledProg = 0; 
          break;
    }
  }

  // ------------------------------------------------
  // 3. OUTPUT PLAYER: Runs EVERY LOOP
  // (This ensures your animation is smooth and never blocked)
  // ------------------------------------------------
  switch (animation) {
    case 1: led.playColor(ledProg, hsv); break;
    case 2: led.playFade(ledProg, hsv); break;
    case 3: led.playTravel(ledProg, hsv); break;
    case 4: led.playTravelNoFill(ledProg, hsv); break;
    default: led.turnOff(ledProg); break;
  }
}