#!/usr/bin/env python3
"""Serial local T-03 checks; fingerprint product inputs and preserve fresh results."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
import zipfile

evidence = Path(__file__).resolve().parent
root = evidence.parents[5]
mode = sys.argv[1]
commands = {
    'tests': [('backend', ['./mvnw', '-B', '-ntp', '-pl', 'wta-common/wta-common-web,wta-common/wta-common-openapi,wta-common/wta-common-encrypt', '-am', 'test']),
              ('backend', ['./mvnw', '-B', '-ntp', 'test'])],
    'bundles': [('backend', ['./mvnw', '-B', '-ntp', 'clean', 'package', '-DskipTests']),
                ('.', ['bash', 'scripts/ci/verify-admin-bundle.sh', 'full']),
                ('backend', ['./mvnw', '-B', '-ntp', 'clean', 'package', '-Pbundle-core', '-Dmaven.test.skip=true']),
                ('.', ['bash', 'scripts/ci/verify-admin-bundle.sh', 'core'])],
}[mode]

def snapshot():
    paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
    prefixes = ('.agents/', '.github/', 'backend/', 'frontend/', 'scripts/', 'release-artifacts/', 'docs/', 'speculo/workflows/')
    files = {p: hashlib.sha256((root/p).read_bytes()).hexdigest() if (root/p).is_file() else None
             for p in sorted(set(paths)) if p and (p.startswith(prefixes) or '/' not in p)}
    return files, hashlib.sha256(json.dumps(files, sort_keys=True, separators=(',', ':')).encode()).hexdigest()

def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n')

baseline, fingerprint = snapshot()
source_path = evidence/'T-03-verification-source.json'
if source_path.exists():
    assert json.loads(source_path.read_text())['files'] == baseline, 'source changed since the test gate'
else:
    save(source_path, dict(files=baseline, source_fingerprint=fingerprint))
assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip() == '76dbbe84a34624234e379661a57b232529e34ed3'
assert not subprocess.check_output(['git', 'diff', '--cached', '--name-only'], cwd=root)
rows = []
for index, (cwd, command) in enumerate(commands, 1):
    assert snapshot()[0] == baseline
    name = f'T-03-{mode}-{index}'
    log = evidence/(name+'.log')
    started = time.time()
    print('START', ' '.join(command), flush=True)
    with log.open('w') as output:
        result = subprocess.run(command, cwd=root/cwd, stdout=output, stderr=subprocess.STDOUT)
    row = dict(command=command, cwd=cwd, exit_code=result.returncode, seconds=round(time.time()-started, 2),
               source_fingerprint=fingerprint, source_unchanged=snapshot()[0] == baseline, log=str(log.relative_to(root)))
    if mode == 'tests':
        row['counts'] = dict(tests=0, failures=0, errors=0, skipped=0)
        row['suites'] = []
        for path in sorted((root/'backend').glob('**/target/surefire-reports/TEST-*.xml')):
            if path.stat().st_mtime < started: continue
            suite = ET.parse(path).getroot()
            counts = {key: int(suite.get(key, 0)) for key in row['counts']}
            row['suites'].append(dict(name=suite.get('name'), **counts))
            for key, value in counts.items(): row['counts'][key] += value
    if mode == 'bundles' and command[-1] in ('full', 'core') and result.returncode == 0:
        jars = list((root/'backend/wta-admin/target').glob('*.jar'))
        assert len(jars) == 1, jars
        jar = jars[0]
        with zipfile.ZipFile(jar) as archive:
            entries = archive.namelist()
        row['artifact'] = dict(path=str(jar.relative_to(root)), sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
                               bytes=jar.stat().st_size, entries=entries)
    rows.append(row)
    save(evidence/f'T-03-{mode}.json', rows)
    print(json.dumps({key:value for key,value in row.items() if key not in ('suites', 'artifact')}, ensure_ascii=False), flush=True)
    if result.returncode or not row['source_unchanged']:
        print(log.read_text()[-7000:], flush=True)
        raise SystemExit(1)
