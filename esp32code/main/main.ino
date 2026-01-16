#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define __SECTION__(x) /* do nothing */
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

/*
+++++++++++++++++++++++++++++++
|                             |
| Pin Declaration             |
|                             |
+++++++++++++++++++++++++++++++
*/
const int ledPin = 2; // Built-in LED on most ESP32
const int TX_PIN = 23; // TX to M5 Atom Echo RX
const int RX_PIN = 22; // RX from M5 Atom Echo TX
const int CO_ANALOG_READ_PIN = 34; // Analog Read Pin from MQ9 CO Sensor

/*
+++++++++++++++++++++++++++++++
|                             |
| BLE Variables               |
|                             |
+++++++++++++++++++++++++++++++
*/
BLEServer* pServer = nullptr;
BLECharacteristic* pCharacteristic = nullptr;
BLEAdvertising* pAdvertising = nullptr;
bool deviceConnected = false;

/*
++++++++++++++++++++++++++++++++
|                              |
| Class Declaration            |
|                              |
++++++++++++++++++++++++++++++++
*/
class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) override {
    deviceConnected = true;
    Serial.println("Device connected");
  }

  void onDisconnect(BLEServer* pServer) override {
    deviceConnected = false;
    Serial.println("Device disconnected");

    // Delay to allow OS to register disconnection
    delay(100);

    // Restart advertising cleanly
    pAdvertising->start();
    Serial.println("Restarted advertising");
  }
};

/*
++++++++++++++++++++++++++++++++
|                              |
| Runtime Variables            |
|                              |
++++++++++++++++++++++++++++++++
*/
bool oldDeviceConnected = false;
unsigned long lastActivityTime = 0;
const unsigned long INACTIVITY_TIMEOUT = 60000; // 60 seconds
int sensorVal = 0;

void setup() {
  __SECTION__(Initialzie Serila Communication between ESP32 & M5 Atom)
  Serial.begin(115200);
  Serial2.begin(115200, SERIAL_8N1, RX_PIN, TX_PIN);

  pinMode(ledPin, OUTPUT);
  
  __SECTION__(Intialize & Start BLE)
  // Reset BLE state
  BLEDevice::deinit(true);
  delay(100);  // Short delay after deinit
  BLEDevice::init("Snoezlen Stroller");
  // Create server and service
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_WRITE |
                      BLECharacteristic::PROPERTY_READ |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pCharacteristic->addDescriptor(new BLE2902());
  pService->start();
  // Prepare advertisement
  pAdvertising = BLEDevice::getAdvertising();
  BLEAdvertisementData advertisementData;
  advertisementData.setName("Snoezlen Stroller");
  advertisementData.setCompleteServices(BLEUUID(SERVICE_UUID));
  pAdvertising->setAdvertisementData(advertisementData);
  pAdvertising->start();
  delay(500);
  Serial.println("BLE device is ready to pair");
}

void loop() {
  if (deviceConnected) {
    String value = pCharacteristic->getValue();

    __SECTION__(Input Received Via App > ESP32)
    if (value.length() > 0) {
      Serial.print("Received: ");
      Serial.println(value);
      // For timeout
      lastActivityTime = millis();
      if (value == "1") digitalWrite(ledPin, HIGH);
      else if (value == "0") digitalWrite(ledPin, LOW);
      else if (value == "3") {
        Serial2.println("3"); // Send to Atom Echo
        Serial.println("Sent '3' to Atom Echo");
      }
      pCharacteristic->setValue(""); // Clear after use
    }

    __SECTION__(Output Receieved Via ESP32 > App)
    sensorVal = analogRead(CO_ANALOG_READ_PIN);
    String sensorValueStr = String(sensorVal);
    pCharacteristic->setValue(sensorValueStr.c_str()); // Send sensor value
    pCharacteristic->notify(); // Notify connected device
    Serial.print("Sent Sensor Data via BLE: ");
    Serial.println(sensorValueStr);
    delay(1000);

  }

  // Optional: Detect device disconnection
  if (!deviceConnected && oldDeviceConnected) {
    delay(500);  // Give time before restarting advertising
    pAdvertising->start();
    Serial.println("Advertising restarted after disconnect");
    oldDeviceConnected = deviceConnected;
  }

  if (deviceConnected && !oldDeviceConnected) {
    // Device just connected
    oldDeviceConnected = deviceConnected;
  }

  if (deviceConnected && (millis() - lastActivityTime > INACTIVITY_TIMEOUT)) {
    Serial.println("No activity for too long. Restarting BLE...");

    pServer->disconnect(0); // Disconnect the central
    delay(100);

    // Optional full reset:
    BLEDevice::deinit(true);
    delay(100);
    ESP.restart(); // or call your init logic again
  }
}