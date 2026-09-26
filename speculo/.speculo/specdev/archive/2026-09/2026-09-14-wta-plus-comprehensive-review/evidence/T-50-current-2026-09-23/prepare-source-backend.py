#!/usr/bin/env python3
"""Run serial backend gates only for an explicitly named clean T-50 candidate."""
from pathlib import Path
import datetime, hashlib, json, shutil, subprocess, sys, xml.etree.ElementTree as ET, zipfile

ROOT = Path('/srv/WTA-plus')
OUT = Path('/tmp/wta-t50')
expected, label = sys.argv[1:]
assert len(expected) == 40 and label.isalnum()
def sha(path):
    return hashlib.file_digest(path.open('rb'), 'sha256').hexdigest()
def identity():
    head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    tree = subprocess.check_output(['git', 'rev-parse', 'HEAD^{tree}'], cwd=ROOT, text=True).strip()
    dirty = subprocess.check_output(['git', 'status', '--porcelain=v1', '--untracked-files=all'], cwd=ROOT, text=True).strip()
    assert head == expected and not dirty, 'candidate identity or cleanliness changed'
    return dict(head=head, tree=tree, clean=True)
def save(name, value):
    p = OUT / (label + '-' + name + '.json')
    with p.open('x') as stream: json.dump(value, stream, indent=2); stream.write('\n')
def run(name, cwd, argv):
    identity()
    p = subprocess.run(['python3', '/tmp/wta-check.py', str(OUT), label + '-' + name, str(cwd), *argv])
    assert p.returncode == 0, 'gate failed: ' + name
    identity()
    return json.loads((OUT / (label + '-' + name + '.json')).read_text())

save('source-before', identity())
record = run('backend-default', ROOT / 'backend', ['./mvnw', 'test'])
subprocess.run(['python3', '/tmp/wta-t02-c1/record-reactor-counts.py', str(OUT), label + '-backend-default'], check=True)
started = datetime.datetime.fromisoformat(record['started_at']).timestamp()
reports = OUT / (label + '-default-reports'); reports.mkdir(exist_ok=False)
for p in sorted((ROOT / 'backend').glob('**/target/surefire-reports/TEST-*.xml')):
    if p.stat().st_mtime >= started:
        target = reports / p.relative_to(ROOT / 'backend')
        target.parent.mkdir(parents=True, exist_ok=True); shutil.copy2(p, target)

record = run('full-package', ROOT / 'backend', ['./mvnw', '-B', '-ntp', 'clean', 'package', '-DskipTests'])
run('full-bundle', ROOT, ['bash', 'scripts/ci/verify-admin-bundle.sh', 'full'])
jar = ROOT / 'backend/wta-admin/target/wta-admin.jar'
with zipfile.ZipFile(jar) as archive:
    libs = sorted(n for n in archive.namelist() if n.startswith('BOOT-INF/lib/wta-') and n.endswith('.jar'))
artifacts = OUT / 'artifacts' / (label + '-full'); artifacts.mkdir(parents=True, exist_ok=False)
shutil.copy2(jar, artifacts / 'wta-admin.jar')
manifest = dict(source=identity(), command_record=label + '-full-package.json', started_at=record['started_at'],
    finished_at=record['finished_at'], jar=dict(path=str(jar.relative_to(ROOT)), sha256=sha(jar), size_bytes=jar.stat().st_size),
    wta_libraries=libs, build_log_sha256=sha(OUT / record['log']), retention='private immutable copy for owned full OpenAPI capture')
(artifacts / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
proof = dict(command=['./mvnw', '-B', '-ntp', 'clean', 'package', '-DskipTests'], cwd='backend', exit_code=0,
    source_clean_at_build=True, source_head=expected, source_tree=identity()['tree'],
    artifact=manifest['jar'], build_log=dict(path=str(OUT / record['log']), sha256=manifest['build_log_sha256']),
    started_utc=record['started_at'], finished_utc=record['finished_at'])
save('full-package-proof', proof)
save('source-after', identity())
print('T50 source default/full preparation complete for ' + expected)
