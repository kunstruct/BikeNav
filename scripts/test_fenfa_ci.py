"""Check Android release trust gates and APK verification without Android builds."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
CERT = 'a' * 64
class AndroidCI(unittest.TestCase):
    def test_workflow_trust_gates(self):
        workflow = (ROOT / '.github/workflows/android-release.yml').read_text()
        for text in ("github.repository == 'kunstruct/BikeNav'", "github.event_name == 'push'", "refs/heads/main", "refs/heads/drakknightmc/strava-biking", "github.run_attempt == '1'", 'tools/build_tracker.sh', 'assembleRelease lintRelease', 'scripts/verify_apk.py'):
            self.assertIn(text, workflow)
        self.assertNotIn('pull_request:', workflow)
        self.assertNotIn('FENFA_ADMIN_TOKEN', workflow)
        self.assertNotIn('          HOME:', workflow)
        self.assertIn('ANDROID_USER_HOME: ${{ runner.temp }}/tracker-android', workflow)
        self.assertEqual(workflow.count('$ANDROID_USER_HOME/debug.keystore'), 4)
        self.assertEqual(workflow.count('sdkmanager" --install'), 2)
        self.assertIn('cancel-in-progress: false', workflow)
        for block in workflow.split('      - name: '):
            if '${{ secrets.FENFA_UPLOAD_TOKEN }}' in block:
                self.assertTrue(block.startswith('Publish '))
            if 'FENFA_TRACKER_KEYSTORE_B64:' in block:
                self.assertTrue(block.startswith('Build and lint tracker'))
            if 'BIKENAV_HELPER_KEYSTORE_B64:' in block or 'BIKENAV_STORE_PASSWORD:' in block or 'BIKENAV_KEY_PASSWORD:' in block:
                self.assertTrue(block.startswith('Build and lint helper'))

    def test_apk_package_and_signer_must_match(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); tools = root/'build-tools/35.0.0'; tools.mkdir(parents=True)
            aapt = tools/'aapt'; aapt.write_text("#!/bin/sh\necho \"package: name='com.example.app' versionCode='4' versionName='0.4.0'\"\n"); aapt.chmod(0o755)
            signer = tools/'apksigner'; signer.write_text('#!/bin/sh\necho "Signer #1 certificate SHA-256 digest: '+CERT+'"\n'); signer.chmod(0o755)
            env = dict(os.environ, ANDROID_HOME=str(root), GITHUB_OUTPUT=str(root/'output'))
            cmd = ['python3', str(ROOT/'scripts/verify_apk.py'), 'fake.apk', 'com.example.app', CERT]
            self.assertEqual(subprocess.run(cmd, env=env, capture_output=True).returncode, 0)
            self.assertNotEqual(subprocess.run(cmd[:-2] + ['com.example.wrong', CERT], env=env, capture_output=True).returncode, 0)
            self.assertNotEqual(subprocess.run(cmd[:-1] + ['b'*64], env=env, capture_output=True).returncode, 0)
            aapt.write_text("#!/bin/sh\necho \"package: name='com.example.app' versionCode='0' versionName='0.4.0'\"\n")
            self.assertNotEqual(subprocess.run(cmd, env=env, capture_output=True).returncode, 0)

if __name__ == '__main__': unittest.main()
