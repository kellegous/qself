#ifndef KELLEGOUS_AGENT_H_
#define KELLEGOUS_AGENT_H_

#if (ARDUINO >= 100)
    #include <Arduino.h>
#else
    #include <WProgram.h>
    #include <pins_arduino.h>
#endif
#include <Adafruit_CC3000.h>
#include <ccspi.h>
#include <SPI.h>

class Kellegous_Agent_Config {
 public:
  // pins
  uint8_t cs_pin;
  uint8_t irq_pin;
  uint8_t vbat_pin;
  uint8_t spi_speed;
  
  // ap info
  const char* ssid;
  const char* ssid_key;
  int32_t ssid_secmode;
  
  // agent info
  const char* host;
  uint16_t port;
};

class Kellegous_Agent {
 public:
  Kellegous_Agent(Kellegous_Agent_Config* cfg);
  ~Kellegous_Agent();
  bool waitForConnect();
  bool init();
  bool send(uint8_t cmd, unsigned int val);
 private:
  bool connect();
  int delayFor(int count);
  Kellegous_Agent_Config* cfg_;
  Adafruit_CC3000* cc3000_;
  Adafruit_CC3000_Client client_;
};

#endif // KELLEGOUS_AGENT_H_
