#include <HardwareSerial.h>

HardwareSerial SerialPort(2);  // Use UART2

#define RXD2 16  // Not used on sender, but must be defined
#define TXD2 17  // GPIO17 will send data

void setup() {
  Serial.begin(9600); // Debug output
  SerialPort.begin(9600, SERIAL_8N1, RXD2, TXD2); // UART2
  SerialPort.setTimeout(100);  // 100 ms
}

void loop() {
  SerialPort.println("Hello from Sender ESP32!");
  Serial.println("Sent: Hello from Sender ESP32!");
  delay(1000);
}