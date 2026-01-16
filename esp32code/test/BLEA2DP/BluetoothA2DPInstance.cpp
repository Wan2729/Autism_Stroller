#include "BluetoothA2DPInstance.hpp"

BluetoothA2DPInstance::BluetoothA2DPInstance() : a2dp_sink(this->i2s){
  this->name = "";
}

void BluetoothA2DPInstance::initialize(String name, int pinBck, int pinWs, int pinData){
  this->name = name;
  auto cfg = i2s.defaultConfig();
  cfg.pin_bck = pinBck;
  cfg.pin_ws = pinWs;
  cfg.pin_data = pinData;
  i2s.begin(cfg);
}

void BluetoothA2DPInstance::start(){
  a2dp_sink.start(this->name.c_str());
  Serial.println("A2DP advertising started");
}