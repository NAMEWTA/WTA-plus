#!/usr/bin/env python3
"""Opt-in T-38 full-JAR live /v3/api-docs capture on owned services only."""

import argparse
import base64
import datetime as dt
import hashlib
import http.client
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import zipfile


ROOT = Path('/srv/WTA-plus')
RUN_ROOT = Path('/tmp/wta-t38/openapi-live')
HELPER_PATH = Path('/tmp/wta-t38/run-notify-manual-retry-integration.py')
HELPER_EXPECTED_SHA = 'd068610f75a9c65cabf77d69d230884b33edd9c9f3d64c73c73296655066c31e'
OWNER = 'T-38-OPENAPI'
REDIS_CONFIG_USER = 65534
MINIO_IMAGE = 'pgsty/minio@sha256:83885c27b3b5b673049e33ddf4029afe2c134fd51ce4309e65e4f39d3b9ca282'
ARTIFACT = ROOT / 'backend/wta-admin/target/wta-admin.jar'
INIT_SCRIPT = ROOT / 'release-artifacts/scripts/init-mysql-container.sh'
SQL_ROOT = ROOT / 'release-artifacts/docker/infrastructure/mysql/init'
SOURCE_POINTER = ROOT / 'frontend/packages/api-contracts/openapi/current.json'
SOURCE_STORE = ROOT / 'frontend/packages/api-contracts/openapi/revisions'
MAX_OPENAPI_BYTES = 16 * 1024 * 1024
HTTP_METHODS = frozenset(('get', 'post', 'put', 'patch', 'delete', 'head', 'options', 'trace'))

if hashlib.sha256(HELPER_PATH.read_bytes()).hexdigest() != HELPER_EXPECTED_SHA:
    raise RuntimeError('Frozen owned-resource helper changed before import')
spec = importlib.util.spec_from_file_location('t38_owned_helper', HELPER_PATH)
helper = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helper)


def sha_file(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for part in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(part)
    return digest.hexdigest()


def private_bytes(path, value):
    with os.fdopen(os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
        stream.write(value)


def redact(value, secrets_to_redact):
    result = value
    for secret in secrets_to_redact:
        if secret:
            result = result.replace(secret, '[REDACTED]')
    return result


def validate_package_proof(proof, expected_head, expected_tree, expected_jar_sha, actual_size):
    expected_command = ['./mvnw', '-B', '-ntp', 'clean', 'package', '-DskipTests']
    command = proof.get('command')
    if command not in (expected_command, expected_command + ['-Pbundle-full']):
        raise RuntimeError('Package proof is not the full clean backend package command')
    if proof.get('cwd') != 'backend' or proof.get('exit_code') != 0 or proof.get('source_clean_at_build') is not True:
        raise RuntimeError('Package proof lacks a successful clean build boundary')
    if proof.get('source_head') != expected_head or proof.get('source_tree') != expected_tree:
        raise RuntimeError('Package proof does not bind the exact source')
    artifact = proof.get('artifact') or {}
    if (artifact.get('path') != 'backend/wta-admin/target/wta-admin.jar'
        or artifact.get('sha256') != expected_jar_sha or artifact.get('size_bytes') != actual_size):
        raise RuntimeError('Package proof does not bind the actual full JAR')
    build_log = proof.get('build_log') or {}
    log_path = Path(build_log.get('path', ''))
    if (not log_path.is_absolute() or not log_path.is_file()
        or not re.fullmatch(r'[0-9a-f]{64}', str(build_log.get('sha256', '')))
        or sha_file(log_path) != build_log['sha256']):
        raise RuntimeError('Package proof does not bind an existing build log')
    if b'BUILD SUCCESS' not in log_path.read_bytes()[-16384:]:
        raise RuntimeError('Bound full package log lacks BUILD SUCCESS')
    try:
        start = dt.datetime.fromisoformat(proof['started_utc'].replace('Z', '+00:00'))
        finish = dt.datetime.fromisoformat(proof['finished_utc'].replace('Z', '+00:00'))
        artifact_time = dt.datetime.fromtimestamp(ARTIFACT.stat().st_mtime, dt.timezone.utc)
    except (KeyError, TypeError, ValueError) as exc:
        raise RuntimeError('Package proof lacks parseable UTC build bounds') from exc
    if (start.tzinfo is None or finish.tzinfo is None or start > finish
        or not start <= artifact_time <= finish + dt.timedelta(seconds=2)):
        raise RuntimeError('Full JAR modification time falls outside the clean package proof')


def validate_full_jar(path):
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
    required = ('wta-system', 'wta-notify', 'wta-third', 'wta-sso', 'wta-profile-person',
                'wta-profile-enterprise', 'wta-job', 'wta-ai', 'wta-demo', 'wta-workflow')
    missing = [module for module in required if not any(
        re.fullmatch(r'BOOT-INF/lib/' + re.escape(module) + r'-[0-9][^/]*\.jar', name)
        for name in names)]
    if missing or any('BOOT-INF/lib/snail-ai-' in name for name in names):
        raise RuntimeError('Artifact is not the current full admin bundle')
    result = helper.run(('bash', 'scripts/ci/verify-admin-bundle.sh', 'full'), timeout=90)
    if result.returncode:
        raise RuntimeError('Full admin bundle verification failed')
    return list(required)


def owned_ids(run_id):
    output = helper.docker('ps', '-aq', '--no-trunc', '--filter', 'label=namewta.test.owner=' + OWNER,
                           '--filter', 'label=namewta.test.run=' + run_id)
    return [helper.full_id(item) for item in output.splitlines()] if output else []


def assert_owned(cid, run_id):
    labels = json.loads(helper.docker('inspect', '--format', '{{json .Config.Labels}}', cid))
    if labels.get('namewta.test.owner') != OWNER or labels.get('namewta.test.run') != run_id:
        raise RuntimeError('Container fails both exact owner/run label checks')


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
                helper.docker('rm', '-fv', cid, timeout=60)
            except Exception:
                errors.append('owned_container_remove_failed')
        remaining = owned_ids(run_id)
        if remaining:
            errors.append('owned_containers_remain')
    except Exception:
        errors.append('owned_container_discovery_or_verification_failed')
    return remaining, errors


def minio_healthy(port):
    client = http.client.HTTPConnection('127.0.0.1', port, timeout=3)
    try:
        client.request('GET', '/minio/health/live')
        response = client.getresponse()
        response.read(1024)
        return response.status == 200
    except (OSError, http.client.HTTPException):
        return False
    finally:
        client.close()


def wait_minio(port, seconds=90):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        if minio_healthy(port):
            return
        time.sleep(1)
    raise RuntimeError('Owned MinIO health timed out')


def redis_config(password):
    if not re.fullmatch(r'[A-Za-z0-9]{40}', password):
        raise RuntimeError('Owned Redis password is not a generated alphanumeric value')
    return ('bind 0.0.0.0\nprotected-mode yes\nport 6379\ndir /tmp\n'
            'save ""\nappendonly no\nrequirepass ' + password + '\n').encode()


def wait_owned_redis(cid, seconds=45):
    """Read the mounted secret inside the owned container; never pass it in argv."""
    command = (*helper.DOCKER, 'exec', cid, 'sh', '-c',
               'REDISCLI_AUTH="$(sed -n "s/^requirepass //p" /etc/redis/redis.conf)" exec redis-cli ping')
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        result = helper.run(command, timeout=12)
        if result.returncode == 0 and helper.output(result) == 'PONG':
            return
        time.sleep(1)
    raise RuntimeError('Owned authenticated Redis readiness timed out')


def isolated_config(mysql_port, redis_port, app_user, app_password, redis_password, run_dir):
    config = {
        'server.address': '127.0.0.1',
        'server.port': 0,
        'spring.datasource.dynamic.datasource.master.url':
            f'jdbc:mysql://127.0.0.1:{mysql_port}/wta-plus?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
        'spring.datasource.dynamic.datasource.master.username': app_user,
        'spring.datasource.dynamic.datasource.master.password': app_password,
        'spring.data.redis.host': '127.0.0.1',
        'spring.data.redis.port': redis_port,
        'spring.data.redis.password': redis_password,
        'spring.boot.admin.client.enabled': False,
        'spring.boot.admin.client.url': 'http://127.0.0.1:1/admin',
        'spring.boot.admin.client.username': 'owned-monitor',
        'spring.boot.admin.client.password': helper.random_password(),
        'snail-job.enabled': False,
        'snail-job.server.host': '127.0.0.1',
        'nacos.config.enabled': False,
        'nacos.config.server-addr': '127.0.0.1:1',
        'spring.ai.mcp.client.enabled': False,
        'spring.ai.mcp.client.streamable-http.connections.knowledge.url': 'http://127.0.0.1:1',
        'spring.ai.mcp.client.streamable-http.connections.crm.url': 'http://127.0.0.1:1',
        'easy-es.enable': False,
        'easy-es.address': '127.0.0.1:1',
        'justauth.address': 'http://127.0.0.1:1',
        'justauth.type.maxkey.server-url': 'http://127.0.0.1:1',
        'justauth.type.topiam.server-url': 'http://127.0.0.1:1',
        'justauth.type.gitea.server-url': 'http://127.0.0.1:1',
        'namewta.sso.web-origin': 'http://127.0.0.1:1',
        'springdoc.api-docs.enabled': True,
        'openapi.enabled': True,
        'openapi.kek-version': 'owned-t38',
        'openapi.kek': base64.b64encode(os.urandom(32)).decode('ascii'),
        'spring.servlet.multipart.location': str(run_dir / 'multipart'),
        'logging.file.path': str(run_dir / 'logs'),
        'management.endpoint.logfile.external-file': str(run_dir / 'logs' / 'sys-console.log'),
        'logging.level.org.namewta.NamewtaApplication': 'INFO',
    }
    if (config['server.address'] != '127.0.0.1'
        or 'jdbc:mysql://127.0.0.1:' not in config['spring.datasource.dynamic.datasource.master.url']
        or config['spring.data.redis.host'] != '127.0.0.1'):
        raise RuntimeError('Owned startup config is not loopback isolated')
    return config


def database_counts(mysql_id):
    queries = {
        'business_tables': 'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();',
        'claimable_outboxes': "SELECT COUNT(*) FROM notify_outbox WHERE status IN ('READY','PROCESSING');",
        'all_outboxes': 'SELECT COUNT(*) FROM notify_outbox;',
        'accepted_sms': "SELECT COUNT(*) FROM notify_delivery WHERE channel='SMS' AND status='ACCEPTED';",
        'all_external_deliveries': "SELECT COUNT(*) FROM notify_delivery WHERE channel IN ('SMS','MAIL');",
        'enabled_external_accounts': "SELECT COUNT(*) FROM notify_channel_account WHERE channel IN ('SMS','MAIL') AND enabled='Y';",
        'private_default_oss': "SELECT COUNT(*) FROM sys_oss_config WHERE config_key='minio' AND status='Y' AND access_policy='0';",
    }
    result = {key: int(helper.mysql(mysql_id, 'wta-plus', sql)) for key, sql in queries.items()}
    if result != {'business_tables': 103, 'claimable_outboxes': 0, 'all_outboxes': 0,
                  'accepted_sms': 0, 'all_external_deliveries': 0,
                  'enabled_external_accounts': 0, 'private_default_oss': 1}:
        raise RuntimeError('Fresh owned database violates the no-provider-call startup preflight')
    return result


def fetch_local_bytes(port):
    client = http.client.HTTPConnection('127.0.0.1', port, timeout=10)
    try:
        client.request('GET', '/v3/api-docs', headers={'Accept': 'application/json'})
        response = client.getresponse()
        if response.status != 200:
            raise RuntimeError('Full backend /v3/api-docs returned non-200')
        body = response.read(MAX_OPENAPI_BYTES + 1)
        if len(body) > MAX_OPENAPI_BYTES:
            raise RuntimeError('Full backend /v3/api-docs exceeded bounded capture size')
        return body, response.getheader('Content-Type', '')
    finally:
        client.close()


def runtime_port(log_text):
    match = re.search(r'访问地址：http://127\.0\.0\.1:(\d+)/', log_text)
    if not match:
        return None
    port = int(match.group(1))
    return port if 0 < port <= 65535 else None


def wait_full_backend(proc, raw_log, seconds=240):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        if proc.poll() is not None:
            raise RuntimeError('Full backend exited before startup completed')
        log_text = raw_log.read_text(errors='replace')[-200000:] if raw_log.exists() else ''
        port = runtime_port(log_text)
        if port and '初始化OSS配置成功' in log_text:
            try:
                body, content_type = fetch_local_bytes(port)
                return port, body, content_type
            except (OSError, http.client.HTTPException, RuntimeError):
                pass
        time.sleep(1)
    raise RuntimeError('Full backend live OpenAPI startup timed out')


def baseline_spec():
    pointer = json.loads(SOURCE_POINTER.read_text())
    revision = pointer.get('revision')
    if not isinstance(revision, str) or not re.fullmatch(r'[0-9a-f]{64}', revision):
        raise RuntimeError('Current committed OpenAPI pointer is invalid')
    path = SOURCE_STORE / revision / 'source.json'
    return json.loads(path.read_bytes()), revision


class OpenApiLossError(RuntimeError):
    def __init__(self, missing):
        super().__init__('Live full document lost paths, methods or schemas from the active snapshot')
        self.missing = missing


def validate_full_openapi(body, current):
    live = json.loads(body)
    paths = live.get('paths')
    schemas = (live.get('components') or {}).get('schemas')
    if (not re.fullmatch(r'3\.(0|1)\.[0-9]+', str(live.get('openapi', '')))
        or not isinstance(paths, dict) or not paths
        or not isinstance(schemas, dict) or not schemas):
        raise RuntimeError('Live response is not a full OpenAPI 3.0/3.1 document')
    old_paths = current.get('paths') or {}
    old_schemas = (current.get('components') or {}).get('schemas') or {}
    missing_paths = sorted(set(old_paths) - set(paths))
    missing_operations = sorted((path, method) for path, operations in old_paths.items()
                                for method in HTTP_METHODS.intersection(operations)
                                if path in paths and method not in paths[path])
    missing_schemas = sorted(set(old_schemas) - set(schemas))
    if missing_paths or missing_operations or missing_schemas:
        raise OpenApiLossError({'paths': missing_paths,
                                'operations': [{'path': path, 'method': method}
                                               for path, method in missing_operations],
                                'schemas': missing_schemas})
    if ('post' not in paths.get('/notify/notification/{notificationId}/retry', {})
        or 'post' not in paths.get('/notify/notification/{notificationId}/cancel', {})):
        raise RuntimeError('Live notification retry/cancel POST operations are absent')
    queued = (schemas.get('RetryReceipt') or {}).get('properties', {}).get('queuedCount')
    if not isinstance(queued, dict) or queued.get('type') != 'integer':
        raise RuntimeError('Live RetryReceipt.queuedCount is not an integer property')
    return {'openapi': live['openapi'], 'paths': len(paths), 'schemas': len(schemas),
            'baseline_paths': len(old_paths), 'baseline_schemas': len(old_schemas),
            'missing_paths': 0, 'missing_operations': 0, 'missing_schemas': 0,
            'queued_count_type': queued['type']}


def persist_and_validate_openapi(run_dir, body, content_type, current, report):
    """Keep the exact bounded HTTP 200 bytes even when strict schema validation fails."""
    source_path = run_dir / 'source.json'
    private_bytes(source_path, body)
    report['openapi_raw'] = {'http_status': 200, 'content_type': content_type,
                             'bytes': len(body), 'raw_sha256': hashlib.sha256(body).hexdigest(),
                             'source_path': str(source_path)}
    try:
        report['openapi'] = validate_full_openapi(body, current)
    except OpenApiLossError as exc:
        report['openapi_missing'] = exc.missing
        raise


def preflight():
    """Static local inspection only: never contacts Docker, JVM or HTTP."""
    _, revision = baseline_spec()
    observed_helper_sha = sha_file(HELPER_PATH)
    result = {
        'mode': 'static-preflight-only',
        'service_started': False,
        'expected_images': ['mysql:8.4.9', 'redis:8.6.3',
                            MINIO_IMAGE],
        'helper_sha256': observed_helper_sha,
        'helper_matches_frozen_sha': observed_helper_sha == HELPER_EXPECTED_SHA,
        'initializer_exists': INIT_SCRIPT.is_file(),
        'six_sql_files_present': all((SQL_ROOT / name).is_file() for name in helper.SQL_NAMES),
        'baseline_revision': revision,
        'source_head': helper.git('rev-parse', 'HEAD'),
        'source_clean': not bool(helper.git('status', '--porcelain', '--untracked-files=all')),
        'jar_exists': ARTIFACT.is_file(),
        'jar_sha256_if_present': sha_file(ARTIFACT) if ARTIFACT.is_file() else None,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if (result['helper_matches_frozen_sha'] and result['initializer_exists']
                 and result['six_sql_files_present']) else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--preflight', action='store_true',
                        help='Only static local files/Git; no Docker, Maven, JVM or HTTP')
    parser.add_argument('--execute', action='store_true')
    parser.add_argument('--expected-head')
    parser.add_argument('--expected-jar-sha256')
    parser.add_argument('--package-proof')
    args = parser.parse_args()
    if args.preflight:
        if args.execute or args.expected_head or args.expected_jar_sha256 or args.package_proof:
            parser.error('--preflight cannot be combined with --execute or execution parameters')
        return preflight()
    if (not args.execute or not args.expected_head or not re.fullmatch(r'[0-9a-f]{40}', args.expected_head)
        or not args.expected_jar_sha256 or not re.fullmatch(r'[0-9a-f]{64}', args.expected_jar_sha256)
        or not args.package_proof):
        parser.error('no launch without --execute, exact clean SHA, JAR SHA-256 and package proof')

    os.umask(0o077)
    run_id = __import__('secrets').token_hex(8)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(parents=True, mode=0o700)
    root_password = helper.random_password()
    app_password = helper.random_password()
    redis_password = helper.random_password()
    minio_password = helper.random_password()
    app_user = 't38o_' + run_id
    minio_user = 't38minio' + run_id
    bucket = 't38-' + run_id
    secrets_to_redact = [root_password, app_password, redis_password, minio_password]
    report = {'gate': 'T-38 full-JAR live OpenAPI capture', 'run_id': run_id,
              'acceptance': False, 'exit_code': 1,
              'started_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'source_before': None, 'artifact_before': None,
              'owned': {}, 'cleanup': {}}
    captured = []
    volumes = []
    ports = []
    errors = []
    app_proc = None
    app_port = None
    raw_log = run_dir / 'backend.raw.log'
    clean_log = run_dir / 'backend.log'
    secret_files = [run_dir / name for name in (
        'mysql-container.env', 'redis.conf', 'minio-container.env', 'init.env', 'owned.yml')]
    try:
        helper_sha = sha_file(HELPER_PATH)
        report['helper_sha256'] = helper_sha
        if helper_sha != HELPER_EXPECTED_SHA:
            raise RuntimeError('Owned resource helper changed since this capture runner review')
        source_before = helper.source_identity(args.expected_head)
        report['source_before'] = source_before
        if not ARTIFACT.is_file() or sha_file(ARTIFACT) != args.expected_jar_sha256:
            raise RuntimeError('JAR does not match the exact expected artifact SHA-256')
        report['artifact_before'] = {'path': str(ARTIFACT.relative_to(ROOT)),
                                     'sha256': args.expected_jar_sha256, 'size_bytes': ARTIFACT.stat().st_size}
        proof = json.loads(Path(args.package_proof).read_text())
        validate_package_proof(proof, args.expected_head, source_before['tree'],
                               args.expected_jar_sha256, ARTIFACT.stat().st_size)
        report['package_proof_sha256'] = sha_file(Path(args.package_proof))
        report['full_bundle_modules'] = validate_full_jar(ARTIFACT)
        current, revision = baseline_spec()
        report['baseline_revision'] = revision
        report['sql_baselines'] = {name: sha_file(SQL_ROOT / name) for name in helper.SQL_NAMES}

        mysql_env = run_dir / 'mysql-container.env'
        private_bytes(mysql_env, ('MYSQL_ROOT_PASSWORD=' + root_password + '\n').encode())
        mysql_id = helper.full_id(helper.docker(
            'run', '--pull=never', '-d', '--name', 't38-openapi-mysql-' + run_id,
            '--label', 'namewta.test.owner=' + OWNER, '--label', 'namewta.test.run=' + run_id,
            '-p', '127.0.0.1::3306', '--env-file', str(mysql_env),
            'mysql:8.4.9', '--character-set-server=utf8mb4',
            '--collation-server=utf8mb4_general_ci', '--log-bin-trust-function-creators=1'))
        captured.append(mysql_id)
        assert_owned(mysql_id, run_id)
        volumes.extend(helper.volume_names(mysql_id))
        report['owned']['mysql_container_id'] = mysql_id
        helper.wait_ready(mysql_id, 'mysql', 120)
        mysql_port = helper.mapped_port(mysql_id, '3306')
        ports.append(mysql_port)

        redis_conf = run_dir / 'redis.conf'
        private_bytes(redis_conf, redis_config(redis_password))
        os.chown(redis_conf, REDIS_CONFIG_USER, REDIS_CONFIG_USER)
        redis_id = helper.full_id(helper.docker(
            'run', '--pull=never', '-d', '--name', 't38-openapi-redis-' + run_id,
            '--label', 'namewta.test.owner=' + OWNER, '--label', 'namewta.test.run=' + run_id,
            '-p', '127.0.0.1::6379', '--user', str(REDIS_CONFIG_USER) + ':' + str(REDIS_CONFIG_USER),
            '--mount', 'type=bind,source=' + str(redis_conf) + ',target=/etc/redis/redis.conf,readonly',
            'redis:8.6.3', 'redis-server', '/etc/redis/redis.conf'))
        captured.append(redis_id)
        assert_owned(redis_id, run_id)
        volumes.extend(helper.volume_names(redis_id))
        report['owned']['redis_container_id'] = redis_id
        wait_owned_redis(redis_id)
        redis_port = helper.mapped_port(redis_id, '6379')
        ports.append(redis_port)

        minio_env = run_dir / 'minio-container.env'
        private_bytes(minio_env, ('MINIO_ROOT_USER=' + minio_user + '\nMINIO_ROOT_PASSWORD=' + minio_password + '\n').encode())
        minio_id = helper.full_id(helper.docker(
            'run', '--pull=never', '-d', '--name', 't38-openapi-minio-' + run_id,
            '--label', 'namewta.test.owner=' + OWNER, '--label', 'namewta.test.run=' + run_id,
            '-p', '127.0.0.1::9000', '--env-file', str(minio_env),
            MINIO_IMAGE, 'server', '--address', ':9000', '/data'))
        captured.append(minio_id)
        assert_owned(minio_id, run_id)
        volumes.extend(helper.volume_names(minio_id))
        report['owned']['minio_container_id'] = minio_id
        minio_port = helper.mapped_port(minio_id, '9000')
        ports.append(minio_port)
        wait_minio(minio_port)
        report['owned']['loopback_ports'] = {'mysql': mysql_port, 'redis': redis_port, 'minio': minio_port}

        init_env = run_dir / 'init.env'
        private_bytes(init_env, (
            'MYSQL_DATABASE=wta-plus\nMYSQL_APP_USER=' + app_user + '\nMYSQL_APP_PASSWORD=' + app_password
            + '\nMINIO_ROOT_USER=' + minio_user + '\nMINIO_ROOT_PASSWORD=' + minio_password
            + '\nMINIO_ENDPOINT=127.0.0.1:' + str(minio_port) + '\nMINIO_BUCKET=' + bucket + '\n').encode())
        init_argv = ('bash', str(INIT_SCRIPT), '--container', mysql_id,
                     '--env-file', str(init_env), '--sql-dir', str(SQL_ROOT))
        init_result = helper.run(init_argv, timeout=900)
        private_bytes(run_dir / 'init.log', redact(
            (init_result.stdout + init_result.stderr).decode('utf-8', errors='replace'), secrets_to_redact).encode())
        report['init_exit_code'] = init_result.returncode
        if init_result.returncode:
            raise RuntimeError('Owned six-SQL initializer failed')
        report['owned']['database_counts'] = database_counts(mysql_id)

        config = isolated_config(mysql_port, redis_port, app_user, app_password, redis_password, run_dir)
        secrets_to_redact.extend((config['spring.boot.admin.client.password'], config['openapi.kek']))
        (run_dir / 'multipart').mkdir(mode=0o700)
        (run_dir / 'logs').mkdir(mode=0o700)
        (run_dir / 'tmp').mkdir(mode=0o700)
        config_path = run_dir / 'owned.yml'
        private_bytes(config_path, (json.dumps(config, ensure_ascii=False) + '\n').encode())
        java_argv = ('java', '-Xms256m', '-Xmx1024m', '-Duser.home=' + str(run_dir),
                     '-Djava.io.tmpdir=' + str(run_dir / 'tmp'), '-jar', str(ARTIFACT),
                     '--spring.profiles.active=prod',
                     '--spring.config.additional-location=file:' + str(config_path))
        if any(secret in ' '.join(java_argv) for secret in secrets_to_redact):
            raise RuntimeError('A secret entered full-JAR argv')
        report['java_command_redacted'] = list(java_argv)
        with os.fdopen(os.open(raw_log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
            app_proc = subprocess.Popen(java_argv, cwd=run_dir, stdout=stream,
                                        stderr=subprocess.STDOUT, start_new_session=True,
                                        env=helper.safe_env(TZ='Asia/Shanghai', HOME=str(run_dir),
                                                            TMPDIR=str(run_dir / 'tmp')))
        report['owned']['backend_pgid'] = app_proc.pid
        app_port, body, content_type = wait_full_backend(app_proc, raw_log)
        ports.append(app_port)
        report['owned']['loopback_ports']['backend'] = app_port
        persist_and_validate_openapi(run_dir, body, content_type, current, report)
        report['exit_code'] = 0
    except BaseException as exc:
        report['error'] = redact(type(exc).__name__ + ': ' + str(exc), secrets_to_redact)
    finally:
        if app_proc is not None:
            try:
                live = helper.stop_group(app_proc)
                report['cleanup']['backend_process_group'] = {'pgid': app_proc.pid, 'live_members': live}
                if live:
                    errors.append('backend_process_group_remains')
            except Exception:
                errors.append('backend_process_group_cleanup_failed')
        try:
            helper.sanitize_log(raw_log, clean_log, secrets_to_redact)
        except Exception:
            errors.append('backend_log_redaction_or_removal_failed')
        for path in secret_files:
            try:
                path.unlink(missing_ok=True)
            except Exception:
                errors.append('private_env_or_config_removal_failed')
        try:
            shutil.rmtree(run_dir / 'logs', ignore_errors=False)
        except FileNotFoundError:
            pass
        except Exception:
            errors.append('application_file_logs_removal_failed')
        report['owned']['captured_anonymous_volume_names'] = list(volumes)
        remaining, container_errors = cleanup_containers(run_id, captured)
        report['cleanup']['remaining_owned_full_ids'] = remaining
        errors.extend(container_errors)
        try:
            report['cleanup']['anonymous_volumes_absent'] = {name: helper.volume_absent(name) for name in volumes}
            if not all(report['cleanup']['anonymous_volumes_absent'].values()):
                errors.append('owned_anonymous_volume_remains')
        except Exception:
            errors.append('owned_volume_absence_check_failed')
        try:
            report['cleanup']['loopback_ports_closed'] = {str(port): helper.closed(port) for port in ports}
            if not all(report['cleanup']['loopback_ports_closed'].values()):
                errors.append('owned_port_still_open')
        except Exception:
            errors.append('owned_port_check_failed')
        try:
            report['source_after'] = helper.source_identity(args.expected_head)
            if report.get('source_before') != report['source_after']:
                errors.append('source_identity_changed')
        except Exception:
            errors.append('source_after_not_clean_exact_head')
        try:
            report['artifact_after_sha256'] = sha_file(ARTIFACT)
            if report['artifact_after_sha256'] != args.expected_jar_sha256:
                errors.append('jar_changed_after_capture')
        except Exception:
            errors.append('jar_after_unreadable')
        try:
            report['helper_after_sha256'] = sha_file(HELPER_PATH)
            if report['helper_after_sha256'] != HELPER_EXPECTED_SHA:
                errors.append('resource_helper_changed_during_capture')
        except Exception:
            errors.append('resource_helper_after_unreadable')
        report['cleanup']['errors'] = errors
        report['exit_code'] = int(bool(report['exit_code'] or errors))
        report['acceptance'] = report['exit_code'] == 0
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        private_bytes(run_dir / 'result.json', (json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode())
        print(json.dumps({'result': str(run_dir / 'result.json'), 'exit_code': report['exit_code']},
                         ensure_ascii=False))
    return report['exit_code']


if __name__ == '__main__':
    sys.exit(main())
