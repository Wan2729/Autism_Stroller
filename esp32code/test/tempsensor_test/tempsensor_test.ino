#include <OneWire.h>
#include <DallasTemperature.h>

// Data wire is connected to digital pin 2
#define ONE_WIRE_BUS 13

// Setup a oneWire instance to communicate with any OneWire devices
OneWire oneWire(ONE_WIRE_BUS);

// Pass oneWire reference to DallasTemperature sensor
DallasTemperature sensors(&oneWire);

void setup() {
  Serial.begin(115200);         // Start serial communication
  sensors.begin();            // Start the DS18B20 sensor

  Serial2.begin(9600, SERIAL_8N1, 16, 17); // RX=16, TX=17
}

void loop() {
  sensors.requestTemperatures();                 // Request temperature from sensor
  float temperatureC = sensors.getTempCByIndex(0);  // Read temperature in Celsius

  Serial.print("Temperature: ");
  Serial.print(temperatureC);
  Serial.println(" °C");

  Serial2.printf("Temperature: %.2f °C\n", temperatureC);

  delay(1000);  // Wait 1 second before next read
}