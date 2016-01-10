void hsvToRgb(float *rgb, float h, float s, float v) {
  h /= 60.0;

  while (h < 0) {
    h += 6.0;
  }
  while (h >= 6.0) {
    h -= 6.0;
  }


  // not very elegant way of dealing with out of range: return black
  if (s < 0.0 || s > 1.0 || v < 0.0 || v > 1.0) {
    rgb[0] = 0;
    rgb[1] = 0;
    rgb[2] = 0;
  } else if ((h < 0.0) || (h > 6.0)) {
    rgb[0] = v;
    rgb[1] = v;
    rgb[2] = v;
  } else {
    float m, n, f;

    int i = floor(h);
    f = h - i;
    if (!(i & 1)) {
      f = 1 - f; // if i is even
    }
    m = v * (1 - s);
    n = v * (1 - s * f);
    switch (i) {
    case 6:
    case 0:
      rgb[0] = v; rgb[1] = n; rgb[2] = m;
      break;
    case 1:
      rgb[0] = n; rgb[1] = v; rgb[2] = m;
      break;
    case 2:
      rgb[0] = m; rgb[1] = v; rgb[2] = n;
      break;
    case 3:
      rgb[0] = m; rgb[1] = n; rgb[2] = v;
      break;
    case 4:
      rgb[0] = n; rgb[1] = m; rgb[2] = v;
      break;
    case 5:
      rgb[0] = v; rgb[1] = m; rgb[2] = n;
      break;
    }
  }
}

