#!/usr/bin/env python3
"""Publish one artifact to the Kunstruct machine-only Fenfa endpoint."""
import argparse
import hashlib
import http.client
import json
import os
from pathlib import Path
import re
import secrets
import sys

URL = 'https://releases-upload.kunstruct.in/upload'


def publish(path, variant, version, build, env, connection_factory=http.client.HTTPSConnection):
    if env.get('GITHUB_RUN_ATTEMPT', '1') != '1':
        raise ValueError('Rerun requires operator reconciliation before publishing')
    if env.get('FENFA_UPLOAD_URL') != URL:
        raise ValueError('FENFA_UPLOAD_URL must be the approved upload endpoint')
    if not re.fullmatch(r'var_[a-zA-Z0-9]+', variant) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._+-]{0,127}', version) or not re.fullmatch(r'[1-9][0-9]{0,17}', build):
        raise ValueError('Invalid variant, version or positive build')
    credentials = [env.get(k, '') for k in ('FENFA_UPLOAD_TOKEN', 'CF_ACCESS_CLIENT_ID', 'CF_ACCESS_CLIENT_SECRET')]
    if not all(credentials) or any('\r' in v or '\n' in v for v in credentials):
        raise ValueError('Missing or invalid publishing credentials')
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._+-]{0,200}', path.name):
        raise ValueError('Invalid artifact filename')
    boundary = 'fenfa' + secrets.token_hex(16)
    fields = b''.join(f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode() for k, v in [('variant_id', variant), ('version', version), ('build', build)])
    prefix = fields + f'--{boundary}\r\nContent-Disposition: form-data; name="app_file"; filename="{path.name}"\r\nContent-Type: application/octet-stream\r\n\r\n'.encode()
    suffix = f'\r\n--{boundary}--\r\n'.encode()
    with path.open('rb') as artifact:
        size = os.fstat(artifact.fileno()).st_size
        conn = connection_factory('releases-upload.kunstruct.in', timeout=600)
        try:
            conn.putrequest('POST', '/upload')
            for key, value in [('Content-Type', 'multipart/form-data; boundary=' + boundary), ('Content-Length', str(len(prefix) + size + len(suffix))), ('X-Auth-Token', credentials[0]), ('CF-Access-Client-Id', credentials[1]), ('CF-Access-Client-Secret', credentials[2])]:
                conn.putheader(key, value)
            conn.endheaders()
            conn.send(prefix)
            digest = hashlib.sha256()
            remaining = size
            while remaining:
                chunk = artifact.read(min(1024 * 1024, remaining))
                if not chunk:
                    raise OSError('Artifact changed during upload')
                digest.update(chunk)
                conn.send(chunk)
                remaining -= len(chunk)
            if artifact.read(1):
                raise OSError('Artifact changed during upload')
            conn.send(suffix)
            response = conn.getresponse()
            payload = response.read(1024 * 1024 + 1)
            if response.status != 200 or len(payload) > 1024 * 1024:
                raise ValueError('Upload failed or returned an unexpected response')
            result = json.loads(payload)
            release = result['data']['release']
            if not result.get('ok') or release['sha256'] != digest.hexdigest() or release['variant_id'] != variant or release['version'] != version or release['build'] != int(build):
                raise ValueError('Upload response does not match the artifact and metadata')
            if not re.fullmatch(r'rel_[a-zA-Z0-9]+', release['id']):
                raise ValueError('Invalid release ID')
            return release['id'], digest.hexdigest()
        finally:
            conn.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('artifact', type=Path)
    parser.add_argument('--variant', required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--build', required=True)
    args = parser.parse_args()
    try:
        release, digest = publish(args.artifact, args.variant, args.version, args.build, os.environ)
    except (OSError, ValueError, KeyError, TypeError, http.client.HTTPException):
        print('Publishing failed; an upload may have completed. Reconcile in Fenfa before retrying.', file=sys.stderr)
        return 1
    print(f'Published release {release} sha256 {digest}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
