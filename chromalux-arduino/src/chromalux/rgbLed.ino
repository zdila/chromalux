// D5, D9 & D10 - timer 1 and 3
// D5 = PC6, OC3A, OC4A
// D9 = PB5, OC1A, !OC4B
// D10 = PB6, OC1B, OC4B
// *D11 = PB7, OC0A, OC1C

void initRgb() {
  pinMode(10, OUTPUT);
  pinMode(9, OUTPUT);
  pinMode(5, OUTPUT);

  TCCR1A = /*R*/ _BV(COM1B1) | _BV(WGM11) | /*G*/_BV(COM1A1);
  TCCR1B = _BV(WGM13) | _BV(WGM12) | _BV(CS10);
  TCCR1C = 0;
  ICR1 = 0xffff; // TOP

  TCCR3A = /*B*/_BV(COM3A1) | _BV(WGM31);
  TCCR3B = _BV(WGM33) | _BV(WGM32) | _BV(CS30);
  TCCR3C = 0;

  ICR3 = 0xffff; // TOP

  // black
  TCCR1A = _BV(WGM11);
  TCCR3A = _BV(WGM31);
}

void applyRgb(const float red, const float green, const float blue) {
  byte a = _BV(WGM11);
  byte b = _BV(WGM31);

  if (red > .0f) {
    OCR1B = (unsigned) (pow(65536.0, sqrt(red)) - 1.0);
    a |= _BV(COM1B1);
  }
  if (green > .0f) {
    OCR1A = (unsigned) (pow(65536.0, sqrt(green)) - 1.0);
    a |= _BV(COM1A1);
  }
  if (blue > .0f) {
    OCR3A = (unsigned) (pow(65536.0, sqrt(blue)) - 1.0);
    b |= _BV(COM3A1);
  }

  TCCR1A = a;
  TCCR3A = b;
}

