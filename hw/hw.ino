#include <stdarg.h>

const int HRPIN = 10;
const int LDPIN = 13;

byte a, b;

int index;
const int num_samples = 5;
unsigned long samples[5];

unsigned long last_pulse_at;

//volatile int diff1 = 0;
//volatile int diff2 = 0;
//volatile int diff3 = 0;
//volatile int diff4 = 0;
//volatile int diff5 = 0;
//
//void HRpulse() {
//  pulsetime = millis();
//  rollBuffer();
//  diff1 = pulsetime - lastpulsetime;
//  if (diff5 != 0) {
//    BPM = 60000 / ((diff1 + diff2 + diff3 + diff4 + diff5)/5);
//  }
//  lastpulsetime = pulsetime;
//}
//
//void rollBuffer() {
//  diff5 = diff4;
//  diff4 = diff3;
//  diff3 = diff2;
//  diff2 = diff1;
//  diff1 = 0;
//}

void setup() {
  memset(samples, 0, sizeof(samples));
  index = 0;
  
  last_pulse_at = millis();
  
  Serial.begin(9600);
  
  pinMode(HRPIN, INPUT);
  pinMode(LDPIN, OUTPUT);
  while (!digitalRead(HRPIN)) {};
  
  Serial.println("Ready");  
}

void push_sample(unsigned long val) {
  samples[index] = val;
  index = (index+1) % num_samples;
}

int compute_hr() {
  unsigned long sum = 0;
  for (int i = 0; i < num_samples; i++) {
    sum += samples[i];
  }
  
  if (sum == 0) {
    return 0;
  }
  
  return 60000 / (sum/num_samples);
}

void dump_samples() {
  Serial.println("BEG DUMP");
  for (int i = 0; i < num_samples; i++) {
    Serial.println(samples[i]);
  }
  Serial.println("END DUMP");
}


void p(char *fmt, ... ){
        char buf[128]; // resulting string limited to 128 chars
        va_list args;
        va_start (args, fmt );
        vsnprintf(buf, 128, fmt, args);
        va_end (args);
        Serial.print(buf);
}

void loop() {
  a = digitalRead(HRPIN);
  if (a && (a != b)) {
    unsigned long time = millis();
    push_sample(time - last_pulse_at);
    last_pulse_at = time;
    int hr = compute_hr();
    p("%d bpm\n", hr);
    // Serial.printf("%d bpm", hr);
  }
  b = a;
}
