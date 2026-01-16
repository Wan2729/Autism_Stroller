#pragma once
#include <Arduino.h>
#include <FastLED.h>

#define LED_PIN     12       // Data pin connected to LED strip
#define NUM_LEDS    30        // Number of LEDs in the strip
#define LED_TYPE    WS2812   // LED strip type
#define COLOR_ORDER RGB      // Color order

enum LEDState{
    TURN_OFF,
    FADING,
    LIGHTING,
    TRAVELLING,
    UPDATING,
    UPDATE_END
};

class LEDInstance {
private:
    CRGB leds[NUM_LEDS];
    float ledStepSize;
    int brightness;
    int animation;
    LEDState ledState;
    float fadeStage;
    float speedStage;

public:
    LEDInstance(int brightness);
    void initialize();
    void playColor(int &prog, int hue, int sat, int val);
    void playColor(int &prog, int* hsv);
    void playFade(int &prog, int* hsv);
    void playTravel(int &prog, int* hsv);
    void playTravel(int &prog, int* hsv, int* hsv2);
    void playTravelNoFill(int &prog, int* hsv);
    void turnOff(int &prog);
    void optimizeStepSize(int ledNumbers, float &stepSize);
};