#include "AudioTools.h"
#include "BluetoothA2DPSink.h"
#include "BluetoothSerial.h"

I2SStream i2s;
BluetoothA2DPSink a2dp_sink(i2s);
BluetoothSerial SerialBT;

const int LED_PIN = 2;

void setup() {
  pinMode(LED_PIN, OUTPUT);
  Serial.begin(115200);

  // Start A2DP Sink for music
  auto cfg = i2s.defaultConfig();
  cfg.pin_bck = 27;
  cfg.pin_ws = 26;
  cfg.pin_data = 25;
  i2s.begin(cfg);
  a2dp_sink.start("ESP32_Speaker");

  // Start Serial Bluetooth for control
  SerialBT.begin("ESP32_Serial_Comm");
}

void loop() {
  if (SerialBT.available()) {
    char cmd = SerialBT.read();
    Serial.println(cmd);
  }
}