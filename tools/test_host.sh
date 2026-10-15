#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
test_binary=$(mktemp /tmp/bikenav-host-test.XXXXXX)
trap 'rm -f "$test_binary"' EXIT HUP INT TERM
"${CXX:-g++}" -std=c++17 -Wall -Wextra -Werror -g -pthread \
    -fsanitize=address,undefined -fno-omit-frame-pointer \
    -I main tests/firmware_host_test.cpp -o "$test_binary"
"$test_binary"
