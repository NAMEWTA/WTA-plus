#!/usr/bin/env python3
"""T41 E recovery: five API-controlled Chrome cases against the C Admin dist.

Preparation is side-effect free. Only --execute starts an owned loopback Vite preview.
No backend, database, rebuild, dev server, production credentials, or remote target.
"""

import argparse
import hashlib
import http.client
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import socket
import subprocess
import sys
import time

ROOT = Path('/srv/WTA-plus')
FRONTEND = ROOT / 'frontend'
APP = FRONTEND / 'apps/admin-web'
TEST = FRONTEND / 'e2e/inbox-without-realtime.spec.ts'
VITE = FRONTEND / 'node_modules/vite/bin/vite.js'
PLAYWRIGHT = FRONTEND / 'node_modules/@playwright/test/cli.js'
MANIFEST = Path('/tmp/wta-t41/c-frontend-artifacts.json')
RUN_ROOT = Path('/tmp/wta-t41/api-controlled-runs-v2')
EXPECTED_HEAD = 'a0dcbac8fd33a754c74c154551f7090a8878fb48'
EXPECTED_C_BUILD = 'eff81f642f08a1590f85a1f1f813a592a8232451'
EXPECTED_MANIFEST_SHA = 'e3a32c6886aa500b1846871a26aaaf84dd0ce0f31bfec00e95b25fccc1f19d84'
EXPECTED_TEST_SHA = '32e34f3268062628b41303903227732e609f9ad625de5f5f2ceaaeb2f5467ad2'
EXPECTED_FRONTEND_DELTA = [
    'frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts',
    'frontend/e2e/run-inbox-paged-real.py',
    'frontend/e2e/test_run_inbox_paged_real.py',
]
TITLES = (
    'T-34 message box distinguishes loading, failure, retry, notice category and read state without realtime',
    'T-34 old inbox failure cannot leak into the next login',
    'T-34 old read failure and detail are gone after unmount and next login',
    'T-34 successful empty inbox is distinct from loading and failure',
    'T-34 opening during the initial request refreshes a notice arriving after its snapshot',
)


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def source_identity():
    if not re.fullmatch(r'[0-9a-f]{40}', EXPECTED_HEAD):
        raise RuntimeError('E recovery candidate SHA is not pinned')
    head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    tree = subprocess.check_output(['git', 'rev-parse', 'HEAD^{tree}'], cwd=ROOT, text=True).strip()
    dirty = subprocess.check_output(['git', 'status', '--porcelain', '--untracked-files=all'], cwd=ROOT, text=True)
    if head != EXPECTED_HEAD or dirty:
        raise RuntimeError('Source is not clean exact E HEAD')
    return {'head': head, 'tree': tree, 'clean': True}


def dist_proof():
    delta = subprocess.check_output(
        ['git', 'diff', '--name-only', EXPECTED_C_BUILD + '...' + EXPECTED_HEAD, '--', 'frontend'],
        cwd=ROOT, text=True).splitlines()
    if delta != EXPECTED_FRONTEND_DELTA:
        raise RuntimeError('E source is not the exact reviewed test/runner delta from C production build')
    if sha(MANIFEST) != EXPECTED_MANIFEST_SHA:
        raise RuntimeError('C production artifact manifest changed')
    data = json.loads(MANIFEST.read_text())
    if data.get('source_commit') != EXPECTED_C_BUILD or data.get('app_modes') != 'production':
        raise RuntimeError('Manifest is not the completed C production build')
    records = data.get('files')
    if not isinstance(records, list) or len(records) != 328:
        raise RuntimeError('Production artifact count differs')
    expected = set()
    counts = {'admin-web': 0, 'home-web': 0, 'sso-web': 0}
    for record in records:
        name = record.get('file')
        if not isinstance(name, str):
            raise RuntimeError('Invalid artifact path')
        parts = Path(name).parts
        if len(parts) < 5 or parts[:2] != ('frontend', 'apps') or parts[2] not in counts or parts[3] != 'dist':
            raise RuntimeError('Artifact outside three app dist directories')
        path = ROOT / name
        if name in expected or path.is_symlink() or not path.is_file() or sha(path) != record.get('sha256') or path.stat().st_size != record.get('bytes'):
            raise RuntimeError('An exact production artifact differs')
        expected.add(name)
        counts[parts[2]] += 1
    if counts != {'admin-web': 310, 'home-web': 14, 'sso-web': 4}:
        raise RuntimeError('Production artifact app distribution differs')
    actual = set()
    for app in counts:
        folder = FRONTEND / 'apps' / app / 'dist'
        for path in folder.rglob('*'):
            if path.is_symlink():
                raise RuntimeError('Production dist contains a symlink')
            if path.is_file():
                actual.add(path.relative_to(ROOT).as_posix())
    if actual != expected:
        raise RuntimeError('Production dist inventory differs from manifest')
    if sha(TEST) != EXPECTED_TEST_SHA:
        raise RuntimeError('API-controlled test is not the fixed D source')
    if not VITE.is_file() or not PLAYWRIGHT.is_file():
        raise RuntimeError('Local Vite or Playwright CLI missing')
    env = (APP / '.env.production').read_text()
    if not all(re.search(r'^' + key + r'=' + re.escape(value) + r'$', env, re.M) for key, value in {
        'VITE_APP_CONTEXT_PATH': '/', 'VITE_APP_BASE_API': '/prod-api', 'VITE_APP_MESSAGE_ENABLED': 'false'
    }.items()):
        raise RuntimeError('Production fixture origin/API/push flags differ')
    return {'manifest_sha256': EXPECTED_MANIFEST_SHA, 'build_source': EXPECTED_C_BUILD,
            'artifacts': len(records), 'apps': counts, 'test_sha256': EXPECTED_TEST_SHA}


def private(path, content):
    with os.fdopen(os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as out:
        out.write(content)


def clean_env(**updates):
    allowed = ('HOME', 'PATH', 'LANG', 'LC_ALL', 'PLAYWRIGHT_BROWSERS_PATH', 'JAVA_HOME')
    result = {key: os.environ[key] for key in allowed if key in os.environ}
    result.update(updates)
    return result


def free_port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]


def closed(port):
    with socket.socket() as sock:
        sock.settimeout(.25)
        return sock.connect_ex(('127.0.0.1', port)) != 0


def members(pgid):
    result = []
    for entry in Path('/proc').iterdir():
        if not entry.name.isdigit():
            continue
        try:
            fields = (entry / 'stat').read_text().rsplit(')', 1)[1].split()
            if fields[0] not in ('Z', 'X') and int(fields[2]) == pgid and int(fields[3]) == pgid:
                result.append(int(entry.name))
        except (FileNotFoundError, ProcessLookupError):
            pass
    return result


def stop_group(proc):
    for signum, duration in ((signal.SIGTERM, 15), (signal.SIGKILL, 10)):
        if not members(proc.pid):
            proc.poll()
            return []
        try:
            os.killpg(proc.pid, signum)
        except ProcessLookupError:
            pass
        until = time.monotonic() + duration
        while time.monotonic() < until:
            if not members(proc.pid):
                proc.poll()
                return []
            time.sleep(.1)
    proc.poll()
    return members(proc.pid)


def wait_preview(port, proc):
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            raise RuntimeError('Owned preview exited before readiness')
        try:
            connection = http.client.HTTPConnection('127.0.0.1', port, timeout=2)
            connection.request('GET', '/login')
            response = connection.getresponse()
            response.read(1024)
            connection.close()
            if response.status == 200:
                return
        except (OSError, http.client.HTTPException):
            pass
        time.sleep(.25)
    raise RuntimeError('Owned preview readiness timed out')


def safe_location(location):
    """Only exact-test numeric coordinates; never copy Playwright error text."""
    if not isinstance(location, dict) or not isinstance(location.get('file'), str):
        return None
    path = Path(location['file'])
    choices = [path.resolve()] if path.is_absolute() else [
        (base / path).resolve() for base in (ROOT, FRONTEND, FRONTEND / 'e2e')]
    if TEST.resolve() not in choices:
        return None
    line, column = location.get('line'), location.get('column')
    if (type(line) is not int or type(column) is not int or
            line < 1 or line > len(TEST.read_text().splitlines()) or
            column < 1 or column > 10000):
        return None
    return {'line': line, 'column': column}


def case_summary(data):
    stats = data.get('stats') or {}
    values = {key: int(stats.get(key, -1)) for key in ('expected', 'unexpected', 'skipped', 'flaky')}
    cases = []
    def visit(suites, source=None):
        for suite in suites:
            file = suite.get('file') or source
            for spec in suite.get('specs', []):
                for case in spec.get('tests', []):
                    cases.append((spec.get('file') or file, spec.get('title'), case, spec))
            visit(suite.get('suites', []), file)
    visit(data.get('suites', []))
    expected_file = TEST.resolve()
    selected = []
    for file, title, case, spec in cases:
        if not isinstance(file, str):
            raise RuntimeError('Playwright case source is missing')
        path = Path(file)
        choices = [path.resolve()] if path.is_absolute() else [(base / path).resolve() for base in (ROOT, FRONTEND, FRONTEND / 'e2e')]
        if expected_file not in choices or title not in TITLES or case.get('projectName') != 'chromium':
            raise RuntimeError('Playwright case identity differs')
        results = case.get('results', [])
        if len(results) != 1:
            raise RuntimeError('Playwright retry or missing attempt')
        status = results[0].get('status')
        if status not in ('passed', 'failed', 'timedOut', 'interrupted', 'skipped'):
            raise RuntimeError('Playwright case status is invalid')
        record = {'title': title, 'status': status, 'attempts': 1}
        if status != 'passed':
            record['declaration_location'] = safe_location({
                'file': spec.get('file') or file,
                'line': spec.get('line'), 'column': spec.get('column')})
            record['assertion_location'] = safe_location(results[0].get('errorLocation'))
        selected.append(record)
    if len(selected) != 5 or {x['title'] for x in selected} != set(TITLES):
        raise RuntimeError('Playwright did not run the five fixed cases')
    return {'counts': values, 'cases': selected}


def accepted_cases(summary):
    return (summary['counts'] == {'expected': 5, 'unexpected': 0, 'skipped': 0, 'flaky': 0}
            and len(summary['cases']) == 5
            and all(case['status'] == 'passed' and case['attempts'] == 1 for case in summary['cases']))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--execute', action='store_true', help='Launch owned preview and exact five-case Chrome gate')
    args = parser.parse_args()
    if not args.execute:
        print('Prepared only; --execute is required to start a service.')
        return 0
    before = source_identity()
    proof = dist_proof()
    RUN_ROOT.mkdir(mode=0o700, parents=True, exist_ok=True)
    run_id = secrets.token_hex(8)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(mode=0o700)
    port = free_port()
    report = {'source_before': before, 'proof_before': proof, 'run_id': run_id,
              'loopback_port': port, 'acceptance': False, 'cleanup': {'errors': []}}
    processes = []
    raw = [run_dir / 'preview.raw.log', run_dir / 'playwright.raw.json', run_dir / 'playwright.raw.err']
    try:
        config = run_dir / 'playwright.config.mjs'
        private(config, ("export default { testDir: " + json.dumps(str(FRONTEND / 'e2e')) +
                        ", testMatch: 'inbox-without-realtime.spec.ts', fullyParallel: false, workers: 1, retries: 0, reporter: 'json', " +
                        "outputDir: " + json.dumps(str(run_dir / 'artifacts')) +
                        ", use: { baseURL: " + json.dumps(f'http://127.0.0.1:{port}') +
                        ", trace: 'off', video: 'off', screenshot: 'off' }, " +
                        "projects: [{ name: 'chromium', use: { browserName: 'chromium', channel: 'chrome' } }] };\n").encode())
        preview_argv = ('node', str(VITE), 'preview', '--host', '127.0.0.1', '--port', str(port), '--strictPort', '--mode', 'production')
        with os.fdopen(os.open(raw[0], os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as log:
            preview = subprocess.Popen(preview_argv, cwd=APP, start_new_session=True,
                                       env=clean_env(BROWSER='none', VITE_APP_CONTEXT_PATH='/', VITE_APP_BASE_API='/prod-api',
                                                     VITE_APP_MESSAGE_ENABLED='false'), stdout=log, stderr=subprocess.STDOUT)
        processes.append(('preview', preview))
        report['preview_pgid'] = preview.pid
        wait_preview(port, preview)
        playwright_argv = ('node', str(PLAYWRIGHT), 'test', str(TEST), '--config', str(config), '--workers=1', '--reporter=json')
        with os.fdopen(os.open(raw[1], os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as out, \
             os.fdopen(os.open(raw[2], os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as err:
            browser = subprocess.Popen(playwright_argv, cwd=FRONTEND, start_new_session=True,
                                       env=clean_env(CI='1'), stdout=out, stderr=err)
            processes.append(('playwright', browser))
            report['playwright_pgid'] = browser.pid
            browser.wait(timeout=420)
        report['playwright_exit_code'] = browser.returncode
        report['playwright_raw_sha256'] = sha(raw[1])
        report['playwright'] = case_summary(json.loads(raw[1].read_text()))
        if not accepted_cases(report['playwright']):
            raise RuntimeError('Playwright five-case count or status gate failed')
        if browser.returncode:
            raise RuntimeError('Playwright exited nonzero')
    except BaseException as error:
        report['error_type'] = type(error).__name__
        report['error'] = str(error) if type(error) is RuntimeError else type(error).__name__
    finally:
        errors = report['cleanup']['errors']
        for name, proc in reversed(processes):
            try:
                live = stop_group(proc)
                report['cleanup'][name + '_group'] = {'pgid': proc.pid, 'live_members': live}
                if live:
                    errors.append(name + '_group_remains')
            except Exception:
                errors.append(name + '_group_cleanup_failed')
        for path in raw:
            try:
                path.unlink(missing_ok=True)
            except Exception:
                errors.append('raw_report_or_log_removal_failed')
        try:
            shutil.rmtree(run_dir / 'artifacts')
        except FileNotFoundError:
            pass
        except Exception:
            errors.append('playwright_artifacts_removal_failed')
        try:
            (run_dir / 'playwright.config.mjs').unlink(missing_ok=True)
        except Exception:
            errors.append('private_config_removal_failed')
        try:
            report['cleanup']['port_closed'] = closed(port)
            if not report['cleanup']['port_closed']:
                errors.append('preview_port_remains')
        except Exception:
            errors.append('preview_port_check_failed')
        try:
            report['source_after'] = source_identity()
            if report['source_after'] != before:
                errors.append('source_changed')
        except Exception:
            errors.append('source_after_not_clean_exact_E')
        try:
            report['proof_after'] = dist_proof()
            if report['proof_after'] != proof:
                errors.append('production_dist_changed')
        except Exception:
            errors.append('production_dist_after_failed')
        report['acceptance'] = 'playwright' in report and not errors and 'error' not in report
        report['exit_code'] = 0 if report['acceptance'] else 1
        private(run_dir / 'result.json', (json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode())
        print(json.dumps({'result': str(run_dir / 'result.json'), 'exit_code': report['exit_code']}))
    return report['exit_code']


if __name__ == '__main__':
    sys.exit(main())
