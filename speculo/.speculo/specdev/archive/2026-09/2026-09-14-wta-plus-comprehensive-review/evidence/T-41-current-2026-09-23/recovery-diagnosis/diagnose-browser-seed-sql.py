#!/usr/bin/env python3
"""Isolated T41 SQL fixture diagnosis; never a browser or ticket acceptance run.

Without --execute this performs only source/fixture preflight. Execution is
reserved for the Lead after review; it creates one dual-labelled owned MySQL.
"""

import argparse
import datetime as dt
import importlib.util
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys

ROOT = Path('/srv/WTA-plus')
DRIVER = ROOT / 'frontend/e2e/run-inbox-paged-real.py'
HELPER_SHA = 'bd1798bba7a3ea78640e054e11f04905faf56ca38b8594196e18fc379c4c1f88'
EXPECTED_HEAD = 'd58fc3d1fdc7e0fd885b3fd217e76395d0ff5347'
OWNER = 'T41-SQL-DIAG'
RUN_ROOT = Path('/tmp/wta-t41/sql-diagnostic-runs')
MYSQL_COMMAND = ('sh', '-c',
                 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch --skip-column-names --database="$1"',
                 'sh', 'wta-plus')
STAGES = frozenset((
    'metadata_columns', 'metadata_expression', 'baseline_tables', 'baseline_outbox',
    'baseline_external', 'baseline_accounts', 'baseline_private_oss',
    'lookup_A_original', 'lookup_A_collated', 'lookup_B_original', 'lookup_B_collated',
    'lookup_client_original', 'lookup_client_collated', 'rotate_A', 'rotate_B',
    'role_existing', 'role_menus', 'role_collision', 'role_insert', 'role_effective',
    'seed_insert', 'seed_count', 'title_like_original', 'title_like_collated',
    'seed_ownership', 'preserve_A_reads', 'preserve_A_seen', 'preserve_B'
))


def load_helper():
    spec = importlib.util.spec_from_file_location('t41_browser_resource_helper', DRIVER)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.OWNER = OWNER  # Private interpreter only; original product file stays immutable.
    return module


def preflight(helper):
    source = helper.source_identity(EXPECTED_HEAD)
    if helper.sha_file(DRIVER) != HELPER_SHA:
        raise RuntimeError('Frozen product resource helper SHA differs')
    if not helper.INIT_SCRIPT.is_file() or not all((helper.SQL_ROOT / name).is_file() for name in helper.SQL_NAMES):
        raise RuntimeError('Exact six-SQL initializer inputs missing')
    init_text = helper.INIT_SCRIPT.read_text()
    if not all(marker in init_text for marker in (
        'CREATE DATABASE `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci',
        'EXPECTED_TABLES=103', 'UPDATE sys_oss_config SET access_key=')):
        raise RuntimeError('Owned initializer changed from reviewed six-SQL contract')
    return {'source': source, 'helper_sha256': HELPER_SHA,
            'six_sql_sha256': {name: helper.sha_file(helper.SQL_ROOT / name) for name in helper.SQL_NAMES}}


def sql_error(stderr):
    """The raw stderr exists in memory only; return a strict numeric allowlist."""
    match = re.search(rb'\bERROR\s+([0-9]{1,5})\s+\(([A-Z0-9]{5})\)', stderr)
    if not match:
        return {'mysql_error_number': None, 'sqlstate': None}
    return {'mysql_error_number': int(match.group(1)), 'sqlstate': match.group(2).decode('ascii')}


class StageFailure(Exception):
    def __init__(self, stage):
        self.stage = stage
        super().__init__(stage)


def query(helper, cid, sql, stage, report):
    if stage not in STAGES:
        raise RuntimeError('Unreviewed diagnostic stage')
    payload = sql.encode('utf-8') if isinstance(sql, str) else sql
    result = helper.run((*helper.DOCKER, 'exec', '-i', cid, *MYSQL_COMMAND), data=payload, timeout=180)
    row = {'stage': stage, 'exit_code': result.returncode}
    if result.returncode:
        row.update(sql_error(result.stderr))
    report['stages'].append(row)
    if result.returncode:
        raise StageFailure(stage)
    return result.stdout.decode('utf-8', errors='strict').strip()


def collated(sql):
    if not isinstance(sql, str):
        raise RuntimeError('Collation control requires a fixed string query')
    updated, count = re.subn(r'CONVERT\(0x[0-9a-f]+ USING utf8mb4\)',
                              lambda m: m.group(0) + ' COLLATE utf8mb4_general_ci', sql, count=1)
    if count != 1:
        raise RuntimeError('Original comparison shape differs')
    return updated


def one_decimal(output, stage):
    rows = output.splitlines()
    if len(rows) != 1 or not re.fullmatch(r'[0-9]+', rows[0]):
        raise StageFailure(stage + '_unexpected_result_shape')
    return int(rows[0])


def lookup(helper, cid, sql, stage, report):
    try:
        raw = query(helper, cid, sql, stage + '_original', report)
        report['stages'][-1]['row_count'] = len(raw.splitlines())
        return one_decimal(raw, stage)
    except StageFailure as error:
        if error.stage != stage + '_original':
            raise
        control = query(helper, cid, collated(sql), stage + '_collated', report)
        report['stages'][-1]['row_count'] = len(control.splitlines())
        return one_decimal(control, stage + '_collated')


def legacy_steps(helper, cid, run_id, report):
    # Metadata exposes only permitted collation labels, never a user/client value.
    metadata = query(helper, cid,
        "SELECT TABLE_NAME,COLUMN_NAME,COLLATION_NAME FROM information_schema.COLUMNS "
        "WHERE TABLE_SCHEMA=DATABASE() AND ((TABLE_NAME='sys_user' AND COLUMN_NAME='user_name') "
        "OR (TABLE_NAME='sys_client' AND COLUMN_NAME='client_id') "
        "OR (TABLE_NAME='notify_message' AND COLUMN_NAME='title')) ORDER BY TABLE_NAME,COLUMN_NAME;",
        'metadata_columns', report)
    allowed_columns = {('sys_user', 'user_name'), ('sys_client', 'client_id'), ('notify_message', 'title')}
    observed = {}
    for line in metadata.splitlines():
        table, column, label = line.split('\t')
        if (table, column) not in allowed_columns or label not in ('utf8mb4_general_ci', 'utf8mb4_0900_ai_ci'):
            raise StageFailure('metadata_columns_unexpected_result_shape')
        observed[table + '.' + column] = label
    if set(observed) != {table + '.' + column for table, column in allowed_columns}:
        raise StageFailure('metadata_columns_missing')
    report['collations'] = observed
    expression = query(helper, cid,
                       'SELECT COLLATION(CONVERT(0x41 USING utf8mb4)),COERCIBILITY(CONVERT(0x41 USING utf8mb4));',
                       'metadata_expression', report).split('\t')
    if len(expression) != 2 or expression[0] not in ('utf8mb4_general_ci', 'utf8mb4_0900_ai_ci') or not expression[1].isdigit():
        raise StageFailure('metadata_expression_unexpected_result_shape')
    report['expression_collation'] = {'label': expression[0], 'coercibility': int(expression[1])}

    baselines = (
        ('baseline_tables', 'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();', 103),
        ('baseline_outbox', 'SELECT COUNT(*) FROM notify_outbox;', 0),
        ('baseline_external', "SELECT COUNT(*) FROM notify_delivery WHERE channel IN ('SMS','MAIL');", 0),
        ('baseline_accounts', "SELECT COUNT(*) FROM notify_channel_account WHERE channel IN ('SMS','MAIL') AND enabled='Y';", 0),
        ('baseline_private_oss', "SELECT COUNT(*) FROM sys_oss_config WHERE config_key='minio' AND status='Y' AND access_policy='0';", 1)
    )
    for stage, sql, expected in baselines:
        actual = one_decimal(query(helper, cid, sql, stage, report), stage)
        report['stages'][-1]['count'] = actual
        if actual != expected:
            raise StageFailure(stage + '_count_differs')

    a = lookup(helper, cid, 'SELECT user_id FROM sys_user WHERE user_name=' + helper.hexsql('WTA')
               + " AND status='0' AND del_flag='0';", 'lookup_A', report)
    b = lookup(helper, cid, 'SELECT user_id FROM sys_user WHERE user_name=' + helper.hexsql('test1')
               + " AND status='0' AND del_flag='0';", 'lookup_B', report)
    client = lookup(helper, cid, 'SELECT id FROM sys_client WHERE client_id=' + helper.hexsql(helper.ADMIN_CLIENT_ID)
                    + " AND status='0' AND del_flag='0';", 'lookup_client', report)
    if a == b:
        raise StageFailure('lookup_duplicate_users')

    for stage, user in (('rotate_A', a), ('rotate_B', b)):
        password_hash = helper.owned_bcrypt_hash(helper.random_password())
        sql = 'UPDATE sys_user SET password=' + helper.hexsql(password_hash)
        sql += f" WHERE user_id={user} AND status='0' AND del_flag='0'; SELECT ROW_COUNT();"
        changed = one_decimal(query(helper, cid, sql, stage, report), stage)
        report['stages'][-1]['affected_rows'] = changed
        if changed != 1:
            raise StageFailure(stage + '_rowcount_differs')

    # The production helper retains the exact role/seed operations. Only its
    # mysql adapter is replaced inside this private interpreter for safe codes.
    original_mysql = helper.mysql
    role_stages = iter(('role_existing', 'role_menus', 'role_collision', 'role_insert', 'role_effective'))
    helper.mysql = lambda _cid, sql: query(helper, cid, sql, next(role_stages), report)
    try:
        helper.create_owned_b_role(cid, run_id, a, b, client)
    finally:
        helper.mysql = original_mysql
    report['role_created_and_effective'] = True

    seed_sql, manifest = helper.seed_plan(run_id, a, b)
    query(helper, cid, seed_sql, 'seed_insert', report)
    seed_stages = iter(('seed_count', 'title_like_original', 'seed_ownership'))
    def seed_mysql(_cid, sql):
        stage = next(seed_stages)
        if stage != 'title_like_original':
            return query(helper, cid, sql, stage, report)
        try:
            return query(helper, cid, sql, stage, report)
        except StageFailure as error:
            if error.stage != stage:
                raise
            return query(helper, cid, collated(sql), 'title_like_collated', report)
    helper.mysql = seed_mysql
    try:
        counts = helper.verify_seed(cid, manifest)
    finally:
        helper.mysql = original_mysql
    report['fixture_counts'] = {
        'a_total': counts['a']['total'], 'a_unread': counts['a']['unread'],
        'b_total': counts['b']['total'], 'b_unread': counts['b']['unread'],
        'messages': counts['synthetic_messages'], 'relations': counts['synthetic_relations'],
        'shared_relationships': counts['shared_relationships']}
    preservation_stages = iter(('preserve_A_reads', 'preserve_A_seen', 'preserve_B'))
    helper.mysql = lambda _cid, sql: query(helper, cid, sql, next(preservation_stages), report)
    try:
        snapshot = helper.preservation_snapshot(cid, manifest)
    finally:
        helper.mysql = original_mysql
    report['preservation_baseline'] = {
        'a_prior_read_rows': len(snapshot['top20_read_rows']),
        'a_oldest_seen': bool(snapshot['oldest_seen_time']),
        'b_unchanged_row_baseline': len(snapshot['b_rows'])}
    report['diagnostic_complete'] = True


def execute(helper, pre):
    os.umask(0o077)
    run_id = secrets.token_hex(8)
    RUN_ROOT.mkdir(parents=True, exist_ok=True, mode=0o700)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(mode=0o700)
    report = {'kind': 'T41_SQL_DIAG_ONLY', 'acceptance': False, 'diagnostic_complete': False,
              'run_id': run_id, 'source_before': pre['source'], 'helper_sha256': HELPER_SHA,
              'six_sql_sha256': pre['six_sql_sha256'], 'stages': [], 'owned': {},
              'cleanup': {'errors': []}, 'started_utc': dt.datetime.now(dt.timezone.utc).isoformat()}
    root_password, app_password, minio_password = (helper.random_password() for _ in range(3))
    app_user = 't41diag_' + run_id
    minio_user = 't41diagminio' + run_id
    bucket = 't41diag-' + run_id
    container_env = run_dir / 'mysql-container.env'
    init_env = run_dir / 'init.env'
    captured, volumes, ports, processes = [], [], [], []
    cid = None
    try:
        helper.private_bytes(container_env, ('MYSQL_ROOT_PASSWORD=' + root_password + '\n').encode())
        cid = helper.full_id(helper.docker('run', '--pull=never', '-d', '--name', 't41-sql-diag-' + run_id,
                                          '--label', 'namewta.test.owner=' + OWNER,
                                          '--label', 'namewta.test.run=' + run_id,
                                          '-p', '127.0.0.1::3306', '--env-file', str(container_env),
                                          'mysql:8.4.9', '--character-set-server=utf8mb4',
                                          '--collation-server=utf8mb4_general_ci',
                                          '--log-bin-trust-function-creators=1'))
        captured.append(cid)
        helper.assert_owned(cid, run_id)
        volumes.extend(helper.volume_names(cid))
        helper.wait_mysql(cid)
        port = helper.mapped_port(cid, '3306')
        ports.append(port)
        report['owned']['mysql_container_full_id'] = cid
        report['owned']['loopback_port'] = port
        report['owned']['captured_anonymous_volumes'] = list(volumes)
        helper.private_bytes(init_env, ('MYSQL_DATABASE=wta-plus\nMYSQL_APP_USER=' + app_user
                         + '\nMYSQL_APP_PASSWORD=' + app_password + '\nMINIO_ROOT_USER=' + minio_user
                         + '\nMINIO_ROOT_PASSWORD=' + minio_password
                         + '\nMINIO_ENDPOINT=127.0.0.1:1\nMINIO_BUCKET=' + bucket + '\n').encode())
        docker_config = run_dir / 'docker-config'
        docker_config.mkdir(mode=0o700)
        argv = ('bash', str(helper.INIT_SCRIPT), '--container', cid,
                '--env-file', str(init_env), '--sql-dir', str(helper.SQL_ROOT))
        # stdout/stderr remain in memory; the initializer only writes owned SQL.
        proc = subprocess.Popen(argv, cwd=ROOT, env=helper.safe_env(
            DOCKER_HOST='unix:///var/run/docker.sock', DOCKER_CONFIG=str(docker_config)),
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, start_new_session=True)
        processes.append(('initializer', proc))
        try:
            _stdout, stderr = proc.communicate(timeout=900)
        except subprocess.TimeoutExpired:
            helper.stop_group(proc)
            _stdout, stderr = proc.communicate(timeout=15)
        report['initializer_exit_code'] = proc.returncode
        if proc.returncode:
            report['initializer_error'] = sql_error(stderr)
            raise StageFailure('initializer')
        legacy_steps(helper, cid, run_id, report)
    except BaseException as error:
        report['stopped_at_stage'] = error.stage if isinstance(error, StageFailure) else 'unclassified_diagnostic_exception'
        report['exception_type'] = type(error).__name__
    finally:
        errors = report['cleanup']['errors']
        for name, proc in reversed(processes):
            try:
                live = helper.stop_group(proc)
                report['cleanup'][name + '_process_group'] = {'pgid': proc.pid, 'live_members': live}
                if live:
                    errors.append(name + '_process_group_remains')
            except Exception:
                errors.append(name + '_process_group_cleanup_failed')
        # Capture volume names from every still-discoverable owned container
        # before rm -fv; never inspect or delete a container without both labels.
        try:
            for owned in helper.owned_ids(run_id):
                helper.assert_owned(owned, run_id)
                volumes.extend(helper.volume_names(owned))
        except Exception:
            errors.append('owned_volume_discovery_failed')
        remaining, container_errors = helper.cleanup_containers(run_id, captured)
        report['cleanup']['remaining_owned_full_ids'] = remaining
        errors.extend(container_errors)
        report['owned']['captured_anonymous_volumes'] = list(dict.fromkeys(volumes))
        try:
            report['cleanup']['anonymous_volumes_absent'] = {
                name: helper.volume_absent(name) for name in dict.fromkeys(volumes)}
            if not all(report['cleanup']['anonymous_volumes_absent'].values()):
                errors.append('owned_anonymous_volume_remains')
        except Exception:
            errors.append('owned_volume_absence_check_failed')
        try:
            report['cleanup']['loopback_ports_closed'] = {str(port): helper.closed(port) for port in ports}
            if not all(report['cleanup']['loopback_ports_closed'].values()):
                errors.append('owned_port_remains')
        except Exception:
            errors.append('owned_port_absence_check_failed')
        for path in (container_env, init_env):
            try:
                path.unlink(missing_ok=True)
            except Exception:
                errors.append('private_env_removal_failed')
        try:
            import shutil
            shutil.rmtree(run_dir / 'docker-config')
        except FileNotFoundError:
            pass
        except Exception:
            errors.append('private_docker_config_removal_failed')
        try:
            report['source_after'] = helper.source_identity(EXPECTED_HEAD)
            if report['source_after'] != report['source_before']:
                errors.append('source_identity_changed')
        except Exception:
            errors.append('source_after_not_clean_exact_head')
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        report['diagnostic_success'] = bool(report['diagnostic_complete'] and not errors)
        report['exit_code'] = 0 if report['diagnostic_success'] else 1
        helper.private_bytes(run_dir / 'result.json', (json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode())
        print(json.dumps({'result': str(run_dir / 'result.json'), 'exit_code': report['exit_code']}))
    return report['exit_code']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--execute', action='store_true')
    args = parser.parse_args()
    helper = load_helper()
    pre = preflight(helper)
    if not args.execute:
        print(json.dumps({'preflight': 'passed', 'source': pre['source'],
                          'helper_sha256': HELPER_SHA, 'six_sql_files': len(pre['six_sql_sha256']),
                          'execution': 'not_started'}))
        return 0
    return execute(helper, pre)


if __name__ == '__main__':
    sys.exit(main())
