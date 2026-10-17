#pragma once
#include <string.h>
#include <stdint.h>
#include <sys/socket.h>
#include <errno.h>

// Read only the relay preface, preserving any coalesced AAP bytes in the socket.
// Clock/yield/link predicates permit the production policy to be tested on host.
template<typename Clock, typename Yield, typename Valid>
static bool helper_read_greeting(int fd, int64_t timeout_us, Clock now, Yield yield, Valid valid) {
    const char expected[] = "BKNAVR1\n";
    char greeting[sizeof expected - 1];
    size_t received = 0;
    const int64_t deadline = now() + timeout_us;
    while (received < sizeof greeting && now() < deadline && valid()) {
        int n = recv(fd, greeting + received, sizeof greeting - received, MSG_DONTWAIT);
        if (n > 0) {
            if (memcmp(greeting + received, expected + received, n)) return false;
            received += n;
        } else if (n == 0 || (errno != EAGAIN && errno != EWOULDBLOCK && errno != EINTR)) {
            return false;
        }
        yield();
    }
    return received == sizeof greeting && valid();
}
