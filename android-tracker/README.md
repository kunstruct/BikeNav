# Ride Tracker

Standalone Android motorcycle recorder for the Pixel 8 Pro. Recording, raw export and line-only sharing work offline. Optional share-card maps use the internet only when selected. Requires Android 15 or newer, using its explicit read-only transaction API so raw exports cannot take the recorder's write lock. Built using Java and Android platform APIs, with no third-party runtime dependencies or accounts. Separate from MiniNav firmware and the Wi-Fi helper.

## Install and record

Install `release-artifacts/RideTracker.apk` (or the copy on the Desktop). Allow installation from the app opening the APK if Android asks. It uses Android's automatic debug signature; no signing setup is required. Updating with the same build key preserves rides. **Do not uninstall or clear app storage to update: that deletes recordings.**

1. Open **Ride Tracker**. Tap **Allow unrestricted recording** and allow the battery exemption for screen-off recording.
2. Turn on Location. Tap **Start ride**, grant **precise location while using the app** and allow notifications. If you deny a permission, the app links to its settings.
3. Start outdoors. Check that GPS and raw-motion counts increase and **Last durable write** stays recent. An initial GPS fix can take time, especially indoors.
4. Lock the screen or use other apps. Recording runs in its own foreground-service process and keeps a notification with **Stop & save**.
5. Stop from the notification or app. Open a saved ride for the route, counts, metrics, ten share designs, or the raw ZIP export.

The share preview omits route points within 300 metres of the route's first and last accepted fix by default. Turn this off only if you want the full route visible. Very short rides may have no visible line after hiding. Surrounding streets and place names remain visible when a basemap is enabled; this is not geographic anonymization. You can export a complete 1080 × 1350 card or a transparent PNG overlay for your own photo. The raw ZIP always contains precise, untrimmed data; sharing it is a separate, explicit action.

## Ride time, breaks and speed

Ride details, all ten share cards and `summary.csv` show **ride time**, **break time**, **average speed excluding breaks**, and **top speed**, alongside distance. These are recomputed from the saved GPS, so older rides gain the new statistics without rewriting samples or migrating the database.

A stop qualifies after at least **two minutes**, counting the whole observed stationary interval retrospectively. It requires usable speed readings at or below 0.5 m/s, GPS accuracy at most 25 m and a cluster within 25 m of its starting fix. A single noisy speed reading does not end a stop: sustained speed at least 1.5 m/s over two seconds, or leaving the cluster, does. The break ends at the last stationary fix, so the departure confirmation delay stays in ride time. Short traffic stops remain ride time. Long stationary traffic can count as a break; this is a GPS estimate, not an understanding of why you stopped.

Missing speed, poor/rejected GPS, gaps over ten seconds and segment/boot changes break the evidence chain. Unobserved time is not invented as a break. Recorded time excludes recorder downtime; ride time is recorded time minus confirmed break time. The average is filtered distance divided by ride time; confirmed stop drift is removed from distance. If the denominator is zero or there are too few usable fixes, show an unavailable speed instead of dividing by zero. Top speed requires supporting adjacent fixes and uses three-fix medians to reject isolated spikes; it is a conservative supported GPS estimate, not the highest raw value or a calibrated speedometer.

**Recording never pauses for a detected break.** GPS, raw acceleration and available linear acceleration keep saving continuously. The live health screen reports total recorded time, while an open ride/card is a snapshot. Field thresholds and GPS/battery performance still need the actual Pixel test.

## Real maps on share cards

Open a saved ride → **Create ride card** → choose **Street · classic**, **light**, **dark** or **paper**. These are four color treatments of OpenStreetMap's standard street map, combined with any of the ten card designs. The route and map use the same Web Mercator projection. Transparent export retains the map panel, route and attribution while leaving the surrounding card background clear.

Maps are optional and default to **Line only · offline**. A dedicated UI worker loads only the selected preview's tiles; the recorder never downloads maps. Changing map colors reuses the loaded image; other layouts reuse cached tiles where their viewports overlap. Cached tiles may be expired and require online revalidation even after a recent view. Network timeouts, cancellation and server errors leave the line preview and raw recording available. The share button waits for map loading, then explicitly offers line-only sharing if the map is unavailable.

The map provider receives tile requests identifying the viewed area, not a ride database, GPS trace or motion samples. Tiles use HTTPS, an app-specific User-Agent and Android's HTTP response cache, including server expiration/validation headers. The 32 MB cache contains public basemap tiles and may retain viewed regions after ride deletion; it is separate from private ride samples and shared PNGs. There is no offline-area download or bulk prefetch feature. Availability is best effort under [OpenStreetMap's tile policy](https://operations.osmfoundation.org/policies/tiles/). Exports retain [OpenStreetMap attribution](https://www.openstreetmap.org/copyright). Satellite imagery and additional providers are not included.

Stop and save any active ride before installing an app update; install over the existing app with the same signing key to retain rides.

## Optional start and finish names

In **Create ride card**, type **Start name (optional)** and/or **Finish name (optional)**. A FROM → TO row appears above the route/map; a single name works too. Leave both blank to omit the row. Names are saved locally per ride, restored when reopening the editor, and included in the PNG. Deleting the ride removes its saved names. They are card metadata and are not added to the raw sensor ZIP.

Names work offline with the line-only preview and with any street-map color or card design. Editing wording reuses the loaded map; adding/removing the names row refits the selected viewport. Long names are shortened visually with an ellipsis; fields accept up to 80 characters. Unicode scripts and emoji use Android text layout. Transparent exports retain an opaque names strip for readability.

These are manual labels, not verified place names or destinations from a routing service. Endpoint hiding still crops the GPS line independently. Names are shared exactly as entered after whitespace cleanup, including names such as Home or Office; clear a field if you do not want that name shared. Existing start/end dots refer to the visible route, which can be cropped.

## Ten share designs

| Template | Direction |
| --- | --- |
| Apex | Orange sports layout, bold headline and clear ride statistics |
| Nightshift | Deep blue, luminous cyan route and oversized distance |
| Field Notes | Warm ruled paper, serif title and journal entry styling |
| Blueprint | Technical grid, monospaced labels and cyan route |
| Grand Tour | Cream editorial cover with large serif typography |
| Contour | Forest green, lime route and abstract contour decoration |
| Slipstream | Red diagonal racing composition and bold condensed type |
| Postmark | Travel postcard with a route panel and date stamp |
| Monolith | Black and white, huge distance and minimal route line |
| Afterglow | Sunset gradient, warm type and an open route composition |

Each design has an opaque card and a transparent overlay mode. Contour's background is decorative, not geographic terrain. All route lines come from recorded GPS; no basemap is needed.

[Original ten-design gallery](../docs/ride-tracker/share-templates.jpg) · [Current statistics and battery validation](../docs/ride-tracker/STATS-0.4.0.md) · [Names release validation](../docs/ride-tracker/NAMES-0.3.0.md) · [Map release validation](../docs/ride-tracker/MAPS-0.2.0.md) · [Original recorder validation and remaining physical-device checks](../docs/ride-tracker/VALIDATION.md)

## Recording and durability

- GPS requests: 1 Hz, zero minimum distance, including while stationary. Android/hardware may deliver at a different rate; an app cannot manufacture fixes during signal loss.
- Motion requests: raw accelerometer at 25 Hz and, if available, linear acceleration at 25 Hz. All delivered finite three-axis samples are kept; no smoothing or downsampling changes the raw archive.
- Raw acceleration includes gravity; linear acceleration excludes gravity. Both use phone axes and m/s². Signed values are retained. They are not calibrated motorcycle acceleration, braking force or lean measurements.
- Sensor callbacks and database writes run on one recorder thread, independently of the UI process. SQLite uses WAL and **FULL** synchronization configured on the connection pool. Sample batches, counters, checkpoint and segment end commit together once per second or at 128 samples. Only successful commits clear the in-memory batch.
- The target loss window on abrupt process death is the current uncommitted batch (normally about a second). OS scheduling delays, storage stalls or device failure can increase this. Completed SQLite commits are preserved; no software can promise data capture after force-stop, power loss, reboot, sensor failure or full storage.
- A session-long partial wake lock prevents non-wakeup motion sensors from stopping when the screen turns off. It is released on stop/error/normal service destruction; Android releases process-owned locks on process death. Expect battery use; physical Pixel endurance is still to be measured.
- Stale streams are logged, shown in recording health and, after the initial grace period, a problem notification. The recorder retries stale GPS/sensor subscriptions. GPS loss keeps motion recording.
- `START_STICKY` asks Android to restart a killed recorder. Every restart creates a new segment and an interruption event, preserving prior samples. Restart timing depends on Android; downtime is a real gap. Swiping the UI away does not intentionally stop the service. Force-stop and reboot require reopening the app and tapping **Resume recorder**; automatic reboot recording is not claimed.
- Storage/sensor subscription errors stop recording visibly and retain already committed samples. Stop remains available from the visible app even when location permission has been revoked.

## Battery work

The recorder now updates its foreground notification only when health changes, cancels problem notifications only when needed, and tracks provider enabled/disabled callbacks instead of querying the GPS service every second. Home polls only the active/latest indexed rows and reloads the full ride history on a change; static ride details stop polling. Health checks, retries and durable writes still run every second. GPS remains requested at 1 Hz, motion at 25 Hz per stream, with zero sensor batching delay and the same recording wake lock/WAL/FULL policy.

In one controlled 60-second emulator workload, notification updates fell from 12 to 1. This measures removed work, not phone battery percentage. CPU ticks were 21 before versus 20 after, too small a single-run difference to claim CPU or battery savings. Continuous GPS and non-wakeup motion capture still consume power; actual Pixel battery drain remains to be measured. See [validation](../docs/ride-tracker/STATS-0.4.0.md).

## Stored data and export

Private SQLite database `rides.db`, schema 1:

| Table | Data |
| --- | --- |
| `rides` | Start/end UTC, status, recorded duration, persisted sample counts, last-sample/checkpoint times, health/error note, analytics version |
| `segments` | Ride, start UTC, start/end monotonic nanoseconds, device boot count |
| `samples` | Ride/segment, kind (`gps`, `accel`, `linear`), sample UTC and monotonic timestamps, callback receipt UTC; nullable coordinates, x/y/z, accuracy, speed, altitude and mock-GPS flag |
| `events` | Timestamped starts/stops, interruptions, stale streams, retries and failures |

The ZIP contains `ride.csv`, `segments.csv`, `gps.csv`, `motion.csv`, `events.csv`, `summary.csv`, and `README.txt` with units and clock semantics. It streams from a consistent read-only database snapshot without blocking recorder writes for the duration of export. Empty fields mean unavailable, never a fabricated zero.

GPS UTC is from `Location.getTime()`. Motion UTC is the segment's initial UTC plus the monotonic sample delta. `received_utc_ms` preserves callback wall time separately. Monotonic timestamps are comparable within one device boot; boot count and segments identify boundaries. Recorded elapsed time includes stops within a segment but excludes downtime between interrupted segments. UTC wall-clock changes do not alter recorded durations.

Raw samples remain unchanged by preview filtering. Derived analytics version 2 retains the route filter for unavailable or >50 m accuracy, out-of-order fixes and jumps over 100 m/s. Route lines do not bridge rejected fixes, gaps over 10 seconds or recording segments. Distance accumulates small GPS displacements until they reach 3 m, so crawling traffic is counted rather than discarding every short step. Reported speeds under 0.5 m/s and confirmed-stop drift are excluded. Very slow movement and tiny trailing displacements can still be undercounted. Break/top-speed evidence requires accuracy at most 25 m. The raw `ride.csv` analytics-version column records its historical value; the new `summary.csv` explicitly identifies the current derived version, times, break count, distance, speeds and GPS quality counts from the same read-only export snapshot. Field accuracy has not yet been established.

Rides and samples are local; cloud backups/device transfers are excluded. Saved-ride deletion cascades to its samples/segments/events and removes this app's associated cached exports. It cannot delete copies already shared to other apps. Recording rides cannot be deleted.

## Build

JDK 17, Android SDK platform 35/build tools 35.0.0, and Gradle 8.9 (wrapper). Set `JAVA_HOME` and `ANDROID_HOME`, then:

```sh
sh tools/build_tracker.sh
# Output: release-artifacts/RideTracker.apk
```

The build helper detects this machine's task-local toolchain under `~/.local/share/ride-tracker-tools` if environment variables are absent. It does not change system Java or Android settings. Dependencies must be downloaded once; subsequent builds can pass `--offline`.

```sh
cd android-tracker
./gradlew assembleDebugAndroidTest
tracker_test_serial=emulator-5554 # Set to your dedicated emulator, never your phone.
adb -s "$tracker_test_serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$tracker_test_serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$tracker_test_serial" shell pm grant com.kunstruct.ridetracker android.permission.ACCESS_FINE_LOCATION
adb -s "$tracker_test_serial" shell pm grant com.kunstruct.ridetracker android.permission.POST_NOTIFICATIONS
adb -s "$tracker_test_serial" shell am instrument -w -e class com.kunstruct.ridetracker.StorageTest,com.kunstruct.ridetracker.RecorderTest,com.kunstruct.ridetracker.MapTest,com.kunstruct.ridetracker.CardLabelsTest,com.kunstruct.ridetracker.StatsTest,com.kunstruct.ridetracker.HomeWorkTest,com.kunstruct.ridetracker.ShareFlowTest com.kunstruct.ridetracker.test/android.test.InstrumentationTestRunner
```

These tests require an emulator and refuse to run on physical hardware. They create synthetic rides, replace its GPS provider during the test and intentionally kill the recorder process. `EnduranceTest` is a separate ten-minute screen-off test, including a three-minute stationary segment with continuous raw capture. `RecorderEnergyCheck` is an explicit 60-second screen-off workload comparison that records notification updates, CPU ticks and durable sample counts, not a physical battery benchmark. Run either separately with the same instrumentation command and its class name. The storage suite includes accelerated four-hour sample retention, transactional rollback, synchronization mode, exports, deletion, metric edge cases, privacy cropping and all ten opaque/transparent designs. Emulator tests do not establish physical GPS reception, battery life or road accuracy.
