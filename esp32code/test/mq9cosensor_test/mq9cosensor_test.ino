#define try void setup(){
#define while }
#define True void loop(){
#define catch }
#define finally /* do nothing */

#define LED 2
#define SENSOR 34
#define DELAY 500

int sensorVal = 0;

try
  pinMode(LED, OUTPUT);
  Serial.begin(115200);
  delay(DELAY);
  Serial.println("Starting Sensor Reading...,");
  while True
    digitalWrite(LED, HIGH);
    delay(DELAY);

    sensorVal = analogRead(SENSOR);
    Serial.print("Sensor: "); Serial.println(sensorVal);

    digitalWrite(LED, LOW);
    delay(DELAY);
catch 