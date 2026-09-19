#!/usr/bin/env python3
"""Render and compile actual classic templates, then execute them on owned MySQL."""
import datetime
import hashlib
import shutil
import tempfile
import os
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
}
record['images'] = images
record['source_fingerprint'] = source_fingerprint
suffix = uuid.uuid4().hex[:12]
database = 'namewta_tree_test_' + suffix
password = 'owned-tree-test-only'
code = 1
process = None
def stop(number, _frame):
    raise SystemExit(128 + number)
signal.signal(signal.SIGTERM, stop)
signal.signal(signal.SIGINT, stop)
start = time.time()
fixture = pathlib.Path(tempfile.mkdtemp(prefix='t30-template-', dir=root/'temp/team/lead'))
record['steps'] = []
record['fixture_directory'] = str(fixture.relative_to(root))
classes = fixture/'classes'; classes.mkdir()

def step(command, label):
    started = time.time()
    log_path = evidence/(name+'-'+label+'.log')
    with log_path.open('w') as log:
        result = subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT)
    record['steps'].append(dict(command=command, cwd='.', exit_code=result.returncode, seconds=round(time.time()-started, 2), log=log_path.name))
    print(label, 'exit', result.returncode, flush=True)
    if result.returncode:
        print(log_path.read_text()[-8500:], flush=True)
        raise RuntimeError(label+' failed')
    return log_path

try:
    engine = root/'temp/team/lead/t27-template-engine/freemarker-2.3.34.jar'
    assert hashlib.sha256(engine.read_bytes()).hexdigest() == '9a9fb91cd64199232eb1ca9766148a5d30ef8944be5fac051018f96c70c8f6a3'
    report = root/'backend/wta-admin/target/surefire-reports/TEST-org.namewta.test.contracts.CrudMySqlHttpIntegrationTest.xml'
    cp = next(x.attrib['value'] for x in ET.parse(report).getroot().find('properties') if x.attrib['name'] == 'java.class.path')
    cp = os.pathsep.join(x for x in cp.split(os.pathsep) if x)
    lombok = next(x for x in cp.split(os.pathsep) if '/lombok/' in x and x.endswith('.jar'))
    step(['javac', '-cp', str(engine), '-d', str(classes), 'docs/fm/tests/RenderClassicFixtures.java'], 'renderer-compile')
    step(['java', '-cp', str(classes)+os.pathsep+str(engine), 'RenderClassicFixtures', str(root/'docs/fm'), str(fixture)], 'render')
    generated = sorted((fixture/'src').rglob('*.java'))
    assert len(generated) == 9
    step(['javac', '-cp', cp, '-processorpath', lombok, '-processor', 'lombok.launch.AnnotationProcessorHider$AnnotationProcessor', '-d', str(classes), *map(str, generated), 'docs/fm/tests/ClassicTemplateMySqlChecks.java'], 'output-compile')
    record['rendered_files'] = []
    saved = evidence/(name+'-generated')
    for source in [*generated, *sorted(classes.rglob('*.xml'))]:
        relative = source.relative_to(fixture)
        target = saved/relative; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(source.read_bytes())
        record['rendered_files'].append(dict(path=str(target.relative_to(root)), sha256=hashlib.sha256(source.read_bytes()).hexdigest()))
    record['input_hashes'] = {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest() for path in [root/'docs/fm/java/service.java.ftl', root/'docs/fm/java/serviceImpl.java.ftl', root/'docs/fm/java/mapper.java.ftl', root/'docs/fm/xml/mapper.xml.ftl', root/'docs/fm/tests/RenderClassicFixtures.java', root/'docs/fm/tests/ClassicTemplateMySqlChecks.java']}
    mysql = run(['docker', 'run', '-d', '--name', 'namewta-t30-27-mysql-' + suffix, '--label', 'namewta.test.owner=T-30', '-p', '127.0.0.1::3306',
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
    url = 'jdbc:mysql://127.0.0.1:' + mysql_port + '/' + database + '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
    command = ['java', '-cp', str(classes)+os.pathsep+cp, 'ClassicTemplateMySqlChecks', url, str(classes)]
    runtime_log = step(command, 'runtime')
    output = runtime_log.read_text()
    print('\n'.join(line for line in output.splitlines() if line.startswith('TEMPLATE_')), flush=True)
    match = re.search(r'TEMPLATE_RESULT tests=(\d+) failures=0 skipped=0', output)
    assert match and int(match.group(1)) == 21, 'Expected all 21 actual generated-output scenarios'
    record['counts'] = dict(tests=int(match.group(1)), failures=0, errors=0, skipped=0)
    code = 0
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
    shutil.rmtree(fixture)
    record['fixture_directory_removed'] = not fixture.exists()
    record.update(exit_code=code, seconds=round(time.time()-start, 2))
    (evidence/(name+'.json')).write_text(json.dumps(record, indent=2)+'\n')
print(json.dumps({key: record.get(key) for key in ['exit_code', 'seconds', 'counts', 'resources_restored']}), flush=True)
raise SystemExit(code)
