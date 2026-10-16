# BikeNav detachable enclosure — review prototype

A complete printable design for the repository's Waveshare ESP32-S3-Touch-LCD-2.8, with a sealed film window, a split handlebar clamp, four captured TPU bushings, a slide rail and a captive release screw. **This is a geometrically checked prototype, not a physically qualified motorcycle mount.** All dimensions are millimetres. Original case STLs are preserved in the parent directory.

![Assembled sun variant](previews/assembled_sun.png)

## Start here

- [Validation results](reports/VALIDATION.md), [machine-readable measurements and checks](reports/validation.json).
- [BOM and assembly](ASSEMBLY.md), [print and fit guidance](PRINTING.md), [parameters and measurements](ENGINEERING.md).
- [Printable STLs](stl/): 22 individual files, each a single closed solid, already oriented on Z=0. Print one front and one angle adapter, not every variant. The BOM specifies repeated TPU parts.
- [Editable Python CAD](source/design.py) and [parameter data](source/parameters.json). [STEP files](step/) retain design coordinates, including three complete assemblies, all printable parts, and two **purchased-material** reference solids.
- [Exploded view](previews/exploded_sun.png), [mount without case](previews/isolation_mount.png), [rear view](previews/rear_mount.png), [section](previews/section.png).
- [1:1 cutting patterns](templates/purchased_materials_1to1.svg) for **purchased transparent PET film**, adhesive and optional silicone gasket. Do not print the window in opaque plastic.
- [Manufacturer drawing](reference/waveshare-2.8-structure.pdf), [manufacturer STEP](reference/waveshare-2.8-2024.stp), [original mesh sections](reference/original-sections.png), [coordinate map](reference/coordinate-map.svg).

## Variants

| Front | Projection beyond front face | Side wings | Common parts |
|---|---:|---:|---|
| `front_compact.stl` | 0 | 0 | Same rear, film, gasket, rail, carrier and clamp |
| `front_sun.stl` — default | 18 | 8 | Same interface |
| `front_touring.stl` | 25 | 12 | Same interface; deeper hood, not a larger electronics cavity |

Adapters of 0°, 20° (default) and 30° tilt the screen plane relative to the clamp's top plane about the handlebar axis. Rotate the clamp on the actual bar for coarse positioning, then select the fixed adapter. Measure sightline, cable and steering clearance on the motorcycle. Defaults **22.2 mm bar diameter and 35 mm clear straight bar length are assumptions**, not measurements of the Art of Motorcycles bar.

Buttons, strap holes, battery/header/card access and decorative cut-outs are closed. BOOT/RESET require opening the enclosure; touchscreen/serial functions remain available. USB-C is the only intended open penetration. A continuous bonded film covers the entire visible display opening. The shell uses an overlapping tongue and a continuous compressed gasket; screw passages sit outside the gasket. There is no waterproof/IP claim, and the open USB port prevents treating the assembly as watertight.

## Reproduce

Tested on Linux x86-64, Python **3.12.15**, CadQuery **2.6.1**, OCCT **7.8.1.1.post1**, VTK **9.3.1** and Trimesh **4.8.3**. Every Python dependency is pinned in [requirements.lock](source/requirements.lock). Xvfb was used for actual-geometry raster rendering. CAD export needs no GUI.

From the repository root, with Python 3.12 available:

```sh
python3.12 -m venv docs/case/codex-work-models/.venv
docs/case/codex-work-models/.venv/bin/python -m pip install -r docs/case/codex-work-models/source/requirements.lock
CAD_PYTHON="$PWD/docs/case/codex-work-models/.venv/bin/python" bash docs/case/codex-work-models/source/rebuild.sh
```

Alternatively use `uv venv --python 3.12.15 docs/case/codex-work-models/.venv`, followed by `uv pip install --python docs/case/codex-work-models/.venv/bin/python -r docs/case/codex-work-models/source/requirements.lock` and the same rebuild command. The implementation session used uv 0.13.1. If no graphical display is available, install the system `xvfb` package before rendering.

Individual commands:

```sh
CAD_PYTHON=docs/case/codex-work-models/.venv/bin/python
"$CAD_PYTHON" docs/case/codex-work-models/source/design.py
"$CAD_PYTHON" docs/case/codex-work-models/source/validate.py
xvfb-run -a "$CAD_PYTHON" docs/case/codex-work-models/source/render.py
"$CAD_PYTHON" docs/case/codex-work-models/source/inspect_references.py
```

The last command remeasures the unchanged originals and the archived manufacturer STEP; it is slower and is not necessary for each export. `rebuild.sh` exits unsuccessfully if validation fails. Generated STEP assemblies include simplified smooth hardware and schematic display/PCB solids; the archived manufacturer STEP and its 451 component keep-outs are the fit evidence. Rendered transparent film is tinted blue for visibility.

## Physical work still required

Measure the bar's diameter, taper, available straight length and controls clearance; identify the board revision and verify its factory support stack; measure the intended USB cable housing and neck. Print the fit coupons, then test electronics fit, glass pressure, touch through the film, rail fit, screw capture, clamp grip and the complete insertion/removal path. Test gasket compression and adhesive bonding with the electronics removed. Check sunlight visibility, solar heating, heat creep, fastener retention, TPU travel/fatigue and vibration on the actual motorcycle before progressively longer rides. Use an independent tether. No physical printing, load testing or road testing was performed for these deliverables.
