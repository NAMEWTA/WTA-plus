#!/usr/bin/env python3
"""T-40 real notice-retraction browser gate on fresh, owned loopback infrastructure.

This file is standalone so it may be copied into frontend/e2e. Nothing starts
without --execute, an exact clean source SHA, a full-JAR SHA and package proof.
"""

import argparse
import datetime as dt
import hashlib
import http.client
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import socket
import string
import subprocess
import sys
import time
import zipfile

import bcrypt


ROOT = Path(__file__).resolve().parents[2] if Path(__file__).resolve().parents[1].name == 'frontend' else Path('/srv/WTA-plus')
RUN_ROOT = Path('/tmp/wta-t40/browser-runs')
ARTIFACT = ROOT / 'backend/wta-admin/target/wta-admin.jar'
INIT_SCRIPT = ROOT / 'release-artifacts/scripts/init-mysql-container.sh'
SQL_ROOT = ROOT / 'release-artifacts/docker/infrastructure/mysql/init'
SQL_NAMES = ('10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql',
             '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql')
TEST_PATH = 'frontend/e2e/notice-retraction-real.e2e.ts'
CONFIG_PATH = 'frontend/e2e/playwright.notice-retraction-real.config.ts'
TEST_TITLES = ('T-40 real published V1 survives retract in A personal off-page inbox',
               'T-40 real B foreign and absent inbox details fail with the same owner response')
OWNER = 'T-40-BROWSER'
DOCKER = ('docker', '--host', 'unix:///var/run/docker.sock')
MINIO_IMAGE = 'pgsty/minio@sha256:83885c27b3b5b673049e33ddf4029afe2c134fd51ce4309e65e4f39d3b9ca282'
REDIS_CONFIG_USER = 65534
ADMIN_CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e'
CHROME_CONTROL_UA = ('Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 '
                     '(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36')
MYSQL_STAGES = frozenset(('rotate_login', 'baseline_tables', 'baseline_outboxes',
    'baseline_external', 'baseline_accounts', 'baseline_oss', 'lookup_a', 'lookup_b',
    'lookup_control', 'lookup_client', 'counts_before', 'counts_seed', 'counts_after',
    'seed_insert', 'role_existing', 'role_menus', 'role_collision', 'role_insert',
    'role_effective', 'role_denied', 'notice_lookup', 'real_delivery', 'real_identity', 'real_after',
    'notice_retracted', 'notice_after', 'audit_client', 'audit_baseline', 'audit_login'))
LOGIN_STAGES = frozenset(('login_control', 'login_a', 'login_b', 'login_chrome'))
ONLINE_STAGES = frozenset(('online_control', 'online_a', 'online_b', 'online_chrome'))
HTTP_STAGES = LOGIN_STAGES | frozenset(('notice_save', 'notice_publish', 'notice_retract',
                                       'recipient_notice_denied')) | ONLINE_STAGES
RUN_PHASES = frozenset(('setup', 'backend_probe', 'login_control', 'login_a', 'login_b',
                        'login_chrome', 'notice_control', 'recipient_permission', 'seed', 'vite_probe',
                        'browser', 'post_browser'))
CONTROL_FAILURE_KINDS = frozenset(('transport', 'response_too_large', 'invalid_json',
                                   'invalid_shape', 'login_rejected', 'token_missing',
                                   'action_rejected'))


class OwnedSqlError(RuntimeError):
    """Retain only allowlisted stage and numeric MySQL diagnostics."""

    def __init__(self, stage, exit_code, number=None, sqlstate=None):
        self.stage = stage
        self.exit_code = exit_code
        self.number = number
        self.sqlstate = sqlstate
        super().__init__('Owned SQL operation failed at ' + stage)

    def safe_details(self):
        return {'stage': self.stage, 'exit_code': self.exit_code,
                'mysql_error_number': self.number, 'sqlstate': self.sqlstate}


def safe_http_status(value):
    return value if type(value) is int and 100 <= value <= 599 else None


def safe_business_code(value):
    return value if type(value) is int and -999_999 <= value <= 999_999 else None


class OwnedControlFailure(RuntimeError):
    """Only fixed stages/kinds and bounded numbers may leave the HTTP control path."""

    def __init__(self, stage, kind, http_status=None, business_code=None):
        self.stage = stage if stage in HTTP_STAGES else None
        self.kind = kind if kind in CONTROL_FAILURE_KINDS else 'transport'
        self.http_status = safe_http_status(http_status)
        self.business_code = safe_business_code(business_code)
        super().__init__('Owned control HTTP failed')

    def safe_details(self):
        return {'stage': self.stage, 'kind': self.kind,
                'http_status': self.http_status, 'business_code': self.business_code}


def safe_env(**updates):
    allowed = ('PATH', 'HOME', 'LANG', 'LC_ALL', 'JAVA_HOME', 'TMPDIR', 'PLAYWRIGHT_BROWSERS_PATH')
    env = {name: os.environ[name] for name in allowed if name in os.environ}
    env.update(updates)
    return env


def run(argv, *, cwd=ROOT, data=None, env=None, timeout=180):
    return subprocess.run(argv, cwd=cwd, input=data, stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE, env=env if env is not None else safe_env(),
                          timeout=timeout, check=False)


def output(result):
    return result.stdout.decode('utf-8', errors='replace').strip()


def git(*args):
    result = run(('git', *args), timeout=30)
    if result.returncode:
        raise RuntimeError('Git inspection failed')
    return output(result)


def source_identity(expected):
    head = git('rev-parse', 'HEAD')
    dirty = git('status', '--porcelain', '--untracked-files=all')
    if head != expected or dirty:
        raise RuntimeError('Source is not the exact expected clean HEAD')
    return {'head': head, 'tree': git('rev-parse', 'HEAD^{tree}'), 'clean': True}


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


def runner_source_line(exc):
    """Retain only an in-range line in this fixed runner, never a traceback string."""
    source = Path(__file__).resolve()
    maximum = len(source.read_text().splitlines())
    location = None
    cursor = exc.__traceback__
    while cursor is not None:
        if Path(cursor.tb_frame.f_code.co_filename).resolve() == source \
            and type(cursor.tb_lineno) is int and 1 <= cursor.tb_lineno <= maximum:
            location = cursor.tb_lineno
        cursor = cursor.tb_next
    return location


def failed_backend_exit_code(processes):
    """Inspect the backend before cleanup sends TERM; never infer from cleanup exit."""
    for name, process in processes:
        if name == 'backend':
            code = process.poll()
            return code if type(code) is int and -255 <= code <= 255 else None
    return None


def safe_failure(exc, phase=None):
    """Arbitrary exception text may contain a temporary bearer or request body."""
    name = type(exc).__name__
    allowed = {'RuntimeError', 'TimeoutExpired', 'OSError', 'ValueError', 'KeyError',
               'TypeError', 'AssertionError', 'JSONDecodeError', 'HTTPException',
               'OwnedControlFailure', 'OwnedSqlError'}
    result = {'error_type': name if name in allowed else 'Exception',
              'error': 'T40 owned browser gate failed',
              'phase': phase if phase in RUN_PHASES else None,
              'runner_line': runner_source_line(exc)}
    if isinstance(exc, OwnedControlFailure):
        result['control_failure'] = exc.safe_details()
    return result


def random_password():
    alphabet = string.ascii_letters + string.digits
    return ''.join(secrets.choice(alphabet) for _ in range(40))


def random_login_password():
    """Use a separate credential within PasswordLoginBody's 5–30 character bound."""
    alphabet = string.ascii_letters + string.digits
    return ''.join(secrets.choice(alphabet) for _ in range(24))


def owned_bcrypt_hash(password):
    """Use the application's supported $2a$ variant, without external argv."""
    if not re.fullmatch(r'[A-Za-z0-9]{24}', password):
        raise RuntimeError('Owned login password is not generated 24-character alphanumeric')
    encoded = bcrypt.hashpw(password.encode('ascii'), bcrypt.gensalt(rounds=10, prefix=b'2a'))
    if not bcrypt.checkpw(password.encode('ascii'), encoded):
        raise RuntimeError('Owned BCrypt hash failed local verification')
    return encoded.decode('ascii')


def rotate_owned_login(cid, user_id, password):
    password_hash = owned_bcrypt_hash(password)
    changed = mysql(cid, 'UPDATE sys_user SET password=' + hexsql(password_hash)
                    + f" WHERE user_id={user_id} AND status='0' AND del_flag='0'; SELECT ROW_COUNT();",
                    stage='rotate_login')
    if changed != '1':
        raise RuntimeError('Owned fixture login password update did not affect exactly one user')


def docker(*args, data=None, timeout=180):
    result = run((*DOCKER, *args), data=data, timeout=timeout)
    if result.returncode:
        raise RuntimeError('Owned Docker operation failed: ' + ' '.join(args[:2]))
    return output(result)


def full_id(value):
    if not re.fullmatch(r'[0-9a-f]{64}', value):
        raise RuntimeError('Docker did not return a full container ID')
    return value


def owned_ids(run_id):
    found = docker('ps', '-aq', '--no-trunc', '--filter', 'label=namewta.test.owner=' + OWNER,
                   '--filter', 'label=namewta.test.run=' + run_id)
    return [full_id(item) for item in found.splitlines()] if found else []


def assert_owned(cid, run_id):
    labels = json.loads(docker('inspect', '--format', '{{json .Config.Labels}}', cid))
    if labels.get('namewta.test.owner') != OWNER or labels.get('namewta.test.run') != run_id:
        raise RuntimeError('Container fails both exact owner/run label checks')


def mapped_port(cid, inside):
    binding = docker('port', cid, inside + '/tcp')
    host, separator, port = binding.rpartition(':')
    if not separator or host != '127.0.0.1' or not port.isdigit():
        raise RuntimeError('Published port is not a random loopback binding')
    return int(port)


def volume_names(cid):
    mounts = json.loads(docker('inspect', '--format', '{{json .Mounts}}', cid))
    names = []
    for mount in mounts:
        if mount.get('Type') == 'volume':
            name = mount.get('Name', '')
            if not re.fullmatch(r'[0-9a-f]{64}', name):
                raise RuntimeError('Owned container has a non-anonymous volume')
            names.append(name)
    return names


def volume_absent(name):
    result = run((*DOCKER, 'volume', 'inspect', name), timeout=30)
    if result.returncode == 0:
        return False
    if (result.returncode == 1 and name.encode('ascii') in result.stderr
            and re.search(rb'\bno such volume\b', result.stderr, re.IGNORECASE)):
        return True
    raise RuntimeError('Owned volume absence could not be verified')


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
                docker('rm', '-fv', cid, timeout=60)
            except Exception:
                errors.append('owned_container_remove_failed')
        remaining = owned_ids(run_id)
        if remaining:
            errors.append('owned_containers_remain')
    except Exception:
        errors.append('owned_container_discovery_or_verification_failed')
    return remaining, errors


def closed(port):
    with socket.socket() as client:
        client.settimeout(.3)
        return client.connect_ex(('127.0.0.1', port)) != 0


def free_port():
    with socket.socket() as server:
        server.bind(('127.0.0.1', 0))
        return server.getsockname()[1]


def live_group_members(pgid):
    members = []
    for entry in Path('/proc').iterdir():
        if not entry.name.isdigit():
            continue
        try:
            fields = (entry / 'stat').read_text().rsplit(')', 1)[1].split()
        except (FileNotFoundError, ProcessLookupError):
            continue
        if len(fields) >= 4 and fields[0] not in ('Z', 'X') and int(fields[2]) == pgid and int(fields[3]) == pgid:
            members.append(int(entry.name))
    return members


def stop_group(proc):
    for signum, seconds in ((signal.SIGTERM, 15), (signal.SIGKILL, 10)):
        if not live_group_members(proc.pid):
            proc.poll()
            return []
        try:
            os.killpg(proc.pid, signum)
        except ProcessLookupError:
            pass
        end = time.monotonic() + seconds
        while time.monotonic() < end:
            if not live_group_members(proc.pid):
                proc.poll()
                return []
            time.sleep(.1)
    remaining = live_group_members(proc.pid)
    proc.poll()
    return remaining


def mysql(cid, sql, *, stage):
    if stage not in MYSQL_STAGES:
        raise ValueError('Owned SQL stage is not allowlisted')
    if isinstance(sql, str):
        sql = sql.encode('utf-8')
    command = (*DOCKER, 'exec', '-i', cid, 'sh', '-c',
               'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch --skip-column-names --database="$1"',
               'sh', 'wta-plus')
    try:
        result = run(command, data=sql, timeout=180)
    except subprocess.TimeoutExpired:
        raise OwnedSqlError(stage, None) from None
    if result.returncode:
        match = re.search(rb'\bERROR\s+([0-9]{1,5})\s+\(([A-Z0-9]{5})\)', result.stderr)
        raise OwnedSqlError(stage, result.returncode,
                            int(match.group(1)) if match else None,
                            match.group(2).decode('ascii') if match else None)
    return output(result)


def wait_mysql(cid, seconds=120):
    command = (*DOCKER, 'exec', cid, 'sh', '-c',
               'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqladmin ping -h 127.0.0.1 -uroot --silent')
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        result = run(command, timeout=12)
        if result.returncode == 0:
            return
        time.sleep(1)
    raise RuntimeError('Owned MySQL readiness timed out')


def redis_config(password):
    if not re.fullmatch(r'[A-Za-z0-9]{40}', password):
        raise RuntimeError('Owned Redis password is not generated alphanumeric')
    return ('bind 0.0.0.0\nprotected-mode yes\nport 6379\ndir /tmp\n'
            'save ""\nappendonly no\nrequirepass ' + password + '\n').encode()


def wait_redis(cid, seconds=45):
    command = (*DOCKER, 'exec', cid, 'sh', '-c',
               'REDISCLI_AUTH="$(sed -n "s/^requirepass //p" /etc/redis/redis.conf)" exec redis-cli ping')
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        result = run(command, timeout=12)
        if result.returncode == 0 and output(result) == 'PONG':
            return
        time.sleep(1)
    raise RuntimeError('Owned authenticated Redis readiness timed out')


def wait_minio(port, seconds=90):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        client = http.client.HTTPConnection('127.0.0.1', port, timeout=3)
        try:
            client.request('GET', '/minio/health/live')
            response = client.getresponse()
            response.read(1024)
            if response.status == 200:
                return
        except (OSError, http.client.HTTPException):
            pass
        finally:
            client.close()
        time.sleep(1)
    raise RuntimeError('Owned MinIO readiness timed out')


def wait_http(port, path, proc, seconds):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        if proc.poll() is not None:
            raise RuntimeError('Owned HTTP process exited before readiness')
        client = http.client.HTTPConnection('127.0.0.1', port, timeout=3)
        try:
            client.request('GET', path)
            response = client.getresponse()
            response.read(1024 * 1024)
            if response.status == 200:
                return
        except (OSError, http.client.HTTPException):
            pass
        finally:
            client.close()
        time.sleep(1)
    raise RuntimeError('Owned HTTP readiness timed out')


def validate_package_proof(proof, expected_head, expected_tree, expected_jar_sha, actual_size):
    command = ['./mvnw', '-B', '-ntp', 'clean', 'package', '-DskipTests']
    if proof.get('command') not in (command, command + ['-Pbundle-full']):
        raise RuntimeError('Package proof is not a full clean backend package command')
    if proof.get('cwd') != 'backend' or proof.get('exit_code') != 0 or proof.get('source_clean_at_build') is not True:
        raise RuntimeError('Package proof lacks a successful clean build boundary')
    if proof.get('source_head') != expected_head or proof.get('source_tree') != expected_tree:
        raise RuntimeError('Package proof is not bound to this source')
    artifact = proof.get('artifact') or {}
    if (artifact.get('path') != 'backend/wta-admin/target/wta-admin.jar'
        or artifact.get('sha256') != expected_jar_sha or artifact.get('size_bytes') != actual_size):
        raise RuntimeError('Package proof is not bound to this JAR')
    log = proof.get('build_log') or {}
    path = Path(log.get('path', ''))
    if (not path.is_absolute() or not path.is_file()
        or not re.fullmatch(r'[0-9a-f]{64}', str(log.get('sha256', '')))
        or sha_file(path) != log['sha256'] or b'BUILD SUCCESS' not in path.read_bytes()[-16384:]):
        raise RuntimeError('Package proof lacks a bound successful build log')
    try:
        start = dt.datetime.fromisoformat(proof['started_utc'].replace('Z', '+00:00'))
        finish = dt.datetime.fromisoformat(proof['finished_utc'].replace('Z', '+00:00'))
        artifact_time = dt.datetime.fromtimestamp(ARTIFACT.stat().st_mtime, dt.timezone.utc)
    except (KeyError, TypeError, ValueError) as exc:
        raise RuntimeError('Package proof lacks UTC build bounds') from exc
    if (start.tzinfo is None or finish.tzinfo is None or start > finish
        or not start <= artifact_time <= finish + dt.timedelta(seconds=2)):
        raise RuntimeError('JAR mtime is outside the full clean package proof')


def validate_full_jar(path):
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
    required = ('wta-system', 'wta-notify', 'wta-third', 'wta-sso', 'wta-profile-person',
                'wta-profile-enterprise', 'wta-job', 'wta-ai', 'wta-demo', 'wta-workflow')
    missing = [module for module in required if not any(
        re.fullmatch(r'BOOT-INF/lib/' + re.escape(module) + r'-[0-9][^/]*\.jar', name)
        for name in names)]
    if missing or any('BOOT-INF/lib/snail-ai-' in name for name in names):
        raise RuntimeError('Artifact is not the full admin bundle')
    result = run(('bash', 'scripts/ci/verify-admin-bundle.sh', 'full'), timeout=90)
    if result.returncode:
        raise RuntimeError('Full admin bundle verification failed')
    return list(required)


def hexsql(value):
    return 'CONVERT(0x' + value.encode('utf-8').hex() + ' USING utf8mb4)'


def compared_hexsql(value):
    """Match the owned schema's general_ci only for string comparisons."""
    return hexsql(value) + ' COLLATE utf8mb4_general_ci'


def owned_user_id(cid, username, stage):
    rows = mysql(cid, 'SELECT user_id FROM sys_user WHERE user_name='
                 + compared_hexsql(username) + " AND status='0' AND del_flag='0';",
                 stage=stage).splitlines()
    if len(rows) != 1 or not rows[0].isdigit():
        raise RuntimeError('Owned enabled fixture user is absent or ambiguous')
    return int(rows[0])


def owned_admin_client_id(cid):
    rows = mysql(cid, 'SELECT id FROM sys_client WHERE client_id='
                 + compared_hexsql(ADMIN_CLIENT_ID) + " AND status='0' AND del_flag='0';",
                 stage='lookup_client').splitlines()
    if len(rows) != 1 or not rows[0].isdigit():
        raise RuntimeError('Owned Admin Client fixture differs')
    return int(rows[0])


def create_owned_recipient_role(cid, run_id, control_user, recipient_user, client_pk, label):
    """Grant one ordinary recipient only the Admin inbox directory and read edges."""
    if label not in ('a', 'b') or control_user == recipient_user:
        raise RuntimeError('Invalid owned recipient role')
    role_id = 8_200_000_000_000_000_000 + secrets.randbelow(100_000_000_000_000)
    permissions = (2100600000000000001, 2100600000000000040,
                   2100600000000000041, 2100600000000000042)
    existing = int(mysql(cid, f'SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r '
                         f'ON r.role_id=ur.role_id WHERE ur.user_id={recipient_user} '
                         f"AND r.client_id={client_pk} AND r.status='0' AND r.del_flag='0';",
                         stage='role_existing'))
    eligible = int(mysql(cid, 'SELECT COUNT(*) FROM sys_menu WHERE menu_id IN ('
                         + ','.join(str(item) for item in permissions)
                         + f") AND client_id={client_pk} AND status='0';", stage='role_menus'))
    collision = int(mysql(cid, f'SELECT COUNT(*) FROM sys_role WHERE role_id={role_id};',
                          stage='role_collision'))
    if existing or eligible != 4 or collision:
        raise RuntimeError('Recipient role or owned Admin menu baseline differs')
    role_key = 't40_' + label + '_' + run_id
    statement = (
        'START TRANSACTION;'
        'INSERT INTO sys_role(role_id,client_id,role_name,role_key,role_sort,data_scope,'
        'menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark) VALUES '
        f'({role_id},{client_pk},{hexsql("T40 Own " + label)},{hexsql(role_key)},99,'
        f"'5',1,1,'0','0',{control_user},NOW(),{hexsql('T40 owned browser only')});"
        f'INSERT INTO sys_user_role(user_id,role_id) VALUES ({recipient_user},{role_id});'
        'INSERT INTO sys_role_menu(role_id,menu_id) VALUES '
        + ','.join(f'({role_id},{menu_id})' for menu_id in permissions) + ';COMMIT;')
    mysql(cid, statement, stage='role_insert')
    effective = int(mysql(cid, 'SELECT COUNT(DISTINCT rm.menu_id) FROM sys_user_role ur '
                          'JOIN sys_role r ON r.role_id=ur.role_id '
                          'JOIN sys_role_menu rm ON rm.role_id=r.role_id '
                          'JOIN sys_menu m ON m.menu_id=rm.menu_id '
                          f'WHERE ur.user_id={recipient_user} AND r.role_id={role_id} '
                          f"AND r.client_id={client_pk} AND r.data_scope='5' "
                          "AND r.status='0' AND r.del_flag='0' "
                          f"AND m.client_id={client_pk} AND m.status='0' "
                          'AND rm.menu_id IN (' + ','.join(str(item) for item in permissions) + ');',
                          stage='role_effective'))
    denied = int(mysql(cid, 'SELECT COUNT(*) FROM sys_user_role ur '
                       'JOIN sys_role r ON r.role_id=ur.role_id '
                       'LEFT JOIN sys_role_menu rm ON rm.role_id=r.role_id '
                       'LEFT JOIN sys_menu m ON m.menu_id=rm.menu_id '
                       f'WHERE ur.user_id={recipient_user} AND r.client_id={client_pk} '
                       "AND r.status='0' AND r.del_flag='0' "
                       "AND (r.role_key='superadmin' OR m.perms LIKE 'notify:notice:%');",
                       stage='role_denied'))
    if effective != 4 or denied:
        raise RuntimeError('Recipient role gained management permission or lacks inbox edges')
    return role_id


def seed_plan(run_id, a_user, b_user, v1_message_id, notice_id, title, content, base=None):
    """Synthetic supplemental inbox rows only; V1 must already be Worker-delivered."""
    if (not re.fullmatch(r'[0-9a-f]{16}', run_id)
        or not all(isinstance(value, int) and value > 0
                   for value in (a_user, b_user, v1_message_id, notice_id))
        or a_user == b_user):
        raise RuntimeError('Invalid owned synthetic seed input')
    if base is None:
        base = 8_000_000_000_000_000_000 + secrets.randbelow(100_000_000_000_000)
    if not 8_000_000_000_000_000_000 <= base < 8_000_100_000_000_000_000:
        raise RuntimeError('Synthetic ID base is outside its owned range')
    filler_ids = [base + index for index in range(1, 21)]
    legacy_id, b_id, absent_id = base + 21, base + 22, base + 23
    if v1_message_id in filler_ids + [legacy_id, b_id, absent_id]:
        raise RuntimeError('Real publication ID overlaps synthetic seed')
    recipient_base = base + 100_000_000_000_000_000
    legacy_title = f'T40 {run_id} legacy owner'
    legacy_content = f'T40 {run_id} legacy full body'
    b_title = f'T40 {run_id} B control'
    b_content = f'T40 {run_id} B full body'
    legacy_notice = base + 24
    messages = [(mid, f'T40 {run_id} filler {index:02d}', f'T40 {run_id} filler body {index:02d}', None)
                for index, mid in enumerate(filler_ids, 1)]
    messages.extend(((legacy_id, legacy_title, legacy_content,
                      f'/notify/notice?noticeId={legacy_notice}'),
                     (b_id, b_title, b_content, None)))
    sql = ['START TRANSACTION;', 'SET @stamp = DATE_ADD(NOW(0), INTERVAL 1 SECOND);',
           'INSERT INTO notify_message(message_id,category,channels_json,type,source,title,message,content,path,create_by,create_time) VALUES',
           ',\n'.join(f"({mid},'notice',JSON_ARRAY('IN_APP'),'NOTICE','NOTICE',"
                      f"{hexsql(row_title)},{hexsql(row_title)},{hexsql(body)},"
                      f"{hexsql(path) if path else 'NULL'},{a_user},@stamp)"
                      for mid, row_title, body, path in messages) + ';',
           'INSERT INTO notify_message_recipient(message_recipient_id,message_id,user_id,seen_time,read_time,create_by,create_time) VALUES',
           ',\n'.join(f'({recipient_base + index},{mid},{a_user if mid != b_id else b_user},'
                      f'NULL,NULL,{a_user},@stamp)'
                      for index, (mid, _, _, _) in enumerate(messages, 1)) + ';', 'COMMIT;']
    manifest = {
        'schema_version': 1, 'run_id': run_id,
        'v1': {'noticeId': str(notice_id), 'messageId': str(v1_message_id),
               'title': title, 'content': content,
               'path': f'/notify/inbox?messageId={v1_message_id}'},
        'legacy': {'messageId': str(legacy_id), 'title': legacy_title,
                   'content': legacy_content, 'noticeId': str(legacy_notice)},
        'bControl': {'messageId': str(b_id), 'title': b_title, 'content': b_content},
        'absentId': str(absent_id),
        'synthetic_filler_count': 20,
        'a_user_id': str(a_user), 'b_user_id': str(b_user)}
    return '\n'.join(sql).encode(), manifest


def verify_real_delivery(cid, notice_id, a_user, b_user, *, stage):
    """Read-only exact publication proof; no direct V1 message/recipient insertion."""
    key = f'notice-published:{notice_id}:1'
    intent = mysql(cid, 'SELECT intent_id FROM notify_intent WHERE app_id=\'notify\' '
                   f'AND idempotency_key={compared_hexsql(key)};', stage=stage).splitlines()
    if len(intent) != 1 or not re.fullmatch(r'[1-9][0-9]*', intent[0]):
        return None
    message_id = int(intent[0])
    deep = f'/notify/inbox?messageId={message_id}'
    facts = mysql(cid, 'SELECT '
                  f'(SELECT COUNT(*) FROM notify_notice_snapshot WHERE notice_id={notice_id} '
                  'AND snapshot_version=1 AND snapshot_id<>0 AND path_snapshot='
                  f'{compared_hexsql(deep)}),'
                  f'(SELECT COUNT(*) FROM notify_intent WHERE intent_id={message_id} '
                  f'AND biz_id={compared_hexsql(str(notice_id))} AND path_snapshot={compared_hexsql(deep)} '
                  f'AND JSON_UNQUOTE(JSON_EXTRACT(template_params_json,\'$.path\'))={compared_hexsql(deep)}),'
                  f'(SELECT COUNT(*) FROM notify_outbox WHERE intent_id={message_id} AND status=\'DONE\'),'
                  f'(SELECT COUNT(*) FROM notify_delivery WHERE intent_id={message_id} '
                  'AND channel=\'IN_APP\' AND status=\'DELIVERED\'),'
                  f'(SELECT COUNT(*) FROM notify_message WHERE message_id={message_id}),'
                  f'(SELECT COUNT(*) FROM notify_message_recipient WHERE message_id={message_id} '
                  f'AND user_id={a_user}),'
                  f'(SELECT COUNT(*) FROM notify_message_recipient WHERE message_id={message_id} '
                  f'AND user_id={b_user});', stage=stage).split('\t')
    if facts != ['1', '1', '1', '1', '1', '1', '0']:
        return None
    return message_id


def positive_int64(value):
    """Accept only Java int or canonical ASCII positive decimal text within signed long."""
    if type(value) is int:
        number = value
    elif type(value) is str and re.fullmatch(r'[1-9][0-9]{0,18}', value):
        number = int(value)
    else:
        return None
    return number if 1 <= number <= 9_223_372_036_854_775_807 else None


def notice_version_fact(cid, notice_id, message_id, *, stage):
    snapshot_id = mysql(cid, f'SELECT snapshot_id FROM notify_notice_snapshot '
                        f'WHERE notice_id={notice_id} AND snapshot_version=1;', stage=stage)
    raw = mysql(cid, f'SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata_json,\'$.noticeVersion\')) '
                f'FROM notify_intent WHERE intent_id={message_id};', stage=stage)
    try:
        marker = json.loads(raw)
    except (ValueError, TypeError):
        marker = None
    expected_notice = positive_int64(notice_id)
    expected_snapshot = positive_int64(snapshot_id)
    if (expected_notice is None or expected_snapshot is None or not isinstance(marker, dict)
        or positive_int64(marker.get('noticeId')) != expected_notice
        or positive_int64(marker.get('snapshotId')) != expected_snapshot
        or positive_int64(marker.get('version')) != 1
        or type(marker.get('retracted')) is not bool):
        raise RuntimeError('Owned Notice/Intent version identity differs')
    return marker


def verify_seed(cid, manifest, *, stage):
    """One real V1 plus 22 synthetic rows; check owner order without reading private bodies."""
    v1 = int(manifest['v1']['messageId'])
    a_user, b_user = int(manifest['a_user_id']), int(manifest['b_user_id'])
    legacy_id = int(manifest['legacy']['messageId'])
    first_synthetic, last_synthetic = legacy_id - 20, int(manifest['bControl']['messageId'])
    absent_id = int(manifest['absentId'])
    rows = mysql(cid, 'SELECT r.message_id FROM notify_message_recipient r '
                 f'WHERE r.user_id={a_user} ORDER BY r.create_time DESC,r.message_id DESC '
                 'LIMIT 20;', stage=stage).splitlines()
    counts = mysql(cid, 'SELECT '
                   f'(SELECT COUNT(*) FROM notify_message_recipient WHERE user_id={a_user}),'
                   f'(SELECT COUNT(*) FROM notify_message_recipient WHERE user_id={b_user}),'
                   f'(SELECT COUNT(*) FROM notify_message WHERE message_id BETWEEN '
                   f'{first_synthetic} AND {last_synthetic}),'
                   f'(SELECT COUNT(*) FROM notify_message WHERE message_id={v1}),'
                   f'(SELECT COUNT(*) FROM notify_message WHERE message_id={absent_id});',
                   stage=stage).split('\t')
    if (counts != ['22', '1', '22', '1', '0'] or len(rows) != 20 or str(v1) in rows
        or manifest['legacy']['messageId'] not in rows[:10]):
        raise RuntimeError('Owned synthetic inbox placement or owner counts differ')
    return {'a_rows': int(counts[0]), 'b_rows': int(counts[1]),
            'synthetic_rows': int(counts[2]), 'v1_off_page_one': True,
            'legacy_in_top_ten': True}


def verify_after_browser(cid, manifest):
    notice_id = int(manifest['v1']['noticeId'])
    a_user, b_user = int(manifest['a_user_id']), int(manifest['b_user_id'])
    message_id = verify_real_delivery(cid, notice_id, a_user, b_user, stage='real_after')
    lifecycle = mysql(cid, f'SELECT lifecycle FROM notify_notice WHERE notice_id={notice_id};',
                      stage='notice_after')
    marker = notice_version_fact(cid, notice_id, message_id, stage='notice_after') if message_id else None
    if (message_id != int(manifest['v1']['messageId']) or lifecycle != 'RETRACTED'
        or not isinstance(marker, dict) or marker.get('retracted') is not True):
        raise RuntimeError('Real V1 immutable delivery or retraction fence changed after Chrome')
    return verify_seed(cid, manifest, stage='counts_after')


def control_request(port, stage, path, *, token=None, body=None, user_agent=None):
    """Real loopback HTTP; never log request, response, bearer or password."""
    if (stage not in HTTP_STAGES or not path.startswith('/')
        or (user_agent is not None and
            (stage != 'login_chrome' or user_agent != CHROME_CONTROL_UA))):
        raise RuntimeError('Invalid owned control request stage or path')
    payload = None if body is None else json.dumps(body, ensure_ascii=False).encode()
    headers = {'clientid': ADMIN_CLIENT_ID, 'Content-Type': 'application/json'}
    if user_agent is not None:
        headers['User-Agent'] = user_agent
    if token:
        headers['Authorization'] = 'Bearer ' + token
    connection = None
    status = None
    try:
        connection = http.client.HTTPConnection('127.0.0.1', port, timeout=20)
        connection.request('GET' if payload is None else 'POST', path, body=payload, headers=headers)
        response = connection.getresponse()
        status = safe_http_status(response.status)
        encoded = response.read(1_000_001)
        if len(encoded) > 1_000_000:
            raise OwnedControlFailure(stage, 'response_too_large', status)
        try:
            data = json.loads(encoded)
        except (ValueError, UnicodeError):
            raise OwnedControlFailure(stage, 'invalid_json', status) from None
        if not isinstance(data, dict):
            raise OwnedControlFailure(stage, 'invalid_shape', status)
        return response.status, data
    except OwnedControlFailure:
        raise
    except (OSError, ValueError, http.client.HTTPException):
        raise OwnedControlFailure(stage, 'transport', status) from None
    finally:
        if connection is not None:
            connection.close()


def control_login(port, username, password, stage, *, user_agent=None):
    if stage not in LOGIN_STAGES:
        raise RuntimeError('Invalid owned control login stage')
    if (stage == 'login_chrome') != (user_agent == CHROME_CONTROL_UA) or (
        user_agent is not None and user_agent != CHROME_CONTROL_UA):
        raise RuntimeError('Owned login User-Agent does not match its fixed stage')
    request = {'body': {'username': username, 'password': password,
                        'clientId': ADMIN_CLIENT_ID, 'grantType': 'password'}}
    if user_agent is not None:
        request['user_agent'] = user_agent
    status, response = control_request(port, stage, '/auth/login', **request)
    data = response.get('data')
    token = data.get('access_token') if isinstance(data, dict) else None
    if status != 200 or response.get('code') != 200:
        raise OwnedControlFailure(stage, 'login_rejected', status, response.get('code'))
    if not isinstance(token, str) or len(token) < 16:
        raise OwnedControlFailure(stage, 'token_missing', status, response.get('code'))
    return token


def audit_client_identity(cid):
    """Read the owned Admin Client identity for private cross-checks only."""
    rows = mysql(cid, 'SELECT client_key,device_type FROM sys_client WHERE client_id='
                 + compared_hexsql(ADMIN_CLIENT_ID) + " AND status='0' AND del_flag='0';",
                 stage='audit_client').splitlines()
    if len(rows) != 1:
        raise RuntimeError('Owned audit Admin Client is absent or ambiguous')
    fields = rows[0].split('\t')
    if len(fields) != 2 or not all(value and value != 'NULL' for value in fields):
        raise RuntimeError('Owned audit Admin Client fields differ')
    return tuple(fields)


def audit_baseline(cid, username):
    """Capture the latest owned audit ID before this individual real HTTP login."""
    value = mysql(cid, 'SELECT COALESCE(MAX(info_id),0) FROM sys_login_info WHERE user_name='
                  + compared_hexsql(username) + ';', stage='audit_baseline')
    if not re.fullmatch(r'[0-9]+', value):
        raise RuntimeError('Owned login audit baseline is invalid')
    return int(value)


def verify_login_audit(cid, username, baseline, client_identity,
                       expected_browser, expected_os, seconds=30):
    """Wait for exactly one new asynchronous success row with the expected UA facts."""
    if type(baseline) is not int or baseline < 0 or len(client_identity) != 2:
        raise RuntimeError('Owned login audit arguments differ')
    client_key, device_type = client_identity
    sql = ('SELECT COUNT(*),COALESCE(SUM(status=\'0\' AND client_key='
           + compared_hexsql(client_key) + ' AND device_type=' + compared_hexsql(device_type)
           + ' AND browser=' + compared_hexsql(expected_browser)
           + ' AND os=' + compared_hexsql(expected_os)
           + '),0) FROM sys_login_info WHERE info_id>' + str(baseline)
           + ' AND user_name=' + compared_hexsql(username) + ';')
    deadline = time.monotonic() + seconds
    while True:
        values = mysql(cid, sql, stage='audit_login').split('\t')
        if len(values) != 2 or any(not re.fullmatch(r'[0-9]+', value) for value in values):
            raise RuntimeError('Owned login audit result is invalid')
        total, expected = map(int, values)
        if total == expected == 1:
            return True
        if total > 1 or (total == 1 and expected != 1):
            raise RuntimeError('Owned login audit row differs')
        if time.monotonic() >= deadline:
            raise RuntimeError('Owned asynchronous login audit timed out')
        time.sleep(.2)


def verify_online_login(port, token, username, client_identity, stage,
                        expected_browser, expected_os):
    """Match the issued bearer only in memory; never persist an online DTO."""
    if stage not in ONLINE_STAGES:
        raise RuntimeError('Invalid owned online verification stage')
    status, response = control_request(port, stage, '/monitor/online', token=token)
    data = response.get('data')
    rows = data.get('rows') if isinstance(data, dict) else None
    if status != 200 or response.get('code') != 200:
        raise OwnedControlFailure(stage, 'action_rejected', status, response.get('code'))
    if not isinstance(rows, list):
        raise OwnedControlFailure(stage, 'invalid_shape', status, response.get('code'))
    matches = [row for row in rows if isinstance(row, dict) and row.get('tokenId') == token]
    if (len(matches) != 1 or matches[0].get('userName') != username
        or matches[0].get('clientKey') != ADMIN_CLIENT_ID
        or matches[0].get('deviceType') != client_identity[1]
        or matches[0].get('browser') != expected_browser
        or matches[0].get('os') != expected_os):
        raise RuntimeError('Owned issued token online identity differs')
    return True


def verified_owned_login(cid, port, username, password, login_stage, online_stage,
                         client_identity, redactions, *, user_agent=None):
    """Prove the same real login in HTTP, its own online DTO, and async audit."""
    baseline = audit_baseline(cid, username)
    token = control_login(port, username, password, login_stage, user_agent=user_agent)
    redactions.append(token)
    expected_browser, expected_os = ('Chrome', 'Linux') if user_agent else ('Unknown', 'Unknown')
    verify_online_login(port, token, username, client_identity, online_stage,
                        expected_browser, expected_os)
    verify_login_audit(cid, username, baseline, client_identity,
                       expected_browser, expected_os)
    return token


def require_control_success(result, stage):
    status, response = result
    if status != 200 or response.get('code') != 200:
        raise OwnedControlFailure(stage, 'action_rejected', status, response.get('code'))


def real_notice_control(cid, port, token, run_id, a_user, b_user):
    """Save/publish/retract through production HTTP; wait for Worker before retract."""
    title = f'T40 {run_id} real notice'
    content = f'T40 {run_id} real V1 full content'
    require_control_success(control_request(port, 'notice_save', '/notify/notice/save', token=token,
                                            body={'noticeTitle': title, 'noticeType': '1',
                                                  'noticeContent': content, 'recipientType': 'USER',
                                                  'recipientIds': [a_user], 'userTypeIds': [],
                                                  'channels': ['IN_APP']}), 'notice_save')
    matches = mysql(cid, 'SELECT notice_id FROM notify_notice WHERE notice_title='
                    f'{compared_hexsql(title)};', stage='notice_lookup').splitlines()
    if len(matches) != 1 or not re.fullmatch(r'[1-9][0-9]*', matches[0]):
        raise RuntimeError('Owned notice save did not create one exact draft')
    notice_id = int(matches[0])
    require_control_success(control_request(port, 'notice_publish',
                                            f'/notify/notice/{notice_id}/publish',
                                            token=token, body={}), 'notice_publish')
    deadline = time.monotonic() + 90
    message_id = None
    while time.monotonic() < deadline:
        message_id = verify_real_delivery(cid, notice_id, a_user, b_user, stage='real_delivery')
        if message_id is not None:
            break
        time.sleep(0.25)
    if message_id is None:
        raise RuntimeError('Owned real Worker did not deliver exact V1 before deadline')
    if notice_version_fact(cid, notice_id, message_id, stage='real_identity')['retracted']:
        raise RuntimeError('Owned V1 was already retracted before control request')
    require_control_success(control_request(port, 'notice_retract',
                                            f'/notify/notice/{notice_id}/retract',
                                            token=token, body={}), 'notice_retract')
    marker = mysql(cid, f'SELECT lifecycle FROM notify_notice WHERE notice_id={notice_id};',
                   stage='notice_retracted')
    if marker != 'RETRACTED':
        raise RuntimeError('Owned real Notice did not persist retraction')
    return {'notice_id': notice_id, 'message_id': message_id, 'title': title, 'content': content}


def playwright_identity(data):
    cases = []
    def visit(suites, inherited_file=None):
        for suite in suites:
            source = suite.get('file') or inherited_file
            for spec in suite.get('specs', []):
                for case in spec.get('tests', []):
                    cases.append((spec.get('file') or source, spec.get('title'),
                                  case.get('projectName'), case.get('results', [])))
            visit(suite.get('suites', []), source)
    visit(data.get('suites', []))
    if len(cases) != 2:
        raise RuntimeError('Playwright reporter did not identify exactly two cases')
    expected = (ROOT / TEST_PATH).resolve(strict=True)
    titles = []
    for source, title, project, results in cases:
        if not isinstance(source, str) or not isinstance(title, str) or len(results) != 1:
            raise RuntimeError('Playwright exact case identity or attempt count differs')
        actual = Path(source)
        candidates = ([actual.resolve()] if actual.is_absolute() else
                      [(base / actual).resolve() for base in (ROOT, ROOT / 'frontend', ROOT / 'frontend/e2e')])
        if expected not in candidates or title not in TEST_TITLES or project != 'chromium':
            raise RuntimeError('Playwright file, title or Chrome project differs')
        if results[0].get('status') != 'passed':
            raise RuntimeError('Playwright case did not pass on its only attempt')
        titles.append(title)
    if sorted(titles) != sorted(TEST_TITLES):
        raise RuntimeError('Playwright case titles are missing or duplicated')
    return {'file': TEST_PATH, 'titles': titles, 'project': 'chromium', 'attempts': 2}


def playwright_diagnostic(data):
    """Only retain exact-test numeric source locations, never reporter text or attachments."""
    cases = []
    def visit(suites, inherited_file=None):
        for suite in suites:
            source = suite.get('file') or inherited_file
            for spec in suite.get('specs', []):
                for case in spec.get('tests', []):
                    cases.append((spec.get('file') or source, spec, case))
            visit(suite.get('suites', []), source)
    visit(data.get('suites', []))
    expected = ROOT / TEST_PATH
    def exact_file(value):
        if not isinstance(value, str):
            return False
        path = Path(value)
        candidates = ([path.resolve()] if path.is_absolute() else
                      [(base / path).resolve() for base in (ROOT, ROOT / 'frontend', ROOT / 'frontend/e2e')])
        return expected.resolve() in candidates
    lines = expected.read_text().splitlines()
    def location(value, *, source_required):
        if not isinstance(value, dict) or (source_required and not exact_file(value.get('file'))):
            return None
        line, column = value.get('line'), value.get('column')
        if type(line) is not int or type(column) is not int or line < 1 or line > len(lines):
            return None
        if column < 1 or column > min(len(lines[line - 1]) + 1, 1000):
            return None
        return {'line': line, 'column': column}
    diagnostics = []
    for source, spec, case in cases:
        title = spec.get('title')
        if not exact_file(source) or title not in TEST_TITLES or case.get('projectName') != 'chromium':
            continue
        results = case.get('results', [])
        assertion = None
        if len(results) == 1:
            result = results[0]
            candidates = [result.get('errorLocation')]
            candidates += [error.get('location') for error in result.get('errors', []) if isinstance(error, dict)]
            assertion = next((safe for item in candidates if (safe := location(item, source_required=True))), None)
        status = results[0].get('status') if len(results) == 1 else None
        if status not in ('passed', 'failed', 'timedOut', 'interrupted', 'skipped'):
            status = None
        diagnostics.append({'file': TEST_PATH, 'title': title, 'project': 'chromium',
                            'attempts': len(results), 'status': status,
                            'case_declaration': location(spec, source_required=False),
                            'assertion_location': assertion})
    return diagnostics


def isolated_config(mysql_port, redis_port, backend_port, vite_port, app_user,
                    app_password, redis_password, run_dir):
    config = {
        'server.address': '127.0.0.1',
        'server.port': backend_port,
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
        'spring.boot.admin.client.password': random_password(),
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
        'namewta.sso.web-origin': f'http://127.0.0.1:{vite_port}',
        'namewta.sso.cookie-secure': False,
        'web.cors.allowed-origins': [f'http://127.0.0.1:{vite_port}'],
        'captcha.enable': False,
        'message.enabled': False,
        'notify.outbox.poll-delay-ms': 1000,
        'springdoc.api-docs.enabled': False,
        'openapi.enabled': False,
        'spring.servlet.multipart.location': str(run_dir / 'multipart'),
        'logging.file.path': str(run_dir / 'logs'),
        'management.endpoint.logfile.external-file': str(run_dir / 'logs' / 'sys-console.log'),
    }
    if (config['server.address'] != '127.0.0.1'
        or 'jdbc:mysql://127.0.0.1:' not in config['spring.datasource.dynamic.datasource.master.url']
        or config['spring.data.redis.host'] != '127.0.0.1'):
        raise RuntimeError('Browser backend config is not loopback isolated')
    return config


def parse_counts(data):
    stats = data.get('stats') or {}
    counts = {key: int(stats.get(key, 0)) for key in ('expected', 'unexpected', 'skipped', 'flaky')}
    if counts != {'expected': 2, 'unexpected': 0, 'skipped': 0, 'flaky': 0}:
        raise RuntimeError('Playwright expected/unexpected/skipped/flaky counts differ')
    return counts


def validate_browser_inputs(test_source, config_source):
    """Prevent automatic credentials/tokens entering retained browser artifacts."""
    for name in ('trace', 'video', 'screenshot'):
        if not re.search(r'\b' + name + r'\s*:\s*([\'\"])off\1', config_source):
            raise RuntimeError('Playwright config does not disable ' + name)
    forbidden = (r'\bstorageState\b', r'\brecordHar\b', r'\brecordVideo\b',
                 r'\brouteFromHAR\b')
    if any(re.search(pattern, test_source + '\n' + config_source) for pattern in forbidden):
        raise RuntimeError('Playwright source attempts to persist authentication or network artifacts')


def preflight():
    """Static file check only; does not contact Docker, JVM or HTTP."""
    paths = [ARTIFACT, INIT_SCRIPT, ROOT / TEST_PATH, ROOT / CONFIG_PATH,
             ROOT / 'frontend/apps/admin-web/node_modules/vite/bin/vite.js',
             ROOT / 'frontend/node_modules/@playwright/test/cli.js']
    result = {'mode': 'static-preflight-only', 'service_started': False,
              'files_present': {str(path.relative_to(ROOT)): path.is_file() for path in paths},
              'sql_present': {name: (SQL_ROOT / name).is_file() for name in SQL_NAMES},
              'expected_images': ['mysql:8.4.9', 'redis:8.6.3', MINIO_IMAGE],
              'generated_child_env_names': ['T40_A_USERNAME', 'T40_A_PASSWORD',
                                            'T40_B_USERNAME', 'T40_B_PASSWORD'],
              'bcrypt_available': True}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return int(not all(result['files_present'].values()) or not all(result['sql_present'].values()))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--preflight', action='store_true', help='Static only; never starts a service')
    parser.add_argument('--execute', action='store_true')
    parser.add_argument('--expected-head')
    parser.add_argument('--expected-jar-sha256')
    parser.add_argument('--package-proof')
    args = parser.parse_args()
    if args.preflight:
        if args.execute or args.expected_head or args.expected_jar_sha256 or args.package_proof:
            parser.error('--preflight cannot combine with execution arguments')
        return preflight()
    if (not args.execute or not args.expected_head or not re.fullmatch(r'[0-9a-f]{40}', args.expected_head)
        or not args.expected_jar_sha256 or not re.fullmatch(r'[0-9a-f]{64}', args.expected_jar_sha256)
        or not args.package_proof):
        parser.error('no launch without --execute, exact clean SHA, JAR SHA-256 and package proof')
    os.umask(0o077)
    run_id = secrets.token_hex(8)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(parents=True, mode=0o700)
    root_password, app_password, redis_password, minio_password = (random_password() for _ in range(4))
    app_user = 't40b_' + run_id
    minio_user = 't40minio' + run_id
    bucket = 't40-' + run_id
    account_env = {'T40_A_USERNAME': 'test', 'T40_A_PASSWORD': random_login_password(),
                   'T40_B_USERNAME': 'test1', 'T40_B_PASSWORD': random_login_password()}
    control_password = random_login_password()
    redactions = [root_password, app_password, redis_password, minio_password,
                  account_env['T40_A_PASSWORD'], account_env['T40_B_PASSWORD'], control_password]
    report = {'gate': 'T-40 real notice retraction recipient Chrome',
              'run_id': run_id, 'acceptance': False, 'exit_code': 1,
              'started_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'owned': {}, 'cleanup': {}, 'source_before': None, 'artifact_before': None}
    captured, volumes, ports, processes, errors = [], [], [], [], []
    mysql_id = None
    browser_complete = False
    manifest = None
    phase = 'setup'
    secret_paths = [run_dir / name for name in ('mysql-container.env', 'redis.conf',
                                                'minio-container.env', 'init.env', 'owned.yml')]
    raw_logs = [(run_dir / (name + '.raw.log'), run_dir / (name + '.log'))
                for name in ('backend', 'vite', 'playwright-stderr')]
    try:
        source = source_identity(args.expected_head)
        report['source_before'] = source
        if not ARTIFACT.is_file() or sha_file(ARTIFACT) != args.expected_jar_sha256:
            raise RuntimeError('JAR differs from the expected full package artifact')
        report['artifact_before'] = {'path': str(ARTIFACT.relative_to(ROOT)),
                                     'sha256': args.expected_jar_sha256,
                                     'size_bytes': ARTIFACT.stat().st_size}
        proof_path = Path(args.package_proof)
        proof = json.loads(proof_path.read_text())
        validate_package_proof(proof, args.expected_head, source['tree'],
                               args.expected_jar_sha256, ARTIFACT.stat().st_size)
        report['package_proof_sha256'] = sha_file(proof_path)
        report['full_bundle_modules'] = validate_full_jar(ARTIFACT)
        product_paths = [ROOT / TEST_PATH, ROOT / CONFIG_PATH,
                         ROOT / 'frontend/apps/admin-web/node_modules/vite/bin/vite.js',
                         ROOT / 'frontend/node_modules/@playwright/test/cli.js']
        if not all(path.is_file() for path in product_paths):
            raise RuntimeError('Real browser spec/config/Vite/Playwright inputs are missing')
        validate_browser_inputs(product_paths[0].read_text(), product_paths[1].read_text())
        report['browser_inputs'] = {str(path.relative_to(ROOT)): sha_file(path) for path in product_paths[:2]}
        if not INIT_SCRIPT.is_file() or not all((SQL_ROOT / name).is_file() for name in SQL_NAMES):
            raise RuntimeError('Owned six-SQL initializer inputs are missing')
        report['sql_baselines'] = {name: sha_file(SQL_ROOT / name) for name in SQL_NAMES}

        mysql_env = run_dir / 'mysql-container.env'
        private_bytes(mysql_env, ('MYSQL_ROOT_PASSWORD=' + root_password + '\n').encode())
        mysql_id = full_id(docker('run', '--pull=never', '-d', '--name', 't40-browser-mysql-' + run_id,
                                  '--label', 'namewta.test.owner=' + OWNER,
                                  '--label', 'namewta.test.run=' + run_id,
                                  '-p', '127.0.0.1::3306', '--env-file', str(mysql_env),
                                  'mysql:8.4.9', '--character-set-server=utf8mb4',
                                  '--collation-server=utf8mb4_general_ci',
                                  '--log-bin-trust-function-creators=1'))
        captured.append(mysql_id)
        assert_owned(mysql_id, run_id)
        volumes.extend(volume_names(mysql_id))
        wait_mysql(mysql_id)
        mysql_port = mapped_port(mysql_id, '3306')
        ports.append(mysql_port)
        report['owned']['mysql_container_id'] = mysql_id

        redis_conf = run_dir / 'redis.conf'
        private_bytes(redis_conf, redis_config(redis_password))
        os.chown(redis_conf, REDIS_CONFIG_USER, REDIS_CONFIG_USER)
        redis_id = full_id(docker('run', '--pull=never', '-d', '--name', 't40-browser-redis-' + run_id,
                                  '--label', 'namewta.test.owner=' + OWNER,
                                  '--label', 'namewta.test.run=' + run_id,
                                  '-p', '127.0.0.1::6379', '--user',
                                  str(REDIS_CONFIG_USER) + ':' + str(REDIS_CONFIG_USER),
                                  '--mount', 'type=bind,source=' + str(redis_conf)
                                  + ',target=/etc/redis/redis.conf,readonly',
                                  'redis:8.6.3', 'redis-server', '/etc/redis/redis.conf'))
        captured.append(redis_id)
        assert_owned(redis_id, run_id)
        volumes.extend(volume_names(redis_id))
        wait_redis(redis_id)
        redis_port = mapped_port(redis_id, '6379')
        ports.append(redis_port)
        report['owned']['redis_container_id'] = redis_id

        minio_env = run_dir / 'minio-container.env'
        private_bytes(minio_env, ('MINIO_ROOT_USER=' + minio_user + '\nMINIO_ROOT_PASSWORD='
                                  + minio_password + '\n').encode())
        minio_id = full_id(docker('run', '--pull=never', '-d', '--name', 't40-browser-minio-' + run_id,
                                  '--label', 'namewta.test.owner=' + OWNER,
                                  '--label', 'namewta.test.run=' + run_id,
                                  '-p', '127.0.0.1::9000', '--env-file', str(minio_env),
                                  MINIO_IMAGE, 'server', '--address', ':9000', '/data'))
        captured.append(minio_id)
        assert_owned(minio_id, run_id)
        volumes.extend(volume_names(minio_id))
        minio_port = mapped_port(minio_id, '9000')
        ports.append(minio_port)
        wait_minio(minio_port)
        report['owned']['minio_container_id'] = minio_id
        report['owned']['loopback_ports'] = {'mysql': mysql_port, 'redis': redis_port, 'minio': minio_port}

        init_env = run_dir / 'init.env'
        private_bytes(init_env, ('MYSQL_DATABASE=wta-plus\nMYSQL_APP_USER=' + app_user
                                 + '\nMYSQL_APP_PASSWORD=' + app_password + '\nMINIO_ROOT_USER='
                                 + minio_user + '\nMINIO_ROOT_PASSWORD=' + minio_password
                                 + '\nMINIO_ENDPOINT=127.0.0.1:' + str(minio_port)
                                 + '\nMINIO_BUCKET=' + bucket + '\n').encode())
        (run_dir / 'docker-config').mkdir(mode=0o700)
        init_result = run(('bash', str(INIT_SCRIPT), '--container', mysql_id,
                           '--env-file', str(init_env), '--sql-dir', str(SQL_ROOT)),
                          env=safe_env(DOCKER_HOST='unix:///var/run/docker.sock',
                                       DOCKER_CONFIG=str(run_dir / 'docker-config')),
                          timeout=900)
        private_bytes(run_dir / 'init.log', redact(
            (init_result.stdout + init_result.stderr).decode('utf-8', errors='replace'), redactions).encode())
        report['init_exit_code'] = init_result.returncode
        if init_result.returncode:
            raise RuntimeError('Owned six-SQL initializer failed')
        tables = int(mysql(mysql_id, 'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();',
                           stage='baseline_tables'))
        outboxes = int(mysql(mysql_id, 'SELECT COUNT(*) FROM notify_outbox;', stage='baseline_outboxes'))
        external = int(mysql(mysql_id, "SELECT COUNT(*) FROM notify_delivery WHERE channel IN ('SMS','MAIL');",
                             stage='baseline_external'))
        enabled_accounts = int(mysql(mysql_id,
                                     "SELECT COUNT(*) FROM notify_channel_account WHERE channel IN ('SMS','MAIL') AND enabled='Y';",
                                     stage='baseline_accounts'))
        private_oss = int(mysql(mysql_id,
                                "SELECT COUNT(*) FROM sys_oss_config WHERE config_key='minio' AND status='Y' AND access_policy='0';",
                                stage='baseline_oss'))
        if (tables, outboxes, external, enabled_accounts, private_oss) != (103, 0, 0, 0, 1):
            raise RuntimeError('Owned schema violates fresh no-supplier-call baseline')
        report['owned']['baseline_counts'] = {
            'business_tables': tables, 'outboxes': outboxes, 'external_deliveries': external,
            'enabled_external_accounts': enabled_accounts, 'private_default_oss': private_oss}

        control_user = owned_user_id(mysql_id, 'WTA', 'lookup_control')
        a_user = owned_user_id(mysql_id, 'test', 'lookup_a')
        b_user = owned_user_id(mysql_id, 'test1', 'lookup_b')
        client_pk = owned_admin_client_id(mysql_id)
        if len({control_user, a_user, b_user}) != 3:
            raise RuntimeError('Owned control and recipients are not distinct')
        rotate_owned_login(mysql_id, control_user, control_password)
        rotate_owned_login(mysql_id, a_user, account_env['T40_A_PASSWORD'])
        rotate_owned_login(mysql_id, b_user, account_env['T40_B_PASSWORD'])
        report['owned']['login_passwords_rotated_in_fresh_schema'] = 3
        before_counts = mysql(mysql_id, 'SELECT '
                              f'(SELECT COUNT(*) FROM notify_message_recipient WHERE user_id={a_user}),'
                              f'(SELECT COUNT(*) FROM notify_message_recipient WHERE user_id={b_user});',
                              stage='counts_before').split('\t')
        if before_counts != ['0', '0']:
            raise RuntimeError('Fresh owned schema already has A/B inbox recipients')
        report['owned']['recipient_role_ids'] = [
            create_owned_recipient_role(mysql_id, run_id, control_user, a_user, client_pk, 'a'),
            create_owned_recipient_role(mysql_id, run_id, control_user, b_user, client_pk, 'b')]

        backend_port = free_port()
        vite_port = free_port()
        while vite_port == backend_port:
            vite_port = free_port()
        ports.extend((backend_port, vite_port))
        for name in ('multipart', 'logs', 'tmp'):
            (run_dir / name).mkdir(mode=0o700)
        overlay = isolated_config(mysql_port, redis_port, backend_port, vite_port,
                                  app_user, app_password, redis_password, run_dir)
        redactions.append(overlay['spring.boot.admin.client.password'])
        overlay_path = run_dir / 'owned.yml'
        private_bytes(overlay_path, (json.dumps(overlay, ensure_ascii=False) + '\n').encode())
        java_argv = ('java', '-Xms256m', '-Xmx1024m', '-Duser.home=' + str(run_dir),
                     '-Djava.io.tmpdir=' + str(run_dir / 'tmp'), '-jar', str(ARTIFACT),
                     '--spring.profiles.active=dev',
                     '--spring.config.additional-location=file:' + str(overlay_path))
        if any(secret in ' '.join(java_argv) for secret in redactions):
            raise RuntimeError('Private value entered Java argv')
        with os.fdopen(os.open(raw_logs[0][0], os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
            backend = subprocess.Popen(java_argv, cwd=run_dir, stdout=stream,
                                       stderr=subprocess.STDOUT, start_new_session=True,
                                       env=safe_env(TZ='Asia/Shanghai', HOME=str(run_dir),
                                                    TMPDIR=str(run_dir / 'tmp')))
        processes.append(('backend', backend))
        report['owned']['backend_pgid'] = backend.pid
        phase = 'backend_probe'
        wait_http(backend_port, '/auth/code', backend, 240)
        report['owned']['backend_probe_http_status'] = 200

        phase = 'login_control'
        client_identity = audit_client_identity(mysql_id)
        control_token = verified_owned_login(
            mysql_id, backend_port, 'WTA', control_password, phase, 'online_control',
            client_identity, redactions)
        recipient_tokens = []
        for login_stage, online_stage, username, password in (
            ('login_a', 'online_a', account_env['T40_A_USERNAME'], account_env['T40_A_PASSWORD']),
            ('login_b', 'online_b', account_env['T40_B_USERNAME'], account_env['T40_B_PASSWORD'])):
            phase = login_stage
            token = verified_owned_login(mysql_id, backend_port, username, password,
                                         phase, online_stage, client_identity, redactions)
            recipient_tokens.append(token)
        report['owned']['control_and_recipient_logins_validated'] = 3
        report['owned']['headerless_online_matches'] = 3
        report['owned']['headerless_success_audits'] = 3
        phase = 'login_chrome'
        control_token = verified_owned_login(
            mysql_id, backend_port, 'WTA', control_password, phase, 'online_chrome',
            client_identity, redactions, user_agent=CHROME_CONTROL_UA)
        report['owned']['chrome_control_online_match'] = True
        report['owned']['chrome_control_success_audit'] = True
        phase = 'notice_control'
        real = real_notice_control(mysql_id, backend_port, control_token, run_id, a_user, b_user)
        phase = 'recipient_permission'
        for token in recipient_tokens:
            denied = control_request(backend_port, 'recipient_notice_denied',
                                     f'/notify/notice/{real["notice_id"]}', token=token)
            if denied[0] != 200 or denied[1].get('code') != 403:
                raise RuntimeError('Owned recipient Notice management permission result differs')
        report['owned']['recipient_notice_get_denials'] = 2
        report['real_publication'] = {'notice_id': str(real['notice_id']),
                                      'message_id': str(real['message_id']),
                                      'worker_delivered_before_retract': True,
                                      'retracted_after_delivery': True}
        phase = 'seed'
        seed_sql, manifest = seed_plan(run_id, a_user, b_user, real['message_id'],
                                       real['notice_id'], real['title'], real['content'])
        mysql(mysql_id, seed_sql, stage='seed_insert')
        report['seed_before'] = verify_seed(mysql_id, manifest, stage='counts_seed')
        seed_path = run_dir / 'seed.json'
        private_bytes(seed_path, (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode())
        report['seed_manifest_sha256'] = sha_file(seed_path)

        vite_argv = ('node', str(product_paths[2]), 'serve', '--mode', 'development',
                     '--host', '127.0.0.1', '--port', str(vite_port), '--strictPort')
        vite_env = safe_env(VITE_APP_PORT=str(vite_port), VITE_APP_CONTEXT_PATH='/',
                            VITE_APP_BASE_API='/dev-api', VITE_APP_BASE_URL='/',
                            VITE_APP_CLIENT_ID=ADMIN_CLIENT_ID,
                            VITE_APP_PROXY_TARGET=f'http://127.0.0.1:{backend_port}',
                            VITE_APP_MESSAGE_ENABLED='false', BROWSER='none')
        with os.fdopen(os.open(raw_logs[1][0], os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stream:
            vite = subprocess.Popen(vite_argv, cwd=ROOT / 'frontend/apps/admin-web',
                                    env=vite_env, stdout=stream, stderr=subprocess.STDOUT,
                                    start_new_session=True)
        processes.append(('vite', vite))
        report['owned']['vite_pgid'] = vite.pid
        phase = 'vite_probe'
        wait_http(vite_port, '/login', vite, 120)
        report['owned']['loopback_ports'].update({'backend': backend_port, 'vite': vite_port})

        phase = 'browser'
        playwright_env = safe_env(T40_ADMIN_ORIGIN=f'http://127.0.0.1:{vite_port}',
                                   T40_BACKEND_ORIGIN=f'http://127.0.0.1:{backend_port}',
                                   T40_SEED_MANIFEST=str(seed_path), T40_RUN_ID=run_id,
                                   CI='1', **account_env)
        playwright_argv = ('node', str(product_paths[3]), 'test', str(product_paths[0]),
                           '--config', str(product_paths[1]), '--grep', 'T-40 real ',
                           '--workers=1', '--reporter=json', '--output', str(run_dir / 'playwright-artifacts'))
        if any(secret in ' '.join(playwright_argv) for secret in redactions):
            raise RuntimeError('Private value entered Playwright argv')
        with os.fdopen(os.open(run_dir / 'playwright.raw.json', os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stdout, \
             os.fdopen(os.open(raw_logs[2][0], os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as stderr:
            playwright = subprocess.Popen(playwright_argv, cwd=ROOT / 'frontend',
                                          env=playwright_env, stdout=stdout, stderr=stderr,
                                          start_new_session=True)
            processes.append(('playwright', playwright))
            report['owned']['playwright_pgid'] = playwright.pid
            playwright.wait(timeout=420)
        report['playwright_exit_code'] = playwright.returncode
        raw_reporter = (run_dir / 'playwright.raw.json').read_bytes()
        report['playwright_raw_sha256'] = hashlib.sha256(raw_reporter).hexdigest()
        data = json.loads(raw_reporter)
        stats = data.get('stats') or {}
        report['playwright_counts'] = {
            key: int(stats.get(key, 0)) for key in ('expected', 'unexpected', 'skipped', 'flaky')}
        report['playwright_diagnostic'] = playwright_diagnostic(data)
        private_bytes(run_dir / 'playwright.json',
                      (json.dumps({'source_reporter_sha256': report['playwright_raw_sha256'],
                                   'counts': report['playwright_counts'],
                                   'diagnostic': report['playwright_diagnostic']},
                                  ensure_ascii=False, indent=2) + '\n').encode())
        parse_counts(data)
        report['playwright_identity'] = playwright_identity(data)
        if playwright.returncode != 0:
            raise RuntimeError('Real Playwright browser gate failed')
        browser_complete = True
        phase = 'post_browser'
        report['seed_after'] = verify_after_browser(mysql_id, manifest)
        report['exit_code'] = 0
    except BaseException as exc:
        report.update(safe_failure(exc, phase))
        backend_exit = failed_backend_exit_code(processes)
        if backend_exit is not None:
            report['backend_exit_code'] = backend_exit
        if isinstance(exc, OwnedSqlError):
            report['sql_failure'] = exc.safe_details()
    finally:
        if mysql_id is not None and browser_complete and manifest is not None and 'seed_after' not in report:
            try:
                report['seed_after_failed_check'] = verify_seed(mysql_id, manifest, stage='counts_after')
            except Exception:
                report['seed_after_failed_check'] = 'unavailable'
        for name, proc in reversed(processes):
            try:
                live = stop_group(proc)
                report['cleanup'][name + '_process_group'] = {'pgid': proc.pid, 'live_members': live}
                if live:
                    errors.append(name + '_process_group_remains')
            except Exception:
                errors.append(name + '_process_group_cleanup_failed')
        for raw_path, _clean_path in raw_logs:
            try:
                raw_path.unlink(missing_ok=True)
            except Exception:
                errors.append('owned_raw_log_removal_failed')
        raw_report = run_dir / 'playwright.raw.json'
        try:
            raw_report.unlink(missing_ok=True)
        except Exception:
            errors.append('playwright_raw_report_removal_failed')
        try:
            shutil.rmtree(run_dir / 'playwright-artifacts')
        except FileNotFoundError:
            pass
        except Exception:
            errors.append('playwright_artifacts_removal_failed')
        for path in secret_paths:
            try:
                path.unlink(missing_ok=True)
            except Exception:
                errors.append('private_env_or_config_removal_failed')
        for directory in ('logs', 'multipart', 'tmp', 'docker-config'):
            try:
                shutil.rmtree(run_dir / directory)
            except FileNotFoundError:
                pass
            except Exception:
                errors.append('owned_runtime_directory_removal_failed')
        report['owned']['captured_anonymous_volume_names'] = list(dict.fromkeys(volumes))
        remaining, container_errors = cleanup_containers(run_id, captured)
        report['cleanup']['remaining_owned_full_ids'] = remaining
        errors.extend(container_errors)
        try:
            report['cleanup']['anonymous_volumes_absent'] = {
                name: volume_absent(name) for name in dict.fromkeys(volumes)}
            if not all(report['cleanup']['anonymous_volumes_absent'].values()):
                errors.append('owned_anonymous_volume_remains')
        except Exception:
            errors.append('owned_volume_absence_check_failed')
        try:
            report['cleanup']['loopback_ports_closed'] = {str(port): closed(port) for port in ports}
            if not all(report['cleanup']['loopback_ports_closed'].values()):
                errors.append('owned_port_still_open')
        except Exception:
            errors.append('owned_port_absence_check_failed')
        try:
            report['source_after'] = source_identity(args.expected_head)
            if report['source_after'] != report.get('source_before'):
                errors.append('source_identity_changed')
        except Exception:
            errors.append('source_after_not_clean_exact_head')
        try:
            report['artifact_after_sha256'] = sha_file(ARTIFACT)
            if report['artifact_after_sha256'] != args.expected_jar_sha256:
                errors.append('jar_changed_after_browser')
        except Exception:
            errors.append('jar_after_unreadable')
        report['cleanup']['errors'] = errors
        report['exit_code'] = int(bool(report['exit_code'] or errors))
        report['acceptance'] = report['exit_code'] == 0
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        try:
            private_bytes(run_dir / 'result.json', (json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode())
        except Exception:
            report['exit_code'] = 1
            print('T-40 browser result write failed', file=sys.stderr)
        print(json.dumps({'result': str(run_dir / 'result.json'), 'exit_code': report['exit_code']},
                         ensure_ascii=False))
    return report['exit_code']


if __name__ == '__main__':
    sys.exit(main())
