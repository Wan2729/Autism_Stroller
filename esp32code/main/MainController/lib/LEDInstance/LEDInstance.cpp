#include "LEDInstance.hpp"

LEDInstance::LEDInstance(int brightness){
    this->brightness = brightness;
    this->optimizeStepSize(NUM_LEDS, this->ledStepSize);
    this->ledState = LEDState::TURN_OFF;
    this->fadeStage = 1.0f;
    this->speedStage = 0.0f;
}

void LEDInstance::initialize(){
    FastLED.addLeds<LED_TYPE, LED_PIN, COLOR_ORDER>(this->leds, NUM_LEDS);
    FastLED.setBrightness(this->brightness);

    Serial.println("LED Initialized");
}

void LEDInstance::turnOff(int &prog){
    playColor(prog, 0, 0, 0);
    if(this->ledState == LEDState::FADING || LEDState::LIGHTING){
        this->fadeStage = 1.0f;
    }
    this->ledState = LEDState::TURN_OFF;
    this->speedStage = 0.0f;
}

void LEDInstance::playColor(int &prog, int hue, int sat, int val){
    int hsv[3] = {hue, sat, val};
    playColor(prog, hsv);
}

void LEDInstance::playColor(int &prog, int* hsv){
    int hue = *(hsv); 
    int sat = *(hsv+1);
    int val = *(hsv+2);
    this->ledState = LEDState::UPDATING;
    float start = this->ledStepSize * prog;
    if(start > NUM_LEDS) {
        this->ledState = LEDState::UPDATE_END;
        prog = 0;
        return;
    }
    float end = start + ledStepSize;
    if(end > NUM_LEDS) end = NUM_LEDS; 
    for (int j = start; j < end; j++) {
        leds[j] = CHSV(hue, sat, val);
    }
    FastLED.show();
    ++prog;
}

void LEDInstance::playFade(int &prog, int* hsv){
    if(this->fadeStage == 1.0f && prog == 0) this->ledState = LEDState::FADING; 
    int hue = *hsv;
    int sat = *(hsv+1);
    int val = *(hsv+2);
    int start = this->ledStepSize * prog;
    if(start > NUM_LEDS) {
        if(this->fadeStage <= this->brightness/4.0f && this->ledState == LEDState::FADING){
            this->fadeStage += 0.002f * this->brightness;
        } else if (this->fadeStage >= this->brightness/20.0f && this->ledState == LEDState::LIGHTING){
            this->fadeStage -= 0.002f * this->brightness;
        } else{
            this->ledState = (this->ledState == LEDState::FADING) ? LEDState::LIGHTING : LEDState::FADING;
        }
        prog = 0;
        return;
    }
    float end = start + ledStepSize;
    if(end > NUM_LEDS) end = NUM_LEDS; 

    int fadeScale = this->brightness / (int)this->fadeStage;
    for (int j = start; j < end; j++) {
        leds[j] = CHSV(hue, sat, val);
        leds[j].nscale8(fadeScale);
    }
    FastLED.show();
    ++prog;
}

void LEDInstance::playTravel(int &prog, int* hsv) {
    int newHue = (hsv[0] + 50) > 255 ? hsv[0]-50 : hsv[0]+50;
    int hsv2[3] = {newHue, hsv[1], hsv[2]};
    playTravel(prog, hsv, hsv2);
}

void LEDInstance::playTravel(int &prog, int* hsv, int* hsv2) {
    int hue = *(hsv);
    int sat = *(hsv+1);
    int val = *(hsv+2);
    
    int hue2 = *(hsv2);
    int sat2 = *(hsv2+1);
    int val2 = *(hsv2+2);

    if(this->ledState != LEDState::TRAVELLING){
        playColor(prog, 0, 0, 0);
        if(this->ledState == LEDState::UPDATE_END){
            this->ledState = LEDState::TRAVELLING;
        }
        return;
    }

    int travelSpeed = 9000;
    if(this->speedStage < travelSpeed){
        speedStage += 0.5f;
        return;
    }
    this->speedStage = 0.0f;

    const int tailLength = 4;
    int start = prog;
    int end = start + tailLength;

    if (start >= NUM_LEDS) {
        this->ledState = LEDState::UPDATE_END;
        prog = 0;
        return;
    }

    if (end > NUM_LEDS) end = NUM_LEDS;

    // Light up the tail
    for (int j = start; j < end; j++) {
        leds[j] = CHSV(hue, sat, val);
    }
    int oldEnd = start;
    int oldStart = oldEnd - tailLength;
    if(oldStart < 0) oldStart = 0;
    for(int j = oldStart; j < oldEnd;++j){
        leds[j] = CHSV(hue2, sat2, val2);
    }
    FastLED.show();

    ++prog;
}

void LEDInstance::playTravelNoFill(int &prog, int* hsv){
    int hsv2[3] = {0, 0, 0};
    playTravel(prog, hsv, hsv2);
}

void LEDInstance::optimizeStepSize(int ledNumbers, float &stepSize){
    int i = 10;
    while(ledNumbers%i == 0){
        i *= 10;
    }
    stepSize = i/10;
}