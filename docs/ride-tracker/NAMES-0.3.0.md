# Ride Tracker 0.3.0 — optional start and finish names

Historical validation for 0.3.0. Current break-aware statistics and battery work are documented in [0.4.0 validation](STATS-0.4.0.md); the unversioned build output now contains the newer APK.

24 November 2026. Delivered and validated on the isolated emulator.

## Behavior

Open a ride → **Create ride card** → type **Start name (optional)** and/or **Finish name (optional)**. The preview and exported PNG show a FROM → TO row above the map or route. One name alone is supported; both blank preserve the previous card layout. All ten designs, four map colors and transparent exports support the names row.

Names are saved in private app preferences per ride, restored when the editor is reopened, and cleared by ride deletion. They are manual card metadata, not geocoded destinations, and are not included in the raw sensor ZIP. Whitespace is normalized. Fields accept up to 80 characters; longer display text is ellipsized using Android text layout, including Unicode scripts and emoji. The row keeps an opaque background in transparent exports.

Endpoint hiding remains independent. Typed names are visible in the PNG even when the route ends are hidden; the editor explicitly says so. Existing start/end dots mark the visible route, not hidden exact coordinates. No reverse geocoding or additional permissions were added.

Map loading and rendering use the same adjusted frame when the names row is present. Editing wording reuses the loaded map; adding/removing the row refits only the current preview. RecorderService, RideStore and database schema are unchanged. Battery optimization and compute-device work remain in backlog RIDETRACK-4; this release does not implement break analytics.

## Verification

- APK, instrumentation build and Android lint passed; APK v2 signature verified. [Build output](names-0.3.0/build.txt).
- Four label tests passed: blank/long/Unicode normalization, per-ride retention and clearing, omission/single/two-name rows confined to all ten route panels, and geographic alignment across four map colors with transparent exports.
- Two UI sharing tests passed: editing/restoring/clearing names, updated accessible preview, exact PNG comparison against the named renderer, Android share sheet/read-only content provider, deletion removing names, and the existing blank-card share path. [Focused 6-test output](names-0.3.0/labels-tests.txt).
- Initial tests caught an unsupported regex flag; fixed by using explicit Unicode categories. Android documents that the [Unicode-character-class flag is unsupported](https://developer.android.com/reference/java/util/regex/Pattern#UNICODE_CHARACTER_CLASS). [Initial output](names-0.3.0/labels-initial.txt).
- A stock System UI startup ANR in the isolated Android 17 emulator blocked the first UI attempt; dismissed that system dialog, then both UI tests passed. [Blocked output](names-0.3.0/labels-system-ui-blocked.txt). No Ride Tracker crash was observed in that run.
- In-place update retained all 44 existing emulator rides and unchanged sample totals: 2,702 GPS / 47,496 raw / 47,454 linear. [Upgrade evidence](names-0.3.0/upgrade.json).

- Full Android 17 regression: **30/30 passed** in 232.63 seconds, including storage retention/faults, recorder screen-off/Doze/recovery, maps, labels and both sharing flows. [Output](names-0.3.0/android17-suite.txt).
- Separate explicit live-provider check: **1/1 passed** in 4.104 seconds, generating all four named map exports and checking offline availability/fallback. [Output](names-0.3.0/live-map-check.txt). It uses the existing synthetic ride 28; it is excluded from normal provider-free regression tests.
- Actual native PNG exports with a synthetic route and manual example names: [Classic](names-0.3.0/classic.png), [Light](names-0.3.0/light.png), [Dark](names-0.3.0/dark.png), [Paper](names-0.3.0/paper.png). These are not real ride measurements.

## Artifact and limits

Package `com.kunstruct.ridetracker`, version 0.3.0/code 3, Android 15+, same signing identity. APK 241,358 bytes; SHA-256 `f10bfebc1ddca10253b2740837a5dcebd7638b7e185238ee5ec1e585ce09f42a`.

Stop and save an active ride before installing over the existing app. Do not uninstall or clear storage. Real maps still require connectivity when uncached or expired; manual names and line-only sharing work offline. Physical Pixel GPS, battery life and road accuracy remain unverified; previous recorder/map evidence is retained in [0.1.0](VALIDATION.md) and [0.2.0](MAPS-0.2.0.md).

Delivered `/home/drak/Desktop/RideTracker-0.3.0.apk` locally and on Cachy via SSH alias `drak`; both checksums match the artifact above. Older Desktop APKs were preserved. Source whitespace and build-helper shell syntax checks passed. No source commit was created.
