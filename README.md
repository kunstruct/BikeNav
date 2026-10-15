# MiniNav

A tiny Android Auto screen for your bike or car. MiniNav runs on the Waveshare ESP32-S3 2.8"
touch display, connects to your phone over WiFi and shows Google Maps from Android Auto.
No companion app on the phone, no Bluetooth, no extra hardware. This firmware requires an Android
phone with its Android Auto developer headunit server enabled; iPhone/CarPlay is not supported.

**Step-by-step guide with diagrams: https://mininavdevice.github.io/MiniNav/**
(hardware, one-click flashing, phone setup, connecting, riding, printing the case, troubleshooting).
Building from source has its own page: [docs/build-from-source.html](docs/build-from-source.html).

## How it works

Android Auto runs on the phone: the phone draws the Maps screen and streams it as video to a car's
screen (the head unit), which only displays it. MiniNav plays the part of that car screen. Android
Auto has a hidden developer option, **Start headunit server**, that lets a head unit connect to the
phone over WiFi (TCP 5277); MiniNav connects, completes the Android Auto handshake (TLS), receives the
H.264 map video and decodes it on the ESP32-S3.

- Hosts its own WiFi by default, so the phone hotspot can stay off. Phone-hotspot and automatic modes remain available.
- Motorcycle profile by default, with a saved Bike/Car switch. Two-wheeler routes depend on the phone, Maps and region.
- Detects both Waveshare touch revisions: V1 CST328 and V2 CST3530.
- Day/night themes follow Android Auto's night mode.
- Optional full screen: hides Android Auto's app bar so the map fills the display.
- Optional touch: taps go to Android Auto (launcher, app lists).
- Built-in demo drive that works without a phone.
- Flips the picture when the board is turned over.

## Hardware

| Item | Notes |
|---|---|
| Waveshare ESP32-S3-Touch-LCD-2.8 | ESP32-S3R8, 16 MB flash, 8 MB PSRAM, 320x240 ST7789 LCD, CST328 (V1) or CST3530 (V2) touch, QMI8658 IMU. [Buy on Robu.in](https://robu.in/product/waveshare-esp32-s3-2-8inch-capacitive-touch-display-development-board-240x320-ips-5-point-touch-32-bit-lx7-dual-core-processor-supports-wifi-bluetooth-esp32-with-display/) |
| USB-C data cable | Flashing and power |
| Android phone | Android Auto installed |
| 3D-printed case | Optional: front frame with BOOT button + back panel with strap slits, in `docs/case/`. No printer? Printing services such as [Robu.in](https://robu.in/product/3d-printing-service1/) print it for about ₹100-200 |

## Flash (easy way)

**For this bike firmware, open `docs/index.html` from this checkout and use its local Install
button. The upstream website may still serve the older firmware.** Read the
[device checklist](docs/device-testing.md) for touch, motorcycle routing and connection checks.

Open the local `docs/index.html` in Chrome or Edge, plug the board in and click **Install MiniNav**. Nothing to install. The firmware file is
`docs/flash/MiniNav.bin` (one image for address **0x0**, not the 0x1000 some flashers default to) if you prefer another flasher.

## Build from source

Only needed to change WiFi names and passwords or other options.

Needs [ESP-IDF v5.4.2](https://docs.espressif.com/projects/esp-idf/en/v5.4.2/esp32s3/get-started/).

```sh
git clone https://github.com/MiniNavDevice/MiniNav.git
cd MiniNav
tools/fetch_lovyangfx.sh            # display library, once
source ~/esp/esp-idf/export.sh
idf.py build
idf.py -p <port> -b 921600 flash    # e.g. /dev/cu.usbmodem101, /dev/ttyACM0, COM5
```

If flashing can't connect: hold BOOT, tap RESET, release BOOT, then flash again.

After changing the code, `python3 tools/make_web_firmware.py` refreshes the files the guide's Install
button flashes (`docs/flash/MiniNav.bin` and `MiniNav.bin.js`).

## Phone setup (once)

1. Android Auto settings: tap **Version** 10 times to unlock developer settings.
2. Three-dot menu → **Start headunit server**.
3. Android Auto battery usage → **Unrestricted**.
4. Leave the phone hotspot off. Join the board's WiFi **MiniNav**, password **mininav123**.
   Choose to stay connected when Android reports no internet; keep mobile data enabled.
5. On the connection screen, check **Bike** at the top. Tap it to switch Bike/Car; this is saved.
   The board announces a separate Android Auto identity for each profile. On first connection,
   accept the phone's setup prompts and select two-wheeler directions if offered.

Some phones do not keep cellular internet while joined to the board. If Maps cannot search or
reroute online, tap **Board WiFi** at the top right to choose **Phone AP** (the board restarts).
Then enable a 2.4 GHz hotspot named **MiniNavHotspot**, password **mininav123**. Tap again for
**Auto WiFi** (both modes), and again to return to **Board WiFi**. The choice is saved.
This changes who hosts WiFi; Android Auto still uses WiFi and the developer headunit server.

Change names and passwords with `idf.py menuconfig` → *MiniNav (Android Auto minimap)*.

## Controls

| Do | Result |
|---|---|
| Tap Bike / Car at the top of the connection screen | Switch vehicle profile (saved) |
| Tap the WiFi label at the top right | Cycle Board WiFi / Phone AP / Auto WiFi; save and restart |
| Tap Touch / Full screen / Theme on the connection screen | Change settings (saved) |
| Press BOOT | Day / night theme |
| Double-press BOOT | Touch on / off |
| Hold BOOT 1 s | Demo drive on / off |

## Options (`idf.py menuconfig`)

| Option | Default |
|---|---|
| Access point name / password / channel | MiniNav / mininav123 / 6 |
| Phone hotspot name / password | MiniNavHotspot / mininav123 |
| Video density (bigger labels when higher) | 200 dpi |
| Hide Android Auto's app bar | on |
| Right-hand drive | on |
| Board AP DTIM | 3 (use 1 for compatibility if necessary) |
| Phone AP idle board power saving | on; off during TCP/TLS/video |
| Motorcycle profile on first boot | on (saved Bike/Car choice overrides it) |

## WiFi behavior

Board WiFi remains the default: the phone hotspot can stay off. Only the selected network
interfaces are created. Phone AP/Auto retries back off from 2 to 60 seconds when absent;
station scans pause while a phone uses the board AP. A working link does not cause periodic
WiFi-management wakeups. Link/IP changes invalidate stale Android Auto sockets, including
same-IP reconnects. `net` prints signal, channel, retry and disconnect diagnostics.

Phone AP mode permits board modem sleep while idle and disables it before TCP/TLS/video.
The board AP uses normal 100-TU beacons with DTIM 3 (configurable). These are idle-power and
recovery optimizations; streaming rate is unchanged and phone battery savings are unmeasured.
Pixel 8 Pro / Android 17 is the current device test target; cellular coexistence needs testing.

## Testing and diagnostics

`sh tools/test_host.sh` runs touch-decoding, rotation, crop-boundary and socket deadline tests
with AddressSanitizer and UndefinedBehaviorSanitizer on a Linux host with `g++`.
See [the device test checklist](docs/device-testing.md) before a ride. The updated firmware
must be flashed before these controls and fixes are available.

Serial commands: `net`, `status`, `touchstatus`, `touchtest`, `bike`, `wifi`, `touch`, `full`, `night`,
`demo`, `shot`. `touchtest` shows/logs local coordinates without forwarding taps to Android Auto;
run it again to exit. This separates a controller problem from Maps ignoring projected input.

## Known issues

- Maps-view touch remains an Android Auto compatibility limitation; local touch fixes do not
  guarantee map gestures. Both board revisions are detected; confirm your actual board with `touchstatus`.
- Motorcycle identity requests bike navigation but cannot guarantee availability in your Maps version/region.
- Board WiFi avoids running the phone hotspot; mobile-data coexistence and battery savings still need
  testing on your phone.
- If the screen sticks at "Version", restart the phone's headunit server
  (three-dot menu → Stop, then Start headunit server).

## Code layout

```
main/app_main.cpp   start-up, inputs, serial console
main/board.cpp      display, connection screen, BOOT button, touch, IMU
main/net.cpp        WiFi (access point + phone hotspot)
main/aap.cpp        Android Auto protocol: framing, TLS, channels, input
main/video.cpp      H.264 decoder, crop and scale to the LCD
main/demo.cpp       built-in demo clip (main/demo.h264)
tools/              helper scripts (display library fetch, dev tools)
docs/               the guide website (GitHub Pages): web flasher (flash/), case STLs (case/)
```

## Credits and license

- Android Auto protocol details follow Open Headunit;
  `main/aap_cert.h` is its published reference head-unit certificate.
- Display: [LovyanGFX](https://github.com/lovyan03/LovyanGFX). Decoder: Espressif `esp_h264`.
- UI font: Nunito (SIL Open Font License, `tools/fonts/OFL.txt`).
- MiniNav is licensed under the GNU AGPL-3.0 (`LICENSE`).

Not affiliated with Google. Android Auto is a trademark of Google LLC.

## Experimental Wi-Fi helper

The `fix/wifi-cellular-coexistence` branch adds **Helper WiFi** mode and a signed Android helper APK in the [1.2.0-helper-alpha1 prerelease](https://github.com/kunstruct/BikeNav/releases/tag/v1.2.0-helper-alpha1). This experiments with board-hosted local-only Wi-Fi while retaining cellular internet. The user reported a successful proof of concept on the Pixel 8 Pro / Android 17 setup on 2026-10-16. Battery savings, endurance and other-phone compatibility remain unmeasured. See [installation, diagnostics and build instructions](docs/helper-wifi.md).
