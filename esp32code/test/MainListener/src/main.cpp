#include <Arduino.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>

// --- PIN DEFINITIONS ---
#define PIN_DS18B20 13
#define PIN_MQ_GAS  36
#define RX_PIN      16 // Connect to TX (23) of other ESP
#define TX_PIN      17 // Connect to RX (22) of other ESP

// --- CONFIGURATION ---
// 1. SCALING: 0.2 means 1 meter real movement = 0.2 meter in code
#define DIST_SCALE 0.2 
// 2. FRICTION: How fast it stops gliding (0.90 = stops fast, 0.99 = stops slow)
#define FRICTION 0.90

// --- OBJECTS ---
OneWire oneWire(PIN_DS18B20);
DallasTemperature sensors(&oneWire);
Adafruit_MPU6050 mpu;

// --- VARIABLES ---
unsigned long previousMillis = 0;
const long interval = 5000; 

// Variables for Distance Calculation
float accelOffsetX = 0; 
float velocityX = 0;
float distanceX = 0;
unsigned long lastTime = 0;

void setup() {
  Serial.begin(115200);
  
  // Initialize Serial2 for communication with 2nd ESP32
  // Format: Serial2.begin(Baud, Config, RX_Pin, TX_Pin);
  Serial2.begin(9600, SERIAL_8N1, RX_PIN, TX_PIN); 

  pinMode(PIN_MQ_GAS, INPUT);
  sensors.begin();

  if (!mpu.begin()) {
    Serial.println("MPU6050 not found!");
    while (1) delay(10);
  }
  
  mpu.setAccelerometerRange(MPU6050_RANGE_2_G);
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);

  Serial.println("Calibrating MPU X-axis... Keep sensor still!");
  delay(1000);
  
  // --- SIMPLE CALIBRATION ---
  // We take 50 readings and average them to find the "Zero" point
  float totalX = 0;
  for(int i=0; i<50; i++) {
    sensors_event_t a, g, temp;
    mpu.getEvent(&a, &g, &temp);
    totalX += a.acceleration.x;
    delay(10);
  }
  accelOffsetX = totalX / 50.0;
  Serial.print("Offset Calculated: "); Serial.println(accelOffsetX);
  Serial.println("System Ready.");
  
  lastTime = millis();
}

void loop() {
  unsigned long currentMillis = millis();
  
  // --- CONTINUOUS PHYSICS ENGINE ---
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  // 1. Remove offset
  float accelX = a.acceleration.x - accelOffsetX;

  // 2. Deadzone
  if (abs(accelX) < 0.15) {
    accelX = 0;
  }

  // 3. Time difference
  float dt = (currentMillis - lastTime) / 1000.0;
  lastTime = currentMillis;

  // 4. Integrate Velocity (Directional)
  // We MUST keep direction here for friction to work properly!
  velocityX = (velocityX * FRICTION) + (accelX * dt);

  // 5. Calculate Distance (ACCUMULATIVE / ODOMETER)
  // Calculate how much we moved in this tiny fraction of a second
  float stepDistance = velocityX * dt;

  // If we are moving (velocity > 0.05), add the MAGNITUDE to the total
  // abs() ensures even if stepDistance is negative (moving back), we add positive distance
  if (abs(velocityX) > 0.05) {
     distanceX += abs(stepDistance) * DIST_SCALE;
  }

  // --- 5-SECOND REPORTING ---
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;

    // Read other sensors
    sensors.requestTemperatures(); 
    float tempC = sensors.getTempCByIndex(0);
    int gasRaw = analogRead(PIN_MQ_GAS);

    // Format Data
    Serial.println("--- 5 Sec Report ---");
    Serial.print("Temp: "); Serial.print(tempC); Serial.println(" C");
    Serial.print("Gas:  "); Serial.println(gasRaw);
    Serial.print("Dist: "); Serial.print(distanceX); Serial.println(" m");

    String dataPacket = "T:" + String(tempC, 1) + 
                        ",G:" + String(gasRaw) + 
                        ",D:" + String(distanceX, 2) + "\n";
    
    Serial2.print(dataPacket);
    Serial.print("Sent >> "); Serial.println(dataPacket);
  }
}