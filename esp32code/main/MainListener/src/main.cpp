#include <Arduino.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>
#include <TinyGPS++.h> // Install "TinyGPSPlus" library

// --- PIN DEFINITIONS ---
#define PIN_DS18B20 13 // (Left side, near VIN)
#define PIN_MQ_GAS  36 // (VP - Top Left)
#define RX_PIN      16 // To Controller TX
#define TX_PIN      17 // To Controller RX

// NEW: GPS Pins (Left side, right next to PIN 13/VIN)
#define GPS_RX_PIN  12 // Connect to GPS TX
#define GPS_TX_PIN  14 // Connect to GPS RX

// --- CONFIGURATION ---
#define DIST_SCALE 0.2 
#define FRICTION 0.90

// --- OBJECTS ---
OneWire oneWire(PIN_DS18B20);
DallasTemperature sensors(&oneWire);
Adafruit_MPU6050 mpu;

// NEW: GPS Objects
TinyGPSPlus gps;
HardwareSerial gpsSerial(1); // Use UART 1

// --- VARIABLES ---
unsigned long previousMillis = 0;
const long interval = 5000; 

// NEW: GPS Timer
unsigned long previousGpsMillis = 0;
const long gpsInterval = 30000; // 30 Seconds

// Physics Variables
float accelOffsetX = 0; 
float velocityX = 0;
float distanceX = 0;
unsigned long lastTime = 0;

void setup() {
  Serial.begin(115200);
  
  // 1. Comm with Controller (Serial2)
  Serial2.begin(9600, SERIAL_8N1, RX_PIN, TX_PIN); 

  // 2. Comm with GPS (Serial1)
  // We map Serial1 to pins 12 and 14
  gpsSerial.begin(9600, SERIAL_8N1, GPS_RX_PIN, GPS_TX_PIN);

  pinMode(PIN_MQ_GAS, INPUT);
  sensors.begin();

  if (!mpu.begin()) {
    Serial.println("MPU6050 not found!");
    while (1) delay(10);
  }
  
  mpu.setAccelerometerRange(MPU6050_RANGE_2_G);
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);

  Serial.println("Calibrating MPU... Keep still!");
  delay(1000);
  
  float totalX = 0;
  for(int i=0; i<50; i++) {
    sensors_event_t a, g, temp;
    mpu.getEvent(&a, &g, &temp);
    totalX += a.acceleration.x;
    delay(10);
  }
  accelOffsetX = totalX / 50.0;
  Serial.println("System Ready.");
  
  lastTime = millis();
}

void loop() {
  // --- 1. ALWAYS READ GPS ---
  // Must feed the object constantly to get data
  while (gpsSerial.available() > 0) {
    char c = gpsSerial.read();
    Serial.write(c);
    gps.encode(c);
  }

  unsigned long currentMillis = millis();
  
  // --- 2. PHYSICS ENGINE ---
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  float accelX = a.acceleration.x - accelOffsetX;
  if (abs(accelX) < 0.15) accelX = 0;

  float dt = (currentMillis - lastTime) / 1000.0;
  lastTime = currentMillis;

  velocityX = (velocityX * FRICTION) + (accelX * dt);
  float stepDistance = velocityX * dt;

  if (abs(velocityX) > 0.05) {
     distanceX += abs(stepDistance) * DIST_SCALE;
  }

  // --- 3. SENSOR REPORT (5 SEC) ---
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;

    sensors.requestTemperatures(); 
    float tempC = sensors.getTempCByIndex(0);
    int gasRaw = analogRead(PIN_MQ_GAS);

    // Packet: T:25.5,G:120,D:10.5
    String dataPacket = "T:" + String(tempC, 1) + 
                        ",G:" + String(gasRaw) + 
                        ",D:" + String(distanceX, 2) + "\n";
    
    Serial2.print(dataPacket);
    Serial.print("Sensors >> "); Serial.print(dataPacket);
  }

  // --- 4. GPS REPORT (60 SEC) ---
  if (currentMillis - previousGpsMillis >= gpsInterval) {
    previousGpsMillis = currentMillis;

    if (gps.location.isValid()) {
      // Packet: L:10.123456,101.123456
      String gpsPacket = "L:" + String(gps.location.lat(), 6) + 
                         "," + String(gps.location.lng(), 6) + "\n";

      Serial2.print(gpsPacket);
      Serial.print("GPS >> "); Serial.println(gpsPacket);
    } else {
      Serial.println("GPS: Searching for satellites...");
    }
  }
}