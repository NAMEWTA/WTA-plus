#!/usr/bin/env python3
"""Draft T-40 owned MySQL/Redis one-class integration runner.

Default invocation is preparation only. Only the Lead may use --execute with
an exact clean implementation SHA after the test class and opt-in seam land.
No production credentials, vendor endpoints, shared database, or real sender.
"""

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import signal
import socket
import string
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path('/srv/WTA-plus')
RUN_ROOT = Path('/tmp/wta-t40/runs')
DOCKER = ('docker', '--host', 'unix:///var/run/docker.sock')
OWNER = 'T-40-NOTICE-RETRACT'
SQL_ROOT = ROOT / 'release-artifacts/docker/infrastructure/mysql/init'
SQL_NAMES = ('10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql',
             '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql')
TEST_CLASS = 'org.namewta.test.notify.NotifyNoticeRetractionIntegrationTest'
TEST_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyNoticeRetractionIntegrationTest.java'
TEST_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + TEST_CLASS + '.xml')
OPT_IN = 'notify.notice.retraction.integration'
RED_METHOD = 'retractBeforeWorkerClaimStopsCurrentVersionAfterPositiveDeliveryControl'


def safe_env(**updates):
    allowed = ('PATH', 'HOME', 'LANG', 'LC_ALL', 'JAVA_HOME', 'TMPDIR')
    env = {key: os.environ[key] for key in allowed if key in os.environ}
    env["JAVA_TOOL_OPTIONS"] = "-Xms128m -Xmx1536m"
    env.update(updates)
    return env


def run(argv, *, cwd=ROOT, data=None, timeout=180):
    return subprocess.run(argv, cwd=cwd, input=data, stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE, env=safe_env(), timeout=timeout,
                          check=False)


def output(result):
    return result.stdout.decode('utf-8', errors='replace').strip()


def git(*args):
    result = run(('git', *args), timeout=30)
    if result.returncode:
        raise RuntimeError('Read-only Git inspection failed')
    return output(result)


def source_identity(expected):
    head = git('rev-parse', 'HEAD')
    dirty = git('status', '--porcelain', '--untracked-files=all')
    if head != expected or dirty:
        raise RuntimeError('Source is not the exact expected clean HEAD')
    return {'head': head, 'tree': git('rev-parse', 'HEAD^{tree}'), 'clean': True}


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def private(path, content):
    if isinstance(content, str):
        content = content.encode()
    with os.fdopen(os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as out:
        out.write(content)


def random_password():
    alphabet = string.ascii_letters + string.digits
    return ''.join(secrets.choice(alphabet) for _ in range(40))


def docker(*args, data=None, timeout=180):
    result = run((*DOCKER, *args), data=data, timeout=timeout)
    if result.returncode:
        raise RuntimeError('Owned Docker operation failed: ' + ' '.join(args[:2]))
    return output(result)


def full_id(value):
    if not re.fullmatch(r'[0-9a-f]{64}', value):
        raise RuntimeError('Docker did not return a full container ID')
    return value


def owned_ids(run_id):
    found = docker('ps', '-aq', '--no-trunc', '--filter', 'label=namewta.test.owner=' + OWNER,
                   '--filter', 'label=namewta.test.run=' + run_id)
    return [full_id(item) for item in found.splitlines()] if found else []


def assert_owned(cid, run_id):
    labels = json.loads(docker('inspect', '--format', '{{json .Config.Labels}}', cid))
    if labels.get('namewta.test.owner') != OWNER or labels.get('namewta.test.run') != run_id:
        raise RuntimeError('Container fails both exact owner/run labels')


def volume_names(cid):
    mounts = json.loads(docker('inspect', '--format', '{{json .Mounts}}', cid))
    names = []
    for mount in mounts:
        if mount.get('Type') == 'volume':
            name = mount.get('Name', '')
            if not re.fullmatch(r'[0-9a-f]{64}', name):
                raise RuntimeError('Owned container has an unexpected non-anonymous volume')
            names.append(name)
    return names


def volume_absent(name):
    result = run((*DOCKER, 'volume', 'inspect', name), timeout=30)
    if result.returncode == 0:
        return False
    if (result.returncode == 1 and name.encode('ascii') in result.stderr
            and re.search(rb'\bno such volume\b', result.stderr, re.IGNORECASE)):
        return True
    raise RuntimeError('Owned volume absence could not be verified')


def mapped_port(cid, inside):
    host, sep, port = docker('port', cid, inside + '/tcp').rpartition(':')
    if not sep or host != '127.0.0.1' or not port.isdigit():
        raise RuntimeError('Published port is not a random loopback binding')
    return int(port)


def closed(port):
    with socket.socket() as client:
        client.settimeout(.3)
        return client.connect_ex(('127.0.0.1', port)) != 0


def wait_ready(cid, role, seconds):
    probe = ('exec', cid, 'sh', '-c',
             'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqladmin ping -h 127.0.0.1 -uroot --silent') \
            if role == 'mysql' else ('exec', cid, 'redis-cli', 'ping')
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        result = run((*DOCKER, *probe), timeout=12)
        if result.returncode == 0 and (role == 'mysql' or output(result) == 'PONG'):
            return
        time.sleep(1)
    raise RuntimeError('Owned ' + role + ' readiness timed out')


class OwnedSqlError(RuntimeError):
    def __init__(self, stage, exit_code, number=None, sqlstate=None):
        self.safe = {'stage': stage, 'exit_code': exit_code,
                     'mysql_error_number': number, 'sqlstate': sqlstate}
        super().__init__('Owned SQL stage failed: ' + stage)


def mysql(cid, database, sql, stage):
    if not (stage in ('table_count', 'grant_app', 'baseline_outbox', 'baseline_external',
                      'baseline_accounts') or re.fullmatch(r'import_(10|20|30|40|50|60)', stage)):
        raise RuntimeError('Unreviewed owned SQL stage')
    payload = sql.encode() if isinstance(sql, str) else sql
    command = (*DOCKER, 'exec', '-i', cid, 'sh', '-c',
               'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch --skip-column-names --database="$1"',
               'sh', database)
    try:
        result = run(command, data=payload, timeout=180)
    except subprocess.TimeoutExpired:
        raise OwnedSqlError(stage, None) from None
    if result.returncode:
        match = re.search(rb'\bERROR\s+([0-9]{1,5})\s+\(([A-Z0-9]{5})\)', result.stderr)
        raise OwnedSqlError(stage, result.returncode,
                            int(match.group(1)) if match else None,
                            match.group(2).decode('ascii') if match else None)
    return output(result)


def live_group_members(pgid):
    members = []
    for entry in Path('/proc').iterdir():
        if not entry.name.isdigit():
            continue
        try:
            fields = (entry / 'stat').read_text().rsplit(')', 1)[1].split()
        except (FileNotFoundError, ProcessLookupError):
            continue
        if len(fields) >= 4 and fields[0] not in ('Z', 'X') and int(fields[2]) == pgid and int(fields[3]) == pgid:
            members.append(int(entry.name))
    return members


def stop_group(proc):
    for signum, seconds in ((signal.SIGTERM, 15), (signal.SIGKILL, 10)):
        if not live_group_members(proc.pid):
            proc.poll()
            return []
        try:
            os.killpg(proc.pid, signum)
        except ProcessLookupError:
            pass
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            if not live_group_members(proc.pid):
                proc.poll()
                return []
            time.sleep(.1)
    remaining = live_group_members(proc.pid)
    proc.poll()
    return remaining


def cleanup_containers(run_id, captured):
    errors = []
    remaining = None
    try:
        discovered = owned_ids(run_id)
        if not set(captured).issubset(discovered):
            errors.append('captured_container_missing_from_dual_label_discovery')
        for cid in reversed(discovered):
            try:
                assert_owned(cid, run_id)
                docker('rm', '-fv', cid, timeout=60)
            except Exception:
                errors.append('owned_container_remove_failed')
        remaining = owned_ids(run_id)
        if remaining:
            errors.append('owned_containers_remain')
    except Exception:
        errors.append('owned_container_discovery_or_verification_failed')
    return remaining, errors


def source_preflight():
    if not TEST_SOURCE.is_file():
        raise RuntimeError('T40 exact integration class is not present yet')
    source = TEST_SOURCE.read_text()
    if 'class NotifyNoticeRetractionIntegrationTest' not in source:
        raise RuntimeError('T40 exact class source differs')
    pattern = r'@EnabledIfSystemProperty\s*\(\s*named\s*=\s*"notify\.notice\.retraction\.integration"\s*,\s*matches\s*=\s*"true"'
    if not re.search(pattern, source):
        raise RuntimeError('T40 class lacks explicit opt-in gate')
    if 'T40_MYSQL_PASSWORD' not in source:
        raise RuntimeError('T40 class lacks owned subprocess password seam')
    if 'T36_MYSQL_PASSWORD' not in source:
        raise RuntimeError('T40 class lacks the same owned password fixture alias')
    if not re.search(r'@Test\s+void\s+' + RED_METHOD + r'\s*\(', source):
        raise RuntimeError('T40 business red method is absent')
    direct = 'notify.mysql.integration.url' in source and 'notify.redis.integration.port' in source
    atomic = 'NotifyAtomicResultIntegrationTest' in source and '.open()' in source
    if not (direct or atomic):
        raise RuntimeError('T40 class lacks an owned MySQL/Redis fixture seam')
    return {'class': TEST_CLASS, 'source_sha256': sha(TEST_SOURCE), 'opt_in': OPT_IN,
            'atomic_fixture': atomic, 'direct_fixture': direct}


def sanitized_xml(path, started_ns, owned_secrets):
    if not path.is_file() or path.stat().st_mtime_ns < started_ns:
        raise RuntimeError('Exact Surefire XML missing or stale')
    raw = path.read_bytes()
    original_sha = hashlib.sha256(raw).hexdigest()
    clean, replacements = redact_exact_owned_secrets(raw, owned_secrets)
    if replacements:
        # Keep assertion details and all other original XML bytes. The target
        # cannot retain owned secrets even if the XML is malformed.
        replacement = path.with_name(path.name + '.t40-redacted-' + secrets.token_hex(8))
        private(replacement, clean)
        os.replace(replacement, path)
    root = ET.fromstring(clean)
    if root.tag != 'testsuite' or root.attrib.get('name') != TEST_CLASS:
        raise RuntimeError('Surefire XML is not the selected T40 class')
    counts = {name: int(root.attrib.get(name, '0')) for name in ('tests', 'failures', 'errors', 'skipped')}
    methods = []
    for testcase in root.findall('testcase'):
        method = {'name': testcase.attrib.get('name', ''),
                  'failure_types': [child.attrib.get('type', '') for child in testcase
                                    if child.tag in ('failure', 'error')]}
        methods.append(method)
    if RED_METHOD not in [method['name'] for method in methods]:
        raise RuntimeError('Fresh T40 XML did not execute the required red method')
    provenance = {'source_sha256': original_sha,
                  'retained_sha256': hashlib.sha256(clean).hexdigest(),
                  'exact_owned_secret_replacements': replacements}
    return counts, methods, clean, provenance


def redact_exact_owned_secrets(raw, owned_secrets):
    clean, replacements = raw, 0
    for secret in dict.fromkeys(secret.encode() for secret in owned_secrets if secret):
        replacements += clean.count(secret)
        clean = clean.replace(secret, b'[REDACTED-OWNED-SECRET]')
    return clean, replacements


def sanitize_log(raw, clean, owned_secrets):
    try:
        if raw.exists():
            data = raw.read_bytes()
            source_sha = hashlib.sha256(data).hexdigest()
            retained, replacements = redact_exact_owned_secrets(data, owned_secrets)
            private(clean, retained)
            return {'source_sha256': source_sha,
                    'retained_sha256': hashlib.sha256(retained).hexdigest(),
                    'exact_owned_secret_replacements': replacements}
    finally:
        raw.unlink(missing_ok=True)
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--execute', action='store_true')
    parser.add_argument('--expected-head')
    args = parser.parse_args()
    if not args.execute:
        if args.expected_head:
            parser.error('--expected-head requires --execute')
        print(json.dumps({'status': 'draft_prepared_only', 'class_source_present': TEST_SOURCE.is_file(),
                          'execution': 'not_started'}))
        return 0
    if not args.expected_head or not re.fullmatch(r'[0-9a-f]{40}', args.expected_head):
        parser.error('no launch without --execute and --expected-head <40-hex clean SHA>')

    os.umask(0o077)
    run_id = secrets.token_hex(8)
    RUN_ROOT.mkdir(parents=True, exist_ok=True, mode=0o700)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(mode=0o700)
    database = 'namewta_notify_test_' + run_id
    app_user = 't40_' + run_id
    root_password, app_password = random_password(), random_password()
    mysql_env = run_dir / 'mysql-container.env'
    raw_log = run_dir / 'maven.raw.log'
    clean_log = run_dir / 'maven.log'
    report = {'gate': 'T40 owned notice-version retraction integration', 'run_id': run_id,
              'acceptance': False, 'exit_code': 1, 'started_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'source_before': None, 'owned': {}, 'sql_baselines': [], 'cleanup': {'errors': []}}
    captured, volumes, ports = [], [], []
    proc = None
    started_ns = None
    docker_touched = False
    try:
        report['source_before'] = source_identity(args.expected_head)
        report['test_source'] = source_preflight()
        for name in SQL_NAMES:
            path = SQL_ROOT / name
            if not path.is_file():
                raise RuntimeError('Exact six-SQL baseline file missing')
            report['sql_baselines'].append({'file': str(path.relative_to(ROOT)), 'sha256': sha(path)})
        private(mysql_env, 'MYSQL_ROOT_PASSWORD=' + root_password + '\nMYSQL_DATABASE=' + database + '\n')
        mysql_args = ('run', '--pull=never', '-d', '--name', 't40-mysql-' + run_id,
                      '--label', 'namewta.test.owner=' + OWNER,
                      '--label', 'namewta.test.run=' + run_id,
                      '-p', '127.0.0.1::3306', '--env-file', str(mysql_env),
                      'mysql:8.4.9', '--character-set-server=utf8mb4',
                      '--collation-server=utf8mb4_general_ci', '--log-bin-trust-function-creators=1')
        docker_touched = True
        mysql_id = full_id(docker(*mysql_args))
        captured.append(mysql_id)
        assert_owned(mysql_id, run_id)
        volumes.extend(volume_names(mysql_id))
        wait_ready(mysql_id, 'mysql', 120)
        mysql_port = mapped_port(mysql_id, '3306')
        ports.append(mysql_port)
        for entry in report['sql_baselines']:
            stage = 'import_' + Path(entry['file']).name[:2]
            mysql(mysql_id, database, (ROOT / entry['file']).read_bytes(), stage)
            entry['import_exit_code'] = 0
        count = int(mysql(mysql_id, database,
                          'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();',
                          'table_count'))
        if count != 103:
            raise RuntimeError('Owned fresh schema has wrong table count')
        report['owned']['table_count'] = count
        baseline = {
            'outbox': int(mysql(mysql_id, database, 'SELECT COUNT(*) FROM notify_outbox;', 'baseline_outbox')),
            'external_deliveries': int(mysql(mysql_id, database,
                "SELECT COUNT(*) FROM notify_delivery WHERE channel IN ('SMS','MAIL');", 'baseline_external')),
            'enabled_external_accounts': int(mysql(mysql_id, database,
                "SELECT COUNT(*) FROM notify_channel_account WHERE channel IN ('SMS','MAIL') AND enabled='Y';",
                'baseline_accounts'))}
        report['owned']['baseline_counts'] = baseline
        if any(baseline.values()):
            raise RuntimeError('Owned schema is not a no-supplier-call fresh baseline')
        grant = ("CREATE USER '" + app_user + "'@'%' IDENTIFIED BY '" + app_password + "';"
                 'GRANT ALL PRIVILEGES ON `' + database + "`.* TO '" + app_user + "'@'%';")
        mysql(mysql_id, database, grant, 'grant_app')
        redis_id = full_id(docker('run', '--pull=never', '-d', '--name', 't40-redis-' + run_id,
                                  '--label', 'namewta.test.owner=' + OWNER,
                                  '--label', 'namewta.test.run=' + run_id,
                                  '-p', '127.0.0.1::6379', 'redis:8.6.3',
                                  'redis-server', '--save', '', '--appendonly', 'no'))
        captured.append(redis_id)
        assert_owned(redis_id, run_id)
        volumes.extend(volume_names(redis_id))
        wait_ready(redis_id, 'redis', 45)
        redis_port = mapped_port(redis_id, '6379')
        ports.append(redis_port)
        report['owned']['container_full_ids'] = list(captured)
        report['owned']['loopback_ports'] = {'mysql': mysql_port, 'redis': redis_port}
        jdbc = (f'jdbc:mysql://127.0.0.1:{mysql_port}/{database}'
                '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai')
        argv = ('./mvnw', '-Pdev', '-pl', 'wta-admin', '-am', '-Dtest=NotifyNoticeRetractionIntegrationTest',
                '-Dsurefire.failIfNoSpecifiedTests=false', '-DforkCount=1', '-DreuseForks=false',
                '-D' + OPT_IN + '=true', '-Dnotify.mysql.integration.url=' + jdbc,
                '-Dnotify.mysql.integration.username=' + app_user,
                '-Dnotify.redis.integration.port=' + str(redis_port), 'test')
        if any(secret in ' '.join(argv) for secret in (root_password, app_password)):
            raise RuntimeError('Owned secret entered Maven argv')
        report['maven_command'] = list(argv)
        started_ns = time.time_ns()
        with os.fdopen(os.open(raw_log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
            proc = subprocess.Popen(argv, cwd=ROOT / 'backend', stdout=stream, stderr=subprocess.STDOUT,
                                    start_new_session=True,
                                    env=safe_env(T40_MYSQL_PASSWORD=app_password,
                                                 T36_MYSQL_PASSWORD=app_password))
            try:
                report['maven_exit_code'] = proc.wait(timeout=3600)
            except subprocess.TimeoutExpired:
                raise RuntimeError('T40 selected Maven test exceeded one-hour limit') from None
        counts, methods, clean, provenance = sanitized_xml(TEST_REPORT, started_ns,
                                                            (root_password, app_password))
        report['counts'], report['methods'] = counts, methods
        private(run_dir / 'selected-surefire.xml', clean)
        report['xml_provenance'] = provenance
        if sha(run_dir / 'selected-surefire.xml') != provenance['retained_sha256']:
            raise RuntimeError('Retained XML byte identity differs')
        if (report['maven_exit_code'] != 0 or counts['tests'] <= 0
                or any(counts[key] for key in ('failures', 'errors', 'skipped'))):
            raise RuntimeError('Exact T40 class failed, skipped or ran zero tests')
        report['exit_code'] = 0
    except BaseException as error:
        report['error_type'] = type(error).__name__
        if isinstance(error, OwnedSqlError):
            report['sql_failure'] = error.safe
        else:
            report['error'] = type(error).__name__ if type(error) is not RuntimeError else str(error)
    finally:
        errors = report['cleanup']['errors']
        if started_ns is not None and TEST_REPORT.is_file() and TEST_REPORT.stat().st_mtime_ns >= started_ns:
            try:
                if 'counts' not in report:
                    counts, methods, clean, provenance = sanitized_xml(TEST_REPORT, started_ns,
                                                                        (root_password, app_password))
                    report['counts'], report['methods'] = counts, methods
                    private(run_dir / 'selected-surefire.xml', clean)
                    report['xml_provenance'] = provenance
                    if sha(run_dir / 'selected-surefire.xml') != provenance['retained_sha256']:
                        errors.append('retained_xml_byte_identity_differs')
            except Exception:
                errors.append('fresh_surefire_xml_redaction_or_retention_failed')
        if proc is not None:
            try:
                live = stop_group(proc)
                report['cleanup']['maven_process_group'] = {'pgid': proc.pid, 'live_members': live}
                if live:
                    errors.append('maven_process_group_remains')
            except Exception:
                errors.append('maven_process_group_cleanup_failed')
        try:
            report['maven_log_provenance'] = sanitize_log(raw_log, clean_log,
                                                           (root_password, app_password))
        except Exception:
            errors.append('maven_log_redaction_or_removal_failed')
        try:
            mysql_env.unlink(missing_ok=True)
        except Exception:
            errors.append('private_mysql_env_removal_failed')
        try:
            for cid in owned_ids(run_id) if docker_touched else []:
                assert_owned(cid, run_id)
                volumes.extend(volume_names(cid))
        except Exception:
            errors.append('owned_volume_discovery_failed')
        report['owned']['captured_anonymous_volume_names'] = list(dict.fromkeys(volumes))
        remaining, container_errors = cleanup_containers(run_id, captured) if docker_touched else ([], [])
        report['cleanup']['remaining_owned_full_ids'] = remaining
        errors.extend(container_errors)
        try:
            report['cleanup']['anonymous_volumes_absent'] = {
                name: volume_absent(name) for name in dict.fromkeys(volumes)}
            if not all(report['cleanup']['anonymous_volumes_absent'].values()):
                errors.append('owned_anonymous_volume_remains')
        except Exception:
            errors.append('owned_volume_absence_check_failed')
        try:
            report['cleanup']['loopback_ports_closed'] = {str(port): closed(port) for port in ports}
            if not all(report['cleanup']['loopback_ports_closed'].values()):
                errors.append('owned_loopback_port_remains')
        except Exception:
            errors.append('owned_loopback_port_check_failed')
        try:
            report['source_after'] = source_identity(args.expected_head)
            if report['source_after'] != report.get('source_before'):
                errors.append('source_identity_changed')
        except Exception:
            errors.append('source_after_not_clean_exact_head')
        report['cleanup']['errors'] = errors
        report['exit_code'] = int(bool(report['exit_code'] or errors))
        report['acceptance'] = report['exit_code'] == 0
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        private(run_dir / 'result.json', json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps({'result': str(run_dir / 'result.json'), 'exit_code': report['exit_code']}))
    return report['exit_code']


if __name__ == '__main__':
    sys.exit(main())
