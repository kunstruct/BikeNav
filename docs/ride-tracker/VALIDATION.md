# Ride Tracker 0.1.0 validation — 24 November 2026

Historical baseline for version 0.1.0. For the current optional-map release, see [0.2.0 validation](MAPS-0.2.0.md). The build output path below now contains the newer APK; the original Desktop `RideTracker.apk` remains version 0.1.0.

The commute APK is built and validated on emulators. Physical Pixel 8 Pro reception, road accuracy and battery endurance remain unverified.

## Delivered artifact

- Package `com.kunstruct.ridetracker`, version `0.1.0` (code 1), Android 15+; target/compile SDK 35.
- `release-artifacts/RideTracker.apk`; also copied to the Desktop. 100,010 bytes, pure Java with no third-party runtime dependencies or internet permission.
- Android APK Signature Scheme v2 verified. Automatic debug signing requires no user signing setup; no private keys are part of the repository.
- SHA-256: `39ed9efbfc6f9cd13b8024723a44ba889a75f604daef3bc523bcbbcf295e9e87`.

## Completed checks

| Check | Result |
| --- | --- |
| Gradle debug APK, instrumentation APK and lint | Passed |
| Android 15 emulator suite | 17 tests passed; [output](android15-suite.txt) |
| Android 17 emulator final suite | 18 tests passed, including forced Doze; [output](android17-suite.txt) |
| Android 17 real-time screen-off capture | 10 minutes passed; [output](android17-endurance.txt), [measurements](screen-off-endurance.json) |
| Forced deep Doze with battery exemption | GPS and motion persisted with fresh checkpoints; [dedicated run](android17-doze.txt) |
| Killing the UI process | Same recorder PID continued; another 259 raw samples committed in the next 10 seconds |
| Notification Stop & save with UI closed | Saved the ride and released the recording wake lock; [evidence](ui-process-recovery.json) |
| Denied location and notifications | Setup guidance shown, zero recording rides created; [evidence](permission-denial.json) |
| All ten share designs | Distinct opaque images and transparent variants, 1080 × 1350; privacy-cropped route tested |
| Android share sheet and content provider | Opened with readable PNG; write access refused; checked on Android 15 and 17 |
| Shell syntax and Git whitespace | Passed |

The storage suite simulates four hours at 25 Hz with 360,000 raw samples and 14,400 separate commits. It checks reopening, counts, unique timestamps and SQLite integrity. This is accelerated storage testing, not a four-hour physical recording.

Fault tests cover a full database preserving the prior commit, transactional rollback, one active ride, deletion cascades, WAL/FULL configuration, concurrent recording during a stable read-only export snapshot, GPS jumps/gaps/out-of-order fixes, stationary drift, missing values, process-death recovery, duplicate Start, rapid Stop/Start, screen-off capture and GPS outage with continuing motion.

## Ten-minute Android 17 capture

GPS was injected through Android's test-provider API and motion came from emulated Android sensors. The screen was explicitly verified off. No phone or road measurements are implied.

| Measurement | Result |
| --- | --- |
| GPS fixes injected / stored | 600 / 600 |
| Raw acceleration samples | 15,086 |
| Linear acceleration samples | 15,085 |
| Recorded interval, including startup/stop | 602.448 seconds |
| Largest GPS timestamp gap | 1.003511868 seconds |
| Largest raw-motion timestamp gap | 0.040 seconds |
| Largest linear-motion timestamp gap | 0.040 seconds |
| SQLite quick check | Passed |

## Test environment and practical limits

Tests used isolated Android 15 and Android 17 Google APIs emulators, separate from other users' services, with a dedicated ADB server on port 5038. Android 17 required current command-line tools (22.0), correct `android-37.0` AVD metadata, an 8 GB data partition and additional container memory headroom. Initial Launcher/System UI ANRs blocked the UI test; the sharing rerun and final full suite passed after fixing the test environment. Test activities use fresh tasks to prevent a previous Android chooser from obstructing the next test. Instrumented fault tests refuse physical hardware.

Actual Pixel battery use, thermal behavior, GNSS reception, background behavior under the user's settings and reference-route accuracy require the commute test tracked as RIDETRACK-2. Enable the app's battery exemption, grant precise location and notifications, start outdoors and check both sample counters before locking the screen.

Force-stop, reboot, loss of power, unavailable GPS/sensors and full storage can stop capture. Already committed samples are preserved; normally the uncommitted batch is the loss window. The app warns about stale streams and errors, marks restart segments and does not fabricate readings. Reopen and Resume after force-stop/reboot. Raw/linear phone acceleration is not calibrated motorcycle braking or lean.

[Build and usage instructions](../../android-tracker/README.md) · [Ten designs with synthetic route examples](share-templates.jpg)
