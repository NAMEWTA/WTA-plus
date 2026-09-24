#!/usr/bin/env python3
"""Two exact OSS MinIO integration classes on wholly owned services.

Inert without --execute and an exact clean 40-character source SHA. This
runner never uses an existing WTA database, Redis, MinIO identity or bucket.
"""

import argparse
import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import socket
import string
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


ROOT = Path('/srv/WTA-plus')
RUN_ROOT = Path('/tmp/wta-t45/oss-two-runs')
BASE_PATH = ROOT / 'frontend/e2e/run-notice-retraction-real.py'
BASE_SHA = '7afb43db7d0086aeda0778389ad2fba95252dddf221eec406024101662c3bf46'
OWNER = 'T-45-OSS-TWO'
MINIO_IMAGE = 'pgsty/minio@sha256:83885c27b3b5b673049e33ddf4029afe2c134fd51ce4309e65e4f39d3b9ca282'
REPORT_ROOT = ROOT / 'backend/wta-admin/target/surefire-reports'
SUITES = (
    'org.namewta.test.oss.readiness.OssStorageReadinessMinioIntegrationTest',
    'org.namewta.test.oss.access.OssAccessUrlMinioIntegrationTest',
)
PROPERTY_KEYS = (
    't45.owned.run', 'oss.minio.integration.endpoint',
    'oss.minio.integration.access-key', 'oss.minio.integration.secret-key',
    'oss.migration.mysql.integration.url', 'oss.migration.mysql.integration.username',
    'oss.migration.mysql.integration.password', 'namewta.sql.root',
    'oss.upload.redis.integration.port', 'namewta.repo.root',
)
PHASES = ('preflight', 'mysql', 'redis', 'minio', 'maven', 'reports', 'cleanup')
AMZ_QUERY = re.compile(r'((?:\?|&amp;|&)X-Amz-[^=\s&"\']+=)[^&\s"\']+', re.IGNORECASE)


def load_base():
    if hashlib.sha256(BASE_PATH.read_bytes()).hexdigest() != BASE_SHA:
        raise RuntimeError('tracked owned helper changed')
    spec = importlib.util.spec_from_file_location('t45_oss_two_base', BASE_PATH)
    base = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(base)
    base.OWNER = OWNER
    return base


def private(path, data):
    with os.fdopen(os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
        stream.write(data)


def random_password():
    alphabet = string.ascii_letters + string.digits
    return ''.join(secrets.choice(alphabet) for _ in range(40))


def xml_path(name):
    return REPORT_ROOT / ('TEST-' + name + '.xml')


def sanitize_bytes(raw, generated):
    text = raw.decode('utf-8', errors='replace')
    replacements = 0
    for value in generated:
        if value:
            replacements += text.count(value)
            text = text.replace(value, '[REDACTED]')
    text, amz_count = AMZ_QUERY.subn(r'\1[REDACTED]', text)
    replacements += amz_count
    return text.encode('utf-8'), replacements


def safe_xml_summary(name, raw, expected_run):
    suite = ET.fromstring(raw)
    if suite.tag != 'testsuite' or suite.attrib.get('name') != name:
        raise RuntimeError('Fresh Surefire XML has wrong suite identity')
    counts = {key: int(suite.attrib.get(key, '-1')) for key in
              ('tests', 'failures', 'errors', 'skipped')}
    props = suite.find('properties')
    marker = None if props is None else next(
        (item.attrib.get('value') for item in props.findall('property')
         if item.attrib.get('name') == 't45.owned.run'), None)
    if marker != expected_run:
        raise RuntimeError('Surefire fork did not receive the private property file')
    cases = suite.findall('testcase')
    if counts['tests'] <= 0 or len(cases) != counts['tests']:
        raise RuntimeError('Surefire testcase count differs from suite count')
    methods = []
    for case in cases:
        method = case.attrib.get('name', '')
        if not re.fullmatch(r'[A-Za-z_$][A-Za-z0-9_$]*', method):
            raise RuntimeError('Surefire method name is not safe metadata')
        methods.append(method)
    summary = {'suite': name, 'counts': counts, 'methods': methods,
               'private_properties_marker': True}
    return summary


def exact_suite_gate(suites):
    return (len(suites) == len(SUITES)
            and [item.get('suite') for item in suites] == list(SUITES)
            and all(item.get('fresh') and item.get('private_properties_marker')
                    and item.get('counts', {}).get('tests') == 1
                    and all(item['counts'].get(key) == 0
                            for key in ('failures', 'errors', 'skipped'))
                    for item in suites))


def retain_fresh_reports(started_ns, run_dir, generated, run_id):
    retained = []
    for name in SUITES:
        path = xml_path(name)
        if not path.is_file() or path.stat().st_mtime_ns < started_ns:
            retained.append({'suite': name, 'fresh': False})
            continue
        raw = path.read_bytes()
        source_sha = hashlib.sha256(raw).hexdigest()
        sanitized, replacements = sanitize_bytes(raw, generated)
        output = run_dir / 'xml' / path.name
        try:
            private(output, sanitized)
            # Surefire writes to ignored target; do not leave owned secret values there.
            temp = path.with_name(path.name + '.t45-' + run_id + '.tmp')
            private(temp, sanitized)
            os.replace(temp, path)
        except Exception:
            path.unlink(missing_ok=True)
            raise
        summary = {'suite': name, 'fresh': True, 'source_sha256': source_sha,
                   'sanitized_sha256': hashlib.sha256(sanitized).hexdigest(),
                   'redactions': replacements, 'retained_path': str(output)}
        try:
            summary.update(safe_xml_summary(name, raw, run_id))
        except Exception as exc:
            summary['parse_failure_type'] = type(exc).__name__
        retained.append(summary)
    return retained


def retain_fresh_text_and_temp(started_ns, run_dir, generated):
    """Only inspect this run's Surefire text/temporary files; never touch old reports."""
    retained = []
    for name in SUITES:
        path = REPORT_ROOT / (name + '.txt')
        if not path.is_file() or path.stat().st_mtime_ns < started_ns:
            continue
        raw = path.read_bytes()
        clean, count = sanitize_bytes(raw, generated)
        private(run_dir / 'xml' / path.name, clean)
        temp = path.with_name(path.name + '.t45-owned.tmp')
        private(temp, clean)
        os.replace(temp, path)
        retained.append({'suite': name, 'source_sha256': hashlib.sha256(raw).hexdigest(),
                         'sanitized_sha256': hashlib.sha256(clean).hexdigest(),
                         'redactions': count})
    temp_files = []
    secret_tmp_count = 0
    target = ROOT / 'backend/wta-admin/target'
    if target.is_dir():
        for directory in (target, target / 'surefire', REPORT_ROOT):
            if not directory.is_dir():
                continue
            for path in directory.iterdir():
                if (not path.is_file() or path.is_symlink() or path.stat().st_mtime_ns < started_ns
                    or not (path.name.startswith('surefire') and path.suffix in ('.tmp', '.properties'))):
                    continue
                raw = path.read_bytes()
                if any(value.encode() in raw for value in generated):
                    path.unlink(missing_ok=True)
                    secret_tmp_count += 1
                    continue
                temp_files.append({'name_sha256': hashlib.sha256(path.name.encode()).hexdigest(),
                                   'size': len(raw)})
    if secret_tmp_count:
        raise RuntimeError('Fresh Surefire temporary files retained owned credentials')
    return retained, temp_files


def retain_owned_java_logs(tmp_dir, run_dir, generated):
    retained = []
    if not tmp_dir.is_dir():
        return retained
    for path in sorted(tmp_dir.rglob('*.log')):
        if path.is_symlink() or not path.is_file() or path.stat().st_size > 2_000_000:
            raise RuntimeError('Owned Java temporary log violates bounded file contract')
        if len(retained) >= 20:
            raise RuntimeError('Owned Java temporary log count exceeds bound')
        raw = path.read_bytes()
        clean, count = sanitize_bytes(raw, generated)
        destination = run_dir / ('java-temp-' + str(len(retained) + 1) + '.log')
        private(destination, clean)
        retained.append({'source_sha256': hashlib.sha256(raw).hexdigest(),
                         'sanitized_sha256': hashlib.sha256(clean).hexdigest(),
                         'redactions': count, 'retained_path': str(destination)})
    return retained


def wait_redis(base, cid):
    deadline = time.monotonic() + 45
    while time.monotonic() < deadline:
        completed = base.run((*base.DOCKER, 'exec', cid, 'redis-cli', 'PING'), timeout=12)
        if completed.returncode == 0 and base.output(completed) == 'PONG':
            return
        time.sleep(1)
    raise RuntimeError('Owned no-password Redis did not become ready')


def live_owned_processes(tmp_dir):
    """Track any fork that leaves Maven's process group by this run's tmp marker."""
    marker = str(tmp_dir).encode()
    environment_marker = b'TMPDIR=' + marker + b'\0'
    found = []
    for entry in Path('/proc').iterdir():
        if not entry.name.isdigit():
            continue
        pid = int(entry.name)
        try:
            state = (entry / 'stat').read_text().rsplit(')', 1)[1].split()[0]
            if state in ('Z', 'X'):
                continue
            environ = (entry / 'environ').read_bytes()
            command = (entry / 'cmdline').read_bytes()
        except (FileNotFoundError, ProcessLookupError, PermissionError):
            continue
        if environment_marker in environ or marker in command:
            found.append(pid)
    return sorted(found)


def stop_owned_processes(tmp_dir):
    """Signal only processes carrying this run's exact private tmp marker."""
    for signum, seconds in ((signal.SIGTERM, 10), (signal.SIGKILL, 5)):
        found = live_owned_processes(tmp_dir)
        if not found:
            return []
        for pid in found:
            try:
                os.kill(pid, signum)
            except ProcessLookupError:
                pass
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            found = live_owned_processes(tmp_dir)
            if not found:
                return []
            time.sleep(.1)
    return live_owned_processes(tmp_dir)


def mysql(base, cid, database, sql):
    result = base.run((*base.DOCKER, 'exec', '-i', cid, 'sh', '-c',
                       'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot '
                       '--default-character-set=utf8mb4 --batch --skip-column-names --database="$1"',
                       'sh', database), data=sql.encode(), timeout=90)
    if result.returncode:
        raise RuntimeError('Owned migration database initialization failed')
    return base.output(result)


def public_failure(exc, phase):
    return {'phase': phase if phase in PHASES else 'unknown',
            'type': type(exc).__name__}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--execute', action='store_true')
    parser.add_argument('--expected-head')
    args = parser.parse_args()
    if not args.execute or not args.expected_head or not re.fullmatch(r'[0-9a-f]{40}', args.expected_head):
        parser.error('requires --execute --expected-head <exact clean 40-hex SHA>')
    os.umask(0o077)
    run_id = secrets.token_hex(8)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(parents=True, mode=0o700)
    (run_dir / 'xml').mkdir(mode=0o700)
    base = None
    containers, volumes, ports = [], [], []
    secret_paths = []
    generated = []
    maven = None
    started_ns = None
    docker_touched = False
    phase = 'preflight'
    cleanup_errors = []
    report = {'gate': 'T45 two exact OSS MinIO integration classes', 'run_id': run_id,
              'owner': OWNER, 'source_before': None, 'source_after': None,
              'maven_exit_code': None,
              'suites': [], 'cleanup': {}, 'acceptance': False, 'exit_code': 1,
              'started_utc': dt.datetime.now(dt.timezone.utc).isoformat()}
    try:
        base = load_base()
        report['source_before'] = base.source_identity(args.expected_head)
        for name in SUITES:
            source = ROOT / 'backend/wta-admin/src/test/java' / Path(name.replace('.', '/') + '.java')
            if not source.is_file() or ('class ' + name.rsplit('.', 1)[1]) not in source.read_text():
                raise RuntimeError('An exact selected integration class is missing')
        descriptor = Path('/root/.m2/repository/org/apache/maven/plugins/maven-surefire-plugin/3.5.5/maven-surefire-plugin-3.5.5.jar')
        if not descriptor.is_file():
            raise RuntimeError('Required cached Surefire 3.5.5 is absent')
        if not (ROOT / 'backend/mvnw').is_file():
            raise RuntimeError('Required Maven wrapper is absent')
        root_password, migration_password, minio_password = (random_password() for _ in range(3))
        minio_user = 't45oss' + run_id
        migration_user = 't45_' + run_id
        migration_db = 't45_oss_migration_' + run_id
        generated = [root_password, migration_password, minio_password, minio_user]
        mysql_env = run_dir / 'mysql.env'
        minio_env = run_dir / 'minio.env'
        properties = run_dir / 'surefire.properties'
        tmp_dir = run_dir / 'tmp'
        tmp_dir.mkdir(mode=0o700)
        secret_paths = [mysql_env, minio_env, properties]
        private(mysql_env, ('MYSQL_ROOT_PASSWORD=' + root_password + '\nMYSQL_DATABASE='
                            + migration_db + '\n').encode())
        private(minio_env, ('MINIO_ROOT_USER=' + minio_user + '\nMINIO_ROOT_PASSWORD='
                            + minio_password + '\n').encode())

        phase = 'mysql'
        docker_touched = True
        mysql_id = base.full_id(base.docker('run', '--pull=never', '-d', '--name',
                             't45-two-mysql-' + run_id,
                             '--label', 'namewta.test.owner=' + OWNER,
                             '--label', 'namewta.test.run=' + run_id,
                             '-p', '127.0.0.1::3306', '--env-file', str(mysql_env),
                             'mysql:8.4.9', '--character-set-server=utf8mb4',
                             '--collation-server=utf8mb4_general_ci',
                             '--log-bin-trust-function-creators=1'))
        containers.append(mysql_id)
        base.assert_owned(mysql_id, run_id)
        volumes.extend(base.volume_names(mysql_id))
        base.wait_mysql(mysql_id)
        mysql_port = base.mapped_port(mysql_id, '3306')
        ports.append(mysql_port)
        if mysql(base, mysql_id, migration_db,
                 'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();') != '0':
            raise RuntimeError('Migration database is not empty before the selected test')
        mysql(base, mysql_id, migration_db,
              "CREATE USER '" + migration_user + "'@'%' IDENTIFIED BY '" + migration_password + "';\n"
              "GRANT ALL PRIVILEGES ON `" + migration_db + "`.* TO '" + migration_user + "'@'%';\n")

        phase = 'redis'
        redis_id = base.full_id(base.docker('run', '--pull=never', '-d', '--name',
                             't45-two-redis-' + run_id,
                             '--label', 'namewta.test.owner=' + OWNER,
                             '--label', 'namewta.test.run=' + run_id,
                             '-p', '127.0.0.1::6379', 'redis:8.6.3', 'redis-server',
                             '--bind', '0.0.0.0', '--protected-mode', 'no',
                             '--save', '', '--appendonly', 'no'))
        containers.append(redis_id)
        base.assert_owned(redis_id, run_id)
        volumes.extend(base.volume_names(redis_id))
        wait_redis(base, redis_id)
        redis_port = base.mapped_port(redis_id, '6379')
        ports.append(redis_port)

        phase = 'minio'
        minio_id = base.full_id(base.docker('run', '--pull=never', '-d', '--name',
                             't45-two-minio-' + run_id,
                             '--label', 'namewta.test.owner=' + OWNER,
                             '--label', 'namewta.test.run=' + run_id,
                             '-p', '127.0.0.1::9000', '--env-file', str(minio_env),
                             MINIO_IMAGE, 'server', '--address', ':9000', '/data'))
        containers.append(minio_id)
        base.assert_owned(minio_id, run_id)
        volumes.extend(base.volume_names(minio_id))
        minio_port = base.mapped_port(minio_id, '9000')
        ports.append(minio_port)
        base.wait_minio(minio_port)
        report['owned'] = {'container_full_ids': containers, 'loopback_ports':
                           {'mysql': mysql_port, 'redis': redis_port, 'minio': minio_port},
                           'migration_database_empty_before': True,
                           'migration_database_name': migration_db,
                           'minio_identity_scope': 'owned bootstrap root'}

        settings = {
            't45.owned.run': run_id,
            'oss.minio.integration.endpoint': 'http://127.0.0.1:' + str(minio_port),
            'oss.minio.integration.access-key': minio_user,
            'oss.minio.integration.secret-key': minio_password,
            'oss.migration.mysql.integration.url':
                'jdbc:mysql://127.0.0.1:' + str(mysql_port) + '/' + migration_db
                + '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC',
            'oss.migration.mysql.integration.username': migration_user,
            'oss.migration.mysql.integration.password': migration_password,
            'namewta.sql.root': str(ROOT / 'release-artifacts/docker/infrastructure/mysql/init'),
            'oss.upload.redis.integration.port': str(redis_port),
            'namewta.repo.root': str(ROOT),
        }
        if tuple(settings) != PROPERTY_KEYS or any('\n' in value or '\r' in value for value in settings.values()):
            raise RuntimeError('Private Surefire property schema differs')
        private(properties, ''.join(key + '=' + value + '\n' for key, value in settings.items()).encode())

        phase = 'maven'
        selector = ','.join(name.rsplit('.', 1)[1] for name in SUITES)
        argv = ('./mvnw', '-B', '-ntp', '-o', '-Pdev', '-pl', 'wta-admin', '-am', '-Dtest=' + selector,
                '-Dsurefire.failIfNoSpecifiedTests=false', '-DforkCount=1',
                '-DreuseForks=false', '-Dsurefire.systemPropertiesFile=' + str(properties), 'test')
        if any(value in ' '.join(argv) for value in generated):
            raise RuntimeError('Generated credential appeared in Maven argv')
        raw_log = run_dir / 'maven.raw.log'
        started_ns = time.time_ns()
        with os.fdopen(os.open(raw_log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as output:
            maven_env = base.safe_env(CI='1',
                                      TMPDIR=str(tmp_dir),
                                      JAVA_TOOL_OPTIONS='-Xms128m -Xmx1536m -Djava.io.tmpdir=' + str(tmp_dir))
            if any(value in ' '.join(maven_env.values()) for value in generated):
                raise RuntimeError('Generated credential appeared in Maven environment')
            maven = subprocess.Popen(argv, cwd=ROOT / 'backend', env=maven_env,
                                     stdout=output, stderr=subprocess.STDOUT,
                                     start_new_session=True, umask=0o077)
            report['owned']['maven_pgid'] = maven.pid
            try:
                maven.wait(timeout=2400)
            except subprocess.TimeoutExpired:
                report['maven_timeout'] = True
        report['maven_exit_code'] = maven.returncode
        phase = 'reports'
    except BaseException as exc:
        report['failure'] = public_failure(exc, phase)
    finally:
        phase = 'cleanup'
        if maven is not None:
            try:
                live = base.stop_group(maven)
                report['cleanup']['maven_process_group'] = {'pgid': maven.pid, 'live_members': live}
                if live:
                    cleanup_errors.append('maven_process_group_remains')
            except Exception:
                cleanup_errors.append('maven_process_group_cleanup_failed')
        if started_ns is not None:
            try:
                live = stop_owned_processes(run_dir / 'tmp')
                report['cleanup']['owned_node_chrome_java_pids_remaining'] = live
                if live:
                    cleanup_errors.append('owned_process_outside_maven_group_remains')
            except Exception:
                cleanup_errors.append('owned_process_outside_maven_group_check_failed')
        if started_ns is not None:
            try:
                report['suites'] = retain_fresh_reports(started_ns, run_dir, generated, run_id)
            except Exception:
                cleanup_errors.append('fresh_xml_retention_failed')
            try:
                text_reports, temp_files = retain_fresh_text_and_temp(started_ns, run_dir, generated)
                report['surefire_text_reports'] = text_reports
                report['surefire_temp_files'] = temp_files
            except Exception:
                cleanup_errors.append('fresh_surefire_text_or_temp_check_failed')
            try:
                report['owned_java_logs'] = retain_owned_java_logs(run_dir / 'tmp', run_dir, generated)
            except Exception:
                cleanup_errors.append('owned_java_log_retention_failed')
        raw_log = run_dir / 'maven.raw.log'
        if raw_log.is_file():
            try:
                raw = raw_log.read_bytes()
                clean, count = sanitize_bytes(raw, generated)
                private(run_dir / 'maven.log', clean)
                report['maven_log'] = {'source_sha256': hashlib.sha256(raw).hexdigest(),
                                       'sanitized_sha256': hashlib.sha256(clean).hexdigest(),
                                       'redactions': count}
            except Exception:
                cleanup_errors.append('maven_log_sanitization_failed')
            finally:
                try:
                    raw_log.unlink(missing_ok=True)
                except Exception:
                    cleanup_errors.append('maven_raw_log_cleanup_failed')
        for path in secret_paths:
            try:
                path.unlink(missing_ok=True)
            except Exception:
                cleanup_errors.append('private_input_cleanup_failed')
        try:
            shutil.rmtree(run_dir / 'tmp')
        except FileNotFoundError:
            pass
        except Exception:
            cleanup_errors.append('owned_java_tmpdir_cleanup_failed')
        if docker_touched and base is not None:
            remaining, errors = base.cleanup_containers(run_id, containers)
            report['cleanup']['remaining_owned_full_ids'] = remaining
            cleanup_errors.extend(errors)
            try:
                absent = {name: base.volume_absent(name) for name in dict.fromkeys(volumes)}
                report['cleanup']['anonymous_volumes_absent'] = absent
                if not all(absent.values()):
                    cleanup_errors.append('owned_anonymous_volume_remains')
            except Exception:
                cleanup_errors.append('owned_volume_absence_check_failed')
            try:
                closed = {str(port): base.closed(port) for port in ports}
                report['cleanup']['loopback_ports_closed'] = closed
                if not all(closed.values()):
                    cleanup_errors.append('owned_loopback_port_remains')
            except Exception:
                cleanup_errors.append('owned_port_absence_check_failed')
        if base is not None:
            try:
                report['source_after'] = base.source_identity(args.expected_head)
                if report['source_after'] != report['source_before']:
                    cleanup_errors.append('source_changed_during_gate')
            except Exception:
                cleanup_errors.append('source_after_check_failed')
        suites = report.get('suites', [])
        suite_ok = exact_suite_gate(suites)
        report['cleanup']['errors'] = cleanup_errors
        report['acceptance'] = (report.get('maven_exit_code') == 0 and suite_ok
                                and not cleanup_errors and 'failure' not in report)
        report['exit_code'] = 0 if report['acceptance'] else 1
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        private(run_dir / 'result.json', (json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode())
        print(str(run_dir / 'result.json'))
    return report['exit_code']


if __name__ == '__main__':
    sys.exit(main())
