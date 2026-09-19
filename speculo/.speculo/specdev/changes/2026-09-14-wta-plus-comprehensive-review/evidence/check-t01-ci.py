"""Validate local CI candidate commands; requires Python 3 and PyYAML."""
from pathlib import Path
import json
import shlex
import subprocess
import yaml

root = Path(__file__).resolve().parents[6]
workflow = yaml.load((root / '.github/workflows/quality-gates.yml').read_text(), Loader=yaml.BaseLoader)
assert workflow['permissions'] == {'contents': 'read'}
assert set(workflow['on']) == {'pull_request', 'push', 'workflow_dispatch'}
previous = None
shell_steps = 0
frontend_scripts = json.loads((root / 'frontend/package.json').read_text())['scripts']
for name, job in workflow['jobs'].items():
    assert job.get('needs') == previous, (name, 'jobs must run serially')
    previous = name
    assert job.get('continue-on-error') is None
    default_cwd = job.get('defaults', {}).get('run', {}).get('working-directory', '.')
    for step in job['steps']:
        assert step.get('continue-on-error') is None
        if 'run' not in step:
            continue
        command = step['run']
        cwd = root / step.get('working-directory', default_cwd)
        assert cwd.is_dir(), cwd
        result = subprocess.run(['bash', '-n'], input=command, text=True, capture_output=True, cwd=cwd)
        assert result.returncode == 0, result.stderr
        assert 'verify-submodules' not in command
        shell_steps += 1
        if command.startswith('pnpm '):
            words = shlex.split(command)
            if words[1] == '--filter':
                package = json.loads((root / 'frontend/tooling/openapi/package.json').read_text())
                assert words[2] == package['name']
                assert words[3] in package['scripts']
            elif words[1] not in {'install', 'exec'}:
                assert words[1] in frontend_scripts, words
        elif command.startswith('./mvnw '):
            assert (cwd / 'mvnw').is_file()
            assert (cwd / 'pom.xml').is_file()
        elif command.startswith('bash '):
            assert (cwd / shlex.split(command)[1]).is_file()
backend = [step['run'] for step in workflow['jobs']['backend']['steps'] if 'run' in step]
assert backend == [
    './mvnw test', './mvnw clean package -DskipTests', 'bash scripts/ci/verify-admin-bundle.sh full',
    './mvnw clean package -Pbundle-core -Dmaven.test.skip=true', 'bash scripts/ci/verify-admin-bundle.sh core',
]
config = json.loads((root / 'speculo/.speculo/specdev/config.json').read_text())
assert config['execution']['max_implementation_agents'] == 3  # change-level prohibition stays separate
for key, command in config['verification'].items():
    result = subprocess.run(['bash', '-n'], input=command, text=True, capture_output=True, cwd=root)
    assert result.returncode == 0, (key, result.stderr)
    assert 'ruoyi-vue-plus-namewta' not in command
assert config['verification']['typecheck'] == 'pnpm --dir frontend typecheck'
print(json.dumps({'yaml': 'parsed', 'shell_steps_checked': shell_steps,
                  'verification_entries_checked': len(config['verification']),
                  'full_before_core': True, 'serial_job_chain': list(workflow['jobs']),
                  'remote_actions': 'not-run', 'business_tests': 'not-run'}, indent=2))
