# Design sessions, recovery and review audit

## Provenance

The user authorized two independent designs using GPT-6 Astra with extra-high reasoning through the available `codex-work` launcher, followed by review and scoped commit/push. Folder names identify the design sessions, not a model benchmark. No new designer session was launched during recovery.

| Run | Session | Original output / outcome |
|---|---|---|
| Original | `01a1416d-3544-73b1-ab58-e0034198b17c` | `docs/case/detachable/`; usage-limit `turn.failed`; renamed to `codex-work-models` during review |
| Independent | `01a1417e-9cb3-7e51-aa54-73c8d1b3c681` | `docs/case/assistant-models/`; usage-limit `turn.failed`; missing handoff documents |
| Scheduled review | `01a1418d-b55e-7162-aa77-f55be0b77b4e` | Failed at the same account limit before reviewing |

Recovery/review was performed by the supervising Codex session on 2026-10-16. Original raw events were read from `/tmp/bikenav-cad-session.jsonl` and `.git/bikenav-model-comparison/independent-events.jsonl`; scheduled-review failure was read from `.git/bikenav-model-comparison/review-events.jsonl`. Logs and credentials are not committed. The expected completion messages were not treated as proof of success.

[Baseline artifact SHA-256 manifest](recovery-baseline-sha256.json) records both trees before correction. Private copies were retained under `.git/bikenav-model-comparison/recovery-baseline/` for diagnosis; these are not tracked deliverables. The original 31/31 PASS report preceded its last aperture revision; a review rerun reproduced 31/32 FAIL. The independent partial log showed gasket/bezel overlap 4.360665 mm³. Neither interrupted run was described as complete or validated.

[Review diagnosis source](review-geometry.py) and [numeric evidence](review-evidence.json) reproduce the compact bounding-box error, original gasket-seat collisions, corner finger obstruction and common final size/volume metrics. [Original self-evaluation](../codex-work-models/SELF-EVALUATION.md) and [independent self-evaluation](../assistant-models/SELF-EVALUATION.md) enumerate the corrections. Historical assignments retain their original paths as provenance; current build instructions use the reviewed directories.

## Actual completed verification

Each command below completed with exit status 0 in its design's recorded Python 3.12.15 environment. Exports preceded validation; affected meshes/STEP and previews were regenerated after geometry changes. The final independent validation also fixed a review-introduced variable-shadowing error in its board-gap check; no tolerance was relaxed to pass it.

| Operation | Original | Independent |
|---|---|---|
| CAD / STL / STEP export | `source/design.py`;22 printable files,3 assemblies | `cad/build.py --skip-preview`;23 printable files,3 assemblies |
| Cutting patterns | `source/make_templates.py` | `cad/make_templates.py` |
| Reference remeasurement | `source/inspect_references.py` | `cad/measure_references.py` |
| Final validation | `source/validate.py`: **33/33 PASS** | `cad/validate.py`: **175/175 PASS** |
| Geometry rendering | `source/render.py` under Xvfb:7 images | `cad/render.py` (starts Xvfb):7 images |
| Common review | `review-geometry.py`: diagnoses and metrics reproduced | Same common report |

Both original reference STLs retain SHA-256:

```text
81ea3e4ac43428d953742b317cc0462f1be3008bd1b2517be56fc48069834902  MiniNav-Frame-with-button.stl
aa613461c9aa7788da97889b617161bbe09f407006efcf66ef158b8e5e236d27  MiniNav-back-panel-with-strap-slits.stl
```

All 45 printable meshes pass watertightness, winding, positive volume, single-component and dimension/volume checks. Installed major-hardware checks cover 2850 non-reference pairs for each original adapter angle and 3240 non-electronic pairs for each independent variant. These counts describe tested geometry, not confidence scores. Small tapped retaining screws/threads are simplified or documented separately; no thread tolerance, pullout or strength proof is implied.

Final preview inspection covered both assembled/rear/exploded sets, all variants, sections, and isolator details. Inspection led to removing stray case screws from the original case-removed view and unrelated clamp washers from independent section views. Clear sheets are visually tinted; CAD section views deliberately remove geometry. [Contact sheets](../previews/) are generated from these actual renders, at independent image scales.

## Reproduce

Create separate environments from each design's pinned dependencies, then follow its [original rebuild instructions](../codex-work-models/README.md#reproduce) / [independent rebuild instructions](../assistant-models/README.md#rebuild). Environments differ in NumPy/Trimesh/plotting versions. They were reused and recorded during recovery; a clean-machine install, slicer run and physical print were not performed.

After both rebuilds, from the repository root:

```sh
# Use the original design's environment (or equivalent recorded CAD stack).
docs/case/codex-work-models/.venv/bin/python docs/case/automation/review-geometry.py
docs/case/codex-work-models/.venv/bin/python docs/case/automation/make-preview-sheet.py
```

`run-comparison.sh` is the historical two-session launcher, not a geometry rebuild command. It now refuses to overwrite reviewed trees. `independent-design.md`, `review-both.md` and the parent brief document the authorized scope. No external notification channel was used.

## Publication scope and physical limits

Publication is isolated from the user's existing research/helper work: a dedicated design branch is based on `origin/main` in a separate Git worktree. Only the brief, these audit/review files, both reviewed design trees and comparison/previews are staged. Reference STLs are unchanged; firmware, Android helper, research documents and unrelated files are not staged. Virtual environments, caches, generated raw logs and redundant downloaded web/archive files are excluded. The final supervising response records the actual branch/commit/push outcome; this document does not pre-claim a push.

No physical tests were performed. Actual bar fit, board revision/thread depth, cable reach, touch/gloves, adhesive and seal behavior, slicer/print quality, temperature creep, clamp grip, capture strength, vibration, fatigue and ride safety remain unverified. There is no claimed waterproof rating, load rating, vibration attenuation, torque or service life.
