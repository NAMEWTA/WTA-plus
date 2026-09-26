#!/usr/bin/env python3
"""Opt-in, owned T-02 HTTP/MySQL log acceptance runner. Never run during preparation."""

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
RUN_ROOT = Path('/tmp/wta-t02/runs')
DOCKER = ('docker', '--host', 'unix:///var/run/docker.sock')
SQL_ROOT = ROOT / 'release-artifacts/docker/infrastructure/mysql/init'
TESTS = {
    'org.namewta.common.json.utils.LogSanitizerTest': 'wta-common/wta-common-json',
    'org.namewta.common.log.aspect.LogAspectRedactionTest': 'wta-common/wta-common-log',
    'org.namewta.common.web.logging.SysLogFilterTest': 'wta-common/wta-common-web',
    'org.namewta.common.web.logging.SysLogFailureRedactionTest': 'wta-common/wta-common-web',
    'org.namewta.common.web.logging.SysLogEventWriterTest': 'wta-common/wta-common-web',
    'org.namewta.test.logging.OperationLogRedactionTest': 'wta-admin',
    'org.namewta.test.logging.LogRedactionHttpMySqlIntegrationTest': 'wta-admin',
}
HTTP_TEST_SOURCE = ROOT / 'backend/wta-admin/src/test/java/org/namewta/test/logging/LogRedactionHttpMySqlIntegrationTest.java'
HTTP_TEST = 'org.namewta.test.logging.LogRedactionHttpMySqlIntegrationTest'
CANARY_METHOD = 'onlineDeviceTokenFromRealIssuerNeverReachesHttpOrErrorAudit'


def report_path(test_class):
    return ROOT / 'backend' / TESTS[test_class] / 'target/surefire-reports' / ('TEST-' + test_class + '.xml')


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
    found = docker('ps', '-aq', '--no-trunc', '--filter', 'label=namewta.test.owner=T-02',
                   '--filter', 'label=namewta.test.run=' + run_id)
    return [full_id(item) for item in found.splitlines()] if found else []


def assert_owned(cid, run_id):
    actual = labels(cid)
    if actual.get('namewta.test.owner') != 'T-02' or actual.get('namewta.test.run') != run_id:
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
    return ('run', '-d', '--name', 't02-mysql-' + run_id,
            '--label', 'namewta.test.owner=T-02',
            '--label', 'namewta.test.run=' + run_id,
            '-p', '127.0.0.1::3306', '--env-file', str(mysql_env),
            'mysql:8.4.9', '--character-set-server=utf8mb4',
            '--collation-server=utf8mb4_general_ci')


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


def xml_issues_for(test_class, report_path, started_ns):
    # Expose only test method and exception type. XML messages/stack traces may contain credentials.
    xml_counts_for(test_class, report_path, started_ns)
    suite = ET.parse(report_path).getroot()
    issues = []
    for case in suite.findall('testcase'):
        for kind in ('failure', 'error', 'skipped'):
            for node in case.findall(kind):
                error_type = node.attrib.get('type', '')
                if not re.fullmatch(r'[A-Za-z_$][A-Za-z0-9_.$]{0,199}', error_type):
                    error_type = '[UNSAFE_OR_MISSING_TYPE]'
                issues.append({'method': case.attrib.get('name', ''), 'kind': kind,
                               'error_type': error_type})
    return issues


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
            content = re.sub(r'credential-canary-[A-Za-z0-9-]+', '[CANARY_REDACTED]', content)
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


def ticket_acceptance(exit_code, required_canary_method):
    return exit_code == 0 and required_canary_method == CANARY_METHOD


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--execute', action='store_true', help='Explicit launch; not used during preparation')
    parser.add_argument('--expected-head', help='Exact full clean implementation commit SHA')
    parser.add_argument('--required-canary-method', help='Exact new HTTP/MySQL token-path test method for ticket acceptance')
    args = parser.parse_args()
    if not args.execute or not args.expected_head or not re.fullmatch(r'[0-9a-f]{40}', args.expected_head):
        parser.error('no launch without --execute and --expected-head <40-hex clean SHA>')
    if args.required_canary_method and args.required_canary_method != CANARY_METHOD:
        parser.error('required canary method must match the fixed token-path test')

    os.umask(0o077)
    run_id = secrets.token_hex(8)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(parents=True, mode=0o700)
    database = 'namewta_log_test_' + run_id
    app_user = 't02_' + run_id
    root_password, app_password = random_password(), random_password()
    mysql_env = run_dir / 'mysql-container.env'
    raw_log = run_dir / 'maven.raw.log'
    clean_log = run_dir / 'maven.log'
    result = {'gate': 'T-02 real Servlet HTTP and owned MySQL logging', 'run_id': run_id,
              'mode': 'ticket-canary' if args.required_canary_method else 'baseline-only',
              'required_canary_method': args.required_canary_method, 'acceptance': False, 'started_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'exit_code': 1, 'owned': {}, 'cleanup': {}, 'counts': None}
    captured = []
    volumes = []
    ports = []
    cleanup_errors = []
    maven_proc = None
    try:
        result['source_before'] = source_identity(args.expected_head)
        test_source = HTTP_TEST_SOURCE.read_text()
        for required in ('log.mysql.integration.url', 'log.mysql.integration.username', 'namewta_log_test_'):
            if required not in test_source:
                raise RuntimeError('HTTP/MySQL test fixture property or owned-database guard changed')
        if 'T02_MYSQL_PASSWORD' not in test_source or 'log.mysql.integration.password' in test_source:
            raise RuntimeError('HTTP/MySQL test fixture must read only T02_MYSQL_PASSWORD')
        if args.required_canary_method and ('void ' + CANARY_METHOD + '(') not in test_source:
            raise RuntimeError('Exact token-path test method is absent before launching services')
        ddl_path = SQL_ROOT / '10-cde-base-ddl.sql'
        result['ddl_baseline'] = {'path': str(ddl_path.relative_to(ROOT)), 'sha256': hash_file(ddl_path)}
        write_secret(mysql_env, 'MYSQL_ROOT_PASSWORD=' + root_password + '\nMYSQL_DATABASE=' + database + '\n')

        cid = full_id(docker(*mysql_run_args(run_id, mysql_env)))
        captured.append(cid)
        volumes.extend(volume_names(cid))
        result['owned']['mysql_container_id'] = cid
        wait_ready(cid, 'mysql', 120)
        mysql_port = mapped_port(cid, '3306')
        ports.append(mysql_port)
        result['owned']['loopback_mysql_port'] = mysql_port
        mysql(cid, database,
              "CREATE USER '" + app_user + "'@'%' IDENTIFIED BY '" + app_password + "';\n"
              "GRANT CREATE, DROP, INDEX, SELECT, INSERT ON `" + database + "`.* TO '" + app_user + "'@'%';\n")
        jdbc = (f'jdbc:mysql://127.0.0.1:{mysql_port}/{database}'
                '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai')
        selectors = ','.join(test_class.rsplit('.', 1)[-1] for test_class in TESTS)
        argv = ('./mvnw', '-B', '-ntp', '-Pdev', '-pl', 'wta-admin', '-am', 'test',
                '-Dtest=' + selectors, '-Dsurefire.failIfNoSpecifiedTests=false',
                '-DforkCount=1', '-DreuseForks=false',
                '-Dlog.mysql.integration.url=' + jdbc,
                '-Dlog.mysql.integration.username=' + app_user,
                '-Dnamewta.sql.root=' + str(SQL_ROOT))
        result['maven_command_redacted'] = list(argv)
        started_ns = time.time_ns()
        with os.fdopen(os.open(raw_log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
            maven_proc = subprocess.Popen(argv, cwd=ROOT / 'backend', stdout=stream,
                                          stderr=subprocess.STDOUT, start_new_session=True,
                                          env=safe_env(T02_MYSQL_PASSWORD=app_password))
            try:
                result['maven_exit_code'] = maven_proc.wait(timeout=3600)
            except subprocess.TimeoutExpired as exc:
                raise RuntimeError('Selected Maven tests exceeded one-hour limit') from exc
        # Preserve attributable counts even for failing Maven commands. No reactor no-match can pass.
        result['counts'] = {test_class: xml_counts_for(test_class, report_path(test_class), started_ns)
                            for test_class in TESTS}
        result['methods'] = {test_class: xml_methods_for(report_path(test_class)) for test_class in TESTS}
        result['issues'] = {test_class: xml_issues_for(test_class, report_path(test_class), started_ns)
                            for test_class in TESTS}
        if result['maven_exit_code']:
            raise RuntimeError('Selected Maven test command failed')
        for test_class, counts in result['counts'].items():
            if counts['tests'] <= 0 or any(counts[key] for key in ('failures', 'errors', 'skipped')):
                raise RuntimeError('Selected Surefire class failed or was skipped: ' + test_class)
        if args.required_canary_method and args.required_canary_method not in result['methods'][HTTP_TEST]:
            raise RuntimeError('Exact new token-path canary method did not execute')
        result['exit_code'] = 0
    except BaseException as exc:
        result['error'] = ((type(exc).__name__ + ': ' + str(exc))
                           .replace(root_password, '[REDACTED]').replace(app_password, '[REDACTED]'))
    finally:
        if maven_proc is not None:
            try:
                result['cleanup']['maven_process_group'] = {'pgid': maven_proc.pid,
                                                             'live_members': stop_group(maven_proc)}
                if result['cleanup']['maven_process_group']['live_members']:
                    cleanup_errors.append('maven_process_group_remains')
            except Exception:
                cleanup_errors.append('maven_process_group_cleanup_failed')
        try:
            sanitize_log(raw_log, clean_log, (root_password, app_password))
        except Exception:
            cleanup_errors.append('log_redaction_or_removal_failed')
        for path in (mysql_env,):
            try:
                path.unlink(missing_ok=True)
            except Exception:
                cleanup_errors.append('private_env_remove_failed')
        result['owned']['captured_anonymous_volume_names'] = list(volumes)
        remaining, container_errors = cleanup_containers(run_id, captured)
        result['cleanup']['remaining_owned_full_ids'] = remaining
        cleanup_errors.extend(container_errors)
        try:
            result['cleanup']['anonymous_volumes_absent'] = {name: volume_absent(name) for name in volumes}
            if not all(result['cleanup']['anonymous_volumes_absent'].values()):
                cleanup_errors.append('owned_anonymous_volume_remains')
        except Exception:
            cleanup_errors.append('owned_volume_absence_check_failed')
        try:
            result['cleanup']['loopback_ports_closed'] = {str(port): closed(port) for port in ports}
            if not all(result['cleanup']['loopback_ports_closed'].values()):
                cleanup_errors.append('owned_port_still_open')
        except Exception:
            cleanup_errors.append('owned_port_check_failed')
        try:
            result['source_after'] = source_identity(args.expected_head)
            if result.get('source_before') != result['source_after']:
                cleanup_errors.append('source_identity_changed')
        except Exception:
            cleanup_errors.append('source_after_not_clean_exact_head')
        result['cleanup']['errors'] = cleanup_errors
        result['exit_code'] = int(bool(result['exit_code'] or cleanup_errors))
        result['acceptance'] = ticket_acceptance(result['exit_code'], args.required_canary_method)
        result['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        write_secret(run_dir / 'result.json', json.dumps(result, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps({'result': str(run_dir / 'result.json'), 'exit_code': result['exit_code']},
                         ensure_ascii=False))
    return result['exit_code']


if __name__ == '__main__':
    sys.exit(main())
