#ifndef REMOTESENSORINSTANCE_HPP
#define REMOTESENSORINSTANCE_HPP

#include <Arduino.h>

class RemoteSensorInstance {
private:
    int _rxPin;
    int _txPin;
    unsigned long _baudRate;
    
    // Internal buffer for building the string
    String _serialBuffer;

    // Store the most recent sensor values
    float _lastTemp;
    int   _lastGas;
    float _lastDist;
    String _lastGPS;       // Store the raw GPS string
    bool _hasNewGPS;       // Flag to tell Main to send it

    // Internal helper to parse the data string
    void processPacket(String packet);

public:
    RemoteSensorInstance();

    // Setup the Serial connection
    void initialize(int rxPin, int txPin, unsigned long baudRate = 9600);

    // Call this in your main loop() to check for new data
    void update();

    // Getters for your main logic to use
    float getTemp();
    int getGas();
    float getDist();
    String getGPS();       // Getter
    bool hasGPS();         // Check flag
};

#endif