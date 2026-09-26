#!/usr/bin/env python3
"""T42 v5 full-Spring mail test with fail-closed held-PUT retries.

Inert without --execute and an exact clean 40-character source SHA. This
runner never uses an existing WTA database, Redis, MinIO identity or bucket.
The opt-in test source and exact method list must exist before execution.
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
import threading
import time
import xml.etree.ElementTree as ET


ROOT = Path('/srv/WTA-plus')
RUN_ROOT = Path('/tmp/wta-t42/runs')
BASE_PATH = ROOT / 'frontend/e2e/run-notice-retraction-real.py'
BASE_SHA = '7afb43db7d0086aeda0778389ad2fba95252dddf221eec406024101662c3bf46'
PROXY_PATH = Path('/tmp/wta-t42/safe_s3_count_proxy_v5.py')
PROXY_SHA = '0b465c2a3a518ad503daeeba787df14a9cc4be06f48d42de517ff9fc14020fca'
MC = Path('/tmp/wta-t44/mc')
MC_SHA = '01f866e9c5f9b87c2b09116fa5d7c06695b106242d829a8bb32990c00312e891'
OWNER = 'T-42-MAIL-ATTACHMENT'
MINIO_IMAGE = 'pgsty/minio@sha256:83885c27b3b5b673049e33ddf4029afe2c134fd51ce4309e65e4f39d3b9ca282'
REPORT_ROOT = ROOT / 'backend/wta-admin/target/surefire-reports'
SUITES = (
    'org.namewta.test.notify.NotifyMailAttachmentIntegrationTest',
)
PROPERTY_KEYS = (
    't42.owned.run', 'notify.mail.attachment.integration',
    't42.mysql.url', 't42.mysql.username', 't42.mysql.password',
    't42.redis.host', 't42.redis.port', 't42.redis.password',
    't42.minio.endpoint', 't42.minio.access-key', 't42.minio.secret-key',
    't42.minio.source-bucket', 't42.minio.snapshot-bucket',
    't42.s3.count-url',
)
PHASES = ('preflight', 'mysql', 'redis', 'minio', 'schema', 'maven', 'reports', 'cleanup')
AMZ_QUERY = re.compile(r'((?:\?|&amp;|&)X-Amz-[^=\s&"\']+=)[^&\s"\']+', re.IGNORECASE)


def load_base():
    if hashlib.sha256(BASE_PATH.read_bytes()).hexdigest() != BASE_SHA:
        raise RuntimeError('tracked owned helper changed')
    spec = importlib.util.spec_from_file_location('t42_attachment_base', BASE_PATH)
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
         if item.attrib.get('name') == 't42.owned.run'), None)
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


def parse_expected_methods(value):
    methods = tuple(part.strip() for part in value.split(','))
    if (not methods or len(methods) != len(set(methods))
            or any(not re.fullmatch(r'[A-Za-z_$][A-Za-z0-9_$]*', method) for method in methods)):
        raise ValueError('Expected methods must be nonempty, unique Java method identifiers')
    return methods


def exact_suite_gate(suites, expected_methods):
    return (len(suites) == len(SUITES)
            and [item.get('suite') for item in suites] == list(SUITES)
            and all(item.get('fresh') and item.get('private_properties_marker')
                    and item.get('counts', {}).get('tests') == len(expected_methods)
                    and len(item.get('methods', [])) == len(expected_methods)
                    and set(item.get('methods', [])) == set(expected_methods)
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
            temp = path.with_name(path.name + '.t42-' + run_id + '.tmp')
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
        temp = path.with_name(path.name + '.t42-owned.tmp')
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
        raise RuntimeError('Owned attachment SQL step failed')
    return base.output(result)


def public_failure(exc, phase):
    return {'phase': phase if phase in PHASES else 'unknown',
            'type': type(exc).__name__}


def load_proxy():
    if hashlib.sha256(PROXY_PATH.read_bytes()).hexdigest() != PROXY_SHA:
        raise RuntimeError('Private safe S3 count proxy changed')
    spec = importlib.util.spec_from_file_location('t42_safe_proxy', PROXY_PATH)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def mc(base, directory, *argv, json_result=False):
    # Credentials are imported from a private JSON file; never interpolate
    # either key in mc's argv, stdout or public error text.
    if any('password' in str(part).lower() or 'secret' in str(part).lower()
           for part in argv):
        raise RuntimeError('MinIO credential-like argv is forbidden')
    result = base.run((str(MC), '--config-dir', str(directory), *argv),
                      env=base.safe_env(MC_CONFIG_DIR=str(directory)), timeout=45)
    if result.returncode:
        raise RuntimeError('Owned MinIO bootstrap operation failed')
    if json_result:
        result = json.loads(result.stdout)
        if type(result) is not dict:
            raise RuntimeError('Owned MinIO bootstrap JSON has wrong shape')
        return result
    return None


def import_alias(base, run_dir, directory, name, endpoint, access, secret):
    directory.mkdir(mode=0o700)
    path = run_dir / ('alias-' + name + '.json')
    private(path, json.dumps({'url': endpoint, 'accessKey': access,
                              'secretKey': secret, 'api': 's3v4', 'path': 'auto'}).encode())
    mc(base, directory, 'alias', 'import', name, str(path))


def one(base, cid, sql):
    rows = mysql(base, cid, 'wta-plus', sql).splitlines()
    if len(rows) != 1 or not re.fullmatch(r'[0-9]+', rows[0]):
        raise RuntimeError('Owned SQL count has wrong shape')
    return int(rows[0])


def expect_row(base, cid, sql):
    if one(base, cid, sql + ' SELECT ROW_COUNT();') != 1:
        raise RuntimeError('Owned OSS row update count differs')


def configure_oss(base, cid, endpoint, access, secret, source_bucket, snapshot_bucket):
    h = base.hexsql
    total = one(base, cid, 'SELECT COUNT(*) FROM sys_oss_config;')
    if total < 2:
        raise RuntimeError('Owned OSS seed lacks required configurations')
    mysql(base, cid, 'wta-plus',
          'UPDATE sys_oss_config SET access_key=' + h(access)
          + ',secret_key=' + h(secret)
          + ',endpoint=' + h('127.0.0.1:1') + ",domain_url='';")
    for key, bucket, enabled in (('minio', source_bucket, 'Y'),
                                 ('image', snapshot_bucket, 'N')):
        if key not in ('minio', 'image'):
            raise RuntimeError('Unexpected owned OSS config selector')
        sql = ('UPDATE sys_oss_config SET access_key=' + h(access)
               + ',secret_key=' + h(secret) + ',bucket_name=' + h(bucket)
               + ',endpoint=' + h(endpoint) + ",domain_url='',is_https='N',"
               + "access_policy='0',status=" + h(enabled)
               + " WHERE config_key='" + key + "';")
        expect_row(base, cid, sql)
    if one(base, cid,
           "SELECT COUNT(*) FROM sys_oss_config WHERE endpoint NOT LIKE '127.0.0.1:%' "
           "OR domain_url IS NOT NULL AND domain_url<>'';"):
        raise RuntimeError('Owned OSS seed retained an external endpoint')
    return total


def properties_bytes(settings):
    lines = []
    for key, value in settings.items():
        if not re.fullmatch(r'[A-Za-z0-9._-]+', key):
            raise RuntimeError('Invalid private property key')
        if type(value) is bool:
            value = str(value).lower()
        elif type(value) is list and all(type(item) is str for item in value):
            value = ','.join(value)
        value = str(value)
        if any(mark in value for mark in ('\n', '\r', '\\')):
            raise RuntimeError('Invalid private property value shape')
        lines.append(key + '=' + value + '\n')
    return ''.join(lines).encode()


def test_source_guard(expected_methods):
    name = SUITES[0]
    path = ROOT / 'backend/wta-admin/src/test/java' / Path(name.replace('.', '/') + '.java')
    if not path.is_file():
        raise RuntimeError('T42 full-context integration class is missing')
    source = path.read_text()
    if (name.rsplit('.', 1)[1] not in source
        or 'notify.mail.attachment.integration' not in source
        or '@SpringBootTest' not in source
        or 'NamewtaApplication' not in source
        or 'MailNotificationSender' not in source
        or 't42.owned.run' not in source
        or 't42.s3.count-url' not in source):
        raise RuntimeError('T42 full-context integration source contract is incomplete')
    methods = set(re.findall(r'\bvoid\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\(', source))
    if not set(expected_methods).issubset(methods):
        raise RuntimeError('T42 expected integration method is absent from source')
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--execute', action='store_true')
    parser.add_argument('--expected-head')
    parser.add_argument('--expected-methods', help='Exact frozen JUnit method names, comma-separated')
    args = parser.parse_args()
    if (not args.execute or not args.expected_head
            or not re.fullmatch(r'[0-9a-f]{40}', args.expected_head)
            or not args.expected_methods):
        parser.error('requires --execute --expected-head <clean SHA> --expected-methods <frozen names>')
    try:
        expected_methods = parse_expected_methods(args.expected_methods)
    except ValueError as exc:
        parser.error(str(exc))
    os.umask(0o077)
    run_id = secrets.token_hex(8)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(parents=True, mode=0o700)
    (run_dir / 'xml').mkdir(mode=0o700)
    for name in ('tmp', 'logs', 'multipart'):
        (run_dir / name).mkdir(mode=0o700)
    base = proxy_module = None
    containers, volumes, ports, secret_paths, generated = [], [], [], [], []
    maven = proxy = proxy_thread = count_reader = count_reader_thread = None
    started_ns = None
    docker_touched = False
    cleanup_errors = []
    phase = 'preflight'
    report = {'gate': 'T42 full-Spring mail attachment integration',
              'run_id': run_id, 'owner': OWNER, 'expected_methods': expected_methods,
              'source_before': None, 'source_after': None, 'maven_exit_code': None,
              'suites': [], 'owned': {'container_full_ids': containers,
                                     'anonymous_volumes': volumes,
                                     'loopback_ports': {}},
              'cleanup': {}, 'acceptance': False, 'exit_code': 1,
              'started_utc': dt.datetime.now(dt.timezone.utc).isoformat()}
    try:
        base = load_base()
        proxy_module = load_proxy()
        report['source_before'] = base.source_identity(args.expected_head)
        report['test_source_sha256'] = test_source_guard(expected_methods)
        if hashlib.sha256(MC.read_bytes()).hexdigest() != MC_SHA:
            raise RuntimeError('Private MinIO client changed')
        init_script = ROOT / 'release-artifacts/scripts/init-mysql-container.sh'
        sql_root = ROOT / 'release-artifacts/docker/infrastructure/mysql/init'
        for name in base.SQL_NAMES:
            if not (sql_root / name).is_file():
                raise RuntimeError('An ordered six-SQL file is absent')
        initializer = init_script.read_text()
        if not re.search(r'EXPECTED_TABLES=104\b', initializer):
            raise RuntimeError('T42 initializer table count is not the new 104-table baseline')
        report['six_sql_sha256'] = {name: hashlib.sha256((sql_root / name).read_bytes()).hexdigest()
                                    for name in base.SQL_NAMES}
        if not (ROOT / 'backend/mvnw').is_file():
            raise RuntimeError('Backend Maven wrapper is missing')
        if not Path('/root/.m2/repository/org/apache/maven/plugins/maven-surefire-plugin/3.5.5/maven-surefire-plugin-3.5.5.jar').is_file():
            raise RuntimeError('Offline Surefire 3.5.5 is missing')

        root_pass, app_pass, redis_pass, minio_pass = (random_password() for _ in range(4))
        minio_user = 't42oss' + run_id
        app_user = 't42_' + run_id
        source_bucket = 't42-' + run_id + '-source'
        snapshot_bucket = 't42-' + run_id + '-snapshot'
        generated = [root_pass, app_pass, redis_pass, minio_user, minio_pass]
        mysql_container_env = run_dir / 'mysql-container.env'
        mysql_init_env = run_dir / 'mysql-init.env'
        redis_conf = run_dir / 'redis.conf'
        minio_env = run_dir / 'minio.env'
        properties = run_dir / 'surefire.properties'
        secret_paths = [mysql_container_env, mysql_init_env, redis_conf, minio_env, properties,
                        run_dir / 'alias-root.json', run_dir / 'alias-app.json',
                        run_dir / 'app-policy.json']
        private(mysql_container_env, ('MYSQL_ROOT_PASSWORD=' + root_pass + '\n').encode())
        private(minio_env, ('MINIO_ROOT_USER=' + minio_user
                            + '\nMINIO_ROOT_PASSWORD=' + minio_pass + '\n').encode())
        phase = 'mysql'
        docker_touched = True
        mysql_id = base.full_id(base.docker('run', '--pull=never', '-d', '--name',
                            't42-mysql-' + run_id,
                            '--label', 'namewta.test.owner=' + OWNER,
                            '--label', 'namewta.test.run=' + run_id,
                            '-p', '127.0.0.1::3306', '--env-file', str(mysql_container_env),
                            'mysql:8.4.9', '--character-set-server=utf8mb4',
                            '--collation-server=utf8mb4_general_ci',
                            '--log-bin-trust-function-creators=1'))
        containers.append(mysql_id)
        base.assert_owned(mysql_id, run_id)
        volumes.extend(base.volume_names(mysql_id))
        base.wait_mysql(mysql_id)
        mysql_port = base.mapped_port(mysql_id, '3306')
        ports.append(mysql_port)
        report['owned']['loopback_ports']['mysql'] = mysql_port
        phase = 'redis'
        private(redis_conf, base.redis_config(redis_pass))
        os.chown(redis_conf, base.REDIS_CONFIG_USER, base.REDIS_CONFIG_USER)
        redis_id = base.full_id(base.docker('run', '--pull=never', '-d', '--name',
                            't42-redis-' + run_id,
                            '--label', 'namewta.test.owner=' + OWNER,
                            '--label', 'namewta.test.run=' + run_id,
                            '-p', '127.0.0.1::6379', '--user', str(base.REDIS_CONFIG_USER),
                            '--mount', 'type=bind,source=' + str(redis_conf)
                            + ',target=/etc/redis/redis.conf,readonly',
                            'redis:8.6.3', 'redis-server', '/etc/redis/redis.conf'))
        containers.append(redis_id)
        base.assert_owned(redis_id, run_id)
        volumes.extend(base.volume_names(redis_id))
        base.wait_redis(redis_id)
        redis_port = base.mapped_port(redis_id, '6379')
        ports.append(redis_port)
        report['owned']['loopback_ports']['redis'] = redis_port
        phase = 'minio'
        minio_id = base.full_id(base.docker('run', '--pull=never', '-d', '--name',
                            't42-minio-' + run_id,
                            '--label', 'namewta.test.owner=' + OWNER,
                            '--label', 'namewta.test.run=' + run_id,
                            '-p', '127.0.0.1::9000', '--env-file', str(minio_env),
                            MINIO_IMAGE, 'server', '--address', ':9000', '/data'))
        containers.append(minio_id)
        base.assert_owned(minio_id, run_id)
        volumes.extend(base.volume_names(minio_id))
        minio_port = base.mapped_port(minio_id, '9000')
        ports.append(minio_port)
        report['owned']['loopback_ports']['minio'] = minio_port
        base.wait_minio(minio_port)
        phase = 'schema'
        private(mysql_init_env,
                ('MYSQL_DATABASE=wta-plus\nMYSQL_APP_USER=' + app_user
                 + '\nMYSQL_APP_PASSWORD=' + app_pass
                 + '\nMINIO_ROOT_USER=' + minio_user
                 + '\nMINIO_ROOT_PASSWORD=' + minio_pass
                 + '\nMINIO_ENDPOINT=127.0.0.1:' + str(minio_port)
                 + '\nMINIO_BUCKET=' + source_bucket + '\n').encode())
        init = base.run(('bash', str(init_script), '--container', mysql_id,
                         '--env-file', str(mysql_init_env)), timeout=600)
        init_clean, init_redactions = sanitize_bytes(init.stdout + init.stderr, generated)
        private(run_dir / 'init.log', init_clean)
        report['init'] = {'exit_code': init.returncode,
                          'raw_sha256': hashlib.sha256(init.stdout + init.stderr).hexdigest(),
                          'sanitized_sha256': hashlib.sha256(init_clean).hexdigest(),
                          'redactions': init_redactions}
        if init.returncode:
            raise RuntimeError('Owned six-SQL initialization failed')
        report['schema_tables'] = one(base, mysql_id,
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='wta-plus';")
        if report['schema_tables'] != 104:
            raise RuntimeError('Owned T42 schema does not contain 104 tables')
        if one(base, mysql_id,
               "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='wta-plus' "
               "AND TABLE_NAME='notify_intent_attachment';") != 1:
            raise RuntimeError('T42 attachment relation table is missing')
        endpoint = 'http://127.0.0.1:' + str(minio_port)
        root_mc, app_mc = run_dir / 'mc-root', run_dir / 'mc-app'
        import_alias(base, run_dir, root_mc, 'root', endpoint, minio_user, minio_pass)
        for bucket in (source_bucket, snapshot_bucket):
            mc(base, root_mc, 'mb', 'root/' + bucket)
        policy = {'Version': '2012-10-17', 'Statement': [
            {'Effect': 'Allow', 'Action': ['s3:GetObject', 's3:PutObject', 's3:DeleteObject',
                                          's3:AbortMultipartUpload', 's3:ListMultipartUploadParts'],
             'Resource': ['arn:aws:s3:::' + bucket + '/*'
                          for bucket in (source_bucket, snapshot_bucket)]},
            {'Effect': 'Allow', 'Action': ['s3:ListBucket', 's3:GetBucketLocation'],
             'Resource': ['arn:aws:s3:::' + bucket for bucket in (source_bucket, snapshot_bucket)]}]}
        policy_path = run_dir / 'app-policy.json'
        private(policy_path, json.dumps(policy).encode())
        item = mc(base, root_mc, 'admin', 'accesskey', 'create', 'root/',
                  '--policy', str(policy_path), '--json', json_result=True)
        item = item.get('data', item)
        if type(item) is not dict:
            raise RuntimeError('Owned MinIO limited identity has wrong shape')
        access, secret = item.get('accessKey'), item.get('secretKey')
        if not all(type(value) is str and len(value) >= 12 for value in (access, secret)):
            raise RuntimeError('Owned MinIO limited identity is absent')
        generated.extend((access, secret))
        import_alias(base, run_dir, app_mc, 'app', endpoint, access, secret)
        proxy = proxy_module.SafeS3CountServer(minio_port,
                                               {source_bucket: 'source', snapshot_bucket: 'snapshot'})
        proxy_port = proxy.server_address[1]
        ports.append(proxy_port)
        proxy_thread = threading.Thread(target=proxy.serve_forever, daemon=True)
        proxy_thread.start()
        report['owned']['loopback_ports']['s3_count_proxy'] = proxy_port
        count_reader = proxy_module.SafeS3CountReadServer(proxy)
        count_port = count_reader.server_address[1]
        ports.append(count_port)
        count_reader_thread = threading.Thread(target=count_reader.serve_forever, daemon=True)
        count_reader_thread.start()
        report['owned']['loopback_ports']['s3_count_reader'] = count_port
        report['oss_config_rows'] = configure_oss(base, mysql_id,
                            '127.0.0.1:' + str(proxy_port), access, secret,
                            source_bucket, snapshot_bucket)
        report['s3_baseline'] = {'request_count': proxy.snapshot_arrivals()}

        private_properties = {
            't42.owned.run': run_id,
            'notify.mail.attachment.integration': 'true',
            't42.mysql.url': ('jdbc:mysql://127.0.0.1:' + str(mysql_port)
                  + '/wta-plus?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'),
            't42.mysql.username': app_user, 't42.mysql.password': app_pass,
            't42.redis.host': '127.0.0.1', 't42.redis.port': str(redis_port),
            't42.redis.password': redis_pass,
            't42.minio.endpoint': 'http://127.0.0.1:' + str(proxy_port),
            't42.minio.access-key': access, 't42.minio.secret-key': secret,
            't42.minio.source-bucket': source_bucket,
            't42.minio.snapshot-bucket': snapshot_bucket,
            't42.s3.count-url': 'http://127.0.0.1:' + str(count_port) + '/count',
        }
        if tuple(private_properties) != PROPERTY_KEYS:
            raise RuntimeError('T42 private property schema differs')
        spring = base.isolated_config(mysql_port, redis_port, 0, base.free_port(),
                                      app_user, app_pass, redis_pass, run_dir)
        generated.append(spring['spring.boot.admin.client.password'])
        spring['server.port'] = 0
        spring['notify.outbox.poll-delay-ms'] = 3600000
        spring['spring.profiles.active'] = 'dev'
        # Do not permit a real SMTP account: MailNotificationSender must be
        # replaced by the test Bean, and the test asserts that exact identity.
        settings = {**private_properties, **spring}
        private(properties, properties_bytes(settings))
        argv = ('./mvnw', '-B', '-ntp', '-o', '-Pdev', '-pl', 'wta-admin', '-am',
                '-Dtest=' + SUITES[0].rsplit('.', 1)[1],
                '-Dsurefire.failIfNoSpecifiedTests=false', '-DforkCount=1',
                '-DreuseForks=false', '-Dsurefire.systemPropertiesFile=' + str(properties),
                'test')
        if any(value in ' '.join(argv) for value in generated):
            raise RuntimeError('Generated credential appeared in Maven argv')
        phase = 'maven'
        raw_log = run_dir / 'maven.raw.log'
        started_ns = time.time_ns()
        with os.fdopen(os.open(raw_log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as output:
            maven_env = base.safe_env(CI='1', TMPDIR=str(run_dir / 'tmp'),
                  JAVA_TOOL_OPTIONS='-Xms128m -Xmx1536m -Djava.io.tmpdir=' + str(run_dir / 'tmp'))
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
        if maven is not None:
            try:
                live = base.stop_group(maven)
                report['cleanup']['maven_group_live_members'] = live
                if live:
                    cleanup_errors.append('maven_process_group_remains')
            except Exception:
                cleanup_errors.append('maven_process_group_cleanup_failed')
        if started_ns is not None:
            try:
                live = stop_owned_processes(run_dir / 'tmp')
                report['cleanup']['owned_processes_remaining'] = live
                if live:
                    cleanup_errors.append('owned_process_outside_maven_group_remains')
            except Exception:
                cleanup_errors.append('owned_process_outside_maven_group_check_failed')
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
        if count_reader is not None:
            try:
                proxy.cancel_holds()
                if not proxy.wait_holds_drained(10):
                    cleanup_errors.append('s3_held_put_threads_remain')
            except Exception:
                cleanup_errors.append('s3_hold_cancel_failed')
            try:
                count_reader.shutdown()
                count_reader.server_close()
                count_reader_thread.join(timeout=10)
                if count_reader_thread.is_alive():
                    cleanup_errors.append('s3_count_reader_thread_remains')
            except Exception:
                cleanup_errors.append('s3_count_reader_cleanup_failed')
        if proxy is not None:
            try:
                metrics, arrivals = proxy.snapshot_drained(5)
                report['s3_total'] = {'arrivals': arrivals,
                    'facts': proxy_module.safe_rows(metrics)}
                report['s3_fault_status'] = proxy.fault_status()
            except Exception:
                cleanup_errors.append('s3_proxy_snapshot_failed')
            try:
                proxy.shutdown()
                proxy.server_close()
                proxy_thread.join(timeout=10)
                if proxy_thread.is_alive():
                    cleanup_errors.append('s3_proxy_thread_remains')
            except Exception:
                cleanup_errors.append('s3_proxy_cleanup_failed')
        for path in secret_paths:
            try:
                path.unlink(missing_ok=True)
            except Exception:
                cleanup_errors.append('private_input_cleanup_failed')
        for path in (run_dir / 'mc-root', run_dir / 'mc-app'):
            try:
                shutil.rmtree(path)
            except FileNotFoundError:
                pass
            except Exception:
                cleanup_errors.append('private_mc_config_cleanup_failed')
        try:
            shutil.rmtree(run_dir / 'tmp')
        except FileNotFoundError:
            pass
        except Exception:
            cleanup_errors.append('owned_java_tmpdir_cleanup_failed')
        if docker_touched and base is not None:
            try:
                remaining, errors = base.cleanup_containers(run_id, containers)
                report['cleanup']['remaining_owned_full_ids'] = remaining
                cleanup_errors.extend(errors)
            except Exception:
                cleanup_errors.append('owned_container_cleanup_failed')
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
        report['cleanup']['errors'] = cleanup_errors
        report['acceptance'] = (report.get('maven_exit_code') == 0
              and exact_suite_gate(report.get('suites', []), expected_methods)
              and report['source_after'] == report['source_before']
              and not cleanup_errors and 'failure' not in report)
        report['exit_code'] = 0 if report['acceptance'] else 1
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        private(run_dir / 'result.json',
                (json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode())
        print(str(run_dir / 'result.json'))
    return report['exit_code']


if __name__ == '__main__':
    sys.exit(main())
