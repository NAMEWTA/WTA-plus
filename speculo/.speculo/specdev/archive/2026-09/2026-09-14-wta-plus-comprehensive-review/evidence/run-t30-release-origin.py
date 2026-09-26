#!/usr/bin/env python3
"""Build isolated App contexts, run real SSO journeys, then restore prior build bytes."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import sys
import tempfile
import time

evidence = Path(__file__).resolve().parent
root = evidence.parents[5]
name = sys.argv[1]
assert name.startswith('T-30-') and '/' not in name
assert not (evidence/(name+'.json')).exists()
def source_snapshot():
    paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
    prefixes = ('.agents/', '.github/', 'backend/', 'frontend/', 'scripts/', 'release-artifacts/', 'docs/', 'speculo/workflows/')
    files = {p: hashlib.sha256((root/p).read_bytes()).hexdigest() if (root/p).is_file() else None
             for p in sorted(set(paths)) if p and (p.startswith(prefixes) or '/' not in p)}
    return files, hashlib.sha256(json.dumps(files, sort_keys=True, separators=(',', ':')).encode()).hexdigest()

source_files, source_fingerprint = source_snapshot()
assert source_files == json.loads((evidence/'T-30-source-current.json').read_text())['files']
assert not (evidence/(name+'.json')).exists(), 'preserve earlier results'
assert not (evidence/(name+'.log')).exists(), 'preserve earlier logs'

owned = Path(tempfile.mkdtemp(prefix='t30-release-origin-', dir=root/'temp/team/lead'))
apps = ['admin', 'home', 'sso']
record = {'commands': [], 'backup': str(owned.relative_to(root))}
process = None
code = 1
backed_up = []

def hashes(directory):
    return {str(p.relative_to(directory)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted(directory.rglob('*')) if p.is_file()}

def stop(number, _frame):
    raise SystemExit(128 + number)

signal.signal(signal.SIGTERM, stop)
signal.signal(signal.SIGINT, stop)
shim = owned/'pnpm'
shim.write_text('#!/bin/sh\nexec corepack pnpm "$@"\n')
shim.chmod(0o700)
env = {**os.environ, 'PATH': str(owned)+os.pathsep+os.environ['PATH']}
env.update(npm_config_workspace_concurrency='1', RAYON_NUM_THREADS='1',
           VITE_APP_BASE_API='/api', VITE_APP_MESSAGE_ENABLED='true',
           VITE_APP_NACOS_ADMIN='/nacos/', VITE_APP_MONITOR_ADMIN='/applications')

def run(command, name, overrides=None):
    global process
    target = evidence/(name+'.log')
    started = time.time()
    print('START', ' '.join(command), flush=True)
    with target.open('w') as output:
        process = subprocess.Popen(command, cwd=root/'frontend', env={**env, **(overrides or {})},
                                   stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        result = process.wait()
    record['commands'].append({'command': command, 'exit_code': result, 'seconds': round(time.time()-started, 2),
                               'log': str(target.relative_to(root)), 'overrides': overrides})
    print(name, 'exit', result, flush=True)
    if result: print(target.read_text()[-5000:], flush=True)
    if result: raise RuntimeError(name+' failed')

try:
    record['before'] = {}
    for app in apps:
        dist = root/f'frontend/apps/{app}-web/dist'
        assert (dist/'index.html').is_file()
        record['before'][app] = hashes(dist)
        shutil.copytree(dist, owned/app)
        backed_up.append(app)
    run([sys.executable, str(root/'release-artifacts/tests/fixtures/sso-release-origin.py'),
         '--evidence', str(evidence/(name+'-fixture.json'))], name+'-driver')
    code = 0
except SystemExit as failure:
    code = int(failure.code)
    record['interrupted_signal'] = code - 128
except Exception as failure:
    record['error'] = str(failure)
finally:
    if process is not None:
        if process.poll() is None:
            process.terminate()
            try: process.wait(timeout=20)
            except subprocess.TimeoutExpired: pass
        # Each command has its own session; no existing service is in this group.
        try: os.killpg(process.pid, signal.SIGTERM)
        except ProcessLookupError: pass
        if process.poll() is None:
            try: process.wait(timeout=5)
            except subprocess.TimeoutExpired: os.killpg(process.pid, signal.SIGKILL); process.wait()
    record['restored'] = {}
    for app in backed_up:
        dist = root/f'frontend/apps/{app}-web/dist'
        shutil.rmtree(dist)
        shutil.copytree(owned/app, dist)
        record['restored'][app] = hashes(dist) == record['before'][app]
    if not all(record['restored'].values()): code = 1
    if all(record['restored'].values()): shutil.rmtree(owned)
    record['source_fingerprint'] = source_fingerprint
    record['source_unchanged'] = source_snapshot()[0] == source_files
    if not record['source_unchanged']: code = 1
    record.update(exit_code=code, backup_cleaned=not owned.exists())
    (evidence/(name+'.json')).write_text(json.dumps(record, indent=2)+'\n')
print('SSO journey exit', code, 'restored', record['restored'], flush=True)
raise SystemExit(code)
