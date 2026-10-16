# Detachable handlebar enclosure — design brief

## User requirements

Design a printable enclosure and handlebar mount for the MiniNav electronics in this repository. The bike is a Himalayan 411 with an Art of Motorcycles handlebar. Installation will probably be near the bar centre. Exact bar diameter and available straight length have NOT been measured: make these parameters and clearly label defaults as assumptions. The user will work out cable routing.

Use the original `MiniNav-Frame-with-button.stl` and `MiniNav-back-panel-with-strap-slits.stl` in this directory as dimensional references. Original source: https://github.com/MiniNavDevice/MiniNav/tree/main/docs/case . Inspect the actual meshes and repository hardware documentation; do not guess the electronics dimensions or infer them solely from outer mesh bounds.

The enclosure must be fully enclosed when assembled, with USB-C as its only open penetration. Remove strap slots. Accommodate the touchscreen with a sealed transparent window/film or a properly sealed existing screen interface; distinguish purchased transparent material from opaque printed parts. Touch response through an added cover requires physical testing. Button access, if retained, must use a sealed membrane. Closed assembly seams are acceptable. Do not claim waterproof certification.

The case should detach from the bike without tools, have a modest sun hood, and allow inexpensive customization. Provide editable parametric CAD and actual exported STL files, not just code or images. Prefer compact, sun-hood, and deeper touring variants sharing one mounting interface, if practical. PLA is for initial prototyping and fit testing, TPU for dampers, PETG/ASA for later rigid prints. All deliverable files belong in this repository; preserve existing STLs and unrelated work.

## Agreed starting architecture (refine if engineering requires)

1. Two-piece bolted handlebar clamp with thin rubber/TPU liner; installation without removing grips/controls.
2. Short, rigid base with replaceable captured TPU bushings supporting a floating carrier. Four separated bushings were proposed to control wobble. Clamp liner primarily provides grip; separate bushings provide compliance.
3. Sleeves/shoulders control bushing compression; bolts must not bridge rigidly to the floating carrier. Include retention if TPU fails and travel stops for large bumps. No unsupported claim of measured vibration reduction.
4. Short slide rail and positively retained captive thumbscrew for detachable case. Check accessible tightening, insertion/removal path, retention, and captive hardware geometry. Avoid depending only on a printed snap latch.
5. Enclosed front/rear case with overlapping seam and feasible gasket/seal path. External mounting features must not penetrate the electronics cavity. Include an external tether eye. Keep the centre of mass close to the bar and provide a practical screen angle, preferably replaceable fixed-angle adapters.
6. Initial modest hood concept: approximately 15–20 mm top projection, shorter side wings; tune for screen visibility, touchscreen finger access, printability, and actual dimensions. Optional compact and deeper variants.
7. USB-C opening sized for plug housing and insertion, not just metal receptacle. Cable slack must allow isolation movement.

## Deliverables and engineering checks

- Editable CAD source, documented parameters, reproducible export command and pinned/recorded dependencies.
- Individual printable STL parts in millimetres, sensible print orientation, and assembled/exploded previews rendered from actual geometry.
- Bill of materials with hardware sizes/counts, clear assembly instructions, print guidance for PLA/TPU and later PETG/ASA, and adjustment instructions.
- Evidence of original case measurements, coordinate system, electronic keep-outs and USB/button/screen locations; explicitly mark unverified measurements and board revision assumptions. Use authoritative board drawings when available.
- Validate meshes for watertightness, positive volume, expected connected components and dimensions. Check wall thickness, clearances, fastening engagement, tool access, assembly sequence, rail insertion, clamping gap, gasket continuity and vibration-isolation load paths. Include useful clearance coupons if dimensions require tuning.
- Distinguish computationally checked geometry from physical fit, sunlight, thermal, vibration and ride testing still needed. PLA road prototypes require appropriate caution and backup retention; avoid invented load ratings or specified bolt torques without evidence.
- Do not alter firmware or unrelated files. Do not commit or push yet: the supervising assistant will review the output and perform the authorized commit/push after corrections.

## Delegation

The user explicitly requested a separate Codex session with approval/sandbox bypass and authorized model selection, design implementation in this repository, and eventual push. They clarified that the available `codex-work` launcher should be used in place of the unavailable `codex-kunstruct` launcher. The supervising assistant is responsible for review.
