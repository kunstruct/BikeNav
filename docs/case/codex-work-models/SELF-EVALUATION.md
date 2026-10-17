# Review of the original design

Reviewed 2026-10-16 from editable CAD, current STL exports, the original meshes, manufacturer drawing/STEP, assembly checks and regenerated images. This directory was originally `docs/case/detachable/`. [Provenance and recovery](../automation/README.md) separate the interrupted design from subsequent review.

**Disposition: suitable for a measured fit prototype, not qualified for riding.** I recommend this option for the first compact enclosure trial, subject to confirming the board's pad support and touch through the added film.

## Recovery and corrections

The original session ended with a usage-limit error while its latest flared-front revision was being exported/checked. The inherited report said31/31 PASS but was stale: the last log showed `front_compact` failing. Review reproduced that failure as31/32, with every actual mesh still watertight, consistently wound and a single positive-volume component.

The failure was the **manifest's CAD bounding box**, not a hole in the mesh. After loft tessellation, OCCT's default box claimed compact Z−0.001396..8.988048, while the STL was Z0..6 and the exact analytic solid was Z0..6.0000001. The export now uses `BRepBndLib.AddOptimal` with triangulation disabled. The0.04 mm bounds and1% volume thresholds remain unchanged; source/parameter hashes, mesh hashes and exact source-bound comparisons were added. [Reproducible diagnosis and measurements](../automation/review-evidence.json) retain the discrepancy.

Review moved the deliverables to `codex-work-models`, updated current build/document paths, rebuilt 22 STLs and STEP assemblies, regenerated templates and all seven previews, remeasured both original meshes, and completed the comparison. The original mechanical architecture was retained: the final flared aperture had already been designed before interruption. The generated images had lagged that final geometry and were refreshed. The case-removed render now also removes case screws/insert instead of showing suspended hardware.

## Evidence against the brief

| Criterion | Finding / evidence | Remaining limitation |
|---|---|---|
| Electronics |451 transformed manufacturer solid boxes clear the front/rear; glass73.06×50.54, cavity74.26×51.74. Original STL section features and factory support pitch were remeasured. |2024 STEP may differ from user's board. Pad retention relies on the real glass/pillar stack and needs careful shimming. |
| USB |16×8 passage;14×6 housing sweep reaches the nominal receptacle face. USB/gasket web1.2695. |Cable housing, nose length and insertion force unmeasured. |
| Closed enclosure |Overlapping tongue/socket, continuous1.6-wide gasket, bonded clear film; screws outside the seal, no strap/button holes. |Open USB, layer porosity and bonds are untested; no waterproof rating. |
| Window/touch |Purchased0.20 PET film,0.15 adhesive,0.15 nominal glass gap. Flared aperture passes a16 mm sphere at LCD centre/corners for all fronts. |Finger proxy is not a glove/human/touch test; film durability, glare and pressure unknown. |
| Hardware |External blind rail insert; captive wheel flange plus screwed bolt-head cover; all three adapter assemblies checked for positive-volume collisions. Case, clamp and isolation tool approaches checked. |Small tapped plastic threads, insert pullout and actual hardware tolerances need testing. Small keeper/cover screws are documented but not detailed threaded CAD. |
| Detachment |32 mm dovetail;36.35 disengagement travel,60 reserved.61 sampled slide positions include case screws;37 knob-withdrawal positions include the bolt/head cover. |Samples plus straight-profile reasoning are not proof of hand access or real bike clearance. |
| Clamp |Two-piece installation,32 axial width plus liner lips=35 straight length;1.0 nominal closing gap, liner1.0→0.8. |22.2 bar diameter is assumed. Grip/taper/controls/torque unknown. |
| Isolation/capture |Four pairs of TPU halves around8 mm sleeves; no rigid carrier bypass at rest.2.5 radial/upper gap,1.5 downward and X stop onset; washer capture2.5 overlap. |TPU properties, combined-axis motion, fatigue and retention strength untested. No measured attenuation. |
| Printing/variants |22 valid single-solid exports;20 pieces for sun including printed gasket.0/18/25 hoods and0/20/30 adapters share one mount. Local floor2.5, tongue1.38, hood2.4. |Touring is a deeper **hood**, not a deeper electronics cavity. Supports, slicer paths and global minimum thickness have not been qualified. |

The [current validation report](reports/VALIDATION.md) records **33/33 PASS**. Pass count reflects this script's aggregation, not a quality ranking against the independent script. Original STL hashes remain unchanged. This is computational evidence only.

## Render inspection

I inspected all three assembled variants, exploded view, rear mount, case-removed mount and X0 cutaway. The images show the actual final flare, the purchased film, continuous seam placement, short rail, four separated bushings and lower captive knob. The section confirms external locking hardware does not enter the PCB cavity. Simplified electronics/hardware in assembly STEP and previews are labelled proxies; full vendor component boxes are used for fit checking. Material colours/transparency are illustrative, not photographs or strength evidence.

The main tradeoff is complexity:20 printed pieces including eight bushing halves and a printed gasket, custom Ø14 washers/8 mm sleeves, and a printed release wheel with small screws. In exchange the90×74 face,163.41 cm³ rigid CAD volume and36.35 slide stroke are smaller than the independent option's96×78,222.97 cm³ and59. There is no inferred printed mass or load advantage beyond those geometric measurements.

## First physical work

Measure the bar and board/cable first. Print the six fit coupons, then one compact front/rear and clear-film test. Confirm support-pad pressure, full USB mating and touch gestures before spending material on the mount. Trial one TPU pair/sleeve, then the full clamp/carrier and capture mechanism. After dimensional fit, test seal contact with electronics removed, sun/heat creep, repeated removal, fastener retention, clamp slip and vibration before progressive tethered ride trials. No physical print, fit, seal, touch, thermal, vibration, fatigue or ride test was performed in this review.
