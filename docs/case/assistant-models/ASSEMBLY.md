# Bill of materials and assembly

Use one matched variant. Quantities below are for one sun assembly, excluding coupons. Purchased hardware must match the measured dimensions; check actual stock and print fit. No torque or load rating is specified.

## Printed parts

| File in `stl/` | Qty | Material / purpose |
|---|---:|---|
| `rear_sun`, `bezel_sun` | 1 each | Rigid shell and hood; choose matching compact/touring instead if wanted |
| `board_tray` | 1 | Rigid, removable board support; touring uses `board_tray_touring` |
| `clamp_upper_base`, `clamp_lower` | 1 each | Rigid, split clamp and integrated fixed base |
| `carrier` | 1 | Rigid, floating dual-rail receiver |
| `captive_cap`, `nut_keeper` | 1 each | Rigid, positive thumbscrew and receiver-nut retention |
| `tpu_bushing` | 4 | TPU, one-piece captured grommets |
| `tpu_liner_upper`, `tpu_liner_lower` | 1 each | TPU grip liners, 1.0 free radial thickness |

That is **14 printed pieces** (8 rigid, 6 TPU), plus purchased sheet seals. STL names above omit `.stl`. There are **seven optional coupon files**: three female rails, one male rail, holes, USB and isolator fit.

## Purchased parts

| Item / dimensional contract | Qty | Use |
|---|---:|---|
| M5×25 socket screw, head Ø8.5 × 5 | 4 | Clamp; enters from below |
| M5×20 socket screw, head Ø8.5 × 5 | 4 | Isolation |
| M5 plain hex nut AF8 × 4 | 8 | Clamp and isolation; use suitable removable locking method after fit checks |
| M5 large washer **OD15 / ID5.3 / t1.1** | 12 | 4 below clamp, 8 on isolator ends; measured thickness matters |
| Metal sleeve **OD8 / ID5.3 / length11.40** | 4 | Isolation compression stops, square/deburred ends; match lengths within about0.05 |
| M3×18 socket screw, head Ø5.5 × 3 | 2 | Positive travel-stop capture; review increased length from 16 to engage the full nut |
| M3 washer **OD14 / ID3.4 / t1** | 2 | Travel-stop capture; source by dimensions |
| M3 plain nut AF5.5 × 2.4 | 6 | 2 stops, 4 enclosure |
| M3×22 socket screw | 4 | Compact/sun enclosure; touring uses M3×27; verify available stock or cut/deburr accurately |
| M2.5×6 screw, head ≤Ø4.5 × 2.5 | 4 | Tray to shell |
| M2.5×8 screw, head ≤Ø4.5 × 2.5 | 2 | Captive cap |
| M2.5 nut AF5 × 2 | 6 | Tray and cap traps |
| **Assumed M3×6 low/button head**, ≤Ø5.7 × 1.65 | 4 | Into factory board pillars through3 mm tray bosses; confirm thread and blind depth before use |
| M4×16 DIN464 high thumbscrew | 1 | Ø16 knurl, Ø8 shoulder, total head9.5, knurl height3.5; threaded length12 per supplier drawing |
| M4 plain nut AF7 × 3.2 | 1 | Side-loaded receiver lock; keeper prevents escaping sideways |
| M2.5×8 screws, head ≤Ø4.5 | 2 | Nut keeper; tap Ø2.1 blind pilots to M2.5, about5 mm plastic engagement |
| Clear PET sheet **53×69×0.5, R0.8** | 1 | Purchased window; touch response must be tested |
| Continuous adhesive ring **outer53×69 R0.8 / inner49×65 R0.5 / t0.2** | 1 | Compatible bond to sheet and selected rigid plastic |
| Continuous silicone gasket **outer69×85 R6.5 / inner64.2×80.2 R4.1 / t1.0** | 1 | Seam: hard seats set0.7 installed height, nominal30% squeeze |
| Independent tether with bar loop | 1 | External Ø5 eye; slack for isolation, short enough to catch released case |

The archived Accu washer drawing specifies1.1 mm, with negative thickness tolerance; the review corrected the model's former1.2 mm stack. Measure the supplied washers/sleeves and recalculate the stack if they differ. Do not replace large capture washers with ordinary small washers. Factory board thread/depth, sleeve procurement, silicone compression and adhesive selection are not verified on physical hardware. No battery, speaker or header cable is accommodated externally.

## Enclosure assembly

1. Measure the board revision, factory pillar threads and available thread depth. In native CAD, the factory support ends are Z8.5 and glass is Z18.5. The removable tray bosses extend5.5..8.5. M3×6 below a3 mm boss gives nominal3 mm thread penetration; shorten or change the screw to match the actual pillar. Never force an uncertain thread or bottom a screw against the display.
2. Load four M2.5 nuts into the shell's inward-facing side slots at native X±27.2, Y±22. These pockets retain a1.5 mm floor underneath. Load four M3 enclosure nuts from the rear pockets at X±34.5, Y±43.5. The enclosure nuts are outside the gasket loop.
3. Attach the factory board to the tray from underneath before lowering the populated tray into the open rear. The tray supports the factory pillars, not the LCD glass. Lower straight down and secure with four M2.5×6 screws. The touring tray/board stack is5 mm higher.
4. Bond the clear sheet into the bezel pocket with the continuous adhesive ring. Native sheet centre is X0,Y3, bottomZ18.7; glass-to-sheet nominal air gap0.2. Adhesive liesZ19.2..19.4. Use compatible material, clean surfaces and the adhesive's cure requirements. Preserve an uninterrupted bond and check all display corners and drags through the extra sheet.
5. Lay the unbroken silicone seam ring on the rear flange, outside the tongue. Bring the bezel down evenly. Four hard screw seats define the0.7 mm installed seal gap. Close using four M3×22 screws (touring27), checking the ring has not walked into the tongue groove and no board load is created. Fastener head/driver reliefs in the hood must be free of support scars.
6. Inspect the USB passage with the intended cable. Nominal opening16×9; tested housing proxy14×7×24 reaches nativeY−36.9,1.62 mm outside the archived receptacle mouth. Metal nose engagement is assumed, not proven. Disconnect USB before sliding the case from the mount.

## Clamp and isolation

1. Measure actual bar diameter and40 mm of straight bar, taper, controls and steering clearance. Regenerate rather than forcing the default22.2 bore. Install both liners and clamp halves without removing grips. Four M5×25 screws and large washers enter below; M5 nuts sit in upper hex traps. The model's installed rigid split gap is2.0 and liner thickness0.8 versus1.0 free. Check grip and even gap; stop if the halves bottom or the bar is marked.
2. Before the carrier hides the base, load the four M5 isolator nuts and two M3 stop nuts into underside traps. The travel-stop towers are integral to the base.
3. Press four TPU bushings into the carrier's Ø12.4 holes. Barrel Ø12, boreØ8.2, flangeØ18. Test the deformation needed to insert a flange on the isolator coupon; do not tear/cut it. Each free bushing is12 high, installed11.4 (flanges3→2.7). TPU hardness, creep and force/displacement are unmeasured.
4. Place lower large M5 washers on the fixed base, fit the carrier/bushings and four metal sleeves, then top washers and M5×20 bolts. Tighten to seat the metal sleeve stack without additional TPU crushing. The bolts, sleeves and washers are fixed; the carrier contacts only TPU at rest. Nominal sleeve-to-carrier radial gap2.2, axial flange gaps2.7. Keep cables/tether from forming another rigid restraint.
5. Fit the two Ø14 travel-stop washers and M3×18 screws. Carrier clearance is nominal1.0 down/radial and0.9 up. The large M5 washers overlap the carrier holes radially by1.3 at rest, providing additional capture if TPU is lost. Capture is a geometric feature, not a tested failure load rating.

## Detachable lock

1. Slide the M4 nut into the carrier nose from native+X. Fit `nut_keeper` using two M2.5×8 screws in tapped pilots. Load the two M2.5 cap nuts into the shell's posts. Drop the DIN464 thumbscrew into the shell ear and fasten `captive_cap` with two M2.5×8 screws. A16 mm knurl is retained by a13 mm cap opening.
2. Retract the thumbscrew fully. Its nominal travel10.5 moves the tip toZ−5.5,1.5 clear of the receiver nose atZ−7. Bring the case from native+Y (screen right) and slide toward−Y until both52 mm T rails are seated. Tighten the thumbscrew by hand into the receiver nut. Do not rely on slide friction alone.
3. Reserve at least62 mm of clear slide travel to the screen's right, plus hand/cable room. Rail disengagement occurs after59 mm (male near end−26 passing receiver far end+33). The check sweeps0..62 in1 mm increments with the retracted screw and captive cap included. Actual bike obstructions are not modeled.
4. Fit the independent tether, orient the clamp for the rider, and reconnect USB with a relaxed loop. To detach, unplug, release any detachable tether connection, unscrew fully, pull the screw to its stop and slide right. The thumbscrew stays with the case; the receiver nut stays with the bike.

Bench check screw captivity, nut retention, all-direction capture, motion, clearance and liner grip before outdoor trials. Recheck every fastener and printed thread after repeated removals; replace damaged parts. No physical fit, water, thermal, vibration or ride test has been performed.
