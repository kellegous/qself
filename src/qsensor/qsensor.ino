// Seems like arduino builds -I flags only from the main file.
#include <Adafruit_CC3000.h>
#include <ccspi.h>
#include <SPI.h>

#include "Kellegous_HrNN.h"
#include "Kellegous_Agent.h"

#define HOST "turtle.kellego.us"
#define PORT 8078;
// TODO(knorton): fix this.
#define SSID "";
#define SKEY "";

const uint8_t CMD_RST = 0xff;
const uint8_t CMD_HRT = 0x00;
const uint8_t CMD_TMP = 0x01;

const int HRPIN = 9;
const int LDPIN = 13;
const int TMPIN = 0;

unsigned long last_tmp_at;
Kellegous_HrNN hr(HRPIN);
Kellegous_Agent* agent;

void getConfig(Kellegous_Agent_Config* cfg) {
  cfg->cs_pin = 10;
  cfg->vbat_pin = 5;
  cfg->irq_pin = 3;
  cfg->spi_speed = SPI_CLOCK_DIVIDER;
  cfg->ssid = SSID;
  cfg->ssid_key = SKEY;
  cfg->ssid_secmode = WLAN_SEC_WPA2;
  cfg->host = HOST;
  cfg->port = PORT;
}

void connectAgent() {
  if (agent->waitForConnect()) {
    agent->send(CMD_RST, 0xffff);
  }
}

void setup() {
  // Serial.begin(9600);
  pinMode(LDPIN, OUTPUT);
  
  Kellegous_Agent_Config* cfg = new Kellegous_Agent_Config;
  getConfig(cfg);
  
  agent = new Kellegous_Agent(cfg);
  if (!agent->init()) {
    agent = NULL;
  }
  
  connectAgent();
 
  last_tmp_at = millis();  
}

float compute_tmp(float v) {
  float tc = (v/1024.0 - 0.5) * 100;
  return (tc * 9.0 / 5.0) + 32.0;
}

void loop() {
  connectAgent();

  unsigned int nn;
  if (hr.Update(&nn)) {
    // Serial.println(nn);
    agent->send(CMD_HRT, nn);
  }
  
  unsigned long t = millis();
  if (t - last_tmp_at > 1000) {
    agent->send(CMD_TMP, compute_tmp(analogRead(TMPIN) * 5.0) * 100);
    last_tmp_at = t;
  }
}
