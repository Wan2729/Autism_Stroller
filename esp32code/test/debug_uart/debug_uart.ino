void setup() {
  Serial.begin(115200);      // For debugging
  Serial2.begin(115200, SERIAL_8N1, 1, 3); // UART to Atom Echo (TX=17, RX=16)
}

void loop() {
  // Example: Send "3" every 10 seconds
  Serial2.println("3");
  delay(10000);
}
