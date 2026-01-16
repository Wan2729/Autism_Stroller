#include <FastLED.h>

#define LED_PIN     12       // Data pin connected to LED strip
#define NUM_LEDS    30        // Number of LEDs in the strip
#define BRIGHTNESS  200      // Brightness (0–255)
#define LED_TYPE    WS2812   // LED strip type
#define COLOR_ORDER GRB      // Color order

CRGB leds[NUM_LEDS];
int animationPreset;

void travelLEDAnimation();
void turnOffLEDAnimation();
void fadeLEDAnimation();

void setup() {
  FastLED.addLeds<LED_TYPE, LED_PIN, COLOR_ORDER>(leds, NUM_LEDS);
  FastLED.setBrightness(BRIGHTNESS);
  Serial.begin(9600);
}

void loop() {
  animationPreset = 1;
  if (Serial.available() > 0) {
    int incoming = Serial.parseInt(); // Read once
    if (incoming != 0) {
      animationPreset = incoming;
      Serial.print("Received preset: ");
      Serial.println(animationPreset);
    }
  }

  switch(animationPreset){
    case 1:
      Serial.println("Playing Travel"); 
      travelLEDAnimation();
    break;
    case 2:
      Serial.println("Playing Fade"); 
      fadeLEDAnimation();
    break;
    case -1:
      Serial.println("Turn OFF"); 
      turnOffLEDAnimation();
    break;
  }
}

void travelLEDAnimation(){
  int lastLedToLightUp;
  int brightness;
  int brightnessAdjustment;
  // Simple color wipe animation
  for (int i = 0; i < NUM_LEDS; i++) {
    lastLedToLightUp = i+4 < NUM_LEDS ? i+4 : NUM_LEDS;
    for(int j = i; j <= lastLedToLightUp; j++){
      leds[j] = CRGB::Green;
    }
    leds[i].nscale8(64);
    leds[lastLedToLightUp].nscale8(64);
    brightnessAdjustment = abs(i - (NUM_LEDS/2));
    brightness = BRIGHTNESS - BRIGHTNESS/(NUM_LEDS/2)*brightnessAdjustment;
    FastLED.setBrightness(brightness);
    FastLED.show();
    delay(200 - 200/(NUM_LEDS/2)*brightnessAdjustment);
    for(int j = i; (j < i+4) || (j == NUM_LEDS); j++){
      leds[j] = CRGB(0, 50, 50);
      leds[j].nscale8(BRIGHTNESS / 4); 
    }
  }
}

void fadeLEDAnimation(){
  int fadeStage = 100;
  for(int i = 0; i < fadeStage; ++i){
    for (int j = 0; j < NUM_LEDS; j++) {
      leds[j] = CRGB::Green;
      leds[j].nscale8(BRIGHTNESS/fadeStage * i);
    }
    FastLED.show();
    delay(100);
  }
}

void turnOffLEDAnimation(){
  for (int j = 0; j < NUM_LEDS; j++) {
    leds[j] = CRGB::Black;
  }
  FastLED.show();
  delay(1000);
}

