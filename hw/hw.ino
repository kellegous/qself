#include "Kellegous_Hr.h"

const int HRPIN = 10;
const int LDPIN = 13;
const int TMPIN = 0;

Kellegous_Hr hr(HRPIN, 5);

void setup() {  
  Serial.begin(9600);
  pinMode(LDPIN, OUTPUT);
  hr.Init();
  Serial.println("Ready");
}

float compute_tmp(float v) {
  float tc = (v/1024.0 - 0.5) * 100;
  return (tc * 9.0 / 5.0) + 32.0;
}

void loop() {
  if (hr.Update()) {
    float tmp = compute_tmp(analogRead(TMPIN) * 5.0);
    Serial.print(hr.GetHr()); Serial.print(" bpm, ");
    Serial.print(tmp); Serial.println(" deg F");
  }  
}
