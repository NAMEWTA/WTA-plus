#!/usr/bin/env python3
"""Read-only exact C/D/E source and C production-dist equivalence check.

This performs no build, package, browser, Docker, JVM, HTTP or source write.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path('/srv/WTA-plus')
FRONTEND = ROOT / 'frontend'
MANIFEST = Path('/tmp/wta-t41/c-frontend-artifacts.json')
C = 'eff81f642f08a1590f85a1f1f813a592a8232451'
D = 'd93eb1d8d6095464a9606619f4161b3dd4ef35c7'
MANIFEST_SHA = 'e3a32c6886aa500b1846871a26aaaf84dd0ce0f31bfec00e95b25fccc1f19d84'
TEST_SHA = '32e34f3268062628b41303903227732e609f9ad625de5f5f2ceaaeb2f5467ad2'
CONTROLLED = 'frontend/e2e/inbox-without-realtime.spec.ts'
REGISTRY = 'frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts'
E_RUNNER = 'frontend/e2e/run-inbox-paged-real.py'
E_TEST = 'frontend/e2e/test_run_inbox_paged_real.py'
APPS = {'admin-web': 310, 'home-web': 14, 'sso-web': 4}


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def git(*args):
    result = subprocess.run(('git', *args), cwd=ROOT, capture_output=True, text=True,
                            timeout=30, check=False)
    if result.returncode:
        raise RuntimeError('Read-only Git inspection failed')
    return result.stdout.strip()


def ancestor(parent, child):
    result = subprocess.run(('git', 'merge-base', '--is-ancestor', parent, child),
                            cwd=ROOT, capture_output=True, timeout=30, check=False)
    if result.returncode != 0:
        raise RuntimeError('Expected clean source ancestry differs')


def changed(before, after):
    return git('diff', '--name-only', before + '...' + after, '--', 'frontend').splitlines()


def verify(expected):
    if not re.fullmatch(r'[0-9a-f]{40}', expected):
        raise RuntimeError('E SHA must be 40 lower-case hex characters')
    if git('rev-parse', 'HEAD') != expected or git('status', '--porcelain', '--untracked-files=all'):
        raise RuntimeError('Not clean exact E source')
    ancestor(C, D)
    ancestor(D, expected)
    c_to_d = changed(C, D)
    d_to_e = changed(D, expected)
    c_to_e = changed(C, expected)
    if c_to_d != [REGISTRY] or d_to_e != [E_RUNNER, E_TEST] or c_to_e != [REGISTRY, E_RUNNER, E_TEST]:
        raise RuntimeError('C/D/E frontend changed-path allowlist differs')
    if sha(MANIFEST) != MANIFEST_SHA:
        raise RuntimeError('C production artifact manifest hash differs')
    data = json.loads(MANIFEST.read_text())
    records = data.get('files')
    if data.get('source_commit') != C or data.get('app_modes') != 'production' or not isinstance(records, list) or len(records) != 328:
        raise RuntimeError('C production artifact manifest shape differs')
    expected_files = set()
    counts = {app: 0 for app in APPS}
    for record in records:
        name = record.get('file')
        if not isinstance(name, str):
            raise RuntimeError('Malformed artifact name')
        parts = Path(name).parts
        if len(parts) < 5 or parts[:2] != ('frontend', 'apps') or parts[2] not in APPS or parts[3] != 'dist':
            raise RuntimeError('Artifact outside reviewed app dist')
        path = ROOT / name
        if name in expected_files or path.is_symlink() or not path.is_file() or sha(path) != record.get('sha256') or path.stat().st_size != record.get('bytes'):
            raise RuntimeError('C production artifact byte proof differs')
        expected_files.add(name)
        counts[parts[2]] += 1
    if counts != APPS:
        raise RuntimeError('C production artifact count by app differs')
    actual_files = set()
    for app in APPS:
        for path in (FRONTEND / 'apps' / app / 'dist').rglob('*'):
            if path.is_symlink():
                raise RuntimeError('Production dist symlink found')
            if path.is_file():
                actual_files.add(path.relative_to(ROOT).as_posix())
    if actual_files != expected_files:
        raise RuntimeError('Production dist inventory differs')
    if sha(ROOT / CONTROLLED) != TEST_SHA:
        raise RuntimeError('Fixed five-case API-controlled source changed')
    return {'source_head': expected, 'source_tree': git('rev-parse', 'HEAD^{tree}'),
            'source_clean': True, 'c_build_source': C, 'd_source': D,
            'c_to_d_frontend': c_to_d, 'd_to_e_frontend': d_to_e, 'c_to_e_frontend': c_to_e,
            'manifest_sha256': MANIFEST_SHA, 'artifact_count': len(records),
            'artifact_app_counts': counts, 'controlled_test_sha256': TEST_SHA,
            'production_dist_byte_equivalent_to_c': True}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--expected-head', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = verify(args.expected_head)
    if args.output.exists():
        raise RuntimeError('Refusing to overwrite evidence')
    with os.fdopen(os.open(args.output, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'w') as stream:
        json.dump(result, stream, ensure_ascii=False, indent=2)
        stream.write('\n')
    print(json.dumps({'result': str(args.output), 'source_clean': True,
                      'artifact_count': result['artifact_count']}))
    return 0


if __name__ == '__main__':
    sys.exit(main())
