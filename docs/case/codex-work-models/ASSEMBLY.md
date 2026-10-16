# Bill of materials and assembly

## Printed parts — one complete mount

| File in `stl/` | Qty | Material / selection |
|---|---:|---|
| `front_sun.stl` | 1 | Rigid; or choose compact/touring |
| `rear_shell.stl` | 1 | Rigid; includes external male rail and tether eye |
| `floating_carrier.stl` | 1 | Rigid; includes female rail, end stop, cage back and sides |
| `cage_front.stl` | 1 | Rigid; positively captures knob flange |
| `thumbwheel.stl`, `thumbwheel_cap.stl` | 1 each | Rigid; hex-head bolt and positive head cover |
| `clamp_upper.stl`, `clamp_lower.stl` | 1 each | Rigid; split clamp, no grip/control removal |
| `angle_adapter_20.stl` | 1 | Rigid; or 00/30, not all three |
| `bushing_half.stl` | 8 | TPU; four pairs |
| `liner_half.stl` | 2 | TPU or replace with equivalent rubber liners |
| `seam_gasket.stl` | 1 | TPU fit trial; continuous 1.2 mm silicone sheet ring preferred for sealing |

That is **20 printed pieces** with the printed gasket, or 19 with a purchased silicone ring. Six additional coupon files are optional test pieces; the three fronts and three adapters are alternatives.

## Purchased hardware

Dimensions below are the geometry used in the checks. Verify the hardware actually supplied. Thread lengths are measured below the head. No installation torque or load rating is specified.

| Item | Qty | Purpose / dimensional requirement |
|---|---:|---|
| M4 × 25 socket-head screw | 4 | Split clamp; standard M4 pitch 0.7 |
| M4 nyloc nut, AF7, height 5 | 4 | Clamp retention |
| M4 washer, OD9 / ID4.3 / t0.8 | 8 | Both sides of four clamp bolts |
| M4 × 10 socket-head screw | 2 | Adapter to upper clamp; head OD7 × height 4 |
| M4 plain hex nut, AF7 / height 3.2 | 2 | Side-loaded traps inside upper clamp |
| M3 × 20 socket-head screw | 4 | Isolation pins; standard M3 pitch 0.5 |
| M3 nyloc nut, AF5.5 / height 4 | 4 | Under adapter, in socket-access recesses |
| Large M3 retention washer, **OD14 / ID3.2 / t0.8** | 4 | Above bushings; part of fail-safe capture. Source this size or cut metal discs; ordinary small M3 washers are not equivalent. |
| M3 washer, OD7 / ID3.2 / t0.5 | 4 | Beneath adapter's isolation-bolt seats |
| Metal sleeve, **OD4 / ID3.2 / length 8.00** | 4 | Controls TPU compression. Cut/deburr stainless or brass tube square, aim for ±0.05 mm equal lengths. No printed structural sleeve substitution. |
| M4 × 35 **hex-head** screw, AF7 / head height 2.8 | 1 | Captive release screw in printed thumbwheel |
| M4 heat-set insert, length 6, recommended pilot Ø5.6, maximum knurl OD6.3 | 1 | Blind hole in external rail; buy to dimensions and coupon-test the supplier's recommended pilot. Model shows pilot envelope, not knurl interference. |
| M3 × 16 button-head screw, head OD5.5 / height 1.7 | 6 | Rear-to-front shell closure |
| M3 plain hex nut, AF5.5 / height 2.4 | 6 | Front blind nut pockets |
| M3 × 10 pan/socket screw | 4 | Cage front; tap the Ø2.5 pilots to M3, 7 mm nominal engagement |
| M2 × 6 pan-head screw, head OD ≤4 | 2 | Thumbwheel head cover; tap Ø1.6 pilots to M2, 4 mm engagement |
| Transparent PET film, **68 × 52.4 × 0.20**, corners R1 | 1 | Purchased optical window; no printed opaque substitute |
| Continuous adhesive ring, outer68×52.4 / inner61.4×47 / t0.15 | 1 | Window-to-bezel bond, compatible with chosen plastic and PET; uninterrupted acrylic adhesive or validated sealant bond |
| Continuous silicone gasket, outer82.4×60.4 / inner79.2×57.2 / t1.2 | 1 optional | Substitute for printed TPU gasket; template gives corner radii |
| Silicone foam rear pads, Ø6 × approximately 0.8 free | 4 | Target installed thickness0.5 on factory support ends; shim after measuring |
| Thin compliant front spacer pads, 4×0.7 | 4 | Target installed thickness0.15 between glass and window bond land |
| Tether cord/webbing and independent bar loop | 1 set | Through external Ø4.6 eye; enough slack for isolation, short enough to retain a detached case |

USB power cable housing is assumed no larger than **14×6 mm** with a compatible nose/neck, within a 16×8 rounded opening. Routing and power supply are the user's responsibility. The case wall is recessed enough for the plug housing to reach the manufacturer connector face; a large right-angle plug may require a different opening and new checks.

## Preparation and enclosure

1. Complete the coupons and inspect all printed walls, dovetail edges and drilled/tapped holes. Remove support scars and elephant's foot from the sliding and gasket surfaces. Do not enlarge functional holes by guesswork.
2. Heat-set the M4 insert into the rear's **external** rail, from the rail's lower-Y end. Pilot is Ø5.6 × 7 deep; insert length 6. Use a depth stop and a straight alignment mandrel. Keep the 1 mm blind end clearance. Check M4×35 engagement with the assembled release mechanism; the tip must not bottom out.
3. Fit six M3 nuts into the front's blind pockets from the rear. Their upper faces seat at z=1.2. A tiny removable adhesive dot may hold nuts during handling; the printed pocket roof takes the fastening load.
4. Clean and bond the transparent film to the **underside** of the bezel with the continuous adhesive ring. Film centre is x=-2.7, y=0. The film spans z=-0.35..-0.15; the adhesive spans -0.15..0. Do not leave gaps at corners. The inner opening remains fully transparent. Allow the adhesive/sealant its specified cure time.
5. Add four thin front spacer pads on the film's **bonded perimeter**, centred at x=-29 and +24, y=±24.6. They are outside the active area. Nominal glass face is z=-0.5: the film-to-glass air gap is only 0.15. Pad compression and touch response need real testing.
6. Lay the board face down into the front, USB to +X and the three side buttons toward +Y. The factory glass and PCB remain an assembly. No printed actuator presses the buttons. Do not remove factory fasteners or use the original back panel.
7. Fit the four rear pads over the Ø6 support pedestals at (x=-30.1/+29.9, y=±20.5). Factory support ends are nominally z=-10.5, printed pedestals end at -11.0. Measure the real board; adjust compliant pad/shim thickness until there is gentle retention, no glass bow, no rattle and no button contact. Do not force the shells shut to compress an incorrect stack.
8. Lay the **one-piece** seam gasket in the rear groove, free height 1.2, installed height 0.9. The groove runs continuously above the USB aperture. Close the tongue/socket overlap and bring the shell hard stops together evenly with six M3×16 screws from the rear. Nominal thread engagement is 2.2 mm in the front nuts; no screw enters the electronics cavity. The screw heads remain accessible while the case is detached.
9. Test the display, touch corners, drag gestures, cable insertion and seals while stationary. Button service requires opening the shell and inspecting/replacing the gasket and bond as necessary.

## Clamp, isolation and captive release

1. Measure the actual bar and regenerate the clamp/liners if needed. Slide two M4 nuts into the side traps in the upper clamp before installation. They seat beneath a 2.1 mm roof.
2. Place the two liner halves around a clean straight section of bar, then install upper and lower clamp halves with four M4×25 screws, eight washers and four nyloc nuts. No controls need removal. The default assembled split is 1.0 mm total, and the nominal liner compresses from 1.0 to 0.8 radially. Tighten evenly while checking grip and gap. If halves bottom out or the bar is marked, correct the liner/diameter instead of applying more force.
3. Fit the selected adapter to the upper clamp with two M4×10 screws through its vertical access bores. The common head seats are 4 mm above the adapter underside. The M4 nuts are captured in the clamp, not in the hollow underside pockets. Change the adapter before fitting the carrier. Clamp fasteners are serviced with the adapter removed.
4. At each of the four positions, place a lower TPU half on the adapter, flange downward. Fit the carrier onto the four waists, add upper halves flange upward, and insert the four metal sleeves. The two TPU halves meet inside each Ø9 carrier hole. Their free stack is 8.4; sleeves limit the installed stack to 8.0. Each flange compresses nominally2.7→2.5. Do not use bolt tension to tune stiffness beyond the sleeve stop.
5. Install the four M3×20 bolts with the **large Ø14 top retention washers**, lower Ø7 washers and nyloc nuts. Access the lower nuts through the Ø10 recesses using a socket whose outside diameter is ≤9. The socket approaches clear the32 mm clamp width. The top washers rest on sleeve ends and TPU, **not** on the rigid carrier.
6. Tap the thumbwheel and cage pilot holes. Push the M4×35 hex-head screw into the thumbwheel: its head seats7 mm behind the wheel's front face (y=-45 in mount coordinates). Fasten the solid head cover with two M2×6 screws. This cover positively retains the bolt; adhesive is not the retaining element.
7. Feed that screw through the carrier cage back and the rail's end-stop clearance hole. Fit `cage_front` with four M3×10 screws. The wheel's Ø18 body and head cover pass through the Ø18.6 aperture, while its Ø22 flange remains captured. It has9 mm axial withdrawal travel. Verify free turning, full withdrawal and positive retention before installing the case.
8. With the screw withdrawn, bring the case from the **+Y/top end** and slide it **toward -Y** into the dovetail. Its lower rail face stops at y=-16. Turn the captive wheel by hand into the external M4 insert until the case bears against this stop. Release requires deliberate unscrewing, withdrawal and sliding toward +Y. It does not rely on a snap latch.
9. Attach the independent tether around the bar. Route USB with a relaxed service loop allowing all isolation travel. The cable and tether must not pull the floating case back against the rigid base.

## Detach and inspect

Unplug USB, release the tether's detachable connection if fitted, unscrew the wheel completely from the case insert and pull it forward to its captive stop. Slide the case **+Y**, away from the wheel. Rail disengagement needs36.35 mm travel; reserve60 mm clear travel and room for hands/hood/cable. Lift the case clear. The wheel and its bolt remain on the bike.

Before each prototype ride, check clamp grip, shell closure, cage screws, rail locking, tether, cable slack and TPU condition. Replace split, softened or permanently deformed parts. Replace damaged plastic screw threads rather than treating adhesive as a substitute for positive retention. The metal sleeves and washers retain the carrier after TPU loss but do not establish a tested fail-safe load rating.
