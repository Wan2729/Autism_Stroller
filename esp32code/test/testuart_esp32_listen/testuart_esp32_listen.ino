#include <HardwareSerial.h>

HardwareSerial SerialPort(2);  // Use UART2

#define RXD2 22  // GPIO16 will receive data
#define TXD2 23  // Not used on listener, but must be defined

void setup() {
  Serial.begin(9600); // Debug output
  SerialPort.begin(9600, SERIAL_8N1, RXD2, TXD2); // UART2
  SerialPort.setTimeout(100);  // 100 ms
}

void loop() {
  delay(500);
  while (SerialPort.available()) {
    char c = SerialPort.read();
    Serial.print(c);  // Print each character
  }
}