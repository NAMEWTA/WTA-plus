#!/usr/bin/env python3
"""Serial local T-14 checks. No release promotion or external writes."""
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
assert name.startswith('T-14-') and '/' not in name
mode = sys.argv[2]
shim = Path(tempfile.mkdtemp(prefix='t14-pnpm-', dir=root / 'temp/team/lead'))
(shim / 'pnpm').write_text('#!/bin/sh\nexec corepack pnpm "$@"\n')
(shim / 'pnpm').chmod(0o700)
env = os.environ.copy()
env.update(PATH=str(shim) + os.pathsep + env['PATH'], npm_config_workspace_concurrency='1', RAYON_NUM_THREADS='1')
if mode == 'requirements':
    commands = [('backend', ['./mvnw', '-B', '-ntp', '-pl', 'wta-modules/wta-profile/wta-profile-person', '-am', '-Dtest=MaterialRequirementsHttpContractTest,ProfileMaterialHttpContractTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test'])]
elif mode == 'domain':
    commands = [('frontend', ['corepack', 'pnpm', '--filter', '@namewta/domain-profile', check]) for check in ['typecheck', 'test', 'lint']]
elif mode == 'focused':
    commands = [('frontend', ['corepack', 'pnpm', '--workspace-concurrency=1', '--filter', '@namewta/domain-profile', '--filter', '@namewta/web-domain-profile', '--filter', '@namewta/home-web', check]) for check in ['typecheck', 'test', 'lint']]
elif mode == 'fixture-compile':
    commands = [('backend', ['./mvnw', '-B', '-ntp', '-pl', 'wta-admin', '-am', '-DskipTests', 'test-compile'])]
elif mode == 'frontend':
    commands = [('frontend', ['corepack', 'pnpm', check]) for check in ['lint', 'typecheck', 'test']]
elif mode == 'profile-backend':
    commands = [('backend', ['./mvnw', '-B', '-ntp', '-pl', 'wta-modules/wta-profile,wta-modules/wta-profile/wta-profile-person,wta-modules/wta-profile/wta-profile-enterprise', '-am', 'package'])]
elif mode == 'static':
    commands = [('frontend', ['corepack', 'pnpm', 'exec', 'tsc', '--ignoreConfig', '--noEmit', '--strict', '--skipLibCheck', '--target', 'ES2023', '--module', 'ESNext', '--moduleResolution', 'Bundler', '--types', 'node', 'e2e/profile-self-materials.spec.ts', 'playwright.profile.config.ts', 'playwright.config.ts']),
                ('frontend', ['corepack', 'pnpm', 'exec', 'oxlint', 'e2e/profile-self-materials.spec.ts', 'playwright.profile.config.ts', 'playwright.config.ts']),
                ('.', ['node', '.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs', 'backend/wta-modules/wta-profile/wta-profile-person', '--mode', 'layered']),
                ('.', ['node', '.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs', 'backend/wta-modules/wta-profile/wta-profile-enterprise', '--mode', 'layered']),
                ('.', ['node', '.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs']),
                ('.', ['git', 'diff', '--check'])]
elif mode == 'build':
    env.update(VITE_APP_CONTEXT_PATH='/', VITE_APP_BASE_API='/prod-api', VITE_APP_MESSAGE_ENABLED='true', VITE_APP_NACOS_ADMIN='/nacos/', VITE_APP_MONITOR_ADMIN='/applications')
    commands = [('frontend', ['corepack', 'pnpm', 'build:prod'])]
elif mode == 'root-app-build':
    env.update(VITE_APP_CONTEXT_PATH='/', VITE_APP_BASE_API='/prod-api', VITE_APP_MESSAGE_ENABLED='true', VITE_APP_NACOS_ADMIN='/nacos/', VITE_APP_MONITOR_ADMIN='/applications')
    commands = [('frontend', ['corepack', 'pnpm', '--filter', '@namewta/admin-web', 'build:prod']), ('frontend', ['corepack', 'pnpm', '--filter', '@namewta/home-web', 'build:prod'])]
elif mode == 'home-build':
    env.update(VITE_APP_CONTEXT_PATH='/', VITE_APP_BASE_API='/prod-api', VITE_APP_MESSAGE_ENABLED='true', VITE_APP_NACOS_ADMIN='/nacos/', VITE_APP_MONITOR_ADMIN='/applications')
    commands = [('frontend', ['corepack', 'pnpm', '--filter', '@namewta/home-web', 'build'])]
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
