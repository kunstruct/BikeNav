#!/usr/bin/env bash
set -euo pipefail
CAD_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
CAD_PYTHON="${CAD_PYTHON:-python3}"
"$CAD_PYTHON" "$CAD_DIR/cad/build.py" --skip-preview
"$CAD_PYTHON" "$CAD_DIR/cad/make_templates.py"
"$CAD_PYTHON" "$CAD_DIR/cad/validate.py"
"$CAD_PYTHON" "$CAD_DIR/cad/render.py"
