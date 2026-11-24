# Ride Tracker 0.4.0 — break-aware statistics and less background work

24 November 2026. Delivered and validated in the isolated Android 17 emulator.

## Delivered behavior

Ride details, all ten opaque/transparent card designs and the ZIP's new `summary.csv` use one derived summary: ride time, break time, average excluding breaks and supported top speed, alongside distance. Older rides are recalculated from their saved GPS. Names, map colors, route privacy and raw data are retained.

A stop qualifies after 120 seconds of usable low-speed GPS in a 25 m cluster (speed <=0.5 m/s, accuracy <=25 m), counting its full observed stationary span. A lone noisy speed does not end it; sustained >=1.5 m/s over two seconds or leaving the cluster does. The break ends at its last stationary fix. Short traffic stops stay in ride time. Long traffic stops may qualify; this is a GPS estimate. Missing speed/poor GPS, rejected fixes, gaps >10 seconds and segment/boot changes do not fabricate break evidence. Unobserved time remains ride time. These initial thresholds need field validation.

Ride time = recorded segment time minus confirmed breaks. Average = filtered distance / ride time; zero/unknown denominators show an unavailable value. Stop drift is excluded from derived distance. Top speed uses supporting adjacent values/three-fix medians with usable accuracy to reject isolated spikes. Raw coordinates, speed samples and timestamps stay unchanged. Capture continues through every break; there is no sensor pause/resume state tied to analytics.

The export remains a read-only snapshot. Existing raw CSVs are unchanged; `summary.csv` adds derived analytics version 2 and numeric times/speeds/quality counts from that same snapshot. The legacy `ride.csv` recording-version field retains its historical value. No schema migration was needed.

## Battery-related changes and evidence

- Foreground notification updates occur on health changes, replacing five-second refreshes of identical text. Warning cancellation occurs on recovery, replacing a cancellation request every healthy second. Startup/restart clears stale warning notifications; duplicate Start restores recording health text.
- Provider enabled/disabled callbacks replace one GPS-status service query per health tick. Initial/retry registration still checks provider state. This uses Android's documented [provider notifications](https://developer.android.com/reference/android/location/LocationListener#onProviderDisabled(java.lang.String)); tests require both warning delivery and recovery cancellation.
- Home reads indexed active/latest rows and reloads full history only when relevant state changes. It detects even a ride completed between polls. Static details stop polling; returning home resumes it. Async responses are guarded against stale screen generations.
- GPS 1 Hz, raw/available linear acceleration 25 Hz, zero sensor batching delay, one-second/128-sample commits, WAL/FULL and the recording wake lock remain unchanged. Storage write/retention logic was not weakened. RideStore gains only a latest-row reader and permits instrumentation subclassing for actual query counts.

Matched explicit 60-second emulator workload:

| Measurement | 0.3.0 baseline | 0.4.0 |
| --- | ---: | ---: |
| Duration | 60,018 ms | 60,030 ms |
| Notification updates/enqueues | 12 | 1 |
| Recorder CPU ticks | 21 | 20 |
| Durable GPS increments at measurement end | 60 | 59 |
| Durable raw increments | 1,482 | 1,538 |
| Durable linear increments | 1,482 | 1,538 |
| Checkpoint age | 889 ms | 657 ms |

[Baseline report](stats-0.4.0/baseline-work.json) and [output](stats-0.4.0/baseline-work.txt); [optimized report](stats-0.4.0/optimized-work.json) and [output](stats-0.4.0/optimized-work.txt).

This is a direct measure of removed work, not battery percentage. Counts are sampled before final Stop flush and hardware delivery/tick alignment varies. One CPU-tick difference is too small a single-run change to claim CPU savings. Continuous GPS/non-wakeup motion still use power; actual Pixel battery drain must be measured.

## Verification

- Debug APK/instrumentation builds and Android lint passed. APK v2 signature verified. [Build output](stats-0.4.0/build.txt).
- 12 statistics cases passed: whole/short/exact-threshold stops, sustained resumption, isolated noise/jitter and stop-distance removal, GPS loss, segment/boot boundaries, missing/invalid speeds and poor accuracy, slow travel out of a cluster, supported top speed, empty/zero denominators, exact snapshot summary/raw preservation, and all ten named/transparent footer layouts.
- UI work/statistics integration passed: a single full history scan across a stable 3.5-second home interval, detection of a quickly saved ride, correct ride/break/average/top detail values, zero active/latest polling during 2.5 seconds on static detail, and polling resumption on returning home. [Focused 13-case output](stats-0.4.0/stats-ui-tests.txt).
- Initial full run passed 42/43; the sharing helper could only scroll forward after the taller stats panel. Updated it to recover by scrolling backward at the end. Both named and blank sharing workflows then passed. [Initial run](stats-0.4.0/android17-initial-suite.txt), [focused sharing rerun](stats-0.4.0/share-tests.txt).
- Before the final slow-traffic distance correction, full Android 17 regression passed **43/43** in 251.812 seconds. [Output](stats-0.4.0/android17-pre-distance-fix.txt).
- Final APK regression: **44/44 passed** in 237.387 seconds, including the new crawling-traffic case and all prior recorder/storage/UI/map checks. [Output](stats-0.4.0/android17-suite.txt).
- Final build integration: **2/2 passed**, replaying the completed raw endurance recording with identical counts/times and producing live map-card exports with the new statistics. [Output](stats-0.4.0/integration-checks.txt), [replay report](stats-0.4.0/endurance-replay.json).
- Final review found the old distance rule discarded individual GPS steps under 3 m. The final build accumulates such displacement before counting it; a new 1 m/s crawling-traffic test requires about 180 m over three minutes and a sensible average. It changes derived analysis only.
- Ten-minute screen-off capture passed: **600/600 injected GPS fixes**, **15,057 raw** and **15,056 linear** samples, motion maximum gap **0.040 s**, GPS maximum gap **1.000889 s**. One sustained stop was identified as **198,999 ms** break time; ride time **403,335 ms** over **602,334 ms** recorded. [Output](stats-0.4.0/endurance.txt), [measurements](stats-0.4.0/endurance.json). This run used the optimized recorder before the final analysis-only distance fix; recorder sampling/commit/health code is identical. The final build replays this preserved raw trace separately.
- The isolated emulator also showed a stock System UI startup ANR; that system dialog was identified and dismissed. It was not a Ride Tracker ANR.
- In-place upgrade retained 56 existing test rides and all sample totals (3,543 GPS / 51,626 raw / 51,576 linear); the saved-name preference file hash stayed identical. [Upgrade evidence](stats-0.4.0/upgrade.json).

- The final in-place update also retained **76 rides**, all sample totals (5,041 GPS / 73,345 raw / 73,276 linear) and identical saved-name preferences. [Final upgrade evidence](stats-0.4.0/final-upgrade.json).
- Actual native map exports with a synthetic route, manual example names, two-minute ride time and a three-minute break: [Classic](stats-0.4.0/classic.png), [Light](stats-0.4.0/light.png), [Dark](stats-0.4.0/dark.png), [Paper](stats-0.4.0/paper.png). These are illustrations, not a real ride.

## Artifact and limits

Package `com.kunstruct.ridetracker`, version 0.4.0/code 4, Android 15+, same signing identity. 252,262 bytes. SHA-256 `4f83bfa0e1c92954ae97cecc958e252fb8615b816b142f792cb02cba2c20b464`.

Stop/save before installing over the existing app. Do not uninstall or clear data. Physical Pixel battery use, GPS reception and road accuracy remain unverified. Compute-device/ignition-off work remains separate future scope. Historical evidence: [0.1.0 recorder](VALIDATION.md), [0.2.0 maps](MAPS-0.2.0.md), [0.3.0 names](NAMES-0.3.0.md).

Delivered `/home/drak/Desktop/RideTracker-0.4.0.apk` locally and on Cachy via SSH alias `drak`; both checksums match. Earlier Desktop APKs were preserved. Build-helper shell syntax and source whitespace checks passed. The original 24 November artifact delivery preceded a source commit. On 25 November Drak authorized committing/pushing the working release and required this for future releases; source, tests, build support and evidence are included in the branch checkpoint. See Git history for the commit and [AGENTS.md](../../AGENTS.md) for the release rule.
