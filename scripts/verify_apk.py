#!/usr/bin/env python3
"""Validate built APK identity, signer and embedded version using Android tools."""
import os
from pathlib import Path
import re
import subprocess
import sys

apk, expected_package, expected_cert = sys.argv[1:]
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0'
badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', apk], text=True)
match = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", badging, re.MULTILINE)
if not match or match.group(1) != expected_package or not re.fullmatch(r'[1-9][0-9]{0,17}', match.group(2)) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._+-]{0,127}', match.group(3)):
    raise SystemExit('APK package or version invalid')
verify = subprocess.check_output([str(tools / 'apksigner'), 'verify', '--print-certs', apk], text=True)
certs = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$', verify, re.MULTILINE)
if len(certs) != 1 or certs[0].lower() != expected_cert:
    raise SystemExit('APK signer mismatch')
with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
    output.write(f'version={match.group(3)}\nbuild={match.group(2)}\n')
print(f'Verified {expected_package} package, version and signer')
