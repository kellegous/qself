#include "Kellegous_Agent.h"

void(* resetDevice) (void) = 0;

Kellegous_Agent::Kellegous_Agent(Kellegous_Agent_Config* cfg) : cfg_(cfg) {
  cc3000_ = new Adafruit_CC3000(
    cfg_->cs_pin,
    cfg_->irq_pin,
    cfg_->vbat_pin,
    cfg_->spi_speed);
}

Kellegous_Agent::~Kellegous_Agent() {
  cc3000_->stop();
  delete cc3000_;
  cc3000_ = NULL;
  delete cfg_;
}

bool Kellegous_Agent::init() {
  return cc3000_->begin();
}

bool Kellegous_Agent::connect() {
  if (!cc3000_->checkConnected()) {
    
    // we need to connect to AP
    if (!cc3000_->connectToAP(cfg_->ssid, cfg_->ssid_key, cfg_->ssid_secmode)) {
      return 0;
    }

    // wait until we have an ip address.
    while (!cc3000_->checkDHCP()) {
      delay(200);
    }
  }

  if (!client_.connected()) {
    if (!client_.connect(cfg_->host, cfg_->port)) {
      return 0;
    }
  }

  return client_.connected();
}

bool Kellegous_Agent::send(uint8_t cmd, unsigned int val) {
  uint8_t buf[3];
  buf[0] = cmd;
  buf[1] = (val>>8) & 0xff;
  buf[2] = val & 0xff;
  if (client_.write(buf, 3) != 3) {
    client_.close();
    return 0;
  }
  return 1;
}

int Kellegous_Agent::delayFor(int c) {
  if (c < 3) {
    return 200;
  } else if (c < 6) {
    return 400;
  } else if (c < 12) {
    return 1000;
  }
  return 10000;
}

bool Kellegous_Agent::waitForConnect() {
  if (cc3000_->checkConnected() && client_.connected()) {
    return 0;
  }
  
  client_.close();
  
  int c = 0;
  for (;;) {
    Serial.print("connect #"); Serial.println(c);
    if (connect()) {
      return 1;
    }
    
    delay(delayFor(c));
    c++;
    
    if (c > 5) {
      resetDevice();
    }
  }
}
