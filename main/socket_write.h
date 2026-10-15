#pragma once
#include <stdint.h>
#include <stddef.h>
#include <errno.h>
#include <sys/socket.h>

// Nonblocking writes with one overall deadline, including partial progress.
// Injecting clock/yield keeps the same policy usable on FreeRTOS and host tests.
template<typename Clock, typename Yield>
static bool socket_write_all(int fd, const uint8_t *p, size_t n, int64_t timeout_us,
                             Clock now, Yield yield, int *error) {
    const int64_t deadline = now() + timeout_us;
    *error = 0;
    while (n) {
        if (now() >= deadline) { *error = ETIMEDOUT; return false; }
        int r = send(fd, p, n, MSG_DONTWAIT);
        if (r < 0) {
            if (errno == EINTR) continue;
            if (errno == EAGAIN || errno == EWOULDBLOCK) { yield(); continue; }
            *error = errno; return false;
        }
        if (!r) { *error = EPIPE; return false; }
        p += r; n -= r;
    }
    return true;
}
