#!/usr/bin/env python3
"""Run each selected final gate serially against the frozen source, with fresh evidence."""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import time
import tempfile
import xml.etree.ElementTree as ET
import zipfile

evidence = Path(__file__).resolve().parent
root = evidence.parents[5]
group = sys.argv[1]
run_name = sys.argv[2] if len(sys.argv) > 2 else 'T-30-v1'
assert run_name.startswith('T-30-') and '/' not in run_name
groups = {'default-browser': [9], 'static': [1, 2, 3, 4, 5, 8, 17, 18], 'frontend-builds': [6, 7],
          'backend-tests': [10], 'backend-bundles': [11, 12, 13, 14], 'release': [15], 'external': [16]}
inventory = json.loads((evidence/'T-30-inventory-current.json').read_text())
frozen = json.loads((evidence/'T-30-source-current.json').read_text())

def snapshot():
    paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
    prefixes = ('.agents/', '.github/', 'backend/', 'frontend/', 'scripts/', 'release-artifacts/', 'docs/', 'speculo/workflows/')
    return {p: hashlib.sha256((root/p).read_bytes()).hexdigest() if (root/p).is_file() else None
            for p in sorted(set(paths)) if p and (p.startswith(prefixes) or '/' not in p)}

def resources():
    return {kind: sorted(subprocess.check_output(cmd, cwd=root, text=True).splitlines()) for kind, cmd in [
        ('containers', ['docker', 'ps', '-aq', '--no-trunc']), ('networks', ['docker', 'network', 'ls', '-q', '--no-trunc']),
        ('volumes', ['docker', 'volume', 'ls', '-q'])]}

assert snapshot() == frozen['files'], 'source differs from final input'
assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip() == frozen['parent_before']
assert not subprocess.check_output(['git', 'diff', '--cached', '--name-only'], cwd=root)
output_json = evidence/f'{run_name}-{group}.json'
assert not output_json.exists(), 'preserve prior attempt; use a new label'
rows = []
environment = {**os.environ, 'npm_config_workspace_concurrency': '1', 'RAYON_NUM_THREADS': '1'}
with tempfile.TemporaryDirectory(prefix='t30-tools-', dir=root/'temp/team/lead') as tool_dir:
    shim = Path(tool_dir)/'pnpm'
    shim.write_text('#!/bin/sh\nexec corepack pnpm "$@"\n')
    shim.chmod(0o700)
    environment['PATH'] = tool_dir + os.pathsep + environment['PATH']
    for number in groups[group]:
        item = inventory['serial_core_commands'][number - 1]
        command, cwd = item['command'], item['cwd']
        if number == 9:
            command = ['python3', str(evidence/'run-t30-default-e2e.py'), run_name+'-default-e2e']
            cwd = '.'
        assert snapshot() == frozen['files']
        before = resources() if group == 'external' else None
        log = evidence/f'{run_name}-{group}-{number}.log'
        started = time.time()
        print('START', number, cwd, ' '.join(command), flush=True)
        with log.open('w') as output:
            result = subprocess.run(command, cwd=root/cwd, env=environment, stdout=output, stderr=subprocess.STDOUT)
        row = dict(number=number, command=command, cwd=cwd, exit_code=result.returncode, seconds=round(time.time()-started, 2),
                   source_fingerprint=frozen['source_fingerprint'], source_unchanged=snapshot() == frozen['files'], log=log.name)
        if before is not None:
            row['resources_before'] = before; row['resources_after'] = resources()
            row['resources_restored'] = row['resources_after'] == before
        if group in ('backend-tests', 'external'):
            row['counts'] = dict(tests=0, failures=0, errors=0, skipped=0); row['suites'] = []
            for path in sorted((root/'backend').glob('**/target/surefire-reports/TEST-*.xml')):
                if path.stat().st_mtime < started: continue
                suite = ET.parse(path).getroot(); counts = {k: int(suite.get(k, 0)) for k in row['counts']}
                row['suites'].append(dict(name=suite.get('name'), **counts))
                for k, value in counts.items(): row['counts'][k] += value
            row['nonzero_tests'] = row['counts']['tests'] > 0
        if group == 'backend-bundles' and command[-1] in ('full', 'core') and result.returncode == 0:
            jars = list((root/'backend/wta-admin/target').glob('*.jar')); assert len(jars) == 1
            jar = jars[0]
            with zipfile.ZipFile(jar) as archive: entries = archive.namelist()
            row['artifact'] = dict(path=str(jar.relative_to(root)), sha256=hashlib.sha256(jar.read_bytes()).hexdigest(), bytes=jar.stat().st_size, entries=entries)
            retained = root/'temp/team/lead'/(run_name+'-artifacts')
            retained.mkdir(exist_ok=True)
            destination = retained/('wta-admin-'+command[-1]+'.jar')
            assert not destination.exists(), 'never overwrite retained artifact'
            shutil.copy2(jar, destination)
            assert hashlib.sha256(destination.read_bytes()).hexdigest() == row['artifact']['sha256']
            row['artifact']['retained_path'] = str(destination.relative_to(root))
        if group == 'frontend-builds' and result.returncode == 0:
            mode = 'development' if number == 6 else 'production'
            row['apps'] = {}
            for app in ('admin-web', 'home-web', 'sso-web'):
                dist = root/'frontend/apps'/app/'dist'
                marker = json.loads((dist/'build-mode.json').read_text())
                assert marker == {'app': app, 'mode': mode}, (app, marker, mode)
                output_files = {str(path.relative_to(dist)): hashlib.sha256(path.read_bytes()).hexdigest()
                                for path in sorted(dist.rglob('*')) if path.is_file()}
                row['apps'][app] = dict(marker=marker, files=output_files,
                    artifact_fingerprint=hashlib.sha256(json.dumps(output_files, sort_keys=True).encode()).hexdigest())
            row['build_mode_verified'] = True
        text = log.read_text()
        row['test_summary_lines'] = [line for line in text.splitlines() if re.search(r'(Tests\s+\d+|tests \d+|pass \d+|fail \d+|Test Files\s+\d+)', line)][-100:]
        rows.append(row); output_json.write_text(json.dumps(rows, ensure_ascii=False, indent=2)+'\n')
        print(json.dumps({k:v for k,v in row.items() if k not in ('suites', 'artifact', 'test_summary_lines', 'resources_before', 'resources_after', 'apps')}, ensure_ascii=False), flush=True)
        if result.returncode or not row['source_unchanged'] or row.get('nonzero_tests') is False or row.get('resources_restored') is False:
            print(text[-10000:], flush=True); raise SystemExit(1)
