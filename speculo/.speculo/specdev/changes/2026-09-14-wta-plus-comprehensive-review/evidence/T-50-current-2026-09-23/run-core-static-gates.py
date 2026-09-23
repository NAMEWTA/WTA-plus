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
for bundle, args in [('core', ['-Pbundle-core', '-Dmaven.test.skip=true'])]:
    record = run(bundle + '-package', ROOT / 'backend', ['./mvnw', '-B', '-ntp', 'clean', 'package', *args])
    run(bundle + '-bundle', ROOT, ['bash', 'scripts/ci/verify-admin-bundle.sh', bundle])
    jar = ROOT / 'backend/wta-admin/target/wta-admin.jar'
    with zipfile.ZipFile(jar) as archive:
        libs = sorted(n for n in archive.namelist() if n.startswith('BOOT-INF/lib/wta-') and n.endswith('.jar'))
    save(bundle + '-artifact', dict(source=identity(), command_record=label + '-' + bundle + '-package.json',
        started_at=record['started_at'], finished_at=record['finished_at'],
        jar=dict(path=str(jar.relative_to(ROOT)), sha256=sha(jar), size_bytes=jar.stat().st_size),
        wta_libraries=libs, build_log_sha256=sha(OUT / record['log']),
        retention='manifest retained; next clean bundle may replace target artifact'))

for name, script, args in [
    ('skill-facts', '.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs', []),
    ('fullstack-facts', '.agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs', []),
    ('notify-layered', '.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs', ['backend/wta-modules/wta-notify', '--mode', 'layered']),
    ('fm', 'docs/fm/scripts/validate.mjs', []),
    ('handbooks', 'scripts/ci/verify-agent-handbooks.mjs', []),
]:
    run(name, ROOT, ['node', script, *args])
save('source-after', identity())
print('T50 core and static gates complete for ' + expected)
