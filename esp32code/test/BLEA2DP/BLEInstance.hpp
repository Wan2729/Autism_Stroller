#pragma once
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

class BLEInstance {
private:
  BLEServer* pServer;
  BLECharacteristic* pCharacteristic;
  BLEAdvertising* pAdvertising;
  BLEService* pService;

public:
  BLEInstance();
  bool initialize(String name, String serviceUUID, String charUUID);
  bool start();
};