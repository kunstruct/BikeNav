# Main integration review — 2026-10-17

Recommendation: merge the documentation, design prototypes, concept media, and empty-file cleanup first. Review the helper implementation separately through [PR #1](https://github.com/kunstruct/BikeNav/pull/1). Do not merge the aggregate vision branch wholesale: its ancestry includes the helper and changes the web installer to the alpha firmware.

## Branch decisions

| Source branch | Decision | Reason |
| --- | --- | --- |
| `design/mininav-model-comparison` | Include all reviewed deliverables | Both prototypes have editable CAD, STL/STEP exports, assembly instructions, provenance, and explicit physical-validation limits. |
| `project/mininav-vision` | Select vision/media, README navigation, empty placeholder removal | These describe proposed work; they do not implement companion features. The full branch also includes experimental executable changes. |
| `research/android-auto-startup` | Select its research document plus supporting helper documentation | Keep the dated findings and hypotheses available without importing its helper ancestors. |
| `fix/wifi-cellular-coexistence` | Include documentation; hold executable changes in PR #1 | Successful user-reported proof of concept, but no recorded endurance/recovery test matrix. It also replaces the default installer image with an alpha. |
| `integration/main-docs` | Prepared merge candidate | Based on current `origin/main`, with the selected material above and no firmware, Android app, or installer changes. |

The core open-source runtime, cloud/local module system, and agent adapters remain a vision. There is no implementation branch for those features to merge yet. The enclosure designs remain prototypes, not physically qualified products.

## Evidence checked during this review

- Refreshed remote refs. At review start, `main` was `37f0c6b`, helper `7dd7277`, research `097a0a5`, models `284ea82`, and aggregate vision `ef37f82`.
- Firmware host tests on the aggregate branch passed with ASan/UBSan, including helper greeting parsing, timeout, link-loss, existing touch/video geometry, socket, and Wi-Fi policy coverage.
- The documentation candidate passed the existing main-branch ASan/UBSan host tests. Its firmware source, build version, tests/tools, and web installer have no diff against `origin/main`; selected documentation links resolve locally.
- Both CAD export-input manifests match the current source and parameter SHA-256 values. All 45 exported STL files match their recorded manifest hashes. Original upstream reference STLs match `main`.
- Recorded CAD validation is **33/33** and **175/175**. The source/export checks above verify that those artifacts have not changed; a new OpenCascade geometry run was not performed in this review.
- The helper's merged firmware and browser-embedded base64 image match exactly, including declared size and MD5. Consistency does not establish release readiness.
- The concept film is 49 seconds, 1280×720, 24 fps, with VP8 video and Opus audio. Its complete streams decoded successfully during the vision work.
- PR #1 is open and mergeable, with no status checks reported at review time. A mergeable Git graph is not a passing hardware test.
- The Java relay test could not run in the current shell because `javac` is absent. The earlier [development record](helper-development-process.md) records successful Java socket tests, Android build/lint, and ESP-IDF build. Those builds were not rerun here; the earlier temporary ESP-IDF toolchain path is no longer available.

## Helper release gates

Before promoting the helper to the default installer, record results for:

1. A sustained, screen-off session on the supported phone with navigation and cellular internet both working; document duration, battery and temperature without claiming unmeasured savings.
2. Board power interruption, Wi-Fi loss, leaving/returning to range, and explicit disconnect/reconnect. Record the manual recovery steps the alpha currently requires.
3. Missing Android Auto developer server, permission rejection/revocation, and cold phone/board startup. Manual server startup remains a known prerequisite.
4. Regression smoke checks of Board WiFi, Phone AP, and Auto WiFi, including mode switching and saved preferences.
5. Reproducible Android assembly/lint, firmware build, host tests, paired release checksums, and a clear installer decision: promote the tested alpha explicitly or keep a separate experimental installation path.

Broader phone compatibility and automatic Android Auto startup are separate goals; they need not block a narrowly scoped experimental release with an accurate support statement. We can also choose to merge helper source as opt-in experimental work earlier, but that should be an explicit decision with the web installer kept on the chosen baseline.

## Merge order and branch retention

1. Review and merge the documentation candidate into `main`.
2. Rebase or merge updated `main` into the helper PR, resolving overlapping documentation deliberately, and complete the agreed helper gate or experimental-source decision.
3. Keep the aggregate branch until its helper ancestry has been accounted for. Selective cherry-picks do not make it safe to infer that every branch is fully merged from Git ancestry alone.
4. Keep release tags and source history. Delete superseded branches only after their contents are reconciled and branch cleanup is explicitly desired.

No production merge, firmware flash, release promotion, or branch deletion was performed as part of preparing this candidate.
