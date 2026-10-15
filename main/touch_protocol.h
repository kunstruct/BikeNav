// Wire decoding for Waveshare 2.8 V1 (CST328) and V2 (CST3530).
// CST328 layout follows Waveshare's ESP32-S3-Touch-LCD-2.8 V1 example.
#pragma once
#include <stdint.h>
#include <stddef.h>

struct touch_contact { bool pressed; int x, y; };

static inline bool touch_decode_cst328(const uint8_t *p, size_t n, unsigned count, touch_contact *c) {
    if (count > 5) return false;
    *c = {false, 0, 0};
    if (!count) return true;
    if (n < 5) return false;
    c->x = (p[1] << 4) | (p[3] >> 4);
    c->y = (p[2] << 4) | (p[3] & 15);
    c->pressed = true;
    return c->x < 240 && c->y < 320;
}

static inline bool touch_decode_cst3530(const uint8_t *p, size_t n, touch_contact *c) {
    if (n < 9) return false;
    // The controller returns an all-zero idle frame without a checksum.
    bool idle = true;
    for (size_t i = 0; i < 9; ++i) if (p[i]) idle = false;
    if (idle) { *c = {false, 0, 0}; return true; }
    unsigned sum = 0x55 + p[4] + p[5] + p[6] + p[7] + p[8];
    if ((unsigned)(p[0] | (p[1] << 8)) != sum) return false;
    unsigned ev = p[8] >> 4;
    unsigned count = p[3] & 15;
    if (ev > 2 || count > 5) return false;
    c->pressed = count > 0 && (ev == 1 || ev == 2);
    c->x = p[4] | ((p[7] & 15) << 8);
    c->y = p[5] | ((p[7] & 0xf0) << 4);
    return !c->pressed || (c->x < 240 && c->y < 320);
}

static inline void touch_rotate(int rx, int ry, int rot, int *x, int *y) {
    switch (rot & 3) {
    case 0: *x = rx; *y = ry; break;
    case 1: *x = ry; *y = 239 - rx; break;
    case 2: *x = 239 - rx; *y = 319 - ry; break;
    default: *x = 319 - ry; *y = rx; break;
    }
}
