# Printing, adjustment and physical tests

All STL units are mm. They are translated to the bed (Z=0). Print only one matched variant; neutral STEP parts are in native assembly coordinates. No slicer job or physical print has been run.

PLA is for inexpensive dimensional prototypes. PETG/ASA are later rigid-material candidates, subject to actual solar heating, creep and mechanical testing. TPU is for liners/bushings. No hardness or infill choice has a demonstrated vibration or strength rating here. Keep early tests stationary and use an independent tether for any later progressive road trial.

## Print setup

A 0.4 mm nozzle,0.2 mm layers and 4–6 perimeter lines are reasonable **trial settings**, not qualification. Use sufficient top/bottom layers to form the modeled floors, inspect the slicer paths and tune to the printer/material. Infill cannot substitute for continuous walls. Check dimensional compensation on coupons before modifying functional CAD.

| Part | Exported orientation / support attention |
|---|---|
| Upper clamp/base and lower clamp | Bore axis vertical; native Y rotated onto Z. Support base overhangs, nut traps and underside spotfaces. Inspect bore/liner surfaces and flat washer seats. |
| Liners | Bore axis vertical; flexible wall, no rigid fill. Upper/lower are separately exported. |
| Carrier | Flat bottom down. Support elevated rail/nose surfaces and horizontal nut slot; remove all debris from T tracks. |
| Rear shells | Rail feet down, cavity up. Support surrounding exterior floor, cage ear/posts and blind nut slots. Keep USB passage clear. |
| Bezel/hood | Rear hard-seat plane down. Support tongue groove, window shelf and blind ledges; do not scar bond/seal faces. Hood rises upward. |
| Tray | Underside flat down; bosses up. Check factory-pillar holes and no support contact with fragile board parts. |
| Captive cap | Flat plate down; verify Ø13 hole, bolt seats and intact perimeter. |
| Nut keeper | Flat face down as exported; horizontal screw bores may need cleanout. |
| TPU bushing | Flange down, open bore vertical; tune bridging under upper flange. Check insertion stretch on coupon. |
| USB coupon | As exported, aperture bridges16 mm; rotate onto a broad face in slicer if preferred. |

The rear floor is3 mm except1.5 under the inner tray-nut slots. Tongue wall is1.6, carrier plate6, base5, hood2.8, and local hood wall beside screw reliefs1.3. The minimum USB-to-seam vertical web is1.6695. These are local nominal sections, not a global minimum-wall or strength analysis. Visible FDM gaps, layers separating at rails, stressed threaded pilots or warped seal lands require replacement.

## Fit coupons first

1. Print `coupon_rail_male` and the 0.2/0.3/0.4 female coupons using intended rigid material, nozzle and orientation. The default0.3 is a per-side profile allowance. Choose a free sliding fit without excessive play; update `rail_clearance`, regenerate and rerun validation before printing full parts.
2. `coupon_holes` has2.4,2.8,3.4,4.6,5.5 mm holes from native−X to+X. Check bolts and pilot/tapping practice. Actual hardware, shrinkage and hole ovality determine corrections.
3. Print `coupon_usb_plug`; check the housing against its16×9 passage, then check full insertion in the real shell. A housing passing the coupon does not establish connector reach.
4. Print one `coupon_isolator`, one TPU bushing, and trial-fit the specified sleeve and large washers. Inspect flange insertion,11.4 sleeve compression height and capture. TPU deformation is not simulated by the CAD.

## Customize and regenerate

`cad/parameters.json` exposes bar diameter, straight width, liner thickness, split gap, rail allowance, board/window and USB assumptions, isolation dimensions and variants. `cad/model.py` is the editable source for all solids. Many coordinates form a coupled stack: these fields are not independent sliders. Changing sleeve/washer/flange dimensions, clamp gap or board-face height requires reviewing screw lengths, stops, cavity height, masks and templates. Rebuild always, and treat failed checks as incompatibilities to diagnose.

The touring shell moves electronics and window+5 while preserving the external rail height; it requires the touring tray. Its cavity may improve wiring room, but adds size/material and does not create a battery qualification. Compact and sun use the same internal stack. Hood colours and shallow external badges are inexpensive changes; never engrave through a sealing surface or structural floor.

## Required physical work

- Measure bar diameter at several angles, taper and straight length, controls/cable/steering sweep, and rider sightline. Recheck on the actual Art of Motorcycles bar;22.2 is not a measured fit.
- Confirm the user's board revision, pillar thread/depth, glass clearance and USB connector/cable geometry. Compare the 2024 STEP to the actual unit. Test board restraint without glass pressure or button activation.
- Test clear-sheet touch corners, gestures and gloves, optical glare, sunlight visibility and condensation. Added0.5 sheet plus0.2 air gap may make touch unsuitable; thin-film alternatives need a changed bezel/bond stack and validation.
- Inspect unbroken gasket compression and adhesive contact using witness marks. With electronics removed, conduct gradual splash/ingress checks; the open USB port remains an opening. No IP claim follows from closed CAD meshes.
- Test rail fit, full thumb retraction, captive hardware, nut keeper, repeat removal and tether capture. Use measured hardware and real hand access; geometric sphere/tool proxies do not prove usability.
- Bench-test clamp slip, fastener loosening, TPU set, fatigue, temperature creep and failure capture before incremental ride exposure. Measure actual vibration if evaluating isolation; do not infer attenuation from TPU presence alone.
