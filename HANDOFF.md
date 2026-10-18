# Desk buddy firmware handoff

Updated: 2026-10-19

## Direction

Reuse the existing Waveshare ESP32-S3 2.8-inch touch display from BikeNav as a Tamagotchi-style desk companion. Keep separate firmware for each use: flash navigation, the desk pet, or another experience whenever wanted. No new hardware or combined mode-switching firmware is required.

Work branch: `feature/tamagotchi-desk-buddy`, created from `project/mininav-vision`. No pet firmware has been implemented; this branch currently preserves the navigation implementation.

## Discussed features and constraints

- Pet animations and interactions, clock, focus timer, and useful desk functions.
- User prefers operation without installing a phone helper app.
- BLE HID can provide music controls (play/pause, skip, volume) after pairing; this does not supply track metadata. Device compatibility still needs testing.
- Spotify now-playing information could come over Wi-Fi through Spotify's Web API, requiring API setup and account authorization. Current API eligibility and limitations need checking before implementation.
- ESP32-S3 supports Wi-Fi and BLE, but not Bluetooth Classic. Android's usual Classic AVRCP metadata route is unavailable on this board.
- General Android notification mirroring needs phone-side software with notification access. Classic MAP only exposes supported messages; changing hardware would not automatically unlock every app notification.
- iPhone has BLE Apple Media Service (AMS) and Apple Notification Center Service (ANCS), which offer a firmware-only integration route subject to pairing/authorization. The existing project phone target is a Pixel 8 Pro.

The app-free pet, clock, focus timer, and music controls were accepted as a workable direction. Spotify integration is optional; notifications remain desirable but should not force a helper app into the core experience. Exact pet design and feature scope are still open.

## Next steps

Inspect the existing display/touch firmware and build instructions, then establish a minimal pet firmware using the same hardware. Validate BLE media controls on the actual phone before expanding integrations. Preserve the navigation firmware as a separately flashable option.

Brain notes could not be saved because the Brain connector required reauthentication; this file is the handoff instead.

## References

- [ESP32-S3 hardware](https://www.espressif.com/products/socs/esp32-s3)
- [Espressif BLE HID example](https://github.com/espressif/esp-idf/blob/master/examples/bluetooth/bluedroid/ble/ble_hid_device_demo/README.md)
- [Android Bluetooth profiles](https://source.android.com/docs/core/connect/bluetooth/services)
- [Spotify now-playing API](https://developer.spotify.com/documentation/web-api/reference/get-the-users-currently-playing-track)
- [Apple AMS](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleMediaService_Reference/Specification/Specification.html)
- [Apple ANCS](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleNotificationCenterServiceSpecification/Introduction/Introduction.html)
