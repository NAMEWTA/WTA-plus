#!/usr/bin/env python3
"""T-34 owned loopback MySQL/Redis/JAR/Vite + real Playwright gate.

Run after compiler/typecheck jobs finish; supply clean HEAD and the core JAR build SHA.
Never points at deployment services. It removes only resources bearing this run's labels.
"""
import argparse
import datetime as dt
import hashlib
import json
import os
import pathlib
import secrets
import shutil
import signal
import socket
import subprocess
import sys
import time
import urllib.request
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
SQL_NAMES = ('10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql',
             '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql')
TEST_NAME = 'T-34 real Admin inbox without realtime'
TEST_PATH = 'frontend/e2e/inbox-real.e2e.ts'
CONFIG_PATH = 'frontend/e2e/playwright.inbox-real.config.ts'
OWNER_LABEL = 'namewta.test.owner=T-34'
RUN_LABEL = 'namewta.test.run='

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def command(args, *, cwd=None, env=None, stdin=None, timeout=120):
    return subprocess.run(args, cwd=cwd, env=env, input=stdin, capture_output=True,
                          timeout=timeout, check=False)

def decode(data):
    return data.decode('utf-8', 'replace').strip()

def git(root, *args):
    result = command(['git', *args], cwd=root)
    if result.returncode:
        raise RuntimeError('git inspection failed: ' + decode(result.stderr)[:300])
    return decode(result.stdout)

def clean_source(root, head):
    actual = git(root, 'rev-parse', 'HEAD')
    status = git(root, 'status', '--porcelain', '--untracked-files=all')
    if actual != head or status:
        raise RuntimeError('Source must be exact expected HEAD and clean; found HEAD=' + actual + ', dirty=' + str(bool(status)))
    return {'head': actual, 'tree': git(root, 'rev-parse', 'HEAD^{tree}'), 'clean': True}

def port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]

def port_closed(number):
    with socket.socket() as sock:
        sock.settimeout(0.5)
        return sock.connect_ex(('127.0.0.1', number)) != 0

def limited_env(**updates):
    keys = ('PATH', 'HOME', 'LANG', 'LC_ALL', 'JAVA_HOME', 'NODE_OPTIONS', 'TMPDIR')
    env = {key: os.environ[key] for key in keys if key in os.environ and key != 'NODE_OPTIONS'}
    env.update(updates)
    return env

def docker(*args, stdin=None, timeout=180):
    # Explicit local socket prevents inherited remote contexts.
    result = command(['docker', '--host', 'unix:///var/run/docker.sock', *args],
                     env=limited_env(), stdin=stdin, timeout=timeout)
    if result.returncode:
        raise RuntimeError('local Docker command failed: ' + ' '.join(args[:3]) +
                           ' exit=' + str(result.returncode) + ' ' + decode(result.stderr)[:500])
    return decode(result.stdout)

def mysql_exec(cid, sql, *, database='wta-plus'):
    return docker('exec', '-i', cid, 'sh', '-c',
                  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -uroot -N -B ' + database,
                  stdin=sql.encode() if isinstance(sql, str) else sql)

def wait_for(url, process=None, seconds=180):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if process is not None and process.poll() is not None:
            raise RuntimeError('Owned process exited before HTTP readiness: ' + url)
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                if response.status == 200:
                    return
        except Exception:
            pass
        time.sleep(1)
    raise RuntimeError('Owned HTTP readiness timeout: ' + url)

def live_group_members(pgid):
    """Inspect our Linux session so an exited leader cannot hide live children."""
    members = []
    for entry in pathlib.Path('/proc').iterdir():
        if not entry.name.isdigit():
            continue
        try:
            fields = (entry / 'stat').read_text().rsplit(')', 1)[1].split()
        except (FileNotFoundError, ProcessLookupError):
            continue
        if len(fields) >= 4 and fields[0] not in ('Z', 'X') and int(fields[2]) == pgid and int(fields[3]) == pgid:
            members.append(int(entry.name))
    return members

def stop(proc, *, term_seconds=15, kill_seconds=10):
    """Stop the owned process group, even when its leader has already exited."""
    pgid = proc.pid
    for sig, seconds in ((signal.SIGTERM, term_seconds), (signal.SIGKILL, kill_seconds)):
        if not live_group_members(pgid):
            proc.poll()  # Reap an exited leader.
            return
        try:
            os.killpg(pgid, sig)
        except ProcessLookupError:
            pass  # A concurrent exit is confirmed by the following scan.
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            if not live_group_members(pgid):
                proc.poll()
                return
            time.sleep(0.1)
    remaining = live_group_members(pgid)
    if remaining:
        raise RuntimeError('Owned process group still has live members: ' + str(pgid))
    proc.poll()

def playwright_identity(data, root):
    """Require the single reporter case to be the fixed source file and title."""
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
        raise RuntimeError('Playwright reporter did not identify exactly one test')
    source, title, project, results = cases[0]
    if not isinstance(source, str) or not isinstance(title, str) or not results:
        raise RuntimeError('Playwright reporter test identity is incomplete')
    expected = (root / TEST_PATH).resolve(strict=True)
    actual = pathlib.Path(source)
    candidates = ([actual.resolve()] if actual.is_absolute() else
                  [(base / actual).resolve() for base in
                   (root, root / 'frontend', root / 'frontend/e2e')])
    if expected not in candidates or title != TEST_NAME:
        raise RuntimeError('Playwright reporter test file/title differs from T-34 gate')
    return {'file': TEST_PATH, 'title': title, 'project': project, 'attempts': len(results)}

def owned_container_ids(run_id):
    """Discover only containers with both exact owner and random run labels."""
    found = docker('ps', '-aq', '--filter', 'label=' + OWNER_LABEL,
                   '--filter', 'label=' + RUN_LABEL + run_id)
    return list(dict.fromkeys(found.splitlines())) if found else []

def verify_owned_container(cid, run_id):
    labels = json.loads(docker('inspect', '--format', '{{json .Config.Labels}}', cid))
    if labels.get('namewta.test.owner') != 'T-34' or labels.get('namewta.test.run') != run_id:
        raise RuntimeError('Discovered container does not have both exact T-34 labels')

def cleanup_resources(processes, logs, containers, run, run_id, redactions, report):
    """Attempt every cleanup action independently; return errors for gate verdict."""
    errors = []
    for proc in reversed(processes):
        try:
            stop(proc)
        except Exception as exc:
            key = 'process_error_' + str(proc.pid)
            report['cleanup'][key] = type(exc).__name__
            errors.append(key)
        try:
            remaining = live_group_members(proc.pid)
            report['cleanup']['process_group_' + str(proc.pid)] = {'live_members': remaining}
            if remaining:
                errors.append('process_group_remaining_' + str(proc.pid))
        except Exception as exc:
            key = 'process_group_check_error_' + str(proc.pid)
            report['cleanup'][key] = type(exc).__name__
            errors.append(key)
    for index, logfile in enumerate(logs):
        try:
            logfile.close()
        except Exception as exc:
            key = 'log_close_error_' + str(index)
            report['cleanup'][key] = type(exc).__name__
            errors.append(key)
    for name in ('backend', 'vite'):
        raw = run / (name + '.raw.log')
        try:
            if not raw.exists():
                continue
            content = raw.read_text(errors='replace')
            for value in redactions:
                content = content.replace(value, '[REDACTED]')
            (run / (name + '.log')).write_text(content)
        except Exception as exc:
            key = 'log_redaction_error_' + name
            report['cleanup'][key] = type(exc).__name__
            errors.append(key)
        finally:
            try:
                raw.unlink(missing_ok=True)
            except Exception as exc:
                key = 'raw_log_remove_error_' + name
                report['cleanup'][key] = type(exc).__name__
                errors.append(key)
    try:
        discovered = owned_container_ids(run_id)
    except Exception as exc:
        discovered = []
        report['cleanup']['container_discovery_error'] = type(exc).__name__
        errors.append('container_discovery_error')
    for cid in reversed(discovered):
        try:
            verify_owned_container(cid, run_id)
            docker('rm', '-fv', cid, timeout=60)
            report['cleanup'][cid] = 'removed'
        except Exception as exc:
            report['cleanup'][cid] = 'remove_failed:' + type(exc).__name__
            errors.append('container_remove_error')
    try:
        remaining = owned_container_ids(run_id)
        report['cleanup']['owned_containers_remaining'] = remaining
        if remaining:
            errors.append('owned_containers_remaining')
    except Exception as exc:
        report['cleanup']['owned_containers_remaining'] = 'unknown:' + type(exc).__name__
        errors.append('container_confirmation_error')
    for cid in containers:
        if cid not in discovered:
            report['cleanup']['captured_container_check_' + cid] = 'not_found_with_exact_labels'
            errors.append('captured_container_not_found_with_exact_labels')
    return errors

def gate_exit_code(report, cleanup_errors, expected_source_after):
    return int(bool(report['exit_code'] or cleanup_errors or
                    report.get('source_after') != expected_source_after))

def hexsql(value):
    return 'CONVERT(0x' + value.encode('utf-8').hex() + ' USING utf8mb4)'

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=pathlib.Path, default=ROOT)
    parser.add_argument('--expected-head', required=True, help='Exact clean checkout commit for browser run')
    parser.add_argument('--jar-build-head', required=True, help='Commit used for the core backend package')
    parser.add_argument('--expected-jar-sha256', required=True, help='SHA-256 from fresh core package record')
    parser.add_argument('--jar', default='backend/wta-admin/target/wta-admin.jar')
    parser.add_argument('--run-root', type=pathlib.Path, default=pathlib.Path('/tmp/namewta-inbox-e2e'))
    args = parser.parse_args()
    os.umask(0o077)
    root = args.root.resolve(strict=True)
    run_id = secrets.token_hex(8)
    run = args.run_root / run_id
    run.mkdir(parents=True, mode=0o700)
    report = {'gate': 'T-34 real Admin inbox without realtime', 'run_id': run_id,
              'started_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
              'run_dir': str(run), 'exit_code': 1, 'owned': {}, 'cleanup': {}, 'counts': None}
    containers = []
    processes = []
    logs = []
    secret = secrets.token_hex(24)
    monitor_secret = secrets.token_hex(16)
    username = 'WTA'  # 50-cde-base-dml.sql baseline fixture
    password = 'admin123'  # Confirmed in existing real login E2E, never printed in report.
    backend_port, vite_port = port(), port()
    mapped_ports = []
    while vite_port == backend_port:
        vite_port = port()
    try:
        report['source'] = clean_source(root, args.expected_head)
        build_head = git(root, 'rev-parse', args.jar_build_head)
        backend_diff = command(['git', 'diff', '--quiet', build_head, args.expected_head, '--', 'backend/', 'release-artifacts/docker/infrastructure/mysql/init/'], cwd=root)
        if backend_diff.returncode != 0:
            raise RuntimeError('Backend or SQL inputs differ from core JAR build commit')
        report['source']['jar_build_head'] = build_head
        report['source']['backend_sql_equal_to_jar_build'] = True
        transport = root / 'backend/wta-common/wta-common-push/src/main/java/org/namewta/common/push/condition/MessageTransportCondition.java'
        autoconfig = root / 'backend/wta-common/wta-common-push/src/main/java/org/namewta/common/push/config/MessageAutoConfiguration.java'
        report['transport_disabled_contract'] = {'backend_message_enabled': False, 'frontend_message_enabled': False,
            'transport_condition_source': str(transport.relative_to(root)), 'transport_condition_sha256': sha(transport),
            'auto_configuration_source': str(autoconfig.relative_to(root)), 'auto_configuration_sha256': sha(autoconfig),
            'browser_must_make_zero_ticket_and_stream_requests': True}
        jar = (root / args.jar).resolve(strict=True)
        config = (root / CONFIG_PATH).resolve(strict=True)
        test = (root / TEST_PATH).resolve(strict=True)
        vite = (root / 'frontend/apps/admin-web/node_modules/vite/bin/vite.js').resolve(strict=True)
        pw = (root / 'frontend/node_modules/@playwright/test/cli.js').resolve(strict=True)
        jar_sha = sha(jar)
        if jar_sha != args.expected_jar_sha256:
            raise RuntimeError('JAR hash differs from fresh package record')
        with zipfile.ZipFile(jar) as bundle:
            names = bundle.namelist()
            if not any(n.startswith('BOOT-INF/lib/wta-notify-') for n in names):
                raise RuntimeError('Core JAR lacks wta-notify')
            if not any(n.startswith('BOOT-INF/lib/wta-system-') for n in names):
                raise RuntimeError('Core JAR lacks wta-system')
        report['artifact'] = {'jar': str(jar.relative_to(root)), 'sha256': jar_sha,
                              'playwright_config': CONFIG_PATH, 'playwright_config_sha256': sha(config),
                              'playwright_test': TEST_PATH, 'playwright_test_sha256': sha(test)}
        sqlroot = root / 'release-artifacts/docker/infrastructure/mysql/init'
        sqlfiles = [sqlroot / name for name in SQL_NAMES]
        report['sql_baselines'] = [{'file': str(p.relative_to(root)), 'sha256': sha(p)} for p in sqlfiles]
        for p in sqlfiles:
            if not p.is_file():
                raise RuntimeError('Missing baseline: ' + p.name)
        label = RUN_LABEL + run_id
        mysql = docker('run', '-d', '--name', 't34-mysql-' + run_id,
                       '--label', OWNER_LABEL, '--label', label,
                       '-p', '127.0.0.1::3306', '-e', 'MYSQL_ROOT_PASSWORD=' + secret,
                       '-e', 'MYSQL_DATABASE=wta-plus', 'mysql:8.4.9',
                       '--character-set-server=utf8mb4', '--collation-server=utf8mb4_general_ci')
        containers.append(mysql)
        report['owned']['mysql_container_id'] = mysql
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            result = command(['docker', '--host', 'unix:///var/run/docker.sock', 'exec', mysql,
                              'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqladmin ping -h 127.0.0.1 -uroot --silent'],
                             env=limited_env(), timeout=10)
            if result.returncode == 0:
                break
            time.sleep(1)
        else:
            raise RuntimeError('Owned MySQL readiness timeout')
        mysql_port = int(docker('port', mysql, '3306/tcp').rsplit(':', 1)[1])
        mapped_ports.append(mysql_port)
        for item in report['sql_baselines']:
            p = root / item['file']
            mysql_exec(mysql, p.read_bytes())
            item['import_exit_code'] = 0
        ids = mysql_exec(mysql, "SELECT user_id FROM sys_user WHERE user_name='WTA' AND status='0' AND del_flag='0';").splitlines()
        if len(ids) != 1 or not ids[0].isdigit():
            raise RuntimeError('Expected exactly one enabled baseline WTA user')
        user_id = int(ids[0])
        message_id = 8000000000000000000 + secrets.randbelow(100000000000000000)
        recipient_id = 8100000000000000000 + secrets.randbelow(100000000000000000)
        title = 'T34 OWNED notice ' + run_id
        body = 'T34 owned body ' + run_id
        seed = (f"INSERT INTO notify_message(message_id,category,channels_json,type,source,title,message,content,create_by,create_time) "
                f"VALUES({message_id},'notice',JSON_ARRAY('IN_APP'),'NOTICE','NOTICE',{hexsql(title)},{hexsql(body)},{hexsql(body)},{user_id},NOW());"
                f"INSERT INTO notify_message_recipient(message_recipient_id,message_id,user_id,read_time,create_by,create_time) "
                f"VALUES({recipient_id},{message_id},{user_id},'2026-09-23 10:00:00',{user_id},NOW());")
        mysql_exec(mysql, seed)
        count = mysql_exec(mysql, f'SELECT COUNT(*) FROM notify_message m JOIN notify_message_recipient r ON r.message_id=m.message_id WHERE m.message_id={message_id} AND r.message_recipient_id={recipient_id} AND r.user_id={user_id};')
        if count != '1':
            raise RuntimeError('Owned inbox seed verification failed')
        report['seed'] = {'message_id': str(message_id), 'recipient_id': str(recipient_id),
                          'user_id': str(user_id), 'joined_rows': 1, 'category': 'notice', 'read_time': '2026-09-23 10:00:00'}
        redis = docker('run', '-d', '--name', 't34-redis-' + run_id,
                       '--label', OWNER_LABEL, '--label', label,
                       '-p', '127.0.0.1::6379', 'redis:8.6.3', 'redis-server',
                       '--save', '', '--appendonly', 'no', '--requirepass', secret)
        containers.append(redis)
        report['owned']['redis_container_id'] = redis
        redis_port = int(docker('port', redis, '6379/tcp').rsplit(':', 1)[1])
        mapped_ports.append(redis_port)
        (run / 'uploads').mkdir()
        back_env = limited_env(
            SPRING_PROFILES_ACTIVE='dev', SERVER_ADDRESS='127.0.0.1', SERVER_PORT=str(backend_port),
            SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL=f'jdbc:mysql://127.0.0.1:{mysql_port}/wta-plus?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
            SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME='root',
            SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=secret,
            SPRING_DATA_REDIS_HOST='127.0.0.1', SPRING_DATA_REDIS_PORT=str(redis_port),
            SPRING_DATA_REDIS_PASSWORD=secret, MESSAGE_ENABLED='false',
            SPRING_BOOT_ADMIN_CLIENT_ENABLED='false', SPRING_BOOT_ADMIN_CLIENT_USERNAME='owned-monitor',
            SPRING_BOOT_ADMIN_CLIENT_PASSWORD=monitor_secret,
            SSO_COOKIE_SECURE='false', SSO_WEB_ORIGIN=f'http://127.0.0.1:{vite_port}',
            WEB_CORS_ALLOWED_ORIGINS=f'http://127.0.0.1:{vite_port}', CAPTCHA_ENABLE='false',
            NACOS_CONFIG_ENABLED='false', SNAIL_JOB_ENABLED='false', SNAIL_AI_ENABLED='false',
            MYBATIS_PLUS_SQL_LOG_ENABLED='false', SPRING_SERVLET_MULTIPART_LOCATION=str(run / 'uploads'))
        backend_log = (run / 'backend.raw.log').open('wb')
        logs.append(backend_log)
        backend = subprocess.Popen(['java', '-Xms128m', '-Xmx768m', '-jar', str(jar)],
                                   cwd=run, env=back_env, stdout=backend_log,
                                   stderr=subprocess.STDOUT, start_new_session=True)
        processes.append(backend)
        report['owned']['backend_pid'] = backend.pid
        wait_for(f'http://127.0.0.1:{backend_port}/auth/code', backend, seconds=240)
        vite_env = limited_env(VITE_APP_PORT=str(vite_port), VITE_APP_CONTEXT_PATH='/',
                               VITE_APP_BASE_API='/dev-api', VITE_APP_BASE_URL='/',
                               VITE_APP_CLIENT_ID='e5cd7e4891bf95d1d19206ce24a7b32e',
                               VITE_APP_PROXY_TARGET=f'http://127.0.0.1:{backend_port}',
                               VITE_APP_MESSAGE_ENABLED='false', BROWSER='none')
        vite_log = (run / 'vite.raw.log').open('wb')
        logs.append(vite_log)
        viteproc = subprocess.Popen(['node', str(vite), 'serve', '--mode', 'development',
                                     '--host', '127.0.0.1', '--port', str(vite_port), '--strictPort'],
                                    cwd=root / 'frontend/apps/admin-web', env=vite_env,
                                    stdout=vite_log, stderr=subprocess.STDOUT, start_new_session=True)
        processes.append(viteproc)
        report['owned']['vite_pid'] = viteproc.pid
        wait_for(f'http://127.0.0.1:{vite_port}/login', viteproc, seconds=120)
        play_env = limited_env(T34_ADMIN_ORIGIN=f'http://127.0.0.1:{vite_port}',
                               T34_USERNAME=username, T34_PASSWORD=password,
                               T34_NOTICE_TITLE=title, T34_NOTICE_BODY=body,
                               CI='1', PLAYWRIGHT_BROWSERS_PATH=os.environ.get('PLAYWRIGHT_BROWSERS_PATH', ''))
        # Direct Playwright CLI avoids a shell; only the dedicated config/test and exact title are selected.
        cmd = ['node', str(pw), 'test', str(test), '--config', str(config),
               '--grep', TEST_NAME, '--workers=1', '--reporter=json',
               '--output', str(run / 'playwright-artifacts')]
        # 浏览器与驱动属于同一进程组；驱动超时也由 finally 回收整个组。
        play_out = (run / 'playwright.json').open('wb')
        play_err = (run / 'playwright.stderr.log').open('wb')
        logs.extend((play_out, play_err))
        playwright = subprocess.Popen(cmd, cwd=root / 'frontend', env=play_env,
                                      stdout=play_out, stderr=play_err, start_new_session=True)
        processes.append(playwright)
        playwright.wait(timeout=360)
        play_out.flush()
        play_err.flush()
        report['playwright_exit_code'] = playwright.returncode
        try:
            data = json.loads((run / 'playwright.json').read_bytes())
            stats = data['stats']
            report['counts'] = {k: int(stats.get(k, 0)) for k in ('expected', 'unexpected', 'skipped', 'flaky')}
            report['playwright_identity'] = playwright_identity(data, root)
        except Exception as exc:
            raise RuntimeError('Playwright JSON reporter missing or wrong test identity: ' + type(exc).__name__) from exc
        if playwright.returncode != 0 or report['counts'] != {'expected': 1, 'unexpected': 0, 'skipped': 0, 'flaky': 0}:
            raise RuntimeError('Real Playwright gate failed or count/skip contract violated')
        report['exit_code'] = 0
    except Exception as exc:
        report['error'] = str(exc).replace(secret, '[REDACTED]').replace(monitor_secret, '[REDACTED]').replace(password, '[REDACTED]')
    finally:
        cleanup_errors = cleanup_resources(processes, logs, containers, run, run_id,
                                           (secret, monitor_secret, password), report)
        try:
            report['cleanup']['ports_closed'] = {str(p): port_closed(p) for p in (backend_port, vite_port, *mapped_ports)}
            if not all(report['cleanup']['ports_closed'].values()):
                cleanup_errors.append('port_still_open')
        except Exception as exc:
            report['cleanup']['ports_closed'] = 'unknown:' + type(exc).__name__
            cleanup_errors.append('port_check_error')
        try:
            report['source_after'] = clean_source(root, args.expected_head)
        except Exception as exc:
            cleanup_errors.append('source_after_error')
            report['source_after'] = {'error': type(exc).__name__}
            try:
                report['source_after']['git_status'] = git(root, 'status', '--porcelain', '--untracked-files=all')
                (run / 'source-after.diff').write_bytes(command(['git', 'diff', '--binary'], cwd=root).stdout)
                report['source_after']['tracked_diff_path'] = str(run / 'source-after.diff')
            except Exception as detail_exc:
                report['source_after']['detail_error'] = type(detail_exc).__name__
        expected_source_after = ({k: report['source'][k] for k in ('head', 'tree', 'clean')}
                                 if 'source' in report else None)
        report['cleanup']['errors'] = cleanup_errors
        report['exit_code'] = gate_exit_code(report, cleanup_errors, expected_source_after)
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        try:
            (run / 'result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        except Exception as exc:
            report['exit_code'] = 1
            print('T-34 result write failed: ' + type(exc).__name__, file=sys.stderr)
        print(json.dumps({'result': str(run / 'result.json'), 'exit_code': report['exit_code'],
                          'counts': report['counts'], 'cleanup': report['cleanup']}, ensure_ascii=False))
    return report['exit_code']

if __name__ == '__main__':
    sys.exit(main())
