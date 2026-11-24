# Ride Tracker 0.2.0 — optional share maps

Historical validation for 0.2.0. The current names update is documented in [0.3.0 validation](NAMES-0.3.0.md); the unversioned build output now contains that newer APK.

24 November 2026. Maps are an optional addition to the existing recorder, history, raw export and ten share designs. No changes to the recorder, sample schema or recording configuration were needed.

## Use

Open a ride → **Create ride card** → select **Street · classic**, **light**, **dark** or **paper**. These are four color treatments of the standard OpenStreetMap raster map, not four different cartographic providers. Choose any of the ten card compositions, optionally make the surrounding background transparent, then share a PNG. Actual exports with a synthetic route over a real map: [Classic](maps-0.2.0/classic.png), [Light](maps-0.2.0/light.png), [Dark](maps-0.2.0/dark.png), [Paper](maps-0.2.0/paper.png). These are not real motorcycle rides.

Line-only remains the default. The first map load needs internet; HTTP-cached tiles may be reused. Cached tiles may already be expired and require online revalidation even after a recent view. Missing maps show an error and a retry button, with explicit **Share line PNG** fallback. GPS and motion recording do not depend on map availability. Satellite imagery, offline-area downloads and turn-by-turn navigation are outside this addition.

Endpoint hiding removes route points within 300 m of either end. The basemap still shows surrounding streets and places, so this is not geographic anonymization. Attribution stays visible in opaque and transparent exports.

## Implementation and provider

The UI has a dedicated map worker, separate from database/export work and the `:recorder` process. The loader projects map tiles and route points through the same Web Mercator viewport, handling date-line wrapping and latitude limits. It loads only the current user-selected preview, with at most 24 tiles, bounded image sizes, connection/read timeouts and cancellation. Changing colors reuses the loaded bitmap.

Tiles come from HTTPS `tile.openstreetmap.org` with an identifiable app User-Agent and a 32 MiB Android HTTP response cache honoring server cache/validation headers. No area prefetch or bulk downloading is implemented. Tile requests expose the viewed region to the provider; raw ride coordinates, timestamps and motion samples are not uploaded. The public tile cache can retain viewed regions after a private ride is deleted.

Provider requirements and best-effort availability: [OSM tile policy](https://operations.osmfoundation.org/policies/tiles/). Exported credit includes the [OpenStreetMap copyright and license URL](https://www.openstreetmap.org/copyright).

## Verification

- Gradle debug APK, instrumentation APK and Android lint passed; [build output](maps-0.2.0/build.txt).
- Six map fixture tests cover projection/date line, tile/route alignment, all four colors across all ten designs in opaque and transparent modes (80 renders), failure/cancellation fallback, invalid/oversized image rejection and empty routes avoiding downloads. Fixture tests do not request provider tiles.
- Regression suite includes storage retention, fault recovery, screen-off/Doze, sharing, and a new slow failed map connection test requiring continuing GPS, motion and durable checkpoints. The initial run passed 24/25; its immediate Doze-state assertion raced Android’s state publication. The assertion now waits up to five seconds while still requiring actual Doze. The final full Android 17 run passed **25/25** in 180.947 seconds; [final output](maps-0.2.0/android17-suite.txt); [initial output](maps-0.2.0/android17-initial-run.txt).
- In-place emulator update from 0.1.0 preserved all 28 rides and unchanged totals: 1,663 GPS, 22,985 raw and 22,959 linear samples. See [upgrade and live-recording measurements](maps-0.2.0/recording-and-upgrade.json).
- Live map preview and PNG export succeeded with real provider tiles while another emulator ride recorded. That recording retained 20,226 raw and 20,226 linear samples over about 809 seconds; maximum motion timestamp gap was 0.040 s. Its 588 emulator GPS fixes had a maximum gap of 2.879 s. The test operator started instrumentation before accepting the stop confirmation, interrupting this test session; this is not a clean-stop endurance test.
- Separate explicit native live-provider check passed (1 test): real tiles, four PNG exports, offline cached-map attempt, uncached map failure and line fallback; [output](maps-0.2.0/live-map-check.txt). Its initial assumption that every just-viewed tile would be fresh was incorrect: the provider returned `Age` beyond `max-age`. Android correctly required revalidation; [initial check](maps-0.2.0/live-map-initial-check.txt). The check now permits this documented offline outcome. `LiveMapCheck` is a manual-only provider check requiring synthetic fixture ride 28, excluded from the regular 25-test fixture suite.
- The original 0.1.0 [ten-minute screen-off baseline and Android 15 tests](VALIDATION.md) remain historical evidence. Physical Pixel GPS, battery life and road accuracy remain unverified.

## Artifact

Version 0.2.0/code 2, package `com.kunstruct.ridetracker`, Android 15+, same debug signing identity as 0.1.0; APK v2 signature verified. 227,490 bytes. SHA-256: `b2c5a36a6cc3344ca5f1e75a8dcec556f6b318bb7f50a9b9bc37655171c08fd0`.

Stop and save a current ride before installing over the existing app. Do not uninstall or clear storage to update.

Delivered to `/home/drak/Desktop/RideTracker-0.2.0.apk` locally and on Cachy via SSH alias `drak`; both copies have the SHA-256 above. The previous Desktop `RideTracker.apk` was preserved.
