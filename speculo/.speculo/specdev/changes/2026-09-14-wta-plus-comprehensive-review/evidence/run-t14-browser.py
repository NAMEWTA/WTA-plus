#!/usr/bin/env python3
"""Serial owned MySQL/Redis/MinIO -> actual Profile/OSS MVC -> real Home Chrome."""
import datetime
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
assert name.startswith('T-14-') and '/' not in name

def run(command):
    return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT).strip()

def inventory():
    return {key: sorted(run(command).splitlines()) for key, command in {
        'containers': ['docker', 'ps', '-aq', '--no-trunc'], 'networks': ['docker', 'network', 'ls', '-q', '--no-trunc'],
        'volumes': ['docker', 'volume', 'ls', '-q']}.items()}

record = dict(captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), before=inventory(), cleanup=[], containers=[], initialization=[])
images = {
    'mysql': 'mysql:8.4.9@sha256:c36050afdca850f23cef85703f84c7531a5ae155a11b5ee1c60acb09937c4084',
    'redis': 'redis:7.4-alpine@sha256:ff02b58f971e7d7d156a1267e283fcbbeee91773b6aa36c49dac28ecfe28eadf',
    'minio': run(['docker', 'image', 'inspect', 'pgsty/minio:RELEASE.2026-04-17T00-00-00Z', '--format', '{{.Id}}'])}
record['images'] = images
suffix = uuid.uuid4().hex[:12]
database = 'namewta_profile_test_' + suffix
password = 'owned-profile-test-only'
code = 1
start = time.time()
try:
    mysql = run(['docker', 'run', '-d', '--name', 'namewta-t14-mysql-' + suffix, '--label', 'namewta.test.owner=T-14', '-p', '127.0.0.1::3306',
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
    for sql in sorted((root/'release-artifacts/docker/infrastructure/mysql/init').glob('*.sql')):
        result = subprocess.run(['docker', 'exec', '-i', mysql, 'mysql', '--default-character-set=utf8mb4', '-uroot', '-p' + password, database], input=sql.read_bytes(), capture_output=True)
        record['initialization'].append(dict(file=sql.name, exit_code=result.returncode))
        if result.returncode: raise RuntimeError('Baseline import ' + sql.name + ': ' + result.stderr.decode()[-3000:])
    redis = run(['docker', 'run', '-d', '--name', 'namewta-t14-redis-' + suffix, '--label', 'namewta.test.owner=T-14', '-p', '127.0.0.1::6379', images['redis'], 'redis-server', '--save', '', '--appendonly', 'no'])
    record['containers'].append(redis)
    for attempt in range(50):
        result = subprocess.run(['docker', 'exec', redis, 'redis-cli', 'ping'], capture_output=True, text=True)
        if result.returncode == 0 and result.stdout.strip() == 'PONG': break
        time.sleep(.1)
    else: raise RuntimeError('Owned Redis readiness timeout')
    redis_port = run(['docker', 'port', redis, '6379/tcp']).rsplit(':', 1)[1]
    minio = run(['docker', 'run', '-d', '--name', 'namewta-t14-minio-' + suffix, '--label', 'namewta.test.owner=T-14', '-p', '127.0.0.1::9000',
                 '-e', 'MINIO_ROOT_USER=namewta', '-e', 'MINIO_ROOT_PASSWORD=namewta123', '-e', 'MINIO_API_CORS_ALLOW_ORIGIN=*', images['minio'], 'server', '/data'])
    record['containers'].append(minio)
    endpoint = 'http://' + run(['docker', 'port', minio, '9000/tcp'])
    for attempt in range(60):
        try:
            with urllib.request.urlopen(endpoint + '/minio/health/ready', timeout=1) as response:
                if response.status == 200: break
        except OSError: time.sleep(.5)
    else: raise RuntimeError('Owned MinIO readiness timeout')
    command = ['./mvnw', '-B', '-ntp', '-pl', 'wta-admin', '-am', '-Dtest=ProfileSelfMaterialsBrowserIntegrationTest',
               '-Dsurefire.failIfNoSpecifiedTests=false', '-Dbrowser.profile.integration=true', '-Dprofile.redis.integration.port=' + redis_port,
               '-Dprofile.mysql.integration.url=jdbc:mysql://127.0.0.1:' + mysql_port + '/' + database + '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
               '-Doss.minio.integration.endpoint=' + endpoint, '-Dnamewta.repo.root=' + str(root), 'test']
    record.update(command=command, cwd='backend', log=name + '.log')
    print('Running actual Profile/MySQL/OSS/Chrome', flush=True)
    with (evidence/record['log']).open('w') as log:
        process = subprocess.Popen(command, cwd=root/'backend', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        for line in process.stdout:
            line = re.sub(r'''([?&]X-Amz-[^=]+)=([^&\s"']+)''', r'\1=[REDACTED]', line)
            log.write(line); log.flush()
            if any(marker in line for marker in ['[ERROR]', 'Tests run:', 'BUILD SUCCESS', 'BUILD FAILURE', 'T-14:', ' passed (', ' failed']): print(line, end='', flush=True)
        code = process.wait()
    record['counts'] = dict(tests=0, failures=0, errors=0, skipped=0)
    record['reports'] = []
    for report in (root/'backend').rglob('target/surefire-reports/TEST-*.xml'):
        if report.stat().st_mtime < start: continue
        suite = ET.parse(report).getroot(); record['reports'].append(str(report.relative_to(root)))
        for key in record['counts']: record['counts'][key] += int(suite.get(key, 0))
except Exception as error:
    code = 1
    record['failure'] = str(error)
    print(record['failure'], flush=True)
finally:
    for owned in reversed(record['containers']):
        result = subprocess.run(['docker', 'rm', '-fv', owned], capture_output=True, text=True)
        record['cleanup'].append(dict(container=owned, exit_code=result.returncode))
        if result.returncode: code = 1
    record['after'] = inventory(); record['resources_restored'] = record['before'] == record['after']
    if not record['resources_restored']: code = 1
    record.update(exit_code=code, seconds=round(time.time()-start, 2))
    (evidence/(name+'.json')).write_text(json.dumps(record, indent=2)+'\n')
print(json.dumps({key: record.get(key) for key in ['exit_code', 'seconds', 'counts', 'resources_restored']}), flush=True)
raise SystemExit(code)
