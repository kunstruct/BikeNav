#pragma once

struct video_crop { int x, y, w, h; };

// I420 needs even dimensions and chroma-aligned crop origins.
static inline bool video_make_crop(int w, int h, int requested_x, bool projected, video_crop *c) {
    if (w < 4 || h < 4 || (w & 1) || (h & 1)) return false;
    if (projected && w == 800 && h == 480) {
        int x = requested_x < 0 ? 0 : requested_x > 160 ? 160 : requested_x;
        *c = {x & ~1, 0, 640, 480};
    } else {
        int cw = w, ch = w * 3 / 4;
        if (ch > h) { ch = h; cw = h * 4 / 3; }
        cw &= ~1; ch &= ~1;
        *c = {((w - cw) / 2) & ~3, ((h - ch) / 2) & ~1, cw, ch};
    }
    return c->w > 0 && c->h > 0 && c->x + c->w <= w && c->y + c->h <= h;
}
