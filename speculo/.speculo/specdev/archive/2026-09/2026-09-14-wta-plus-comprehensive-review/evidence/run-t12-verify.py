#!/usr/bin/env python3
"""Serial local T-12 checks. No release promotion or external writes."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
name = sys.argv[1]
assert name.startswith('T-12-') and '/' not in name
mode = sys.argv[2]
shim = Path(tempfile.mkdtemp(prefix='t12-pnpm-', dir=root / 'temp/team/lead'))
(shim / 'pnpm').write_text('#!/bin/sh\nexec corepack pnpm "$@"\n')
(shim / 'pnpm').chmod(0o700)
env = os.environ.copy()
env.update(PATH=str(shim) + os.pathsep + env['PATH'], npm_config_workspace_concurrency='1', RAYON_NUM_THREADS='1')
if mode == 'build':
    env.update(VITE_APP_CONTEXT_PATH='/', VITE_APP_BASE_API='/prod-api')
    commands = [('frontend', ['corepack', 'pnpm', 'build:prod'])]
elif mode == 'browser':
    commands = [('backend', ['./mvnw', '-pl', 'wta-admin', '-am', '-Dtest=BrowserHttpsTransportIntegrationTest',
                 '-Dsurefire.failIfNoSpecifiedTests=false', '-Dbrowser.transport.integration=true',
                 '-Dnamewta.repo.root=' + str(root), 'test'])]
elif mode == 'frontend':
    commands = [('frontend', ['corepack', 'pnpm', 'lint']), ('frontend', ['corepack', 'pnpm', 'typecheck']),
                ('frontend', ['corepack', 'pnpm', 'test'])]
elif mode == 'final':
    env.update(VITE_APP_CONTEXT_PATH='/', VITE_APP_BASE_API='/prod-api', VITE_APP_MESSAGE_ENABLED='true',
               VITE_APP_NACOS_ADMIN='/nacos/', VITE_APP_MONITOR_ADMIN='/applications')
    commands = [('frontend', ['corepack', 'pnpm', 'lint']),
                ('frontend', ['corepack', 'pnpm', '--workspace-concurrency=1', '-r', '--filter', '@namewta/admin-web', '--filter', '@namewta/home-web', 'typecheck']),
                ('frontend', ['corepack', 'pnpm', '--workspace-concurrency=1', '-r', '--filter', '@namewta/admin-web', '--filter', '@namewta/home-web', 'test']),
                ('frontend', ['corepack', 'pnpm', 'exec', 'tsc', '--ignoreConfig', '--noEmit', '--strict', '--skipLibCheck', '--target', 'ES2023', '--module', 'ESNext', '--moduleResolution', 'Bundler', '--types', 'node', 'e2e/session-navigation-lifecycle.spec.ts', 'playwright.lifecycle.config.ts']),
                ('frontend', ['corepack', 'pnpm', 'exec', 'oxlint', 'playwright.lifecycle.config.ts']),
                ('frontend', ['corepack', 'pnpm', '--filter', '@namewta/admin-web', 'build:prod']),
                ('.', ['node', '.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs']),
                ('.', ['git', 'diff', '--check'])]
elif mode == 'maven':
    commands = [('backend', ['./mvnw', 'clean', 'test'])]
else:
    raise ValueError(mode)
results = []
try:
    for index, (cwd, command) in enumerate(commands):
        target = evidence / f'{name}-{index + 1}.log'
        start = time.time()
        print('START', cwd, ' '.join(command), flush=True)
        with target.open('w') as log:
            result = subprocess.run(command, cwd=root / cwd, env=env, stdout=log, stderr=subprocess.STDOUT)
        output = target.read_text()
        row = dict(command=command, cwd=cwd, exit_code=result.returncode, seconds=round(time.time()-start, 2), log=str(target.relative_to(root)))
        if cwd == 'backend':
            row['counts'] = dict(tests=0, failures=0, errors=0, skipped=0)
            row['reports'] = []
            for report in (root / 'backend').rglob('target/surefire-reports/TEST-*.xml'):
                if report.stat().st_mtime < start: continue
                suite = ET.parse(report).getroot()
                row['reports'].append(str(report.relative_to(root)))
                for key in row['counts']: row['counts'][key] += int(suite.get(key, 0))
        results.append(row)
        (evidence / (name + '.json')).write_text(json.dumps(results, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps(row, ensure_ascii=False), flush=True)
        if result.returncode:
            print(output[-6500:], flush=True)
            break
finally:
    shutil.rmtree(shim)
raise SystemExit(0 if len(results) == len(commands) and all(row['exit_code'] == 0 for row in results) else 1)
