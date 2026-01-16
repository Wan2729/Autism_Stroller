#include "CameraInstance.hpp"

CameraInstance::CameraInstance() {
    _currentIP = "";
    _newIPReceived = false;
}

void CameraInstance::initialize(int rxPin, int txPin, unsigned long baudRate) {
    _rxPin = rxPin;
    _txPin = txPin;
    
    // We use Serial1 for the Camera
    // format: Serial1.begin(baud, config, rx, tx)
    Serial1.begin(baudRate, SERIAL_8N1, _rxPin, _txPin);
}

void CameraInstance::update() {
    if (Serial1.available()) {
        String msg = Serial1.readStringUntil('\n');
        msg.trim(); // Remove whitespace/newlines

        // Check if the message is an IP address
        if (msg.startsWith("IP:")) {
            _currentIP = msg;     // Save the full string "IP:192.168.x.x"
            _newIPReceived = true; // Flag that we have news for the main loop
        }
    }
}

void CameraInstance::sendWiFiCredentials(String ssid, String password) {
    // Format the command: "WIFI:SSID,PASSWORD"
    String command = "WIFI:" + ssid + "," + password;
    Serial1.println(command);
}

String CameraInstance::getIP() {
    _newIPReceived = false; // Reset flag after reading
    return _currentIP;
}

bool CameraInstance::hasNewIP() {
    return _newIPReceived;
}