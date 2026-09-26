#!/usr/bin/env python3
"""One owned MinIO, real Chrome uploads, targeted existing OSS service protections; serial checks."""
import datetime
import json
import pathlib
import subprocess
import sys
import time
import urllib.request
import uuid
import xml.etree.ElementTree as ET

root = pathlib.Path(__file__).resolve().parents[6]
evidence = pathlib.Path(__file__).resolve().parent
name = sys.argv[1]
assert name.startswith('T-26-') and '/' not in name

def run(command):
    return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT).strip()

def inventory():
    return {key: sorted(run(command).splitlines()) for key, command in {
        'containers': ['docker', 'ps', '-aq', '--no-trunc'], 'networks': ['docker', 'network', 'ls', '-q', '--no-trunc'],
        'volumes': ['docker', 'volume', 'ls', '-q']}.items()}

record = dict(captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), before=inventory(), cleanup=[])
record['image'] = run(['docker', 'image', 'inspect', 'pgsty/minio:RELEASE.2026-04-17T00-00-00Z', '--format', '{{.Id}}'])
owned = None
code = 1
start = time.time()
try:
    owned = run(['docker', 'run', '-d', '--name', 'namewta-t26-minio-' + uuid.uuid4().hex[:12],
                 '--label', 'namewta.test.owner=T-26', '-p', '127.0.0.1::9000',
                 '-e', 'MINIO_ROOT_USER=namewta', '-e', 'MINIO_ROOT_PASSWORD=namewta123',
                 '-e', 'MINIO_API_CORS_ALLOW_ORIGIN=*', record['image'], 'server', '/data'])
    endpoint = 'http://' + run(['docker', 'port', owned, '9000/tcp'])
    print('Created owned MinIO', flush=True)
    for attempt in range(60):
        try:
            with urllib.request.urlopen(endpoint + '/minio/health/ready', timeout=1) as response:
                if response.status == 200: break
        except OSError:
            time.sleep(0.5)
    else: raise RuntimeError('Owned MinIO readiness timeout')
    command = ['./mvnw', '-B', '-ntp', '-pl', 'wta-admin', '-am',
               '-Dtest=BrowserUploadLifecycleIntegrationTest,OssUploadServiceUnitTest,OssLifecycleManagerUnitTest,OssAccessUrlServiceUnitTest,OssUploadStorageRoutingMinioIntegrationTest',
               '-Dsurefire.failIfNoSpecifiedTests=false', '-Dbrowser.upload.integration=true',
               '-Doss.minio.integration.endpoint=' + endpoint, '-Dnamewta.repo.root=' + str(root), 'test']
    record.update(command=command, cwd='backend', log=name + '.log')
    with (evidence/record['log']).open('w') as log:
        result = subprocess.run(command, cwd=root/'backend', stdout=log, stderr=subprocess.STDOUT)
    code = result.returncode
    record['counts'] = dict(tests=0, failures=0, errors=0, skipped=0)
    record['reports'] = []
    for report in (root/'backend').rglob('target/surefire-reports/TEST-*.xml'):
        if report.stat().st_mtime < start: continue
        suite = ET.parse(report).getroot()
        record['reports'].append(str(report.relative_to(root)))
        for key in record['counts']: record['counts'][key] += int(suite.get(key, 0))
finally:
    if owned:
        result = subprocess.run(['docker', 'rm', '-fv', owned], capture_output=True, text=True)
        record['cleanup'].append(dict(container=owned, exit_code=result.returncode))
        if result.returncode: code = 1
    record['after'] = inventory()
    record['resources_restored'] = record['before'] == record['after']
    if not record['resources_restored']: code = 1
    record.update(exit_code=code, seconds=round(time.time()-start, 2))
    (evidence/(name+'.json')).write_text(json.dumps(record, indent=2)+'\n')
print(json.dumps({key: record.get(key) for key in ['exit_code', 'seconds', 'counts', 'resources_restored']}), flush=True)
if 'log' in record:
    print((evidence/record['log']).read_text()[-12000:], flush=True)
raise SystemExit(code)
