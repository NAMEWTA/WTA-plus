#!/usr/bin/env python3
"""Verify current App types/lint and preserve each mode before the next build."""
import hashlib
import json
import os
import re
import shutil
import subprocess
import tempfile
import time
from pathlib import Path

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
frontend = root / 'frontend'
shim = Path(tempfile.mkdtemp(prefix='t09-pnpm-', dir=root / 'temp/team/lead'))
(shim / 'pnpm').write_text('#!/bin/sh\nexec corepack pnpm "$@"\n')
(shim / 'pnpm').chmod(0o700)
environment = os.environ.copy()
environment['PATH'] = str(shim) + os.pathsep + environment['PATH']
environment['npm_config_workspace_concurrency'] = '1'
environment['RAYON_NUM_THREADS'] = '1'
apps = ['admin-web', 'home-web', 'sso-web']
commands = [(['corepack', 'pnpm', '--filter', '@namewta/' + app, check], None)
            for app in apps for check in ['typecheck', 'lint']]
commands += [(['corepack', 'pnpm', 'build:' + label], mode)
             for label, mode in [('dev', 'development'), ('prod', 'production')]]
results = []
try:
    for index, (command, mode) in enumerate(commands):
        start = time.time()
        name = 'T-09-frontend-final-' + (mode or str(index + 1))
        with (evidence / f'{name}.log').open('w') as log:
            process = subprocess.Popen(command, cwd=frontend, env=environment,
                                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
            for line in process.stdout:
                log.write(line)
                log.flush()
                if re.search(r'^> @namewta/(?:admin-web|home-web|sso-web)|^vite |Architecture check|error|ERR_PNPM', line):
                    print(line.rstrip(), flush=True)
            exit_code = process.wait()
        result = {'command': command, 'cwd': 'frontend', 'exit_code': exit_code, 'seconds': round(time.time() - start, 2)}
        if mode and exit_code == 0:
            result['apps'] = {}
            output = (evidence / f'{name}.log').read_text()
            for app in apps:
                dist = frontend / 'apps' / app / 'dist'
                result['apps'][app] = {
                    'marker': json.loads((dist / 'build-mode.json').read_text()),
                    'app_build_invocations': len(re.findall(r'^> @namewta/' + app + r'@[^\n]+ build(?::\w+)? ', output, re.M)),
                    'index_sha256': hashlib.sha256((dist / 'index.html').read_bytes()).hexdigest(),
                }
            result['mode_matches_once'] = all(value['marker'] == {'app': app, 'mode': mode}
                                              and value['app_build_invocations'] == 1
                                              for app, value in result['apps'].items())
        (evidence / f'{name}.json').write_text(json.dumps(result, indent=2) + '\n')
        results.append(result)
        print(json.dumps(result), flush=True)
        if exit_code or result.get('mode_matches_once') is False:
            break
finally:
    shutil.rmtree(shim)
(evidence / 'T-09-frontend-final.json').write_text(json.dumps(results, indent=2) + '\n')
raise SystemExit(0 if len(results) == len(commands)
                 and all(row['exit_code'] == 0 and row.get('mode_matches_once', True) for row in results) else 1)
