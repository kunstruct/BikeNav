# Printing, tuning and physical checks

Use **PLA only for initial bench/fit prototypes**. Its temperature and creep behaviour make a sun-exposed motorcycle clamp an unqualified application. Do not treat an indoor fit as evidence for riding. A road prototype needs a suitable rigid material, an independent tether, static proof testing and staged inspection. PETG or ASA is the intended later rigid material; each still needs actual heat, creep and vibration evaluation. TPU is for liners/bushings; a continuous silicone sheet gasket is preferable to assuming an FDM TPU ring is leak-tight.

## Starting print setup

These are proposed process settings, not tested print results:0.4 nozzle, 0.2 layer, 5–6 perimeter walls and at least5 top/bottom layers for rigid load-bearing parts;40–60% infill, with solid regions around rail, clamp ears, nut pockets and sleeve seats. Do not let sparse infill replace the CAD's thin structural sections. Use0.15–0.2 layers on the rail/cage parts if your printer benefits from them. Print TPU slowly using the supplier's settings; begin with 95A and solid walls/infill for the bushings. Stiffness and grip are unmeasured. Softer or hollow bushings are a separate experiment.

Each STL is in mm and rests on Z=0, with the orientation recorded in [parts-manifest.json](reports/parts-manifest.json). Do not scale the STLs to tune an individual fit; change the corresponding parameter and regenerate.

| Part | Exported orientation and support considerations |
|---|---|
| Fronts | Rear tongue/rim down, hood grows upward. Support the underside bezel overhang and nut-pocket roofs as the slicer requires; keep bond land clean. No bridge over the whole display opening is intended. |
| Rear | Male rail foot down, cavity open upward. Support the exterior floor around the raised rail and tether. The rail narrows upward; avoid support scarring its flanks. Do not fill the electronics cavity with support by printing it upside down. |
| Carrier | Lowest plate/cage surfaces down. Support female dovetail overhangs and cage underside; channel is open at+Y for removal of support. Do not leave support in the closed rail end or screw passage. |
| Cage front | Rear face down. Inspect the continuous capture edge around the18.6 aperture and all four screw lands. |
| Thumbwheel | Flange down. Clean the hex pocket and tapping pilots. Check that the bolt head is fully seated before fitting the cover. |
| Thumbwheel cover | Flat face down. Two tapped fasteners retain it; do not omit the cover. |
| Clamp halves | Bar axis vertical, axial end on bed. Gives a round bore profile; inspect layer adhesion at ear transitions and support the nut slots. Use a brim if needed. |
| Angle adapter | Flat underside down. The pockets open downward and their roofs will need support/bridging chosen by the slicer. Clear the four angled socket recesses and two vertical head access bores. |
| Liner halves | Bar axis vertical. Flexible lip overhangs may need local support or careful cooling. No supports on the gripping surface if avoidable. |
| Bushing halves | Large flange down, waist upward;8 copies. Hole must admit the sleeve without cutting TPU. Use the same material/process for all four pairs. |
| Gasket | Flat, 6 layers at 0.2 gives1.2. Inspect the entire loop for pinholes and seams; gasket mesh validity alone says nothing about sealing. |

Layer adhesion and support removal may be the controlling weaknesses. Reorienting can be appropriate for a particular process, but recheck fit and mechanical behaviour. Do not claim these suggested settings constitute a tested load rating.

## Print coupons before the full assembly

1. `coupon_rail_male` and `coupon_rail_female_0p2`, `_0p35`, `_0p5`: same rail profile, 12 mm long. The suffix is profile-coordinate clearance in mm. Choose the smallest pair that slides freely without rocking after support/elephant-foot cleanup. The default full carrier uses0.35. A longer32 mm rail may bind more than the coupon; retest the real part. Heat-set installation must not distort the rail.
2. `coupon_usb_plug`: the actual16×8, R1 opening in a3 mm wall. Pass the intended **housing**, not only its metal connector. The full enclosure adds a recessed path to the board; also check mating reach and cable strain relief in the assembled case. The model validates a14×6, R1 housing sweep.
3. `coupon_holes_insert`: with the exported top face upward, left-to-right3.4 bolt clearance, 5.6 insert pilot and 9.0 grommet hole. Confirm with callipers; test heat insertion and removal loads in spare material. Verify your chosen M4 insert's recommended pilot and final alignment. The coupon is 5 thick, so a6 mm insert projects below it: use a scrap support with clearance; this tests diameter, not blind seating depth.
4. Print two bushing halves and trial-fit a real8.00 sleeve, Ø14 washer and scrap3 mm plate before making all eight. Free stack should be8.4 and controlled installed stack8.0. Check flange squirm and hole clearance instead of forcing it together.
5. Check the bar liner and split clamp on the measured bar before finishing/sealing the electronics. Desired grip must coexist with a remaining clamp gap; the nominal model is not a torque instruction.

## Inspection and test record to complete physically

Record the printer, filament batch, temperatures, layer direction, support method, actual dimensions and hardware supplier for each iteration. The following remain **not performed**:

| Check | Measurements / result needed |
|---|---|
| Motorcycle geometry | Bar diameter at several points, taper/ovality, ≥35 mm straight length for defaults, rotation range and full-lock control/tank clearance |
| Board revision | Exact product/revision, glass dimensions, factory support pitch and depth, connector and button positions; compare against2024 STEP |
| Plug | Housing width/height, nose length, cable exit/bend radius, full seating, no load on receptacle |
| Enclosure | Board retention without glass pressure or button actuation, six nut/screw engagement depths, shell hard-stop closure |
| Window | Touch taps/drag at all corners, gloved use if required, glare, film scratching, bubble/adhesive creep, optical clarity |
| Seal | Continuous film bond and gasket contact, corner compression, print porosity; bench ingress test with electronics removed. USB remains intentionally open. |
| Rail/capture | Full insertion, hand tightening, positive end-stop contact, release/captive retention, ≥60 mm removal envelope, wear and dirt sensitivity |
| Isolation | All sleeves seated, washers clear carrier at rest, static sag, wobble, combined-axis stops, cable/tether slack, TPU permanent set and fatigue |
| Clamp | Grip without bar damage or gap collapse; inspection after hot soak, cooling and vibration |
| Thermal/sun | Electronics temperatures and thermal throttling under power/solar exposure; polymer creep and adhesive softening |
| Vibration/ride | Retained mass and actual response; stationary engine run first, then controlled staged road use with independent tether and inspection. No attenuation target has been measured. |

Do not compensate for a poor fit by overtightening. Revise the dimensional parameters, gasket/pad thickness or print process, regenerate, rerun the geometric checks, and repeat the affected physical tests.
