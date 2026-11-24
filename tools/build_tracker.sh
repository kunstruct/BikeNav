#!/bin/sh
set -eu
repo=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
tracker_tools="$HOME/.local/share/ride-tracker-tools"
if [ -z "${JAVA_HOME:-}" ] && [ -d "$tracker_tools" ]; then
    for tracker_jdk in "$tracker_tools"/jdk-17*; do
        if [ -x "$tracker_jdk/bin/java" ]; then JAVA_HOME="$tracker_jdk"; export JAVA_HOME; break; fi
    done
fi
if [ -z "${ANDROID_HOME:-}" ] && [ -d "$tracker_tools/sdk" ]; then
    ANDROID_HOME="$tracker_tools/sdk"; export ANDROID_HOME
fi
cd "$repo/android-tracker"
./gradlew --no-daemon assembleDebug lintDebug "$@"
mkdir -p "$repo/release-artifacts"
cp app/build/outputs/apk/debug/app-debug.apk "$repo/release-artifacts/RideTracker.apk"
sha256sum "$repo/release-artifacts/RideTracker.apk"
