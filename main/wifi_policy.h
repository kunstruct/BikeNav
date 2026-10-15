#pragma once
#include <stdint.h>
#include <string.h>

// Kept allocation-free; net.cpp protects this state with its endpoint lock.
struct wifi_endpoint {
    char ap[16] = {}, hotspot[16] = {}, current[16] = {};
    bool via_hotspot = false;
    uint32_t generation = 1;

    static void copy_ip(char *dst, const char *src) {
        size_t n = strlen(src);
        if (n > 15) n = 15;
        memcpy(dst, src, n); dst[n] = 0;
    }

    void refresh() {
        bool use_hotspot = hotspot[0] != 0;
        const char *ip = use_hotspot ? hotspot : ap;
        if (strcmp(current, ip) || via_hotspot != use_hotspot) {
            copy_ip(current, ip);
            via_hotspot = use_hotspot;
            ++generation;
        }
    }
    void set_ap(const char *ip) { copy_ip(ap, ip); refresh(); }
    void set_hotspot(const char *ip) { copy_ip(hotspot, ip); refresh(); }
};

struct wifi_retry {
    unsigned failures = 0;
    int64_t next_ms = 0;
    void failed(int64_t now_ms) {
        unsigned shift = failures < 5 ? failures : 5;
        unsigned delay_ms = 2000u << shift;
        if (delay_ms > 60000) delay_ms = 60000;
        if (failures < 6) ++failures;
        next_ms = now_ms + delay_ms;
    }
    void connected() { failures = 0; next_ms = 0; }
    bool due(int64_t now_ms, bool ap_busy) const { return !ap_busy && now_ms >= next_ms; }
};
