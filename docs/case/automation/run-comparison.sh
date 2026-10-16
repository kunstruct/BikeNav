#!/usr/bin/env bash
# Invoked by an interactive bash so the user's existing codex-work function is available.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.."
if [[ -f docs/case/codex-work-models/SELF-EVALUATION.md || -f docs/case/assistant-models/SELF-EVALUATION.md ]]; then
  printf "%s\n" "Reviewed deliverables exist. Use each design rebuild.sh; do not rerun the historical design launcher." >&2
  exit 1
fi
type codex-work >/dev/null
run_dir="$(git rev-parse --absolute-git-dir)/bikenav-model-comparison"
mkdir -p "$run_dir"
exec 9>"$run_dir/runner.lock"
flock -n 9 || exit 0
date -Is > "$run_dir/started.txt"
trap 'printf "%s\n" "$?" > "$run_dir/exit-code.txt"' EXIT

design_status=0
codex-work exec --yolo -m gpt-6-astra -c 'model_reasoning_effort="xhigh"' \
  --json -o "$run_dir/independent-result.md" \
  'Read docs/case/automation/independent-design.md and complete that assignment autonomously.' \
  > "$run_dir/independent-events.jsonl" 2> "$run_dir/independent-stderr.log" || design_status=$?
printf '%s\n' "$design_status" > "$run_dir/independent-exit-code.txt"

# Do not relocate or review the first designer's files while it is still writing them.
deadline=$((SECONDS + 21600))
while ! python3 - <<'PY'
import json
from pathlib import Path
p = Path('/tmp/bikenav-cad-session.jsonl')
if not p.exists():
    raise SystemExit(1)
completed = False
for line in p.read_text(errors='replace').splitlines():
    try:
        event = json.loads(line)
    except json.JSONDecodeError:
        continue
    if event.get('type') in ('turn.completed', 'turn.failed'):
        completed = True
raise SystemExit(0 if completed else 1)
PY
do
  if (( SECONDS >= deadline )); then
    printf '%s\n' 'Review blocked: first session did not report completion within six hours.' > "$run_dir/review-blocker.txt"
    exit 1
  fi
  sleep 30
done

codex-work exec --yolo -m gpt-6-astra -c 'model_reasoning_effort="xhigh"' \
  --json -o "$run_dir/review-result.md" \
  'Read docs/case/automation/review-both.md and carry out the complete review, corrections, comparison, commit and push assignment.' \
  > "$run_dir/review-events.jsonl" 2> "$run_dir/review-stderr.log"
date -Is > "$run_dir/completed.txt"
