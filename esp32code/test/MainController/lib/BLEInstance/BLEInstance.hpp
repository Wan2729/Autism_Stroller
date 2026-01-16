#pragma once
#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

class BLEInstance {
private:
  BLEServer* pServer;
  BLECharacteristic* pCharacteristic;
  BLECharacteristic* pSensorCharacteristic;
  BLEAdvertising* pAdvertising;
  BLEService* pService;
  std::string data;

public:
  BLEInstance();
  std::string getData();
  void setData(std::string newData);
  void restartAdvertising();
  void sendSensorData(String payload);
  bool initialize(String name, String serviceUUID, String writeUUID, String notifyUUID);
  bool start();
};