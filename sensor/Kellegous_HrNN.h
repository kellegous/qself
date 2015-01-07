#ifndef KELLEGOUS_HRNN_H_
#define KELLEGOUS_HRNN_H_

#if (ARDUINO >= 100)
    #include <Arduino.h>
#else
    #include <WProgram.h>
    #include <pins_arduino.h>
#endif

class Kellegous_HrNN {
 public:
    Kellegous_HrNN(unsigned int pin);
    bool Update(unsigned int* sample);
 private:
    unsigned long last_pulse_at_;
    unsigned int pin_;
    byte last_val_;
};

#endif // KELLEGOUS_HRNN_H_
