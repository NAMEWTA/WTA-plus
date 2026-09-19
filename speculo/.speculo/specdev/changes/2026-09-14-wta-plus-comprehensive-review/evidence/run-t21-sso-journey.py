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
owned = Path(tempfile.mkdtemp(prefix='t21-journey-', dir=root/'temp/team/lead'))
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
env = os.environ.copy()
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
    for app in apps:
        run(['corepack', 'pnpm', '--filter', f'@namewta/{app}-web', 'build:prod'],
            f'T-21-sso-build-{app}', {'VITE_APP_CONTEXT_PATH': '/' if app == 'sso' else f'/{app}/'})
    run([sys.executable, str(evidence/'run-t21-sso-regression.py'), '--evidence-name', 'T-21-sso-real',
         '--test', 'SsoHttpsSessionIntegrationTest', '--journey'], 'T-21-sso-driver')
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
    record.update(exit_code=code, backup_cleaned=not owned.exists())
    (evidence/'T-21-sso-journey.json').write_text(json.dumps(record, indent=2)+'\n')
print('SSO journey exit', code, 'restored', record['restored'], flush=True)
raise SystemExit(code)
