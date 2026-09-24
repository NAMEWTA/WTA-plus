#!/usr/bin/env python3
"""T45 owned full-app plus real Admin dist/Chrome gate; inert without --execute.

All raw HTTP, SQL and mc output stays in memory or a 0700 run directory. The
public result contains only bounded counts, hashes, phases and cleanup facts.
"""
import argparse
import base64
import datetime as dt
import hashlib
import hmac
import http.client
import importlib.util
import json
import os
from pathlib import Path
import re
import secrets
import signal
import stat
import shutil
import subprocess
import threading
import time
from urllib.parse import parse_qs, urlsplit

ROOT = Path('/srv/WTA-plus')
RUN_ROOT = Path('/tmp/wta-t45/ui-runs')
BASE_PATH = ROOT / 'frontend/e2e/run-notice-retraction-real.py'
BASE_SHA = '7afb43db7d0086aeda0778389ad2fba95252dddf221eec406024101662c3bf46'
MC = Path('/tmp/wta-t44/mc')
MC_SHA = '01f866e9c5f9b87c2b09116fa5d7c06695b106242d829a8bb32990c00312e891'
OWNER = 'T-45-DIAGNOSTIC-UI'
PROXY_PATH = Path('/tmp/wta-t45/safe_s3_count_proxy_v3.py')
PROXY_SHA = 'd369ab13c078bac3863c18276e7acdeefc5405e52b15746972067e68492acee9'
UI_ROOT = Path('/tmp/wta-t45/ui')
UI_SPEC = UI_ROOT / 'oss-diagnostic-real.spec.ts'
UI_CONFIG = UI_ROOT / 'playwright.config.ts'
UI_SERVER = UI_ROOT / 'owned_admin_dist_server.py'
UI_HASHES = {
    'spec': '2abe0d9cd65c47fb1bd2d6b069910334283564e44aca7331cdd0b86ce262f0af',
    'config': 'ca8fb0010c2827663c0f14c866bff78311fcda250904b4860539546d05662a54',
    'server': '6821f6724fc7571c48802bbc2288b79c1f47da1983530318f0d0d6dceff3e26e',
}
DIST_ROOT = ROOT / 'frontend/apps/admin-web/dist'
ADMIN_APP_ROOT = ROOT / 'frontend/apps/admin-web'
PLAYWRIGHT_CLI = ROOT / 'frontend/node_modules/@playwright/test/cli.js'
UI_TITLES = ('T-45 real Admin sees scoped public and private diagnostic facts',
             'T-45 real ordinary and anonymous identities cannot diagnose')
PHASES = ('preflight', 'owned_services', 'six_sql', 'owned_minio', 'backend',
          'login', 'startup_zero', 'diagnostic', 'private_a', 'public',
          'negative', 'switch_b', 'private_b', 'openapi', 'browser', 'postcheck')


class OwnedMcFailure(RuntimeError):
    def __init__(self, step, exit_code, reason):
        self.step = step
        self.exit_code = exit_code if type(exit_code) is int and -255 <= exit_code <= 255 else None
        self.reason = reason
        super().__init__('owned mc step failed')

    def safe(self):
        return {'step': self.step, 'exit_code': self.exit_code, 'reason': self.reason}


def load_base():
    if hashlib.sha256(BASE_PATH.read_bytes()).hexdigest() != BASE_SHA:
        raise RuntimeError('tracked owned runner changed')
    spec = importlib.util.spec_from_file_location('t44_owned_base', BASE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.OWNER = OWNER
    return module


def load_proxy():
    if hashlib.sha256(PROXY_PATH.read_bytes()).hexdigest() != PROXY_SHA:
        raise RuntimeError('private safe proxy changed')
    spec = importlib.util.spec_from_file_location('t45_safe_s3_count_proxy', PROXY_PATH)
    module = importlib.util.module_from_spec(spec)
    import sys
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def private(path, data):
    with os.fdopen(os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as handle:
        handle.write(data)


class OwnedSqlStep(RuntimeError):
    def __init__(self, stage, stderr=b'', rows=None):
        super().__init__('owned SQL fixture failure')
        self.stage = stage if re.fullmatch(r'[a-z_]{1,40}', stage) else 'unknown'
        match = re.search(rb'ERROR ([0-9]{1,5}) \(([0-9A-Z]{5})\)', stderr)
        self.details = {'stage': self.stage}
        if match:
            self.details.update(errno=int(match[1]), sqlstate=match[2].decode())
        if rows is not None:
            self.details['row_count'] = len(rows)


def count_sql(base, cid, sql, stage):
    """Use owned mysql, SQL through stdin, never expose raw mysql diagnostics."""
    command = (*base.DOCKER, 'exec', '-i', cid, 'sh', '-c',
               'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot '
               '--default-character-set=utf8mb4 --batch --skip-column-names --database="$1"',
               'sh', 'wta-plus')
    completed = subprocess.run(command, input=sql.encode(), stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE, timeout=90, check=False,
                               env=base.safe_env())
    if completed.returncode:
        raise OwnedSqlStep(stage, completed.stderr)
    return completed.stdout.decode().strip()


def one(base, cid, sql, stage):
    rows = count_sql(base, cid, sql, stage).splitlines()
    if len(rows) != 1 or not rows[0].isdigit():
        raise OwnedSqlStep(stage, rows=rows)
    return int(rows[0])


def mc(base, config_dir, *argv, json_out=False):
    if any('secret' in str(part).lower() or 'password' in str(part).lower() for part in argv):
        raise RuntimeError('credential-like argument forbidden')
    step = ('alias_import' if argv[:2] == ('alias', 'import') else
            'bucket_create' if argv[:1] == ('mb',) else
            'public_policy' if argv[:3] == ('anonymous', 'set', 'download') else
            'access_key_create' if argv[:3] == ('admin', 'accesskey', 'create') else
            'bootstrap_object' if argv[:1] == ('cp',) else
            'object_head' if argv[:1] == ('stat',) else 'other')
    try:
        done = subprocess.run((str(MC), '--config-dir', str(config_dir), *argv),
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                              env=base.safe_env(MC_CONFIG_DIR=str(config_dir)),
                              timeout=45, check=False)
    except subprocess.TimeoutExpired:
        raise OwnedMcFailure(step, None, 'timeout') from None
    if done.returncode:
        reason = ('access_denied' if re.search(rb'Access\s*Denied|AccessDenied|not authorized',
                                                done.stderr, re.IGNORECASE) else
                  'not_found' if re.search(rb'NoSuch|not found', done.stderr, re.IGNORECASE) else
                  'transport' if re.search(rb'connection refused|timeout|unreachable',
                                           done.stderr, re.IGNORECASE) else 'other')
        raise OwnedMcFailure(step, done.returncode, reason)
    if json_out:
        try: item = json.loads(done.stdout)
        except (ValueError, UnicodeError):
            raise OwnedMcFailure(step, 0, 'invalid_json') from None
        if type(item) is not dict:
            raise OwnedMcFailure(step, 0, 'invalid_json')
        return item
    return None


def alias_import(base, config_dir, alias, url, access, secret, run_dir):
    config_dir.mkdir(mode=0o700)
    path = run_dir / ('alias-' + alias + '.json')
    private(path, json.dumps({'url': url, 'accessKey': access,
                              'secretKey': secret, 'api': 's3v4', 'path': 'auto'}).encode())
    mc(base, config_dir, 'alias', 'import', alias, str(path))


def http_raw(port, method, path, token=None, body=None, headers=None, max_size=1_000_000):
    if not path.startswith('/'):
        raise RuntimeError('non-local HTTP path')
    data = None if body is None else json.dumps(body, separators=(',', ':')).encode()
    hdrs = {'clientid': 'e5cd7e4891bf95d1d19206ce24a7b32e',
            'Content-Type': 'application/json', 'User-Agent': 'T45-owned-real-gate'}
    if token: hdrs['Authorization'] = 'Bearer ' + token
    if headers: hdrs.update(headers)
    client = http.client.HTTPConnection('127.0.0.1', port, timeout=20)
    try:
        client.request(method, path, body=data, headers=hdrs)
        response = client.getresponse()
        raw = response.read(max_size + 1)
        if len(raw) > max_size:
            raise RuntimeError('HTTP response too large')
        return response.status, raw
    finally:
        client.close()


def api(port, method, path, token, body=None):
    status, raw = http_raw(port, method, path, token, body)
    try: value = json.loads(raw)
    except (UnicodeError, ValueError): raise RuntimeError('API JSON shape differs') from None
    if type(value) is not dict:
        raise RuntimeError('API JSON shape differs')
    return status, value


def wait_core_readiness(port, monitor_password, process, seconds=120):
    auth = base64.b64encode(('owned-monitor:' + monitor_password).encode()).decode()
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError('owned core exited before readiness')
        try:
            status, raw = http_raw(port, 'GET', '/actuator/health/readiness',
                                   headers={'Authorization': 'Basic ' + auth})
        except (OSError, http.client.HTTPException):
            time.sleep(.5)
            continue
        if status not in (200, 503):
            raise RuntimeError('core readiness HTTP contract differs')
        try: data = json.loads(raw)
        except (ValueError, UnicodeError):
            raise RuntimeError('core readiness JSON shape differs') from None
        if type(data) is not dict or type(data.get('components')) is not dict:
            raise RuntimeError('core readiness contributor shape differs')
        if status == 200:
            if (data.get('status') != 'UP' or not {'db', 'redis', 'readinessState'}.issubset(data['components'])
                or data['components']['readinessState'].get('status') != 'UP'):
                raise RuntimeError('core readiness excluded DB or Redis')
            return
        if data.get('status') not in ('DOWN', 'OUT_OF_SERVICE', 'UNKNOWN'):
            raise RuntimeError('core readiness transient status differs')
        time.sleep(.5)
    raise RuntimeError('owned core readiness timed out')


def success(port, method, path, token, body=None):
    status, value = api(port, method, path, token, body)
    if status != 200 or value.get('code') != 200:
        raise RuntimeError('owned API success contract failed')
    return value.get('data')


def denied(port, method, path, token, body=None):
    status, value = api(port, method, path, token, body)
    if status != 200 or value.get('code') not in (401, 403):
        raise RuntimeError('owned authorization denial differs')


def presigned(url, method, owned_port, payload=None, required=None, expected=200):
    parsed = urlsplit(url)
    if parsed.scheme != 'http' or parsed.hostname != '127.0.0.1' or parsed.port != owned_port:
        raise RuntimeError('presigned URL escaped owned loopback')
    path = parsed.path + ('?' + parsed.query if parsed.query else '')
    conn = http.client.HTTPConnection(parsed.hostname, parsed.port, timeout=20)
    try:
        headers = dict(required or {})
        conn.request(method, path, body=payload, headers=headers)
        response = conn.getresponse()
        raw = response.read(2_000_001)
        if len(raw) > 2_000_000 or response.status != expected:
            raise RuntimeError('owned presigned HTTP contract failed')
        return raw
    finally:
        conn.close()


def upload(port, token, payload, fingerprint, owned_minio_port, dynamic_canaries, slot):
    request = {'policy': 'general', 'fileName': 't44-owned.txt',
               'fileSize': len(payload), 'contentType': 'text/plain',
               'fingerprint': fingerprint}
    data = success(port, 'POST', '/resource/oss/uploads', token, request)
    if type(data) is not dict or data.get('mode') != 'SINGLE':
        raise RuntimeError('owned single upload response differs')
    signed = data.get('presignedRequest')
    if type(signed) is not dict or signed.get('method') != 'PUT' or type(signed.get('url')) is not str:
        raise RuntimeError('owned presign response differs')
    upload_token = data.get('uploadToken')
    if type(upload_token) is not str or len(upload_token) < 16:
        raise RuntimeError('upload token shape differs')
    signatures = parse_qs(urlsplit(signed['url']).query).get('X-Amz-Signature', [])
    if len(signatures) != 1 or re.fullmatch(r'[0-9a-fA-F]{64}', signatures[0]) is None:
        raise RuntimeError('owned presign signature shape differs')
    if slot not in ('a', 'b') or slot + '_upload_token' in dynamic_canaries:
        raise RuntimeError('owned dynamic log canary slot differs')
    dynamic_canaries[slot + '_upload_token'] = upload_token
    dynamic_canaries[slot + '_put_signature'] = signatures[0]
    presigned(signed['url'], 'PUT', owned_minio_port, payload,
              signed.get('requiredHeaders'), expected=200)
    oss_id = success(port, 'POST', '/resource/oss/uploads/' + upload_token + '/complete', token, {})
    if not re.fullmatch(r'[1-9][0-9]{0,18}', str(oss_id)):
        raise RuntimeError('completed OSS ID shape differs')
    return str(oss_id)


def download(port, token, oss_id, expected_payload, owned_minio_port, public=False,
             dynamic_canaries=None, signature_slot=None):
    data = success(port, 'GET', '/resource/oss/' + oss_id + '/download-url', token)
    if type(data) is not dict or data.get('accessType') not in ('PRIVATE', 'PUBLIC'):
        raise RuntimeError('access URL shape differs')
    if (data['accessType'] == 'PUBLIC') != public:
        raise RuntimeError('access policy mismatch')
    parsed = urlsplit(data['url'])
    query = parse_qs(parsed.query, keep_blank_values=True)
    parameters = set(query)
    if public:
        if data.get('expiresAt') is not None or any(name.startswith('X-Amz-') for name in parameters):
            raise RuntimeError('public URL carried private signature or expiry')
    else:
        if not {'X-Amz-Signature', 'X-Amz-Expires'}.issubset(parameters) or not data.get('expiresAt'):
            raise RuntimeError('private URL lacks signature or expiry')
        signatures = query.get('X-Amz-Signature', [])
        if (dynamic_canaries is None or signature_slot is None or signature_slot in dynamic_canaries
            or len(signatures) != 1 or re.fullmatch(r'[0-9a-fA-F]{64}', signatures[0]) is None):
            raise RuntimeError('private URL signature canary differs')
        dynamic_canaries[signature_slot] = signatures[0]
        expires = dt.datetime.fromisoformat(data['expiresAt'].replace('Z', '+00:00'))
        ttl = (expires - dt.datetime.now(dt.timezone.utc)).total_seconds()
        if not 0 < ttl <= 1800:
            raise RuntimeError('private URL TTL differs')
        try:
            signed_at = dt.datetime.strptime(query['X-Amz-Date'][0], '%Y%m%dT%H%M%SZ').replace(tzinfo=dt.timezone.utc)
            signature_expires = int(query['X-Amz-Expires'][0])
        except (KeyError, ValueError, IndexError):
            raise RuntimeError('private signature time differs') from None
        if not 0 < signature_expires <= 1800 or abs((signed_at + dt.timedelta(seconds=signature_expires) - expires).total_seconds()) > 2:
            raise RuntimeError('private signature and response expiry differ')
    if presigned(data['url'], 'GET', owned_minio_port) != expected_payload:
        raise RuntimeError('owned object bytes differ')
    return data['accessType']



LOG_URL_RE = re.compile(rb"(?i)\b(?:https?|s3|jdbc:mysql)://[^\s<>]+")
LOG_JWT_RE = re.compile(rb"(?<![A-Za-z0-9_-])[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}(?![A-Za-z0-9_-])")
LOG_AUTH_RE = re.compile(rb"(?i)\b(?:Bearer|Basic)[ \t]+[A-Za-z0-9+/=_-]{10,}")
LOG_ANSI_RE = re.compile(rb"\x1b\[[0-9;]*m")
LOG_LOGGER_RE = re.compile(rb"\b(?:TRACE|DEBUG|INFO|WARN|ERROR)[ \t]+([A-Za-z_$][A-Za-z0-9_.$]{2,159})(?:[ \t]|$)")
LOG_EXCEPTION_RE = re.compile(rb"\b(?:[A-Za-z_$][A-Za-z0-9_$]*\.)*[A-Za-z_$][A-Za-z0-9_$]*(?:Exception|Error)\b")
LOG_FRAME_RE = re.compile(rb"\(([A-Za-z_$][A-Za-z0-9_$]*\.java):([1-9][0-9]{0,6})\)")


def inspect_backend_log(raw, canaries, tokens, run_dir):
    """Retain bounded safe metadata, never raw or redacted message context."""
    fixed_names = {'mysql_root_password', 'mysql_app_password', 'redis_password',
                   'minio_root_user', 'minio_root_password', 'minio_app_access_key',
                   'minio_app_secret_key', 'admin_control_password',
                   'ordinary_user_password', 'monitor_password'}
    dynamic_names = {'a_upload_token', 'a_put_signature', 'b_upload_token', 'b_put_signature',
                     'a_get_signature_first', 'a_get_signature_stale',
                     'b_get_signature', 'a_get_signature_after_switch'}
    if set(canaries) != fixed_names | dynamic_names:
        raise RuntimeError('backend log canary inventory differs')
    if any(type(value) is not str or len(value) < 12 for value in (*canaries.values(), *tokens)):
        raise RuntimeError('backend log canary shape differs')
    values = {name: value.encode() for name, value in canaries.items()}
    category_counts = {name: raw.count(value) for name, value in values.items()}
    sanitized = raw
    for name, value in sorted(values.items(), key=lambda item: len(item[1]), reverse=True):
        sanitized = sanitized.replace(value, b'[REDACTED_CANARY_' + name.upper().encode() + b']')
    token_replacements = 0
    for value in tokens:
        encoded = value.encode()
        token_replacements += sanitized.count(encoded)
        sanitized = sanitized.replace(encoded, b'[REDACTED_SESSION_TOKEN]')
    sanitized, authorization_replacements = LOG_AUTH_RE.subn(b'[REDACTED_AUTHORIZATION]', sanitized)
    sanitized, jwt_replacements = LOG_JWT_RE.subn(b'[REDACTED_JWT]', sanitized)
    sanitized, url_replacements = LOG_URL_RE.subn(b'[REDACTED_URL]', sanitized)
    if any(value in sanitized for value in (*values.values(), *(token.encode() for token in tokens))):
        raise RuntimeError('backend log sanitization incomplete')
    if LOG_AUTH_RE.search(sanitized) or LOG_JWT_RE.search(sanitized) or LOG_URL_RE.search(sanitized):
        raise RuntimeError('backend log sanitization incomplete')
    raw_lines, safe_lines = raw.splitlines(), sanitized.splitlines()
    if len(raw_lines) != len(safe_lines):
        raise RuntimeError('backend log line mapping differs')
    locations = []
    matching_lines = 0
    logger = 'unidentified'
    for line_no, (original_line, safe_line) in enumerate(zip(raw_lines, safe_lines), 1):
        clean_line = LOG_ANSI_RE.sub(b'', safe_line)
        match = LOG_LOGGER_RE.search(clean_line)
        if match:
            logger = match[1].decode('ascii')
        hits = {name: original_line.count(value) for name, value in values.items()}
        hits = {name: count for name, count in hits.items() if count}
        if not hits:
            continue
        matching_lines += 1
        if len(locations) >= 100:
            continue
        exception_classes = sorted({match.decode('ascii') for match in LOG_EXCEPTION_RE.findall(clean_line)})[:3]
        frame = LOG_FRAME_RE.search(clean_line)
        locations.append({'log_line': line_no, 'logger': logger,
                          'exception_classes': exception_classes,
                          'stack_file': frame[1].decode('ascii') if frame else None,
                          'stack_line': int(frame[2]) if frame else None,
                          'categories': hits})
    summary = {
        'source_raw_sha256': hashlib.sha256(raw).hexdigest(),
        'sanitized_sha256': hashlib.sha256(sanitized).hexdigest(),
        'source_bytes': len(raw), 'source_lines': len(raw_lines),
        'category_counts': category_counts,
        'redaction_counts': {'known_canary': sum(category_counts.values()),
                             'session_token': token_replacements,
                             'authorization': authorization_replacements,
                             'jwt': jwt_replacements, 'full_url': url_replacements},
        'matching_lines': matching_lines,
        'locations_truncated': max(0, matching_lines - len(locations)),
        'locations': locations,
        'context_retained': False,
    }
    payload = json.dumps(summary, sort_keys=True, indent=2).encode() + b'\n'
    if any(value in payload for value in (*values.values(), *(token.encode() for token in tokens))):
        raise RuntimeError('backend log diagnostic contains canary')
    if LOG_AUTH_RE.search(payload) or LOG_JWT_RE.search(payload) or LOG_URL_RE.search(payload):
        raise RuntimeError('backend log diagnostic contains unsafe context')
    directory = run_dir / 'diagnostics'
    directory.mkdir(mode=0o700)
    if stat.S_IMODE(directory.stat().st_mode) != 0o700 or directory.is_symlink():
        raise RuntimeError('backend log diagnostic directory unsafe')
    path = directory / 'backend-log-summary.json'
    private(path, payload)
    if stat.S_IMODE(path.stat().st_mode) != 0o600:
        raise RuntimeError('backend log diagnostic file mode unsafe')
    return {'private_summary': str(path), 'known_canary_hits': sum(category_counts.values()),
            'source_raw_sha256': summary['source_raw_sha256'],
            'sanitized_sha256': summary['sanitized_sha256'],
            'redaction_counts': summary['redaction_counts']}

def report_write(path, report):
    safe = json.dumps(report, sort_keys=True, indent=2).encode() + b'\n'
    private(path, safe)


def nested_reason(value, key):
    if type(value) is not dict:
        return None
    item = value.get(key)
    if type(item) is dict and type(item.get('reason')) is str:
        return item['reason']
    for child in value.values():
        found = nested_reason(child, key)
        if found is not None:
            return found
    return None


def acl_write_denied(port, bucket, key, access, secret):
    """A signed no-op ACL write against one owned object must be denied by IAM."""
    instant = dt.datetime.now(dt.timezone.utc)
    stamp = instant.strftime('%Y%m%dT%H%M%SZ')
    day = instant.strftime('%Y%m%d')
    path = '/' + bucket + '/' + key
    payload_hash = hashlib.sha256(b'').hexdigest()
    canonical_headers = (f'host:127.0.0.1:{port}\n'
                         + 'x-amz-acl:private\n'
                         + 'x-amz-content-sha256:' + payload_hash + '\n'
                         + 'x-amz-date:' + stamp + '\n')
    scope = day + '/us-east-1/s3/aws4_request'
    canonical = ('PUT\n' + path + '\nacl=\n' + canonical_headers
                 + '\nhost;x-amz-acl;x-amz-content-sha256;x-amz-date\n' + payload_hash)
    string_to_sign = ('AWS4-HMAC-SHA256\n' + stamp + '\n' + scope + '\n'
                      + hashlib.sha256(canonical.encode()).hexdigest())
    keybytes = ('AWS4' + secret).encode()
    for component in (day, 'us-east-1', 's3', 'aws4_request'):
        keybytes = hmac.new(keybytes, component.encode(), hashlib.sha256).digest()
    signature = hmac.new(keybytes, string_to_sign.encode(), hashlib.sha256).hexdigest()
    auth = ('AWS4-HMAC-SHA256 Credential=' + access + '/' + scope
            + ', SignedHeaders=host;x-amz-acl;x-amz-content-sha256;x-amz-date, Signature=' + signature)
    conn = http.client.HTTPConnection('127.0.0.1', port, timeout=5)
    try:
        conn.putrequest('PUT', path + '?acl', skip_host=True)
        conn.putheader('Host', f'127.0.0.1:{port}')
        conn.putheader('x-amz-acl', 'private')
        conn.putheader('x-amz-content-sha256', payload_hash)
        conn.putheader('x-amz-date', stamp)
        conn.putheader('Authorization', auth)
        conn.putheader('Content-Length', '0')
        conn.endheaders()
        response = conn.getresponse()
        raw = response.read(4097)
        if response.status != 403 or b'AccessDenied' not in raw or len(raw) > 4096:
            raise RuntimeError('owned object ACL write was not AccessDenied')
    finally:
        conn.close()


def policy_write_denied(base, app_mc, bucket):
    try:
        done = subprocess.run((str(MC), '--config-dir', str(app_mc),
                               'anonymous', 'set', 'private', 'app/' + bucket),
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                              env=base.safe_env(MC_CONFIG_DIR=str(app_mc)),
                              timeout=20, check=False)
    except subprocess.TimeoutExpired:
        raise OwnedMcFailure('policy_write_denial', None, 'timeout') from None
    if done.returncode == 0:
        raise OwnedMcFailure('policy_write_denial', 0, 'unexpected_success')
    if not re.search(rb'Access\s*Denied|AccessDenied|not authorized',
                     done.stderr, re.IGNORECASE):
        raise OwnedMcFailure('policy_write_denial', done.returncode, 'unexpected_failure')


FACT_SUBJECTS = frozenset(('POLICY_READ', 'POLICY_WRITE', 'ACL_LIST',
                           'ACL_WRITE_RISK', 'OBJECT_HEAD', 'OBJECT_GET'))
FACT_BASIS = frozenset(('POLICY_ALLOW', 'POLICY_DENY', 'NO_SUCH_POLICY',
                        'POLICY_UNREADABLE', 'COMPLEX_POLICY', 'INVALID_POLICY',
                        'ACL_GRANT', 'ACL_NO_GRANT', 'ACL_UNREADABLE',
                        'HTTP_SUCCESS', 'HTTP_DENIED', 'HTTP_NOT_FOUND',
                        'REDIRECT', 'HTTP_ERROR', 'TIMEOUT', 'NETWORK_ERROR',
                        'INTERRUPTED', 'NOT_EVALUATED', 'UNSUPPORTED'))
FACT_PROVENANCE = {
    'POLICY_READ': ('BUCKET_POLICY', 'BUCKET'),
    'POLICY_WRITE': ('BUCKET_POLICY', 'BUCKET'),
    'ACL_LIST': ('BUCKET_ACL', 'BUCKET'),
    'ACL_WRITE_RISK': ('BUCKET_ACL', 'BUCKET'),
    'OBJECT_HEAD': ('ANONYMOUS_HEAD', 'OBJECT'),
    'OBJECT_GET': ('ANONYMOUS_GET', 'OBJECT'),
}


def iso_instant(value):
    if type(value) is not str or len(value) > 48:
        raise RuntimeError('diagnostic timestamp shape differs')
    try:
        parsed = dt.datetime.fromisoformat(value.replace('Z', '+00:00'))
    except ValueError:
        raise RuntimeError('diagnostic timestamp shape differs') from None
    if parsed.tzinfo is None:
        raise RuntimeError('diagnostic timestamp shape differs')
    return parsed


def validate_fact_response(http_status, response, *, public):
    """Reject false certainty and raw-provider context without retaining either."""
    fields = response.get('data') if type(response) is dict else None
    if (http_status != 200 or type(response) is not dict or response.get('code') != 200
        or type(fields) is not dict
        or set(fields) != {'status', 'reason', 'checkedAt', 'facts'}):
        raise RuntimeError('diagnostic public shape or authorization differs')
    if fields['status'] != 'NOT_SERVING' or fields['reason'] not in (
        'POLICY_UNREADABLE', 'DIAGNOSTIC_UNVERIFIED'):
        raise RuntimeError('unknown diagnostic was reported as verified or mismatch')
    iso_instant(fields['checkedAt'])
    facts = fields['facts']
    if type(facts) is not list or len(facts) != len(FACT_SUBJECTS):
        raise RuntimeError('diagnostic fact set differs')
    by_subject = {}
    for fact in facts:
        if (type(fact) is not dict or set(fact) != {'subject', 'observation',
            'source', 'scope', 'observedAt', 'basis'}):
            raise RuntimeError('diagnostic fact shape differs')
        subject = fact['subject']
        if subject not in FACT_SUBJECTS or subject in by_subject:
            raise RuntimeError('diagnostic fact set differs')
        if (fact['source'], fact['scope']) != FACT_PROVENANCE[subject]:
            raise RuntimeError('diagnostic fact provenance differs')
        if fact['observation'] not in ('ALLOWED', 'DENIED', 'UNKNOWN') or fact['basis'] not in FACT_BASIS:
            raise RuntimeError('diagnostic fact enum differs')
        iso_instant(fact['observedAt'])
        by_subject[subject] = (fact['observation'], fact['basis'])
    if set(by_subject) != FACT_SUBJECTS:
        raise RuntimeError('diagnostic fact set differs')
    for subject, basis in (('POLICY_READ', 'POLICY_UNREADABLE'),
                           ('POLICY_WRITE', 'POLICY_UNREADABLE'),
                           ('ACL_LIST', 'ACL_UNREADABLE'),
                           ('ACL_WRITE_RISK', 'ACL_UNREADABLE')):
        if by_subject[subject] != ('UNKNOWN', basis):
            raise RuntimeError('unreadable policy or ACL gained false certainty')
    expected = ('ALLOWED', 'HTTP_SUCCESS') if public else ('DENIED', 'HTTP_DENIED')
    if by_subject['OBJECT_HEAD'] != expected or by_subject['OBJECT_GET'] != expected:
        raise RuntimeError('single-object anonymous fact differs')
    return {'status': fields['status'], 'reason': fields['reason'],
            'fact_count': len(facts), 'unknown_bucket_facts': 4,
            'object_observation': expected[0]}


def validate_probe_delta(proxy_helper, before, after, *, before_arrivals,
                         after_arrivals, bucket_alias, public):
    rows = proxy_helper.safe_delta(before, after)
    object_status = (200, 206) if public else (403, 403)
    expected = [
        ('HEAD', 'OBJECT', True, False, 200),
        ('GET', 'POLICY', True, False, 403),
        ('GET', 'ACL', True, False, 403),
        ('HEAD', 'OBJECT', False, False, object_status[0]),
        ('GET', 'OBJECT', False, True, object_status[1]),
    ]
    actual = sorted((row['method'], row['category'], row['signed'], row['range'],
                     row['status']) for row in rows for _ in range(row['count']))
    if (after_arrivals - before_arrivals != 5 or len(actual) != 5 or actual != sorted(expected)
        or any(row['bucket_alias'] != bucket_alias for row in rows)):
        raise RuntimeError('diagnostic provider request boundary differs')
    return rows


def validate_diagnostic_openapi(parsed):
    if type(parsed) is not dict:
        raise RuntimeError('live diagnostic schema differs')
    paths = parsed.get('paths')
    schemas = (parsed.get('components') or {}).get('schemas')
    route = (paths or {}).get('/resource/oss/config/diagnose/{ossConfigId}')
    if type(schemas) is not dict or type(route) is not dict or 'post' not in route:
        raise RuntimeError('live diagnostic schema differs')
    response = (((route['post'].get('responses') or {}).get('200') or {})
                .get('content') or {})
    response_refs = [body.get('schema', {}).get('$ref') for body in response.values()
                     if type(body) is dict]
    if '#/components/schemas/ROssStorageDiagnosticVo' not in response_refs:
        raise RuntimeError('live diagnostic schema differs')
    vo = schemas.get('OssStorageDiagnosticVo')
    if type(vo) is not dict or set((vo.get('properties') or {})) != {
        'status', 'reason', 'checkedAt', 'facts'}:
        raise RuntimeError('live diagnostic schema differs')
    facts = vo['properties']['facts']
    if type(facts) is not dict or facts.get('type') != 'array':
        raise RuntimeError('live diagnostic schema differs')
    fact_ref = (facts.get('items') or {}).get('$ref')
    if type(fact_ref) is not str or not fact_ref.startswith('#/components/schemas/'):
        raise RuntimeError('live diagnostic schema differs')
    fact_schema = schemas.get(fact_ref.rsplit('/', 1)[-1])
    if type(fact_schema) is not dict or set((fact_schema.get('properties') or {})) != {
        'subject', 'observation', 'source', 'scope', 'observedAt', 'basis'}:
        raise RuntimeError('live diagnostic schema differs')
    return {'diagnostic_vo_fields': sorted(vo['properties']),
            'fact_fields': sorted(fact_schema['properties'])}


def load_ui_server():
    if base_sha(UI_SERVER) != UI_HASHES['server']:
        raise RuntimeError('private Admin dist server changed')
    spec = importlib.util.spec_from_file_location('t45_owned_admin_dist_server', UI_SERVER)
    module = importlib.util.module_from_spec(spec)
    import sys
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def base_sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_public_admin_env():
    """Reject Vite URL/auth overrides that break the owned same-origin proxy."""
    if any((ADMIN_APP_ROOT / name).exists() for name in
           ('.env.local', '.env.production.local')):
        raise RuntimeError('local Admin build environment override exists')
    path = ADMIN_APP_ROOT / '.env.production'
    values = {}
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        key, separator, value = line.partition('=')
        if separator and key in ('VITE_APP_CONTEXT_PATH', 'VITE_APP_BASE_API',
                                  'VITE_APP_MESSAGE_ENABLED', 'VITE_APP_CLIENT_ID'):
            if key in values:
                raise RuntimeError('duplicate Admin build environment key')
            values[key] = value.strip()
    if values != {
        'VITE_APP_CONTEXT_PATH': '/', 'VITE_APP_BASE_API': '/prod-api',
        'VITE_APP_MESSAGE_ENABLED': 'false',
        'VITE_APP_CLIENT_ID': 'e5cd7e4891bf95d1d19206ce24a7b32e',
    }:
        raise RuntimeError('Admin build environment differs')
    return base_sha(path)


def validate_dist_manifest(path, head, tree):
    """Bind the final B source, successful production build log and entire dist."""
    if path.is_symlink() or stat.S_IMODE(path.stat().st_mode) != 0o600:
        raise RuntimeError('private Admin dist manifest differs')
    manifest = json.loads(path.read_text())
    if (type(manifest) is not dict or manifest.get('schema_version') != 1
        or manifest.get('source_head') != head or manifest.get('source_tree') != tree
        or manifest.get('dist_root') != 'frontend/apps/admin-web/dist'
        or manifest.get('build_exit_code') != 0):
        raise RuntimeError('Admin dist source or build proof differs')
    log_path = Path(manifest.get('build_log_path', ''))
    log_sha = manifest.get('build_log_sha256')
    if (not log_path.is_absolute() or not log_path.resolve().is_relative_to(Path('/tmp/wta-t45'))
        or log_path.is_symlink() or not log_path.is_file()
        or stat.S_IMODE(log_path.stat().st_mode) != 0o600
        or type(log_sha) is not str
        or not re.fullmatch(r'[0-9a-f]{64}', log_sha) or base_sha(log_path) != log_sha):
        raise RuntimeError('Admin dist build log proof differs')
    expected = manifest.get('files')
    if type(expected) is not dict or len(expected) < 10:
        raise RuntimeError('Admin dist inventory differs')
    actual = {}
    for item in DIST_ROOT.rglob('*'):
        if item.is_symlink() or (not item.is_file() and not item.is_dir()):
            raise RuntimeError('Admin dist inventory differs')
        if item.is_file():
            actual[item.relative_to(DIST_ROOT).as_posix()] = base_sha(item)
    if expected != actual:
        raise RuntimeError('Admin dist inventory differs')
    mode = json.loads((DIST_ROOT / 'build-mode.json').read_text())
    if mode != {'app': 'admin-web', 'mode': 'production'}:
        raise RuntimeError('Admin dist build mode differs')
    return {'file_count': len(actual), 'manifest_sha256': base_sha(path),
            'build_log_sha256': log_sha, 'inventory_sha256': hashlib.sha256(
                json.dumps(actual, sort_keys=True, separators=(',', ':')).encode()).hexdigest()}


def browser_report(data):
    if type(data) is not dict:
        raise RuntimeError('Chrome reporter shape differs')
    stats = data.get('stats') or {}
    counts = {key: int(stats.get(key, 0)) for key in ('expected', 'unexpected', 'skipped', 'flaky')}
    cases = []
    def visit(suites, inherited=None):
        for suite in suites:
            source = suite.get('file') or inherited
            for spec in suite.get('specs', []):
                for case in spec.get('tests', []):
                    cases.append((spec.get('file') or source, spec.get('title'),
                                  case.get('projectName'), case.get('results', [])))
            visit(suite.get('suites', []), source)
    visit(data.get('suites', []))
    if len(cases) != 2 or counts != {'expected': 2, 'unexpected': 0, 'skipped': 0, 'flaky': 0}:
        raise RuntimeError('Chrome exact two-case counts differ')
    titles = []
    for source, title, project, results in cases:
        actual = Path(source) if type(source) is str else None
        candidates = ([actual.resolve()] if actual is not None and actual.is_absolute() else
                      [(root / actual).resolve() for root in (UI_ROOT, ROOT / 'frontend')]
                      if actual is not None else [])
        if (UI_SPEC.resolve() not in candidates
            or title not in UI_TITLES or project != 'chromium' or len(results) != 1
            or results[0].get('status') != 'passed'):
            raise RuntimeError('Chrome exact case identity differs')
        titles.append(title)
    if sorted(titles) != sorted(UI_TITLES):
        raise RuntimeError('Chrome exact case identity differs')
    return {'counts': counts, 'titles': titles, 'project': 'chromium', 'attempts': 2}


def owned_profile_pids(tmpdir):
    """Find only this run's browser profile processes; never expose cmdline."""
    marker = str(tmpdir).encode()
    matches = []
    for entry in Path('/proc').iterdir():
        if not entry.name.isdigit():
            continue
        try:
            command = (entry / 'cmdline').read_bytes()
        except (FileNotFoundError, ProcessLookupError, PermissionError):
            continue
        if marker in command and (b'chrome' in command.lower() or b'chromium' in command.lower()):
            matches.append(int(entry.name))
    return matches


def cleanup_owned_profiles(tmpdir):
    for signum, delay in ((signal.SIGTERM, 2), (signal.SIGKILL, 2)):
        pids = owned_profile_pids(tmpdir)
        if not pids:
            return []
        for pid in pids:
            try:
                fresh = (Path('/proc') / str(pid) / 'cmdline').read_bytes()
                if (str(tmpdir).encode() in fresh
                    and (b'chrome' in fresh.lower() or b'chromium' in fresh.lower())):
                    os.kill(pid, signum)
            except (FileNotFoundError, ProcessLookupError, PermissionError):
                pass
        time.sleep(delay)
    return owned_profile_pids(tmpdir)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--preflight', action='store_true')
    parser.add_argument('--execute', action='store_true')
    parser.add_argument('--expected-head')
    parser.add_argument('--expected-jar-sha256')
    parser.add_argument('--package-proof', type=Path)
    parser.add_argument('--dist-manifest', type=Path)
    args = parser.parse_args()
    if args.preflight:
        if args.execute:
            raise SystemExit('Choose exactly one mode')
        inputs = {'tracked_owned_runner': BASE_PATH.is_file(),
                  'private_mc_copy': MC.is_file(),
                  'private_safe_proxy': PROXY_PATH.is_file(),
                  'private_ui_inputs': all(item.is_file() for item in
                      (UI_SPEC, UI_CONFIG, UI_SERVER, PLAYWRIGHT_CLI)),
                  'production_admin_dist': (DIST_ROOT / 'index.html').is_file(),
                  'public_admin_env': bool(validate_public_admin_env()),
                  'full_jar_path': (ROOT / 'backend/wta-admin/target/wta-admin.jar').is_file(),
                  'six_sql': all((ROOT / 'release-artifacts/docker/infrastructure/mysql/init' / item).is_file()
                                 for item in ('10-cde-base-ddl.sql', '20-cde-job.sql',
                                              '30-cde-workflow.sql', '40-cde-ai.sql',
                                              '50-cde-base-dml.sql', '60-cde-nacos.sql'))}
        print(json.dumps({'mode': 'static-only', 'services_started': False, 'inputs': inputs}))
        return 0 if all(inputs.values()) else 1
    if not args.execute:
        raise SystemExit('No action without --execute')
    if (args.expected_head is None or args.expected_jar_sha256 is None
        or args.package_proof is None or args.dist_manifest is None):
        raise SystemExit('Exact source, JAR, package proof and dist manifest are required')
    if not re.fullmatch(r'[0-9a-f]{40}', args.expected_head) or not re.fullmatch(r'[0-9a-f]{64}', args.expected_jar_sha256):
        raise SystemExit('Expected hashes are malformed')
    base = load_base()
    proxy_helper = load_proxy()
    ui_module = load_ui_server()
    run_id = secrets.token_hex(8)
    RUN_ROOT.mkdir(mode=0o700, parents=True, exist_ok=True)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(mode=0o700)
    report = {'accepted': False, 'phase': 'preflight', 'source': None,
              'jar_sha256': None, 'counts': {}, 'owned': {'run_id': run_id,
              'owner_label': OWNER, 'containers': [], 'volumes': [], 'ports': [],
              'process_groups': []}, 'cleanup_errors': []}
    containers, volumes, ports, groups = [], [], [], []
    backend = None
    playwright = None
    proxy = None
    proxy_thread = None
    ui_server = None
    ui_thread = None
    phase = 'preflight'
    try:
        if base.sha_file(MC) != MC_SHA or not os.access(MC, os.X_OK):
            raise RuntimeError('private mc copy changed')
        source = base.source_identity(args.expected_head)
        report['source'] = source
        if base.sha_file(base.ARTIFACT) != args.expected_jar_sha256:
            raise RuntimeError('full JAR hash differs')
        report['jar_sha256'] = args.expected_jar_sha256
        base.validate_package_proof(json.loads(args.package_proof.read_text()),
                                    source['head'], source['tree'], args.expected_jar_sha256,
                                    base.ARTIFACT.stat().st_size)
        report['full_bundle_modules'] = base.validate_full_jar(base.ARTIFACT)
        if (base_sha(UI_SPEC) != UI_HASHES['spec'] or base_sha(UI_CONFIG) != UI_HASHES['config']
            or UI_ROOT.joinpath('node_modules').resolve() != ROOT / 'frontend/node_modules'):
            raise RuntimeError('private Chrome input changed')
        report['browser_inputs'] = dict(UI_HASHES)
        report['public_admin_env_sha256'] = validate_public_admin_env()
        report['playwright_cli_sha256'] = base_sha(PLAYWRIGHT_CLI)
        report['dist'] = validate_dist_manifest(args.dist_manifest, source['head'], source['tree'])
        report['six_sql_sha256'] = {name: base.sha_file(base.SQL_ROOT / name)
                                    for name in base.SQL_NAMES}
        root_pass, app_pass, redis_pass, minio_user, minio_pass = [base.random_password() for _ in range(5)]
        app_user = 't45_' + run_id
        control_pass, ordinary_pass = base.random_login_password(), base.random_login_password()
        bucket_a, bucket_p, bucket_b = ('t45-' + run_id + '-' + suffix for suffix in ('a', 'p', 'b'))
        for name in ('tmp', 'logs', 'multipart', 'docker-config'):
            (run_dir / name).mkdir(mode=0o700)
        phase = 'owned_services'
        mysql_env = run_dir / 'mysql.env'; private(mysql_env, ('MYSQL_ROOT_PASSWORD=' + root_pass + '\n').encode())
        mysql_id = base.full_id(base.docker('run', '--pull=never', '-d', '--name', 't45-mysql-' + run_id,
            '--label', 'namewta.test.owner=' + OWNER, '--label', 'namewta.test.run=' + run_id,
            '-p', '127.0.0.1::3306', '--env-file', str(mysql_env), 'mysql:8.4.9',
            '--character-set-server=utf8mb4', '--collation-server=utf8mb4_general_ci',
            '--log-bin-trust-function-creators=1'))
        containers.append(mysql_id); base.assert_owned(mysql_id, run_id)
        volumes += base.volume_names(mysql_id); base.wait_mysql(mysql_id)
        mysql_port = base.mapped_port(mysql_id, '3306'); ports.append(mysql_port)
        redis_conf = run_dir / 'redis.conf'; private(redis_conf, base.redis_config(redis_pass))
        os.chown(redis_conf, base.REDIS_CONFIG_USER, base.REDIS_CONFIG_USER)
        redis_id = base.full_id(base.docker('run', '--pull=never', '-d', '--name', 't45-redis-' + run_id,
            '--label', 'namewta.test.owner=' + OWNER, '--label', 'namewta.test.run=' + run_id,
            '-p', '127.0.0.1::6379', '--user', str(base.REDIS_CONFIG_USER) + ':' + str(base.REDIS_CONFIG_USER),
            '--mount', 'type=bind,source=' + str(redis_conf) + ',target=/etc/redis/redis.conf,readonly',
            'redis:8.6.3', 'redis-server', '/etc/redis/redis.conf'))
        containers.append(redis_id); base.assert_owned(redis_id, run_id)
        volumes += base.volume_names(redis_id); base.wait_redis(redis_id)
        redis_port = base.mapped_port(redis_id, '6379'); ports.append(redis_port)
        minio_env = run_dir / 'minio.env'
        private(minio_env, ('MINIO_ROOT_USER=' + minio_user + '\nMINIO_ROOT_PASSWORD=' + minio_pass + '\n').encode())
        minio_id = base.full_id(base.docker('run', '--pull=never', '-d', '--name', 't45-minio-' + run_id,
            '--label', 'namewta.test.owner=' + OWNER, '--label', 'namewta.test.run=' + run_id,
            '-p', '127.0.0.1::9000', '--env-file', str(minio_env), base.MINIO_IMAGE,
            'server', '--address', ':9000', '/data'))
        containers.append(minio_id); base.assert_owned(minio_id, run_id)
        volumes += base.volume_names(minio_id)
        minio_port = base.mapped_port(minio_id, '9000'); ports.append(minio_port)
        base.wait_minio(minio_port)
        phase = 'six_sql'
        init_env = run_dir / 'init.env'
        private(init_env, ('MYSQL_DATABASE=wta-plus\nMYSQL_ROOT_PASSWORD=' + root_pass
            + '\nMYSQL_APP_USER=' + app_user
            + '\nMYSQL_APP_PASSWORD=' + app_pass + '\nMINIO_ROOT_USER=' + minio_user
            + '\nMINIO_ROOT_PASSWORD=' + minio_pass + '\nMINIO_ENDPOINT=127.0.0.1:'
            + str(minio_port) + '\nMINIO_BUCKET=' + bucket_a + '\n').encode())
        init = base.run(('bash', str(base.INIT_SCRIPT), '--container', mysql_id,
                         '--env-file', str(init_env), '--sql-dir', str(base.SQL_ROOT)),
                        env=base.safe_env(DOCKER_HOST='unix:///var/run/docker.sock',
                                          DOCKER_CONFIG=str(run_dir / 'docker-config')), timeout=900)
        # Raw init output may contain credentials; retain only its exit code.
        if init.returncode:
            raise RuntimeError('owned six-SQL initializer failed')
        report['counts']['tables'] = one(base, mysql_id,
            'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();', 'tables')
        if report['counts']['tables'] != 103:
            raise RuntimeError('owned schema table count differs')
        phase = 'owned_minio'
        root_mc = run_dir / 'mc-root'; app_mc = run_dir / 'mc-app'
        alias_import(base, root_mc, 'root', 'http://127.0.0.1:' + str(minio_port), minio_user, minio_pass, run_dir)
        for bucket in (bucket_a, bucket_p, bucket_b):
            mc(base, root_mc, 'mb', 'root/' + bucket)
        mc(base, root_mc, 'anonymous', 'set', 'download', 'root/' + bucket_p)
        policy = {'Version': '2012-10-17', 'Statement': [
            {'Effect': 'Allow', 'Action': ['s3:GetObject', 's3:PutObject', 's3:AbortMultipartUpload',
                                          's3:ListMultipartUploadParts'],
             'Resource': ['arn:aws:s3:::' + bucket + '/*' for bucket in (bucket_a, bucket_p, bucket_b)]},
            {'Effect': 'Allow', 'Action': ['s3:ListBucket', 's3:GetBucketLocation'],
             'Resource': ['arn:aws:s3:::' + bucket for bucket in (bucket_a, bucket_p, bucket_b)]}]}
        policy_path = run_dir / 'app-policy.json'; private(policy_path, json.dumps(policy).encode())
        item = mc(base, root_mc, 'admin', 'accesskey', 'create', 'root/', '--policy', str(policy_path),
                  '--json', json_out=True)
        item = item.get('data', item)
        if type(item) is not dict:
            raise OwnedMcFailure('access_key_create', 0, 'invalid_shape')
        access, secret = item.get('accessKey'), item.get('secretKey')
        if not all(type(x) is str and len(x) >= 12 for x in (access, secret)):
            raise OwnedMcFailure('access_key_create', 0, 'invalid_shape')
        alias_import(base, app_mc, 'app', 'http://127.0.0.1:' + str(minio_port), access, secret, run_dir)
        # App has no bucket-policy/ACL API permission; prove object HEAD capability.
        payload_public = b'T45-owned-public-' + run_id.encode()
        public_file = run_dir / 'public-payload'; private(public_file, payload_public)
        mc(base, root_mc, 'cp', str(public_file), 'root/' + bucket_p + '/public-' + run_id)
        canary_key = 'canary-' + run_id
        mc(base, root_mc, 'cp', str(public_file), 'root/' + bucket_a + '/' + canary_key)
        mc(base, root_mc, 'cp', str(public_file), 'root/' + bucket_b + '/' + canary_key)
        mc(base, app_mc, 'stat', 'app/' + bucket_p + '/public-' + run_id)
        mc(base, app_mc, 'stat', 'app/' + bucket_a + '/' + canary_key)
        policy_write_denied(base, app_mc, bucket_a)
        acl_write_denied(minio_port, bucket_a, canary_key, access, secret)
        report['minimal_privilege'] = {'bucket_policy_write_denied': True,
                                       'object_acl_write_denied': True,
                                       'object_head_allowed': True}
        proxy = proxy_helper.SafeS3CountServer(minio_port,
            {bucket_a: 'private_a', bucket_p: 'public', bucket_b: 'private_b'})
        proxy_port = proxy.server_address[1]; ports.append(proxy_port)
        proxy_thread = threading.Thread(target=proxy.serve_forever, daemon=True)
        proxy_thread.start()
        endpoint = '127.0.0.1:' + str(proxy_port)
        h = base.hexsql
        id_a = one(base, mysql_id, "SELECT oss_config_id FROM sys_oss_config WHERE config_key='minio';", 'id_a')
        id_p = one(base, mysql_id, "SELECT oss_config_id FROM sys_oss_config WHERE config_key='image';", 'id_p')
        id_b = 8_300_000_000_000_000_000 + secrets.randbelow(10_000_000_000)
        # These rows are owned by this new DB; the copied row contains only synthetic endpoints/credentials.
        sql = ("UPDATE sys_oss_config SET access_key=" + h(access) + ",secret_key=" + h(secret)
            + ",bucket_name=" + h(bucket_a) + ",endpoint=" + h(endpoint)
            + ",domain_url=" + "''" + ",is_https='N',access_policy='0',status='Y'"
            + f" WHERE oss_config_id={id_a}; SELECT ROW_COUNT();")
        if count_sql(base, mysql_id, sql, 'config_a').splitlines()[-1] != '1':
            raise RuntimeError('owned default config update differs')
        sql = ("UPDATE sys_oss_config SET access_key=" + h(access) + ",secret_key=" + h(secret)
            + ",bucket_name=" + h(bucket_p) + ",endpoint=" + h(endpoint)
            + ",domain_url=" + h('http://' + endpoint + '/' + bucket_p) + ",is_https='N',access_policy='2',status='N'"
            + f" WHERE oss_config_id={id_p}; SELECT ROW_COUNT();")
        if count_sql(base, mysql_id, sql, 'config_p').splitlines()[-1] != '1':
            raise RuntimeError('owned public config update differs')
        key_b = 'b' + run_id[:12]
        sql = ("INSERT INTO sys_oss_config (oss_config_id,config_key,access_key,secret_key,bucket_name,prefix,endpoint,domain_url,is_https,region,access_policy,status,ext1,create_dept,create_by,create_time,update_by,update_time,remark) "
            + f"SELECT {id_b}," + h(key_b) + ',' + h(access) + ',' + h(secret) + ',' + h(bucket_b)
            + ',prefix,' + h(endpoint) + ',' + "''"
            + ",'N',region,'0','N',ext1,create_dept,create_by,NOW(),update_by,NOW(),remark "
            + f"FROM sys_oss_config WHERE oss_config_id={id_a}; SELECT ROW_COUNT();")
        if count_sql(base, mysql_id, sql, 'config_b').splitlines()[-1] != '1':
            raise RuntimeError('owned second private config insertion differs')
        id_bad = 8_305_000_000_000_000_000 + secrets.randbelow(10_000_000_000)
        key_bad = 'x' + run_id[:12]
        sql = ("INSERT INTO sys_oss_config (oss_config_id,config_key,access_key,secret_key,bucket_name,prefix,endpoint,domain_url,is_https,region,access_policy,status,ext1,create_dept,create_by,create_time,update_by,update_time,remark) "
            + f"SELECT {id_bad}," + h(key_bad) + ',' + h(access) + ',' + h(secret) + ',' + h(bucket_b)
            + ',prefix,' + h(endpoint) + ',' + h('http://' + endpoint)
            + ",'N',region,'9','N',ext1,create_dept,create_by,NOW(),update_by,NOW(),remark "
            + f"FROM sys_oss_config WHERE oss_config_id={id_a}; SELECT ROW_COUNT();")
        if count_sql(base, mysql_id, sql, 'config_bad').splitlines()[-1] != '1':
            raise RuntimeError('owned wrong-policy config insertion differs')
        # A real object in the public bucket, with owned metadata and a valid ACTIVE state.
        public_id = 8_310_000_000_000_000_000 + secrets.randbelow(10_000_000_000)
        sql = ("INSERT INTO sys_oss (oss_id,file_name,original_name,file_suffix,url,ext1,create_dept,create_time,create_by,update_time,update_by,service,is_temp,expire_time,delete_state) "
            + f"VALUES ({public_id}," + h('public-' + run_id) + ',' + h('public-' + run_id)
            + ",'txt'," + h('public-' + run_id) + ",NULL,0,NOW(),1,NOW(),1,'image','N',NULL,'ACTIVE'); SELECT ROW_COUNT();")
        if count_sql(base, mysql_id, sql, 'public_row').splitlines()[-1] != '1':
            raise RuntimeError('owned public metadata insert differs')
        # Route every legacy example provider to the same owned count proxy;
        # a startup probe of any one of them must also break startup-zero.
        count_sql(base, mysql_id,
            'UPDATE sys_oss_config SET endpoint=' + h(endpoint)
            + ",domain_url='' WHERE config_key IN ('qiniu','aliyun','qcloud');",
            'sample_endpoints')
        bad_id = 8_315_000_000_000_000_000 + secrets.randbelow(10_000_000_000)
        missing_id = 8_316_000_000_000_000_000 + secrets.randbelow(10_000_000_000)
        for object_id, service in ((bad_id, key_bad), (missing_id, 'm' + run_id[:12])):
            sql = ("INSERT INTO sys_oss (oss_id,file_name,original_name,file_suffix,url,ext1,create_dept,create_time,create_by,update_time,update_by,service,is_temp,expire_time,delete_state) "
                + f"SELECT {object_id},file_name,original_name,file_suffix,url,ext1,create_dept,create_time,create_by,update_time,update_by,"
                + h(service) + ",is_temp,expire_time,delete_state FROM sys_oss WHERE oss_id="
                + str(public_id) + '; SELECT ROW_COUNT();')
            if count_sql(base, mysql_id, sql, 'negative_row').splitlines()[-1] != '1':
                raise RuntimeError('owned negative metadata insertion differs')
        control_id = base.owned_user_id(mysql_id, 'WTA', 'lookup_control')
        ordinary_id = base.owned_user_id(mysql_id, 'test', 'lookup_a')
        client_pk = base.owned_admin_client_id(mysql_id)
        base.create_owned_recipient_role(mysql_id, run_id, control_id, ordinary_id,
                                         client_pk, 'a')
        forbidden = one(base, mysql_id,
            'SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role_menu rm ON rm.role_id=ur.role_id '
            'JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE ur.user_id=' + str(ordinary_id)
            + " AND m.perms LIKE 'system:oss%';", 'ordinary_oss_perms')
        if forbidden:
            raise RuntimeError('owned ordinary user has OSS permission')
        base.rotate_owned_login(mysql_id, control_id, control_pass)
        base.rotate_owned_login(mysql_id, ordinary_id, ordinary_pass)
        phase = 'backend'
        backend_port = base.free_port()
        ports.append(backend_port)
        ui_server = ui_module.OwnedAdminDistServer(DIST_ROOT, backend_port)
        ui_port = ui_server.server_address[1]; ports.append(ui_port)
        overlay = base.isolated_config(mysql_port, redis_port, backend_port, ui_port,
                                       app_user, app_pass, redis_pass, run_dir)
        overlay.update({'springdoc.api-docs.enabled': True, 'openapi.enabled': True,
                        'message.enabled': True,
                        'management.endpoint.health.probes.enabled': True,
                        'management.endpoints.web.exposure.include': 'health,info',
                        'oss.readiness.refresh-interval': 'PT0.2S',
                        'oss.readiness.max-snapshot-age': 'PT1S',
                        'oss.readiness.diagnostic-objects.minio': canary_key,
                        'oss.readiness.diagnostic-objects.image': 'public-' + run_id,
                        'oss.readiness.diagnostic-objects.' + key_b: canary_key})
        overlay_path = run_dir / 'owned.yml'; private(overlay_path, json.dumps(overlay).encode())
        backend_log = run_dir / 'backend.raw.log'
        logfd = os.open(backend_log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        try:
            backend = subprocess.Popen(('java', '-Xms256m', '-Xmx1024m',
               '-Duser.home=' + str(run_dir), '-Djava.io.tmpdir=' + str(run_dir / 'tmp'),
               '-jar', str(base.ARTIFACT), '--spring.profiles.active=dev',
               '--spring.config.additional-location=file:' + str(overlay_path)),
               cwd=run_dir, stdout=logfd, stderr=subprocess.STDOUT, start_new_session=True,
               env=base.safe_env(TZ='Asia/Shanghai', TMPDIR=str(run_dir / 'tmp')))
        finally: os.close(logfd)
        groups.append(backend.pid)
        base.wait_http(backend_port, '/auth/code', backend, 240)
        wait_core_readiness(backend_port, overlay['spring.boot.admin.client.password'], backend)
        phase = 'startup_zero'
        startup_calls = proxy.snapshot_arrivals()
        report['counts']['startup_minio_calls'] = startup_calls
        if startup_calls != 0:
            raise RuntimeError('core startup made remote MinIO requests')
        phase = 'login'
        control_token = base.control_login(backend_port, 'WTA', control_pass, 'login_control')
        ordinary_token = base.control_login(backend_port, 'test', ordinary_pass, 'login_a')
        phase = 'private_a'
        dynamic_log_canaries = {}
        payload_a = b'T45-owned-private-A-' + run_id.encode()
        id_obj_a = upload(backend_port, control_token, payload_a, 't44a' + run_id,
                          proxy_port, dynamic_log_canaries, 'a')
        if count_sql(base, mysql_id, 'SELECT service FROM sys_oss WHERE oss_id=' + id_obj_a + ';', 'a_service') != 'minio':
            raise RuntimeError('private A object did not bind default A')
        key_a = count_sql(base, mysql_id, 'SELECT file_name FROM sys_oss WHERE oss_id=' + id_obj_a + ';', 'a_key')
        if not key_a.startswith('direct/general/') or '..' in key_a:
            raise RuntimeError('owned A object key differs')
        mc(base, app_mc, 'stat', 'app/' + bucket_a + '/' + key_a)
        download(backend_port, control_token, id_obj_a, payload_a, proxy_port, public=False,
                 dynamic_canaries=dynamic_log_canaries, signature_slot='a_get_signature_first')
        denied(backend_port, 'GET', '/resource/oss/' + id_obj_a + '/download-url', ordinary_token)
        phase = 'public'
        before_public, before_public_arrivals = proxy.snapshot_drained(3.0)
        public_diag_status, public_diag = api(backend_port, 'POST',
            f'/resource/oss/config/diagnose/{id_p}', control_token)
        after_public, after_public_arrivals = proxy.snapshot_drained(3.0)
        report['diagnostic'] = {'public_observed_provider_rows':
                                proxy_helper.safe_delta(before_public, after_public),
                                'public_arrivals': after_public_arrivals - before_public_arrivals}
        public_fact_summary = validate_fact_response(public_diag_status, public_diag, public=True)
        public_probe_rows = validate_probe_delta(proxy_helper, before_public, after_public,
            before_arrivals=before_public_arrivals, after_arrivals=after_public_arrivals,
            bucket_alias='public', public=True)
        download(backend_port, control_token, str(public_id), payload_public, proxy_port, public=True)
        report['diagnostic'].update({'public': public_fact_summary,
                                     'public_provider_rows': public_probe_rows,
                                     'public_get_did_not_require_diagnostic_success': True})
        phase = 'negative'
        # Synthetic status alteration is restricted to an owned copy of the real object row.
        id_inactive = 8_320_000_000_000_000_000 + secrets.randbelow(10_000_000_000)
        sql = (f"INSERT INTO sys_oss SELECT {id_inactive},file_name,original_name,file_suffix,url,ext1,"
               "create_dept,create_time,create_by,update_time,update_by,service,is_temp,expire_time,'PENDING' "
               f"FROM sys_oss WHERE oss_id={id_obj_a}; SELECT ROW_COUNT();")
        if count_sql(base, mysql_id, sql, 'inactive_copy').splitlines()[-1] != '1':
            raise RuntimeError('owned inactive row copy differs')
        status, value = api(backend_port, 'GET', '/resource/oss/' + str(id_inactive) + '/download-url', control_token)
        if status != 200 or value.get('code') == 200:
            raise RuntimeError('non-ACTIVE object was downloadable')
        for object_id in (bad_id, missing_id):
            status, value = api(backend_port, 'GET', '/resource/oss/' + str(object_id) + '/download-url', control_token)
            if status == 200 and value.get('code') == 200:
                raise RuntimeError('invalid or missing config was downloadable')
        before_bad_arrivals = proxy.snapshot_drained(3.0)[1]
        bad_diag_status, bad_diag = api(backend_port, 'POST',
            f'/resource/oss/config/diagnose/{id_bad}', control_token)
        if (bad_diag_status != 200 or bad_diag.get('code') != 200
            or (bad_diag.get('data') or {}).get('reason') != 'INVALID_ACCESS_POLICY'
            or proxy.snapshot_drained(3.0)[1] != before_bad_arrivals):
            raise RuntimeError('invalid policy diagnostic differs')
        report['diagnostic']['invalid_policy_fail_closed'] = True
        phase = 'diagnostic'
        denied(backend_port, 'POST', f'/resource/oss/config/diagnose/{id_a}', ordinary_token)
        before_diag, before_diag_arrivals = proxy.snapshot_drained(3.0)
        diag_started = time.monotonic()
        diag_status, diag = api(backend_port, 'POST', f'/resource/oss/config/diagnose/{id_a}', control_token)
        diag_elapsed_ms = int((time.monotonic() - diag_started) * 1000)
        after_diag, after_diag_arrivals = proxy.snapshot_drained(3.0)
        report['diagnostic']['private_a_observed_provider_rows'] = proxy_helper.safe_delta(
            before_diag, after_diag)
        report['diagnostic']['private_a_arrivals'] = after_diag_arrivals - before_diag_arrivals
        private_fact_summary = validate_fact_response(diag_status, diag, public=False)
        private_probe_rows = validate_probe_delta(proxy_helper, before_diag, after_diag,
            before_arrivals=before_diag_arrivals, after_arrivals=after_diag_arrivals,
            bucket_alias='private_a', public=False)
        report['diagnostic'].update({'ordinary_denied': True,
                                     'private_a': private_fact_summary,
                                     'private_a_provider_rows': private_probe_rows,
                                     'private_a_elapsed_ms': diag_elapsed_ms})
        # Reject invalid path IDs before any provider diagnostic call; retain only safe codes.
        invalid_ids = []
        before_invalid = proxy.snapshot_drained(3.0)[1]
        for label, raw_id in (('zero', '0'), ('negative', '-1'), ('non_numeric', 'not-a-number')):
            invalid_status, invalid_value = api(backend_port, 'POST',
                '/resource/oss/config/diagnose/' + raw_id, control_token)
            invalid_code = invalid_value.get('code')
            if type(invalid_code) is not int:
                raise RuntimeError('invalid diagnostic ID response shape differs')
            invalid_ids.append({'case': label, 'http_status': invalid_status,
                                'business_code': invalid_code})
            if invalid_status == 200 and invalid_code == 200:
                raise RuntimeError('invalid diagnostic ID was accepted')
        after_invalid = proxy.snapshot_drained(3.0)[1]
        if after_invalid != before_invalid:
            raise RuntimeError('invalid diagnostic ID reached provider')
        report['diagnostic']['invalid_id_rejections'] = invalid_ids
        audit = 0
        for _ in range(20):
            audit = one(base, mysql_id,
                'SELECT COUNT(*) FROM sys_oper_log WHERE oper_url LIKE '
                + base.compared_hexsql('/resource/oss/config/diagnose/%')
                + " AND request_method='POST' AND COALESCE(oper_param,'')=''"
                + " AND COALESCE(json_result,'')='';", 'diagnostic_audit')
            if audit >= 1: break
            time.sleep(.25)
        if audit < 1:
            raise RuntimeError('safe diagnostic audit absent')
        report['diagnostic']['safe_audit_count'] = audit
        for canary in (access, secret, minio_user, minio_pass):
            leaked = one(base, mysql_id,
                'SELECT COUNT(*) FROM sys_oper_log WHERE oper_url LIKE '
                + base.compared_hexsql('/resource/oss/config/diagnose/%')
                + ' AND (COALESCE(oper_param,\'\') LIKE CONCAT(\'%\',' + base.compared_hexsql(canary) + ',\'%\')'
                + ' OR COALESCE(json_result,\'\') LIKE CONCAT(\'%\',' + base.compared_hexsql(canary) + ',\'%\')'
                + ' OR COALESCE(error_msg,\'\') LIKE CONCAT(\'%\',' + base.compared_hexsql(canary) + ',\'%\'));',
                'diagnostic_secret_audit')
            if leaked:
                raise RuntimeError('owned diagnostic audit leaked credential material')
        report['diagnostic']['audit_secret_canaries'] = 0
        time.sleep(1.2)
        for health_path in ('/actuator/health/readiness', '/actuator/health/liveness',
                            '/actuator/health/ossdiagnostics'):
            basic = base64.b64encode(('owned-monitor:' + overlay['spring.boot.admin.client.password']).encode()).decode()
            health_status, health_raw = http_raw(backend_port, 'GET', health_path,
                headers={'Authorization': 'Basic ' + basic})
            if health_status not in (200, 503) or not health_raw.startswith(b'{'):
                raise RuntimeError('independent health group contract differs')
            health_data = json.loads(health_raw)
            if health_path.endswith(('readiness', 'liveness')):
                if health_status != 200 or health_data.get('status') != 'UP':
                    raise RuntimeError('core health did not remain UP')
                if health_path.endswith('readiness') and not {'db', 'redis'}.issubset(
                    (health_data.get('components') or {}).keys()):
                    raise RuntimeError('core readiness lost DB or Redis contributor')
            else:
                if health_data.get('status') not in ('UP', 'DOWN', 'UNKNOWN'):
                    raise RuntimeError('OSS diagnostic health status differs')
                reason = nested_reason(health_data, 'minio')
                if reason != 'STALE':
                    raise RuntimeError('expired diagnostic did not show STALE')
                report['diagnostic']['expired_health_reason'] = reason
        download(backend_port, control_token, id_obj_a, payload_a, proxy_port, public=False,
                 dynamic_canaries=dynamic_log_canaries, signature_slot='a_get_signature_stale')
        report['diagnostic']['expired_snapshot_did_not_block_private_download'] = True
        phase = 'switch_b'
        changed = success(backend_port, 'POST', '/resource/oss/config/changeStatus',
                          control_token, {'ossConfigId': str(id_b)})
        if changed not in (None, 1):
            raise RuntimeError('owned default switch result differs')
        defaults = count_sql(base, mysql_id,
            "SELECT config_key FROM sys_oss_config WHERE status='Y';", 'default_after_switch').splitlines()
        if defaults != [key_b]:
            raise RuntimeError('owned default switch did not select only B')
        phase = 'private_b'
        payload_b = b'T45-owned-private-B-' + run_id.encode()
        id_obj_b = upload(backend_port, control_token, payload_b, 't44b' + run_id,
                          proxy_port, dynamic_log_canaries, 'b')
        if count_sql(base, mysql_id, 'SELECT service FROM sys_oss WHERE oss_id=' + id_obj_b + ';', 'b_service') != key_b:
            raise RuntimeError('private B object did not bind new default B')
        key_b_obj = count_sql(base, mysql_id, 'SELECT file_name FROM sys_oss WHERE oss_id=' + id_obj_b + ';', 'b_key')
        if not key_b_obj.startswith('direct/general/') or '..' in key_b_obj:
            raise RuntimeError('owned B object key differs')
        mc(base, app_mc, 'stat', 'app/' + bucket_b + '/' + key_b_obj)
        download(backend_port, control_token, id_obj_b, payload_b, proxy_port, public=False,
                 dynamic_canaries=dynamic_log_canaries, signature_slot='b_get_signature')
        download(backend_port, control_token, id_obj_a, payload_a, proxy_port, public=False,
                 dynamic_canaries=dynamic_log_canaries, signature_slot='a_get_signature_after_switch')
        if count_sql(base, mysql_id, 'SELECT service FROM sys_oss WHERE oss_id=' + id_obj_a + ';', 'a_service_after') != 'minio':
            raise RuntimeError('old A object changed service after B switch')
        before_b, before_b_arrivals = proxy.snapshot_drained(3.0)
        b_status, b_diag = api(backend_port, 'POST',
                               f'/resource/oss/config/diagnose/{id_b}', control_token)
        after_b, after_b_arrivals = proxy.snapshot_drained(3.0)
        report['diagnostic']['private_b_observed_provider_rows'] = proxy_helper.safe_delta(
            before_b, after_b)
        report['diagnostic']['private_b_arrivals'] = after_b_arrivals - before_b_arrivals
        report['diagnostic']['private_b'] = validate_fact_response(b_status, b_diag, public=False)
        report['diagnostic']['private_b_provider_rows'] = validate_probe_delta(
            proxy_helper, before_b, after_b, before_arrivals=before_b_arrivals,
            after_arrivals=after_b_arrivals, bucket_alias='private_b', public=False)
        phase = 'openapi'
        status, raw = http_raw(backend_port, 'GET', '/v3/api-docs', control_token,
                               max_size=2_000_000)
        if status != 200:
            raise RuntimeError('live OpenAPI capture failed')
        openapi_path = run_dir / 'openapi.raw.json'; private(openapi_path, raw)
        parsed = json.loads(raw)
        paths = parsed.get('paths') if type(parsed) is dict else None
        required_schema_paths = ('/resource/oss/config/diagnose/{ossConfigId}',
                                 '/resource/message', '/resource/message/close')
        report['schema_capture'] = {'message_enabled': overlay['message.enabled'],
                                    'message_transport_default': 'sse',
                                    'required_paths_expected': list(required_schema_paths),
                                    'required_paths_present': [path for path in required_schema_paths
                                                               if type(paths) is dict and path in paths]}
        if len(report['schema_capture']['required_paths_present']) != len(required_schema_paths):
            raise RuntimeError('live OpenAPI required path absent')
        report['schema_capture'].update(validate_diagnostic_openapi(parsed))
        report['openapi'] = {'sha256': base.sha_file(openapi_path), 'bytes': len(raw),
                             'path_count': len(paths), 'private_capture': str(openapi_path)}
        phase = 'browser'
        # The final B source/JAR/dist are all checked above. The same owned DB
        # and MinIO identities now back real production Admin bytes in Chrome.
        ordinary_status, ordinary_value = api(backend_port, 'POST',
            f'/resource/oss/config/diagnose/{id_a}', ordinary_token)
        anonymous_status, anonymous_value = api(backend_port, 'POST',
            f'/resource/oss/config/diagnose/{id_a}', None)
        if (ordinary_status != 200 or ordinary_value.get('code') != 403
            or anonymous_status != 200 or anonymous_value.get('code') != 401):
            raise RuntimeError('ordinary or anonymous diagnostic denial differs')
        report['browser_authorization'] = {'ordinary_code': 403, 'anonymous_code': 401}
        ui_manifest = run_dir / 'ui-manifest.json'
        private(ui_manifest, json.dumps({'schema_version': 1, 'run_id': run_id,
            'private_key': 'minio', 'public_key': 'image',
            'private_id': str(id_a), 'public_id': str(id_p)}, separators=(',', ':')).encode())
        ui_thread = threading.Thread(target=ui_server.serve_forever, daemon=True)
        ui_thread.start()
        ui_status, ui_html = http_raw(ui_port, 'GET', '/login', max_size=2_000_000)
        if ui_status != 200 or b'<html' not in ui_html.lower():
            raise RuntimeError('owned Admin production dist unavailable')
        playwright_env = base.safe_env(T45_ADMIN_ORIGIN=f'http://127.0.0.1:{ui_port}',
            T45_UI_MANIFEST=str(ui_manifest), T45_RUN_ID=run_id,
            T45_CONTROL_USERNAME='WTA', T45_CONTROL_PASSWORD=control_pass,
            T45_ORDINARY_USERNAME='test', T45_ORDINARY_PASSWORD=ordinary_pass,
            TMPDIR=str(run_dir / 'tmp'), CI='1')
        playwright_argv = ('node', str(PLAYWRIGHT_CLI), 'test', str(UI_SPEC),
            '--config', str(UI_CONFIG), '--workers=1', '--reporter=json',
            '--output', str(run_dir / 'playwright-artifacts'))
        if any(secret in ' '.join(playwright_argv) for secret in
               (root_pass, app_pass, redis_pass, minio_user, minio_pass,
                access, secret, control_pass, ordinary_pass, control_token, ordinary_token)):
            raise RuntimeError('private value entered Chrome argv')
        chrome_stdout = run_dir / 'playwright.raw.json'
        chrome_stderr = run_dir / 'playwright.raw.stderr'
        with os.fdopen(os.open(chrome_stdout, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stdout, \
             os.fdopen(os.open(chrome_stderr, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stderr:
            playwright = subprocess.Popen(playwright_argv, cwd=ROOT / 'frontend',
                env=playwright_env, stdout=stdout, stderr=stderr, start_new_session=True)
            groups.append(playwright.pid)
            try:
                playwright.wait(timeout=420)
            except subprocess.TimeoutExpired:
                raise RuntimeError('owned Chrome timed out') from None
        raw_chrome = chrome_stdout.read_bytes()
        report['browser_raw_sha256'] = hashlib.sha256(raw_chrome).hexdigest()
        try:
            chrome_data = json.loads(raw_chrome)
        except (ValueError, UnicodeError):
            raise RuntimeError('Chrome reporter shape differs') from None
        chrome_stats = chrome_data.get('stats') or {}
        report['browser_counts_observed'] = {key: int(chrome_stats.get(key, 0))
            for key in ('expected', 'unexpected', 'skipped', 'flaky')}
        report['browser_exit_code'] = playwright.returncode
        report['browser'] = browser_report(chrome_data)
        if playwright.returncode != 0:
            raise RuntimeError('real Admin diagnostic Chrome gate failed')
        phase = 'postcheck'
        provider_rows = proxy_helper.safe_rows(proxy.snapshot_drained(3.0)[0])
        report['counts']['safe_provider_requests'] = provider_rows
        if sum(row['count'] for row in provider_rows if row['method'] == 'HEAD') < 2:
            raise RuntimeError('real uploads did not exercise provider HEAD')
        report['counts']['private_uploads'] = 2
        report['counts']['public_reads'] = 1
        report['counts']['unauthorized_denials'] = 2
        raw_backend = backend_log.read_bytes()
        report['backend_log_diagnostic'] = inspect_backend_log(raw_backend, {
            'mysql_root_password': root_pass,
            'mysql_app_password': app_pass,
            'redis_password': redis_pass,
            'minio_root_user': minio_user,
            'minio_root_password': minio_pass,
            'minio_app_access_key': access,
            'minio_app_secret_key': secret,
            'admin_control_password': control_pass,
            'ordinary_user_password': ordinary_pass,
            'monitor_password': overlay['spring.boot.admin.client.password'],
            **dynamic_log_canaries,
        }, (control_token, ordinary_token), run_dir)
        report['counts']['raw_backend_secret_canaries'] = report['backend_log_diagnostic']['known_canary_hits']
        if report['counts']['raw_backend_secret_canaries']:
            raise RuntimeError('owned backend raw log contains credential material')
        report['accepted'] = True
    except Exception as exc:
        report['failure'] = {'type': type(exc).__name__ if type(exc).__name__ in
                             ('RuntimeError', 'ValueError', 'KeyError', 'TimeoutExpired',
                              'OwnedMcFailure') else 'Exception',
                             'phase': phase if phase in PHASES else None}
        if type(exc) is RuntimeError and str(exc) in ['API JSON shape differs', 'HTTP response too large', 'OSS diagnostic health status differs', 'access URL shape differs', 'access policy mismatch', 'completed OSS ID shape differs', 'core health did not remain UP', 'core readiness HTTP contract differs', 'core readiness JSON shape differs', 'core readiness contributor shape differs', 'core readiness excluded DB or Redis', 'core readiness lost DB or Redis contributor', 'core readiness transient status differs', 'core startup made remote MinIO requests', 'credential-like argument forbidden', 'diagnostic enum shape differs', 'diagnostic public shape or authorization differs', 'expired diagnostic did not show STALE', 'full JAR hash differs', 'independent health group contract differs', 'invalid or missing config was downloadable', 'invalid policy diagnostic differs', 'invalid diagnostic ID response shape differs', 'invalid diagnostic ID was accepted', 'invalid diagnostic ID reached provider', 'live OpenAPI capture failed', 'live OpenAPI required path absent', 'missing public canary diagnostic differs', 'non-ACTIVE object was downloadable', 'non-local HTTP path', 'old A object changed service after B switch', 'owned A object key differs', 'owned API success contract failed', 'owned B object key differs', 'owned authorization denial differs', 'owned backend raw log contains credential material', 'backend log canary inventory differs', 'backend log canary shape differs', 'backend log sanitization incomplete', 'backend log line mapping differs', 'backend log diagnostic contains canary', 'backend log diagnostic contains unsafe context', 'backend log diagnostic directory unsafe', 'backend log diagnostic file mode unsafe', 'owned core exited before readiness', 'owned core readiness timed out', 'owned default config update differs', 'owned default switch did not select only B', 'owned default switch result differs', 'owned diagnostic audit leaked credential material', 'owned inactive row copy differs', 'owned negative metadata insertion differs', 'owned object ACL write was not AccessDenied', 'owned object bytes differ', 'owned ordinary user has OSS permission', 'owned presign response differs', 'owned presign signature shape differs', 'owned dynamic log canary slot differs', 'owned presigned HTTP contract failed', 'owned public config update differs', 'owned public metadata insert differs', 'owned schema table count differs', 'owned second private config insertion differs', 'owned single upload response differs', 'owned six-SQL initializer failed', 'owned wrong-policy config insertion differs', 'presigned URL escaped owned loopback', 'private A object did not bind default A', 'private B object did not bind new default B', 'private URL TTL differs', 'private URL lacks signature or expiry', 'private URL signature canary differs', 'private mc copy changed', 'private signature and response expiry differ', 'private signature time differs', 'public URL carried private signature or expiry', 'real uploads did not exercise provider HEAD', 'safe diagnostic audit absent', 'single-config diagnostic did not reach owned MinIO', 'single-config diagnostic touched another bucket', 'symlink in owned run directory', 'tracked owned runner changed', 'upload token shape differs', 'upstream response too large']:
            report["failure"]["fixed_reason"] = str(exc)
        if type(exc) is RuntimeError and str(exc) in (
            'private safe proxy changed', 'diagnostic timestamp shape differs',
            'unknown diagnostic was reported as verified or mismatch',
            'diagnostic fact set differs', 'diagnostic fact shape differs',
            'diagnostic fact provenance differs', 'diagnostic fact enum differs',
            'unreadable policy or ACL gained false certainty',
            'single-object anonymous fact differs',
            'diagnostic provider request boundary differs',
            'live diagnostic schema differs', 'provider request still in flight'):
            report['failure']['fixed_reason'] = str(exc)
        if type(exc) is RuntimeError and str(exc) in (
            'private Admin dist server changed', 'private Admin dist manifest differs',
            'Admin dist source or build proof differs', 'Admin dist build log proof differs',
            'Admin dist inventory differs', 'Admin dist build mode differs',
            'Chrome reporter shape differs', 'Chrome exact two-case counts differ',
            'Chrome exact case identity differs', 'private Chrome input changed',
            'ordinary or anonymous diagnostic denial differs',
            'owned Admin production dist unavailable', 'private value entered Chrome argv',
            'owned Chrome timed out', 'real Admin diagnostic Chrome gate failed'):
            report['failure']['fixed_reason'] = str(exc)
        if type(exc) is RuntimeError and str(exc) in ['Artifact is not the full admin bundle', 'Browser backend config is not loopback isolated', 'Container fails both exact owner/run label checks', 'Docker did not return a full container ID', 'Fresh owned schema already has A/B inbox recipients', 'Full admin bundle verification failed', 'Git inspection failed', 'Invalid owned control login stage', 'Invalid owned control request stage or path', 'Invalid owned online verification stage', 'Invalid owned recipient role', 'Invalid owned synthetic seed input', 'JAR differs from the expected full package artifact', 'JAR mtime is outside the full clean package proof', 'Owned Admin Client fixture differs', 'Owned BCrypt hash failed local verification', 'Owned HTTP process exited before readiness', 'Owned HTTP readiness timed out', 'Owned MinIO readiness timed out', 'Owned MySQL readiness timed out', 'Owned Notice/Intent version identity differs', 'Owned Redis password is not generated alphanumeric', 'Owned V1 seed anchor identity is invalid', 'Owned V1 seed anchor is missing, ambiguous or has no time', 'Owned V1 was already retracted before control request', 'Owned asynchronous login audit timed out', 'Owned audit Admin Client fields differ', 'Owned audit Admin Client is absent or ambiguous', 'Owned authenticated Redis readiness timed out', 'Owned container has a non-anonymous volume', 'Owned control and recipients are not distinct', 'Owned enabled fixture user is absent or ambiguous', 'Owned fixture login password update did not affect exactly one user', 'Owned issued token online identity differs', 'Owned login User-Agent does not match its fixed stage', 'Owned login audit arguments differ', 'Owned login audit baseline is invalid', 'Owned login audit result is invalid', 'Owned login audit row differs', 'Owned login password is not generated 24-character alphanumeric', 'Owned notice save did not create one exact draft', 'Owned real Notice did not persist retraction', 'Owned real Worker did not deliver exact V1 before deadline', 'Owned recipient Notice management permission result differs', 'Owned schema violates fresh no-supplier-call baseline', 'Owned six-SQL initializer failed', 'Owned six-SQL initializer inputs are missing', 'Owned volume absence could not be verified', 'Package proof is not a full clean backend package command', 'Package proof is not bound to this JAR', 'Package proof is not bound to this source', 'Package proof lacks UTC build bounds', 'Package proof lacks a bound successful build log', 'Package proof lacks a successful clean build boundary', 'Playwright case did not pass on its only attempt', 'Playwright case titles are missing or duplicated', 'Playwright exact case identity or attempt count differs', 'Playwright expected/unexpected/skipped/flaky counts differ', 'Playwright file, title or Chrome project differs', 'Playwright reporter did not identify exactly two cases', 'Playwright source attempts to persist authentication or network artifacts', 'Private value entered Java argv', 'Private value entered Playwright argv', 'Published port is not a random loopback binding', 'Real Playwright browser gate failed', 'Real V1 immutable delivery or retraction fence changed after Chrome', 'Real browser spec/config/Vite/Playwright inputs are missing', 'Real publication ID overlaps synthetic seed', 'Recipient role gained management permission or lacks inbox edges', 'Recipient role or owned Admin menu baseline differs', 'Source is not the exact expected clean HEAD', 'Synthetic ID base is outside its owned range']:
            report["failure"]["fixed_base_reason"] = str(exc)
        if isinstance(exc, OwnedSqlStep):
            report['failure']['sql'] = exc.details
        if isinstance(exc, base.OwnedSqlError):
            report['failure']['base_sql'] = exc.safe_details()
        if isinstance(exc, OwnedMcFailure):
            report['failure']['mc'] = exc.safe()
    finally:
        report['phase'] = phase if phase in PHASES else None
        try:
            if playwright is not None:
                live = base.stop_group(playwright)
                if live: report['cleanup_errors'].append('playwright_group_remains')
        except Exception: report['cleanup_errors'].append('playwright_group_cleanup_failed')
        try:
            live_profiles = cleanup_owned_profiles(run_dir / 'tmp')
            report['owned']['profile_processes_after_cleanup'] = len(live_profiles)
            if live_profiles: report['cleanup_errors'].append('owned_browser_profile_processes_remain')
        except Exception: report['cleanup_errors'].append('owned_browser_profile_cleanup_failed')
        if ui_server is not None:
            try:
                if not ui_server.wait_idle(5.0):
                    report['cleanup_errors'].append('ui_server_requests_remain')
                if ui_thread is not None:
                    ui_server.shutdown()
                ui_server.server_close()
                if ui_thread is not None: ui_thread.join(timeout=5)
                if ui_thread is not None and ui_thread.is_alive():
                    report['cleanup_errors'].append('ui_server_thread_remains')
            except Exception: report['cleanup_errors'].append('ui_server_cleanup_failed')
        try:
            if backend is not None:
                live = base.stop_group(backend)
                if live: report['cleanup_errors'].append('backend_group_remains')
        except Exception: report['cleanup_errors'].append('backend_group_cleanup_failed')
        if proxy is not None:
            try:
                proxy.shutdown(); proxy.server_close()
                if proxy_thread is not None: proxy_thread.join(timeout=5)
                if proxy_thread is not None and proxy_thread.is_alive():
                    report['cleanup_errors'].append('proxy_thread_remains')
            except Exception: report['cleanup_errors'].append('proxy_cleanup_failed')
        try:
            for found_id in base.owned_ids(run_id):
                base.assert_owned(found_id, run_id)
                for name in base.volume_names(found_id):
                    if name not in volumes: volumes.append(name)
            remaining, errors = base.cleanup_containers(run_id, containers)
            report['cleanup_errors'] += errors
            if remaining: report['cleanup_errors'].append('owned_containers_remain')
        except Exception: report['cleanup_errors'].append('owned_container_cleanup_failed')
        for name in volumes:
            try:
                if not base.volume_absent(name): report['cleanup_errors'].append('owned_volume_remains')
            except Exception: report['cleanup_errors'].append('owned_volume_check_failed')
        for port in ports:
            if not base.closed(port): report['cleanup_errors'].append('owned_loopback_port_open')
        try:
            if base.source_identity(args.expected_head) != report.get('source'):
                report['cleanup_errors'].append('source_after_changed')
            if base.sha_file(base.ARTIFACT) != args.expected_jar_sha256:
                report['cleanup_errors'].append('jar_after_changed')
        except Exception: report['cleanup_errors'].append('source_or_jar_after_failed')
        try:
            if (report.get('dist') is None or validate_dist_manifest(
                args.dist_manifest, args.expected_head, report['source']['tree']) != report['dist']):
                report['cleanup_errors'].append('dist_after_changed')
            if (base_sha(UI_SPEC) != UI_HASHES['spec']
                or base_sha(UI_CONFIG) != UI_HASHES['config']
                or base_sha(UI_SERVER) != UI_HASHES['server']
                or base_sha(PLAYWRIGHT_CLI) != report.get('playwright_cli_sha256')
                or validate_public_admin_env() != report.get('public_admin_env_sha256')):
                report['cleanup_errors'].append('private_browser_inputs_after_changed')
        except Exception: report['cleanup_errors'].append('dist_or_browser_after_check_failed')
        report['owned'].update({'containers': containers, 'volumes': volumes,
                                'ports': ports, 'process_groups': groups})
        # Remove every private credential, alias, raw log, temp and payload from
        # this new run directory. Preserve only sanitized result and OpenAPI.
        try:
            retained = {'result.json', 'openapi.raw.json'}
            for child in run_dir.iterdir():
                if child.is_symlink():
                    raise RuntimeError('symlink in owned run directory')
                if child.name == 'diagnostics':
                    if not child.is_dir() or stat.S_IMODE(child.stat().st_mode) != 0o700:
                        raise RuntimeError('private diagnostic directory differs')
                    entries = list(child.iterdir())
                    if len(entries) != 1 or entries[0].name != 'backend-log-summary.json' or entries[0].is_symlink() \
                       or not entries[0].is_file() or stat.S_IMODE(entries[0].stat().st_mode) != 0o600:
                        raise RuntimeError('private diagnostic file differs')
                    continue
                if child.name in retained:
                    continue
                if child.is_dir(): shutil.rmtree(child)
                else: child.unlink()
            report['owned']['private_ephemeral_files_removed'] = True
        except Exception:
            report['cleanup_errors'].append('private_ephemeral_cleanup_failed')
        if report['cleanup_errors']: report['accepted'] = False
        report_write(run_dir / 'result.json', report)
        print(json.dumps({'accepted': report['accepted'], 'run_dir': str(run_dir),
                          'result': str(run_dir / 'result.json'), 'phase': report['phase'],
                          'cleanup_errors': len(report['cleanup_errors'])}))
    return 0 if report['accepted'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
