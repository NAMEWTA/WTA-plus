"""Generate the registered transport contract from the accepted full-app capture only."""
from pathlib import Path
import hashlib, json, os, subprocess, sys

r = Path('/srv/WTA-plus')
t = Path('/tmp/wta-t44')
head, run_id, label = sys.argv[1:]
assert len(head) == 40 and len(run_id) == 16 and label.isalnum()
assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=r, text=True).strip() == head
assert not subprocess.check_output(['git', 'status', '--porcelain'], cwd=r, text=True)
accepted = json.loads((t / 'runs' / run_id / 'result.json').read_text())
assert accepted['accepted'] is True and not accepted['cleanup_errors']
assert accepted['source']['head'] == head and accepted['source']['clean'] is True
source = t / 'runs' / run_id / 'openapi.raw.json'
assert hashlib.sha256(source.read_bytes()).hexdigest() == accepted['openapi']['sha256']
spec = json.loads(source.read_text())
assert {'/resource/message', '/resource/message/close', '/resource/oss/config/diagnose/{ossConfigId}'} <= set(spec['paths']), 'capture omitted default SSE or diagnostic contract'
env = os.environ.copy()
env['PATH'] = '/tmp/wta-t02-c1/tool-bin:' + env['PATH']
commands = [
    ('fetch', ['--filter', '@namewta/tooling-openapi', 'openapi:fetch', '--', '--source', str(source), '--backend-commit', head]),
    ('generate', ['--filter', '@namewta/tooling-openapi', 'openapi:generate']),
    ('check', ['--filter', '@namewta/tooling-openapi', 'openapi:check']),
    ('types', ['--filter', '@namewta/api-contracts', 'typecheck']),
]
for name, args in commands:
    outcome = subprocess.run(['python3', '/tmp/wta-check.py', str(t), label + '-openapi-' + name, str(r / 'frontend'), 'corepack', 'pnpm', *args], env=env)
    assert outcome.returncode == 0, 'OpenAPI gate failed: ' + name
assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=r, text=True).strip() == head
status = subprocess.check_output(['git', 'status', '--porcelain=v1', '--untracked-files=all'], cwd=r, text=True).splitlines()
for line in status:
    path = line[3:]
    assert path in ('frontend/packages/api-contracts/openapi/current.json', 'frontend/packages/api-contracts/generated/openapi.ts') or path.startswith('frontend/packages/api-contracts/openapi/revisions/'), 'generation escaped registered paths'
record = dict(source=accepted['source'], accepted_run=run_id, raw_sha256=accepted['openapi']['sha256'],
              revision=json.loads((r / 'frontend/packages/api-contracts/openapi/current.json').read_text())['revision'],
              generated_sha256=hashlib.sha256((r / 'frontend/packages/api-contracts/generated/openapi.ts').read_bytes()).hexdigest(), status=status)
with (t / (label + '-openapi-generation-proof.json')).open('x') as stream:
    json.dump(record, stream, indent=2)
    stream.write('\n')
print(json.dumps(record, indent=2))
