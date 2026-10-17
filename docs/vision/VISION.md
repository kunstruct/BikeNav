# MiniNav — a little companion for wherever your day goes

MiniNav is a small, personal device that belongs on your bike, beside your keyboard, and next to your phone. Plug it into a supported host and it takes on a useful role. A tiny original character, Pip, makes those roles feel like one continuous relationship.

The core promise: **Your route. Your rhythm. Your little companion.**

## The experience

- **Ride:** Navigation owns the screen. Personality appears at departure and after parking, without competing with directions.
- **Desk:** Connect a USB data cable to a PC running the companion service. After initial pairing, MiniNav offers focus sessions, selected notifications, media controls, and host-reported system stats.
- **Phone:** Connect to a supported phone integration for battery/status, media, and selected stats. Exactly which metrics are available depends on the host APIs and permissions; this is not a promise of unrestricted phone telemetry.
- **Conversation:** Explicitly start a notes session. The phone or PC captures audio and runs transcription locally or through a selected service; MiniNav shows recording state, elapsed time, and pause/stop controls. Make participant awareness and consent part of starting a session. Do not record automatically upon connection. Recording and processing location must be clear.
- **Offline:** Pip, a clock once time is set, and a local focus timer remain useful without a host connection. The initial product is externally powered; unplugging is not a promise of battery operation.

## Personality without chores

Pip is a small mint-colored pixel creature with curious ears and a warm, restrained personality. It wakes when the device powers up, settles during focus, and celebrates an arrival. A later memory system could collect places and completed focus sessions as small illustrated keepsakes.

Borrow the emotional attachment of a virtual pet, without hunger penalties, guilt, broken streaks, or rewards for looking at the device while riding. Progress should reflect time spent living, not time spent tapping. User control comes first: quiet mode, reduced motion, and a simple utility-only display.

## The connection is the interaction

The intended everyday flow is: plug in → recognize an authorized host → show the appropriate home screen. First connection requires setup, permissions, and pairing. A charging-only cable or unsupported host should leave the device in local mode with a clear connection status.

Use a common capability handshake: the host identifies its role and offers specific services, such as notifications, media, system metrics, or transcription controls. MiniNav shows only supported modules. A user can override the suggested mode. Reconnection restores the useful state without replaying stale notifications.

This is a proposed architecture, not an implemented USB feature. Validate the board's exposed USB path, available USB-device support, flashing/recovery behavior, and phone USB role/power requirements before committing to a transport. Desktop integration needs an installed service; phone support needs a companion app and platform-specific permission work. The ESP32 should render lightweight native UI and relay commands while the host performs transcription and other heavy processing. Ride mode can retain the existing Android Auto path; switching must respect its resource needs.

## Build sequence

### Open-source platform and agent integrations

The core runtime, companion bridge, server, protocols, and example modules should use open-source technology and support self-hosting. Basic use must not require a proprietary cloud account. Optional adapters may connect to proprietary providers without making them dependencies of the core experience.

Agent adapters connect through each provider's supported endpoints or integration protocol, advertise their capabilities, and translate work into a common task lifecycle: requested, running, awaiting approval, completed, failed, or cancelled. Pip can convey that state while the display shows progress and results. The bridge retains credentials and checks scoped permissions; a downloaded module does not inherit access to files, microphones, or command execution. Device actions that need approval must show the actual proposed action and wait for an explicit response.

Make installation a product feature: a browser flasher, a packaged local bridge, a documented self-hosting recipe, a versioned protocol, and a small example module. These are delivery goals, not features already implemented. A useful first agent demonstration is to connect the device to a PC, load a module from a self-hosted service, request a task, and show its progress and result on MiniNav.

### Architecture direction: local bridge plus cloud sync

The user selected both a local PC/phone bridge and cloud sync. MiniNav should evolve into a reusable device runtime: a stable renderer, input handling, connection management, a persistent companion, and a small library of native widgets. Integrations supply versioned module descriptions and data rather than each becoming a separate firmware fork.

Data path: **cloud services ↔ PC/phone companion bridge ↔ MiniNav runtime**. The bridge also reads authorized local notifications, media, and system metrics directly. Cloud access should initially go through the bridge, keeping provider credentials and expensive work off the display. Direct device-to-cloud access can be evaluated later if a concrete use case needs it.

Separate three kinds of change:

- **Live data:** subscriptions or streams while connected, with reconnect/backoff, timestamps, and bounded queues. Coalesce frequent stats updates and expire old notifications.
- **Module updates:** download validated manifests, screen layouts, allowed action definitions, and bounded assets; cache a last-known-good version. Keep a locally available default home screen.
- **Firmware updates:** explicit versioned releases for new capabilities, with validation and a recovery strategy. Remote layouts cannot magically add a missing driver or arbitrary new native behavior.

Start with declarative screens: text, icon, progress, gauge, list, button, and pet state. Do not start with unrestricted downloaded code. Negotiate protocol and widget versions, enforce screen/asset size limits, and reject unsupported modules gracefully. The service offers capabilities; the device reports what it can render.

The cloud synchronizes module catalog, preferences, and optional companion memories. Device permissions remain local and scoped to each integration; syncing an account should not silently grant notification, microphone, or system-control access. Transcripts and raw notifications should not sync by default. Pair the device and bridge, authenticate service access, and verify update provenance.

“Available at any time” means fresh data whenever the required host, network, permissions, and power are available. Offline mode shows cached content with its age and disables actions that need a live host. A server outage must not prevent local focus sessions or access to the existing navigation mode when its own prerequisites are met.

First architecture proof: connect MiniNav to one PC bridge, discover its capabilities, render a remotely described stats card, receive live metric updates, disconnect gracefully, and restore after reconnect. Then add cloud-delivered module configuration to that same flow. This proves adaptability before growing the catalog.

1. **Make it feel alive:** Native local home, Pip's idle states, clock, focus timer, and a clean transition back to navigation.
2. **Prove the cable experience:** One supported PC platform, USB handshake, connection status, a single useful metric, and explicit reconnect behavior.
3. **Make the desk useful:** Selected notifications and media controls. Add permissions and per-app filtering before widening notification access.
4. **Extend to the phone:** Validate data/power roles on the actual board and target phone; expose only metrics available through supported APIs.
5. **Try conversation notes:** Host-based audio capture and transcription, explicit recording controls, clear retention/deletion settings, and no ambient background recording by default.
6. **Grow the relationship:** Optional memories, cosmetic customization, and additional host-provided modules after the core connections are reliable.

The first success criterion is simple: after setup, connecting a data cable reliably produces something useful without opening an app manually. The second is emotional: you miss the little companion when it is absent.

## Intro film

`mininav-intro.webm` is a 49-second, 1280×720, 24 fps animated concept film with an original synthesized soundtrack. It is caption-led, with no spoken narration. All device drawings and screens are conceptual, not a physical fit model or a demonstration of shipped firmware.

| Time | Beat | Message |
| --- | --- | --- |
| 0–7 s | Reveal | Small screen. Little soul. |
| 7–14 s | Wake | Meet Pip, your everyday companion. |
| 14–21 s | Ride | Directions take priority. |
| 21–28 s | Arrive | Celebrate once parked. |
| 28–35 s | Focus | A riding buddy becomes a desk buddy. |
| 35–42 s | Connect | PC updates, phone stats, explicitly started notes. |
| 42–49 s | Invitation | Go places. Grow together. |

Re-render with `/usr/bin/python3 docs/vision/render_intro.py`. Requires Pillow, NumPy, Python GObject/GStreamer, and the VP8/Opus/WebM plugins. `soundtrack.wav` and the selected PNG frames are also exported for editing.

Grounding: the local MiniNav README and the Brain session `mininav-companion-20261016-a1b72c`, extended by the user's cable-connected PC/phone and conversation-transcription ideas. The existing project provides a navigation foundation; companion behavior, USB host integrations, stats, memories, and transcription are proposed work.
