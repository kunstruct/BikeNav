# Evidence, parameters and engineering checks

## Sources and revision assumptions

The repository [hardware list](../../../README.md#hardware) identifies **ESP32-S3-Touch-LCD-2.8**, not the similarly named 2.8B or 2.8C. The firmware supports CST328 and CST3530 touch revisions. The user's actual PCB revision and mechanical stack have not been inspected.

Manufacturer sources retrieved on 2026-10-16:

- [Waveshare hardware resources](https://docs.waveshare.com/ESP32-S3-Touch-LCD-2.8/Resources-And-Documents).
- [Dimensioned structure PDF](https://files.waveshare.com/wiki/ESP32-S3-Touch-LCD-2.8/ESP32-S3-Touch-LCD-2.8_structure.pdf), archived as `reference/waveshare-2.8-structure.pdf` with a raster for offline inspection.
- [Manufacturer 3D ZIP](https://files.waveshare.com/wiki/ESP32-S3-Touch-LCD-2.8/ESP32-S3-Touch-LCD-2.8_3D.zip); its unmodified `esp32-s3-touch-lcd-2_8.stp` is archived as `reference/waveshare-2.8-2024.stp`. The STEP header dates it **2024-07-26**, so mechanical equivalence of a later V2 board remains unverified.
- [Original MiniNav case source](https://github.com/MiniNavDevice/MiniNav/tree/main/docs/case). The actual local meshes were measured, not replaced by newly downloaded files.

Manufacturer documents retain their original ownership; their inclusion records the dimensional evidence, not an assertion of a new license. [Reference hashes](reports/reference-sha256.txt) identify the exact files used. [Raw measurement JSON](reports/reference-measurements.json) contains original mesh planes, volumes and components and all 451 manufacturer component bounds.

## Actual original mesh measurements

`source/inspect_references.py` loads the triangles, checks topology, measures plane locations and produces cross-sections. Precision below reflects tessellation, not manufacturing accuracy.

| Original local STL | Bounds / features measured from triangles |
|---|---|
| `MiniNav-Frame-with-button.stl` | X −30.200073..30.199839, Y −41.500065..41.501343, Z −0.000100..13.500100; nominal 60.400×83.001×13.500; 520,668 triangles; 16,428.7755 mm³; watertight; one connected component |
| Frame inner planes | X ±25.15, Y −35.0 / +35.001: approximately 50.3×70.0 PCB pocket. Additional planes X ±25.3, Y ±36.6 identify the glass relief. Back ledge Z1.5; front relief Z12.4; front rim Z13.5. These are stepped interior features, not the outer bounding box. |
| `MiniNav-back-panel-with-strap-slits.stl` | X −26.500101..26.500078, Y −36.250114..36.249241, Z −0.000099..1.500100; nominal 53.000×72.499×1.500; 560,368 triangles; 5,416.8807 mm³; watertight; one component |
| Original rear holes, Z0.75 section | Approximately X ±20.280, Y −30.0301 / +30.0292, diameter 3.397–3.400. Its approximately 40.56×60.06 pitch differs slightly from the manufacturer 41×60 reference. The new design uses the manufacturer's factory support positions rather than copying that discrepancy. |

See [sections through both original meshes](reference/original-sections.png). The new rear is independently modelled, with no strap openings or text penetrations. The original flex-button feature is not reused.

## Coordinates and electronics keep-outs

New enclosure coordinates are landscape: +X points out through USB, +Y is the hood/top edge, +Z points out of the display. Glass is centred in XY; its **front face is Z−0.5**. Units are mm. The original case coordinates use portrait X for width, Y for height and a separate Z origin; no original STL is used as a hidden Boolean operand in the new CAD.

For an original manufacturer STEP point `(u,v,w)`, new case coordinates are:

```text
x = -v - 3.0
y =  u
z =  w - 1.2
```

This is a proper 90° Z rotation followed by translation. The manufacturer glass has native bounds X±25.27, Y−39.53..33.53, face Z0.7. All 451 native solids collectively fit these XY bounds and Z−9.3000001..0.7. Validation transforms each solid's bounding box and tests it against the real front/rear CAD; zero box intersections is conservative evidence that all archived components clear those walls. This is **not** a claim to have measured a physical board.

| Feature | Evidence / case coordinates |
|---|---|
| Touch glass | Manufacturer drawing 73.06×50.54, R2; X±36.53, Y±25.27; front Z−0.5 |
| LCD active area | Drawing 57.60×43.20, centre X−2.7, Y0; orientation is landscape |
| Touch viewing area | Drawing 58.20×43.80; opening 61.4×47 clears it by 1.6/1.6 each side. Original viewing-area tolerances still require checking on the unit. |
| PCB outline | Drawing 69.00×49.90; STEP PCB solid 2 approximately 69.017×49.920, thickness1.6; centred near X−0.1, Y−0.05 |
| Overall manufacturer depth | Drawing10.00; actual STEP extrema −9.3000001..0.7. New assembly from glass to factory support ends remains10.00. |
| Four factory support centres | X −30.1 / +29.9, Y±20.5, giving60×41 pitch; support backs Z−10.5. STEP solids440–443. New support pedestals end Z−11.0, with 0.5 installed compliant pads. No screw goes from the mount through the PCB cavity. |
| USB metal body/envelope | STEP solids181/202; outer solid202 maps to X27.7483..35.2783, Y−4.8500..4.7300, Z−9.7495..−5.5895. Opening axis Y−0.06, Z−7.6695. Housing sweep reaches X35.28. |
| Three button bodies | STEP437–439: centres near (−6.623,22.952,−7.2), (−14.442,22.952,−7.2), (−22.260,22.952,−7.2). BOOT/RESET/power mapping is not inferred from these unnamed solid indices. All are inside closed walls with no external actuator. |
| Back components/connectors | Their individual STEP boxes are included in all 451 checks. No external speaker, battery or header connection is supported in this enclosure. USB is the power/data connection. |

The main cavity is 74.26×51.74, giving0.6 nominal glass clearance per side, with rounded corners that also clear the conservative rectangular boxes. A flared opening grows by 8 mm per side from the underside to the front face, providing access over the raised rim. A 16 mm spherical fingertip tangent to the film is checked at the LCD centre and all four corners for each hood. This is a geometric proxy, not a human-factors or touch-response test. The visible film window is centred on the actual display, not on the whole board. The archived manufacturer's model is broader evidence than the old case's external dimensions.

## Parameters and how to change them

`source/parameters.json` records dimensions and assumptions; `source/design.py` is the editable, authoritative solid model. Some entries are fixed reference dimensions of this board and assembly stack, not independent styling knobs. The checked defaults should be regenerated and revalidated after changes. The validation uses fixed design criteria deliberately, so an incompatible change should fail rather than silently generate an apparently approved design.

| Parameter/group | Default | Adjustment contract |
|---|---:|---|
| `bar_diameter` | **22.2 assumed** | Measure the actual straight mounting section with callipers. Regenerates bore, liner, ear spacing, neck height and assembled adapter height. No assumption that the aftermarket bar centre is 22.2. |
| `clamp_width` | **32 assumed** | Axial rigid clamp length; reserve width+3 for liner lips, 35 default. Width changes also move the side-slot entrances. Use at least32; recheck available straight length and tool access. |
| `clamp_split_gap` | 1.0 total | Assembly gap between rigid halves, not each side. Changing it requires changing/rechecking hardware seat elevations in assembly and validation. |
| `liner_free_thickness` / `liner_installed_thickness` | 1.0 / 0.8 | Free STL vs compressed assembly. Recheck grip, compression and bar finish physically. Changing installed thickness also changes clamp bore. |
| `clamp_wall` | 6 | Radial shell thickness before bolt-seat reliefs; no strength qualification implied. |
| `adapter_angles`, `default_angle_degrees` | [0,20,30],20 | Fixed adapter choices. Added angles need the complete clearance/mesh checks and updated BOM. CAD input degrees; angle about bar X. |
| `hood_projection` / `hood_side_projection` | 0/18/25 and 0/8/12 | Front/style parameters. Larger values affect print support, touch access, view angle and wind loads. |
| `rail_clearance` | 0.35 | Profile coordinate allowance, not a uniform normal offset. Ideal flank clearance approximately 0.313; floor0.35; neck0.35 each side. Test0.20/0.35/0.50 coupons. |
| `usb_opening_width/height`, centre Y/Z | 16×8,−0.06/−7.6695 | Housing opening. Raise/expand only with seal-web and plug-sweep checks; current web below gasket is 1.2695. |
| `insert_diameter` / `insert_length` | 5.6 pilot /6 | The diameter is the **pilot**, not maximum knurl OD. Nominal insert≤6.3 OD. Blind bore adds1 mm end space; validate the actual insert on the coupon. |
| `electronics_xy_clearance` | 0.6 | Main glass envelope allowance per side. Tongue/socket/gasket dimensions remain a coordinated design; large changes require editing those profiles. |
| `bushing_free_flange` / installed flange | 2.7 /2.5 | Free STL vs loaded view. Physical load/compliance is unknown. Metal8 mm sleeve fixes the installed stack, not a tightening torque. |
| Sleeve OD/ID/length and bushing bore | 4/3.2/8 and 4.2 | Hardware contract. Carrier holeØ9, washerØ14 and 3 mm carrier must be changed together if resizing the isolation stack. |
| Glass, window, case/seam/back Z entries | See JSON | Board and stack reference values. Their mating profiles, support pads and templates are explicitly constructed in Python; do not independently alter one reference dimension without updating dependent geometry and checks. |

Safe customization begins with front colour, hood projection and a shallow **external** badge. Do not engrave through the2.5 mm bezel, 1.2 mm blind screw roofs, 1.2695 mm USB/gasket web or2.5 mm rear floor. Front variants share all existing hardware and seals.

## Seals, hardware and load paths

The rear floor is 2.5 thick. The front flange is 2.5 thick, with a closed film on its underside. Its6 shell nuts are outside the gasket loop; pocket roofs are 1.3 thick, and blind screw pilot roofs1.2. The two shells meet at Z−1.5. A2 mm tongue overlaps a2.3 deep socket with 0.3 clearance on each side and at the end. Tongue narrow-side thickness is 1.38. A1.8 wide, 0.9 deep groove holds a1.6 wide, 1.2 free-height gasket:0.1 lateral clearance each side and nominal25% axial compression. The gasket is one continuous ring above the USB cut-out. No external mount fastener penetrates the cavity. Material porosity, adhesive wetting and real gasket compression have not been tested.

The rail foot is 32 long; its cross-section is 30 wide at Z8.5 and 22 wide at Z16.5, with a22-wide neck to Z19 in the isolation frame. Female floor is Z8.15 and top Z16.85. Its closed end is Y−16 and open end Y+20.35. The six shell screw heads clear its top by 0.45 in the removal sweep; clean print scars here. A blind M4 insert lies entirely in the external rail, nominally6.2 below the electronics cavity at the pilot envelope. Even the maximum6.3 mm knurl envelope leaves5.85 vertical separation. Release screw travel is 9; when released its tip is Y−19,3 mm clear of the rail's near face. The Ø22 flange cannot pass through the Ø18.6 cage front. The separate screwed head cover retains the hex bolt inside the thumbwheel.

The steady-state isolation path is:

```text
bar → thin grip liner → split clamp → fixed-angle adapter
    → four TPU lower/upper flanges and waists → floating carrier
    → dovetail + locked release screw → case
```

Each M3 isolation bolt clamps a metal sleeve against the rigid adapter, using a top retention washer and a lower washer/nut. The carrier is separated from that sleeve by TPU and a2.5 radial rigid-to-rigid gap; the upper washer is 2.5 above the carrier plate. The nominal free TPU stack8.4 compresses to 8.0, approximately 7.41% of the total flange thickness. Rendered TPU is a simplified installed shape, **not a deformation simulation**. At large movement, downward carrier travel reaches the two stops after 1.5; upward travel reaches retaining washers after 2.5; X travel reaches a rail-side/washer-edge stop after 1.5 (earlier than the sleeve clearance). These are geometrically checked contact limits, not usable dynamic stroke or a vibration specification. Rotation and combined-axis impacts need physical testing. Four positions at 56×36 pitch reduce unconstrained rocking geometrically, but no measured attenuation or resonance frequency is claimed.

The adapter underside is pocketed, preserving4 mm perimeter walls, 5 mm sloping roof, the central clamp-bolt block and Ø13 isolation columns. Isolation nut counterbores areØ10 and retain4 mm to the top. The clamp has flat Ø9.4 hardware seat reliefs cut into the curved shell, allowingØ9 washers and aØ9 socket envelope. Bolts into the upper clamp use side-loaded nuts under a2.1 mm roof. These geometric sections are checked design details, not stress/creep/fatigue analysis.

## Validation scope

[VALIDATION.md](reports/VALIDATION.md) and [validation.json](reports/validation.json) are generated from the actual BReps and exported meshes. Checks cover:

- Every STL: watertightness, consistent winding, positive volume, exactly one connected component, bounds against export manifest and volume against CAD; BReps valid and one solid before export.
- All three adapter assemblies: pairwise positive-volume interference, including major clamp, adapter, isolation and shell hardware. Intended contacts have zero volume. Simplified threads are clearance cylinders; heat-set knurl interference and small tapped cage/knob threads are not simulated.
- All front variants mated to the common rear, and 451 conservative manufacturer component boxes against each enclosure half.
- Case/rail and protruding case hardware swept through61 positions over60 mm travel, plus analytical constant-section review; captive screw swept through37 positions over9 mm.
- USB housing insertion, continuous fitted gasket, shell tool access, clamp/socket access, isolation nut access, nominal fastener engagement and stop contact onset.
- SHA-256 equality of both original STLs.

No finite-element analysis, global minimum-thickness tomography, slicer/printer run, real touch test, weather sealing test, sunlight/thermal test, resonance measurement, fatigue test or ride test has been performed. Nominal wall and clearance checks are listed individually rather than described as a blanket strength or waterproof certification.
