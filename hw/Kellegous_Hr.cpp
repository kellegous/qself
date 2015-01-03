#include "Kellegous_Hr.h"

Kellegous_Hr::Kellegous_Hr(int pin, int size)
    : pin_(pin),
      num_samples_(size),
      last_val_(0),
      samples_(NULL),
      samples_idx_(0),
      last_pulse_at_(millis()),
      count_(0) {
  samples_ = (unsigned long*)malloc(sizeof(unsigned long) * size);
  memset(samples_, 0, sizeof(unsigned long) * size);
  
}

Kellegous_Hr::~Kellegous_Hr() {
  free(samples_);
}

bool Kellegous_Hr::Update() {
  byte val = digitalRead(pin_);
  bool changed = val && (val != last_val_);
  last_val_ = val;
  
  // no new reading.
  if (!changed) {
    return 0;
  }
  
  unsigned long now = millis();
  AddSample(now - last_pulse_at_);
  last_pulse_at_ = now;
  
  // we don't have enough data yet.
  if (count_ < num_samples_) {
    return 0;
  }
  
  return 1;
}

void Kellegous_Hr::AddSample(unsigned long t) {
  samples_[samples_idx_] = t;
  samples_idx_ = (samples_idx_ + 1) % num_samples_;
  if (count_ < num_samples_) {
    count_++;
  }
}

float Kellegous_Hr::GetHr() {
  unsigned long sum = 0;
  for (int i = 0; i < num_samples_; i++) {
    sum += samples_[i];
  }

  if (sum == 0) {
    return 0;
  }

  return 60000.0 / ((float)sum / num_samples_);
}

void Kellegous_Hr::Init() {
    pinMode(pin_, INPUT);
    
}
