# Evidence, coordinates and engineering scope

## Sources

The repository [hardware list](../../../README.md#hardware) identifies Waveshare ESP32-S3-Touch-LCD-2.8. Current [display code](../../../main/board.cpp) selects landscape rotations1/3 and keeps the last landscape rotation while held portrait. The review corrected the original independent mount's portrait orientation mechanically; firmware is unchanged.

Manufacturer/source material was retrieved during the design sessions on 2026-10-16. The review inspected the archived drawing raster, actual STLs and STEP, and rechecked the current [Waveshare resources page](https://docs.waveshare.com/ESP32-S3-Touch-LCD-2.8/Resources-And-Documents).

| Source | Local evidence / role |
|---|---|
| [Original MiniNav case](https://github.com/MiniNavDevice/MiniNav/tree/main/docs/case) | Two unchanged STLs in the parent directory; [measurement JSON](evidence/reference-measurements.json), [sections](evidence/reference-sections.png). Source attribution, not a claim that outer mesh bounds equal electronics dimensions. |
| [Waveshare structure PDF](https://files.waveshare.com/wiki/ESP32-S3-Touch-LCD-2.8/ESP32-S3-Touch-LCD-2.8_structure.pdf) | [Archived PDF](evidence/waveshare-structure.pdf) and [raster](evidence/waveshare-structure.png); separate PCB, cover glass, active/viewing areas and support pitch. |
| [Waveshare 3D archive](https://files.waveshare.com/wiki/ESP32-S3-Touch-LCD-2.8/ESP32-S3-Touch-LCD-2.8_3D.zip) | [Unmodified STEP](evidence/esp32-s3-touch-lcd-2_8.stp), [451 solid bounds](evidence/board-solids.json). STEP header dated2024-07-26; no guarantee it matches a later board revision. |
| Accu SKT-M4-16-A1 | [Archived DIN464 drawing](evidence/accu-din464-m4x16.pdf): Ø16±0.35 head,9.5±0.29 height, Ø8 shoulder,3.5 knurl height,16 length,12 threaded length. Used to size positive screw capture. |
| Accu HPLW-M5-A2-BL | [Archived washer drawing](evidence/accu-m5-large-washer.pdf): OD15(+0/−0.43),ID5.3(+0.18/−0),thickness1.1(+0/−0.2). Reviewed nominal stack uses1.1, not the interrupted model's1.2. |

Supplier documents remain their authors' material; no new license or dimensional certification is implied. Supplier hardware is an envelope reference, not a purchase/stock guarantee. [Recovery baseline hashes](../automation/recovery-baseline-sha256.json) identify pre-review files. Final exported mesh/source hashes appear in validation manifests. No scans or physical measurements of the user's board or bike were taken.

## Original meshes and actual electronic dimensions

| Feature | Measured / documented evidence |
|---|---|
| Original frame | Approximately60.400×83.001×13.500;520,668 triangles; single watertight component. Interior planar pocket approximately50.3×70.0, with separate glass relief around50.6×73.2. |
| Original back | Approximately53.000×72.499×1.500;560,368 triangles; single watertight component. Rear screw section pitch approximately40.56×60.06, differing slightly from the drawing's41×60. |
| Factory glass |50.54×73.06, R2; native STEP X±25.27, Y−39.53..33.53, frontZ0.7. |
| PCB |49.90×69.00 in drawing, not the glass outline. |
| Active / viewing area |43.20×57.60 active;43.80×58.20 viewing. Centre is offset relative to cover glass. |
| Factory support pitch / total stack |41×60; glass face to rear pillar ends10.00. Thread size and blind depth are **assumed**, not specified by these overall dimensions. |

Native model transform from vendor `(u,v,w)` is `(u,v+3,w+17.8+extra_depth)`. Thus glass is centred X0,Y0, faceZ18.5+extra; active/viewing centre X0,Y3. Factory supports are X±20.5,Y−29.9/+30.1, rear endsZ8.5. Main shell cavity60×76 gives4.73/1.47 clearance per side around the glass's rectangular extent (rounded corners are also checked against actual geometry). The PCB and all 451 component solids, not just a glass box, are checked against rear, bezel, tray and window.

USB is at native X−0.06,Z11.3305+extra; receptacle mouthY−35.2783. The16×9 shell passage faces−Y, below the seam. An assumed14×7 housing stops atY−36.9: metal nose reach and full mating need physical verification. The three native-side buttons, card/header/battery interfaces remain closed inside. No membrane button is included, so no unsealed button hole is created.

The reviewed lower aperture is48.2×62.6 and flares to60.2×74.6 over the outer bezel. The clear sheet is53×69×0.5, atZ18.7; adhesive0.2 above it. The gap from native glass to sheet is0.2. The adhesive ring's2 mm nominal bond land and the0.7 compressed seam are geometric intentions, not verified seal performance.

## Interfaces, load path and checks

The steady load path is bar → grip liner → split clamp/integrated base → four TPU grommets → floating carrier → dual T rails/locked M4 screw → case. Sleeves and large washers clamp to the fixed base; they do not touch the carrier at rest. Static nominal radial metal clearance2.2 and flange spacing2.7 are checked geometrically. Separate stops engage after1.0 down/radial or0.9 up; the validation probes0.7 free movement and1.2 stop contact. Combined rotation, elastomer deformation, dynamic impacts and strength are untested.

The52 mm rails have8 mm feet/4 mm necks, with default0.3 per-side receiver allowance. Case/receiver are0.5 apart outside the rails. Removal to native+Y (screen right) needs59 mm to clear the receiver; the sampled check covers62. The captured DIN464 head has10.5 nominal lift and1.5 tip clearance over the receiver nose; manufacturing tolerances and printed deformation reduce these margins. The13 mm cap opening is smaller than the nominal16 mm head, and post spacing18 exceeds it. The nut keeper closes the side-loading receiver slot.

Seam hard seats were moved from X±34,Y±43 toX±34.5,Y±43.5 to remove4.360665 mm³ of gasket overlap. The gasket is continuous and remains outside the tongue. Four closure heads and drivers clear the hood through dedicated reliefs. Clamp lower spotfaces prevent large washers cutting into the curved clamp shell. Rotated hex-nut geometry matches the rotated clamp traps.

Validation distinguishes actual mesh topology, exact CAD bounds/volume, installed pair intersections, conservative board installation sweep, sampled tray/slide/screw motion and nominal local sections. Source/export SHA-256 checks prevent accepting stale mesh artifacts. It does not establish an overall stress factor, minimum wall everywhere, watertight assembled FDM body, production fit, safe ride speed, measured vibration damping or qualified service life.
