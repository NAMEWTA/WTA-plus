#!/usr/bin/env python3
"""T-41 real Admin browser gate on fresh, owned loopback infrastructure.

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
RUN_ROOT = Path('/tmp/wta-t41/browser-runs')
ARTIFACT = ROOT / 'backend/wta-admin/target/wta-admin.jar'
INIT_SCRIPT = ROOT / 'release-artifacts/scripts/init-mysql-container.sh'
SQL_ROOT = ROOT / 'release-artifacts/docker/infrastructure/mysql/init'
SQL_NAMES = ('10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql',
             '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql')
TEST_PATH = 'frontend/e2e/inbox-paged-real.e2e.ts'
CONFIG_PATH = 'frontend/e2e/playwright.inbox-paged-real.config.ts'
TEST_TITLE = 'T-41 real Admin inbox page 26 and global read-all'
OWNER = 'T-41-BROWSER'
DOCKER = ('docker', '--host', 'unix:///var/run/docker.sock')
MINIO_IMAGE = 'pgsty/minio@sha256:83885c27b3b5b673049e33ddf4029afe2c134fd51ce4309e65e4f39d3b9ca282'
REDIS_CONFIG_USER = 65534
ADMIN_CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e'


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


def random_password():
    alphabet = string.ascii_letters + string.digits
    return ''.join(secrets.choice(alphabet) for _ in range(40))


def owned_bcrypt_hash(password):
    """Use the application's supported $2a$ variant, without external argv."""
    if not re.fullmatch(r'[A-Za-z0-9]{40}', password):
        raise RuntimeError('Owned login password is not generated alphanumeric')
    encoded = bcrypt.hashpw(password.encode('ascii'), bcrypt.gensalt(rounds=10, prefix=b'2a'))
    if not bcrypt.checkpw(password.encode('ascii'), encoded):
        raise RuntimeError('Owned BCrypt hash failed local verification')
    return encoded.decode('ascii')


def rotate_owned_login(cid, user_id, password):
    password_hash = owned_bcrypt_hash(password)
    changed = mysql(cid, 'UPDATE sys_user SET password=' + hexsql(password_hash)
                    + f" WHERE user_id={user_id} AND status='0' AND del_flag='0'; SELECT ROW_COUNT();")
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


def mysql(cid, sql):
    if isinstance(sql, str):
        sql = sql.encode('utf-8')
    return docker('exec', '-i', cid, 'sh', '-c',
                  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch --skip-column-names --database="$1"',
                  'sh', 'wta-plus', data=sql, timeout=180)


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


def seed_plan(run_id, a_user, b_user, base=None):
    """Build only synthetic rows; callers execute on a fresh owned database."""
    if (not re.fullmatch(r'[0-9a-f]{16}', run_id)
        or not all(isinstance(value, int) and value > 0 for value in (a_user, b_user))
        or a_user == b_user):
        raise RuntimeError('Invalid owned seed identities')
    if base is None:
        base = 8_000_000_000_000_000_000 + secrets.randbelow(100_000_000_000_000)
    if not 8_000_000_000_000_000_000 <= base < 8_000_100_000_000_000_000:
        raise RuntimeError('Synthetic ID base is outside its owned range')
    a_ids = [base + 2 * index for index in range(1, 502)]
    b_only = base + 7  # Global-first pagination would leave two A rows on page 26.
    shared = a_ids[250]
    a_oldest = a_ids[0]
    recipient_base = base + 100_000_000_000_000_000
    message_rows = []
    recipient_rows = []
    for index, message_id in enumerate(a_ids, 1):
        title = f'T41 {run_id} A {index:03d}'
        message_rows.append((message_id, title))
        read_time = '@old' if index > 481 else 'NULL'
        seen_time = '@old' if index == 1 or index > 481 else 'NULL'
        recipient_rows.append((recipient_base + index, message_id, a_user, seen_time, read_time))
    b_title = f'T41 {run_id} B only'
    message_rows.append((b_only, b_title))
    recipient_rows.append((recipient_base + 502, shared, b_user, 'NULL', 'NULL'))
    recipient_rows.append((recipient_base + 503, b_only, b_user, 'NULL', 'NULL'))
    if len({message_id for message_id, _ in message_rows}) != 502:
        raise RuntimeError('Synthetic message IDs overlap')
    sql = [
        'START TRANSACTION;',
        'SET @stamp = NOW(0);',
        'SET @old = DATE_SUB(@stamp, INTERVAL 1 DAY);',
        'INSERT INTO notify_message(message_id,category,channels_json,type,source,title,message,content,create_by,create_time) VALUES',
        ',\n'.join(f"({message_id},'notice',JSON_ARRAY('IN_APP'),'NOTICE','NOTICE',"
                   f"{hexsql(title)},{hexsql(title)},{hexsql(title)},{a_user},@stamp)"
                   for message_id, title in message_rows) + ';',
        'INSERT INTO notify_message_recipient(message_recipient_id,message_id,user_id,seen_time,read_time,create_by,create_time) VALUES',
        ',\n'.join(f'({recipient_id},{message_id},{user_id},{seen_time},{read_time},{a_user},@stamp)'
                   for recipient_id, message_id, user_id, seen_time, read_time in recipient_rows) + ';',
        'COMMIT;',
    ]
    manifest = {
        'schema_version': 1, 'run_id': run_id, 'pageNum': 26, 'pageSize': 20,
        'a': {'userId': str(a_user), 'total': 501, 'unread': 481},
        'b': {'userId': str(b_user), 'total': 2, 'unread': 2},
        'aOldest': {'messageId': str(a_oldest), 'title': f'T41 {run_id} A 001'},
        'shared': {'messageId': str(shared), 'title': f'T41 {run_id} A 251'},
        'bOnly': {'messageId': str(b_only), 'title': b_title},
        'preReadMessageIds': [str(item) for item in a_ids[-20:]],
    }
    return '\n'.join(sql).encode(), manifest


def inbox_counts(cid, a_user, b_user):
    query = (f"SELECT user_id,COUNT(*),SUM(read_time IS NULL) "
             f"FROM notify_message_recipient WHERE user_id IN ({a_user},{b_user}) "
             'GROUP BY user_id ORDER BY user_id;')
    rows = {}
    for line in mysql(cid, query).splitlines():
        user_id, total, unread = line.split('\t')
        rows[int(user_id)] = {'total': int(total), 'unread': int(unread)}
    return rows


def verify_seed(cid, manifest):
    a_user = int(manifest['a']['userId'])
    b_user = int(manifest['b']['userId'])
    counts = inbox_counts(cid, a_user, b_user)
    if counts != {a_user: {'total': 501, 'unread': 481},
                   b_user: {'total': 2, 'unread': 2}}:
        raise RuntimeError('Owned seed count or unread distribution differs')
    prefix = manifest['run_id']
    query = ("SELECT COUNT(*),COUNT(DISTINCT message_id) FROM notify_message WHERE title LIKE "
             + hexsql('T41 ' + prefix + '%') + ';')
    values = mysql(cid, query).split('\t')
    if values != ['502', '502']:
        raise RuntimeError('Owned seed message count differs')
    shared_id = int(manifest['shared']['messageId'])
    b_only = int(manifest['bOnly']['messageId'])
    ownership = mysql(cid, f'SELECT message_id,COUNT(*) FROM notify_message_recipient '
                      f'WHERE message_id IN ({shared_id},{b_only}) GROUP BY message_id ORDER BY message_id;')
    expected = {str(shared_id): 2, str(b_only): 1}
    actual = {fields[0]: int(fields[1]) for fields in (line.split('\t') for line in ownership.splitlines())}
    if actual != expected:
        raise RuntimeError('Shared and B-only relationship ownership differs')
    return {'a': counts[a_user], 'b': counts[b_user], 'synthetic_messages': 502,
            'synthetic_relations': 503, 'shared_relationships': 2}


def verify_after_browser(cid, manifest):
    a_user = int(manifest['a']['userId'])
    b_user = int(manifest['b']['userId'])
    counts = inbox_counts(cid, a_user, b_user)
    if counts != {a_user: {'total': 501, 'unread': 0},
                   b_user: {'total': 2, 'unread': 2}}:
        raise RuntimeError('Browser did not mark all 501 A rows while preserving both B rows')
    return {'a': counts[a_user], 'b': counts[b_user]}


def preservation_snapshot(cid, manifest):
    """Read pre-existing A timestamps and all B per-row state exactly."""
    a_user = int(manifest['a']['userId'])
    ids = [int(item) for item in manifest['preReadMessageIds']]
    if len(ids) != 20 or len(set(ids)) != 20:
        raise RuntimeError('Pre-read synthetic ID set differs')
    rows = mysql(cid, 'SELECT message_id,read_time FROM notify_message_recipient '
                 f"WHERE user_id={a_user} AND message_id IN ({','.join(str(item) for item in ids)}) "
                 'ORDER BY message_id;').splitlines()
    if len(rows) != 20 or any(len(row.split('\t')) != 2 or row.split('\t')[1] == 'NULL' for row in rows):
        raise RuntimeError('Pre-read snapshot is missing a seeded timestamp')
    oldest = int(manifest['aOldest']['messageId'])
    seen = mysql(cid, f'SELECT seen_time FROM notify_message_recipient '
                 f'WHERE user_id={a_user} AND message_id={oldest};')
    if not seen or seen == 'NULL':
        raise RuntimeError('Off-page seen timestamp is missing')
    b_user = int(manifest['b']['userId'])
    b_rows = mysql(cid, 'SELECT message_id,seen_time,read_time FROM notify_message_recipient '
                   f'WHERE user_id={b_user} ORDER BY message_id;').splitlines()
    expected_b_ids = {manifest['shared']['messageId'], manifest['bOnly']['messageId']}
    if (len(b_rows) != 2 or {row.split('\t')[0] for row in b_rows} != expected_b_ids
        or any(row.split('\t')[1:] != ['NULL', 'NULL'] for row in b_rows)):
        raise RuntimeError('B per-message read/seen baseline differs')
    return {'top20_read_rows': rows, 'oldest_seen_time': seen, 'b_rows': b_rows}


def create_owned_b_role(cid, run_id, a_user, b_user, client_pk):
    role_id = 8_200_000_000_000_000_000 + secrets.randbelow(100_000_000_000_000)
    permissions = (2100600000000000001, 2100600000000000040,
                   2100600000000000041, 2100600000000000042)
    existing = int(mysql(cid, f'SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r '
                         f'ON r.role_id=ur.role_id WHERE ur.user_id={b_user} '
                         f"AND r.client_id={client_pk} AND r.status='0' AND r.del_flag='0';"))
    eligible = int(mysql(cid, 'SELECT COUNT(*) FROM sys_menu WHERE menu_id IN ('
                         + ','.join(str(item) for item in permissions)
                         + f") AND client_id={client_pk} AND status='0';"))
    collision = int(mysql(cid, f'SELECT COUNT(*) FROM sys_role WHERE role_id={role_id};'))
    if existing or eligible != 4 or collision:
        raise RuntimeError('B role or owned menu/client baseline differs')
    role_key = 't41_b_' + run_id
    statement = (
        'START TRANSACTION;'
        'INSERT INTO sys_role(role_id,client_id,role_name,role_key,role_sort,data_scope,'
        'menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark) VALUES '
        f'({role_id},{client_pk},{hexsql("T41 Own B")},{hexsql(role_key)},99,'
        f"'5',1,1,'0','0',{a_user},NOW(),{hexsql('T41 owned browser only')});"
        f'INSERT INTO sys_user_role(user_id,role_id) VALUES ({b_user},{role_id});'
        'INSERT INTO sys_role_menu(role_id,menu_id) VALUES '
        + ','.join(f'({role_id},{menu_id})' for menu_id in permissions) + ';COMMIT;')
    mysql(cid, statement)
    effective = int(mysql(cid, 'SELECT COUNT(DISTINCT rm.menu_id) FROM sys_user_role ur '
                          'JOIN sys_role r ON r.role_id=ur.role_id '
                          'JOIN sys_role_menu rm ON rm.role_id=r.role_id '
                          'JOIN sys_menu m ON m.menu_id=rm.menu_id '
                          f'WHERE ur.user_id={b_user} AND r.role_id={role_id} '
                          f"AND r.client_id={client_pk} AND r.data_scope='5' "
                          "AND r.status='0' AND r.del_flag='0' "
                          f"AND m.client_id={client_pk} AND m.status='0' "
                          'AND rm.menu_id IN (' + ','.join(str(item) for item in permissions) + ');'))
    if effective != 4:
        raise RuntimeError('Owned B role did not grant exactly the required active menus')
    return role_id


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
    if len(cases) != 1:
        raise RuntimeError('Playwright reporter did not identify exactly one case')
    source, title, project, results = cases[0]
    if not isinstance(source, str) or not isinstance(title, str) or len(results) != 1:
        raise RuntimeError('Playwright exact case identity or attempt count differs')
    expected = (ROOT / TEST_PATH).resolve(strict=True)
    actual = Path(source)
    candidates = ([actual.resolve()] if actual.is_absolute() else
                  [(base / actual).resolve() for base in (ROOT, ROOT / 'frontend', ROOT / 'frontend/e2e')])
    if expected not in candidates or title != TEST_TITLE or project != 'chromium':
        raise RuntimeError('Playwright file, title or Chrome project differs')
    if results[0].get('status') != 'passed':
        raise RuntimeError('Playwright case did not pass on its only attempt')
    return {'file': TEST_PATH, 'title': title, 'project': project, 'attempts': 1}


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
    if len(cases) != 1:
        return None
    source, spec, case = cases[0]
    expected = ROOT / TEST_PATH
    def exact_file(value):
        if not isinstance(value, str):
            return False
        path = Path(value)
        candidates = ([path.resolve()] if path.is_absolute() else
                      [(base / path).resolve() for base in (ROOT, ROOT / 'frontend', ROOT / 'frontend/e2e')])
        return expected.resolve() in candidates
    if (not exact_file(source) or spec.get('title') != TEST_TITLE or
            case.get('projectName') != 'chromium'):
        return None
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
    declaration = location(spec, source_required=False)
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
    return {'file': TEST_PATH, 'title': TEST_TITLE, 'project': 'chromium',
            'attempts': len(results), 'status': status,
            'case_declaration': declaration, 'assertion_location': assertion}


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
    if counts != {'expected': 1, 'unexpected': 0, 'skipped': 0, 'flaky': 0}:
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
              'generated_child_env_names': ['T41_A_USERNAME', 'T41_A_PASSWORD',
                                            'T41_B_USERNAME', 'T41_B_PASSWORD'],
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
    app_user = 't41b_' + run_id
    minio_user = 't41minio' + run_id
    bucket = 't41-' + run_id
    account_env = {'T41_A_USERNAME': 'WTA', 'T41_A_PASSWORD': random_password(),
                   'T41_B_USERNAME': 'test1', 'T41_B_PASSWORD': random_password()}
    redactions = [root_password, app_password, redis_password, minio_password,
                  account_env['T41_A_PASSWORD'], account_env['T41_B_PASSWORD']]
    report = {'gate': 'T-41 real Admin inbox page 26 and global read-all',
              'run_id': run_id, 'acceptance': False, 'exit_code': 1,
              'started_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'owned': {}, 'cleanup': {}, 'source_before': None, 'artifact_before': None}
    captured, volumes, ports, processes, errors = [], [], [], [], []
    mysql_id = None
    browser_complete = False
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
        mysql_id = full_id(docker('run', '--pull=never', '-d', '--name', 't41-browser-mysql-' + run_id,
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
        redis_id = full_id(docker('run', '--pull=never', '-d', '--name', 't41-browser-redis-' + run_id,
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
        minio_id = full_id(docker('run', '--pull=never', '-d', '--name', 't41-browser-minio-' + run_id,
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
        tables = int(mysql(mysql_id, 'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();'))
        outboxes = int(mysql(mysql_id, 'SELECT COUNT(*) FROM notify_outbox;'))
        external = int(mysql(mysql_id, "SELECT COUNT(*) FROM notify_delivery WHERE channel IN ('SMS','MAIL');"))
        enabled_accounts = int(mysql(mysql_id,
                                     "SELECT COUNT(*) FROM notify_channel_account WHERE channel IN ('SMS','MAIL') AND enabled='Y';"))
        private_oss = int(mysql(mysql_id,
                                "SELECT COUNT(*) FROM sys_oss_config WHERE config_key='minio' AND status='Y' AND access_policy='0';"))
        if (tables, outboxes, external, enabled_accounts, private_oss) != (103, 0, 0, 0, 1):
            raise RuntimeError('Owned schema violates fresh no-supplier-call baseline')
        report['owned']['baseline_counts'] = {
            'business_tables': tables, 'outboxes': outboxes, 'external_deliveries': external,
            'enabled_external_accounts': enabled_accounts, 'private_default_oss': private_oss}

        def one_user(username):
            rows = mysql(mysql_id, 'SELECT user_id FROM sys_user WHERE user_name='
                         + hexsql(username) + " AND status='0' AND del_flag='0';").splitlines()
            if len(rows) != 1 or not rows[0].isdigit():
                raise RuntimeError('Owned enabled fixture user is absent or ambiguous')
            return int(rows[0])
        a_user, b_user = one_user('WTA'), one_user('test1')
        client_rows = mysql(mysql_id, 'SELECT id FROM sys_client WHERE client_id='
                            + hexsql(ADMIN_CLIENT_ID) + " AND status='0' AND del_flag='0';").splitlines()
        if len(client_rows) != 1 or not client_rows[0].isdigit():
            raise RuntimeError('Owned Admin Client fixture differs')
        client_pk = int(client_rows[0])
        rotate_owned_login(mysql_id, a_user, account_env['T41_A_PASSWORD'])
        rotate_owned_login(mysql_id, b_user, account_env['T41_B_PASSWORD'])
        report['owned']['login_passwords_rotated_in_fresh_schema'] = 2
        before_counts = inbox_counts(mysql_id, a_user, b_user)
        if before_counts:
            raise RuntimeError('Fresh owned schema already has A/B inbox recipients')
        report['owned']['b_role_id'] = create_owned_b_role(mysql_id, run_id, a_user, b_user, client_pk)
        seed_sql, manifest = seed_plan(run_id, a_user, b_user)
        mysql(mysql_id, seed_sql)
        report['seed_before'] = verify_seed(mysql_id, manifest)
        preserved_before = preservation_snapshot(mysql_id, manifest)
        seed_path = run_dir / 'seed.json'
        private_bytes(seed_path, (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode())
        report['seed_manifest_sha256'] = sha_file(seed_path)

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
        wait_http(backend_port, '/auth/code', backend, 240)

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
        wait_http(vite_port, '/login', vite, 120)
        report['owned']['loopback_ports'].update({'backend': backend_port, 'vite': vite_port})

        playwright_env = safe_env(T41_ADMIN_ORIGIN=f'http://127.0.0.1:{vite_port}',
                                   T41_BACKEND_ORIGIN=f'http://127.0.0.1:{backend_port}',
                                   T41_SEED_MANIFEST=str(seed_path), T41_RUN_ID=run_id,
                                   CI='1', **account_env)
        playwright_argv = ('node', str(product_paths[3]), 'test', str(product_paths[0]),
                           '--config', str(product_paths[1]), '--grep', TEST_TITLE,
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
        report['seed_after'] = verify_after_browser(mysql_id, manifest)
        preserved_after = preservation_snapshot(mysql_id, manifest)
        if preserved_after != preserved_before:
            raise RuntimeError('Global read-all rewrote pre-existing read or seen timestamps')
        report['preexisting_read_and_seen_timestamps_preserved'] = True
        report['exit_code'] = 0
    except BaseException as exc:
        report['error'] = redact(type(exc).__name__ + ': ' + str(exc), redactions)
    finally:
        if mysql_id is not None and browser_complete and 'seed_after' not in report:
            try:
                report['seed_after_failed_check'] = inbox_counts(
                    mysql_id, int(manifest['a']['userId']), int(manifest['b']['userId']))
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
            print('T-41 browser result write failed', file=sys.stderr)
        print(json.dumps({'result': str(run_dir / 'result.json'), 'exit_code': report['exit_code']},
                         ensure_ascii=False))
    return report['exit_code']


if __name__ == '__main__':
    sys.exit(main())
