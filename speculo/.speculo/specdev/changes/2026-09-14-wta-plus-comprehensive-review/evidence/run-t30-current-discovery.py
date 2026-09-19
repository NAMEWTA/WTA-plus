#!/usr/bin/env python3
"""List existing Playwright suites serially. This never runs product tests or servers."""
from pathlib import Path
import json
import os
import subprocess
import time

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
configs = [Path('playwright.config.ts'), *sorted((root/'frontend').glob('playwright.*.config.ts')),
           Path('apps/sso-web/playwright.security.config.ts')]
rows = []
for index, config in enumerate(configs, 1):
    if config.is_absolute(): config = config.relative_to(root/'frontend')
    cmd = ['corepack', 'pnpm', 'exec', 'playwright', 'test', '--config', str(config), '--list', '--workers=1', '--reporter=json']
    log = evidence/f'T-30-current-discovery-{index}.json'; start = time.time()
    print('DISCOVER', config, flush=True)
    result = subprocess.run(cmd, cwd=root/'frontend', env={**os.environ, 'npm_config_workspace_concurrency':'1', 'PROFILE_TEST_ORIGIN':'http://127.0.0.1:9', 'TRANSFER_TEST_ORIGIN':'http://127.0.0.1:9'}, text=True, capture_output=True)
    log.write_text(result.stdout)
    (evidence/f'T-30-current-discovery-{index}.stderr.log').write_text(result.stderr)
    row = dict(command=cmd, cwd='frontend', exit_code=result.returncode, seconds=round(time.time()-start, 2), report=log.name, config=str(config), status='discovery-only', tests_executed=0)
    try:
        data = json.loads(result.stdout)
        def flatten(suites):
            for suite in suites:
                for spec in suite.get('specs', []):
                    yield dict(file=spec['file'], title=spec['title'], line=spec['line'], projects=[test['projectName'] for test in spec.get('tests', [])])
                yield from flatten(suite.get('suites', []))
        row['discovered_specs'] = list(flatten(data.get('suites', [])))
        row['errors'] = data.get('errors', [])
    except json.JSONDecodeError:
        row['errors'] = ['Playwright did not produce JSON; inspect raw output']
    rows.append(row)
    (evidence/'T-30-current-browser-discovery.json').write_text(json.dumps(rows, ensure_ascii=False, indent=2)+'\n')
    print(config, 'exit', result.returncode, 'listed', len(row.get('discovered_specs', [])), flush=True)
raise SystemExit(0 if all(row['exit_code']==0 and not row['errors'] for row in rows) else 1)
