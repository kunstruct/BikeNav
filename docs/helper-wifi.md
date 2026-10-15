# Experimental Helper WiFi release

Firmware **1.2.0-helper-alpha1**, Android helper **0.1.0-alpha1**.

This experiment keeps the ESP32 as the Wi-Fi access point and uses an Android app to request a local-only connection. It aims to preserve cellular internet without running the phone hotspot. **The user has reported a successful proof of concept on the Pixel 8 Pro / Android 17 setup. Battery savings and extended screen-off reliability remain unmeasured.** The existing Phone AP mode remains the tested fallback.

## Successful device proof of concept — 2026-10-16

After installing the matched firmware and APK, the user reported: “The proof of concept works brilliantly.” This records user-reported functional success of the Helper WiFi approach: Android Auto over board-hosted Wi-Fi with cellular internet retained, without running the phone hotspot.

Test setup: Pixel 8 Pro on Android 17 (previously recorded device details), Waveshare ESP32-S3 2.8-inch board, firmware **1.2.0-helper-alpha1**, helper **0.1.0-alpha1**. This is feedback from the user's device test, not an independently observed lab result. No debug report, test duration or battery measurements accompanied the confirmation.

The result supports continuing development of the local-only Wi-Fi relay. Remaining checks are battery consumption against Phone AP, prolonged screen-off operation, reconnection after interruptions and compatibility with other phones. Keep this release marked experimental until those checks are completed.

## Install and connect (no ADB)

1. Download `MiniNav.bin` and `BikeNav-Helper-0.1.0-alpha1.apk` from the [matching prerelease](https://github.com/kunstruct/BikeNav/releases/tag/v1.2.0-helper-alpha1).
2. Flash the merged `MiniNav.bin` at **0x0**, then reset the board. The firmware preserves saved preferences; flashing does not automatically select the new mode.
3. Install the APK on Android 10 or newer. Allow installation from the browser/file manager when Android prompts. This is a signed release APK, not a debug build.
4. Turn the phone hotspot off and keep mobile data enabled. In Android Wi-Fi settings, **forget the saved MiniNav network**. Let the helper request the connection instead of manually joining it.
5. On BikeNav's connection screen, tap the top-right Wi-Fi label to cycle **Board WiFi → Phone AP → Auto WiFi → Helper WiFi**. Each change saves and restarts the board. Select **Helper WiFi**.
6. Start Android Auto's developer **headunit server** as before. The helper cannot start or replace it. If a previous session stalled, stop and restart the server first.
7. Open BikeNav Helper and tap **Connect**. Grant Nearby Wi-Fi Devices and notification permission, and accept Android's Wi-Fi connection dialog. Android 10–12 requires precise location permission and Location enabled for this API; the helper does not collect location.
8. Defaults are Wi-Fi name `MiniNav`, password `mininav123`, and board address `192.168.4.1`. Change the app fields if your firmware uses different settings.
9. Check the display starts Android Auto. Open a **fresh** webpage or search for an uncached destination to verify cellular internet. The app's `Default network` line should normally show `cellular=true` and `validated=true`; that line alone does not prove another app's requests work.

The app runs a visible connection service while in use. You can lock the phone after connection. Tap **Disconnect** in the app or notification when finished. After an ended/failed Android Auto session, **Disconnect, stop/start the headunit server, then Connect**. The app deliberately does not repeatedly reopen the Android Auto server, which can strand sessions.

If Android does not show the connection dialog, check permissions and Wi-Fi, forget the saved board network, and tap Disconnect then Connect. If a VPN is active, record that in your report and compare with it temporarily disconnected. Do not change multiple settings between tests.

## Diagnostics without ADB

The screen shows:

- Default network transport and Android's internet validation status.
- Local Wi-Fi interface, addresses, routes and DNS.
- Bytes relayed in each direction and a bounded event log.
- Battery percentage, temperature and charging state at connection and report time.

Tap **Share debug report** to export a text attachment. Review its preview before sharing: it contains local network addresses and the phone model, but no Wi-Fi password or Android Auto payload. Reports are kept in the app's private cache until replaced/cleared; nothing is uploaded automatically. The APK does not contact a cloud server or collect analytics.

For useful comparison, run 20–30 minutes of the same route with similar brightness, phone screen off, same mobile coverage and no charging. Compare Phone AP against Helper WiFi and note the start/end battery levels and temperatures. Removing tethering does not remove Android Auto rendering/encoding, cellular or Wi-Fi costs; the relay itself also consumes CPU. The session keeps a partial CPU wake lock only while relaying (released on EOF/error/disconnect, with a six-hour safety timeout); it does not keep the screen on or request a high-performance Wi-Fi lock.

## Architecture and limits

```text
Android Auto developer server 127.0.0.1:5277
                  ↕ byte-transparent TCP
             Android helper
                  ↕ socket bound to local-only Wi-Fi
           ESP32 listener :5278
                  ↕ existing AAP/TLS/video implementation
                display

Other phone apps → Android-selected internet network (intended: cellular)
```

The helper sends an eight-byte version preface `BKNAVR1\n` to the board, then relays bytes unchanged in both directions using bounded 32 KiB buffers. It never terminates Android Auto TLS or decodes/re-encodes video. The firmware accepts connections only in Helper WiFi mode from its current AP client's DHCP address, validates the preface with a five-second deadline, and uses existing endpoint-generation checks to abandon stale sessions. The AP admits one phone and uses the existing WPA2 credentials. The preface identifies the protocol; it is **not an authentication secret**. Configure a private AP password for use around other people.

Only the helper's board socket is bound to Wi-Fi. The app does **not** bind its entire process or Android Auto's process to Wi-Fi and does not implement a VPN. Android still controls cellular routing. The successful user proof of concept supports phone-local access to Android Auto's headunit server on this setup; access on other phone/software combinations remains unverified.

The helper targets SDK 35 and uses the nearby-Wi-Fi permission on Android 13+. The user reports the proof of concept works on the recorded Pixel / Android 17 setup; broader Android 17 compatibility is unverified. Builds have no native Android libraries and are architecture independent. No Play Store publication is part of this release.

## Build and validation

Android: JDK 17, Android SDK Platform 35, build tools 35.0.0, Gradle 8.9 (wrapper with pinned distribution checksum), Android Gradle Plugin 8.7.3. No application library dependencies.

```sh
cd android-helper
./gradlew assembleDebug lintDebug
```

To produce a signed release, set `BIKENAV_KEYSTORE`, `BIKENAV_STORE_PASSWORD`, and `BIKENAV_KEY_PASSWORD` in your environment. The keystore alias must be `bikenav-helper`, then run `./gradlew assembleRelease lintRelease`. Keep the keystore outside the repository and back it up: later APK updates require the same signing key. Never commit signing credentials. Release signing material for the development machine is stored privately under `~/.local/share/bikenav/signing/`.

```sh
sh tools/test_android_relay.sh   # JDK on PATH
sh tools/test_host.sh           # g++, ASan/UBSan
idf.py build                   # ESP-IDF 5.4.2 environment
python tools/make_web_firmware.py
```

Host tests exercise real TCP sockets with simultaneous binary traffic, fragmented writes, EOF, board disconnect and cancellation while a writer is blocked. Firmware tests cover fragmented/coalesced prefaces, preservation of AAP bytes, invalid greetings, deadline expiry and link loss, plus existing touch/video/socket/Wi-Fi policy tests. Build/lint/host tests do not substitute for phone and hardware integration tests.

References: [Android Wi-Fi request API](https://developer.android.com/develop/connectivity/wifi/wifi-bootstrap), [network-specific sockets](https://developer.android.com/reference/android/net/Network), [foreground connected-device services](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device).
