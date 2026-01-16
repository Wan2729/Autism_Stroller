#include <HardwareSerial.h>
#include <TinyGPS++.h>

HardwareSerial gpsSerial(1);
TinyGPSPlus gps;
float latitude, longitude;

void setup() {
  Serial.begin(9600);
  gpsSerial.begin(9600, SERIAL_8N1, 13, 12); // RX = 13, TX = 12
}

void loop() {
  while (gpsSerial.available()) {
    int data = gpsSerial.read();
    if (gps.encode(data)) {
      latitude = gps.location.lat();
      longitude = gps.location.lng();
      Serial.print("Latitude: ");
      Serial.println(latitude);
      Serial.print("Longitude: ");
      Serial.println(longitude);
      delay(1000);
    }
  }
}