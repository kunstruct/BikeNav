# Android Auto startup without the developer server

Research recorded 2026-10-16 on `research/android-auto-startup`, branched from `7dd7277` after [PR #1](https://github.com/kunstruct/BikeNav/pull/1) was opened against main. The full implementation and successful user test are documented in [the development record](../helper-development-process.md). This branch contains research, not a replacement APK or firmware.

## Decision so far

The helper solves the phone's local Wi-Fi/cellular routing problem, but still depends on Android Auto's developer server at `127.0.0.1:5277`. Removing that prerequisite is a separate startup/protocol problem. A settings shortcut can reduce navigation; it cannot by itself start projection.

There is no verified, supported, ordinary-app API in the sources reviewed that starts Google's developer server. Do not promise silent startup on an unrooted phone. Native wireless Android Auto is the strongest direction for removing the developer workflow, but needs a hardware/protocol feasibility prototype before we can recommend a board change.

## Requirements and current evidence

- Wireless projection; phone cellular internet remains usable.
- No phone hotspot as the daily solution; its drain was unacceptable to the user.
- No ADB requirement for installation or everyday operation.
- Prefer existing ESP32-S3 hardware, and make any hardware dependency explicit.
- Avoid permanent background polling and measure battery before claiming improvements.
- User confirmed the helper proof of concept works brilliantly. We have no endurance, reconnect, startup-after-reboot or measured battery result yet.

The user's recorded Android **OS** version is 17. That does not establish the installed Android **Auto app** version. Version-specific startup claims below concern the app, not the operating system. We have not inspected the installed Google APK or performed reverse engineering in this investigation.

## What the primary sources establish

### Google's documented developer path

[Google's DHU guide](https://developer.android.com/training/cars/testing/dhu) separates USB Accessory Mode from the developer-server/ADB path. The latter explicitly requires manually starting the server and uses TCP 5277. USB AOA can avoid that server, but adds a cable and USB host integration; it does not meet the preferred wireless experience.

[Google's consumer setup guide](https://support.google.com/androidauto/answer/6348029) describes Bluetooth pairing for first-time wireless setup and automatic subsequent connection. This establishes that the developer server is not a requirement of normal wireless Android Auto; it does not document a third-party app startup API or certify our ESP implementation.

### Existing open-source native startup

Source snapshot reviewed: Open Headunit commit **d2d0a6bf71217e8658545314de63ed87c0031451**. This is an Android receiver implementation, not code that can be dropped into ESP-IDF.

- [NativeAaHandshakeManager.kt](https://github.com/andreknieriem/open-headunit/blob/d2d0a6bf71217e8658545314de63ed87c0031451/app/src/main/java/com/andrerinas/openheadunit/connection/wifi/modes/nativeaa/NativeAaHandshakeManager.kt) publishes RFCOMM services, uses Android Auto service UUID `4de17a00-52cb-11e6-bdf4-0800200c9a66`, and sends Wi-Fi startup/credential messages. It also has hands-free-related handling. A generic BLE advertisement is insufficient for this implementation.
- [wireless.proto](https://github.com/andreknieriem/open-headunit/blob/d2d0a6bf71217e8658545314de63ed87c0031451/app/src/main/proto/wireless.proto) describes endpoint and AP-credential exchange before projection, and contains 2.4 GHz channel capability values. This makes testing our AP worthwhile; schema support alone does not prove Pixel compatibility.
- [SelfLauncherBroadcast.kt](https://github.com/andreknieriem/open-headunit/blob/d2d0a6bf71217e8658545314de63ed87c0031451/app/src/main/java/com/andrerinas/openheadunit/connection/self/launchers/SelfLauncherBroadcast.kt) targets Google's private wireless startup receiver with a loopback endpoint. This starts a different path from the developer server.
- The project's [README](https://github.com/andreknieriem/open-headunit/blob/d2d0a6bf71217e8658545314de63ed87c0031451/README.md) reports legacy startup trouble on Android Auto 17.4+, with a rooted workaround. Treat this as upstream experience, not a confirmed result on our phone.

The source is AGPL-3.0. Consult its license before incorporating code; this research adds no upstream code to BikeNav.

### ESP32-S3 hardware constraint

[Espressif's Bluetooth documentation](https://docs.espressif.com/projects/esp-idf/en/v5.3.1/esp32s3/api-guides/bluetooth.html) states that S3 supports Bluetooth LE, while Bluetooth Classic is unsupported. The RFCOMM route above therefore cannot run on the existing S3 radio. Adding an external Classic-capable controller or a gateway is a possible investigation, not a proven fix: service discovery, pairing, profile behavior, Wi-Fi handoff and projection integration still need validation.

## Options against our requirements

| Option | Removes manual server start? | Current hardware / no root / no ADB | Assessment |
| --- | --- | --- | --- |
| Better AA settings launch and guided connection | No | Yes, subject to resolving an exported settings activity on the phone | Small, useful UX improvement; still a developer workflow |
| Private wireless-startup broadcast from helper | Possibly on compatible AA versions | Potentially; unverified on this phone | Worth a small isolated compatibility experiment, never assume it works on recent AA |
| Directly start Google's internal server service | Unverified | No supported path found | Do not build product reliability around private components |
| Accessibility tap automation | Could perform the taps | Requires another special permission and stable UI | Brittle across Google UI changes; poor daily-use foundation |
| Root or shell-assisted automation | May enable private paths | Violates normal no-root/no-ADB deployment goal | Optional research only, not default |
| Native Bluetooth/Wi-Fi handshake | Yes in existing receiver implementations | S3 lacks required Classic radio | Best long-term investigation; needs additional hardware/protocol work |
| USB AOA directly from phone | Yes | Requires USB host integration/cable | Technical fallback, outside preferred wireless use |
| Linux/USB wireless AA dongle gateway | Moves startup to gateway | Extra hardware/power and bridge integration | Reference design, not a direct plug-in replacement for this board |
| BLE trigger or cloud tunnel | No demonstrated way | Does not supply missing AA startup capability | Does not solve this requirement |

Reference gateway: [WirelessAndroidAutoDongle](https://github.com/nisargjhaveri/WirelessAndroidAutoDongle) targets a Raspberry Pi between a wireless phone and an existing wired AA head unit. BikeNav would still need the relevant USB host/transport bridge; purchasing a dongle alone is not an established solution.

## Proposed next work, in order

1. **Make the working helper easier to operate.** Resolve an available AA settings entry point with fallbacks, explain server state clearly, and provide a notification/launcher connection action. Current `MainActivity.java` uses `com.google.android.gms.car.settings` and otherwise opens general settings. Verify on the actual phone before saying the hidden-page problem is fixed. Do not start a repeated TCP probe merely to show readiness.
2. **Run a bounded software startup experiment.** Obtain the AA app version from its app-info/settings UI; ADB is unnecessary. Put a private-receiver startup attempt behind an explicit experimental action, retaining the working developer-server flow. Success means projection actually starts after a phone restart with the server stopped, not merely that `sendBroadcast` returns. Stop investing if the installed version blocks the receiver. Do not require a downgrade as the product solution.
3. **Prototype native wireless on a capable reference receiver.** Prove cold pairing and reconnect with this phone, then test credentials/endpoint handoff using a board-hosted local AP. Check cellular internet during projection. A laptop or spare Classic-capable receiver can establish feasibility before ordering hardware; it is a development rig, not the daily-use device.
4. **Choose hardware only after that result.** Evaluate an external Classic-capable coprocessor versus a different receiver platform. Verify SDP/RFCOMM support, profile dependencies, Wi-Fi channel behavior, power budget and integration effort. A second ESP32 is not automatically a complete AA startup solution.
5. **Validate daily-use behavior.** Cold phone/board boot, screen locked, board power interruption, leaving and returning to range, Wi-Fi rejection, server missing, permissions revoked, and phone reboot. Use the helper's shareable report and a simple timed test checklist. Measure battery over equivalent sessions, including an idle baseline; no always-on-hotspot comparison is needed to establish absolute drain.

## Validation boundary

Research used official Google/Espressif documentation and a pinned public source checkout. No phone was connected, no private Google component was tested, and no APK/firmware behavior changed. The native route and private broadcast remain hypotheses for this device. The existing successful helper release remains the known working baseline.
