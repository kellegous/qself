#include "Kellegous_Hr.h"
#include "Kellegous_HrNN.h"

const uint8_t CMD_RST = 0xff;
const uint8_t CMD_HRT = 0x00;
const uint8_t CMD_TMP = 0x01;

const int HRPIN = 10;
const int LDPIN = 13;
const int TMPIN = 0;

unsigned long last_tmp_at;
Kellegous_Hr hr(HRPIN, 5);

void send(uint8_t tag, unsigned int val) {
  uint8_t buf[3];
  buf[0] = tag;
  buf[1] = (val>>8) & 0xff;
  buf[2] = val & 0xff;
  Serial.write(buf, 3);
}

void setup() {
  Serial.begin(9600);
  pinMode(LDPIN, OUTPUT);
  hr.Init();
  
  last_tmp_at = millis();
  
  send(CMD_RST, 0xffff);
  
  for (int i = 0; i < 4; i++) {
    digitalWrite(LDPIN, HIGH);
    delay(200);
    digitalWrite(LDPIN, LOW);
    delay(200);
  }
}

float compute_tmp(float v) {
  float tc = (v/1024.0 - 0.5) * 100;
  return (tc * 9.0 / 5.0) + 32.0;
}

void loop() {  
  if (hr.Update()) {
    send(CMD_HRT, hr.GetHr() * 100);
  }
  
  unsigned long t = millis();
  if (t - last_tmp_at > 1000) {
    send(CMD_TMP, compute_tmp(analogRead(TMPIN) * 5.0) * 100);
    last_tmp_at = t;
  }
}
