#pragma once
#include <Arduino.h>
#include <SoftwareSerial.h>

class DisplayInstance {
private:
    SoftwareSerial* displaySerial;
    const int rxPin;
    const int txPin;

public:
    DisplayInstance(int rx, int tx) : rxPin(rx), txPin(tx) {
        displaySerial = new SoftwareSerial();
    }

    void initialize() {
        // Use 9600 baud for stability with SoftwareSerial
        displaySerial->begin(9600, SWSERIAL_8N1, rxPin, txPin);
    }

    void playVideo(int videoIndex) {
        // Protocol: "V:1", "V:2", etc.
        String cmd = "V:" + String(videoIndex);
        displaySerial->println(cmd);
        Serial.println("Sent to Display: " + cmd);
    }

    void stopVideo() {
        displaySerial->println("STOP");
    }
};