#include "RemoteSensorInstance.hpp"

RemoteSensorInstance::RemoteSensorInstance() {
    _serialBuffer = "";
    _lastTemp = 0.0;
    _lastGas = 0;
    _lastDist = 0.0;
}

void RemoteSensorInstance::initialize(int rxPin, int txPin, unsigned long baudRate) {
    _rxPin = rxPin;
    _txPin = txPin;
    _baudRate = baudRate;
    
    // Initialize Hardware Serial 2
    Serial2.begin(_baudRate, SERIAL_8N1, _rxPin, _txPin);
}

void RemoteSensorInstance::update() {
    // Non-blocking read: If no data is there, it returns immediately
    while (Serial2.available()) {
        char inChar = (char)Serial2.read();

        // Check for end of packet (Newline)
        if (inChar == '\n') {
            processPacket(_serialBuffer);
            _serialBuffer = ""; // Reset buffer for next packet
        } 
        else if (inChar != '\r') { 
            // Append char to buffer (ignoring carriage returns)
            _serialBuffer += inChar;
        }
    }
}

void RemoteSensorInstance::processPacket(String packet) {
    // Expecting format: "T:24.5,G:120,D:1.50"
    int tIndex = packet.indexOf("T:");
    int gIndex = packet.indexOf("G:");
    int dIndex = packet.indexOf("D:");

    if (tIndex != -1 && gIndex != -1 && dIndex != -1) {
        // Parse Temperature
        String tStr = packet.substring(tIndex + 2, packet.indexOf(",", tIndex));
        _lastTemp = tStr.toFloat();

        // Parse Gas
        String gStr = packet.substring(gIndex + 2, packet.indexOf(",", gIndex));
        _lastGas = gStr.toInt();

        // Parse Distance
        String dStr = packet.substring(dIndex + 2);
        _lastDist = dStr.toFloat();
        
        // Debugging (Optional)
        // Serial.printf("Updated -> T:%.1f G:%d D:%.2f\n", _lastTemp, _lastGas, _lastDist);
    }
}

float RemoteSensorInstance::getTemp() { return _lastTemp; }
int   RemoteSensorInstance::getGas()  { return _lastGas; }
float RemoteSensorInstance::getDist() { return _lastDist; }