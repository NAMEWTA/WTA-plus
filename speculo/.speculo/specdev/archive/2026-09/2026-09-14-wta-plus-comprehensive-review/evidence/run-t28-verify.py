#!/usr/bin/env python3
"""Run T-28 repository checks serially and retain real outcomes."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET

evidence = Path(__file__).resolve().parent
root = evidence.parents[5]
name, mode = sys.argv[1:3]
assert name.startswith('T-28-') and '/' not in name
shim = Path(tempfile.mkdtemp(prefix='t28-pnpm-', dir=root/'temp/team/lead'))
(shim/'pnpm').write_text('#!/bin/sh\nexec corepack pnpm "$@"\n'); (shim/'pnpm').chmod(0o700)
env = {**os.environ, 'PATH': str(shim)+os.pathsep+os.environ['PATH'], 'npm_config_workspace_concurrency': '1'}
if mode == 'backend':
    commands = [('backend', ['./mvnw', '-B', '-ntp', '-pl', 'wta-modules/wta-notify,wta-admin', '-am', 'test'])]
elif mode == 'static':
    commands = [('.', ['node', '.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs', 'backend/wta-modules/wta-notify', '--mode', 'layered']),
                ('.', ['node', '--test', 'release-artifacts/tests/notify-baseline-contract.test.mjs']),
                ('frontend', ['corepack', 'pnpm', 'typecheck']),
                ('.', ['node', '.agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs']),
                ('.', ['node', '.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs']),
                ('.', ['node', 'docs/fm/scripts/validate.mjs']),
                ('.', ['git', 'diff', '--check'])]
else: raise ValueError(mode)
rows = []
try:
    for index, (cwd, command) in enumerate(commands, 1):
        log = evidence/f'{name}-{index}.log'; started = time.time()
        print('START', ' '.join(command), flush=True)
        with log.open('w') as output:
            result = subprocess.run(command, cwd=root/cwd, env=env, stdout=output, stderr=subprocess.STDOUT)
        row = dict(command=command, cwd=cwd, exit_code=result.returncode, seconds=round(time.time()-started, 2), log=str(log.relative_to(root)))
        if cwd == 'backend':
            row['counts'] = dict(tests=0, failures=0, errors=0, skipped=0); row['reports'] = []
            for report in (root/'backend').rglob('target/surefire-reports/TEST-*.xml'):
                if report.stat().st_mtime < started: continue
                suite = ET.parse(report).getroot(); row['reports'].append(str(report.relative_to(root)))
                for key in row['counts']: row['counts'][key] += int(suite.get(key, 0))
        rows.append(row)
        (evidence/(name+'.json')).write_text(json.dumps(rows, ensure_ascii=False, indent=2)+'\n')
        print(json.dumps({k:v for k,v in row.items() if k != 'reports'}, ensure_ascii=False), flush=True)
        if result.returncode: print(log.read_text()[-6500:], flush=True)
        if result.returncode and mode == 'backend': break
finally: shutil.rmtree(shim)
raise SystemExit(0 if all(row['exit_code'] == 0 for row in rows) else 1)
