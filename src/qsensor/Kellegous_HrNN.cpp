#include "Kellegous_HrNN.h"

Kellegous_HrNN::Kellegous_HrNN(unsigned int pin)
    : last_pulse_at_(0),
      pin_(pin),
      last_val_(0) {
}

bool Kellegous_HrNN::Update(unsigned int* sample) {
    *sample = 0;

    // read the value from the pin & look for the rising edge of the signal
    byte val = digitalRead(pin_);
    bool is_rising_edge = val && (val != last_val_);
    last_val_ = val;

    if (!is_rising_edge) {
        return 0;
    }

    // we now need the time since the last rising edge.
    unsigned long now = millis();
    unsigned long smp = now - last_pulse_at_;
    last_pulse_at_ = now;
    
    // note: this will discard the first sample since last_pulse_at_
    // is initialized to zero. this is good.
    if (smp > 2000 || smp < 300) {
        return 0;
    }

    *sample = smp;
    return 1;
}
