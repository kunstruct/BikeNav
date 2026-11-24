# Android publishing

The independent tracker and helper jobs build and lint on Ubuntu 24.04 with
JDK 17 and Android SDK/build-tools 35, then inspect the actual APK package,
version and signing certificate before publishing. Their project-managed version
codes are preserved; Android updates require intentional version progression
and do not accept downgrades. Check the Actions run for the pushed commit;
a successful local build alone does not confirm CI publication.

The publishing workflow, its verification/publishing scripts and the tracker
source/build helper are included on `drakknightmc/strava-biking`. Main needs
both apps and workflow changes merged before its pipeline can work. Follow
the release source-control rules in [AGENTS.md](../AGENTS.md): validate,
commit and push release source before delivery/publication, then check CI.
Preserve
the current debug keystore for the tracker and release keystore for the helper;
the workflow verifies their known certificate fingerprints before publication.

Configure trusted repository secrets `FENFA_TRACKER_KEYSTORE_B64`,
`BIKENAV_HELPER_KEYSTORE_B64`, `BIKENAV_STORE_PASSWORD`, `BIKENAV_KEY_PASSWORD`,
`FENFA_UPLOAD_TOKEN`, `CF_ACCESS_CLIENT_ID` and `CF_ACCESS_CLIENT_SECRET`,
and variables `FENFA_TRACKER_VARIANT_ID` and `FENFA_HELPER_VARIANT_ID`, only
after the endpoint and Fenfa variants are provisioned. Never put the admin token
in CI. Native upload credentials have global upload scope across variants.

`scripts/fenfa_publish.py` is a byte-for-byte copy of the homelab-infra
`services/fenfa/publish.py`, SHA-256
`6ff4e88b4ab6cd1a93e9107d84c3412bf79cb9ca3df3e71bb709ed74cce947f5`.
An interrupted request may have succeeded: reconcile in Fenfa before a new
publish; the publisher refuses GitHub run attempts above one. Only trusted
pushes to main and the specified development branch run these jobs; PRs and
forks do not receive publishing or signing credentials.
