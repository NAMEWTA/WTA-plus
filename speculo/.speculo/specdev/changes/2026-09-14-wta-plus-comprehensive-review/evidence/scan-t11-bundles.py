#!/usr/bin/env python3
"""Report only hashes and matches; never print retired key material."""
import gzip
import hashlib
import json
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
sha = lambda value: hashlib.sha256(value).hexdigest()
old_material = {}
for app in ['admin-web', 'home-web']:
    for mode in ['development', 'production']:
        path = f'frontend/apps/{app}/.env.{mode}'
        old = subprocess.check_output(['git', 'show', 'HEAD:' + path], cwd=root).decode()
        for key in ['VITE_APP_RSA_PRIVATE_KEY', 'VITE_APP_RSA_PUBLIC_KEY']:
            match = re.search(r'^' + key + r'\s*=\s*[\'"]?([^\'"\r\n]+)', old, re.M)
            if match:
                value = match.group(1).strip().encode()
                old_material[sha(value)] = value
        assert not re.search(r'^VITE_APP_(ENCRYPT|RSA_PRIVATE_KEY|RSA_PUBLIC_KEY)\s*=', (root / path).read_text(), re.M)
assert old_material
patterns = [b'crypto-js', b'jsencrypt', b'CryptoJS', b'JSEncrypt', b'mode.ECB', b'VITE_APP_RSA_PRIVATE_KEY', b'encrypt-key']
result = {'retired_material': [{'sha256': digest, 'length': len(value)} for digest, value in old_material.items()], 'apps': {}, 'preserved_sources': {}}
for app in ['admin-web', 'home-web', 'sso-web']:
    dist = root / 'frontend/apps' / app / 'dist'
    marker = json.loads((dist / 'build-mode.json').read_text())
    assert marker == {'app': app, 'mode': 'production'}
    files = []; matches = []
    for path in sorted(dist.rglob('*')):
        if not path.is_file(): continue
        content = path.read_bytes()
        files.append({'path': str(path.relative_to(dist)), 'sha256': sha(content), 'bytes': len(content)})
        if path.suffix == '.gz': content = gzip.decompress(content)
        for digest, value in old_material.items():
            if value in content: matches.append({'path': str(path.relative_to(dist)), 'material_sha256': digest})
        for pattern in patterns:
            if pattern in content: matches.append({'path': str(path.relative_to(dist)), 'pattern': pattern.decode()})
    result['apps'][app] = {'mode': marker, 'files': files, 'matches': matches}
for scope in ['wta-common-openapi', 'wta-common-oss', 'wta-common-encrypt']:
    directory = root / 'backend/wta-common' / scope / 'src/main/java'
    rows = []
    for path in sorted(directory.rglob('*.java')):
        relative = str(path.relative_to(root))
        before = subprocess.check_output(['git', 'show', 'HEAD:' + relative], cwd=root)
        after = path.read_bytes()
        rows.append({'path': relative, 'sha256': sha(after), 'unchanged_from_head': before == after})
    result['preserved_sources'][scope] = rows
retired = ['ApiEncrypt', 'ApiDecryptAutoConfiguration', 'ApiDecryptProperties', 'CryptoFilter', 'DecryptRequestBodyWrapper', 'EncryptResponseBodyWrapper']
classes = root / 'backend/wta-common/wta-common-encrypt/target/classes'
result['retired_backend_classes_found'] = [str(path.relative_to(root)) for path in classes.rglob('*.class') if path.stem.split('$')[0] in retired]
result['database_auto_configuration_present'] = (classes / 'org/namewta/common/encrypt/config/EncryptorAutoConfiguration.class').is_file()
result['lock_has_retired_dependencies'] = any(value in (root / 'frontend/pnpm-lock.yaml').read_text() for value in ['crypto-browser', 'crypto-js', 'jsencrypt'])
result['passed'] = (all(not app['matches'] for app in result['apps'].values())
    and all(row['unchanged_from_head'] for rows in result['preserved_sources'].values() for row in rows)
    and not result['retired_backend_classes_found'] and result['database_auto_configuration_present']
    and not result['lock_has_retired_dependencies'])
(evidence / 'T-11-bundle-scan.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'passed': result['passed'], 'apps': {app: {'files': len(value['files']), 'matches': len(value['matches'])} for app, value in result['apps'].items()}, 'preserved_sources': {scope: len(rows) for scope, rows in result['preserved_sources'].items()}, 'retired_backend_classes_found': result['retired_backend_classes_found']}, ensure_ascii=False))
raise SystemExit(0 if result['passed'] else 1)
