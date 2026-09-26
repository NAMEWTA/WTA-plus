#!/usr/bin/env python3
"""Capture current source and test entry points; never execute acceptance gates."""
from pathlib import Path
import datetime
import hashlib
import json
import re
import subprocess

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
change = evidence.parent


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None


def save(name, data):
    (evidence/name).write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n')


upstream = json.loads((evidence/'T-30-input-current.json').read_text())
drift = [path for path, expected in upstream['files'].items() if digest(root/path) != expected]
if drift:
    raise SystemExit('Upstream source changed: '+str(drift))

head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
assert head == upstream['parent_before']
assert not subprocess.check_output(['git', 'diff', '--cached', '--name-only'], cwd=root)

# Product/tooling inputs include tracked and untracked source, and tombstones.
# Runtime outputs and the changing acceptance evidence are not source inputs.
paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
prefixes = ('.agents/', '.github/', 'backend/', 'frontend/', 'scripts/', 'release-artifacts/', 'docs/', 'speculo/workflows/')
files = {p: digest(root/p) for p in sorted(set(paths)) if p and (p.startswith(prefixes) or '/' not in p)}
fingerprint = hashlib.sha256(json.dumps(files, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
save('T-30-source-current.json', dict(captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
    parent_before=head, implementation_commit=None, result_sha=None, candidate=None,
    purpose='frozen local acceptance input; not a release candidate', scope=list(prefixes)+['root files'],
    source_fingerprint=fingerprint, files=files))

external_source = (root/'scripts/ci/run-external-services.sh').read_text()
selector_block = re.search(r'integration_tests=\((.*?)\)', external_source, re.S)[1]
ci_classes = re.findall(r'\b[A-Za-z][A-Za-z0-9]*Test\b', selector_block)
java_files = sorted((root/'backend').glob('**/src/test/java/**/*.java'))
classes = {p.stem: p for p in java_files}
assert all(name in classes for name in ci_classes)
gated = []
for path in java_files:
    source = path.read_text()
    properties = sorted(set(re.findall(r'System\.getProperty\("([^"]+)"', source)
                            + re.findall(r'@EnabledIfSystemProperty\(named\s*=\s*"([^"]+)"', source)))
    if properties:
        gated.append(dict(path=str(path.relative_to(root)), sha256=digest(path), properties=properties,
                          selected_by_existing_external_script=path.stem in ci_classes))

discovery = json.loads((evidence/'T-30-current-browser-discovery.json').read_text())
assert all(row['exit_code']==0 and not row['errors'] for row in discovery)
browser_entries = []
covered = set()
for row in discovery:
    base = 'frontend/apps/sso-web/e2e/' if row['config'].startswith('apps/') else 'frontend/e2e/'
    selected = sorted(set(base+spec['file'] for spec in row['discovered_specs']))
    # Reporter files are relative to its configured testDir.
    assert all((root/path).is_file() for path in selected), selected
    covered.update(selected)
    browser_entries.append(dict(config='frontend/'+row['config'], config_sha256=digest(root/'frontend'/row['config']),
        command=row['command'], cwd='frontend', discovered_tests=len(row['discovered_specs']),
        files=selected, tests_executed=0, status='discovery-only', discovery_report=row['report']))
all_specs = {str(p.relative_to(root)) for p in (root/'frontend/e2e').glob('*.spec.ts')}
all_specs.update(str(p.relative_to(root)) for p in (root/'frontend/apps/sso-web/e2e').glob('*.spec.ts'))
unlisted = sorted(all_specs-covered)

core_commands = [('frontend', ['corepack', 'pnpm', script]) for script in
    ['architecture:check', 'architecture:test', 'lint', 'typecheck', 'test', 'build:dev', 'build:prod']]
core_commands += [('frontend', ['corepack', 'pnpm', '--filter', '@namewta/tooling-openapi', 'openapi:check']),
    ('frontend', ['corepack', 'pnpm', 'test:e2e', '--workers=1']),
    ('backend', ['./mvnw', '-B', '-ntp', 'test']),
    ('backend', ['./mvnw', '-B', '-ntp', 'clean', 'package', '-DskipTests']),
    ('.', ['bash', 'scripts/ci/verify-admin-bundle.sh', 'full']),
    ('backend', ['./mvnw', '-B', '-ntp', 'clean', 'package', '-Pbundle-core', '-Dmaven.test.skip=true']),
    ('.', ['bash', 'scripts/ci/verify-admin-bundle.sh', 'core']),
    ('.', ['bash', 'release-artifacts/scripts/verify-release.sh']),
    ('.', ['bash', 'scripts/ci/run-external-services.sh'])]
core_commands += [('.', ['node', 'scripts/ci/verify-agent-handbooks.mjs']), ('.', ['node', '--test', 'scripts/ci/verify-agent-handbooks.test.mjs'])]
plan = json.loads((change/'plan-data.json').read_text())
save('T-30-inventory-current.json', dict(captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
    status='acceptance-input-ready', acceptance_status='in-progress', source_fingerprint=fingerprint,
    upstream_paths_verified=len(upstream['files']), source_paths=len(files),
    ticket_statuses={r['id']:r['status'] for r in plan},
    blockers=['All implementation commits held; direct-parent/result/release candidate incomplete'],
    existing_ci_external_classes=ci_classes, test_source_property_inventory=gated,
    browser_discovery=browser_entries, browser_specs_without_persistent_config=unlisted,
    remaining_browser_entrypoints=[dict(spec='frontend/e2e/workflow-task-integrity.spec.ts',
        driver=str((evidence/'run-t16-dialog-browser.py').relative_to(root)),
        status='not-run; driver creates an isolated temporary Playwright config')],
    serial_core_commands=[dict(cwd=cwd, command=command, status='not-run', exit_code=None) for cwd,command in core_commands],
    acceptance_requirements=['Verify unchanged source fingerprint before/after each gate; stop on drift',
        'One command/service fixture at a time; no subagents/worktrees',
        'Default Maven skips are not real-service evidence; each required class must have one fresh nonzero zero-skip report',
        'Build and verify full JAR before core clean; retain both verified artifact hashes',
        'SSO/Profile/upload/browser tests need owned real fixtures; discovery placeholder origins are never runtime inputs',
        'Do not rerun historical drivers into old evidence names; adapt output paths and cleanup first',
        'Capture only safe diagnostics; no credentials/raw signed bodies in evidence',
        'Only owned containers/ports/files may be removed; resource inventory restored',
        'Not-run/blocked/skipped required checks never become passed']))
print(json.dumps(dict(source_paths=len(files), fingerprint=fingerprint, upstream_paths=len(upstream['files']),
    existing_ci_external_classes=len(ci_classes), property_bearing_test_files=len(gated),
    browser_configs=len(browser_entries), discovered_tests=sum(x['discovered_tests'] for x in browser_entries),
    unlisted_specs=unlisted, product_tests_executed=0), ensure_ascii=False, indent=2))
