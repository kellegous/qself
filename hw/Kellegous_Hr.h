#ifndef KELLEGOUS_HR_H_
#define KELLEGOUS_HR_H_

#if (ARDUINO >= 100)
 #include <Arduino.h>
#else
 #include <WProgram.h>
 #include <pins_arduino.h>
#endif

class Kellegous_Hr {
 public:
  Kellegous_Hr(int pin, int size);
  ~Kellegous_Hr();
  bool Update();
  float GetHr();
  void Init();
 private:
  void AddSample(unsigned long t);
  int pin_;
  int num_samples_;
  byte last_val_;
  unsigned long* samples_;
  int samples_idx_;
  unsigned long last_pulse_at_;
  int count_;
};

#endif // KELLEGOUS_HR_H_
