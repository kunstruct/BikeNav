#include <assert.h>
#include <chrono>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <thread>
#include <unistd.h>
#include <vector>
#include "touch_protocol.h"
#include "video_geometry.h"
#include "socket_write.h"
#include "wifi_policy.h"
#include "helper_transport.h"

static int64_t now_us() {
    return std::chrono::duration_cast<std::chrono::microseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();
}

static void test_touch() {
    touch_contact c;
    const uint8_t v1[] = {0x06, 0x0e, 0x13, 0xff, 0x20};
    assert(touch_decode_cst328(v1, sizeof v1, 1, &c));
    assert(c.pressed && c.x == 239 && c.y == 319);
    assert(touch_decode_cst328(nullptr, 0, 0, &c) && !c.pressed);
    assert(!touch_decode_cst328(v1, 4, 1, &c));
    assert(!touch_decode_cst328(v1, sizeof v1, 6, &c));
    uint8_t v2[] = {0xa3, 0x01, 0, 1, 0xef, 0x3f, 0, 0x10, 0x10};
    assert(touch_decode_cst3530(v2, sizeof v2, &c));
    assert(c.pressed && c.x == 239 && c.y == 319);
    v2[0] ^= 1;
    assert(!touch_decode_cst3530(v2, sizeof v2, &c));
    assert(!touch_decode_cst3530(v2, 8, &c));
    memset(v2, 0, sizeof v2);
    assert(touch_decode_cst3530(v2, sizeof v2, &c) && !c.pressed);
    const uint8_t outside[] = {0x06, 0x0f, 0x13, 0x0f, 0x20}; // x=240
    assert(!touch_decode_cst328(outside, sizeof outside, 1, &c));
    for (int rot = 0; rot < 4; ++rot) {
        for (int x : {0, 239}) for (int y : {0, 319}) {
            int sx, sy;
            touch_rotate(x, y, rot, &sx, &sy);
            assert(sx >= 0 && sx < ((rot & 1) ? 320 : 240));
            assert(sy >= 0 && sy < ((rot & 1) ? 240 : 320));
        }
    }
    int x, y;
    touch_rotate(239, 319, 1, &x, &y); assert(x == 319 && y == 0);
    touch_rotate(239, 319, 3, &x, &y); assert(x == 0 && y == 239);
}

static void test_crop() {
    video_crop c;
    assert(video_make_crop(800, 480, 200, true, &c));
    assert(c.x == 160 && c.x + c.w == 800); // old margin=400 over-read
    for (int x = -400; x <= 800; ++x) {
        assert(video_make_crop(800, 480, x, true, &c));
        assert(c.x >= 0 && c.x + c.w <= 800 && !(c.x & 1));
    }
    for (int w = 4; w <= 1024; w += 2) for (int h : {4, 240, 480, 720}) {
        assert(video_make_crop(w, h, 0, false, &c));
        assert(c.x >= 0 && c.y >= 0 && c.x + c.w <= w && c.y + c.h <= h);
        assert(!((c.x | c.y | c.w | c.h) & 1));
    }
    assert(!video_make_crop(0, 480, 0, true, &c));
    assert(!video_make_crop(801, 480, 0, true, &c));
}

static void test_socket() {
    signal(SIGPIPE, SIG_IGN);
    int fd[2]; assert(socketpair(AF_UNIX, SOCK_STREAM, 0, fd) == 0);
    auto yield = [] { usleep(1000); };
    int error;
    const uint8_t small[] = {1, 2, 3, 4};
    assert(socket_write_all(fd[0], small, sizeof small, 100000, now_us, yield, &error));
    uint8_t got[4]; assert(recv(fd[1], got, sizeof got, MSG_WAITALL) == 4);
    assert(!memcmp(got, small, sizeof got));
    int size = 1024; assert(setsockopt(fd[0], SOL_SOCKET, SO_SNDBUF, &size, sizeof size) == 0);
    std::vector<uint8_t> large(1024 * 1024, 42);
    int64_t start = now_us();
    assert(!socket_write_all(fd[0], large.data(), large.size(), 30000, now_us, yield, &error));
    assert(error == ETIMEDOUT && now_us() - start < 1000000);
    close(fd[1]);
    assert(!socket_write_all(fd[0], small, sizeof small, 100000, now_us, yield, &error));
    assert(error != ETIMEDOUT);
    close(fd[0]);

    assert(socketpair(AF_UNIX, SOCK_STREAM, 0, fd) == 0);
    assert(setsockopt(fd[0], SOL_SOCKET, SO_SNDBUF, &size, sizeof size) == 0);
    std::vector<uint8_t> received(large.size());
    std::thread reader([&] {
        size_t n = 0;
        while (n < received.size()) {
            int r = recv(fd[1], received.data() + n, received.size() - n, 0);
            assert(r > 0); n += r;
        }
    });
    assert(socket_write_all(fd[0], large.data(), large.size(), 2000000, now_us, yield, &error));
    reader.join(); assert(received == large);
    close(fd[0]); close(fd[1]);
}

static void test_wifi_policy() {
    wifi_endpoint e;
    assert(!e.current[0]);
    uint32_t initial = e.generation;
    e.set_ap("192.168.4.2");
    assert(!strcmp(e.current, "192.168.4.2") && e.generation != initial && !e.via_hotspot);
    uint32_t ap_session = e.generation;
    e.set_ap("192.168.4.2"); assert(e.generation == ap_session);
    e.set_hotspot("192.168.4.2"); // same address on a different interface is a new link
    assert(e.via_hotspot && e.generation != ap_session);
    uint32_t sta_session = e.generation;
    e.set_ap(""); assert(e.generation == sta_session); // unrelated AP loss must not kill STA
    e.set_ap("192.168.4.3"); assert(e.generation == sta_session);
    e.set_hotspot("");
    assert(!e.via_hotspot && !strcmp(e.current, "192.168.4.3"));
    uint32_t before_loss = e.generation;
    e.set_ap(""); e.set_ap("192.168.4.3");
    assert(e.generation != before_loss); // same-IP reconnect must invalidate the old socket

    wifi_retry retry;
    int64_t now = 0;
    assert(retry.due(now, false));
    for (int delay : {2000, 4000, 8000, 16000, 32000, 60000, 60000}) {
        retry.failed(now);
        assert(retry.next_ms == now + delay);
        assert(!retry.due(retry.next_ms - 1, false));
        assert(!retry.due(retry.next_ms, true)); // never scan over an AP client
        assert(retry.due(retry.next_ms, false));
        now = retry.next_ms;
    }
    retry.connected();
    assert(retry.failures == 0 && retry.due(now, false));
    retry.failed(now); assert(retry.next_ms == now + 2000);
}


static void test_helper_greeting() {
    int fd[2];
    auto yield = [] { usleep(1000); };
    auto valid = [] { return true; };
    assert(socketpair(AF_UNIX, SOCK_STREAM, 0, fd) == 0);
    std::thread sender([&] {
        const char preface[] = "BKNAVR1\n";
        for (size_t i = 0; i < sizeof preface - 1; i++) {
            assert(send(fd[1], preface + i, 1, 0) == 1);
            usleep(1000);
        }
        const uint8_t frame[] = {0, 3, 0, 4, 0, 1, 0, 1};
        assert(send(fd[1], frame, sizeof frame, 0) == sizeof frame);
    });
    assert(helper_read_greeting(fd[0], 1000000, now_us, yield, valid));
    sender.join();
    uint8_t frame[8]; assert(recv(fd[0], frame, sizeof frame, MSG_WAITALL) == 8);
    assert(frame[1] == 3 && frame[7] == 1);
    close(fd[0]); close(fd[1]);

    assert(socketpair(AF_UNIX, SOCK_STREAM, 0, fd) == 0);
    const char coalesced[] = "BKNAVR1\nAAP";
    assert(send(fd[1], coalesced, sizeof coalesced - 1, 0) == sizeof coalesced - 1);
    assert(helper_read_greeting(fd[0], 100000, now_us, yield, valid));
    char tail[3]; assert(recv(fd[0], tail, sizeof tail, 0) == 3 && !memcmp(tail, "AAP", 3));
    close(fd[0]); close(fd[1]);

    assert(socketpair(AF_UNIX, SOCK_STREAM, 0, fd) == 0);
    int64_t start = now_us();
    assert(!helper_read_greeting(fd[0], 20000, now_us, yield, valid));
    assert(now_us() - start < 1000000);
    assert(send(fd[1], "BAD", 3, 0) == 3);
    assert(!helper_read_greeting(fd[0], 100000, now_us, yield, valid));
    close(fd[0]); close(fd[1]);

    assert(socketpair(AF_UNIX, SOCK_STREAM, 0, fd) == 0);
    assert(!helper_read_greeting(fd[0], 1000000, now_us, yield, [] { return false; }));
    close(fd[1]);
    assert(!helper_read_greeting(fd[0], 1000000, now_us, yield, valid));
    close(fd[0]);
}

int main() {
    test_touch(); test_crop(); test_socket(); test_wifi_policy(); test_helper_greeting();
    puts("PASS: touch, rotation, crop bounds, socket deadlines, WiFi endpoint/retry policy, helper greeting/timeout/link loss");
}
