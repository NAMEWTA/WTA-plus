#!/usr/bin/env python3
"""Verify the final full JAR on owned MySQL/Redis/MinIO, preserving legacy AI data.

Only container IDs returned by this process are eligible for cleanup. No existing
runtime DB is accepted. The historical schema is read from immutable Git solely
into this disposable fixture; the six canonical SQL files remain untouched.
"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[6]
HISTORICAL = 'dea1754fb1cae6d8d32fcc1ed19a91d6610ea5cd'
SQL_PATH = 'release-artifacts/docker/infrastructure/mysql/init'


def run(args, **kwargs):
    return subprocess.run([str(arg) for arg in args], check=True, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, **kwargs).stdout.strip()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', default=str(ROOT / 'backend/wta-admin/target/wta-admin.jar'))
    parser.add_argument('--output', required=True)
    parser.add_argument('--backend-commit', required=True)
    args = parser.parse_args()
    assert re.fullmatch(r'[0-9a-f]{40}', args.backend_commit)
    assert not run(['git', 'diff', args.backend_commit, '--', 'backend'], cwd=ROOT)
    artifact = Path(args.artifact).resolve()
    assert artifact.is_file()
    output = Path(args.output).resolve()
    output.mkdir(parents=True, exist_ok=True)
    owner = 'phone-ai-' + secrets.token_hex(10)
    password = secrets.token_hex(20)
    containers, app = [], None
    evidence = {'backend_commit': args.backend_commit,
                'backend_tree': run(['git', 'rev-parse', args.backend_commit + ':backend'], cwd=ROOT),
                'artifact_sha256': hashlib.sha256(artifact.read_bytes()).hexdigest(),
                'historical_schema_commit': HISTORICAL}

    def create(image, port, env=(), command=()):
        cid = run(['docker', 'create', '--label', 'namewta.test.owner=' + owner,
                   '-p', f'127.0.0.1::{port}', *sum((['-e', item] for item in env), []), image, *command])
        containers.append(cid)
        run(['docker', 'start', cid])
        published = int(run(['docker', 'port', cid, f'{port}/tcp']).rsplit(':', 1)[1])
        return cid, published

    def sql(statement):
        return run(['docker', 'exec', '-i', mysql, 'sh', '-c',
                    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --protocol=TCP -h127.0.0.1 -uroot --batch --skip-column-names --default-character-set=utf8mb4'], input=statement)

    def request(route):
        try:
            with urllib.request.urlopen(f'http://127.0.0.1:{app_port}' + route, timeout=10) as response:
                return response.status, response.read()
        except urllib.error.HTTPError as error:
            return error.code, error.read()

    def stop_app():
        nonlocal app
        if app is not None:
            app.terminate()
            try:
                app.wait(timeout=30)
            except subprocess.TimeoutExpired:
                app.kill()
                app.wait(timeout=10)
            app = None

    try:
        mysql, mysql_port = create('mysql:8.4.9', 3306, ['MYSQL_ROOT_PASSWORD=' + password],
                                  ['--character-set-server=utf8mb4', '--collation-server=utf8mb4_general_ci'])
        redis, redis_port = create('redis:8.6.3', 6379, command=['redis-server', '--save', '', '--appendonly', 'no'])
        minio, minio_port = create('pgsty/minio:RELEASE.2026-04-17T00-00-00Z', 9000,
                                  ['MINIO_ROOT_USER=owneduser', 'MINIO_ROOT_PASSWORD=' + password],
                                  ['server', '--address', ':9000', '/data'])
        for _ in range(120):
            try:
                assert sql('SELECT 1;') == '1'
                break
            except (AssertionError, subprocess.CalledProcessError):
                time.sleep(1)
        else:
            raise RuntimeError('Owned MySQL did not become ready')
        assert run(['docker', 'exec', redis, 'redis-cli', 'ping']) == 'PONG'
        with tempfile.TemporaryDirectory(prefix='wta-ai-runtime-') as temporary:
            scratch = Path(temporary)
            envfile = scratch / 'init.env'
            envfile.write_text(f'MYSQL_DATABASE=wta-plus\nMYSQL_APP_USER=ownedapp\nMYSQL_APP_PASSWORD={password}\n'
                               f'MINIO_ROOT_USER=owneduser\nMINIO_ROOT_PASSWORD={password}\nMINIO_ENDPOINT=127.0.0.1:{minio_port}\nMINIO_BUCKET=wta\n')
            envfile.chmod(0o600)
            init_command = ['bash', ROOT / 'release-artifacts/scripts/init-mysql-container.sh',
                            '--container', mysql, '--env-file', envfile, '--sql-dir', ROOT / SQL_PATH]
            result = subprocess.run([str(a) for a in init_command], text=True, capture_output=True)
            (output / 'init.log').write_text(result.stdout + result.stderr)
            assert result.returncode == 0, 'Protected initialization failed; see init.log'
            evidence['fresh_business_tables'] = int(sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wta-plus';"))
            evidence['fresh_ai_tables'] = int(sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wta-plus' AND LEFT(table_name,4)='sai_';"))
            evidence['nacos_tables'] = int(sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='nacos';"))
            assert (evidence['fresh_business_tables'], evidence['fresh_ai_tables'], evidence['nacos_tables']) == (103, 0, 10)
            assert sql("SELECT COUNT(*) FROM `wta-plus`.sys_user WHERE phone_number IS NULL OR phone_number NOT REGEXP '^1[3-9][0-9]{9}$';") == '0'
            assert sql("SELECT COUNT(*) FROM `wta-plus`.sys_menu WHERE component IN ('ai/chat/index','monitor/snailai/index');") == '0'
            with socket.socket() as sock:
                sock.bind(('127.0.0.1', 0))
                app_port = sock.getsockname()[1]
            config = {
                'server.address': '127.0.0.1', 'server.port': app_port,
                'spring.datasource.dynamic.datasource.master.url': f'jdbc:mysql://127.0.0.1:{mysql_port}/wta-plus?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
                'spring.datasource.dynamic.datasource.master.username': 'ownedapp',
                'spring.datasource.dynamic.datasource.master.password': password,
                'spring.data.redis.host': '127.0.0.1', 'spring.data.redis.port': redis_port,
                'spring.data.redis.password': '', 'spring.boot.admin.client.enabled': False,
                'spring.servlet.multipart.location': str(scratch),
                'snail-job.enabled': False, 'snail-ai.enabled': True,
                'openapi.enabled': True, 'openapi.kek-version': 'owned-v1',
                'openapi.kek': base64.b64encode(secrets.token_bytes(32)).decode(),
                'logging.file.path': str(scratch / 'logs'), 'springdoc.api-docs.enabled': True,
            }
            configfile = scratch / 'owned.yml'
            configfile.write_text(json.dumps(config));configfile.chmod(0o600)

            def boot(phase):
                nonlocal app
                logfile = (output / (phase + '-boot.log')).open('w')
                app = subprocess.Popen(['java', '-Xms256m', '-Xmx1024m', '-jar', str(artifact),
                                        '--spring.profiles.active=prod', '--spring.config.additional-location=file:' + str(configfile)],
                                       cwd=scratch, stdout=logfile, stderr=subprocess.STDOUT)
                logfile.close()
                for _ in range(180):
                    if app.poll() is not None:
                        raise RuntimeError(phase + ' backend exited; inspect boot log')
                    try:
                        status, body = request('/v3/api-docs')
                        if status == 200 and 'openapi' in json.loads(body):
                            # The ApplicationRunner must finish too; servlet availability alone is insufficient.
                            if '初始化OSS配置成功' in (output / (phase + '-boot.log')).read_text():
                                return json.loads(body)
                    except (OSError, ValueError):
                        pass
                    time.sleep(1)
                raise RuntimeError(phase + ' backend readiness timeout')

            fresh = boot('fresh')
            stop_app()
            # Construct only the disposable legacy scenario, never replay the new baseline here.
            historical = run(['git', 'show', HISTORICAL + ':' + SQL_PATH + '/40-cde-ai.sql'], cwd=ROOT)
            sql('USE `wta-plus`;\n' + historical)
            sql("USE `wta-plus`; INSERT INTO sai_model_config(provider_id,model_name,model_key,model_type,api_key) VALUES(1,'owned model','owned-model','CHAT','owned opaque encrypted sentinel');"
                "INSERT INTO sai_agent_conversation(agent_id,user_id,conversation_id,title) VALUES(1,1,'owned-conversation','保留会话');"
                "INSERT INTO sai_rag(name,embedding_model_id,dimension_of_vector_model,description) VALUES('保留知识库',1,3,'owned document metadata');")
            tables = sql("SELECT table_name FROM information_schema.tables WHERE table_schema='wta-plus' AND LEFT(table_name,4)='sai_' ORDER BY table_name;").splitlines()
            assert len(tables) == 23

            def fingerprint():
                dump = run(['docker', 'exec', mysql, 'sh', '-c',
                            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --skip-comments --skip-dump-date --skip-add-locks --skip-lock-tables --no-tablespaces --set-gtid-purged=OFF --order-by-primary wta-plus ' + ' '.join(tables)])
                return hashlib.sha256(dump.encode()).hexdigest()

            before = fingerprint()
            legacy = boot('legacy')
            required = legacy['components']['schemas']['RegisterBody']['required']
            assert 'phoneNumber' in required
            assert not any('snail' in path.lower() for path in legacy['paths'])
            assert any(path.startswith('/openapi') for path in legacy['paths']), 'NAMEWTA OpenAPI must remain'
            evidence['legacy_http'] = {}
            for route in ['/snail-ai/user/register', '/snail-chat/chat', '/api/snail/chat/chat']:
                status, body = request(route)
                # Complete security chain may reject unauthenticated unknown paths before dispatch.
                payload = json.loads(body) if body.startswith(b'{') else {}
                assert status in (401, 403, 404) or payload.get('code') in (401, 403, 404), (route, status)
                evidence['legacy_http'][route] = {'status': status, 'code': payload.get('code')}
            stop_app()
            after = fingerprint()
            assert before == after
            evidence.update(legacy_ai_tables=len(tables), legacy_before_sha256=before, legacy_after_sha256=after,
                            fresh_path_count=len(fresh['paths']), legacy_path_count=len(legacy['paths']),
                            register_required=required, fresh_boot='passed', legacy_boot='passed')
            (output / 'openapi.json').write_text(json.dumps(legacy, ensure_ascii=False) + '\n')
            evidence['openapi_sha256'] = hashlib.sha256((output / 'openapi.json').read_bytes()).hexdigest()
            # The protected initializer must refuse even this owned existing DB without mutation.
            repeated = subprocess.run([str(a) for a in init_command], text=True, capture_output=True)
            assert repeated.returncode != 0 and 'refusing existing database' in repeated.stderr
            assert after == fingerprint()
            evidence['existing_baseline_refused'] = True
    finally:
        stop_app()
        for cid in reversed(containers):
            actual = run(['docker', 'inspect', '--format', '{{ index .Config.Labels "namewta.test.owner" }}', cid])
            assert actual == owner, 'Refusing cleanup of foreign container'
            run(['docker', 'rm', '-fv', cid])
    evidence['owned_resources_removed'] = True
    (output / 'acceptance.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n')
    print('Owned runtime acceptance passed:', output)


if __name__ == '__main__':
    main()
