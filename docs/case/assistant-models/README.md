# Independent MiniNav enclosure — reviewed prototype

A second, independently started design, recovered and corrected after its design session was interrupted. **Geometric prototype only: no part has been printed or physically qualified.** See [self-evaluation and correction history](SELF-EVALUATION.md) and [comparison](../COMPARISON.md). All dimensions are mm.

![Reviewed sun assembly](previews/assembly-front.png)

- [BOM and assembly](ASSEMBLY.md), [printing and fit tests](PRINTING.md), [dimensional evidence and sources](EVIDENCE.md).
- [Validation](validation/VALIDATION.md), [numeric checks](validation/checks.json), [mesh manifest](validation/mesh-report.json).
- [Editable CadQuery](cad/model.py), [parameters](cad/parameters.json), [23 printable STL files](stl/), [neutral STEP parts and three assemblies](cad/neutral/).
- [Exploded](previews/exploded.png), [rear](previews/assembly-rear.png), [variants](previews/variants.png), [bushing detail](previews/bushing-detail.png), [electronics](previews/electronics.png).
- [Purchased clear sheet and seal cutting patterns](templates/purchased-materials-1to1.svg). The blue window is purchased transparent material, not an opaque printed part.

The reviewed option uses landscape orientation to match the current firmware. Native CAD retains the manufacturer's portrait X/Y coordinates; the clamp runs along native Y and the hood is at native −X. Assembly previews/STEP rotate the whole design −90° about Z. USB faces the screen's left side; slide removal is to the right. Do not mix native individual-part coordinates with assembled STEP coordinates.

| Variant | Rear cavity depth change | Top / side hood projection | Mount |
|---|---:|---:|---|
| compact | 0 | 0 / 0 | Common dual T rails and carrier |
| sun | 0 | 18 / 10 | Same |
| touring | +5 | 22 / 12 | Same; use its matching rear, bezel and tray |

The screen plane is parallel to the base. Rotate the clamp about the actual bar to set the viewing angle; this option has no separate fixed-angle adapter. The **22.2 bar diameter and 40 straight length are assumptions**. Measure the aftermarket bar, taper, control clearance and sightline before printing the clamp. Buttons are deliberately internal; service requires opening the enclosure. USB is the only intended open penetration into the electronics cavity. Closed screw passages outside the seal, external rail hardware and tether eye do not enter that cavity. The open USB port, FDM porosity and untested bonds prevent any waterproof claim.

## Rebuild

Recorded environment: Linux x86-64, Python 3.12.15, CadQuery 2.6.1, cadquery-ocp 7.8.1.1.post1, Trimesh 4.7.4, VTK 9.3.1. Full installed Python versions are [pinned](cad/requirements.lock). Rendering requires system `Xvfb` and DejaVu Sans fonts (on Debian/Ubuntu: `xvfb`, `fonts-dejavu-core`). CAD generation is headless.

From the repository root:

```sh
python3.12 -m venv docs/case/assistant-models/.venv
docs/case/assistant-models/.venv/bin/python -m pip install -r docs/case/assistant-models/cad/requirements.lock
CAD_PYTHON="$PWD/docs/case/assistant-models/.venv/bin/python" bash docs/case/assistant-models/cad/rebuild.sh
```

The original session used a uv-created environment without pip; `uv pip install --python <environment>/bin/python -r .../requirements.lock` is equivalent for that environment. Keep this environment separate from the original design's environment: their NumPy, Trimesh and plotting versions differ. No environment or raw session log belongs in Git.

`rebuild.sh` exports, makes templates, validates, then renders; a failed check exits nonzero. `cad/measure_references.py` separately remeasures the two preserved upstream STLs. STL coordinates are already oriented and translated to Z=0; STEP parts retain native design coordinates. Source/export fingerprints and mesh hashes reject stale exports. Default bounding boxes after loft tessellation can be inflated; exact BRep bounds are used for export/mesh comparison. Assembly STEP files use labelled glass/PCB proxies to avoid repeating the detailed vendor STEP in every assembly; the original vendor STEP drives fit validation and the electronics render. Models use nominal smooth hardware, not thread-tolerance or material-deformation simulation.

Print coupons first, then one compact enclosure and a single isolator trial. Validate the actual board thread/revision, plug reach, window touch response, clamp grip, screw capture and removal motion before progressing. No strength, vibration reduction, fatigue life, thermal suitability or IP rating has been established.
