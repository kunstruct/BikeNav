#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
relay_test_dir=$(mktemp -d /tmp/bikenav-relay-test.XXXXXX)
trap 'rm -rf "$relay_test_dir"' EXIT HUP INT TERM
javac -d "$relay_test_dir" android-helper/app/src/main/java/com/kunstruct/bikenavhelper/RelayPipe.java android-helper/app/src/test/java/com/kunstruct/bikenavhelper/RelayPipeTest.java
java -cp "$relay_test_dir" com.kunstruct.bikenavhelper.RelayPipeTest
