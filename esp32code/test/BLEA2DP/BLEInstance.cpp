#include "BLEInstance.hpp"

// Callback class
class MyCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) override {
    String value = pCharacteristic->getValue();
    if (value.length() > 0) {
      Serial.print("Received BLE command: ");
      Serial.println(value);
    }
  }
};

// Constructor
BLEInstance::BLEInstance() {
  pServer = nullptr;
  pCharacteristic = nullptr;
  pAdvertising = nullptr;
  pService = nullptr;
}

// Initialize BLE
bool BLEInstance::initialize(String name, String serviceUUID, String charUUID) {
  BLEDevice::init(name);
  pServer = BLEDevice::createServer();
  pService = pServer->createService(serviceUUID);

  pCharacteristic = pService->createCharacteristic(
    charUUID,
    BLECharacteristic::PROPERTY_WRITE
  );

  pCharacteristic->setCallbacks(new MyCallbacks());
  return true;
}

// Start BLE advertising
bool BLEInstance::start() {
  pService->start();
  pAdvertising = BLEDevice::getAdvertising();
  if (pAdvertising == nullptr) {
    return false;
  }
  pAdvertising->start();
  Serial.println("BLE advertising started");
  return true;
}