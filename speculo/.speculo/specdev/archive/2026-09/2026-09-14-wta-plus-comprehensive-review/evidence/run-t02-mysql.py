"""Run T-02 logging tests serially against an owned, disposable MySQL 8.4 container."""
from pathlib import Path
import datetime
import json
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[6]
EVIDENCE = Path(__file__).resolve().parent
suffix = uuid.uuid4().hex[:12]
container = 'namewta-log-test-' + suffix
database = 'namewta_log_test_' + suffix
password = 'namewta-log-test-only'
records = []

def run(command, **kwargs):
    result = subprocess.run(command, text=True, capture_output=True, **kwargs)
    records.append(dict(command=command, exit_code=result.returncode, stdout=result.stdout, stderr=result.stderr))
    if result.returncode:
        raise RuntimeError(f'{command[0]} failed ({result.returncode}): {result.stderr}')
    return result.stdout.strip()

code = 1
created = False
try:
    run(['docker', 'run', '--rm', '-d', '--name', container, '--label', 'namewta.test.owner=T-02',
         '-p', '127.0.0.1::3306', '-e', 'MYSQL_ROOT_PASSWORD=' + password, '-e', 'MYSQL_DATABASE=' + database,
         'mysql:8.4.9', '--character-set-server=utf8mb4', '--collation-server=utf8mb4_general_ci'])
    created = True
    print('Created owned MySQL fixture:', container, flush=True)
    for attempt in range(90):
        result = subprocess.run(['docker', 'exec', container, 'mysqladmin', 'ping', '-h', '127.0.0.1', '-uroot', '-p' + password, '--silent'], capture_output=True)
        if result.returncode == 0:
            break
        time.sleep(1)
    else:
        raise RuntimeError('MySQL fixture did not become ready in 90 seconds')
    port = run(['docker', 'port', container, '3306/tcp']).rsplit(':', 1)[1]
    command = ['./mvnw', '-B', '-ntp', '-pl', 'wta-admin', '-am', 'test',
               '-Dtest=LogSanitizerTest,LogAspectRedactionTest,SysLog*Test,OperationLogRedactionTest,LogRedactionHttpMySqlIntegrationTest',
               '-Dsurefire.failIfNoSpecifiedTests=false',
               '-Dlog.mysql.integration.url=jdbc:mysql://127.0.0.1:' + port + '/' + database + '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
               '-Dlog.mysql.integration.username=root', '-Dlog.mysql.integration.password=' + password,
               '-Dnamewta.sql.root=' + str(ROOT / 'release-artifacts/docker/infrastructure/mysql/init')]
    log_path = EVIDENCE / ('T-02-mysql-' + suffix + '.log')
    print('Running focused logging tests; raw output:', log_path.name, flush=True)
    with log_path.open('w') as log:
        process = subprocess.Popen(command, cwd=ROOT / 'backend', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        for line in process.stdout:
            log.write(line)
            log.flush()
            if any(marker in line for marker in ['Building ', '[ERROR]', 'Tests run:', 'BUILD SUCCESS', 'BUILD FAILURE']):
                print(line, end='', flush=True)
        code = process.wait()
    records.append(dict(command=command, cwd=str(ROOT / 'backend'), exit_code=code, log=log_path.name))
finally:
    if created:
        run(['docker', 'rm', '-f', container])
        print('Removed owned MySQL fixture:', container, flush=True)
    (EVIDENCE / ('T-02-mysql-' + suffix + '.json')).write_text(json.dumps(dict(
        captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        records=records, mysql_image='mysql:8.4.9',
        mysql_digest='sha256:c36050afdca850f23cef85703f84c7531a5ae155a11b5ee1c60acb09937c4084',
        exit_code=code, container_removed=created), ensure_ascii=False, indent=2) + '\n')
print('MAVEN_EXIT', code)
sys.exit(code)
