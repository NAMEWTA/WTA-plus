#!/usr/bin/env python3
"""Serial owned MySQL/Redis/MinIO -> actual Profile/OSS MVC -> real Home Chrome."""
import datetime
import hashlib
import signal
import json
import pathlib
import re
import subprocess
import sys
import time
import urllib.request
import uuid
import xml.etree.ElementTree as ET

root = pathlib.Path(__file__).resolve().parents[6]
evidence = pathlib.Path(__file__).resolve().parent
name = sys.argv[1]
assert name.startswith('T-30-') and '/' not in name

def run(command):
    return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT).strip()

def inventory():
    return {key: sorted(run(command).splitlines()) for key, command in {
        'containers': ['docker', 'ps', '-aq', '--no-trunc'], 'networks': ['docker', 'network', 'ls', '-q', '--no-trunc'],
        'volumes': ['docker', 'volume', 'ls', '-q']}.items()}

def source_snapshot():
    paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
    prefixes = ('.agents/', '.github/', 'backend/', 'frontend/', 'scripts/', 'release-artifacts/', 'docs/', 'speculo/workflows/')
    files = {p: hashlib.sha256((root/p).read_bytes()).hexdigest() if (root/p).is_file() else None
             for p in sorted(set(paths)) if p and (p.startswith(prefixes) or '/' not in p)}
    return files, hashlib.sha256(json.dumps(files, sort_keys=True, separators=(',', ':')).encode()).hexdigest()

source_files, source_fingerprint = source_snapshot()
assert source_files == json.loads((evidence/'T-30-source-current.json').read_text())['files']
assert not (evidence/(name+'.json')).exists(), 'preserve earlier results'
assert not (evidence/(name+'.log')).exists(), 'preserve earlier logs'
(evidence/(name+'-source.json')).write_text(json.dumps(dict(files=source_files, source_fingerprint=source_fingerprint), indent=2)+'\n')

record = dict(captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), before=inventory(), cleanup=[], containers=[], initialization=[])
images = {
    'mysql': 'mysql:8.4.9@sha256:c36050afdca850f23cef85703f84c7531a5ae155a11b5ee1c60acb09937c4084',
    'redis': 'redis:7.4-alpine@sha256:ff02b58f971e7d7d156a1267e283fcbbeee91773b6aa36c49dac28ecfe28eadf',
    'minio': run(['docker', 'image', 'inspect', 'pgsty/minio:RELEASE.2026-04-17T00-00-00Z', '--format', '{{.Id}}'])}
record['images'] = images
record['source_fingerprint'] = source_fingerprint
suffix = uuid.uuid4().hex[:12]
database = 'namewta_profile_test_' + suffix
password = 'owned-profile-test-only'
code = 1
process = None
def stop(number, _frame):
    raise SystemExit(128 + number)
signal.signal(signal.SIGTERM, stop)
signal.signal(signal.SIGINT, stop)
start = time.time()
try:
    mysql = run(['docker', 'run', '-d', '--name', 'namewta-t30-extra-mysql-' + suffix, '--label', 'namewta.test.owner=T-30', '-p', '127.0.0.1::3306',
                 '-e', 'MYSQL_ROOT_PASSWORD=' + password, '-e', 'MYSQL_DATABASE=' + database, images['mysql'],
                 '--character-set-server=utf8mb4', '--collation-server=utf8mb4_general_ci'])
    record['containers'].append(mysql)
    print('Created owned MySQL', flush=True)
    for attempt in range(90):
        result = subprocess.run(['docker', 'exec', mysql, 'mysqladmin', 'ping', '-h', '127.0.0.1', '-uroot', '-p' + password, '--silent'], capture_output=True)
        if result.returncode == 0: break
        time.sleep(1)
    else: raise RuntimeError('Owned MySQL readiness timeout')
    mysql_port = run(['docker', 'port', mysql, '3306/tcp']).rsplit(':', 1)[1]
    database_properties = {
        'admin.runtime': 'namewta_admin_runtime_test_',
        'oss.access.migration': 'namewta_oss_access_test_',
        'password.migration': 'namewta_password_test_',
        'profile.schema': 'namewta_profile_schema_test_',
        'oss.migration': 'namewta_oss_migration_test_',
        'log': 'namewta_log_test_',
    }
    extra_properties = []
    for property_prefix, database_prefix in database_properties.items():
        isolated_database = database_prefix + suffix
        run(['docker', 'exec', mysql, 'mysql', '-uroot', '-p' + password, '-e',
             'CREATE DATABASE ' + isolated_database + ' CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci'])
        record['initialization'].append(dict(database=isolated_database, purpose='Empty dedicated database; selected integration test creates its real contract fixture', exit_code=0))
        extra_properties.extend([
            '-D' + property_prefix + '.mysql.integration.url=jdbc:mysql://127.0.0.1:' + mysql_port + '/' + isolated_database + '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
            '-D' + property_prefix + '.mysql.integration.username=root',
            '-D' + property_prefix + '.mysql.integration.password=' + password,
        ])
    redis = run(['docker', 'run', '-d', '--name', 'namewta-t30-extra-redis-' + suffix, '--label', 'namewta.test.owner=T-30', '-p', '127.0.0.1::6379', images['redis'], 'redis-server', '--save', '', '--appendonly', 'no'])
    record['containers'].append(redis)
    for attempt in range(50):
        result = subprocess.run(['docker', 'exec', redis, 'redis-cli', 'ping'], capture_output=True, text=True)
        if result.returncode == 0 and result.stdout.strip() == 'PONG': break
        time.sleep(.1)
    else: raise RuntimeError('Owned Redis readiness timeout')
    redis_port = run(['docker', 'port', redis, '6379/tcp']).rsplit(':', 1)[1]
    minio = run(['docker', 'run', '-d', '--name', 'namewta-t30-extra-minio-' + suffix, '--label', 'namewta.test.owner=T-30', '-p', '127.0.0.1::9000',
                 '-e', 'MINIO_ROOT_USER=namewta', '-e', 'MINIO_ROOT_PASSWORD=namewta123', '-e', 'MINIO_API_CORS_ALLOW_ORIGIN=*', images['minio'], 'server', '/data'])
    record['containers'].append(minio)
    endpoint = 'http://' + run(['docker', 'port', minio, '9000/tcp'])
    for attempt in range(60):
        try:
            with urllib.request.urlopen(endpoint + '/minio/health/ready', timeout=1) as response:
                if response.status == 200: break
        except OSError: time.sleep(.5)
    else: raise RuntimeError('Owned MinIO readiness timeout')
    selected_tests = ['AuthorizationSessionRedisIntegrationTest', 'ClusterCacheInvalidationRedisIntegrationTest',
        'PasswordPolicyRedisIntegrationTest', 'TemporaryPasswordRedisIntegrationTest',
        'AdminRuntimeCapabilityMySqlIntegrationTest', 'OssAccessMigrationMySqlIntegrationTest',
        'PasswordMigrationMySqlIntegrationTest', 'ProfileSchemaMySqlIntegrationTest',
        'OssStorageMigrationIntegrationTest', 'LogRedactionHttpMySqlIntegrationTest']
    command = ['./mvnw', '-B', '-ntp', '-pl', 'wta-admin', '-am', '-Dtest=' + ','.join(selected_tests),
        '-Dsurefire.failIfNoSpecifiedTests=false', '-Dprofiles.active=dev,local',
        '-Dauthorization.session.redis.integration.port=' + redis_port,
        '-Dcache.redis.integration.port=' + redis_port,
        '-Dpassword.policy.redis.integration.port=' + redis_port,
        '-Dtemporary.password.redis.integration.port=' + redis_port,
        '-Doss.minio.integration.endpoint=' + endpoint,
        '-Dnamewta.sql.root=' + str(root/'release-artifacts/docker/infrastructure/mysql/init'),
        '-Dprofile.schema.sql.root=' + str(root), '-Dnamewta.repo.root=' + str(root), *extra_properties, 'test']
    record['selected_tests'] = selected_tests
    record.update(command=command, cwd='backend', log=name + '.log')
    print('Running actual session/cache/password/log/database/OSS migration contracts', flush=True)
    with (evidence/record['log']).open('w') as log:
        process = subprocess.Popen(command, cwd=root/'backend', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        for line in process.stdout:
            line = re.sub(r'''([?&]X-Amz-[^=]+)=([^&\s"']+)''', r'\1=[REDACTED]', line)
            log.write(line); log.flush()
            if any(marker in line for marker in ['[ERROR]', 'Tests run:', 'BUILD SUCCESS', 'BUILD FAILURE', 'T-15:', ' passed (', ' failed']): print(line, end='', flush=True)
        code = process.wait()
    record['counts'] = dict(tests=0, failures=0, errors=0, skipped=0)
    record['reports'] = []
    for report in (root/'backend').rglob('target/surefire-reports/TEST-*.xml'):
        if report.stat().st_mtime < start: continue
        suite = ET.parse(report).getroot(); record['reports'].append(dict(path=str(report.relative_to(root)), name=suite.get('name'), **{key: int(suite.get(key, 0)) for key in record['counts']}))
        for key in record['counts']: record['counts'][key] += int(suite.get(key, 0))
except Exception as error:
    code = 1
    record['failure'] = str(error)
    print(record['failure'], flush=True)
finally:
    if process is not None and process.poll() is None:
        process.terminate()
        try: process.wait(timeout=10)
        except subprocess.TimeoutExpired: process.kill(); process.wait()
    for owned in reversed(record['containers']):
        result = subprocess.run(['docker', 'rm', '-fv', owned], capture_output=True, text=True)
        record['cleanup'].append(dict(container=owned, exit_code=result.returncode))
        if result.returncode: code = 1
    if not record.get('counts', {}).get('tests') or any(record.get('counts', {}).get(key, 0) for key in ('failures', 'errors', 'skipped')): code = 1
    record['source_unchanged'] = source_snapshot()[0] == source_files
    if not record['source_unchanged']: code = 1
    record['after'] = inventory(); record['resources_restored'] = record['before'] == record['after']
    if not record['resources_restored']: code = 1
    record.update(exit_code=code, seconds=round(time.time()-start, 2))
    (evidence/(name+'.json')).write_text(json.dumps(record, indent=2)+'\n')
print(json.dumps({key: record.get(key) for key in ['exit_code', 'seconds', 'counts', 'resources_restored']}), flush=True)
raise SystemExit(code)
