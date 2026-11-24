# Project instructions

## Shared project knowledge

Use the connected `brain-mcp` server for shared project knowledge and session records. For non-trivial tasks, call `check_related` before assuming what exists. Generate a UUID and call `start_session` for each task worth remembering. Before finishing, call `end_session` with that ID, a concise transcript of the actual work and a factual summary including remaining blockers. Do not call a recap a full transcript. Pass `CLAUDE_CODE_SESSION_ID` as `parent_session_id` when set. Keep secrets out of records and report failed saves honestly.

## Planning

When Drak says “plan it”, use ItsAPlan to find or create the project and a draft plan after checking existing work. Record scope, assumptions, decisions, tasks, dependencies and acceptance checks. Interview Drak about consequential unresolved questions before implementation; execution must be authorized, and “plan it and do it” authorizes execution after the interview. Keep status and results current under Codex supervision. Treat unattended ItsAPlan-to-Orca scheduling as a separate future phase until recovery and supervision are verified. If access is unavailable, say so instead of claiming the plan was saved.

## Release source control

Drak requires releases to include a source-control checkpoint. When creating a release:

- Commit and push the working source, tests, build configuration and release documentation to the current branch before delivering or publishing the release. A release request authorizes this commit and push; do not ask separately.
- Run the relevant checks and review the staged files. Keep signing keys, credentials, local SDK settings, build caches and generated APKs out of source control; publish binaries through the configured release workflow.
- Verify the remote branch contains the release commit and the working tree is clean. Report the commit and any remaining validation or publishing failures accurately.
- Preserve other contributors' work. Do not discard changes or rewrite remote history to obtain a clean tree.

## Release builds and distribution

Drak's system-wide release direction, clarified 25 November 2026:

- Build, test, lint, package and sign locally wherever possible. Avoid GitHub-hosted Actions build minutes and other cloud build compute; upload finished artifacts instead.
- Build once and upload the exact same verified files to both GitHub Releases and Fenfa for now. Verify checksums at both destinations; do not rebuild separately for each host.
- Preserve source revision/tag, app version/build code and signing identity. The commit/push requirement above still applies.
- GitHub Releases is the long-term alternative; retiring Fenfa is a later migration. Keep existing artifacts and shared services intact until that migration is authorized and verified.
- The existing hosted/Fenfa workflow still needs migration to this local dual-publish process; recording the policy does not itself change that workflow. Do not silently fall back to cloud builds when local compute is unavailable.
