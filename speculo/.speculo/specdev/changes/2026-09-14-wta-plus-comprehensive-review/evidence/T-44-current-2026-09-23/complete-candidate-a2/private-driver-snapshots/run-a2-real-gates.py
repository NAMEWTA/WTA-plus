"""Lead serial fresh full-app verification; fail before proceeding on any failed owned run."""
from pathlib import Path
import hashlib, json, subprocess, sys

r = Path('/srv/WTA-plus')
t = Path('/tmp/wta-t44')
head, label = sys.argv[1:]
assert len(head) == 40 and label.isalnum()
proof = t / (label + '-full-package-proof.json')
p = json.loads(proof.read_text())
assert p['source_head'] == head and p['exit_code'] == 0
jar_hash = p['artifact']['sha256']
drivers = {
    'minio': ('run-minio-real-v7.py', 'ed3f0f8600728c469e8a6cca412a9f1fc1d8e0a38b04e7c05b82b06fa507b98c'),
    'core': ('run-full-app-core-fallback-v4.py', '7f9faa062f4847c550e98d5faf9f6f2421442f4ebee0b4d38acfa0094ed64db0'),
}
for name, digest in drivers.values():
    assert hashlib.sha256((t / name).read_bytes()).hexdigest() == digest
common = ['--execute', '--expected-head', head, '--expected-jar-sha256', jar_hash, '--package-proof', str(proof)]
health = ['--core-health-path', '/actuator/health/readiness', '--liveness-health-path', '/actuator/health/liveness',
          '--oss-health-path', '/actuator/health/ossdiagnostics', '--oss-health-http', '503', '--oss-health-state', 'DOWN']
gates = [('minio', ['python3', str(t / drivers['minio'][0]), *common])]
for scenario in ['empty-config', 'bad-nondefault', 'duplicate-default', 'bad-default', 'invalid-diagnostic', 'minio-offline', 'redis-publish-denied']:
    args = ['python3', str(t / drivers['core'][0]), *common, '--scenario', scenario, *health]
    if scenario == 'invalid-diagnostic':
        args += ['--invalid-diagnostic-key', 'oss.readiness.diagnostic-timeout', '--invalid-diagnostic-value', 'invalid-owned-duration',
                 '--diagnostic-path-template', '/resource/oss/config/diagnose/{config_id}', '--diagnostic-method', 'POST',
                 '--diagnostic-expected-http', '200', '--diagnostic-expected-code', '200']
    gates.append((scenario, args))
results = []
for name, args in gates:
    before = set((t / 'runs').glob('*/result.json'))
    outcome = subprocess.run(['python3', '/tmp/wta-check.py', str(t), label + '-real-' + name, str(r), *args])
    fresh = set((t / 'runs').glob('*/result.json')) - before
    assert len(fresh) == 1, 'unexpected owned result inventory'
    result_path = fresh.pop()
    result = json.loads(result_path.read_text())
    accepted = result['accepted'] if name == 'minio' else result['acceptance']
    cleanup_errors = result['cleanup_errors'] if name == 'minio' else result['cleanup']['errors']
    results.append(dict(gate=name, command_exit=outcome.returncode, result=str(result_path), sha256=hashlib.sha256(result_path.read_bytes()).hexdigest(), accepted=accepted, cleanup_errors=cleanup_errors))
    (t / (label + '-real-summary.json')).write_text(json.dumps(results, indent=2) + '\n')
    assert outcome.returncode == 0 and accepted is True and not cleanup_errors, 'owned real gate failed: ' + name
print('All eight full-app gates accepted for ' + head)
