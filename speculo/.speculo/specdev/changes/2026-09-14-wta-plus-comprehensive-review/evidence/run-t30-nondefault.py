#!/usr/bin/env python3
"""Serial HTTP/browser gates on owned loopback fixtures, with source and fresh report guards."""
import datetime
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
root = pathlib.Path(__file__).resolve().parents[6]
evidence = pathlib.Path(__file__).resolve().parent
name = sys.argv[1]
assert name.startswith('T-30-') and '/' not in name
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

def inventory():
    return {kind: sorted(subprocess.check_output(command, text=True).splitlines()) for kind, command in {
        'containers': ['docker', 'ps', '-aq', '--no-trunc'], 'networks': ['docker', 'network', 'ls', '-q', '--no-trunc'],
        'volumes': ['docker', 'volume', 'ls', '-q']}.items()}
record = dict(captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), before=inventory(),
              source_fingerprint=source_fingerprint, cwd='backend')
(evidence/(name+'-source.json')).write_text(json.dumps(dict(files=source_files, source_fingerprint=source_fingerprint), indent=2)+'\n')
env = os.environ.copy()
command = ['./mvnw', '-B', '-ntp', 'test', '-Dprofiles.active=', '-Dtest=ParamUnitTest,DemoUnitTest,AssertUnitTest,ThirdOutboundSysLogPersistenceTest,TestRichTextOssOwnerUnitTest,ThirdProviderServiceTest,ThirdEndpointSecurityTest,ThirdCredentialServiceTest,ThirdCredentialCryptoTest,ThirdProviderAdapterRegistryTest,ThirdHttpClientFactoryTest,ThirdAdminPermissionHttpTest,ThirdInvocationRecorderAdapterTest,ThirdGatewayAdapterTest,ThirdGatewayApplicationContextTest,ThirdLogSanitizerAdapterTest,PushTicketControllerTest,ValidationUtilsTest', '-Dsurefire.failIfNoSpecifiedTests=false']
record.update(command=command, log=name+'.log')
started = time.time()
print('START', ' '.join(command), flush=True)
with (evidence/record['log']).open('w') as log:
    result = subprocess.run(command, cwd=root/'backend', env=env, stdout=log, stderr=subprocess.STDOUT)
record['exit_code'] = result.returncode
record['counts'] = dict(tests=0, failures=0, errors=0, skipped=0)
record['suites'] = []
for path in sorted((root/'backend').rglob('target/surefire-reports/TEST-*.xml')):
    if path.stat().st_mtime < started: continue
    suite = ET.parse(path).getroot(); counts = {key:int(suite.get(key, 0)) for key in record['counts']}
    record['suites'].append(dict(name=suite.get('name'), **counts))
    for key, value in counts.items(): record['counts'][key] += value
record.update(after=inventory(), source_unchanged=source_snapshot()[0] == source_files, seconds=round(time.time()-started, 2))
record['resources_restored'] = record['before'] == record['after']
record['selected_classes'] = ['ParamUnitTest', 'DemoUnitTest', 'AssertUnitTest', 'ThirdOutboundSysLogPersistenceTest', 'TestRichTextOssOwnerUnitTest', 'ThirdProviderServiceTest', 'ThirdEndpointSecurityTest', 'ThirdCredentialServiceTest', 'ThirdCredentialCryptoTest', 'ThirdProviderAdapterRegistryTest', 'ThirdHttpClientFactoryTest', 'ThirdAdminPermissionHttpTest', 'ThirdInvocationRecorderAdapterTest', 'ThirdGatewayAdapterTest', 'ThirdGatewayApplicationContextTest', 'ThirdLogSanitizerAdapterTest', 'PushTicketControllerTest', 'ValidationUtilsTest']
record['declared_skip'] = 'DemoUnitTest.testDisabled: original JUnit Disabled teaching example; not a product acceptance scenario'
selected_seen = {row['name'].rsplit('.', 1)[-1] for row in record['suites']}
expected_skip = sum(row['skipped'] for row in record['suites'] if row['name']=='org.namewta.test.DemoUnitTest')
if selected_seen != set(record['selected_classes']) or not record['counts']['tests'] or any(record['counts'][key] for key in ('failures', 'errors')) or record['counts']['skipped'] != 1 or expected_skip != 1 or not record['resources_restored'] or not record['source_unchanged']:
    record['exit_code'] = 1
(evidence/(name+'.json')).write_text(json.dumps(record, ensure_ascii=False, indent=2)+'\n')
print(json.dumps({key:record[key] for key in ('exit_code','counts','resources_restored','source_unchanged','seconds')}), flush=True)
if record['exit_code']: print((evidence/record['log']).read_text()[-9000:], flush=True)
raise SystemExit(record['exit_code'])
