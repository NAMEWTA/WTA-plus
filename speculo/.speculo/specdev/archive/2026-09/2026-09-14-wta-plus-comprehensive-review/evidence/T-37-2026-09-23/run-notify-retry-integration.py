#!/usr/bin/env python3
"""Opt-in, owned T-37 MySQL/Redis acceptance runner. Never run during preparation."""

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
RUN_ROOT = Path('/tmp/wta-t37/runs')
DOCKER = ('docker', '--host', 'unix:///var/run/docker.sock')
SQL_NAMES = (
    '10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql',
    '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql',
)
SQL_ROOT = ROOT / 'release-artifacts/docker/infrastructure/mysql/init'
TEST_CLASS = 'org.namewta.test.notify.NotifySmsDispatchIntegrationTest'
TEST_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/NotifySmsDispatchIntegrationTest.java'
TEST_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + TEST_CLASS + '.xml')
REDIS_CLASS = 'org.namewta.test.notify.idempotency.RedisNotifyIdempotencyStoreIntegrationTest'
REDIS_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/notify/idempotency/RedisNotifyIdempotencyStoreIntegrationTest.java'
REDIS_REPORT = ROOT / ('backend/wta-admin/target/surefire-reports/TEST-' + REDIS_CLASS + '.xml')


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
    found = docker('ps', '-aq', '--no-trunc', '--filter', 'label=namewta.test.owner=T-37',
                   '--filter', 'label=namewta.test.run=' + run_id)
    return [full_id(item) for item in found.splitlines()] if found else []


def assert_owned(cid, run_id):
    actual = labels(cid)
    if actual.get('namewta.test.owner') != 'T-37' or actual.get('namewta.test.run') != run_id:
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
    return ('run', '-d', '--name', 't37-mysql-' + run_id,
            '--label', 'namewta.test.owner=T-37',
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


def xml_verdict_for(test_class, report_path, started_ns):
    counts = xml_counts_for(test_class, report_path, started_ns)
    if counts['tests'] <= 0 or any(counts[key] for key in ('failures', 'errors', 'skipped')):
        raise RuntimeError('Target Surefire class has zero tests, failures, errors or skips')
    return counts


def xml_verdict(started_ns):
    return xml_verdict_for(TEST_CLASS, TEST_REPORT, started_ns)


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
    if result.returncode == 1 and b'No such volume' in result.stderr:
        return True
    raise RuntimeError('Owned volume absence could not be verified')


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
    app_user = 't37_' + run_id
    root_password, app_password = random_password(), random_password()
    mysql_env = run_dir / 'mysql-container.env'
    test_env = run_dir / 'test-process.env'
    raw_log = run_dir / 'maven.raw.log'
    clean_log = run_dir / 'maven.log'
    report = {'gate': 'T-37 real SMS runtime and Redis idempotency', 'run_id': run_id,
              'mode': 'acceptance',
              'acceptance': False,
              'started_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'exit_code': 1, 'owned': {}, 'sql_baselines': [], 'cleanup': {}, 'counts': None}
    captured = []
    ports = []
    volumes = []
    errors = []
    maven_proc = None
    try:
        report['source_before'] = source_identity(args.expected_head)
        test_text = TEST_SOURCE.read_text()
        if 'T35_MYSQL_PASSWORD' not in test_text:
            raise RuntimeError('Target test must read T35_MYSQL_PASSWORD from process environment')
        if 'notify.redis.integration.port' not in REDIS_SOURCE.read_text():
            raise RuntimeError('Redis class must retain its owned-port assumption')
        for name in SQL_NAMES:
            sql_path = SQL_ROOT / name
            report['sql_baselines'].append({'file': str(sql_path.relative_to(ROOT)),
                                             'sha256': hash_file(sql_path)})
        write_secret(mysql_env, 'MYSQL_ROOT_PASSWORD=' + root_password + '\nMYSQL_DATABASE=' + database + '\n')
        write_secret(test_env, 'T35_MYSQL_PASSWORD=' + app_password + '\n')

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

        redis_id = full_id(docker('run', '-d', '--name', 't37-redis-' + run_id,
                                  '--label', 'namewta.test.owner=T-37',
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
        selectors = 'NotifySmsDispatchIntegrationTest,RedisNotifyIdempotencyStoreIntegrationTest'
        argv = ('./mvnw', '-Pdev', '-pl', 'wta-admin', '-am', 'test',
                '-Dtest=' + selectors, '-Dsurefire.failIfNoSpecifiedTests=false',
                '-DforkCount=1', '-DreuseForks=false',
                '-Dnotify.sms.integration=true', '-Dnotify.mysql.integration.url=' + jdbc,
                '-Dnotify.mysql.integration.username=' + app_user,
                '-Dnotify.redis.integration.port=' + str(redis_port))
        report['maven_command_redacted'] = list(argv)
        started_ns = time.time_ns()
        with os.fdopen(os.open(raw_log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
            maven_proc = subprocess.Popen(argv, cwd=ROOT / 'backend', stdout=stream,
                                          stderr=subprocess.STDOUT, start_new_session=True,
                                          env=safe_env(T35_MYSQL_PASSWORD=app_password))
            try:
                report['maven_exit_code'] = maven_proc.wait(timeout=3600)
            except subprocess.TimeoutExpired as exc:
                raise RuntimeError('Selected Maven test exceeded one-hour limit') from exc
        # Save both fresh exact-class counts even when Maven fails, preserving an attributable red verdict.
        report['counts'] = {
            TEST_CLASS: xml_counts_for(TEST_CLASS, TEST_REPORT, started_ns),
            REDIS_CLASS: xml_counts_for(REDIS_CLASS, REDIS_REPORT, started_ns),
        }
        report['methods'] = {
            TEST_CLASS: xml_methods_for(TEST_REPORT),
            REDIS_CLASS: xml_methods_for(REDIS_REPORT),
        }
        if report['maven_exit_code']:
            raise RuntimeError('Selected Maven test command failed')
        for test_class, counts in report['counts'].items():
            if counts['tests'] <= 0 or any(counts[key] for key in ('failures', 'errors', 'skipped')):
                raise RuntimeError('Selected Surefire class failed or was skipped: ' + test_class)
        report['exit_code'] = 0
    except BaseException as exc:
        report['error'] = (type(exc).__name__ + ': ' + str(exc)).replace(root_password, '[REDACTED]').replace(app_password, '[REDACTED]')
    finally:
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
        remaining, container_errors = cleanup_containers(run_id, captured)
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
