#ifndef CAMERA_INSTANCE_HPP
#define CAMERA_INSTANCE_HPP

#include <Arduino.h>

class CameraInstance {
public:
    CameraInstance();
    
    // Setup the Serial1 connection
    void initialize(int rxPin, int txPin, unsigned long baudRate = 115200);
    
    // Listen for incoming data (Call this in loop)
    void update();
    
    // Send WiFi command to Camera
    void sendWiFiCredentials(String ssid, String password);
    
    // Get the IP address if we found one
    String getIP();
    
    // Check if a brand new IP just arrived (so we only notify phone once)
    bool hasNewIP();

private:
    int _rxPin;
    int _txPin;
    String _currentIP;
    bool _newIPReceived;
};

#endif