#!/usr/bin/env python3
"""Serial, owned three-Origin fixture; never builds/stages a deployable release.

Uses production Nginx templates and freshly built Apps with the existing Java
SSO/Redis/MySQL fixture. System identity/menu and business token minting remain
explicit test doubles. Only recorded container/network IDs are removed.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import socket
import subprocess
import tempfile
import time
import uuid

ROOT = Path(__file__).resolve().parents[3]
IMAGES = {
    'nginx': 'nginx:1.31.1@sha256:608a100c71651bf5b773c89083b4a1ad7ef4b2bd05d7a7e552271e03123692ad',
    'mysql': 'mysql:8.4.9@sha256:c36050afdca850f23cef85703f84c7531a5ae155a11b5ee1c60acb09937c4084',
    'redis': 'redis:7.4-alpine@sha256:ff02b58f971e7d7d156a1267e283fcbbeee91773b6aa36c49dac28ecfe28eadf',
}


def output(command):
    return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT).strip()


def inventory():
    return {kind: sorted(output(['docker', *command]).splitlines()) for kind, command in {
        'containers': ['ps', '-aq', '--no-trunc'], 'networks': ['network', 'ls', '-q', '--no-trunc'],
        'volumes': ['volume', 'ls', '-q'],
    }.items()}


def available_port(host='127.0.0.1'):
    with socket.socket() as listener:
        listener.bind((host, 0))
        return listener.getsockname()[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--evidence', type=Path, required=True, help='New result JSON; logs share its filename stem')
    args = parser.parse_args()
    evidence = args.evidence.resolve()
    if evidence.exists():
        raise SystemExit('Refusing to overwrite prior evidence')
    evidence.parent.mkdir(parents=True, exist_ok=True)
    owner = 'namewta-sso-release-' + uuid.uuid4().hex[:12]
    record = {'owner': owner, 'images': IMAGES, 'commands': [], 'containers': [], 'network': None,
              'cleanup': [], 'before': inventory(), 'boundary': 'production Nginx/App/SSO/Redis/MySQL; mocked System identity/menu/token minting'}

    def save():
        evidence.write_text(json.dumps(record, indent=2) + '\n')

    def run(command, cwd=ROOT, env=None, timeout=900):
        log = evidence.with_name(evidence.stem + '-' + str(len(record['commands']) + 1) + '.log')
        started = time.time()
        with log.open('w') as stream:
            process = subprocess.Popen(command, cwd=cwd, env=env, stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
            try:
                code = process.wait(timeout=timeout)
            except BaseException:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
                raise
        # Never expose transient OAuth query values through fixture error reports.
        log.write_text(re.sub(r'''([?&](?:code|state|code_challenge|code_verifier)=)[^&\s"'<>]+''', r'\1[REDACTED]', log.read_text()))
        record['commands'].append({'command': command, 'cwd': str(cwd), 'exit_code': code,
                                   'seconds': round(time.time() - started, 2), 'log': log.name})
        save()
        print(f'{command[0]} {command[1]}: exit={code}, log={log.name}', flush=True)
        if code:
            raise RuntimeError('Fixture command failed; inspect recorded log')

    def container(name, arguments):
        identifier = output(['docker', 'create', '--name', owner + '-' + name, '--label', 'namewta.test.owner=' + owner, *arguments])
        record['containers'].append({'name': name, 'id': identifier})
        save()
        output(['docker', 'start', identifier])
        return identifier

    def ready(command, expected, attempts=90):
        for _ in range(attempts):
            result = subprocess.run(command, capture_output=True, text=True)
            if result.returncode == 0 and expected in result.stdout:
                return
            time.sleep(1)
        raise RuntimeError('Owned service readiness timeout')

    exit_code = 1
    try:
        with tempfile.TemporaryDirectory(prefix=owner + '-') as temporary:
            work = Path(temporary)
            shim = work / 'pnpm'
            shim.write_text('#!/bin/sh\nexec corepack pnpm "$@"\n')
            shim.chmod(0o700)
            environment = {**os.environ, 'PATH': str(work) + os.pathsep + os.environ['PATH'],
                           'npm_config_workspace_concurrency': '1', 'RAYON_NUM_THREADS': '1'}
            run(['corepack', 'pnpm', 'build:dependencies'], ROOT / 'frontend', environment)
            for app, prefix in [('admin-web', 'admin-app'), ('home-web', 'home-app'), ('sso-web', 'sso-app')]:
                build_environment = {**environment, 'VITE_APP_CONTEXT_PATH': '/' + prefix + '/',
                                     'VITE_APP_BASE_API': '/' + prefix + '/prod-api', 'VITE_SSO_API': ''}
                run(['corepack', 'pnpm', '--filter', '@namewta/' + app, 'build:prod'], ROOT / 'frontend', build_environment)
                dist = ROOT / 'frontend/apps' / app / 'dist'
                assert json.loads((dist / 'build-mode.json').read_text()) == {'app': app, 'mode': 'production'}
                record.setdefault('built_apps', {})[app] = hashlib.sha256((dist / 'index.html').read_bytes()).hexdigest()

            network = output(['docker', 'network', 'create', '--label', 'namewta.test.owner=' + owner, owner])
            record['network'] = network
            save()
            gateway = json.loads(output(['docker', 'network', 'inspect', network]))[0]['IPAM']['Config'][0]['Gateway']
            backend_port = available_port(gateway)
            ports = {app: available_port() for app in ['admin', 'home', 'sso']}
            origins = {app: f'https://{"localhost" if app == "sso" else "127.0.0.1"}:{port}' for app, port in ports.items()}
            record['origins'] = origins
            cert = work / 'cert'
            cert.mkdir()
            run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-days', '1', '-subj', '/CN=localhost',
                 '-addext', 'subjectAltName=DNS:localhost,IP:127.0.0.1', '-keyout', str(cert / 'privkey.pem'), '-out', str(cert / 'fullchain.pem')])
            templates = ROOT / 'release-artifacts/docker/frontend/nginx'
            backend = f'{gateway}:{backend_port}'

            def nginx(name, template, html, bindings, port=None, alias=None, tls_dir=None):
                arguments = ['--network', network, '-v', f'{template}:/etc/nginx/templates/default.conf.template:ro',
                             '-v', f'{html}:/usr/share/nginx/html:ro']
                if alias:
                    arguments += ['--network-alias', alias]
                if port:
                    arguments += ['-p', f'127.0.0.1:{port}:443']
                if tls_dir:
                    arguments += ['-v', f'{cert}:/etc/nginx/cert/{tls_dir}:ro']
                for key, value in bindings.items():
                    arguments += ['-e', key + '=' + value]
                for host in ['namewta-monitor-admin', 'namewta-snailjob-server', 'namewta-snailai-server', 'nacos']:
                    arguments += ['--add-host', host + ':' + gateway]
                identifier = container(name, [*arguments, IMAGES['nginx']])
                record.setdefault('templates', {})[str(template.relative_to(ROOT))] = hashlib.sha256(template.read_bytes()).hexdigest()
                ready(['docker', 'exec', identifier, 'nginx', '-t'], '', attempts=15)
                return identifier

            for app in ['admin', 'home']:
                nginx(app, templates / f'apps/nginx-{app}-web.conf.template', ROOT / f'frontend/apps/{app}-web/dist',
                      {'APP_PREFIX': app + '-app', 'BACKEND_SERVER1': backend, 'BACKEND_SERVER2': backend},
                      alias=f'namewta-nginx-{app}-web')
            for app in ['admin', 'home']:
                nginx(app + '-tls', templates / 'lb/nginx-lb-tls.conf.template', templates / 'html',
                      {'APP_ADMIN_WEB_PREFIX': 'admin-app', 'APP_HOME_WEB_PREFIX': 'home-app', 'LB_SERVER_NAME': '127.0.0.1'},
                      port=ports[app], tls_dir='lb')
            nginx('sso-tls', templates / 'apps/nginx-sso-web-tls.conf.template', ROOT / 'frontend/apps/sso-web/dist',
                  {'APP_PREFIX': 'sso-app', 'APP_ORIGIN': origins['sso'], 'BACKEND_SERVER1': backend, 'BACKEND_SERVER2': backend},
                  port=ports['sso'], tls_dir='sso-web')

            database = 'namewta_sso_test_' + uuid.uuid4().hex[:12]
            mysql = container('mysql', ['-p', '127.0.0.1::3306', '-e', 'MYSQL_ROOT_PASSWORD=owned-sso-test-only',
                              '-e', 'MYSQL_DATABASE=' + database, IMAGES['mysql'], '--character-set-server=utf8mb4', '--collation-server=utf8mb4_general_ci'])
            ready(['docker', 'exec', mysql, 'mysqladmin', 'ping', '-h', '127.0.0.1', '-uroot', '-powned-sso-test-only', '--silent'], 'alive')
            mysql_port = output(['docker', 'port', mysql, '3306/tcp']).rsplit(':', 1)[1]
            redis = container('redis', ['-p', '127.0.0.1::6379', IMAGES['redis'], 'redis-server', '--save', '', '--appendonly', 'no'])
            ready(['docker', 'exec', redis, 'redis-cli', 'ping'], 'PONG')
            redis_port = output(['docker', 'port', redis, '6379/tcp']).rsplit(':', 1)[1]
            command = ['./mvnw', '-B', '-ntp', '-pl', 'wta-admin', '-am', 'test', '-Dtest=SsoHttpsSessionIntegrationTest,AuthClientContextSsoUnitTest',
                       '-Dsurefire.failIfNoSpecifiedTests=false', '-Dsso.release.integration=true',
                       '-Dsso.release.backend.host=' + gateway, '-Dsso.release.backend.port=' + str(backend_port),
                       '-Dsso.redis.integration.port=' + redis_port,
                       '-Dsso.mysql.integration.url=jdbc:mysql://127.0.0.1:' + mysql_port + '/' + database + '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
                       '-Dnamewta.repo.root=' + str(ROOT), '-Dnamewta.sql.root=' + str(ROOT / 'release-artifacts/docker/infrastructure/mysql/init'),
                       *['-Dsso.release.' + app + '.origin=' + origin for app, origin in origins.items()]]
            environment['MAVEN_OPTS'] = environment.get('MAVEN_OPTS', '') + ' -Daether.connector.requestTimeout=60000 -Daether.connector.connectTimeout=10000 -Daether.connector.basic.threads=1 -Dmaven.artifact.threads=1'
            run(command, ROOT / 'backend', environment)
            report = ROOT / 'backend/wta-admin/target/surefire-reports/TEST-org.namewta.test.sso.SsoHttpsSessionIntegrationTest.xml'
            import xml.etree.ElementTree as xml
            suite = xml.parse(report).getroot().attrib
            record['tests'] = {key: int(suite[key]) for key in ['tests', 'failures', 'errors', 'skipped']}
            assert record['tests'] == {'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0}
            context_report = ROOT / 'backend/wta-admin/target/surefire-reports/TEST-org.namewta.web.controller.AuthClientContextSsoUnitTest.xml'
            context_suite = xml.parse(context_report).getroot().attrib
            record['client_context_tests'] = {key: int(context_suite[key]) for key in ['tests', 'failures', 'errors', 'skipped']}
            assert record['client_context_tests'] == {'tests': 3, 'failures': 0, 'errors': 0, 'skipped': 0}
            exit_code = 0
    except Exception as failure:
        record['failure'] = str(failure)
        print(str(failure), flush=True)
    finally:
        for row in reversed(record['containers']):
            if row['name'] not in ('mysql', 'redis'):
                logs = subprocess.run(['docker', 'logs', row['id']], capture_output=True, text=True)
                safe_logs = re.sub(r'''\?[^\s"']+''', '?[REDACTED]', logs.stdout + logs.stderr)
                evidence.with_name(evidence.stem + '-' + row['name'] + '.log').write_text(safe_logs)
            removed = subprocess.run(['docker', 'rm', '-fv', row['id']], capture_output=True, text=True)
            record['cleanup'].append({'id': row['id'], 'exit_code': removed.returncode})
            if removed.returncode:
                exit_code = 1
        if record['network']:
            removed = subprocess.run(['docker', 'network', 'rm', record['network']], capture_output=True, text=True)
            record['cleanup'].append({'network': record['network'], 'exit_code': removed.returncode})
            if removed.returncode:
                exit_code = 1
        record['after'] = inventory()
        record['resources_restored'] = record['before'] == record['after']
        if not record['resources_restored']:
            exit_code = 1
        record['exit_code'] = exit_code
        save()
    return exit_code


if __name__ == '__main__':
    raise SystemExit(main())
