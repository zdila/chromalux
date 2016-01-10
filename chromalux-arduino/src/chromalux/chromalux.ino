// ino clean
// ino build -d ~/arduino-1.6.4 -m 'leonardo'
// ino upload -d ~/arduino-1.6.4 -m 'leonardo'
// ino serial

#include <Streaming.h>        //http://arduiniana.org/libraries/streaming/
#include <DS3232RTC.h>        //http://github.com/JChristensen/DS3232RTC
#include <Time.h>             //http://playground.arduino.cc/Code/Time
#include <Wire.h>             //http://arduino.cc/en/Reference/Wire
#include <EEPROM.h>

void hsvToRgb(float *rgb, float h, float s, float v);

unsigned long previousMicros;
unsigned long lastWrite;
unsigned long lastSchedulerCheck;

float transitionSpeed = 1.0;

typedef struct hsv {
  float h, s, v;
};

typedef struct rgb {
  float r, g, b;
};

unsigned addr = 0;
const unsigned VERSION_ADDR = addr; // unsigned byte
const unsigned TARGET_RGB_ADDR = addr += sizeof(byte); // struct rgb
const unsigned TRANSITION_SPEED_ADDR = addr += sizeof(struct rgb); // float

struct hsv targetHsv = { .h = 0, .s = 0, .v = 0 };
struct rgb targetRgb = { .r = 0, .g = 0, .b = 0 };
struct rgb currentRgb = { .r = 0, .g = 0, .b = 0 };

unsigned pos = 0;

union params {
  struct hsv hsv; // 1
  float amp; // 2
  float freq; // 3
  time_t time; // 4
};

struct scheduled {
  time_t time;
  byte cmd;
  union params params;
};

struct command {
  byte cmd;

  union payload {
    union params params;
    struct scheduled scheduled; // 5
  } payload;
};

union data {
  char buffer[sizeof(struct command)];
  struct command command;
} data;

float amp = 0.0;

float freq = 0.0;

unsigned status = 0;
unsigned len;

struct scheduled scheduled;

void setup() {
  initRgb();

  Serial.begin(9600);
  // while(!Serial); // wait for connect (for debug only, remove for production)

  pinMode(7, OUTPUT);
  if (false) {
    digitalWrite(7, HIGH);
    Serial1.begin(38400);
    delay(3000);
    digitalWrite(7, LOW);
    delay(1000);
    Serial1 << "AT+NAME=ChromaLux" << endl;
    delay(1000);
    Serial1 << "AT+UART=38400,1,0" << endl;
    delay(1000);
    Serial1 << "AT+RESET" << endl;
    delay(1000);
  } else {
    digitalWrite(7, LOW);
  }

  Serial1.begin(38400);

  //setSyncProvider() causes the Time library to synchronize with the
  //external RTC by calling RTC.get() every five minutes by default.

  setSyncProvider(RTC.get);

  Serial << F("RTC Sync");
  if (timeStatus() != timeSet) {
    Serial << F(" FAIL!");
  } else {
    Serial << F(" OK");
  }
  Serial << endl;

  // TODO read scheduler stuff from EEPROM
  scheduled.time = 0L; // nothing is scheduled

  const byte version = EEPROM.read(VERSION_ADDR);
  if (version == 255 || version == 0) {
    EEPROM.write(VERSION_ADDR, 1);
    EEPROM.put(TARGET_RGB_ADDR, targetRgb);
    EEPROM.put(TRANSITION_SPEED_ADDR, transitionSpeed);
  } else {
    EEPROM.get(TARGET_RGB_ADDR, targetRgb);
    EEPROM.get(TRANSITION_SPEED_ADDR, transitionSpeed);
  }

  previousMicros = micros();
  lastWrite = millis();
  lastSchedulerCheck = millis();
}

void loop() {

  if (Serial1.available()) {
    int c = Serial1.read();

    // TODO reset to status 0 if no char received in eg 100 ms
    // TODO CRC

    if (status == 0) { // read sync byte
      if (c == 0xAA) {
        status++;
      } else {
        // Serial << "garbage " << (int) c << endl;
      }
    } else if (status == 1) { // read command length
      len = c;
      status++;
      pos = 0;
    } else if (status == 2) {
      if (pos < sizeof(struct command)) {
        data.buffer[pos++] = c;
      }

      if (--len == 0) {
        status = 0;
        runCommand(data.command.cmd, data.command.payload.params);
      }
    }
  }

  // float rgb[3];
  // hsvToRgb(rgb, targetHsv.h + (freq <= 0.f ? 0.f : (180.f * amp * sin(millis() / 1000.f * 2.f * freq * PI))), targetHsv.s, targetHsv.v);

  unsigned long currentMillis = millis();

  if (currentMillis - lastWrite > 5000) { // prevent many write cycles (5s)
    struct rgb testRgb;
    EEPROM.get(TARGET_RGB_ADDR, testRgb);

    // TODO better implement as dirty flag checking
    if (testRgb.r != targetRgb.r || testRgb.g != targetRgb.g || testRgb.b != targetRgb.b) {
      EEPROM.put(TARGET_RGB_ADDR, targetRgb);
    }
    lastWrite = currentMillis;
  }

  if (currentMillis - lastSchedulerCheck > 1000) {
    // TODO maybe also check if not too late
    // TODO recurring events
    if (scheduled.time && scheduled.time <= now()) {
      scheduled.time = 0L;
      // TODO write to EEPROM
      runCommand(scheduled.cmd, scheduled.params);
    }

    lastSchedulerCheck = currentMillis;

    // Serial << hour() << ':' << minute() << ':' << second() << endl;
  }

  // move currentRgb closer to targetRgb
  unsigned long currentMicros = micros();
  unsigned long delta = currentMicros - previousMicros;
  previousMicros = currentMicros;
  float d = delta / 1000000.0 * transitionSpeed;
  currentRgb.r = trans(currentRgb.r, targetRgb.r, d);
  currentRgb.g = trans(currentRgb.g, targetRgb.g, d);
  currentRgb.b = trans(currentRgb.b, targetRgb.b, d);

  applyRgb(currentRgb.r, currentRgb.g, currentRgb.b);
}

void runCommand(const unsigned cmd, const union params &params) {
  switch (cmd) {
    case 1:
      targetHsv = params.hsv;

      float rgb[3];
      hsvToRgb(rgb, targetHsv.h, targetHsv.s, targetHsv.v);
      targetRgb = { .r = rgb[0], .g = rgb[1], .b = rgb[2]};
      break;
    case 2:
      amp = params.amp;
      break;
    case 3:
      freq = params.freq;
      break;
    case 4:
      RTC.set(params.time);
      setTime(params.time);
      break;
    case 5:
      scheduled = data.command.payload.scheduled;
      // TODO save to EEPROM
      break;
    default:
      // log error
      break;
  }
}

float trans(float c, const float t, const float delta) {
  if (c < t) {
    c += delta;
    if (c > t) {
      c = t; // to prevent fibrilation
    }
  } else if (c > t) {
    c -= delta;
    if (c < t) {
      c = t; // to prevent fibrilation
    }
  }

  return c;
}

