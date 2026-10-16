# Review of the independent design

Reviewed 2026-10-16 from editable CAD, STL exports, manufacturer geometry, assembly/motion checks and actual-geometry renders. This was an independent second session through the same authorized launcher/model, not a different-model benchmark. It was interrupted before completing its reports, BOM and handoff. [Recovery provenance](../automation/README.md) records that boundary.

**Disposition: viable alternate fit prototype after corrections.** Its removable board tray, four one-piece grommets and purchased thumbscrew are useful alternatives. It requires more space/material than the original and assumes usable factory board threads, so I would print it second unless tray mounting is the priority.

## Problems found and corrected

1. **Gasket collision:** each of four hard screw seats overlapped the seam by 1.090166 mm³ (4.360665 total). Moving paired closure holes/seats0.5 outward toX±34.5,Y±43.5 leaves them inside the case outline and clear of the unbroken seal. No intersection tolerance was relaxed.
2. **Corner touch access:** the straight45×59.4 aperture obstructed a16 mm spherical fingertip by about 107.36 mm³ at each LCD corner. A48.2×62.6 opening, flare above the bond plane, and coordinated49×65 adhesive opening now clear that same proxy while retaining2 mm adhesive lands. This does not establish capacitive touch through0.5 sheet plus0.2 air.
3. **Firmware orientation:** the original clamp aligned the board in portrait. The firmware uses landscape rotations1/3. Review turned the clamp axis in native CAD, moved the hood to the landscape top, and rotated display assemblies −90° about Z. Native part coordinates remain documented; the lock/removal direction is now screen right.
4. **Hardware interference:** full installed assembly checks found large lower clamp washers cutting into curved clamp material; flat spotfaces were added. Rotated clamp hex traps now have correspondingly rotated nut proxies. Hood reliefs clear closure screw heads and drivers. Tray/cap nuts were added to the assembly evidence.
5. **Stack and retention:** the archived large-washer drawing is1.1 thick, whereas the interrupted model used1.2. Isolation/stop elevations now derive from the 1.1 stack. M3 travel-stop screws grew 16 → 18 to engage the full nut. Compressed liners are used in installed assemblies, free liners in printable exports.
6. **Missing handoff:** completed 23 STL exports, three STEP assemblies, purchased-sheet templates, seven previews including a bushing cutaway, BOM/assembly/printing/evidence docs, pinned environment and a fail-on-validation rebuild script. Assembly STEP uses labelled board/glass proxies to avoid duplicating 170 MB of vendor detail in every view; actual vendor STEP remains archived and drives fit checks/renders.

Validation was expanded to compare mesh dimensions/volume with source, verify source/export hashes, check every non-electronic installed pair for all variants, include the retracted screw in slide motion, sweep full thumb withdrawal, check corner access and head clearance, measure selected local sections and preserve reference hashes. Board installation uses a conservative **continuous swept box for every vendor component**; the tray remains sampled at16 positions. A review-introduced loop variable shadowed the board in the window-gap test; this was fixed as a test bug, leaving the 0.2 mm criterion unchanged. No known geometry failure is waived as an acceptable overlap.

## Evidence against the brief

| Criterion | Finding / evidence | Remaining limitation |
|---|---|---|
| Electronics |451 vendor solids checked against rear, bezel, tray and window for all variants; populated-tray insertion reviewed. Factory support pitch41×60; thread engagement3 nominal. |Actual pillar thread/depth and board revision unknown; unsuitable screws could damage the unit. |
| USB |16×9 opening;14×7×24 housing proxy and insertion sweep;1.6695 web to seam. |Housing stops1.62 before receptacle mouth; cable nose reach and full engagement unproven. |
| Sealing |Purchased continuous silicone ring1→0.7, tongue overlap, external closure holes, continuous window bond, closed buttons. |Seal squeeze uniformity, adhesive chemistry, FDM porosity and open USB prevent a waterproof claim. |
| Screen |Landscape layout, clear0.5 sheet,0.2 air gap; full viewing prism and corner finger proxy clear. |Touch/gloves, reflections, glare and thermal expansion need real testing. |
| Hardware |Metal DIN464 thumbscrew retained by cap; M4 receiver nut held by separate keeper. All major hardware proxies checked for collisions. |Keeper plastic threads, head tolerances, hand grip and real nut insertion/access untested. |
| Detachment |52 mm dual T rails,59 mm disengagement;62 mm sampled sweep includes cap and withdrawn screw.10.5 screw travel,1.5 tip clearance. |Requires right-side clearance on the actual bike; combined motion and user ergonomics not qualified. |
| Clamp |Two-piece40 mm clamp,1→0.8 liners and2 mm rigid gap; no control removal. |22.2 diameter/40 straight length assumed. No independent angle adapter; set sightline by clamp rotation. |
| Isolation |Four12 mm barrel grommets,11.4 metal sleeves;2.2 radial metal gap,2.7 axial flanges.0.7 motion clear,1.2 engages stops. |Nominal compressed geometry is not an elastomer simulation. Washer capture1.3 radial overlap has no tested failure load. |
| Printability |23 valid single-solid files;14 pieces for sun. Three hood variants, touring adds5 cavity depth, all share mount. Rear3, local nut floor1.5, tongue1.6, hood2.8/relief1.3. |No print/slicer trial, global wall scan, strength or fatigue analysis. TPU flange insertion and support cleanup need coupons. |

The final run reports **175/175 PASS**; see [generated validation](validation/VALIDATION.md) for the complete check list and [raw evidence](validation/checks.json) for every result. Counts are more granular than the original script and cannot be used as a score. [Common review measurements](../automation/review-evidence.json) are the basis for size/material comparisons.

## Render inspection and remaining work

I inspected assembled front/rear, exploded assembly, all variants, populated rear, isolation section and detailed bushing cutaway. The revised images show the landscape hood, accessible screw reliefs, right-side captive cap, separated TPU stack and positive capture. Section images deliberately remove surrounding material; the bushing detail shows metal staying clear of the rigid carrier. The clear sheet is drawn frosted blue for depth, not as a claim it is opaque or printed. Native electronics/section views use different orientation from installed previews, as labelled in the documentation.

The sun version's rigid CAD volume is222.97 cm³ versus163.41 for the original; the face is96×78 versus90×74. Its14 printed pieces are fewer than20, but do not imply fewer fasteners or a proven stronger mount. Its67.5 mm nominal glass height above the bar axis is close to the original's68.96 at20°; neither is a measured bike installation.

Start with rail/holes/USB and a single isolator coupon, then verify factory pillar threads before printing the tray and compact shell. Test added-window touch early. Measure bar/controls and rightward slide clearance before the full clamp/carrier. Confirm seal witness marks with electronics removed, then retention, thermal creep, TPU fatigue, loosening and actual vibration before incremental tethered ride trials. No physical print, fit, sealing, touch, temperature, vibration, fatigue or ride test occurred during this review.
