#pragma once
#include "AudioTools.h"
#include "BluetoothA2DPSink.h"

class BluetoothA2DPInstance{
private:
  I2SStream i2s;
  BluetoothA2DPSink a2dp_sink;
  String name;
public:
  BluetoothA2DPInstance();
  void initialize(String name, int pinBck, int pinWs, int pinData);
  void start();
};