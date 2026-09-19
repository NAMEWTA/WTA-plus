#!/usr/bin/env python3
"""Build immutable releases and atomically select one. Never rewrite live Docker contexts.

The manifest detects corruption/mixed inputs, not a hostile administrator who can replace
both code and manifests. All deployable inputs come from one clean Git archive; ignored
workspace artifacts and runtime credentials are deliberately absent from that archive.
"""
import argparse
import contextlib
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import tarfile
import tempfile
import uuid
import zipfile
from urllib.parse import urlsplit

RELEASE_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = RELEASE_ROOT.parent
BACKENDS = {
    'wta-admin': 'wta-admin/target/wta-admin.jar',
    'wta-monitor-admin': 'wta-extend/wta-monitor-admin/target/wta-monitor-admin.jar',
    'wta-snailjob-server': 'wta-extend/wta-snailjob-server/target/wta-snailjob-server.jar',
    'wta-snailai-server': 'wta-extend/wta-snailai-server/target/wta-snailai-server.jar',
}
SQL_FILES = ('10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql',
             '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql')
CATEGORIES = ('infrastructure', 'observability', 'backend', 'frontend')
RESERVED = {'admin', 'monitor', 'snail-job', 'snail-ai', 'dev-api', 'prod-api', 'actuator'}
MANIFEST = 'release-manifest.json'
VERSION_PATTERN = r'(dev|prod)-[0-9a-f]{12}-[0-9a-f]{64}'


def require(condition, message):
    if not condition:
        raise ValueError(message)


def info(message):
    print('[INFO] ' + message, file=sys.stderr, flush=True)


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def run(args, *, cwd=None, env=None, capture=False, stdout=None):
    """An interrupted owner waits for its child group before removing its private scratch."""
    with subprocess.Popen([str(a) for a in args], cwd=cwd, env=env,
                          stdout=subprocess.PIPE if capture else stdout,
                          start_new_session=True) as child:
        try:
            output, _ = child.communicate()
            require(child.returncode == 0, f'command failed ({child.returncode}): {Path(args[0]).name}')
            return output.decode().strip() if capture else None
        except BaseException:
            if child.poll() is None:
                os.killpg(child.pid, signal.SIGTERM)
                try:
                    child.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(child.pid, signal.SIGKILL)
                    child.wait()
            raise


def env_values(path):
    values = {}
    if path.is_file():
        for line in path.read_text().splitlines():
            match = re.fullmatch(r'\s*(?:export\s+)?([A-Z][A-Z0-9_]*)\s*=\s*(.*?)\s*', line)
            if match:
                values[match[1]] = match[2].strip('\'"')
    return {**values, **os.environ}


def prefixes(apps, env_file):
    values, result = env_values(env_file), {}
    for app in apps:
        key = app.upper().replace('-', '_') + '_PREFIX'
        value = values.get(key, '')
        require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9-]*', value)
                and not value.startswith('replace-') and value not in RESERVED,
                f'{key} missing, placeholder or reserved/invalid prefix')
        require(value not in result.values(), 'duplicate app prefix')
        result[app] = value
    return result


def app_registry(release, frontend=None):
    """The checked-in inventory decides shipping; discovery only detects omissions/drift."""
    document = json.loads((release / 'apps.json').read_text())
    require(document['schemaVersion'] == 1 and isinstance(document['apps'], list), 'invalid app registry')
    rows = document['apps']
    require(rows and len({row['id'] for row in rows}) == len(rows), 'empty or duplicate app registration')
    compose = (release / 'docker/docker-compose-frontend.yml').read_text()
    example = (release / '.env.example').read_text()
    active, ports, services = [], set(), set()
    for row in rows:
        app = row['id']
        require(re.fullmatch(r'[a-z][a-z0-9-]*', app), 'invalid registered app name')
        require(isinstance(row['shipped'], bool), 'shipped must be explicit boolean')
        if frontend is not None:
            package = json.loads((frontend / 'apps' / app / 'package.json').read_text())
            require(package['name'] == row['package'], f'app package mismatch: {app}')
            require(all(package.get('scripts', {}).get('build:' + mode) for mode in ('dev', 'prod')), f'app lacks build scripts: {app}')
        if not row['shipped']:
            continue
        active.append(row)
        require(row['apiKind'] in ('business', 'sso'), f'unknown API kind: {app}')
        require(row['apiPath'] == ('/sso' if row['apiKind'] == 'sso' else '/{prefix}/{environment}-api'), f'unsupported API route: {app}')
        require(row.get('authorizePath') == '/authorize' if row['apiKind'] == 'sso' else row.get('callbackPath') == '/sso/callback', f'unsupported auth entry: {app}')
        require(row['healthPath'] == '/healthz', f'unsupported health path: {app}')
        require(row['ingress'] in ('lb', 'dedicated'), f'unknown ingress kind: {app}')
        require(row['apiKind'] != 'sso' or row['ingress'] == 'dedicated', 'SSO must have a dedicated ingress')
        for key in ('prefixEnv', 'originEnv', 'portEnv'):
            variable = row[key]
            require(re.fullmatch(r'[A-Z][A-Z0-9_]*', variable), f'invalid env variable for {app}')
            require(re.search(r'^' + re.escape(variable) + r'=', example, re.M), f'missing env declaration: {variable}')
        require(row['prefixEnv'] == app.upper().replace('-', '_') + '_PREFIX', f'prefix key mismatch: {app}')
        endpoints = [row] + ([row['tls']] if row.get('tls') else [])
        for endpoint in endpoints:
            name, template = endpoint['composeService'], endpoint['nginxTemplate']
            require(re.fullmatch(r'namewta-nginx-[a-z0-9-]+', name), 'invalid Compose service name')
            require(len(re.findall(r'^  ' + re.escape(name) + r':$', compose, re.M)) == 1, f'app needs exactly one Compose service: {name}')
            require(re.fullmatch(r'docker/frontend/nginx/apps/nginx-[a-z0-9-]+\.conf\.template', template), 'invalid nginx template path')
            require((release / template).is_file(), f'missing nginx template: {app}')
            require('location = /healthz' in (release / template).read_text(), f'missing App health route: {app}')
            require(name not in services, 'duplicate App Compose service registration')
            services.add(name)
            require(re.search(r'^' + re.escape(endpoint['portEnv']) + r'=', example, re.M), f'missing port env: {endpoint["portEnv"]}')
            port = endpoint['defaultPort']
            require(type(port) is int and 1024 <= port <= 65535 and port not in ports, 'invalid/duplicate app port')
            ports.add(port)
            block = re.search(r'^  ' + re.escape(name) + r':\n(.*?)(?=^\S|^  \S|\Z)', compose, re.M | re.S)[1]
            # This repository owns the YAML layout. Reject drift before invoking compilers;
            # the independent Compose parser gate still validates the whole document.
            bindings = [
                './' + template.removeprefix('docker/') + ':/etc/nginx/templates/default.conf.template:ro',
                './frontend/nginx/html/' + app + ':/usr/share/nginx/html:ro',
                'APP_PREFIX: "${' + row['prefixEnv'] + ':?' + row['prefixEnv'] + ' is required}"',
                '${' + endpoint['portEnv'] + ':-' + str(port) + '}:' + ('443' if endpoint is row.get('tls') else '80'),
                'BACKEND_SERVER1: "${BACKEND_SERVER1:-namewta-server1:8080}"',
                'BACKEND_SERVER2: "${BACKEND_SERVER2:-namewta-server2:8080}"',
            ]
            if row['apiKind'] == 'sso':
                bindings.append('APP_ORIGIN: "${' + row['originEnv'] + ':?' + row['originEnv'] + ' is required}"')
            require(all(block.count(binding) == 1 for binding in bindings), f'Compose App binding drift: {name}')
        for name in ('nginx-lb-http.conf.template', 'nginx-lb-tls.conf.template'):
            lb = (release / 'docker/frontend/nginx/lb' / name).read_text()
            marker = 'APP_' + row['prefixEnv']
            if row['ingress'] == 'lb':
                require(marker in lb, f'App missing from LB: {app}')
            else:
                require(marker not in lb and row['composeService'] not in lb, f'dedicated App exposed on shared LB: {app}')
    require(active, 'no shipped apps')
    require(sum(app['apiKind'] == 'sso' for app in active) == 1, 'exactly one shipped SSO App is required')
    configured = set(re.findall(r'^  (namewta-nginx-[a-z0-9-]+):$', compose, re.M)) - {'namewta-nginx-lb', 'namewta-nginx-lb-tls'}
    require(configured == services, 'unregistered or unshipped App Compose service')
    if frontend is not None:
        discovered = {p.parent.name for p in (frontend / 'apps').glob('*/package.json')}
        require(discovered == {row['id'] for row in rows}, 'unregistered or missing frontend App')
    return active


def validate_origin_matrix(apps, values, environment):
    origins = {}
    for app in apps:
        value = values.get(app['id'], '')
        require(isinstance(value, str), f'invalid origin: {app["originEnv"]}')
        try:
            parsed = urlsplit(value)
            port = parsed.port
        except ValueError:
            raise ValueError(f'invalid origin: {app["originEnv"]}') from None
        require(parsed.scheme in ('http', 'https') and parsed.hostname and not parsed.username and not parsed.password
                and parsed.path in ('', '/') and '?' not in value and '#' not in value
                and not re.search(r'\s', value), f'invalid origin: {app["originEnv"]}')
        require(environment != 'prod' or parsed.scheme == 'https', 'production App origins require HTTPS')
        require(not parsed.hostname.endswith('.invalid') and 'replace-' not in parsed.hostname, 'placeholder App origin')
        require(':' in parsed.hostname or re.fullmatch(r'[A-Za-z0-9.-]+', parsed.hostname), 'invalid App hostname')
        host = '[' + parsed.hostname.lower() + ']' if ':' in parsed.hostname else parsed.hostname.lower()
        require(port is None or 1 <= port <= 65535, 'invalid App origin port')
        origins[app['id']] = parsed.scheme + '://' + host + (f':{port}' if port and port != (443 if parsed.scheme == 'https' else 80) else '')
    for app in apps:
        if app['apiKind'] == 'sso':
            require(all(origin != origins[app['id']] for name, origin in origins.items() if name != app['id']), 'SSO must use an independent Web Origin')
            if environment == 'prod':
                # Cookies have a hostname/path boundary; different ports do not isolate them.
                require(all(urlsplit(origin).hostname != urlsplit(origins[app['id']]).hostname
                            for name, origin in origins.items() if name != app['id']),
                        'production SSO cookie requires a separate hostname')
    return origins


def app_origins(apps, env_file, environment):
    values = env_values(env_file)
    return validate_origin_matrix(apps, {app['id']: values.get(app['originEnv'], '') for app in apps}, environment)


def application_matrix(registered, prefixes_by_app, origins, environment):
    matrix = {}
    for app in registered:
        name, prefix = app['id'], prefixes_by_app[app['id']]
        base = origins[name] + '/' + prefix
        matrix[name] = {'baseUrl': base + '/', 'apiPath': app['apiPath'].format(prefix=prefix, environment=environment),
                        'httpPortEnv': app['portEnv'], 'httpPortDefault': app['defaultPort']}
        if app.get('tls'):
            matrix[name].update(httpsPortEnv=app['tls']['portEnv'], httpsPortDefault=app['tls']['defaultPort'])
        if app['apiKind'] == 'sso':
            require(prefix != 'sso', 'SSO static prefix collides with /sso API')
            matrix[name]['authorizeUrl'] = base + app['authorizePath']
        else:
            matrix[name]['callbackUrl'] = base + app['callbackPath']
    return matrix


def regular_files(root):
    files = []
    require(root.is_dir() and not root.is_symlink(), 'release directory must be a real directory')
    for path in sorted(root.rglob('*')):
        require(not path.is_symlink(), f'symlink not allowed inside release: {path.relative_to(root)}')
        require(path.is_dir() or path.is_file(), 'special file not allowed inside release')
        if path.is_file() and path != root / MANIFEST:
            files.append(path)
    return files


def validate_sql(root):
    directory = root / 'docker/infrastructure/mysql/init'
    require(directory.is_dir(), 'missing SQL baseline directory')
    require(sorted(p.name for p in directory.glob('*.sql')) == list(SQL_FILES), 'SQL baseline must contain exactly six ordered files')
    for name in SQL_FILES:
        text = (directory / name).read_text(encoding='utf-8').strip()
        # This is an envelope check. SQL execution/schema correctness remains a MySQL CI gate.
        require(text.startswith('SET NAMES utf8mb4;') and text.endswith(';') and '\0' not in text,
                f'empty or truncated SQL baseline: {name}')


def validate_jar(path):
    require(path.is_file(), f'missing JAR: {path.name}')
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        require(len(names) == len(set(names)) and jar.testzip() is None, f'corrupt JAR: {path.name}')
        require('META-INF/MANIFEST.MF' in names and any(n.startswith('BOOT-INF/classes/') for n in names),
                f'not an executable Spring Boot JAR: {path.name}')
        manifest = jar.read('META-INF/MANIFEST.MF')
        require(b'Main-Class:' in manifest and b'Start-Class:' in manifest, f'missing JAR entry point: {path.name}')


def validate_payload(root, metadata):
    require(metadata['target'] == 'all' and metadata['source']['clean'] is True, 'only clean complete builds are deployable')
    require(re.fullmatch(r'[0-9a-f]{40}', metadata['source']['revision']), 'invalid source revision')
    require(re.fullmatch(r'[0-9a-f]{40}', metadata['source']['tree']), 'invalid source tree')
    require(re.fullmatch(r'[0-9a-f]{64}', metadata['source']['archiveSha256']), 'invalid source archive digest')
    require(metadata['environment'] in ('dev', 'prod') and metadata['backendBundle'] in ('full', 'core'), 'invalid release mode')
    apps = metadata['apps']
    require(isinstance(apps, dict) and apps, 'missing app inventory')
    registered = app_registry(root)
    require(set(apps) == {app['id'] for app in registered}, 'manifest differs from shipped app registry')
    require(set(metadata['appOrigins']) == set(apps), 'missing App origin matrix')
    require(validate_origin_matrix(registered, metadata['appOrigins'], metadata['environment']) == metadata['appOrigins'], 'noncanonical App origin matrix')
    require(application_matrix(registered, apps, metadata['appOrigins'], metadata['environment']) == metadata['applicationMatrix'], 'release application matrix mismatch')
    for app in registered:
        require(app['apiKind'] != 'sso' or apps[app['id']] != 'sso', 'SSO static prefix collides with /sso API')
    for name in BACKENDS:
        context = root / 'docker/backend/images' / name
        validate_jar(context / 'app.jar')
        require((context / 'Dockerfile').is_file(), f'missing Dockerfile: {name}')
    validate_sql(root)
    for category in CATEGORIES:
        require((root / f'docker/docker-compose-{category}.yml').is_file(), f'missing Compose: {category}')
    for name in ('nginx-lb-http.conf.template', 'nginx-lb-tls.conf.template'):
        require((root / 'docker/frontend/nginx/lb' / name).is_file(), f'missing LB template: {name}')
    for app, prefix in apps.items():
        require(re.fullmatch(r'[a-z][a-z0-9-]*', app), 'invalid app name')
        require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9-]*', prefix) and prefix not in RESERVED, 'invalid app prefix')
        base = root / 'docker/frontend/nginx'
        require((base / f'apps/nginx-{app}.conf.template').is_file(), f'missing nginx template: {app}')
        require((base / f'html/{app}/index.html').is_file(), f'missing app HTML: {app}')
        mode = json.loads((base / f'html/{app}/build-mode.json').read_text())
        require(mode == {'app': app, 'mode': 'production' if metadata['environment'] == 'prod' else 'development'},
                f'wrong build mode: {app}')
    require(len(set(apps.values())) == len(apps), 'duplicate app prefix')


def seal(root, metadata):
    validate_payload(root, metadata)
    source = metadata['source']['revision']
    metadata = {'schemaVersion': 2, **metadata, 'files': [
        {'path': str(p.relative_to(root)), 'sha256': digest(p), 'size': p.stat().st_size,
         'executable': bool(p.stat().st_mode & 0o111), 'sourceRevision': source}
        for p in regular_files(root)]}
    (root / MANIFEST).write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n')
    return metadata


def version_id(root, manifest):
    return f"{manifest['environment']}-{manifest['source']['revision'][:12]}-{digest(root / MANIFEST)}"


def verify(root, environment=None):
    require(not root.is_symlink(), 'version must not be a symlink')
    manifest_path = root / MANIFEST
    require(manifest_path.is_file() and not manifest_path.is_symlink(), 'missing release manifest')
    files = regular_files(root)  # Reject links before reading any target metadata.
    require(all(not (p.stat().st_mode & 0o222) for p in [root, *root.rglob('*')]), 'version is not sealed read-only')
    manifest = json.loads(manifest_path.read_text())
    require(manifest['schemaVersion'] == 2, 'unsupported manifest')
    require(environment is None or manifest['environment'] == environment, 'release environment mismatch')
    require(root.name == version_id(root, manifest), 'version ID does not match manifest digest')
    rows = manifest['files']
    require(isinstance(rows, list) and rows, 'empty manifest inventory')
    require(len({r['path'] for r in rows}) == len(rows), 'duplicate manifest path')
    expected = {r['path']: r for r in rows}
    actual = {str(p.relative_to(root)): p for p in files}
    require(expected.keys() == actual.keys(), 'release file inventory changed')
    for name, path in actual.items():
        record = expected[name]
        require(record['sourceRevision'] == manifest['source']['revision'], f'mixed source: {name}')
        require(record['size'] == path.stat().st_size and record['sha256'] == digest(path), f'artifact digest mismatch: {name}')
        require(record['executable'] == bool(path.stat().st_mode & 0o111), f'artifact executable mode changed: {name}')
    validate_payload(root, manifest)
    return manifest


def release_path(release_id):
    require(re.fullmatch(VERSION_PATTERN, release_id or ''), 'explicit immutable release ID required')
    builds = RELEASE_ROOT / 'builds'
    versions = builds / 'versions'
    require(not builds.is_symlink() and not versions.is_symlink(), 'release storage cannot be a symlink')
    return versions / release_id


def resolve(environment, env_file=None):
    current = RELEASE_ROOT / 'builds' / ('current_' + environment)
    require(current.is_symlink(), 'current must select an immutable version; stage --release ID first')
    target = current.readlink()
    require(len(target.parts) == 2 and target.parts[0] == 'versions', 'current points outside release versions')
    version = release_path(target.name)
    manifest = verify(version, environment)
    if env_file is not None:
        require(prefixes(manifest['apps'], env_file) == manifest['apps'], 'runtime prefixes differ from built app prefixes')
        require(app_origins(app_registry(version), env_file, environment) == manifest['appOrigins'], 'runtime origins differ from release origin matrix')
    return version


@contextlib.contextmanager
def release_lock():
    builds = RELEASE_ROOT / 'builds'
    require(not builds.is_symlink(), 'builds cannot be a symlink')
    builds.mkdir(exist_ok=True)
    # Keep the inode permanently: unlinking a flock file allows two owners on different inodes.
    fd = os.open(builds / '.release.lock', os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
    with os.fdopen(fd, 'w') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as exc:
            raise ValueError('another release operation owns the lock') from exc
        yield


def make_read_only(root):
    for path in regular_files(root) + [root / MANIFEST]:
        path.chmod(0o555 if path.stat().st_mode & 0o111 else 0o444)
    for path in sorted(root.rglob('*'), reverse=True):
        if path.is_dir():
            path.chmod(0o555)
    root.chmod(0o555)


def fsync_tree(root):
    for path in regular_files(root) + [root / MANIFEST]:
        with path.open('rb') as stream:
            os.fsync(stream.fileno())
    for path in [p for p in root.rglob('*') if p.is_dir()] + [root]:
        fsync_dir(path)


def fsync_dir(path):
    fd = os.open(path, os.O_RDONLY | os.O_DIRECTORY)
    try:
        os.fsync(fd)
    finally:
        os.close(fd)


def stage(environment, release_id):
    version = release_path(release_id)
    verify(version, environment)
    builds = RELEASE_ROOT / 'builds'
    current = builds / ('current_' + environment)
    require(not current.exists() or current.is_symlink(), 'legacy current directory must be handled explicitly; refusing to replace it')
    # A unique link in the same directory gives exactly one rename visibility boundary.
    pending = builds / ('.pointer-' + uuid.uuid4().hex)
    try:
        pending.symlink_to('versions/' + release_id)
        fsync_dir(builds)
        os.replace(pending, current)
        fsync_dir(builds)
    finally:
        if pending.is_symlink():
            pending.unlink()
    info(f'selected {release_id}; running containers require explicit recreation')


def clean_source():
    require(run(['git', '-C', REPO_ROOT, 'rev-parse', '--show-toplevel'], capture=True) == str(REPO_ROOT), 'build requires repository root')
    require(not run(['git', '-C', REPO_ROOT, 'status', '--porcelain=v1', '--untracked-files=all'], capture=True),
            'deployable build requires clean tracked and untracked source')
    return run(['git', '-C', REPO_ROOT, 'rev-parse', 'HEAD'], capture=True)


def archive_source(revision, scratch):
    archive = scratch / 'source.tar'
    with archive.open('wb') as stream:
        run(['git', '-C', REPO_ROOT, 'archive', '--format=tar', revision], stdout=stream)
    source = scratch / 'source'
    source.mkdir()
    with tarfile.open(archive) as tar:
        # Git stores regular files/symlinks only. Reject all links and traversal before extraction.
        for entry in tar.getmembers():
            require((entry.isfile() or entry.isdir()) and not Path(entry.name).is_absolute()
                    and '..' not in Path(entry.name).parts, 'unsupported link or path in source archive')
        tar.extractall(source, filter='data')
    return source, digest(archive)


def compile_artifacts(source, output, args, apps, registered):
    if args.target in ('all', 'backend'):
        backend = source / 'backend'
        profiles = args.env + (',bundle-core' if args.bundle == 'core' else '')
        flag = '-Dmaven.test.skip=true' if args.bundle == 'core' else '-DskipTests'
        run(['./mvnw', 'clean', 'package', flag, '-P' + profiles], cwd=backend)
        run(['bash', source / 'scripts/ci/verify-admin-bundle.sh', args.bundle],
            env={**os.environ, 'ADMIN_ARTIFACT': str(backend / BACKENDS['wta-admin'])})
        for name, relative in BACKENDS.items():
            target = output / 'docker/backend/images' / name
            target.mkdir(parents=True, exist_ok=True)
            shutil.copy2(backend / relative, target / 'app.jar')
    if args.target in ('all', 'frontend'):
        frontend = source / 'frontend'
        run(['pnpm', 'install', '--frozen-lockfile', '--network-concurrency=1', '--child-concurrency=1'], cwd=frontend)
        run(['pnpm', 'architecture:check'], cwd=frontend)
        run(['pnpm', 'build:dependencies'], cwd=frontend)
        for app, prefix in apps.items():
            definition = next(row for row in registered if row['id'] == app)
            package = definition['package']
            dist = frontend / 'apps' / app / 'dist'
            # No inherited ignored output may make a successful no-op build look complete.
            if dist.exists():
                shutil.rmtree(dist)
            build_env = {**os.environ, 'VITE_APP_CONTEXT_PATH': '/' + prefix + '/',
                         'VITE_APP_BASE_API': definition['apiPath'].format(prefix=prefix, environment=args.env)}
            if definition['apiKind'] == 'sso':
                # SSO uses its own same-origin /sso namespace, not the business env-api route.
                build_env['VITE_SSO_API'] = ''
            run(['pnpm', '--filter', package, 'build:' + args.env], cwd=frontend, env=build_env)
            shutil.copytree(dist, output / 'docker/frontend/nginx/html' / app, dirs_exist_ok=True)


def build(args):
    complete = args.target == 'all'
    revision = clean_source() if complete else None
    builds = RELEASE_ROOT / 'builds'
    # TemporaryDirectory only owns this randomly allocated path. No sweep of old stages/history.
    with tempfile.TemporaryDirectory(prefix='.build-', dir=builds) as temporary:
        scratch = Path(temporary)
        source, archive_digest = archive_source(revision, scratch) if complete else (REPO_ROOT, None)
        output = scratch / 'payload'
        output.mkdir()
        registered = app_registry(source / 'release-artifacts', source / 'frontend') if args.target != 'backend' else []
        app_names = [app['id'] for app in registered]
        apps = prefixes(app_names, args.env_file) if args.target != 'backend' else {}
        origins = app_origins(registered, args.env_file, args.env) if complete else {}
        matrix = application_matrix(registered, apps, origins, args.env) if complete else {}
        if complete:
            archived_release = source / 'release-artifacts'
            for name in ('docker', 'scripts', 'skills'):
                shutil.copytree(archived_release / name, output / name)
            for name in ('README.md', '.env.example', 'apps.json'):
                shutil.copy2(archived_release / name, output / name)
            validate_sql(output)
            for app in apps:
                require((output / f'docker/frontend/nginx/apps/nginx-{app}.conf.template').is_file(), f'missing nginx template: {app}')
        compile_artifacts(source, output, args, apps, registered)
        if not complete:
            destination = builds / 'development' / (args.target + '-' + uuid.uuid4().hex)
            destination.parent.mkdir(exist_ok=True)
            require(not destination.parent.is_symlink(), 'development output cannot be a symlink')
            output.rename(destination)
            info('development output only; cannot stage: ' + str(destination))
            print(destination)
            return
        require(clean_source() == revision, 'source changed during build; candidate discarded')
        source_metadata = {'revision': revision, 'tree': run(['git', '-C', REPO_ROOT, 'rev-parse', revision + '^{tree}'], capture=True),
                           'archiveSha256': archive_digest, 'clean': True}
        metadata = {'environment': args.env, 'backendBundle': args.bundle, 'target': 'all', 'apps': apps,
                    'appOrigins': origins, 'applicationMatrix': matrix, 'source': source_metadata}
        for category in CATEGORIES:
            run(['docker', 'compose', '--env-file', args.env_file, '-f', output / f'docker/docker-compose-{category}.yml', 'config', '--quiet'])
        manifest = seal(output, metadata)
        release_id = version_id(output, manifest)
        destination = release_path(release_id)
        destination.parent.mkdir(exist_ok=True)
        # Complete durable payload before exposing it under versions; never overwrite history.
        fsync_tree(output)
        if destination.exists():
            verify(destination, args.env)
        else:
            output.rename(destination)
            make_read_only(destination)
            fsync_tree(destination)
            fsync_dir(destination.parent)
        info('built immutable release; stage explicitly: ' + release_id)
        print(release_id)


def backup():
    for environment in ('dev', 'prod'):
        current = RELEASE_ROOT / 'builds' / ('current_' + environment)
        if not current.is_symlink():
            continue
        version = resolve(environment)
        pins = RELEASE_ROOT / 'builds/pins'
        require(not pins.is_symlink(), 'pins cannot be a symlink')
        pins.mkdir(exist_ok=True)
        (pins / (environment + '-' + uuid.uuid4().hex)).symlink_to('../versions/' + version.name)
        fsync_dir(pins)
        info('pinned ' + version.name + '; current unchanged')


def bundle(environment):
    version = resolve(environment)
    bundles = RELEASE_ROOT / 'bundles'
    require(not bundles.is_symlink(), 'bundles cannot be a symlink')
    bundles.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.bundle-', dir=bundles) as temporary:
        scratch = Path(temporary)
        package = scratch / 'namewta'
        stored = package / 'builds/versions' / version.name
        stored.parent.mkdir(parents=True)
        # Archive the verified version directly: no credentials, log directories or live contexts.
        package.mkdir(exist_ok=True)
        for name in ('scripts', 'skills'):
            shutil.copytree(version / name, package / name)
        for name in ('README.md', '.env.example'):
            shutil.copy2(version / name, package / name)
        (package / 'builds' / ('current_' + environment)).symlink_to('versions/' + version.name)
        target = scratch / 'bundle.tar.gz'
        with tarfile.open(target, 'w:gz') as tar:
            tar.add(package, arcname='namewta')
            tar.add(version, arcname='namewta/builds/versions/' + version.name)
        with target.open('rb') as stream:
            os.fsync(stream.fileno())
        # Never clobber an earlier bundle, even of the same release.
        destination = bundles / (version.name + '-' + uuid.uuid4().hex + '.tar.gz')
        target.rename(destination)
        fsync_dir(bundles)
        print(destination)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    actions = parser.add_subparsers(dest='action', required=True)
    p = actions.add_parser('build')
    p.add_argument('--target', choices=('all', 'frontend', 'backend'), default='all')
    p.add_argument('--env', choices=('dev', 'prod'), default='prod')
    p.add_argument('--bundle', choices=('full', 'core'), default='full')
    p.add_argument('--env-file', type=Path, default=RELEASE_ROOT / '.env')
    p = actions.add_parser('stage')
    p.add_argument('--env', choices=('dev', 'prod'), required=True)
    p.add_argument('--release', required=True)
    p = actions.add_parser('resolve')
    p.add_argument('--env', choices=('dev', 'prod'), required=True)
    p.add_argument('--env-file', type=Path)
    p = actions.add_parser('bundle')
    p.add_argument('--env', choices=('dev', 'prod'), required=True)
    actions.add_parser('backup')
    actions.add_parser('stage-mysql')
    actions.add_parser('check-apps')
    args = parser.parse_args()
    if hasattr(args, 'env_file') and args.env_file is not None:
        args.env_file = args.env_file.resolve()
    if args.action == 'stage-mysql':
        validate_sql(RELEASE_ROOT)
        info('MySQL baseline verified read-only; six canonical SQL files unchanged')
        return
    if args.action == 'check-apps':
        registered = app_registry(RELEASE_ROOT, REPO_ROOT / 'frontend')
        info('explicit shipped App registration verified: ' + ', '.join(app['id'] for app in registered))
        return
    if args.action == 'resolve':
        print(resolve(args.env, args.env_file))
        return
    with release_lock():
        if args.action == 'build':
            build(args)
        elif args.action == 'stage':
            stage(args.env, args.release)
        elif args.action == 'backup':
            backup()
        elif args.action == 'bundle':
            bundle(args.env)


def interrupted(signum, _frame):
    raise InterruptedError(f'release operation interrupted by signal {signum}')


if __name__ == '__main__':
    signal.signal(signal.SIGTERM, interrupted)
    signal.signal(signal.SIGINT, interrupted)
    try:
        main()
    except (ValueError, OSError, KeyError, TypeError, zipfile.BadZipFile, tarfile.TarError) as error:
        print('[ERROR] ' + str(error), file=sys.stderr)
        sys.exit(1)
