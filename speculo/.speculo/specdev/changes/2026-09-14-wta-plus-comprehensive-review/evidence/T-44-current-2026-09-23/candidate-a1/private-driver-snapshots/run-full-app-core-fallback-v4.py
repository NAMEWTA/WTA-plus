#!/usr/bin/env python3
"""Owned T44 full-app core/Notify gate. Static --preflight; services only with explicit --execute.

This private runner imports the reviewed T40 resource/proof helpers. It never
uses deployed configuration, published ports other than random loopback, or
real suppliers. Raw backend/initializer logs remain 0600 under a 0700 run dir;
result.json contains only allowlisted counts/states, never response bodies.
"""

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
import secrets
import shutil
import socket
import socketserver
import subprocess
import sys
import threading
import time

ROOT = Path('/srv/WTA-plus')
HELPER = ROOT / 'frontend/e2e/run-notice-retraction-real.py'
RUN_ROOT = Path('/tmp/wta-t44/runs')
OWNER = 'T-44-FULL-APP'
POLL_DELAY_MS = 15000
DEFAULT_OSS_CACHE_KEY = 'global:sys_oss:default_config'
CONFIG_CACHE_KEY = 'sys_oss_config'
CACHE_INVALIDATION_CHANNEL = 'namewta:cache:invalidation:v1'
SCENARIOS = ('empty-config', 'bad-nondefault', 'duplicate-default',
             'bad-default', 'invalid-diagnostic', 'minio-offline',
             'redis-publish-denied')
OSS_REASONS = frozenset(('READY', 'CONFIG_MISSING', 'INVALID_ACCESS_POLICY',
    'DOMAIN_REQUIRED', 'DIAGNOSTIC_OBJECT_MISSING', 'DIAGNOSTIC_CONFIG_INVALID',
    'DIAGNOSTIC_UNVERIFIED', 'PROVIDER_MISMATCH', 'DISCOVERY_FAILED', 'STALE'))
SQL_NAMES = ('10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql',
             '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql')
EXTRA_SQL_STAGES = frozenset(('t44_baseline', 't44_empty', 't44_bad', 't44_config',
    't44_control', 't44_notice', 't44_ready', 't44_final', 't44_duplicate',
    't44_oss_id'))
POLL_LINE = re.compile(r'notify outbox drain trigger=(POLL|WAKE) claimed=([0-9]{1,4}) batches=([0-9]{1,4})')
HEALTH_PATH = re.compile(r'/actuator/health/[A-Za-z0-9/_-]+\Z')
DIAGNOSTIC_PATH = re.compile(r'/resource/oss/config/[A-Za-z0-9/_{}-]+\Z')


def load_helper():
    spec = importlib.util.spec_from_file_location('t40_owned_helper', HELPER)
    if spec is None or spec.loader is None:
        raise RuntimeError('Owned helper could not be loaded')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.OWNER = OWNER
    module.MYSQL_STAGES = module.MYSQL_STAGES | EXTRA_SQL_STAGES
    return module


h = load_helper()


def save_private(path, data):
    with os.fdopen(os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
        stream.write(data)


def runner_source_line(error):
    """Expose only a line number in this reviewed private runner."""
    source = Path(__file__).resolve()
    maximum = len(source.read_text().splitlines())
    location = None
    cursor = error.__traceback__
    while cursor is not None:
        if (Path(cursor.tb_frame.f_code.co_filename).resolve() == source
                and type(cursor.tb_lineno) is int and 1 <= cursor.tb_lineno <= maximum):
            location = cursor.tb_lineno
        cursor = cursor.tb_next
    return location


def safe_number(value, maximum=1_000_000):
    if isinstance(value, str) and re.fullmatch(r'[0-9]+', value):
        number = int(value)
        if number <= maximum:
            return number
    raise RuntimeError('Owned numeric fact is invalid')


def numeric_rows(value, length):
    rows = value.split('\t')
    if len(rows) != length:
        raise RuntimeError('Owned fact column count differs')
    return [safe_number(item) for item in rows]


class CountedEndpoint(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = False
    daemon_threads = True

    def __init__(self):
        self.count = 0
        self.lock = threading.Lock()
        super().__init__(('127.0.0.1', 0), CountedHandler)


class CountedHandler(socketserver.BaseRequestHandler):
    def handle(self):
        with self.server.lock:
            self.server.count += 1
        try:
            self.request.settimeout(1)
            # No incoming request bytes, headers, credentials or body are read/retained.
            self.request.sendall(b'HTTP/1.1 503 Service Unavailable\r\n'
                                 b'Connection: close\r\nContent-Length: 0\r\n\r\n')
        except OSError:
            pass


def endpoint_count(server):
    with server.lock:
        return server.count


def redis_conf(app_password, admin_password=None):
    for value in (app_password, admin_password):
        if value is not None and not re.fullmatch(r'[A-Za-z0-9]{40}', value):
            raise RuntimeError('Owned Redis secret shape differs')
    lines = ['bind 0.0.0.0', 'protected-mode yes', 'port 6379', 'dir /tmp',
             'save ""', 'appendonly no']
    if admin_password is None:
        lines.append('requirepass ' + app_password)
    else:
        lines.extend(('user default on >' + app_password + ' ~* &* +@all',
                      'user t44admin on >' + admin_password + ' ~* &* +@all'))
    return ('\n'.join(lines) + '\n').encode('ascii')


def resp_command(port, args, *, username='default', password=None):
    """Small RESP2 client: secrets travel only on an owned loopback socket."""
    if password is None:
        raise RuntimeError('Owned Redis authentication is required')

    def encode(words):
        values = [word.encode('utf-8') if isinstance(word, str) else word for word in words]
        return (b'*' + str(len(values)).encode() + b'\r\n' + b''.join(
            b'$' + str(len(value)).encode() + b'\r\n' + value + b'\r\n' for value in values))

    def read(stream, depth=0):
        if depth > 12:
            raise RuntimeError('Owned Redis response nesting differs')
        prefix = stream.read(1)
        line = stream.readline(4096)
        if not prefix or not line.endswith(b'\r\n'):
            raise RuntimeError('Owned Redis response is incomplete')
        body = line[:-2]
        if prefix == b'-':
            return {'redis_error': body.decode('utf-8', errors='replace')[:512]}
        if prefix == b'+':
            return body.decode('utf-8', errors='replace')
        if prefix == b':':
            return int(body)
        if prefix == b'$':
            size = int(body)
            if size == -1:
                return None
            if size > 65_536 or size < 0:
                raise RuntimeError('Owned Redis bulk reply is oversized')
            data = stream.read(size)
            if len(data) != size or stream.read(2) != b'\r\n':
                raise RuntimeError('Owned Redis bulk reply is incomplete')
            return data.decode('utf-8', errors='replace')
        if prefix == b'*':
            count = int(body)
            if count == -1:
                return None
            if count < 0 or count > 100:
                raise RuntimeError('Owned Redis array reply is oversized')
            return [read(stream, depth + 1) for _ in range(count)]
        raise RuntimeError('Owned Redis reply type differs')

    with socket.create_connection(('127.0.0.1', port), timeout=3) as connection:
        connection.settimeout(4)
        stream = connection.makefile('rb')
        connection.sendall(encode(('AUTH', username, password)))
        if read(stream) != 'OK':
            raise RuntimeError('Owned Redis AUTH failed')
        connection.sendall(encode(args))
        return read(stream)


class OwnedAclEvidenceError(RuntimeError):
    """Only bounded shape/category facts can leave an unexpected ACL log."""

    def __init__(self, kind, facts):
        if kind not in ('invalid_log_shape', 'invalid_denial_count', 'no_expected_denial'):
            raise ValueError('Owned ACL error kind is not allowlisted')
        self.kind = kind
        self.facts = facts
        super().__init__('Owned ACL evidence did not meet its fixed contract')

    def safe_details(self):
        return {'kind': self.kind, **self.facts}


def acl_entries(value):
    """Discard ACL client-info and any arbitrary values before public reporting."""
    facts = {'entry_count': len(value) if isinstance(value, list) and len(value) <= 20 else None,
             'accepted_command_entries': 0, 'accepted_wake_channel_entries': 0,
             'unmatched_entries': 0}
    if not isinstance(value, list):
        raise OwnedAclEvidenceError('invalid_log_shape', facts)
    safe = []
    for entry in value:
        if not isinstance(entry, list) or len(entry) % 2:
            raise OwnedAclEvidenceError('invalid_log_shape', facts)
        fields = dict(zip(entry[::2], entry[1::2]))
        command = (fields.get('username') == 'default'
                   and fields.get('reason') == 'command'
                   and str(fields.get('object', '')).upper() == 'PUBLISH')
        wake_channel = (fields.get('username') == 'default'
                        and fields.get('reason') == 'channel'
                        and fields.get('object') == 'notify:outbox:wake')
        if not (command or wake_channel):
            facts['unmatched_entries'] += 1
            continue
        count = fields.get('count')
        if type(count) is not int or count < 1 or count > 1_000_000:
            raise OwnedAclEvidenceError('invalid_denial_count', facts)
        if command:
            facts['accepted_command_entries'] += 1
        else:
            facts['accepted_wake_channel_entries'] += 1
        safe.append({'reason': 'command' if command else 'channel',
                     'object': 'PUBLISH' if command else 'notify:outbox:wake',
                     'username': 'default', 'count': count})
    if not safe:
        raise OwnedAclEvidenceError('no_expected_denial', facts)
    return safe, facts


def http_json(port, path, *, method='GET', body=None, token=None, basic=None,
              limit=1_000_000, timeout=10):
    if (not path.startswith('/') or path.startswith('//') or '?' in path or '#' in path
            or method not in ('GET', 'POST') or (method == 'GET' and body is not None)):
        raise RuntimeError('Owned HTTP request shape differs')
    headers = {'clientid': h.ADMIN_CLIENT_ID, 'Content-Type': 'application/json'}
    if token is not None:
        headers['Authorization'] = 'Bearer ' + token
    if basic is not None:
        user, password = basic
        headers['Authorization'] = 'Basic ' + base64.b64encode((user + ':' + password).encode()).decode()
    payload = None if body is None else json.dumps(body, ensure_ascii=False).encode()
    connection = http.client.HTTPConnection('127.0.0.1', port, timeout=timeout)
    try:
        connection.request(method, path, body=payload, headers=headers)
        response = connection.getresponse()
        raw = response.read(limit + 1)
        if len(raw) > limit:
            raise RuntimeError('Owned HTTP response exceeded fixed bound')
        value = json.loads(raw)
        if type(value) is not dict:
            raise RuntimeError('Owned HTTP response is not an object')
        return response.status, value, raw
    finally:
        connection.close()


def health(port, path, basic, expected_http, expected_state):
    code, data, _ = http_json(port, path, basic=basic, limit=65_536)
    state = data.get('status')
    if code != expected_http or state != expected_state:
        raise RuntimeError('Owned health group differs from explicit expected contract')
    component = data.get('components', {}).get('ossStorageReadiness', {})
    details = component.get('details', data.get('details', {})) if isinstance(component, dict) else {}
    configs = details.get('configs', {}) if isinstance(details, dict) else {}
    reasons = sorted({entry.get('reason') for entry in configs.values()
                      if isinstance(entry, dict) and entry.get('reason') in OSS_REASONS}) \
        if isinstance(configs, dict) else []
    return {'path': path, 'http_status': code, 'status': state, 'reason_codes': reasons}


def wait_core_readiness(port, path, basic, process, seconds=120):
    """Allow only a bounded Boot startup transition, never auth/shape failure."""
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError('Owned backend exited before core readiness')
        try:
            code, body, _ = http_json(port, path, basic=basic, limit=65_536)
        except (OSError, http.client.HTTPException):
            time.sleep(.25)
            continue
        state = body.get('status')
        if code == 200 and state == 'UP':
            return
        if code == 503 and state in ('DOWN', 'OUT_OF_SERVICE'):
            time.sleep(.25)
            continue
        raise RuntimeError('Owned core readiness returned a non-startup state or authorization error')
    raise RuntimeError('Owned core readiness did not reach accepting traffic in time')


def poll_events(log_path):
    text = log_path.read_text(encoding='utf-8', errors='replace')
    return [(match.group(1), int(match.group(2)), int(match.group(3)))
            for match in POLL_LINE.finditer(text)]


def publisher_failed(log_path, outbox_id):
    text = log_path.read_text(encoding='utf-8', errors='replace')
    return bool(re.search(r'notify outbox cross-process wake publish failed, outboxId='
                          + re.escape(str(outbox_id)) + r', reason=[A-Za-z]{1,40}', text))


def wait_condition(check, seconds, label):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if check():
            return
        time.sleep(.15)
    raise RuntimeError('Owned ' + label + ' timed out')


def owned_sql(cid, sql, stage):
    return h.mysql(cid, sql, stage=stage)


def sql_count(cid, sql, stage):
    return safe_number(owned_sql(cid, sql, stage))


def startup_matrix_mutation(cid, scenario):
    if scenario == 'empty-config':
        owned_sql(cid, 'DELETE FROM sys_oss_config;', 't44_empty')
        if sql_count(cid, 'SELECT COUNT(*) FROM sys_oss_config;', 't44_config') != 0:
            raise RuntimeError('Owned empty OSS configuration differs')
    elif scenario == 'bad-nondefault':
        changed = owned_sql(cid, "UPDATE sys_oss_config SET access_policy='9' "
                            "WHERE config_key='image' AND status='N'; SELECT ROW_COUNT();", 't44_bad')
        if changed != '1':
            raise RuntimeError('Owned bad nondefault row was not uniquely prepared')
    elif scenario == 'duplicate-default':
        changed = owned_sql(cid, "UPDATE sys_oss_config SET status='Y',access_policy='0' "
                            "WHERE config_key='image' AND status='N'; SELECT ROW_COUNT();", 't44_bad')
        if changed != '1':
            raise RuntimeError('Owned second PRIVATE default was not uniquely prepared')
    elif scenario == 'bad-default':
        changed = owned_sql(cid, "UPDATE sys_oss_config SET access_policy='9' "
                            "WHERE config_key='minio' AND status='Y'; SELECT ROW_COUNT();", 't44_bad')
        if changed != '1':
            raise RuntimeError('Owned invalid default was not uniquely prepared')


def seed_stale_cache(redis_port, password):
    """Seed only owned Redis physical keys; app startup must discard both."""
    app = lambda args: resp_command(redis_port, args, password=password)
    if app(('SET', DEFAULT_OSS_CACHE_KEY, 'stale-owned-default')) != 'OK':
        raise RuntimeError('Owned stale default key was not prepared')
    if app(('HSET', CONFIG_CACHE_KEY, 'stale-owned-config', '{}')) != 1:
        raise RuntimeError('Owned stale OSS map was not prepared')
    if (app(('EXISTS', DEFAULT_OSS_CACHE_KEY)) != 1
            or app(('EXISTS', CONFIG_CACHE_KEY)) != 1):
        raise RuntimeError('Owned stale OSS cache positive control differs')


def verify_unusable_default(port, token, redis_port, password, cid, counted_endpoint,
                            scenario, baseline_config_rows):
    app = lambda args: resp_command(redis_port, args, password=password)
    if (app(('EXISTS', DEFAULT_OSS_CACHE_KEY)) != 0
            or app(('HEXISTS', CONFIG_CACHE_KEY, 'stale-owned-config')) != 0):
        raise RuntimeError('Owned stale OSS cache survived DB-authoritative startup')
    if scenario == 'empty-config':
        expected = [0, 0, 0]
    elif scenario == 'duplicate-default':
        expected = [baseline_config_rows, 2, 0]
    elif scenario == 'bad-default':
        expected = [baseline_config_rows, 1, 1]
    else:
        raise RuntimeError('Owned unusable-default scenario is invalid')
    counts = numeric_rows(owned_sql(cid,
        'SELECT COUNT(*),'
        "COALESCE(SUM(status='Y'),0),"
        "COALESCE(SUM(status='Y' AND access_policy='9'),0) FROM sys_oss_config;",
        't44_config'), 3)
    if counts != expected:
        raise RuntimeError('Owned unusable-default SQL configuration differs')
    if scenario == 'empty-config' and app(('EXISTS', CONFIG_CACHE_KEY)) != 0:
        raise RuntimeError('Owned empty OSS config map was repopulated')
    if scenario != 'empty-config' and app(('EXISTS', CONFIG_CACHE_KEY)) != 1:
        raise RuntimeError('Owned valid nondefault OSS map was not rebuilt')
    request = {'policy': 'general', 'fileName': 't44-empty.txt', 'fileSize': 1,
               'contentType': 'text/plain', 'fingerprint': 't44-owned-empty-config'}
    status, reply, _ = http_json(port, '/resource/oss/uploads', method='POST',
                                 token=token, body=request)
    error = reply.get('data')
    if (status != 200 or reply.get('code') == 200 or not isinstance(error, dict)
            or error.get('error') != 'STORAGE_NOT_SERVING'):
        raise RuntimeError('Owned unusable-default upload did not fail locally')
    if counted_endpoint is not None and endpoint_count(counted_endpoint) != 0:
        raise RuntimeError('Owned unusable-default upload contacted optional OSS endpoint')
    return {'stale_default_key_cleared': True, 'stale_config_map_cleared': True,
            'upload_http': status, 'upload_error': 'STORAGE_NOT_SERVING',
            'remote_oss_connections': 0, 'config_rows': counts[0],
            'default_rows': counts[1], 'invalid_default_rows': counts[2]}


def init_schema(cid, run_dir, root_password, app_user, app_password,
                minio_user, minio_password, endpoint_port):
    env_path = run_dir / 'init.env'
    data = ('MYSQL_DATABASE=wta-plus\nMYSQL_ROOT_PASSWORD=' + root_password
            + '\nMYSQL_APP_USER=' + app_user
            + '\nMYSQL_APP_PASSWORD=' + app_password + '\nMINIO_ROOT_USER=' + minio_user
            + '\nMINIO_ROOT_PASSWORD=' + minio_password
            + '\nMINIO_ENDPOINT=127.0.0.1:' + str(endpoint_port)
            + '\nMINIO_BUCKET=t44-' + run_dir.name + '\n')
    save_private(env_path, data.encode())
    argv = ('bash', str(h.INIT_SCRIPT), '--container', cid, '--env-file', str(env_path),
            '--sql-dir', str(h.SQL_ROOT))
    result = h.run(argv, env=h.safe_env(DOCKER_HOST='unix:///var/run/docker.sock',
                        DOCKER_CONFIG=str(run_dir / 'docker-config')), timeout=900)
    save_private(run_dir / 'init.raw.log', result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError('Owned six-SQL initialization failed')
    tables = sql_count(cid, 'SELECT COUNT(*) FROM information_schema.TABLES '
                       'WHERE TABLE_SCHEMA=DATABASE();', 't44_baseline')
    outboxes = sql_count(cid, 'SELECT COUNT(*) FROM notify_outbox;', 't44_baseline')
    external = sql_count(cid, "SELECT COUNT(*) FROM notify_delivery WHERE channel IN ('SMS','MAIL');",
                         't44_baseline')
    accounts = sql_count(cid, "SELECT COUNT(*) FROM notify_channel_account "
                         "WHERE channel IN ('SMS','MAIL') AND enabled='Y';", 't44_baseline')
    if (tables, outboxes, external, accounts) != (103, 0, 0, 0):
        raise RuntimeError('Owned fresh schema or supplier isolation differs')
    # The initializer rewrites only minio/image. Replace *every* seeded OSS
    # destination/identity in this fresh database before the full app starts.
    endpoint = '127.0.0.1:' + str(endpoint_port)
    owned_sql(cid, 'UPDATE sys_oss_config SET access_key=' + h.hexsql(minio_user)
              + ',secret_key=' + h.hexsql(minio_password)
              + ',bucket_name=' + h.hexsql('t44-' + run_dir.name)
              + ',endpoint=' + h.hexsql(endpoint)
              + ",domain_url='',is_https='N';", 't44_config')
    config_rows = numeric_rows(owned_sql(cid, 'SELECT COUNT(*),'
        "COALESCE(SUM(endpoint LIKE '127.0.0.1:%'),0),"
        "COALESCE(SUM(domain_url=''),0),COUNT(DISTINCT endpoint),"
        'COUNT(DISTINCT access_key),COUNT(DISTINCT secret_key) FROM sys_oss_config;',
        't44_config'), 6)
    if config_rows[0] < 2 or config_rows != [config_rows[0], config_rows[0],
                                             config_rows[0], 1, 1, 1]:
        raise RuntimeError('Owned OSS rows still contain an outside endpoint or identity')
    return {'tables': tables, 'outboxes': outboxes,
            'oss_config_rows': config_rows[0],
            'external_deliveries': external, 'enabled_external_accounts': accounts}


def overlay_config(mysql_port, redis_port, app_user, app_password, redis_password,
                   backend_port, endpoint_port, admin_password, run_dir, scenario,
                   invalid_key, invalid_value, capture_openapi):
    value = h.isolated_config(mysql_port, redis_port, backend_port, 1,
                              app_user, app_password, redis_password, run_dir)
    value['spring.data.redis.username'] = 'default'
    # KeyPrefixHandler maps topic names too. Pin the owned namespace so the
    # observed PUBSUB channel is the same one used by the production publisher.
    value['redisson.key-prefix'] = ''
    value['spring.boot.admin.client.password'] = admin_password
    value['notify.outbox.poll-delay-ms'] = POLL_DELAY_MS
    value['snail-job.enabled'] = False
    value['message.enabled'] = False
    value['logging.level.org.namewta.notify.adapter.worker.NotifyOutboxWorker'] = 'INFO'
    value['springdoc.api-docs.enabled'] = bool(capture_openapi)
    value['openapi.enabled'] = bool(capture_openapi)
    if scenario == 'invalid-diagnostic':
        value[invalid_key] = invalid_value
    if any(not str(value[key]).startswith('127.0.0.1') for key in
           ('server.address', 'spring.data.redis.host')):
        raise RuntimeError('Owned overlay is not loopback-bound')
    if not str(value['spring.datasource.dynamic.datasource.master.url']).startswith('jdbc:mysql://127.0.0.1:'):
        raise RuntimeError('Owned MySQL URL is not loopback-bound')
    # The initializer already bound all sys_oss_config rows to endpoint_port.
    if endpoint_port < 1:
        raise RuntimeError('Owned OSS endpoint is invalid')
    return value


def core_http_checks(port, basic, core_path, liveness_path, oss_path, oss_http, oss_state,
                     control_user, control_password):
    core = health(port, core_path, basic, 200, 'UP')
    liveness = health(port, liveness_path, basic, 200, 'UP')
    oss = health(port, oss_path, basic, oss_http, oss_state)
    token = h.control_login(port, control_user, control_password, 'login_control')
    status, menu, _ = http_json(port, '/system/menu/getRouters', token=token)
    if (status != 200 or menu.get('code') != 200 or type(menu.get('data')) is not list
            or not menu['data']):
        raise RuntimeError('Owned real login/menu path differs')
    return token, {'core': core, 'liveness': liveness, 'oss': oss, 'login_http': 200,
                   'menu_http': status, 'menu_count': len(menu['data'])}


def optional_diagnostic(port, cid, token, template, method, expected_http, expected_code,
                        counted_endpoint, scenario, basic, oss_path, oss_http, oss_state):
    if template is None:
        return {'requested': False}
    config_id = owned_sql(cid, "SELECT oss_config_id FROM sys_oss_config WHERE status='Y';",
                          't44_oss_id').splitlines()
    if len(config_id) != 1 or not re.fullmatch(r'[1-9][0-9]*', config_id[0]):
        raise RuntimeError('Owned single configuration ID is unavailable')
    path = template.replace('{config_id}', config_id[0])
    before = endpoint_count(counted_endpoint) if counted_endpoint is not None else None
    started = time.monotonic()
    status, data, _ = http_json(port, path, method=method,
                                body={} if method == 'POST' else None, token=token,
                                timeout=25)
    elapsed_ms = int((time.monotonic() - started) * 1000)
    if elapsed_ms > 20_000:
        raise RuntimeError('Owned explicit OSS diagnosis exceeded its bounded network budget')
    if status != expected_http or data.get('code') != expected_code:
        raise RuntimeError('Owned administrator diagnostic HTTP differs')
    result = {'requested': True, 'method': method, 'path_template': template,
            'http_status': status, 'business_code': data.get('code'),
            'elapsed_ms': elapsed_ms,
            'oss_connections_before': before,
            'oss_connections_after': endpoint_count(counted_endpoint)
            if counted_endpoint is not None else None}
    if scenario == 'invalid-diagnostic':
        value = data.get('data')
        if (not isinstance(value, dict) or set(value) != {'status', 'reason', 'checkedAt'}
                or value.get('status') != 'NOT_SERVING'
                or value.get('reason') != 'DIAGNOSTIC_CONFIG_INVALID'
                or not isinstance(value.get('checkedAt'), str)
                or not value['checkedAt']):
            raise RuntimeError('Owned malformed duration diagnostic projection differs')
        after_health = health(port, oss_path, basic, oss_http, oss_state)
        if 'DIAGNOSTIC_CONFIG_INVALID' not in after_health['reason_codes']:
            raise RuntimeError('Owned malformed duration health lacks fixed reason')
        if counted_endpoint is not None and endpoint_count(counted_endpoint) != before:
            raise RuntimeError('Owned malformed duration contacted remote OSS')
        result['fixed_reason_verified_after_explicit_diagnosis'] = True
        result['remote_oss_connections_delta'] = 0
    return result


def optional_openapi(port, run_dir, requested):
    if not requested:
        return {'requested': False}
    status, data, raw = http_json(port, '/v3/api-docs', limit=2_000_000)
    if status != 200 or not isinstance(data.get('paths'), dict) or not isinstance(data.get('components'), dict):
        raise RuntimeError('Owned live OpenAPI response differs')
    schemas = data['components'].get('schemas')
    if not isinstance(schemas, dict):
        raise RuntimeError('Owned live OpenAPI schema map differs')
    save_private(run_dir / 'openapi-source.raw.json', raw)
    return {'requested': True, 'http_status': status, 'source_sha256': hashlib.sha256(raw).hexdigest(),
            'path_count': len(data['paths']), 'schema_count': len(schemas),
            'raw_path': 'openapi-source.raw.json', 'full_contract_validated': False}


def redis_fallback(cid, redis_port, app_password, admin_password, backend_port,
                   token, control_password, a_user, log_path, run_id):
    if admin_password is None:
        raise RuntimeError('Owned ACL management identity is missing')
    admin = lambda args: resp_command(redis_port, args, username='t44admin', password=admin_password)
    app = lambda args: resp_command(redis_port, args, password=app_password)
    if (admin(('ACL', 'DRYRUN', 'default', 'PUBLISH', 'notify:outbox:wake', 't44-probe')) != 'OK'
            or admin(('ACL', 'DRYRUN', 'default', 'SUBSCRIBE', 'notify:outbox:wake')) != 'OK'
            or app(('PING',)) != 'PONG'):
        raise RuntimeError('Owned Redis initial publisher/subscriber positive control differs')
    key = 't44-owned:' + run_id
    if app(('SET', key, '1')) != 'OK' or app(('GET', key)) != '1':
        raise RuntimeError('Owned app Redis read/write differs')
    subscribers = admin(('PUBSUB', 'NUMSUB', 'notify:outbox:wake'))
    if (not isinstance(subscribers, list) or len(subscribers) != 2
            or subscribers[0] != 'notify:outbox:wake'
            or type(subscribers[1]) is not int or subscribers[1] < 1):
        raise RuntimeError('Owned app did not retain its wake subscriber')
    title = 'T44 ' + run_id + ' fallback notice'
    content = 'T44 ' + run_id + ' inbox proof'
    status, save, _ = http_json(backend_port, '/notify/notice/save', method='POST', token=token,
        body={'noticeTitle': title, 'noticeType': '1', 'noticeContent': content,
              'recipientType': 'USER', 'recipientIds': [a_user], 'userTypeIds': [],
              'channels': ['IN_APP']})
    if status != 200 or save.get('code') != 200:
        raise RuntimeError('Owned real Notice draft save failed')
    notice_ids = owned_sql(cid, 'SELECT notice_id FROM notify_notice WHERE notice_title='
                           + h.compared_hexsql(title) + ';', 't44_notice').splitlines()
    if len(notice_ids) != 1 or not re.fullmatch(r'[1-9][0-9]*', notice_ids[0]):
        raise RuntimeError('Owned real Notice draft identity differs')
    notice_id = int(notice_ids[0])
    # Start/health/login/menu/draft-save all ran with normal Redis permissions.
    # Redis 7+ selector keeps the existing cache-invalidation channel usable
    # while denying the independent Notify wake channel. A global -publish
    # would break login/menu cache writes before the Notice can be published.
    if admin(('ACL', 'SETUSER', 'default', '-publish',
              '(+publish &' + CACHE_INVALIDATION_CHANNEL + ')')) != 'OK':
        raise RuntimeError('Owned Redis PUBLISH fault could not be armed')
    denied = admin(('ACL', 'DRYRUN', 'default', 'PUBLISH', 'notify:outbox:wake', 't44-probe'))
    cache_allowed = admin(('ACL', 'DRYRUN', 'default', 'PUBLISH',
                           CACHE_INVALIDATION_CHANNEL, 't44-probe'))
    allowed = admin(('ACL', 'DRYRUN', 'default', 'SUBSCRIBE', 'notify:outbox:wake'))
    if denied == 'OK' or cache_allowed != 'OK' or allowed != 'OK' or app(('PING',)) != 'PONG':
        raise RuntimeError('Owned Redis ACL did not isolate Notify PUBLISH')
    if app(('GET', key)) != '1':
        raise RuntimeError('Owned Redis ordinary cache read failed after fault injection')
    if app(('SET', key, '2')) != 'OK' or app(('GET', key)) != '2':
        raise RuntimeError('Owned Redis ordinary cache write failed after fault injection')
    subscribers_after = admin(('PUBSUB', 'NUMSUB', 'notify:outbox:wake'))
    if (not isinstance(subscribers_after, list) or len(subscribers_after) != 2
            or subscribers_after[0] != 'notify:outbox:wake'
            or type(subscribers_after[1]) is not int or subscribers_after[1] < 1):
        raise RuntimeError('Owned wake subscriber disappeared after ACL fault')
    # Re-enter the real auth/menu path after restricting PUBLISH. A direct
    # redis-cli SET/GET alone cannot establish that the app still serves users.
    token = h.control_login(backend_port, 'WTA', control_password, 'login_control')
    menu_status, menu, _ = http_json(backend_port, '/system/menu/getRouters', token=token)
    if (menu_status != 200 or menu.get('code') != 200
            or not isinstance(menu.get('data'), list) or not menu['data']):
        raise RuntimeError('Owned app authentication/menu regressed after PUBLISH denial')
    if admin(('ACL', 'LOG', 'RESET')) != 'OK':
        raise RuntimeError('Owned ACL log reset failed')
    before = len(poll_events(log_path))
    wait_condition(lambda: len(poll_events(log_path)) > before and
                   poll_events(log_path)[-1][0] == 'POLL' and poll_events(log_path)[-1][1] == 0,
                   30, 'initial empty scheduled poll')
    status, published, _ = http_json(backend_port, f'/notify/notice/{notice_id}/publish',
                                     method='POST', body={}, token=token)
    if status != 200 or published.get('code') != 200:
        raise RuntimeError('Owned real Notice publish failed')
    rows = owned_sql(cid, 'SELECT i.intent_id,o.outbox_id FROM notify_intent i '
        'JOIN notify_outbox o ON o.intent_id=i.intent_id WHERE i.app_id=\'notify\' '
        'AND i.idempotency_key=' + h.compared_hexsql(f'notice-published:{notice_id}:1') + ';',
        't44_notice').splitlines()
    if len(rows) != 1:
        raise RuntimeError('Owned real Notice did not create one Outbox')
    ids = rows[0].split('\t')
    if len(ids) != 2:
        raise RuntimeError('Owned real Notice ID fields differ')
    intent_id, outbox_id = (safe_number(item, 9_223_372_036_854_775_807) for item in ids)
    ready = numeric_rows(owned_sql(cid, 'SELECT '
        f'(SELECT COUNT(*) FROM notify_outbox WHERE intent_id={intent_id} AND status=\'READY\'),'
        f'(SELECT COUNT(*) FROM notify_delivery WHERE intent_id={intent_id} AND channel=\'IN_APP\' AND status=\'PENDING\'),'
        f'(SELECT COUNT(*) FROM notify_attempt WHERE intent_id={intent_id}),'
        f'(SELECT COUNT(*) FROM notify_message WHERE message_id={intent_id}),'
        f'(SELECT COUNT(*) FROM notify_message_recipient WHERE message_id={intent_id});',
        't44_ready'), 5)
    if ready != [1, 1, 0, 0, 0]:
        raise RuntimeError('Owned READY pre-poll state was not observed')
    if not publisher_failed(log_path, outbox_id):
        raise RuntimeError('Owned wake publisher denial was not observed for the same Outbox')
    acl, acl_facts = acl_entries(admin(('ACL', 'LOG', '20')))
    if any(trigger == 'WAKE' and claimed > 0 for trigger, claimed, _ in poll_events(log_path)):
        raise RuntimeError('Owned task was processed by WAKE rather than scheduled POLL')

    def delivered():
        counts = numeric_rows(owned_sql(cid, 'SELECT '
            f'(SELECT COUNT(*) FROM notify_outbox WHERE intent_id={intent_id} AND status=\'DONE\'),'
            f'(SELECT COUNT(*) FROM notify_delivery WHERE intent_id={intent_id} AND channel=\'IN_APP\' AND status=\'DELIVERED\'),'
            f'(SELECT COUNT(*) FROM notify_attempt WHERE intent_id={intent_id}),'
            f'(SELECT COUNT(*) FROM notify_message WHERE message_id={intent_id}),'
            f'(SELECT COUNT(*) FROM notify_message_recipient WHERE message_id={intent_id} AND user_id={a_user});',
            't44_final'), 5)
        return counts == [1, 1, 1, 1, 1]
    wait_condition(delivered, 40, 'scheduled POLL delivery')
    events = poll_events(log_path)
    if not any(trigger == 'POLL' and claimed == 1 for trigger, claimed, _ in events):
        raise RuntimeError('Owned successful scheduled POLL source was not observed')
    if any(trigger == 'WAKE' and claimed > 0 for trigger, claimed, _ in events):
        raise RuntimeError('Owned WAKE unexpectedly claimed a task')
    existing = sql_count(cid, f'SELECT COUNT(*) FROM notify_message_recipient '
                           f'WHERE message_id={intent_id} AND user_id<>{a_user};', 't44_final')
    if existing:
        raise RuntimeError('Owned IN_APP relation escaped the exact recipient')
    delivered_polls = sum(1 for trigger, _, _ in events if trigger == 'POLL')
    wait_condition(lambda: sum(1 for trigger, _, _ in poll_events(log_path)
                               if trigger == 'POLL') >= delivered_polls + 2,
                   45, 'two subsequent scheduled polls')
    duplicate = numeric_rows(owned_sql(cid, 'SELECT '
        f'(SELECT COUNT(*) FROM notify_attempt WHERE intent_id={intent_id}),'
        f'(SELECT COUNT(*) FROM notify_message WHERE message_id={intent_id}),'
        f'(SELECT COUNT(*) FROM notify_message_recipient WHERE message_id={intent_id});',
        't44_duplicate'), 3)
    if duplicate != [1, 1, 1]:
        raise RuntimeError('Owned repeat scheduled polls duplicated delivery')
    return {'notice_published_http': status, 'ready_before_poll': True,
            'wake_publisher_failed_for_owned_outbox': True,
            'acl_publish_denials': sum(row['count'] for row in acl),
            'acl_classification': acl_facts,
            'subscribers_before_submit': subscribers[1],
            'subscribers_after_fault': subscribers_after[1],
            'cache_publish_allowed_after_fault': True,
            'login_menu_after_acl': True,
            'poll_claimed_one': True, 'wake_claimed_positive': False,
            'final_outbox_done': 1, 'final_delivery_in_app_delivered': 1,
            'final_attempt': duplicate[0], 'final_message': duplicate[1],
            'final_recipient_relation': duplicate[2], 'post_delivery_polls': 2}


def preflight():
    paths = [HELPER, h.INIT_SCRIPT, h.ARTIFACT] + [h.SQL_ROOT / name for name in SQL_NAMES]
    result = {'mode': 'static-preflight-only', 'services_started': False,
              'files_present': {str(path): path.is_file() for path in paths},
              'scenarios': SCENARIOS, 'images_expected': ('mysql:8.4.9', 'redis:8.6.3')}
    print(json.dumps(result, ensure_ascii=False))
    return int(not all(result['files_present'].values()))


def argument_parser():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--preflight', action='store_true')
    parser.add_argument('--execute', action='store_true')
    parser.add_argument('--scenario', choices=SCENARIOS)
    parser.add_argument('--expected-head')
    parser.add_argument('--expected-jar-sha256')
    parser.add_argument('--package-proof')
    parser.add_argument('--core-health-path')
    parser.add_argument('--liveness-health-path')
    parser.add_argument('--oss-health-path')
    parser.add_argument('--oss-health-http', type=int)
    parser.add_argument('--oss-health-state', choices=('UP', 'DOWN', 'UNKNOWN'))
    parser.add_argument('--invalid-diagnostic-key')
    parser.add_argument('--invalid-diagnostic-value')
    parser.add_argument('--diagnostic-path-template')
    parser.add_argument('--diagnostic-method', choices=('GET', 'POST'))
    parser.add_argument('--diagnostic-expected-http', type=int)
    parser.add_argument('--diagnostic-expected-code', type=int)
    parser.add_argument('--capture-openapi', action='store_true')
    return parser


def validate_args(parser, args):
    if args.preflight:
        if any(value for key, value in vars(args).items() if key != 'preflight'):
            parser.error('--preflight cannot combine with execution arguments')
        return
    required = (args.execute, args.scenario, args.expected_head, args.expected_jar_sha256,
                args.package_proof, args.core_health_path, args.liveness_health_path,
                args.oss_health_path,
                args.oss_health_http, args.oss_health_state)
    if (not all(required) or not re.fullmatch(r'[0-9a-f]{40}', args.expected_head or '')
            or not re.fullmatch(r'[0-9a-f]{64}', args.expected_jar_sha256 or '')
            or args.core_health_path != '/actuator/health/readiness'
            or args.liveness_health_path != '/actuator/health/liveness'
            or args.oss_health_path != '/actuator/health/ossdiagnostics'
            or args.oss_health_http not in (200, 503)):
        parser.error('execute needs exact clean source/JAR/proof and explicit distinct health groups')
    if args.scenario == 'invalid-diagnostic':
        if (args.invalid_diagnostic_key != 'oss.readiness.diagnostic-timeout'
                or args.invalid_diagnostic_value != 'invalid-owned-duration'):
            parser.error('invalid-diagnostic needs the explicit owned malformed property')
    elif args.invalid_diagnostic_key or args.invalid_diagnostic_value:
        parser.error('invalid diagnostic inputs belong only to that scenario')
    optional = (args.diagnostic_path_template, args.diagnostic_method,
                args.diagnostic_expected_http, args.diagnostic_expected_code)
    if any(item is not None for item in optional):
        if (not all(item is not None for item in optional)
                or args.scenario in ('empty-config', 'duplicate-default', 'bad-default',
                                     'redis-publish-denied')
                or args.diagnostic_path_template != '/resource/oss/config/diagnose/{config_id}'
                or args.diagnostic_method != 'POST'
                or args.diagnostic_expected_http not in (200, 400, 403, 404, 503)
                or type(args.diagnostic_expected_code) is not int):
            parser.error('diagnostic hook needs exact local template/method/expected statuses')
    if args.scenario == 'invalid-diagnostic' and (optional !=
            ('/resource/oss/config/diagnose/{config_id}', 'POST', 200, 200)):
        parser.error('invalid-diagnostic requires explicit fixed administrator diagnosis')


def execute(args):
    os.umask(0o077)
    run_id = secrets.token_hex(8)
    RUN_ROOT.mkdir(parents=True, exist_ok=True)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(mode=0o700)
    report = {'gate': 'T44 owned full-app core and scheduled Notify fallback',
              'scenario': args.scenario, 'run_id': run_id, 'acceptance': False,
              'exit_code': 1, 'source_before': None, 'source_after': None,
              'started_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'owned': {}, 'observed': {}, 'cleanup': {}}
    captured, volumes, ports, processes, errors = [], [], [], [], []
    count_proxy = None
    proxy_thread = None
    closed_guard = None
    phase = 'source_proof'
    mysql_id = None
    root_password, app_password, redis_password, minio_password = (
        h.random_password() for _ in range(4))
    redis_admin_password = h.random_password() if args.scenario == 'redis-publish-denied' else None
    admin_password = h.random_password()
    control_password = h.random_login_password()
    app_user = 't44_' + run_id
    minio_user = 't44minio' + run_id
    secret_paths = [run_dir / name for name in ('mysql-container.env', 'redis.conf',
                                                'init.env', 'owned.yml')]
    backend_log = run_dir / 'backend.raw.log'
    try:
        source = h.source_identity(args.expected_head)
        report['source_before'] = source
        if not h.ARTIFACT.is_file() or h.sha_file(h.ARTIFACT) != args.expected_jar_sha256:
            raise RuntimeError('Expected full JAR differs')
        proof_path = Path(args.package_proof)
        proof = json.loads(proof_path.read_text())
        h.validate_package_proof(proof, args.expected_head, source['tree'],
                                 args.expected_jar_sha256, h.ARTIFACT.stat().st_size)
        report['package_proof_sha256'] = h.sha_file(proof_path)
        report['jar_sha256_before'] = args.expected_jar_sha256
        report['helper_sha256'] = h.sha_file(HELPER)
        report['full_bundle_modules'] = h.validate_full_jar(h.ARTIFACT)
        if not h.INIT_SCRIPT.is_file() or not all((h.SQL_ROOT / name).is_file() for name in SQL_NAMES):
            raise RuntimeError('Owned six-SQL inputs are missing')
        report['sql_baselines'] = {name: h.sha_file(h.SQL_ROOT / name) for name in SQL_NAMES}

        if args.scenario == 'minio-offline':
            closed_guard = socket.socket()
            closed_guard.bind(('127.0.0.1', 0))
            endpoint_port = closed_guard.getsockname()[1]
            if not h.closed(endpoint_port):
                raise RuntimeError('Owned offline MinIO port is reachable')
        else:
            count_proxy = CountedEndpoint()
            endpoint_port = count_proxy.server_address[1]
            proxy_thread = threading.Thread(target=count_proxy.serve_forever, daemon=True)
            proxy_thread.start()
        ports.append(endpoint_port)
        report['owned']['oss_endpoint_mode'] = 'closed_loopback' if closed_guard else 'counted_loopback_503'

        phase = 'mysql_redis_setup'
        mysql_env = run_dir / 'mysql-container.env'
        save_private(mysql_env, ('MYSQL_ROOT_PASSWORD=' + root_password + '\n').encode())
        mysql_id = h.full_id(h.docker('run', '--pull=never', '-d', '--name', 't44-mysql-' + run_id,
              '--label', 'namewta.test.owner=' + OWNER, '--label', 'namewta.test.run=' + run_id,
              '-p', '127.0.0.1::3306', '--env-file', str(mysql_env), 'mysql:8.4.9',
              '--character-set-server=utf8mb4', '--collation-server=utf8mb4_general_ci',
              '--log-bin-trust-function-creators=1'))
        captured.append(mysql_id)
        h.assert_owned(mysql_id, run_id)
        volumes.extend(h.volume_names(mysql_id))
        h.wait_mysql(mysql_id)
        mysql_port = h.mapped_port(mysql_id, '3306')
        ports.append(mysql_port)
        report['owned']['mysql_full_id'] = mysql_id

        redis_path = run_dir / 'redis.conf'
        save_private(redis_path, redis_conf(redis_password, redis_admin_password))
        os.chown(redis_path, h.REDIS_CONFIG_USER, h.REDIS_CONFIG_USER)
        redis_id = h.full_id(h.docker('run', '--pull=never', '-d', '--name', 't44-redis-' + run_id,
              '--label', 'namewta.test.owner=' + OWNER, '--label', 'namewta.test.run=' + run_id,
              '-p', '127.0.0.1::6379', '--user', str(h.REDIS_CONFIG_USER) + ':' + str(h.REDIS_CONFIG_USER),
              '--mount', 'type=bind,source=' + str(redis_path) + ',target=/etc/redis/redis.conf,readonly',
              'redis:8.6.3', 'redis-server', '/etc/redis/redis.conf'))
        captured.append(redis_id)
        h.assert_owned(redis_id, run_id)
        volumes.extend(h.volume_names(redis_id))
        redis_port = h.mapped_port(redis_id, '6379')
        ports.append(redis_port)
        def redis_ready():
            try:
                return resp_command(redis_port, ('PING',), password=redis_password) == 'PONG'
            except (OSError, TimeoutError):
                return False
        wait_condition(redis_ready,
                       45, 'authenticated Redis readiness')
        report['owned']['redis_full_id'] = redis_id

        phase = 'schema_init'
        (run_dir / 'docker-config').mkdir(mode=0o700)
        report['owned']['baseline'] = init_schema(mysql_id, run_dir, root_password, app_user,
                                                  app_password, minio_user, minio_password,
                                                  endpoint_port)
        startup_matrix_mutation(mysql_id, args.scenario)
        baseline_config_rows = report['owned']['baseline']['oss_config_rows']
        post_mutation_config_rows = sql_count(mysql_id,
            'SELECT COUNT(*) FROM sys_oss_config;', 't44_config')
        if post_mutation_config_rows != (0 if args.scenario == 'empty-config'
                                        else baseline_config_rows):
            raise RuntimeError('Owned scenario unexpectedly changed OSS row count')
        if args.scenario in ('empty-config', 'duplicate-default', 'bad-default'):
            seed_stale_cache(redis_port, redis_password)
        control_user = h.owned_user_id(mysql_id, 'WTA', 'lookup_control')
        h.rotate_owned_login(mysql_id, control_user, control_password)
        a_user = h.owned_user_id(mysql_id, 'test', 'lookup_a')

        phase = 'backend_start'
        backend_port = h.free_port()
        ports.append(backend_port)
        for name in ('tmp', 'logs', 'multipart'):
            (run_dir / name).mkdir(mode=0o700)
        overlay = overlay_config(mysql_port, redis_port, app_user, app_password,
                                 redis_password, backend_port, endpoint_port,
                                 admin_password, run_dir, args.scenario,
                                 args.invalid_diagnostic_key, args.invalid_diagnostic_value,
                                 args.capture_openapi)
        overlay_path = run_dir / 'owned.yml'
        save_private(overlay_path, (json.dumps(overlay, ensure_ascii=False) + '\n').encode())
        java_argv = ('java', '-Xms256m', '-Xmx1024m', '-Duser.home=' + str(run_dir),
                     '-Djava.io.tmpdir=' + str(run_dir / 'tmp'), '-jar', str(h.ARTIFACT),
                     '--spring.profiles.active=dev',
                     '--spring.config.additional-location=file:' + str(overlay_path))
        if any(secret in ' '.join(java_argv) for secret in
               (root_password, app_password, redis_password, minio_password,
                admin_password, control_password, redis_admin_password)
               if secret):
            raise RuntimeError('Owned secret entered Java argv')
        with os.fdopen(os.open(backend_log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as raw:
            backend = subprocess.Popen(java_argv, cwd=run_dir, stdout=raw,
                stderr=subprocess.STDOUT, start_new_session=True,
                env=h.safe_env(TZ='Asia/Shanghai', TMPDIR=str(run_dir / 'tmp')))
        processes.append(('backend', backend))
        report['owned']['backend_pgid'] = backend.pid
        h.wait_http(backend_port, '/auth/code', backend, 240)
        basic = ('owned-monitor', admin_password)
        wait_core_readiness(backend_port, args.core_health_path, basic, backend)
        token, report['observed']['core_http'] = core_http_checks(
            backend_port, basic, args.core_health_path, args.liveness_health_path,
            args.oss_health_path,
            args.oss_health_http, args.oss_health_state,
            'WTA', control_password)
        report['observed']['startup_oss_endpoint_connections'] = (
            endpoint_count(count_proxy) if count_proxy is not None else None)
        if count_proxy is not None and endpoint_count(count_proxy) != 0:
            raise RuntimeError('Optional OSS endpoint was contacted before explicit diagnosis')
        if args.scenario in ('empty-config', 'duplicate-default', 'bad-default'):
            report['observed']['unusable_default'] = verify_unusable_default(
                backend_port, token, redis_port, redis_password, mysql_id,
                count_proxy, args.scenario, baseline_config_rows)
        if args.scenario == 'bad-nondefault':
            rows = numeric_rows(owned_sql(mysql_id,
                "SELECT (SELECT COUNT(*) FROM sys_oss_config WHERE config_key='image' "
                "AND status='N' AND access_policy='9'),"
                "(SELECT COUNT(*) FROM sys_oss_config WHERE status='Y' AND access_policy='0');",
                't44_bad'), 2)
            if rows != [1, 1]:
                raise RuntimeError('Owned bad nondefault/default pair changed unexpectedly')
            if resp_command(redis_port, ('EXISTS', DEFAULT_OSS_CACHE_KEY),
                            password=redis_password) != 1:
                raise RuntimeError('Owned valid default key disappeared with bad optional row')
            report['observed']['bad_nondefault_preserved'] = True

        if args.scenario == 'redis-publish-denied':
            phase = 'redis_scheduled_fallback'
            report['observed']['redis_fallback'] = redis_fallback(mysql_id, redis_port,
                redis_password, redis_admin_password, backend_port, token,
                control_password, a_user, backend_log, run_id)
            if count_proxy is not None:
                report['observed']['oss_endpoint_connections_after_fallback'] = endpoint_count(count_proxy)
                if endpoint_count(count_proxy) != 0:
                    raise RuntimeError('IN_APP fallback contacted optional OSS endpoint before diagnosis')
        if args.diagnostic_path_template:
            phase = 'optional_diagnostic'
            report['observed']['diagnostic'] = optional_diagnostic(
                backend_port, mysql_id, token, args.diagnostic_path_template,
                args.diagnostic_method, args.diagnostic_expected_http,
                args.diagnostic_expected_code, count_proxy, args.scenario, basic,
                args.oss_health_path, args.oss_health_http, args.oss_health_state)
        else:
            report['observed']['diagnostic'] = {'requested': False}
        phase = 'optional_openapi'
        report['observed']['openapi'] = optional_openapi(backend_port, run_dir,
                                                        args.capture_openapi)
        report['exit_code'] = 0
    except BaseException as error:
        report['error'] = {'phase': phase, 'type': type(error).__name__ if
            type(error).__name__ in ('RuntimeError', 'OwnedSqlError', 'OwnedControlFailure',
                                     'OwnedAclEvidenceError',
                                     'TimeoutExpired', 'OSError', 'ValueError',
                                     'JSONDecodeError') else 'Exception'}
        if isinstance(error, h.OwnedSqlError):
            report['error']['sql'] = error.safe_details()
        if isinstance(error, h.OwnedControlFailure):
            report['error']['http'] = error.safe_details()
        if isinstance(error, OwnedAclEvidenceError):
            report['error']['acl'] = error.safe_details()
        report['error']['runner_line'] = runner_source_line(error)
        backend_exit = h.failed_backend_exit_code(processes)
        if backend_exit is not None:
            report['error']['backend_exit_code'] = backend_exit
    finally:
        for name, process in reversed(processes):
            try:
                live = h.stop_group(process)
                report['cleanup'][name + '_process_group'] = {'pgid': process.pid, 'live': live}
                if live:
                    errors.append(name + '_process_group_remains')
            except Exception:
                errors.append(name + '_process_group_cleanup_failed')
        if count_proxy is not None:
            try:
                count_proxy.shutdown()
                count_proxy.server_close()
                proxy_thread.join(timeout=5)
                if proxy_thread.is_alive():
                    errors.append('owned_count_proxy_thread_remains')
            except Exception:
                errors.append('owned_count_proxy_cleanup_failed')
        if closed_guard is not None:
            try:
                closed_guard.close()
            except Exception:
                errors.append('owned_closed_port_guard_cleanup_failed')
        for path in secret_paths:
            try:
                path.unlink(missing_ok=True)
            except Exception:
                errors.append('owned_private_config_removal_failed')
        for directory in ('tmp', 'logs', 'multipart', 'docker-config'):
            try:
                shutil.rmtree(run_dir / directory)
            except FileNotFoundError:
                pass
            except Exception:
                errors.append('owned_runtime_directory_removal_failed')
        remaining, container_errors = h.cleanup_containers(run_id, captured)
        report['cleanup']['remaining_owned_full_ids'] = remaining
        errors.extend(container_errors)
        try:
            report['cleanup']['anonymous_volumes_absent'] = {
                name: h.volume_absent(name) for name in dict.fromkeys(volumes)}
            if not all(report['cleanup']['anonymous_volumes_absent'].values()):
                errors.append('owned_anonymous_volume_remains')
        except Exception:
            errors.append('owned_volume_absence_check_failed')
        try:
            report['cleanup']['ports_closed'] = {str(port): h.closed(port) for port in ports}
            if not all(report['cleanup']['ports_closed'].values()):
                errors.append('owned_loopback_port_remains')
        except Exception:
            errors.append('owned_port_absence_check_failed')
        try:
            report['source_after'] = h.source_identity(args.expected_head)
            if report['source_after'] != report['source_before']:
                errors.append('source_identity_changed')
        except Exception:
            errors.append('source_after_not_exact_clean_head')
        try:
            report['jar_sha256_after'] = h.sha_file(h.ARTIFACT)
            if report['jar_sha256_after'] != args.expected_jar_sha256:
                errors.append('jar_changed_after_run')
        except Exception:
            errors.append('jar_after_unreadable')
        report['cleanup']['errors'] = errors
        report['exit_code'] = int(bool(report['exit_code'] or errors))
        report['acceptance'] = report['exit_code'] == 0
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        report['private_raw_logs'] = [name for name in ('backend.raw.log', 'init.raw.log')
                                      if (run_dir / name).exists()]
        try:
            save_private(run_dir / 'result.json',
                         (json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode())
        except Exception:
            report['exit_code'] = 1
            print('T44 result write failed', file=sys.stderr)
        print(json.dumps({'result': str(run_dir / 'result.json'), 'exit_code': report['exit_code']}))
    return report['exit_code']


def main():
    parser = argument_parser()
    args = parser.parse_args()
    validate_args(parser, args)
    if args.preflight:
        return preflight()
    return execute(args)


if __name__ == '__main__':
    sys.exit(main())
