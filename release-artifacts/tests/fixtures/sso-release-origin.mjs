#!/usr/bin/env node
// Serial, owned three-Origin fixture; never builds/stages a deployable release.
//
// Uses production Nginx templates and freshly built Apps with the existing Java
// SSO/Redis/MySQL fixture. System identity/menu and business token minting remain
// explicit test doubles. Only recorded container/network IDs are removed.

import { spawn, spawnSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import net from 'node:net';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.dirname(path.dirname(path.dirname(path.dirname(fileURLToPath(import.meta.url)))));
const IMAGES = {
  nginx: 'nginx:1.31.1@sha256:608a100c71651bf5b773c89083b4a1ad7ef4b2bd05d7a7e552271e03123692ad',
  mysql: 'mysql:8.4.9@sha256:c36050afdca850f23cef85703f84c7531a5ae155a11b5ee1c60acb09937c4084',
  redis: 'redis:7.4-alpine@sha256:ff02b58f971e7d7d156a1267e283fcbbeee91773b6aa36c49dac28ecfe28eadf',
};

function output(command) {
  const result = spawnSync(command[0], command.slice(1), { encoding: 'utf8' });
  if (result.error) throw result.error;
  const combined = `${result.stdout ?? ''}${result.stderr ?? ''}`;
  if (result.status !== 0) throw new Error(combined.trim() || `command failed (${result.status})`);
  return (result.stdout ?? '').trim();
}

function inventory() {
  const queries = {
    containers: ['ps', '-aq', '--no-trunc'],
    networks: ['network', 'ls', '-q', '--no-trunc'],
    volumes: ['volume', 'ls', '-q'],
  };
  return Object.fromEntries(Object.entries(queries).map(([kind, command]) => [kind, output(['docker', ...command]).split('\n').filter(Boolean).sort()]));
}

function availablePort(host = '127.0.0.1') {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', reject);
    server.listen(0, host, () => {
      const { port } = server.address();
      server.close(() => resolve(port));
    });
  });
}

function sleep(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

function killGroup(pid, signalName) {
  try { process.kill(-pid, signalName); }
  catch (error) { if (error.code !== 'ESRCH') throw error; }
}

function assertSame(actual, expected) {
  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    throw new Error(`${JSON.stringify(actual)} != ${JSON.stringify(expected)}`);
  }
}

function usage(message) {
  const error = new Error(message);
  error.usage = true;
  throw error;
}

function parseArgs(argv) {
  let evidence = null;
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    if (token === '--evidence') {
      evidence = argv[index + 1];
      if (!evidence || evidence.startsWith('--')) usage('missing value for --evidence');
      index += 1;
    } else usage(`unrecognized arguments: ${token}`);
  }
  if (!evidence) usage('missing required argument --evidence');
  return path.resolve(evidence);
}

async function main() {
  const evidence = parseArgs(process.argv.slice(2));
  if (fs.existsSync(evidence)) {
    console.error('Refusing to overwrite prior evidence');
    return 1;
  }
  fs.mkdirSync(path.dirname(evidence), { recursive: true });
  const owner = `namewta-sso-release-${crypto.randomBytes(6).toString('hex')}`;
  const record = {
    owner, images: IMAGES, commands: [], containers: [], network: null, cleanup: [], before: inventory(),
    boundary: 'production Nginx/App/SSO/Redis/MySQL; mocked System identity/menu/token minting',
  };
  const save = () => fs.writeFileSync(evidence, `${JSON.stringify(record, null, 2)}\n`);
  const stem = path.basename(evidence, path.extname(evidence));
  const sibling = (suffix) => path.join(path.dirname(evidence), `${stem}-${suffix}`);

  async function run(command, cwd = ROOT, env = null, timeout = 900) {
    const log = sibling(`${record.commands.length + 1}.log`);
    const started = Date.now();
    const fd = fs.openSync(log, 'w');
    const child = spawn(command[0], command.slice(1), {
      cwd: String(cwd), env: env ?? undefined, detached: true, stdio: ['ignore', fd, fd],
    });
    let timer;
    let interrupted = null;
    const finished = new Promise((resolve, reject) => {
      child.once('error', reject);
      child.once('exit', (code) => resolve(code ?? 1));
    });
    const stop = (error) => {
      interrupted = error;
      killGroup(child.pid, 'SIGTERM');
      const killer = setTimeout(() => killGroup(child.pid, 'SIGKILL'), 5000);
      finished.finally(() => clearTimeout(killer));
    };
    const previous = run.interrupt;
    run.interrupt = stop;
    let code;
    try {
      code = await Promise.race([
        finished,
        new Promise((_, reject) => { timer = setTimeout(() => reject(Object.assign(new Error('timeout'), { timeout: true })), timeout * 1000); }),
      ]);
      if (interrupted) throw interrupted;
    } catch (error) {
      if (!interrupted) stop(error);
      await finished.catch(() => {});
      throw error;
    } finally {
      clearTimeout(timer);
      run.interrupt = previous;
      fs.closeSync(fd);
    }
    const redacted = fs.readFileSync(log, 'utf8').replace(/([?&](?:code|state|code_challenge|code_verifier)=)[^&\s"'<>]+/g, '$1[REDACTED]');
    fs.writeFileSync(log, redacted);
    record.commands.push({
      command, cwd: String(cwd), exit_code: code, seconds: Math.round((Date.now() - started) / 10) / 100, log: path.basename(log),
    });
    save();
    console.log(`${command[0]} ${command[1]}: exit=${code}, log=${path.basename(log)}`);
    if (code) throw new Error('Fixture command failed; inspect recorded log');
  }

  function container(name, arguments_) {
    const identifier = output(['docker', 'create', '--name', `${owner}-${name}`, '--label', `namewta.test.owner=${owner}`, ...arguments_]);
    record.containers.push({ name, id: identifier });
    save();
    output(['docker', 'start', identifier]);
    return identifier;
  }

  async function ready(command, expected, attempts = 90) {
    for (let attempt = 0; attempt < attempts; attempt += 1) {
      const result = spawnSync(command[0], command.slice(1), { encoding: 'utf8' });
      if (result.status === 0 && (result.stdout ?? '').includes(expected)) return;
      await sleep(1000);
    }
    throw new Error('Owned service readiness timeout');
  }

  let exitCode = 1;
  let temporary = null;
  try {
    temporary = fs.mkdtempSync(path.join(os.tmpdir(), `${owner}-`));
    const shim = path.join(temporary, 'pnpm');
    fs.writeFileSync(shim, '#!/bin/sh\nexec corepack pnpm "$@"\n', { mode: 0o700 });
    const environment = {
      ...process.env,
      PATH: `${temporary}${path.delimiter}${process.env.PATH}`,
      npm_config_workspace_concurrency: '1',
      RAYON_NUM_THREADS: '1',
    };
    await run(['corepack', 'pnpm', 'build:dependencies'], path.join(ROOT, 'frontend'), environment);
    for (const [app, prefix] of [['admin-web', 'admin-app'], ['home-web', 'home-app'], ['sso-web', 'sso-app']]) {
      const buildEnvironment = {
        ...environment,
        VITE_APP_CONTEXT_PATH: `/${prefix}/`,
        VITE_APP_BASE_API: `/${prefix}/prod-api`,
        VITE_SSO_API: '',
      };
      await run(['corepack', 'pnpm', '--filter', `@namewta/${app}`, 'build:prod'], path.join(ROOT, 'frontend'), buildEnvironment);
      const dist = path.join(ROOT, 'frontend/apps', app, 'dist');
      assertSame(JSON.parse(fs.readFileSync(path.join(dist, 'build-mode.json'), 'utf8')), { app, mode: 'production' });
      record.built_apps ??= {};
      record.built_apps[app] = crypto.createHash('sha256').update(fs.readFileSync(path.join(dist, 'index.html'))).digest('hex');
    }

    const network = output(['docker', 'network', 'create', '--label', `namewta.test.owner=${owner}`, owner]);
    record.network = network;
    save();
    const gateway = JSON.parse(output(['docker', 'network', 'inspect', network]))[0].IPAM.Config[0].Gateway;
    const backendPort = await availablePort(gateway);
    const ports = {
      admin: await availablePort(),
      home: await availablePort(),
      sso: await availablePort(),
    };
    const origins = Object.fromEntries(Object.entries(ports).map(([app, port]) => [app, `https://${app === 'sso' ? 'localhost' : '127.0.0.1'}:${port}`]));
    record.origins = origins;
    const cert = path.join(temporary, 'cert');
    fs.mkdirSync(cert);
    await run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-days', '1', '-subj', '/CN=localhost',
      '-addext', 'subjectAltName=DNS:localhost,IP:127.0.0.1', '-keyout', path.join(cert, 'privkey.pem'), '-out', path.join(cert, 'fullchain.pem')]);
    const templates = path.join(ROOT, 'release-artifacts/docker/frontend/nginx');
    const backend = `${gateway}:${backendPort}`;

    async function nginx(name, template, html, bindings, { port = null, alias = null, tlsDir = null } = {}) {
      const arguments_ = ['--network', network, '-v', `${template}:/etc/nginx/templates/default.conf.template:ro`, '-v', `${html}:/usr/share/nginx/html:ro`];
      if (alias) arguments_.push('--network-alias', alias);
      if (port) arguments_.push('-p', `127.0.0.1:${port}:443`);
      if (tlsDir) arguments_.push('-v', `${cert}:/etc/nginx/cert/${tlsDir}:ro`);
      for (const [key, value] of Object.entries(bindings)) arguments_.push('-e', `${key}=${value}`);
      for (const host of ['namewta-monitor-admin', 'namewta-snailjob-server', 'nacos']) arguments_.push('--add-host', `${host}:${gateway}`);
      const identifier = container(name, [...arguments_, IMAGES.nginx]);
      record.templates ??= {};
      record.templates[path.relative(ROOT, template)] = crypto.createHash('sha256').update(fs.readFileSync(template)).digest('hex');
      await ready(['docker', 'exec', identifier, 'nginx', '-t'], '', 15);
      return identifier;
    }

    for (const app of ['admin', 'home']) {
      await nginx(app, path.join(templates, `apps/nginx-${app}-web.conf.template`), path.join(ROOT, `frontend/apps/${app}-web/dist`),
        { APP_PREFIX: `${app}-app`, BACKEND_SERVER1: backend, BACKEND_SERVER2: backend }, { alias: `namewta-nginx-${app}-web` });
    }
    for (const app of ['admin', 'home']) {
      await nginx(`${app}-tls`, path.join(templates, 'lb/nginx-lb-tls.conf.template'), path.join(templates, 'html'),
        { APP_ADMIN_WEB_PREFIX: 'admin-app', APP_HOME_WEB_PREFIX: 'home-app', LB_SERVER_NAME: '127.0.0.1' },
        { port: ports[app], tlsDir: 'lb' });
    }
    await nginx('sso-tls', path.join(templates, 'apps/nginx-sso-web-tls.conf.template'), path.join(ROOT, 'frontend/apps/sso-web/dist'),
      { APP_PREFIX: 'sso-app', APP_ORIGIN: origins.sso, BACKEND_SERVER1: backend, BACKEND_SERVER2: backend },
      { port: ports.sso, tlsDir: 'sso-web' });

    const database = `namewta_sso_test_${crypto.randomBytes(6).toString('hex')}`;
    const mysql = container('mysql', ['-p', '127.0.0.1::3306', '-e', 'MYSQL_ROOT_PASSWORD=owned-sso-test-only',
      '-e', `MYSQL_DATABASE=${database}`, IMAGES.mysql, '--character-set-server=utf8mb4', '--collation-server=utf8mb4_general_ci']);
    await ready(['docker', 'exec', mysql, 'mysqladmin', 'ping', '-h', '127.0.0.1', '-uroot', '-powned-sso-test-only', '--silent'], 'alive');
    const mysqlPort = output(['docker', 'port', mysql, '3306/tcp']).split(':').at(-1);
    const redis = container('redis', ['-p', '127.0.0.1::6379', IMAGES.redis, 'redis-server', '--save', '', '--appendonly', 'no']);
    await ready(['docker', 'exec', redis, 'redis-cli', 'ping'], 'PONG');
    const redisPort = output(['docker', 'port', redis, '6379/tcp']).split(':').at(-1);
    const command = ['./mvnw', '-B', '-ntp', '-pl', 'wta-admin', '-am', 'test', '-Dtest=SsoHttpsSessionIntegrationTest,AuthClientContextSsoUnitTest',
      '-Dsurefire.failIfNoSpecifiedTests=false', '-Dsso.release.integration=true',
      `-Dsso.release.backend.host=${gateway}`, `-Dsso.release.backend.port=${backendPort}`,
      `-Dsso.redis.integration.port=${redisPort}`,
      `-Dsso.mysql.integration.url=jdbc:mysql://127.0.0.1:${mysqlPort}/${database}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai`,
      `-Dnamewta.repo.root=${ROOT}`, `-Dnamewta.sql.root=${path.join(ROOT, 'release-artifacts/docker/infrastructure/mysql/init')}`,
      ...Object.entries(origins).map(([app, origin]) => `-Dsso.release.${app}.origin=${origin}`)];
    environment.MAVEN_OPTS = `${environment.MAVEN_OPTS ?? ''} -Daether.connector.requestTimeout=60000 -Daether.connector.connectTimeout=10000 -Daether.connector.basic.threads=1 -Dmaven.artifact.threads=1`;
    await run(command, path.join(ROOT, 'backend'), environment);
    const attributes = (file) => {
      const xml = fs.readFileSync(file, 'utf8');
      const start = xml.match(/<testsuite\b([^>]*)\/?>/);
      const values = {};
      for (const match of start[1].matchAll(/([A-Za-z_:][\w:.-]*)\s*=\s*"([^"]*)"/g)) values[match[1]] = match[2];
      return Object.fromEntries(['tests', 'failures', 'errors', 'skipped'].map((key) => [key, Number(values[key])]));
    };
    record.tests = attributes(path.join(ROOT, 'backend/wta-admin/target/surefire-reports/TEST-org.namewta.test.sso.SsoHttpsSessionIntegrationTest.xml'));
    assertSame(record.tests, { tests: 1, failures: 0, errors: 0, skipped: 0 });
    record.client_context_tests = attributes(path.join(ROOT, 'backend/wta-admin/target/surefire-reports/TEST-org.namewta.web.controller.AuthClientContextSsoUnitTest.xml'));
    assertSame(record.client_context_tests, { tests: 3, failures: 0, errors: 0, skipped: 0 });
    exitCode = 0;
  } catch (failure) {
    record.failure = String(failure?.message ?? failure);
    console.log(record.failure);
  } finally {
    if (temporary) fs.rmSync(temporary, { recursive: true, force: true });
    for (const row of [...record.containers].reverse()) {
      if (row.name !== 'mysql' && row.name !== 'redis') {
        const logs = spawnSync('docker', ['logs', row.id], { encoding: 'utf8' });
        const safe = `${logs.stdout ?? ''}${logs.stderr ?? ''}`.replace(/\?[^\s"']+/g, '?[REDACTED]');
        fs.writeFileSync(sibling(`${row.name}.log`), safe);
      }
      const removed = spawnSync('docker', ['rm', '-fv', row.id], { encoding: 'utf8' });
      record.cleanup.push({ id: row.id, exit_code: removed.status ?? 1 });
      if (removed.status) exitCode = 1;
    }
    if (record.network) {
      const removed = spawnSync('docker', ['network', 'rm', record.network], { encoding: 'utf8' });
      record.cleanup.push({ network: record.network, exit_code: removed.status ?? 1 });
      if (removed.status) exitCode = 1;
    }
    record.after = inventory();
    record.resources_restored = JSON.stringify(record.before) === JSON.stringify(record.after);
    if (!record.resources_restored) exitCode = 1;
    record.exit_code = exitCode;
    save();
  }
  return exitCode;
}

const scriptPath = fs.realpathSync(fileURLToPath(import.meta.url));
if (process.argv[1] && fs.existsSync(process.argv[1]) && fs.realpathSync(process.argv[1]) === scriptPath) {
  main().then((code) => process.exit(code ?? 0)).catch((error) => {
    console.error(error.usage ? error.message : (error.stack || error.message));
    process.exit(error.usage ? 2 : 1);
  });
}
