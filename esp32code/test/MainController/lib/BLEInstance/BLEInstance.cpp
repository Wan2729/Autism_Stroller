#include "BLEInstance.hpp"

// Callback class
class MyCallbacks : public BLECharacteristicCallbacks {
private:
  BLEInstance* BLE;

public:
  MyCallbacks(BLEInstance* ble){
    BLE = ble;
  }
  void onWrite(BLECharacteristic* pCharacteristic) override {
    std::string value = pCharacteristic->getValue();
    if (value.length() > 0) {
      Serial.print("Received BLE command: ");
      BLE->setData(value.c_str());
      Serial.println(BLE->getData().c_str());
    }
  }
};


class MyServerCallbacks : public BLEServerCallbacks {
private:
  BLEInstance* ble;

public:
  MyServerCallbacks(BLEInstance* instance) {
    ble = instance;
  }

  void onConnect(BLEServer* pServer) override {
    Serial.println("BLE client connected");
  }

  void onDisconnect(BLEServer* pServer) override {
    Serial.println("BLE client disconnected");
    ble->restartAdvertising();
  }
};

// Constructor
BLEInstance::BLEInstance() {
  pServer = nullptr;
  pCharacteristic = nullptr;
  pSensorCharacteristic = nullptr;
  pAdvertising = nullptr;
  pService = nullptr;
  data = "";
}

void BLEInstance::setData(std::string newData){
  this->data = newData;
}

std::string BLEInstance::getData(){
  return this->data;
}

void BLEInstance::restartAdvertising() {
  if (pAdvertising == nullptr) {
    pAdvertising = BLEDevice::getAdvertising();
  }

  pAdvertising->start();
  Serial.println("BLE advertising restarted");
}

void BLEInstance::sendSensorData(String payload) {
    if (pServer->getConnectedCount() > 0) {
        pSensorCharacteristic->setValue((uint8_t*)payload.c_str(), payload.length());
        pSensorCharacteristic->notify();
    }
}

// Initialize BLE
bool BLEInstance::initialize(String name, String serviceUUID, String writeUUID, String notifyUUID) {
  BLEDevice::init(name.c_str());

  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks(this));

  pService = pServer->createService(serviceUUID.c_str());

  // 1. Create the WRITE Characteristic (For Phone -> ESP commands)
  pCharacteristic = pService->createCharacteristic(
    writeUUID.c_str(),
    BLECharacteristic::PROPERTY_WRITE
  );
  pCharacteristic->setCallbacks(new MyCallbacks(this));

  // 2. Create the NOTIFY Characteristic (For ESP -> Phone data) <--- NEW
  pSensorCharacteristic = pService->createCharacteristic(
      notifyUUID.c_str(),
      BLECharacteristic::PROPERTY_NOTIFY
  );
  // This Descriptor is required for Android to receive notifications
  pSensorCharacteristic->addDescriptor(new BLE2902()); 

  return true;
}

// Start BLE advertising
bool BLEInstance::start() {
  pService->start();
  pAdvertising = BLEDevice::getAdvertising();
  if (pAdvertising == nullptr) {
    return false;
  }
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  pAdvertising->start();
  Serial.println("BLE advertising started");
  return true;
}