import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import crypto from 'node:crypto';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const releaseRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const workspaceRoot = path.resolve(releaseRoot, '..');
const dockerRoot = path.join(releaseRoot, 'docker');

function read(relativePath, root = releaseRoot) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

test('deployment is split into exactly four compose categories', () => {
  const expected = [
    'docker-compose-backend.yml',
    'docker-compose-frontend.yml',
    'docker-compose-infrastructure.yml',
    'docker-compose-observability.yml',
  ];
  const actual = fs.readdirSync(dockerRoot)
    .filter((name) => name.startsWith('docker-compose-') && name.endsWith('.yml'))
    .sort();

  assert.deepEqual(actual, expected);
  for (const filename of actual) {
    const compose = read(`docker/${filename}`);
    assert.match(compose, /external: true/);
    assert.match(compose, /NAMEWTA_NETWORK/);
  }
});

test('runtime resources use the namewta prefix', () => {
  const composeFiles = [
    'docker/docker-compose-backend.yml',
    'docker/docker-compose-frontend.yml',
    'docker/docker-compose-infrastructure.yml',
    'docker/docker-compose-observability.yml',
  ];
  for (const filename of composeFiles) {
    const compose = read(filename);
    const containerNames = [...compose.matchAll(/^\s+container_name:\s+(\S+)$/gm)]
      .map((match) => match[1]);
    assert.ok(containerNames.length > 0, `${filename} must declare container names`);
    assert.ok(containerNames.every((name) => name.startsWith('namewta-')));
    assert.doesNotMatch(compose, /wta-namewta/);
  }

  for (const filename of [
    'docker/docker-compose-backend.yml',
    'docker/docker-compose-frontend.yml',
  ]) {
    const servicesBlock = read(filename).split(/\r?\nservices:\r?\n/)[1].split(/\r?\nnetworks:/)[0];
    const services = [...servicesBlock.matchAll(/^  ([a-z0-9-]+):$/gm)]
      .map((match) => match[1]);
    assert.ok(services.every((name) => name.startsWith('namewta-')));
  }

  const env = read('.env.example');
  assert.doesNotMatch(env, /^WTA_.*_IMAGE=/m);
  assert.match(env, /^NAMEWTA_NETWORK=namewta-network$/m);
  assert.match(read('scripts/docker-manage.sh'), /-p "namewta-\$\{category\}"/);
});

test('frontend compose provides LB, TLS profile, and independent app nginx', () => {
  const compose = read('docker/docker-compose-frontend.yml');
  assert.match(compose, /^  namewta-nginx-lb:$/m);
  assert.match(compose, /^  namewta-nginx-lb-tls:$/m);
  assert.match(compose, /profiles: \[tls\]/);
  assert.match(compose, /^  namewta-nginx-admin-web:$/m);
  assert.match(compose, /ADMIN_WEB_PREFIX:\?ADMIN_WEB_PREFIX is required/);
  assert.match(compose, /NGINX_ENVSUBST_FILTER: "\^\(APP_\|BACKEND_\|LB_\)"/);
});

test('nginx templates preserve prefix stripping and protect actuator', () => {
  for (const filename of ['nginx-lb-http.conf.template', 'nginx-lb-tls.conf.template']) {
    const config = read(`docker/frontend/nginx/lb/${filename}`);
    assert.match(config, /proxy_pass http:\/\/app_admin_web\/;/);
    assert.match(config, /APP_UPSTREAMS:/);
    assert.match(config, /APP_ROUTES:/);
    assert.match(config, /actuator/);
    assert.match(config, /return 403;/);
  }

  const app = read('docker/frontend/nginx/apps/nginx-admin-web.conf.template');
  assert.match(app, /location ~ \^\/(?:\\\$\{APP_PREFIX\}\/)?/);
  assert.match(app, /proxy_pass http:\/\/backend_server;/);
  assert.match(app, /try_files \$uri \$uri\/ \/index\.html;/);
});

test('both load balancers serve the internal no-cache maintenance page for upstream errors', () => {
  const compose = read('docker/docker-compose-frontend.yml');
  const maintenanceMount = /\.\/frontend\/nginx\/html\/nginx-lb:\/usr\/share\/nginx\/html\/nginx-lb:ro/g;
  assert.equal(compose.match(maintenanceMount)?.length, 2);

  const maintenancePage = read('docker/frontend/nginx/html/nginx-lb/system-updating.html');
  assert.match(maintenancePage, /<meta name="robots" content="noindex, nofollow">/);
  assert.match(maintenancePage, /系统正在更新中/);
  assert.doesNotMatch(maintenancePage, /<(?:script|img)\b|https?:\/\//i);
  if (fs.existsSync(path.join(releaseRoot, '.gitignore'))) {
    assert.match(read('.gitignore'), /!docker\/frontend\/nginx\/html\/nginx-lb\/system-updating\.html/);
  }

  for (const filename of ['nginx-lb-http.conf.template', 'nginx-lb-tls.conf.template']) {
    const config = read(`docker/frontend/nginx/lb/${filename}`);
    assert.match(config, /proxy_intercept_errors on;/);
    assert.match(config, /error_page 404 500 502 503 504 \/nginx-lb\/system-updating\.html;/);
    assert.match(
      config,
      /location = \/nginx-lb\/system-updating\.html \{\s+internal;\s+root \/usr\/share\/nginx\/html;\s+charset utf-8;\s+add_header Cache-Control "no-store, no-cache, must-revalidate" always;\s+\}/,
    );
  }
});

test('committed env file contains placeholders instead of runtime secrets', () => {
  const env = read('.env.example');
  for (const key of [
    'MYSQL_ROOT_PASSWORD',
    'MYSQL_APP_PASSWORD',
    'REDIS_PASSWORD',
    'MONITOR_PASSWORD',
    'GRAFANA_ADMIN_PASSWORD',
  ]) {
    assert.match(env, new RegExp(`^${key}=replace-`, 'm'));
  }
  assert.match(env, /^MYSQL_DATABASE=wta-plus$/m);
  assert.match(env, /^MINIO_ROOT_USER=namewta$/m);
  assert.match(env, /^MINIO_ENDPOINT=replace-/m);
  assert.match(env, /^MINIO_BUCKET=wta$/m);
  assert.match(env, /^MINIO_DIAGNOSTIC_OBJECT=\.well-known\/oss-readiness\/private-canary\.txt$/m);
  if (fs.existsSync(path.join(releaseRoot, '.gitignore'))) {
    assert.match(read('.gitignore'), /^\.env$/m);
  }
});

test('both admin instances inherit a default-off OpenAPI secret contract', () => {
  const backend = read('docker/docker-compose-backend.yml');
  const sharedAdminEnvironment = backend.split('\nservices:\n')[0];
  const env = read('.env.example');

  assert.equal((backend.match(/<<: \*admin-environment/g) ?? []).length, 2);
  for (const [key, expression] of [
    ['OPENAPI_ENABLED', '\\$\\{OPENAPI_ENABLED:-false\\}'],
    ['OPENAPI_KEK_VERSION', '\\$\\{OPENAPI_KEK_VERSION:-\\}'],
    ['OPENAPI_KEK', '\\$\\{OPENAPI_KEK:-\\}'],
  ]) {
    assert.equal(
      (sharedAdminEnvironment.match(new RegExp(`^  ${key}: "${expression}"$`, 'gm')) ?? []).length,
      1,
      `${key} must be declared exactly once in x-admin-environment`,
    );
  }

  assert.match(env, /^OPENAPI_ENABLED=false$/m);
  assert.match(env, /^OPENAPI_KEK_VERSION=replace-with-an-openapi-kek-version$/m);
  assert.match(env, /^OPENAPI_KEK=replace-with-a-32-byte-base64-openapi-kek$/m);
  assert.doesNotMatch(env, /^OPENAPI_ENABLED=true$/m);
});

test('Nacos infrastructure is optional, pinned, authenticated, and locally bound', () => {
  const compose = read('docker/docker-compose-infrastructure.yml');
  assert.match(compose, /^  nacos:$/m);
  assert.match(compose, /image: nacos\/nacos-server:v2\.5\.4/);
  assert.match(compose, /profiles: \[nacos\]/);
  assert.match(compose, /MODE: standalone/);
  assert.match(compose, /NACOS_AUTH_ENABLE: "true"/);
  assert.match(compose, /NACOS_AUTH_CACHE_ENABLE: "false"/);
  assert.match(compose, /JAVA_OPT: -Dnacos\.core\.auth\.caching\.enabled=false/);
  assert.match(compose, /NACOS_AUTH_USER_AGENT_AUTH_WHITE_ENABLE: "false"/);
  assert.match(compose, /NACOS_MYSQL_INIT_ENABLED: "\$\{NACOS_MYSQL_INIT_ENABLED:-false\}"/);
  assert.match(compose, /\$\{NAMEWTA_BIND_HOST:-127\.0\.0\.1\}:8848:8848/);
  assert.match(compose, /\$\{NAMEWTA_BIND_HOST:-127\.0\.0\.1\}:9848:9848/);
  assert.match(compose, /nacos\/v1\/console\/health\/readiness/);
  assert.match(compose, /condition: service_healthy/);
  assert.doesNotMatch(compose, /nacos\/nacos-server:latest/);
});

test('Nacos override requires secrets and enables both admin instances after health', () => {
  const override = read('docker/overrides/nacos-enabled.yml');
  assert.match(override, /NACOS_MYSQL_INIT_ENABLED: "true"/);
  for (const key of [
    'NACOS_DB_PASSWORD',
    'NACOS_AUTH_TOKEN',
    'NACOS_AUTH_IDENTITY_KEY',
    'NACOS_AUTH_IDENTITY_VALUE',
    'NACOS_CONFIG_PASSWORD',
  ]) {
    assert.match(override, new RegExp(`\\$\\{${key}:\\?${key} is required\\}`));
    assert.match(read('.env.example'), new RegExp(`^${key}=replace-`, 'm'));
  }
  assert.match(override, /\$\{NACOS_CONFIG_USERNAME:\?NACOS_CONFIG_USERNAME is required\}/);
  assert.match(read('.env.example'), /^NACOS_CONFIG_USERNAME=nacos$/m);
  for (const service of ['namewta-server1', 'namewta-server2']) {
    assert.match(override, new RegExp(`^  ${service}:`, 'm'));
  }
  assert.equal((override.match(/NACOS_CONFIG_ENABLED: "true"/g) ?? []).length, 2);
  assert.equal((override.match(/condition: service_healthy/g) ?? []).length, 2);
  assert.equal((override.match(/NACOS_CONFIG_SERVER_ADDR: nacos:8848/g) ?? []).length, 2);
  assert.match(override, /profiles: !reset \[\]/);

  const backend = read('docker/docker-compose-backend.yml');
  assert.doesNotMatch(backend, /NACOS_CONFIG_ENABLED/);
  assert.doesNotMatch(backend, /^\s+nacos:\s*$/m);
});

test('Nacos MySQL schema is pinned and both initialization paths are idempotent', () => {
  const schema = read('docker/infrastructure/mysql/init/60-cde-nacos.sql');
  assert.match(schema, /^SET NAMES utf8mb4;/);
  assert.match(schema, /CREATE DATABASE IF NOT EXISTS `nacos`/);
  assert.match(schema, /USE `nacos`;/);
  assert.match(schema, /alibaba\/nacos\/2\.5\.4\/distribution\/conf\/mysql-schema\.sql/);
  assert.equal(schema.match(/^CREATE TABLE/gm)?.length, 10);
  assert.doesNotMatch(schema, /INSERT\s+INTO\s+[`']?users/i);
  assert.match(schema, /config_info_gray/);

  const script = read('scripts/init-nacos-mysql-container.sh');
  assert.match(script, /CREATE DATABASE IF NOT EXISTS/);
  assert.match(script, /CREATE USER IF NOT EXISTS/);
  assert.match(script, /ALTER USER/);
  assert.match(script, /REVOKE ALL PRIVILEGES, GRANT OPTION/);
  assert.match(script, /GRANT SELECT, INSERT, UPDATE, DELETE/);
  assert.match(script, /EXPECTED_TABLES=10/);
  assert.match(script, /schema table-name verification failed/);
  assert.doesNotMatch(script, /DROP DATABASE|DROP USER|GRANT ALL PRIVILEGES/);

  const releaseScript = read('scripts/release-manage.sh');
  assert.match(releaseScript, /60-cde-nacos\.sql/);
  assert.doesNotMatch(releaseScript, /15-nacos-init\.sh/);
  assert.doesNotMatch(releaseScript, /nacos\/mysql-schema\.sql/);
  assert.doesNotMatch(releaseScript, /mysql\/init.*-delete|source_root=.*script\/sql/s);
});

test('existing-volume Nacos initializer rejects placeholder credentials without leaking them', () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'namewta-nacos-init-test-'));
  try {
    const fakeDocker = path.join(tempRoot, 'docker');
    const envFile = path.join(tempRoot, '.env');
    fs.writeFileSync(fakeDocker, '#!/bin/sh\nexit 0\n');
    fs.chmodSync(fakeDocker, 0o755);
    fs.writeFileSync(envFile, [
      'NACOS_DB_NAME=nacos',
      'NACOS_DB_USER=nacos',
      'NACOS_DB_PASSWORD=replace-with-a-real-secret',
      '',
    ].join('\n'));

    let output = '';
    try {
      execFileSync('bash', [
        path.join(releaseRoot, 'scripts/init-nacos-mysql-container.sh'),
        '--env-file', envFile,
      ], {
        encoding: 'utf8',
        env: { ...process.env, PATH: `${tempRoot}:${process.env.PATH}` },
        stdio: ['ignore', 'pipe', 'pipe'],
      });
      assert.fail('initializer should reject placeholder credentials');
    } catch (error) {
      output = `${error.stdout ?? ''}${error.stderr ?? ''}`;
    }
    assert.match(output, /NACOS_DB_PASSWORD is missing or still uses a placeholder/);
    assert.doesNotMatch(output, /replace-with-a-real-secret/);
  } finally {
    fs.rmSync(tempRoot, { recursive: true, force: true });
  }
});

test('Nacos schema SQL is not a bash hook and compose still gates the Nacos service', () => {
  const schema = read('docker/infrastructure/mysql/init/60-cde-nacos.sql');
  assert.match(schema, /^SET NAMES utf8mb4;/);
  assert.doesNotMatch(schema, /^#!/);
  const compose = read('docker/docker-compose-infrastructure.yml');
  assert.match(compose, /NACOS_MYSQL_INIT_ENABLED/);
  assert.match(compose, /profiles: \[nacos\]/);
});

test('stage-mysql validates the canonical SQL baseline without writing it', () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'namewta-stage-mysql-test-'));
  const tempRelease = path.join(tempRoot, 'release-artifacts');
  const tempInit = path.join(tempRelease, 'docker/infrastructure/mysql/init');
  try {
    fs.cpSync(releaseRoot, tempRelease, { recursive: true });
    const before = fs.readdirSync(tempInit).map((name) => [
      name,
      fs.statSync(path.join(tempInit, name)).isFile()
        ? crypto.createHash('sha256').update(fs.readFileSync(path.join(tempInit, name))).digest('hex')
        : 'directory',
    ]);

    execFileSync('bash', [path.join(tempRelease, 'scripts/release-manage.sh'), 'stage-mysql']);

    const after = fs.readdirSync(tempInit).map((name) => [
      name,
      fs.statSync(path.join(tempInit, name)).isFile()
        ? crypto.createHash('sha256').update(fs.readFileSync(path.join(tempInit, name))).digest('hex')
        : 'directory',
    ]);
    assert.deepEqual(after, before);

    fs.rmSync(path.join(tempInit, '40-cde-ai.sql'));
    assert.throws(
      () => execFileSync('bash', [path.join(tempRelease, 'scripts/release-manage.sh'), 'stage-mysql']),
      /Command failed/,
    );
  } finally {
    fs.rmSync(tempRoot, { recursive: true, force: true });
  }
});

test('MySQL initialization targets one protected wta-plus database', () => {
  const script = read('scripts/init-mysql-container.sh');
  const releaseScript = read('scripts/release-manage.sh');
  const expectedSql = [
    '10-cde-base-ddl.sql',
    '20-cde-job.sql',
    '30-cde-workflow.sql',
    '40-cde-ai.sql',
    '50-cde-base-dml.sql',
    '60-cde-nacos.sql',
];

  assert.match(script, /database.*== wta-plus/);
  assert.match(script, /refusing existing database/);
  assert.match(script, /EXPECTED_TABLES=125/);
  assert.match(script, /--default-character-set=utf8mb4/);
  assert.match(script, /access_policy='0'/);
  assert.match(script, /config_key='minio' THEN 'Y' ELSE 'N'/);
  assert.match(script, /exactly one PRIVATE default named minio/);
  assert.doesNotMatch(script, /--force/);
  for (const filename of expectedSql) {
    assert.match(script, new RegExp(filename.replaceAll('.', '\\.')));
    assert.match(releaseScript, new RegExp(filename.replaceAll('.', '\\.')));
  }

  for (const composeName of [
    'docker/docker-compose-infrastructure.yml',
    'docker/docker-compose-backend.yml',
  ]) {
    assert.match(read(composeName), /MYSQL_DATABASE:-wta-plus/);
    assert.doesNotMatch(read(composeName), /MYSQL_DATABASE:-ry-vue/);
  }
});

test('admin instances receive the private MinIO readiness canary contract', () => {
  const backend = read('docker/docker-compose-backend.yml');
  assert.match(backend, /OSS_READINESS_DIAGNOSTIC_OBJECTS_MINIO/);
  assert.match(backend, /MINIO_DIAGNOSTIC_OBJECT:-\.well-known\/oss-readiness\/private-canary\.txt/);
});

test('reserved ingress routes cannot be used as an app prefix', () => {
  const script = read('scripts/release-manage.sh');
  for (const route of ['admin', 'monitor', 'snail-job', 'snail-ai', 'dev-api', 'prod-api', 'actuator']) {
    assert.match(script, new RegExp(`\\b${route.replace('-', '\\-')}\\b`));
  }
});

const pythonExecutable = process.platform === 'win32' ? 'python' : 'python3';
let pythonAvailable = true;
try {
  execFileSync(pythonExecutable, ['--version'], { stdio: 'ignore' });
} catch {
  pythonAvailable = false;
}

(pythonAvailable ? test : test.skip)('add_app.py registers a buildable app across compose and both LBs', () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'namewta-release-test-'));
  try {
    fs.cpSync(releaseRoot, path.join(tempRoot, 'release-artifacts'), { recursive: true });
    const appRoot = path.join(tempRoot, 'frontend/apps/sample-web');
    fs.mkdirSync(appRoot, { recursive: true });
    fs.writeFileSync(
      path.join(appRoot, 'package.json'),
      '{"name":"@namewta/sample-web","scripts":{"build:dev":"vite","build:prod":"vite"}}\n',
    );

    execFileSync(pythonExecutable, [
      path.join(tempRoot, 'release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py'),
      '--repo-root', tempRoot,
      '--app', 'sample-web',
      '--prefix', 'sample',
      '--port', '41081',
    ], { stdio: 'pipe' });

    const compose = read('docker/docker-compose-frontend.yml', path.join(tempRoot, 'release-artifacts'));
    assert.match(compose, /^  namewta-nginx-sample-web:$/m);
    assert.match(compose, /SAMPLE_WEB_PREFIX/);
    assert.match(compose, /SAMPLE_WEB_PORT:-41081/);
    for (const filename of ['nginx-lb-http.conf.template', 'nginx-lb-tls.conf.template']) {
      const lb = read(
        `docker/frontend/nginx/lb/${filename}`,
        path.join(tempRoot, 'release-artifacts'),
      );
      assert.match(lb, /upstream app_sample_web/);
      assert.match(lb, /location \/\$\{APP_SAMPLE_WEB_PREFIX\}\//);
    }
    assert.ok(fs.existsSync(path.join(
      tempRoot,
      'release-artifacts/docker/frontend/nginx/apps/nginx-sample-web.conf.template',
    )));
  } finally {
    fs.rmSync(tempRoot, { recursive: true, force: true });
  }
});

test('docker manager forwards an optional logs service on macOS-compatible bash', () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'namewta-docker-cli-test-'));
  try {
    const fakeDocker = path.join(tempRoot, 'docker');
    fs.writeFileSync(fakeDocker, '#!/bin/sh\nprintf "%s\\n" "$*"\n');
    fs.chmodSync(fakeDocker, 0o755);
    const output = execFileSync(
      'bash',
      [path.join(releaseRoot, 'scripts/docker-manage.sh'), 'logs', 'backend', 'namewta-server1'],
      {
        encoding: 'utf8',
        env: {
          ...process.env,
          PATH: `${tempRoot}:${process.env.PATH}`,
          RELEASE_ENV_FILE: path.join(releaseRoot, '.env.example'),
        },
      },
    );
    assert.match(output, /logs -f --tail 200 namewta-server1/);
  } finally {
    fs.rmSync(tempRoot, { recursive: true, force: true });
  }
});
