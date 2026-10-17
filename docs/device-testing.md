# Bike firmware device checks

This checkout defaults to motorcycle identity and Board WiFi, with automatic V1/V2 touch
detection. Source builds and host tests do not establish that a phone accepts motorcycle
navigation or that physical touch works. Complete these checks while parked.

## Flash this checkout

Use the local `docs/index.html` Install button after regenerating the web firmware, or flash
`docs/flash/MiniNav.bin` at **0x0**. The upstream website may still provide the older image.
The image reports firmware version `1.1.1-bike`. Existing display preferences survive;
new Bike/Car and WiFi preferences are saved separately.

With ESP-IDF 5.4.2 active:

```sh
idf.py build
python3 tools/make_web_firmware.py
idf.py -p /dev/ttyACM0 flash monitor
```

Adjust the port for your computer. No automatic erase is needed.

## 1. Local touch, without a phone

1. Leave the phone disconnected. Boot logs should say `CST328 (V1)` or `CST3530 (V2)` found.
2. Tap Theme on the connection screen and check the colours change.
3. Test both landscape orientations by turning the board over.
4. Run the independent local test:

   ```sh
   python3 tools/serial_log.py 30 --port /dev/ttyACM0 --send 3:touchtest --send 25:touchstatus
   ```

   Tap corners and drag. The screen should show coordinates and logs should identify sensible
   raw/screen positions. This test suppresses Android Auto input and settings taps. `touchtest`
   again exits, or reboot. If no controller is found, firmware retries detection every 10 seconds.
   Save the logs if touch still fails; the exact board revision is useful.
5. Check BOOT still changes theme, double press toggles projected touch, and a one-second hold
   starts/stops the demo. Projected Touch can remain off while local settings touch works.

## 2. Bike navigation

1. Confirm **Bike** at the top; tap it if **Car** is selected, or send `bike` over serial.
2. Enable the Android Auto developer headunit server. Accept any first-use prompts on the phone.
3. Select a short familiar destination and check that motorcycle/two-wheeler routing is offered
   and the phone/board agree. This profile is for a motorcycle/scooter, not a bicycle.
4. If Maps still uses car routes, record the phone's Android/Maps/Android Auto versions and
   region. Do not assume the firmware label means Maps accepted the mode. Android Auto caches
   vehicle identity; remove an old MiniNav entry and reconnect if it appears to reuse old setup.
5. A Car fallback remains available from the connection screen if motorcycle mode fails setup.

## 3. Connection and phone internet

1. Default **Board WiFi**: keep the phone hotspot off, join `MiniNav`, and choose to stay
   connected despite no internet. Keep cellular data enabled. Start navigation and test search,
   traffic and rerouting, including with the phone locked.
2. Check the phone reconnects after board power cycles and the firmware recovers after WiFi is
   disabled/re-enabled. Some phones may still need the developer server restarted.
3. If cellular internet stops, choose **Phone AP** via the top-right label. This saves the mode
   and restarts the board. Enable the configured 2.4 GHz phone hotspot. In this mode the board's
   AP is off. **Auto WiFi** enables both; the next tap selects experimental **Helper WiFi**, then **Board WiFi**.
   Helper WiFi requires the matching Android APK; see [helper testing](helper-wifi.md).
4. `wifi` over serial cycles these same modes if screen input is unavailable.
5. Compare phone battery drain over equal navigation durations and similar conditions. The
   board-hosted mode removes phone tethering, but the phone still renders Android Auto video;
   no quantified battery improvement has been measured.

## 4. Recovery and endurance

- Interrupt WiFi during streaming; check the board returns to connection status and reconnects.
- Stop the headunit server during video, then restart it. Check any error gives a useful next step.
- Change Touch/Full screen and verify layout recovery without repeated watchdog reboots.
- Run navigation for at least 30 minutes with logs; review dropped frames, decode errors and
  free internal/PSRAM. A map that stops updating must not be treated as current guidance.
- Maps-view gestures remain a known compatibility limitation even if local touch passes.

## Compatibility boundary

This build still requires the Android Auto developer headunit server on an Android phone.
It has no iPhone/CarPlay implementation. Wired Android Auto via USB Android Open Accessory
could avoid the developer-server workflow, but that transport is not implemented here; the
USB serial test harness is not a phone-compatible wired Android Auto connection.

## Host checks

```sh
sh tools/test_host.sh
```

Tests exercise the actual production V1/V2 touch parsers, rotation/crop functions and bounded
socket writer, including stalled receivers, partial writes and disconnects, with ASan/UBSan.
The `tools/fake_phone.py` USB integration harness still requires an attached board and a
`CONFIG_AAM_SERIAL_TRANSPORT=y` build. It cannot establish real Maps motorcycle-route support.

## WiFi optimization checks (Pixel 8 Pro, Android 17)

The user's target phone is a Pixel 8 Pro on Android 17. No physical measurements have been
made for this phone. Begin with Board WiFi and the phone hotspot off; leave cellular data on.
After joining MiniNav, accept any prompt to stay connected without internet. Verify both the
projected map and a fresh online destination search with the phone locked/unlocked. The board
omits DHCP gateway/DNS advertisements but cannot dictate Android's default network selection.

Capture `net` or `status` over serial to see channel, peer address, link generation, RSSI,
retry count and last WiFi disconnect reason. Link generation should change when a phone
leaves/rejoins, even if DHCP assigns the same address, and when Auto switches interfaces.
The protocol should leave the stale connection and retry without waiting for the old 20-second
message timeout. Rejoining can still take time in the phone WiFi stack or developer server.

In Phone AP/Auto mode, leave the hotspot absent and watch retry delays grow through
2, 4, 8, 16, 32 and 60 seconds. While a phone uses Board WiFi, station scans must stop.
A stalled association/DHCP attempt is cancelled after 15 seconds; DHCP IP loss invalidates the
current session. Management sleeps until an event or retry deadline, rather than polling a
working connection. Backoff means a newly enabled hotspot may take up to 60 seconds to be
found after repeated failures; reboot for an immediate attempt if needed.

In Phone AP mode, `net` should report idle sleep on while waiting for the headunit server,
and off during TCP/TLS/video. This saves board power, not phone tethering power. Auto mode
keeps modem sleep off because both interfaces are active. Board WiFi uses 100-TU beacons and
DTIM 3, giving an idle client more time between broadcast wakeups. This does not reduce the
unicast video frame rate. If a phone has trouble with it, rebuild with AAM_AP_DTIM=1.

For battery comparison, use the same route, duration, phone-screen state, mobile signal,
traffic and brightness; record start/end charge and duration for both modes. Improvement in
phone battery drain is not established until measured. Keep a working fallback image/mode.
