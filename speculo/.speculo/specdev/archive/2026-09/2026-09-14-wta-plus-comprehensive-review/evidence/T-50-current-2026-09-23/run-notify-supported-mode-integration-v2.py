#!/usr/bin/env python3
"""Opt-in, owned T-50 supported-mode and T-36/T-37/T-38/T-39 regression acceptance runner."""

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
RUN_ROOT = Path('/tmp/wta-t50/runs')
DOCKER = ('docker', '--host', 'unix:///var/run/docker.sock')
SQL_NAMES = (
    '10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql',
    '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql',
)
SQL_ROOT = ROOT / 'release-artifacts/docker/infrastructure/mysql/init'
TEST_CLASS = 'org.namewta.test.notify.NotifyDeadlineIntegrationTest'
TEST_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyDeadlineIntegrationTest.java'
TEST_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + TEST_CLASS + '.xml')
PROFILE_CLASS = 'org.namewta.test.notify.EnterpriseQueuedNotificationIntegrationTest'
PROFILE_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/EnterpriseQueuedNotificationIntegrationTest.java'
PROFILE_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + PROFILE_CLASS + '.xml')
WAKE_CLASS = 'org.namewta.test.notify.NotifyWakeIntegrationTest'
WAKE_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyWakeIntegrationTest.java'
WAKE_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + WAKE_CLASS + '.xml')
MODE_CLASS = 'org.namewta.test.notify.NotifySupportedModeIntegrationTest'
MODE_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/NotifySupportedModeIntegrationTest.java'
MODE_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + MODE_CLASS + '.xml')
ATOMIC_CLASS = 'org.namewta.test.notify.NotifyAtomicResultIntegrationTest'
ATOMIC_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyAtomicResultIntegrationTest.java'
ATOMIC_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + ATOMIC_CLASS + '.xml')
MANUAL_CLASS = 'org.namewta.test.notify.NotifyManualRetryIntegrationTest'
MANUAL_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyManualRetryIntegrationTest.java'
MANUAL_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + MANUAL_CLASS + '.xml')
SMS_CLASS = 'org.namewta.test.notify.NotifySmsDispatchIntegrationTest'
SMS_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/NotifySmsDispatchIntegrationTest.java'
SMS_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + SMS_CLASS + '.xml')
IDEMPOTENCY_CLASS = 'org.namewta.test.notify.idempotency.RedisNotifyIdempotencyStoreIntegrationTest'
IDEMPOTENCY_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/idempotency/RedisNotifyIdempotencyStoreIntegrationTest.java'
IDEMPOTENCY_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + IDEMPOTENCY_CLASS + '.xml')
SUITES = ((TEST_CLASS, TEST_REPORT), (PROFILE_CLASS, PROFILE_REPORT),
          (WAKE_CLASS, WAKE_REPORT), (MODE_CLASS, MODE_REPORT),
          (ATOMIC_CLASS, ATOMIC_REPORT), (MANUAL_CLASS, MANUAL_REPORT),
          (SMS_CLASS, SMS_REPORT), (IDEMPOTENCY_CLASS, IDEMPOTENCY_REPORT))


def safe_env(**updates):
    allowed = ('PATH', 'HOME', 'LANG', 'LC_ALL', 'JAVA_HOME', 'TMPDIR')
    result = {key: os.environ[key] for key in allowed if key in os.environ}
    result.update(updates)
    return result


def run(argv, *, cwd=ROOT, data=None, env=None, timeout=180):
    return subprocess.run(argv, cwd=cwd, input=data, stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE, env=env or safe_env(), timeout=timeout,
                          check=False)


def output(result):
    return result.stdout.decode('utf-8', errors='replace').strip()


def git(*args):
    result = run(('git', *args), timeout=30)
    if result.returncode:
        raise RuntimeError('Git inspection failed')
    return output(result)


def source_identity(expected):
    head = git('rev-parse', 'HEAD')
    dirty = git('status', '--porcelain', '--untracked-files=all')
    if head != expected or dirty:
        raise RuntimeError('Source is not the exact expected clean HEAD')
    return {'head': head, 'tree': git('rev-parse', 'HEAD^{tree}'), 'clean': True}


def docker(*args, data=None, timeout=180):
    result = run((*DOCKER, *args), data=data, timeout=timeout)
    if result.returncode:
        raise RuntimeError('Owned Docker operation failed: ' + ' '.join(args[:2]))
    return output(result)


def full_id(value):
    if not re.fullmatch(r'[0-9a-f]{64}', value):
        raise RuntimeError('Docker did not return a full container ID')
    return value


def labels(cid):
    return json.loads(docker('inspect', '--format', '{{json .Config.Labels}}', cid))


def owned_ids(run_id):
    found = docker('ps', '-aq', '--no-trunc', '--filter', 'label=namewta.test.owner=T-50',
                   '--filter', 'label=namewta.test.run=' + run_id)
    return [full_id(item) for item in found.splitlines()] if found else []


def assert_owned(cid, run_id):
    actual = labels(cid)
    if actual.get('namewta.test.owner') != 'T-50' or actual.get('namewta.test.run') != run_id:
        raise RuntimeError('Container fails both exact owner/run label checks')


def mapped_port(cid, inside):
    binding = docker('port', cid, inside + '/tcp')
    host, sep, number = binding.rpartition(':')
    if not sep or host != '127.0.0.1' or not number.isdigit():
        raise RuntimeError('Published port is not a random loopback binding')
    return int(number)


def closed(port):
    with socket.socket() as client:
        client.settimeout(.3)
        return client.connect_ex(('127.0.0.1', port)) != 0


def wait_ready(cid, role, deadline):
    command = ('exec', cid, 'sh', '-c',
               'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqladmin ping -h 127.0.0.1 -uroot --silent') \
              if role == 'mysql' else ('exec', cid, 'redis-cli', 'ping')
    end = time.monotonic() + deadline
    while time.monotonic() < end:
        result = run((*DOCKER, *command), timeout=12)
        if result.returncode == 0 and (role == 'mysql' or output(result) == 'PONG'):
            return
        time.sleep(1)
    raise RuntimeError(role + ' owned container readiness timed out')


def mysql(cid, database, sql):
    if isinstance(sql, str):
        sql = sql.encode('utf-8')
    return docker('exec', '-i', cid, 'sh', '-c',
                  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch --skip-column-names --database="$1"',
                  'sh', database, data=sql, timeout=180)


def write_secret(path, content):
    with os.fdopen(os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'w') as stream:
        stream.write(content)


def random_password():
    alphabet = string.ascii_letters + string.digits
    return ''.join(secrets.choice(alphabet) for _ in range(40))


def mysql_run_args(run_id, mysql_env):
    return ('run', '--pull=never', '-d', '--name', 't50-mysql-' + run_id,
            '--label', 'namewta.test.owner=T-50',
            '--label', 'namewta.test.run=' + run_id,
            '-p', '127.0.0.1::3306', '--env-file', str(mysql_env),
            'mysql:8.4.9', '--character-set-server=utf8mb4',
            '--collation-server=utf8mb4_general_ci',
            '--log-bin-trust-function-creators=1')


def hash_file(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def xml_counts_for(test_class, report_path, started_ns):
    if not report_path.is_file() or report_path.stat().st_mtime_ns < started_ns:
        raise RuntimeError('Target Surefire XML is absent or stale')
    suite = ET.parse(report_path).getroot()
    if suite.attrib.get('name') != test_class:
        raise RuntimeError('Surefire XML is not the exact selected test class')
    counts = {key: int(suite.attrib.get(key, '0')) for key in ('tests', 'failures', 'errors', 'skipped')}
    return counts


def xml_methods_for(report_path):
    return [case.attrib.get('name', '') for case in ET.parse(report_path).getroot().findall('testcase')]


def redact_fresh_xml(report_path, started_ns, secrets_to_redact):
    if started_ns is None or not report_path.is_file() or report_path.stat().st_mtime_ns < started_ns:
        return False
    content = report_path.read_text(errors='replace')
    clean = content
    for value in secrets_to_redact:
        if value:
            clean = clean.replace(value, '[REDACTED]')
    if clean != content:
        report_path.write_text(clean)
        return True
    return False


def xml_verdict_for(test_class, report_path, started_ns):
    counts = xml_counts_for(test_class, report_path, started_ns)
    if counts['tests'] <= 0 or any(counts[key] for key in ('failures', 'errors', 'skipped')):
        raise RuntimeError('Target Surefire class has zero tests, failures, errors or skips')
    return counts


def sanitize_log(raw, clean, secrets_to_redact):
    try:
        if raw.exists():
            content = raw.read_text(errors='replace')
            for value in secrets_to_redact:
                if value:
                    content = content.replace(value, '[REDACTED]')
            write_secret(clean, content)
    finally:
        raw.unlink(missing_ok=True)


def live_group_members(pgid):
    """An exited Maven leader must not conceal a surviving forked JVM."""
    members = []
    for entry in Path('/proc').iterdir():
        if not entry.name.isdigit():
            continue
        try:
            fields = (entry / 'stat').read_text().rsplit(')', 1)[1].split()
        except (FileNotFoundError, ProcessLookupError):
            continue
        if (len(fields) >= 4 and fields[0] not in ('Z', 'X')
                and int(fields[2]) == pgid and int(fields[3]) == pgid):
            members.append(int(entry.name))
    return members


def stop_group(proc):
    pgid = proc.pid  # start_new_session=True sets both session and group to pid.
    for signum, seconds in ((signal.SIGTERM, 15), (signal.SIGKILL, 10)):
        if not live_group_members(pgid):
            proc.poll()
            return []
        try:
            os.killpg(pgid, signum)
        except ProcessLookupError:
            pass
        end = time.monotonic() + seconds
        while time.monotonic() < end:
            remaining = live_group_members(pgid)
            if not remaining:
                proc.poll()
                return []
            time.sleep(.1)
    remaining = live_group_members(pgid)
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


def volume_names(cid):
    mounts = json.loads(docker('inspect', '--format', '{{json .Mounts}}', cid))
    names = []
    for mount in mounts:
        if mount.get('Type') != 'volume':
            continue
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


def validate_mode_source(mode_text):
    """Fail before launching services if the provisional T50 real seam is absent."""
    if 'class NotifySupportedModeIntegrationTest' not in mode_text:
        raise RuntimeError('T50 exact integration class is absent')
    if not re.search(r'@EnabledIfSystemProperty\s*\(\s*named\s*=\s*"notify\.supported\.mode\.integration"\s*,\s*matches\s*=\s*"true"', mode_text):
        raise RuntimeError('T50 integration class lacks its opt-in property')
    if 'T50_MYSQL_PASSWORD' not in mode_text:
        raise RuntimeError('T50 integration class lacks the owned DB/Redis credential seam')
    direct_seam = 'notify.mysql.integration.url' in mode_text and 'notify.redis.integration.port' in mode_text
    atomic_fixture_seam = 'NotifyAtomicResultIntegrationTest' in mode_text and '.open()' in mode_text
    if not (direct_seam or atomic_fixture_seam):
        raise RuntimeError('T50 integration class lacks the owned DB/Redis fixture seam')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--execute', action='store_true', help='Explicit launch; never used in preparation')
    parser.add_argument('--expected-head', help='Exact full clean implementation commit SHA')
    args = parser.parse_args()
    if not args.execute or not args.expected_head or not re.fullmatch(r'[0-9a-f]{40}', args.expected_head):
        parser.error('no launch without --execute and --expected-head <40-hex clean SHA>')

    os.umask(0o077)
    run_id = secrets.token_hex(8)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(parents=True, mode=0o700)
    database = 'namewta_notify_test_' + run_id
    app_user = 't50_' + run_id
    root_password, app_password = random_password(), random_password()
    mysql_env = run_dir / 'mysql-container.env'
    test_env = run_dir / 'test-process.env'
    raw_log = run_dir / 'maven.raw.log'
    clean_log = run_dir / 'maven.log'
    report = {'gate': 'T-50 supported-mode and T-36/T-37/T-38/T-39 eight-class regression', 'run_id': run_id,
              'mode': 'acceptance',
              'acceptance': False,
              'started_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'exit_code': 1, 'owned': {}, 'sql_baselines': [], 'cleanup': {}, 'counts': None}
    captured = []
    ports = []
    volumes = []
    errors = []
    maven_proc = None
    started_ns = None
    docker_touched = False
    try:
        report['source_before'] = source_identity(args.expected_head)
        test_text = TEST_SOURCE.read_text()
        if 'class NotifyDeadlineIntegrationTest' not in test_text:
            raise RuntimeError('Target source does not define the exact selected class')
        if 'T39_MYSQL_PASSWORD' not in test_text:
            raise RuntimeError('Target test must read T39_MYSQL_PASSWORD from process environment')
        if not re.search(r'@EnabledIfSystemProperty\s*\(\s*named\s*=\s*"notify\.deadline\.integration"\s*,\s*matches\s*=\s*"true"', test_text):
            raise RuntimeError('Target test must keep the explicit opt-in integration property')
        # The new class may use the existing Atomic fixture rather than read these properties directly.
        # Its concrete owned MySQL/Redis fixture seam is reviewed when the writer lands the source.
        profile_text = PROFILE_SOURCE.read_text()
        if 'class EnterpriseQueuedNotificationIntegrationTest' not in profile_text:
            raise RuntimeError('Profile regression source does not define the exact selected class')
        if not re.search(r'@EnabledIfSystemProperty\s*\(\s*named\s*=\s*"profile\.notify\.integration"\s*,\s*matches\s*=\s*"true"', profile_text):
            raise RuntimeError('Profile regression class must retain its explicit opt-in property')
        if 'new NotifyAtomicResultIntegrationTest()' not in profile_text:
            raise RuntimeError('Profile regression must still use the owned Atomic fixture')
        atomic_text = ATOMIC_SOURCE.read_text()
        if 'T36_MYSQL_PASSWORD' not in atomic_text or 'notify.mysql.integration.url' not in atomic_text:
            raise RuntimeError('Profile Atomic fixture must read the owned T36 credential and URL')
        if not re.search(r'@EnabledIfSystemProperty\s*\(\s*named\s*=\s*"notify\.atomic\.integration"\s*,\s*matches\s*=\s*"true"', atomic_text):
            raise RuntimeError('Atomic regression must retain its explicit opt-in property')
        if 'notify.wake.integration' not in WAKE_SOURCE.read_text():
            raise RuntimeError('Wake regression class must retain its explicit opt-in property')
        validate_mode_source(MODE_SOURCE.read_text())
        manual_text = MANUAL_SOURCE.read_text()
        if 'class NotifyManualRetryIntegrationTest' not in manual_text or 'T38_MYSQL_PASSWORD' not in manual_text:
            raise RuntimeError('Manual retry regression must use its owned T38 credential')
        if not re.search(r'@EnabledIfSystemProperty\s*\(\s*named\s*=\s*"notify\.manual\.retry\.integration"\s*,\s*matches\s*=\s*"true"', manual_text):
            raise RuntimeError('Manual retry regression must retain its explicit opt-in property')
        sms_text = SMS_SOURCE.read_text()
        if 'class NotifySmsDispatchIntegrationTest' not in sms_text or 'T35_MYSQL_PASSWORD' not in sms_text:
            raise RuntimeError('SMS regression must use its owned T35 credential')
        if not re.search(r'@EnabledIfSystemProperty\s*\(\s*named\s*=\s*"notify\.sms\.integration"\s*,\s*matches\s*=\s*"true"', sms_text):
            raise RuntimeError('SMS regression must retain its explicit opt-in property')
        idempotency_text = IDEMPOTENCY_SOURCE.read_text()
        if 'class RedisNotifyIdempotencyStoreIntegrationTest' not in idempotency_text or 'notify.redis.integration.port' not in idempotency_text:
            raise RuntimeError('Idempotency regression must use owned Redis port')
        for name in SQL_NAMES:
            sql_path = SQL_ROOT / name
            report['sql_baselines'].append({'file': str(sql_path.relative_to(ROOT)),
                                             'sha256': hash_file(sql_path)})
        write_secret(mysql_env, 'MYSQL_ROOT_PASSWORD=' + root_password + '\nMYSQL_DATABASE=' + database + '\n')
        write_secret(test_env, ''.join(name + '=' + app_password + '\n' for name in
                     ('T50_MYSQL_PASSWORD', 'T39_MYSQL_PASSWORD', 'T36_MYSQL_PASSWORD', 'T38_MYSQL_PASSWORD', 'T35_MYSQL_PASSWORD')))

        docker_touched = True
        mysql_id = full_id(docker(*mysql_run_args(run_id, mysql_env)))
        captured.append(mysql_id)
        volumes.extend(volume_names(mysql_id))
        report['owned']['mysql_container_id'] = mysql_id
        wait_ready(mysql_id, 'mysql', 120)
        mysql_port = mapped_port(mysql_id, '3306')
        ports.append(mysql_port)
        for item in report['sql_baselines']:
            mysql(mysql_id, database, (ROOT / item['file']).read_bytes())
            item['import_exit_code'] = 0
        table_count = int(mysql(mysql_id, database, 'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();'))
        if table_count != 103:
            raise RuntimeError('Owned application schema table count differs from six-SQL contract')
        report['owned']['application_table_count'] = table_count
        mysql(mysql_id, database,
              "CREATE USER '" + app_user + "'@'%' IDENTIFIED BY '" + app_password + "';\n"
              "GRANT ALL PRIVILEGES ON `" + database + "`.* TO '" + app_user + "'@'%';\n")

        redis_id = full_id(docker('run', '--pull=never', '-d', '--name', 't50-redis-' + run_id,
                                  '--label', 'namewta.test.owner=T-50',
                                  '--label', 'namewta.test.run=' + run_id,
                                  '-p', '127.0.0.1::6379', 'redis:8.6.3',
                                  'redis-server', '--save', '', '--appendonly', 'no'))
        captured.append(redis_id)
        volumes.extend(volume_names(redis_id))
        report['owned']['redis_container_id'] = redis_id
        wait_ready(redis_id, 'redis', 45)
        redis_port = mapped_port(redis_id, '6379')
        ports.append(redis_port)
        report['owned']['loopback_ports'] = {'mysql': mysql_port, 'redis': redis_port}
        jdbc = (f'jdbc:mysql://127.0.0.1:{mysql_port}/{database}'
                '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai')
        selectors = ('NotifyDeadlineIntegrationTest,EnterpriseQueuedNotificationIntegrationTest,'
                     'NotifyWakeIntegrationTest,NotifySupportedModeIntegrationTest,'
                     'NotifyAtomicResultIntegrationTest,NotifyManualRetryIntegrationTest,'
                     'NotifySmsDispatchIntegrationTest,RedisNotifyIdempotencyStoreIntegrationTest')
        argv = ('./mvnw', '-Pdev', '-pl', 'wta-admin', '-am', 'test',
                '-Dtest=' + selectors, '-Dsurefire.failIfNoSpecifiedTests=false',
                '-DforkCount=1', '-DreuseForks=false',
                '-Dnotify.deadline.integration=true', '-Dprofile.notify.integration=true',
                '-Dnotify.wake.integration=true', '-Dnotify.supported.mode.integration=true',
                '-Dnotify.atomic.integration=true', '-Dnotify.manual.retry.integration=true',
                '-Dnotify.sms.integration=true',
                '-Dnotify.mysql.integration.url=' + jdbc,
                '-Dnotify.mysql.integration.username=' + app_user,
                '-Dnotify.redis.integration.port=' + str(redis_port))
        if any(secret in ' '.join(argv) for secret in (root_password, app_password)):
            raise RuntimeError('A secret entered Maven argv')
        report['maven_command_redacted'] = list(argv)
        started_ns = time.time_ns()
        with os.fdopen(os.open(raw_log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
            maven_proc = subprocess.Popen(argv, cwd=ROOT / 'backend', stdout=stream,
                                          stderr=subprocess.STDOUT, start_new_session=True,
                                          env=safe_env(T50_MYSQL_PASSWORD=app_password, T39_MYSQL_PASSWORD=app_password,
                                                       T36_MYSQL_PASSWORD=app_password,
                                                       T38_MYSQL_PASSWORD=app_password, T35_MYSQL_PASSWORD=app_password))
            try:
                report['maven_exit_code'] = maven_proc.wait(timeout=3600)
            except subprocess.TimeoutExpired as exc:
                raise RuntimeError('Selected Maven test exceeded one-hour limit') from exc
        for _, path in SUITES:
            if redact_fresh_xml(path, started_ns, (root_password, app_password)):
                errors.append('fresh_surefire_xml_exposed_owned_secret')
        # Retain each attributable class even if its peer report is missing or stale.
        report['counts'] = {}
        report['methods'] = {}
        xml_errors = {}
        for test_class, path in SUITES:
            try:
                report['counts'][test_class] = xml_counts_for(test_class, path, started_ns)
                report['methods'][test_class] = xml_methods_for(path)
            except Exception as exc:
                xml_errors[test_class] = type(exc).__name__
        if xml_errors:
            report['xml_errors'] = xml_errors
            raise RuntimeError('One or more exact fresh Surefire reports are missing or invalid')
        if report['maven_exit_code']:
            raise RuntimeError('Selected Maven test command failed')
        for test_class, counts in report['counts'].items():
            if counts['tests'] <= 0 or any(counts[key] for key in ('failures', 'errors', 'skipped')):
                raise RuntimeError('Selected Surefire class failed or was skipped: ' + test_class)
        report['exit_code'] = 0
    except BaseException as exc:
        report['error'] = (type(exc).__name__ + ': ' + str(exc)).replace(root_password, '[REDACTED]').replace(app_password, '[REDACTED]')
    finally:
        try:
            for _, path in SUITES:
                if redact_fresh_xml(path, started_ns, (root_password, app_password)):
                    errors.append('fresh_surefire_xml_exposed_owned_secret')
        except Exception:
            errors.append('fresh_surefire_xml_redaction_failed')
        if maven_proc is not None:
            try:
                report['cleanup']['maven_process_group'] = {
                    'pgid': maven_proc.pid, 'live_members': stop_group(maven_proc)}
                if report['cleanup']['maven_process_group']['live_members']:
                    errors.append('maven_process_group_remains')
            except Exception:
                errors.append('maven_process_group_cleanup_failed')
        try:
            sanitize_log(raw_log, clean_log, (root_password, app_password))
        except Exception:
            errors.append('log_redaction_or_removal_failed')
        for path in (mysql_env, test_env):
            try:
                path.unlink(missing_ok=True)
            except Exception:
                errors.append('private_env_remove_failed')
        report['owned']['captured_anonymous_volume_names'] = list(volumes)
        remaining, container_errors = cleanup_containers(run_id, captured) if docker_touched else ([], [])
        report['cleanup']['remaining_owned_full_ids'] = remaining
        errors.extend(container_errors)
        try:
            report['cleanup']['anonymous_volumes_absent'] = {name: volume_absent(name) for name in volumes}
            if not all(report['cleanup']['anonymous_volumes_absent'].values()):
                errors.append('owned_anonymous_volume_remains')
        except Exception:
            errors.append('owned_volume_absence_check_failed')
        try:
            report['cleanup']['loopback_ports_closed'] = {str(port): closed(port) for port in ports}
            if not all(report['cleanup']['loopback_ports_closed'].values()):
                errors.append('owned_port_still_open')
        except Exception:
            errors.append('owned_port_check_failed')
        try:
            report['source_after'] = source_identity(args.expected_head)
            if report.get('source_before') != report['source_after']:
                errors.append('source_identity_changed')
        except Exception:
            errors.append('source_after_not_clean_exact_head')
        report['cleanup']['errors'] = errors
        report['exit_code'] = int(bool(report['exit_code'] or errors))
        report['acceptance'] = bool(report['exit_code'] == 0)
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        write_secret(run_dir / 'result.json', json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps({'result': str(run_dir / 'result.json'), 'exit_code': report['exit_code']},
                         ensure_ascii=False))
    return report['exit_code']


if __name__ == '__main__':
    sys.exit(main())
