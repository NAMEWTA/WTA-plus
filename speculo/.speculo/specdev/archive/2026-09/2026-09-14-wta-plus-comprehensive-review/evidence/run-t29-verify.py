#!/usr/bin/env python3
"""Run documentation owners' real commands serially without starting product services."""
from pathlib import Path
import json
import subprocess
import time

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
selectors = json.loads((evidence/'T-29-audit-final.json').read_text())['maven_selectors']
commands = [
    ('.', ['node', '.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs']),
    ('.', ['node', '.agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs']),
    ('.', ['node', 'docs/fm/scripts/validate.mjs']),
    ('.', ['bash', 'scripts/ci/verify-dev-build-guard.sh']),
    ('backend', ['./mvnw', '-B', '-ntp', '-pl', ','.join(selectors), '-am', 'validate']),
    ('frontend', ['corepack', 'pnpm', 'architecture:check']),
    ('frontend', ['corepack', 'pnpm', '--filter', '@namewta/tooling-openapi', 'openapi:check']),
    ('.', ['git', 'diff', '--check']),
]
rows = []
for i, (cwd, command) in enumerate(commands, 1):
    log = evidence/f'T-29-verification-{i}.log'; start = time.time()
    print('START', cwd, ' '.join(command), flush=True)
    with log.open('w') as output:
        result = subprocess.run(command, cwd=root/cwd, stdout=output, stderr=subprocess.STDOUT)
    row = dict(command=command, cwd=cwd, exit_code=result.returncode, seconds=round(time.time()-start, 2), log=str(log.relative_to(root)))
    rows.append(row); (evidence/'T-29-verification.json').write_text(json.dumps(rows, indent=2)+'\n')
    print(json.dumps(row), flush=True)
    print(log.read_text()[-600:], flush=True)
    if result.returncode: break
raise SystemExit(0 if all(row['exit_code'] == 0 for row in rows) else 1)
