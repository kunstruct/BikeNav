#!/usr/bin/env bash
set -euo pipefail
CAD_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
CAD_PYTHON="${CAD_PYTHON:-python3}"
"$CAD_PYTHON" "$CAD_DIR/source/design.py"
"$CAD_PYTHON" "$CAD_DIR/source/make_templates.py"
"$CAD_PYTHON" "$CAD_DIR/source/validate.py"
if command -v xvfb-run >/dev/null 2>&1; then
  xvfb-run -a "$CAD_PYTHON" "$CAD_DIR/source/render.py"
else
  "$CAD_PYTHON" "$CAD_DIR/source/render.py"
fi
