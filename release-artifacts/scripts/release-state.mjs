#!/usr/bin/env node
// Build immutable releases and atomically select one. Never rewrite live Docker contexts.
//
// The manifest detects corruption/mixed inputs, not a hostile administrator who can replace
// both code and manifests. All deployable inputs come from one clean Git archive; ignored
// workspace artifacts and runtime credentials are deliberately absent from that archive.

import { spawn, spawnSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import zlib from 'node:zlib';

const SCRIPT_PATH = fs.realpathSync(fileURLToPath(import.meta.url));
const RELEASE_ROOT = path.dirname(path.dirname(SCRIPT_PATH));
const REPO_ROOT = path.dirname(RELEASE_ROOT);
export const BACKENDS = {
  'wta-admin': 'wta-admin/target/wta-admin.jar',
  'wta-monitor-admin': 'wta-extend/wta-monitor-admin/target/wta-monitor-admin.jar',
  'wta-snailjob-server': 'wta-extend/wta-snailjob-server/target/wta-snailjob-server.jar',
};
const SQL_FILES = ['10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql', '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql'];
const CATEGORIES = ['infrastructure', 'observability', 'backend', 'frontend'];
const RESERVED = new Set(['admin', 'monitor', 'snail-job', 'snail-ai', 'dev-api', 'prod-api', 'actuator']);
const MANIFEST = 'release-manifest.json';
const VERSION_PATTERN = /^(dev|prod)-[0-9a-f]{12}-[0-9a-f]{64}$/;

function fail(message) {
  const error = new Error(message);
  error.userFacing = true;
  throw error;
}

function require(condition, message) {
  if (!condition) fail(message);
}

function info(message) {
  console.error(`[INFO] ${message}`);
}

function isSymlink(file) {
  try { return fs.lstatSync(file).isSymbolicLink(); }
  catch (error) { if (error.code === 'ENOENT') return false; throw error; }
}

function isFile(file) {
  try { return fs.statSync(file).isFile(); }
  catch { return false; }
}

function isDir(file) {
  try { return fs.statSync(file).isDirectory(); }
  catch { return false; }
}

function readText(file) {
  return fs.readFileSync(file, 'utf8');
}

function readJson(file) {
  try { return JSON.parse(readText(file)); }
  catch (error) { if (error instanceof SyntaxError) fail(`invalid JSON: ${path.basename(file)}`); throw error; }
}

function writeText(file, text) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, text);
}

function digest(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function fullMatch(pattern, value) {
  return new RegExp(`^(?:${pattern})$`).test(value ?? '');
}

function removePrefix(value, prefix) {
  return value.startsWith(prefix) ? value.slice(prefix.length) : value;
}

function same(left, right) {
  if (left === right) return true;
  if (Array.isArray(left) || Array.isArray(right)) {
    return Array.isArray(left) && Array.isArray(right) && left.length === right.length && left.every((item, index) => same(item, right[index]));
  }
  if (left && right && typeof left === 'object' && typeof right === 'object') {
    const keys = Object.keys(left);
    return keys.length === Object.keys(right).length && keys.every((key) => Object.hasOwn(right, key) && same(left[key], right[key]));
  }
  return false;
}

function sameSet(left, right) {
  if (left.size !== right.size) return false;
  for (const value of left) if (!right.has(value)) return false;
  return true;
}

let interruptReject = null;

function onSignal(signalName) {
  const error = new Error(`release operation interrupted by signal ${signalName}`);
  error.userFacing = true;
  if (interruptReject) interruptReject(error);
  else {
    console.error(`[ERROR] ${error.message}`);
    process.exit(1);
  }
}

function killGroup(pid, signalName) {
  try { process.kill(-pid, signalName); }
  catch (error) { if (error.code !== 'ESRCH') throw error; }
}

// An interrupted owner waits for its child group before removing its private scratch.
function run(args, { cwd, env, capture = false, stdoutFd } = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(String(args[0]), args.slice(1).map(String), {
      cwd: cwd == null ? undefined : String(cwd),
      env,
      detached: true,
      stdio: capture ? ['ignore', 'pipe', 'inherit'] : ['ignore', stdoutFd ?? 'inherit', 'inherit'],
    });
    let output = '';
    if (capture) child.stdout.on('data', (chunk) => { output += chunk; });
    const previous = interruptReject;
    let settled = false;
    let interrupted = null;
    const finish = (callback, value) => {
      if (settled) return;
      settled = true;
      interruptReject = previous;
      callback(value);
    };
    interruptReject = (error) => {
      interrupted = error;
      if (child.exitCode === null && child.signalCode === null) {
        killGroup(child.pid, 'SIGTERM');
        const timer = setTimeout(() => killGroup(child.pid, 'SIGKILL'), 5000);
        child.once('exit', () => clearTimeout(timer));
      }
    };
    child.on('error', (error) => finish(reject, error));
    child.on('exit', (code) => {
      if (interrupted) finish(reject, interrupted);
      else if (code === 0) finish(resolve, capture ? output.trim() : undefined);
      else finish(reject, Object.assign(new Error(`command failed (${code ?? 'signal'}): ${path.basename(String(args[0]))}`), { userFacing: true }));
    });
  });
}

function envValues(file) {
  const values = {};
  if (isFile(file)) {
    for (const line of readText(file).split(/\r\n|\n|\r/)) {
      const match = line.match(/^\s*(?:export\s+)?([A-Z][A-Z0-9_]*)\s*=\s*(.*?)\s*$/);
      if (match) values[match[1]] = match[2].replace(/^['"]+|['"]+$/g, '');
    }
  }
  return { ...values, ...process.env };
}

function prefixes(apps, envFile) {
  const values = envValues(envFile);
  const result = {};
  for (const app of apps) {
    const key = `${app.toUpperCase().replaceAll('-', '_')}_PREFIX`;
    const value = values[key] ?? '';
    require(fullMatch('[A-Za-z0-9][A-Za-z0-9-]*', value) && !value.startsWith('replace-') && !RESERVED.has(value),
      `${key} missing, placeholder or reserved/invalid prefix`);
    require(!Object.values(result).includes(value), 'duplicate app prefix');
    result[app] = value;
  }
  return result;
}

function serviceBody(compose, name) {
  const match = new RegExp(`^  ${escapeRegExp(name)}:\\n([\\s\\S]*?)(?=^\\S|^  \\S|(?![\\s\\S]))`, 'm').exec(compose);
  require(match, `app needs exactly one Compose service: ${name}`);
  return match[1];
}

function appRegistry(release, frontend = null) {
  // The checked-in inventory decides shipping; discovery only detects omissions/drift.
  const document = readJson(path.join(release, 'apps.json'));
  require(document.schemaVersion === 1 && Array.isArray(document.apps), 'invalid app registry');
  const rows = document.apps;
  require(rows.length > 0 && new Set(rows.map((row) => row.id)).size === rows.length, 'empty or duplicate app registration');
  const compose = readText(path.join(release, 'docker/docker-compose-frontend.yml'));
  const example = readText(path.join(release, '.env.example'));
  const active = [];
  const ports = new Set();
  const services = new Set();
  for (const row of rows) {
    const app = row.id;
    require(fullMatch('[a-z][a-z0-9-]*', app), 'invalid registered app name');
    require(typeof row.shipped === 'boolean', 'shipped must be explicit boolean');
    if (frontend !== null) {
      const packageJson = readJson(path.join(frontend, 'apps', app, 'package.json'));
      require(packageJson.name === row.package, `app package mismatch: ${app}`);
      const scripts = packageJson.scripts ?? {};
      require(scripts['build:dev'] && scripts['build:prod'], `app lacks build scripts: ${app}`);
    }
    if (!row.shipped) continue;
    active.push(row);
    require(row.apiKind === 'business' || row.apiKind === 'sso', `unknown API kind: ${app}`);
    require(row.apiPath === (row.apiKind === 'sso' ? '/sso' : '/{prefix}/{environment}-api'), `unsupported API route: ${app}`);
    require(row.apiKind === 'sso' ? row.authorizePath === '/authorize' : row.callbackPath === '/sso/callback', `unsupported auth entry: ${app}`);
    require(row.healthPath === '/healthz', `unsupported health path: ${app}`);
    require(row.ingress === 'lb' || row.ingress === 'dedicated', `unknown ingress kind: ${app}`);
    require(row.apiKind !== 'sso' || row.ingress === 'dedicated', 'SSO must have a dedicated ingress');
    for (const key of ['prefixEnv', 'originEnv', 'portEnv']) {
      const variable = row[key];
      require(fullMatch('[A-Z][A-Z0-9_]*', variable), `invalid env variable for ${app}`);
      require(new RegExp(`^${escapeRegExp(variable)}=`, 'm').test(example), `missing env declaration: ${variable}`);
    }
    require(row.prefixEnv === `${app.toUpperCase().replaceAll('-', '_')}_PREFIX`, `prefix key mismatch: ${app}`);
    const endpoints = [row, ...(row.tls ? [row.tls] : [])];
    for (const endpoint of endpoints) {
      const name = endpoint.composeService;
      const template = endpoint.nginxTemplate;
      require(fullMatch('namewta-nginx-[a-z0-9-]+', name), 'invalid Compose service name');
      require((compose.match(new RegExp(`^  ${escapeRegExp(name)}:$`, 'gm')) ?? []).length === 1, `app needs exactly one Compose service: ${name}`);
      require(fullMatch(String.raw`docker/frontend/nginx/apps/nginx-[a-z0-9-]+\.conf\.template`, template), 'invalid nginx template path');
      const templateFile = path.join(release, template);
      require(isFile(templateFile), `missing nginx template: ${app}`);
      require(readText(templateFile).includes('location = /healthz'), `missing App health route: ${app}`);
      require(!services.has(name), 'duplicate App Compose service registration');
      services.add(name);
      require(new RegExp(`^${escapeRegExp(endpoint.portEnv)}=`, 'm').test(example), `missing port env: ${endpoint.portEnv}`);
      const port = endpoint.defaultPort;
      require(Number.isInteger(port) && port >= 1024 && port <= 65535 && !ports.has(port), 'invalid/duplicate app port');
      ports.add(port);
      const block = serviceBody(compose, name);
      // This repository owns the YAML layout. Reject drift before invoking compilers;
      // the independent Compose parser gate still validates the whole document.
      const bindings = [
        `./${removePrefix(template, 'docker/')}:/etc/nginx/templates/default.conf.template:ro`,
        `./frontend/nginx/html/${app}:/usr/share/nginx/html:ro`,
        `APP_PREFIX: "\${${row.prefixEnv}:?${row.prefixEnv} is required}"`,
        `\${${endpoint.portEnv}:-${port}}:${endpoint === row.tls ? '443' : '80'}`,
        'BACKEND_SERVER1: "${BACKEND_SERVER1:-namewta-server1:8080}"',
        'BACKEND_SERVER2: "${BACKEND_SERVER2:-namewta-server2:8080}"',
      ];
      if (row.apiKind === 'sso') bindings.push(`APP_ORIGIN: "\${${row.originEnv}:?${row.originEnv} is required}"`);
      require(bindings.every((binding) => block.split(binding).length - 1 === 1), `Compose App binding drift: ${name}`);
    }
    for (const name of ['nginx-lb-http.conf.template', 'nginx-lb-tls.conf.template']) {
      const lb = readText(path.join(release, 'docker/frontend/nginx/lb', name));
      const marker = `APP_${row.prefixEnv}`;
      if (row.ingress === 'lb') require(lb.includes(marker), `App missing from LB: ${app}`);
      else require(!lb.includes(marker) && !lb.includes(row.composeService), `dedicated App exposed on shared LB: ${app}`);
    }
  }
  require(active.length > 0, 'no shipped apps');
  require(active.filter((app) => app.apiKind === 'sso').length === 1, 'exactly one shipped SSO App is required');
  const configured = new Set([...(compose.matchAll(/^  (namewta-nginx-[a-z0-9-]+):$/gm))].map((match) => match[1]));
  configured.delete('namewta-nginx-lb');
  configured.delete('namewta-nginx-lb-tls');
  require(sameSet(configured, services), 'unregistered or unshipped App Compose service');
  if (frontend !== null) {
    const discovered = new Set(fs.readdirSync(path.join(frontend, 'apps')).filter((name) => isFile(path.join(frontend, 'apps', name, 'package.json'))));
    require(sameSet(discovered, new Set(rows.map((row) => row.id))), 'unregistered or missing frontend App');
  }
  return active;
}

function splitOrigin(value, label) {
  let parsed;
  try { parsed = new URL(value); }
  catch { fail(`invalid origin: ${label}`); }
  const scheme = parsed.protocol.slice(0, -1);
  let hostname = parsed.hostname ?? '';
  if (hostname.startsWith('[') && hostname.endsWith(']')) hostname = hostname.slice(1, -1);
  const port = parsed.port === '' ? null : Number(parsed.port);
  require(['http', 'https'].includes(scheme) && hostname && !parsed.username && !parsed.password
    && (parsed.pathname === '' || parsed.pathname === '/') && !value.includes('?') && !value.includes('#') && !/\s/.test(value),
    `invalid origin: ${label}`);
  require(port === null || (Number.isInteger(port) && port >= 1 && port <= 65535), 'invalid App origin port');
  return { scheme, hostname, port };
}

function canonicalOrigin(parts) {
  const host = parts.hostname.includes(':') ? `[${parts.hostname}]` : parts.hostname;
  const defaultPort = parts.scheme === 'https' ? 443 : 80;
  return `${parts.scheme}://${host}${parts.port && parts.port !== defaultPort ? `:${parts.port}` : ''}`;
}

function validateOriginMatrix(apps, values, environment) {
  const origins = {};
  for (const app of apps) {
    const value = values[app.id] ?? '';
    require(typeof value === 'string', `invalid origin: ${app.originEnv}`);
    const parts = splitOrigin(value, app.originEnv);
    require(environment !== 'prod' || parts.scheme === 'https', 'production App origins require HTTPS');
    require(!parts.hostname.endsWith('.invalid') && !parts.hostname.includes('replace-'), 'placeholder App origin');
    require(parts.hostname.includes(':') || fullMatch('[A-Za-z0-9.-]+', parts.hostname), 'invalid App hostname');
    origins[app.id] = canonicalOrigin(parts);
  }
  for (const app of apps) {
    if (app.apiKind !== 'sso') continue;
    require(Object.entries(origins).every(([name, origin]) => name === app.id || origin !== origins[app.id]), 'SSO must use an independent Web Origin');
    if (environment === 'prod') {
      // Cookies have a hostname/path boundary; different ports do not isolate them.
      require(Object.entries(origins).every(([name, origin]) => name === app.id || splitOrigin(origin, app.originEnv).hostname !== splitOrigin(origins[app.id], app.originEnv).hostname),
        'production SSO cookie requires a separate hostname');
    }
  }
  return origins;
}

function appOrigins(apps, envFile, environment) {
  const values = envValues(envFile);
  return validateOriginMatrix(apps, Object.fromEntries(apps.map((app) => [app.id, values[app.originEnv] ?? ''])), environment);
}

function formatTemplate(template, prefix, environment) {
  return template.replaceAll('{prefix}', prefix).replaceAll('{environment}', environment);
}

function applicationMatrix(registered, prefixesByApp, origins, environment) {
  const matrix = {};
  for (const app of registered) {
    const name = app.id;
    const prefix = prefixesByApp[name];
    const base = `${origins[name]}/${prefix}`;
    const entry = {
      baseUrl: `${base}/`,
      apiPath: formatTemplate(app.apiPath, prefix, environment),
      httpPortEnv: app.portEnv,
      httpPortDefault: app.defaultPort,
    };
    if (app.tls) {
      entry.httpsPortEnv = app.tls.portEnv;
      entry.httpsPortDefault = app.tls.defaultPort;
    }
    if (app.apiKind === 'sso') {
      require(prefix !== 'sso', 'SSO static prefix collides with /sso API');
      entry.authorizeUrl = base + app.authorizePath;
    } else entry.callbackUrl = base + app.callbackPath;
    matrix[name] = entry;
  }
  return matrix;
}

function listTree(root) {
  const entries = [];
  const walk = (directory) => {
    for (const name of fs.readdirSync(directory)) {
      const file = path.join(directory, name);
      const stat = fs.lstatSync(file);
      entries.push({ file, stat });
      if (stat.isDirectory() && !stat.isSymbolicLink()) walk(file);
    }
  };
  walk(root);
  entries.sort((left, right) => (left.file < right.file ? -1 : left.file > right.file ? 1 : 0));
  return entries;
}

function regularFiles(root) {
  require(isDir(root) && !isSymlink(root), 'release directory must be a real directory');
  const files = [];
  for (const entry of listTree(root)) {
    const relative = path.relative(root, entry.file);
    require(!entry.stat.isSymbolicLink(), `symlink not allowed inside release: ${relative}`);
    require(entry.stat.isDirectory() || entry.stat.isFile(), 'special file not allowed inside release');
    if (entry.stat.isFile() && entry.file !== path.join(root, MANIFEST)) files.push(entry.file);
  }
  return files;
}

function validateSql(root) {
  const directory = path.join(root, 'docker/infrastructure/mysql/init');
  require(isDir(directory), 'missing SQL baseline directory');
  const found = fs.readdirSync(directory).filter((name) => name.endsWith('.sql')).sort();
  require(same(found, SQL_FILES), 'SQL baseline must contain exactly six ordered files');
  for (const name of SQL_FILES) {
    const text = readText(path.join(directory, name)).trim();
    // This is an envelope check. SQL execution/schema correctness remains a MySQL CI gate.
    require(text.startsWith('SET NAMES utf8mb4;') && text.endsWith(';') && !text.includes('\0'), `empty or truncated SQL baseline: ${name}`);
  }
}

function readZip(buffer, label) {
  const failZip = (message = 'File is not a zip file') => fail(message.includes('corrupt JAR') ? message : message);
  if (buffer.length < 22 || buffer.readUInt32LE(0) !== 0x04034b50) failZip();
  let eocd = -1;
  for (let index = buffer.length - 22; index >= Math.max(0, buffer.length - 22 - 65535); index -= 1) {
    if (buffer.readUInt32LE(index) === 0x06054b50) { eocd = index; break; }
  }
  if (eocd < 0) failZip();
  const count = buffer.readUInt16LE(eocd + 10);
  const centralStart = buffer.readUInt32LE(eocd + 16);
  if (count === 0xffff || centralStart === 0xffffffff) failZip();
  const names = [];
  const files = new Map();
  let central = centralStart;
  for (let index = 0; index < count; index += 1) {
    if (central + 46 > buffer.length || buffer.readUInt32LE(central) !== 0x02014b50) failZip();
    const flags = buffer.readUInt16LE(central + 8);
    const method = buffer.readUInt16LE(central + 10);
    const crc = buffer.readUInt32LE(central + 16);
    const compressedSize = buffer.readUInt32LE(central + 20);
    const nameLength = buffer.readUInt16LE(central + 28);
    const extraLength = buffer.readUInt16LE(central + 30);
    const commentLength = buffer.readUInt16LE(central + 32);
    const localOffset = buffer.readUInt32LE(central + 42);
    if ((flags & 1) !== 0 || compressedSize === 0xffffffff || localOffset === 0xffffffff) fail(`corrupt JAR: ${label}`);
    const name = buffer.toString('utf8', central + 46, central + 46 + nameLength);
    names.push(name);
    if (localOffset + 30 > buffer.length || buffer.readUInt32LE(localOffset) !== 0x04034b50) failZip();
    const start = localOffset + 30 + buffer.readUInt16LE(localOffset + 26) + buffer.readUInt16LE(localOffset + 28);
    const compressed = buffer.subarray(start, start + compressedSize);
    let data;
    if (method === 0) data = Buffer.from(compressed);
    else if (method === 8) {
      try { data = zlib.inflateRawSync(compressed); }
      catch { fail(`corrupt JAR: ${label}`); }
    } else fail(`corrupt JAR: ${label}`);
    if ((zlib.crc32(data) >>> 0) !== crc) fail(`corrupt JAR: ${label}`);
    files.set(name, data);
    central += 46 + nameLength + extraLength + commentLength;
  }
  if (new Set(names).size !== names.length) fail(`corrupt JAR: ${label}`);
  return { names, files };
}

function validateJar(file) {
  const label = path.basename(file);
  require(isFile(file), `missing JAR: ${label}`);
  const zip = readZip(fs.readFileSync(file), label);
  require(zip.names.includes('META-INF/MANIFEST.MF') && zip.names.some((name) => name.startsWith('BOOT-INF/classes/')),
    `not an executable Spring Boot JAR: ${label}`);
  const manifest = zip.files.get('META-INF/MANIFEST.MF');
  require(manifest.includes(Buffer.from('Main-Class:')) && manifest.includes(Buffer.from('Start-Class:')), `missing JAR entry point: ${label}`);
}

function validatePayload(root, metadata) {
  require(metadata.target === 'all' && metadata.source.clean === true, 'only clean complete builds are deployable');
  require(fullMatch('[0-9a-f]{40}', metadata.source.revision), 'invalid source revision');
  require(fullMatch('[0-9a-f]{40}', metadata.source.tree), 'invalid source tree');
  require(fullMatch('[0-9a-f]{64}', metadata.source.archiveSha256), 'invalid source archive digest');
  require(['dev', 'prod'].includes(metadata.environment) && ['full', 'core'].includes(metadata.backendBundle), 'invalid release mode');
  const apps = metadata.apps;
  require(apps && typeof apps === 'object' && !Array.isArray(apps) && Object.keys(apps).length > 0, 'missing app inventory');
  const registered = appRegistry(root);
  const registeredIds = new Set(registered.map((app) => app.id));
  require(Object.keys(apps).length === registeredIds.size && Object.keys(apps).every((app) => registeredIds.has(app)), 'manifest differs from shipped app registry');
  require(sameSet(new Set(Object.keys(metadata.appOrigins)), new Set(Object.keys(apps))), 'missing App origin matrix');
  require(same(validateOriginMatrix(registered, metadata.appOrigins, metadata.environment), metadata.appOrigins), 'noncanonical App origin matrix');
  require(same(applicationMatrix(registered, apps, metadata.appOrigins, metadata.environment), metadata.applicationMatrix), 'release application matrix mismatch');
  for (const app of registered) require(app.apiKind !== 'sso' || apps[app.id] !== 'sso', 'SSO static prefix collides with /sso API');
  for (const name of Object.keys(BACKENDS)) {
    const context = path.join(root, 'docker/backend/images', name);
    validateJar(path.join(context, 'app.jar'));
    require(isFile(path.join(context, 'Dockerfile')), `missing Dockerfile: ${name}`);
  }
  validateSql(root);
  for (const category of CATEGORIES) require(isFile(path.join(root, `docker/docker-compose-${category}.yml`)), `missing Compose: ${category}`);
  for (const name of ['nginx-lb-http.conf.template', 'nginx-lb-tls.conf.template']) {
    require(isFile(path.join(root, 'docker/frontend/nginx/lb', name)), `missing LB template: ${name}`);
  }
  for (const [app, prefix] of Object.entries(apps)) {
    require(fullMatch('[a-z][a-z0-9-]*', app), 'invalid app name');
    require(fullMatch('[A-Za-z0-9][A-Za-z0-9-]*', prefix) && !RESERVED.has(prefix), 'invalid app prefix');
    const base = path.join(root, 'docker/frontend/nginx');
    require(isFile(path.join(base, `apps/nginx-${app}.conf.template`)), `missing nginx template: ${app}`);
    require(isFile(path.join(base, `html/${app}/index.html`)), `missing app HTML: ${app}`);
    const mode = readJson(path.join(base, `html/${app}/build-mode.json`));
    require(same(mode, { app, mode: metadata.environment === 'prod' ? 'production' : 'development' }), `wrong build mode: ${app}`);
  }
  require(new Set(Object.values(apps)).size === Object.keys(apps).length, 'duplicate app prefix');
}

function seal(root, metadata) {
  validatePayload(root, metadata);
  const source = metadata.source.revision;
  const sealed = {
    schemaVersion: 2, ...metadata,
    files: regularFiles(root).map((file) => {
      const stat = fs.statSync(file);
      return {
        path: path.relative(root, file), sha256: digest(file), size: stat.size,
        executable: (stat.mode & 0o111) !== 0, sourceRevision: source,
      };
    }),
  };
  writeText(path.join(root, MANIFEST), `${JSON.stringify(sealed, null, 2)}\n`);
  return sealed;
}

function versionId(root, manifest) {
  return `${manifest.environment}-${manifest.source.revision.slice(0, 12)}-${digest(path.join(root, MANIFEST))}`;
}

function verify(root, environment = null) {
  require(!isSymlink(root), 'version must not be a symlink');
  const manifestPath = path.join(root, MANIFEST);
  require(isFile(manifestPath) && !isSymlink(manifestPath), 'missing release manifest');
  const files = regularFiles(root); // Reject links before reading any target metadata.
  require([root, ...listTree(root).map((entry) => entry.file)].every((file) => (fs.statSync(file).mode & 0o222) === 0), 'version is not sealed read-only');
  const manifest = readJson(manifestPath);
  require(manifest.schemaVersion === 2, 'unsupported manifest');
  require(environment === null || manifest.environment === environment, 'release environment mismatch');
  require(path.basename(root) === versionId(root, manifest), 'version ID does not match manifest digest');
  const rows = manifest.files;
  require(Array.isArray(rows) && rows.length > 0, 'empty manifest inventory');
  require(new Set(rows.map((row) => row.path)).size === rows.length, 'duplicate manifest path');
  const expected = new Map(rows.map((row) => [row.path, row]));
  const actual = new Map(files.map((file) => [path.relative(root, file), file]));
  require(expected.size === actual.size && [...expected.keys()].every((name) => actual.has(name)), 'release file inventory changed');
  for (const [name, file] of actual) {
    const record = expected.get(name);
    const stat = fs.statSync(file);
    require(record.sourceRevision === manifest.source.revision, `mixed source: ${name}`);
    require(record.size === stat.size && record.sha256 === digest(file), `artifact digest mismatch: ${name}`);
    require(record.executable === ((stat.mode & 0o111) !== 0), `artifact executable mode changed: ${name}`);
  }
  validatePayload(root, manifest);
  return manifest;
}

function releasePath(releaseId) {
  require(VERSION_PATTERN.test(releaseId ?? ''), 'explicit immutable release ID required');
  const builds = path.join(RELEASE_ROOT, 'builds');
  const versions = path.join(builds, 'versions');
  require(!isSymlink(builds) && !isSymlink(versions), 'release storage cannot be a symlink');
  return path.join(versions, releaseId);
}

function resolveRelease(environment, envFile = null) {
  const current = path.join(RELEASE_ROOT, 'builds', `current_${environment}`);
  require(isSymlink(current), 'current must select an immutable version; stage --release ID first');
  const target = fs.readlinkSync(current);
  const parts = target.split('/');
  require(parts.length === 2 && parts[0] === 'versions', 'current points outside release versions');
  const version = releasePath(parts[1]);
  const manifest = verify(version, environment);
  if (envFile !== null) {
    require(same(prefixes(Object.keys(manifest.apps), envFile), manifest.apps), 'runtime prefixes differ from built app prefixes');
    require(same(appOrigins(appRegistry(version), envFile, environment), manifest.appOrigins), 'runtime origins differ from release origin matrix');
  }
  return version;
}

function acquireLock() {
  const builds = path.join(RELEASE_ROOT, 'builds');
  require(!isSymlink(builds), 'builds cannot be a symlink');
  fs.mkdirSync(builds, { recursive: true });
  // Keep the inode permanently: unlinking a flock file allows two owners on different inodes.
  const fd = fs.openSync(path.join(builds, '.release.lock'), fs.constants.O_CREAT | fs.constants.O_RDWR | fs.constants.O_NOFOLLOW, 0o600);
  const result = spawnSync('flock', ['-n', '3'], { stdio: ['ignore', 'ignore', 'ignore', fd] });
  if (result.error || result.status !== 0) {
    fs.closeSync(fd);
    fail(result.error ? `flock is required to serialize release operations: ${result.error.message}` : 'another release operation owns the lock');
  }
  return fd;
}

async function withLock(body) {
  const fd = acquireLock();
  try { await body(); }
  finally { fs.closeSync(fd); }
}

function makeReadOnly(root) {
  for (const file of [...regularFiles(root), path.join(root, MANIFEST)]) {
    const executable = (fs.statSync(file).mode & 0o111) !== 0;
    fs.chmodSync(file, executable ? 0o555 : 0o444);
  }
  const directories = listTree(root).filter((entry) => entry.stat.isDirectory()).map((entry) => entry.file);
  for (const directory of directories.reverse()) fs.chmodSync(directory, 0o555);
  fs.chmodSync(root, 0o555);
}

function fsyncFile(file) {
  const fd = fs.openSync(file, 'r');
  try { fs.fsyncSync(fd); }
  finally { fs.closeSync(fd); }
}

function fsyncDir(directory) {
  const fd = fs.openSync(directory, fs.constants.O_RDONLY | fs.constants.O_DIRECTORY);
  try { fs.fsyncSync(fd); }
  finally { fs.closeSync(fd); }
}

function fsyncTree(root) {
  for (const file of [...regularFiles(root), path.join(root, MANIFEST)]) fsyncFile(file);
  for (const entry of listTree(root)) if (entry.stat.isDirectory()) fsyncDir(entry.file);
  fsyncDir(root);
}

function replaceFile(source, target) {
  return new Promise((resolve, reject) => {
    const previous = interruptReject;
    let settled = false;
    const finish = (callback, value) => {
      if (settled) return;
      settled = true;
      interruptReject = previous;
      callback(value);
    };
    interruptReject = (error) => finish(reject, error);
    fs.rename(source, target, (error) => (error ? finish(reject, error) : finish(resolve)));
  });
}

async function stage(environment, releaseId) {
  const version = releasePath(releaseId);
  verify(version, environment);
  const builds = path.join(RELEASE_ROOT, 'builds');
  const current = path.join(builds, `current_${environment}`);
  let currentExists = true;
  try { fs.lstatSync(current); }
  catch (error) { if (error.code !== 'ENOENT') throw error; currentExists = false; }
  require(!currentExists || isSymlink(current), 'legacy current directory must be handled explicitly; refusing to replace it');
  // A unique link in the same directory gives exactly one rename visibility boundary.
  const pending = path.join(builds, `.pointer-${crypto.randomBytes(16).toString('hex')}`);
  try {
    fs.symlinkSync(`versions/${releaseId}`, pending);
    fsyncDir(builds);
    await replaceFile(pending, current);
    fsyncDir(builds);
  } finally {
    try {
      if (fs.lstatSync(pending).isSymbolicLink()) fs.unlinkSync(pending);
    } catch (error) { if (error.code !== 'ENOENT') throw error; }
  }
  info(`selected ${releaseId}; running containers require explicit recreation`);
}

async function cleanSource() {
  require(await run(['git', '-C', REPO_ROOT, 'rev-parse', '--show-toplevel'], { capture: true }) === REPO_ROOT, 'build requires repository root');
  require(!await run(['git', '-C', REPO_ROOT, 'status', '--porcelain=v1', '--untracked-files=all'], { capture: true }),
    'deployable build requires clean tracked and untracked source');
  return run(['git', '-C', REPO_ROOT, 'rev-parse', 'HEAD'], { capture: true });
}

function readOctal(buffer, start, length) {
  let end = start;
  while (end < start + length && buffer[end] !== 0 && buffer[end] !== 0x20) end += 1;
  const text = buffer.toString('ascii', start, end).trim();
  if (!text) return 0;
  const value = Number.parseInt(text, 8);
  require(Number.isFinite(value), 'invalid tar header');
  return value;
}

function cString(buffer, start, length) {
  let end = start;
  while (end < start + length && buffer[end] !== 0) end += 1;
  return buffer.toString('utf8', start, end);
}

function headerChecksum(header) {
  let sum = 0;
  for (let index = 0; index < 512; index += 1) sum += index >= 148 && index < 156 ? 0x20 : header[index];
  return sum;
}

function parsePax(buffer) {
  const records = {};
  let offset = 0;
  while (offset < buffer.length) {
    const space = buffer.indexOf(0x20, offset);
    if (space === -1) break;
    const length = Number(buffer.toString('ascii', offset, space));
    if (!Number.isInteger(length) || length <= 0 || offset + length > buffer.length) break;
    const body = buffer.subarray(space + 1, offset + length - 1);
    const eq = body.indexOf(0x3d);
    records[body.subarray(0, eq).toString('utf8')] = body.subarray(eq + 1).toString('utf8');
    offset += length;
  }
  return records;
}

function readTar(buffer) {
  const members = [];
  let offset = 0;
  let global = {};
  let next = null;
  let longName = null;
  while (offset + 512 <= buffer.length) {
    const header = buffer.subarray(offset, offset + 512);
    offset += 512;
    if (header.every((byte) => byte === 0)) break;
    require(headerChecksum(header) === readOctal(header, 148, 8), 'invalid tar header');
    const size = readOctal(header, 124, 12);
    const data = buffer.subarray(offset, offset + size);
    offset += Math.ceil(size / 512) * 512;
    const type = String.fromCharCode(header[156]);
    if (type === 'g') { global = { ...global, ...parsePax(data) }; continue; }
    if (type === 'x') { next = parsePax(data); continue; }
    if (type === 'L') { longName = data.toString('utf8').replace(/\0+$/g, ''); continue; }
    if (type === 'K') { next = { ...(next ?? {}), linkpath: data.toString('utf8').replace(/\0+$/g, '') }; continue; }
    const pax = { ...global, ...(next ?? {}) };
    const prefix = cString(header, 345, 155);
    const rawName = cString(header, 0, 100);
    const name = pax.path || longName || (prefix ? `${prefix}/${rawName}` : rawName);
    const kind = type === '5' ? 'dir' : (type === '0' || type === '\0' ? 'file' : 'other');
    const mode = pax.mode ? Number.parseInt(pax.mode, 8) : readOctal(header, 100, 8);
    members.push({ name, kind, mode, data: kind === 'file' ? Buffer.from(data) : null });
    next = null;
    longName = null;
  }
  return members;
}

function safeParts(name) {
  const normalized = name.replaceAll('\\', '/');
  if (normalized.startsWith('/') || normalized.includes('\0')) return null;
  const parts = normalized.split('/').filter((part) => part !== '' && part !== '.');
  if (parts.some((part) => part === '..')) return null;
  return parts;
}

function extractArchive(buffer, destination) {
  const members = readTar(buffer);
  const planned = [];
  for (const member of members) {
    const parts = safeParts(member.name);
    require(parts && (member.kind === 'file' || member.kind === 'dir'), 'unsupported link or path in source archive');
    const target = path.join(destination, ...parts);
    const relative = path.relative(destination, target);
    require(relative === '' || (!relative.startsWith('..') && !path.isAbsolute(relative)), 'unsupported link or path in source archive');
    planned.push({ ...member, target });
  }
  for (const member of planned) {
    if (member.kind === 'dir') fs.mkdirSync(member.target, { recursive: true });
    else {
      fs.mkdirSync(path.dirname(member.target), { recursive: true });
      fs.writeFileSync(member.target, member.data);
    }
    fs.chmodSync(member.target, (member.mode & 0o777) || (member.kind === 'dir' ? 0o755 : 0o644));
  }
}

async function archiveSource(revision, scratch) {
  const archive = path.join(scratch, 'source.tar');
  const fd = fs.openSync(archive, 'w');
  try { await run(['git', '-C', REPO_ROOT, 'archive', '--format=tar', revision], { stdoutFd: fd }); }
  finally { fs.closeSync(fd); }
  const source = path.join(scratch, 'source');
  fs.mkdirSync(source);
  // Git stores regular files and symlinks only. Reject links and traversal before extraction.
  extractArchive(fs.readFileSync(archive), source);
  return { source, archiveDigest: digest(archive) };
}

function copyFile(from, to) {
  fs.mkdirSync(path.dirname(to), { recursive: true });
  fs.copyFileSync(from, to);
  fs.chmodSync(to, fs.statSync(from).mode & 0o777);
}

function copyTree(from, to) {
  fs.mkdirSync(to, { recursive: true });
  for (const entry of fs.readdirSync(from, { withFileTypes: true })) {
    const source = path.join(from, entry.name);
    const target = path.join(to, entry.name);
    if (entry.isDirectory() && !entry.isSymbolicLink()) copyTree(source, target);
    else if (entry.isFile()) copyFile(source, target);
    else fail(`special file not allowed inside release: ${entry.name}`);
  }
  fs.chmodSync(to, fs.statSync(from).mode & 0o777);
}

async function compileArtifacts(source, output, args, apps, registered) {
  if (args.target === 'all' || args.target === 'backend') {
    const backend = path.join(source, 'backend');
    const profiles = args.env + (args.bundle === 'core' ? ',bundle-core' : '');
    const flag = args.bundle === 'core' ? '-Dmaven.test.skip=true' : '-DskipTests';
    await run(['./mvnw', 'clean', 'package', flag, `-P${profiles}`], { cwd: backend });
    await run(['bash', path.join(source, 'scripts/ci/verify-admin-bundle.sh'), args.bundle], {
      env: { ...process.env, ADMIN_ARTIFACT: path.join(backend, BACKENDS['wta-admin']) },
    });
    for (const [name, relative] of Object.entries(BACKENDS)) {
      const target = path.join(output, 'docker/backend/images', name);
      fs.mkdirSync(target, { recursive: true });
      copyFile(path.join(backend, relative), path.join(target, 'app.jar'));
    }
  }
  if (args.target === 'all' || args.target === 'frontend') {
    const frontend = path.join(source, 'frontend');
    await run(['pnpm', 'install', '--frozen-lockfile', '--network-concurrency=1', '--child-concurrency=1'], { cwd: frontend });
    await run(['pnpm', 'architecture:check'], { cwd: frontend });
    await run(['pnpm', 'build:dependencies'], { cwd: frontend });
    for (const [app, prefix] of Object.entries(apps)) {
      const definition = registered.find((row) => row.id === app);
      const dist = path.join(frontend, 'apps', app, 'dist');
      // No inherited ignored output may make a successful no-op build look complete.
      if (fs.existsSync(dist)) fs.rmSync(dist, { recursive: true, force: true });
      const buildEnv = {
        ...process.env,
        VITE_APP_CONTEXT_PATH: `/${prefix}/`,
        VITE_APP_BASE_API: formatTemplate(definition.apiPath, prefix, args.env),
      };
      if (definition.apiKind === 'sso') buildEnv.VITE_SSO_API = '';
      await run(['pnpm', '--filter', definition.package, `build:${args.env}`], { cwd: frontend, env: buildEnv });
      copyTree(dist, path.join(output, 'docker/frontend/nginx/html', app));
    }
  }
}

async function build(args) {
  const complete = args.target === 'all';
  const revision = complete ? await cleanSource() : null;
  const builds = path.join(RELEASE_ROOT, 'builds');
  fs.mkdirSync(builds, { recursive: true });
  const scratch = fs.mkdtempSync(path.join(builds, '.build-'));
  try {
    const archived = complete ? await archiveSource(revision, scratch) : { source: REPO_ROOT, archiveDigest: null };
    const output = path.join(scratch, 'payload');
    fs.mkdirSync(output);
    const registered = args.target === 'backend' ? [] : appRegistry(path.join(archived.source, 'release-artifacts'), path.join(archived.source, 'frontend'));
    const appNames = registered.map((app) => app.id);
    const apps = args.target === 'backend' ? {} : prefixes(appNames, args.envFile);
    const origins = complete ? appOrigins(registered, args.envFile, args.env) : {};
    const matrix = complete ? applicationMatrix(registered, apps, origins, args.env) : {};
    if (complete) {
      const archivedRelease = path.join(archived.source, 'release-artifacts');
      for (const name of ['docker', 'scripts', 'skills']) copyTree(path.join(archivedRelease, name), path.join(output, name));
      for (const name of ['README.md', '.env.example', 'apps.json']) copyFile(path.join(archivedRelease, name), path.join(output, name));
      validateSql(output);
      for (const app of Object.keys(apps)) require(isFile(path.join(output, `docker/frontend/nginx/apps/nginx-${app}.conf.template`)), `missing nginx template: ${app}`);
    }
    await compileArtifacts(archived.source, output, args, apps, registered);
    if (!complete) {
      const destination = path.join(builds, 'development', `${args.target}-${crypto.randomBytes(16).toString('hex')}`);
      fs.mkdirSync(path.dirname(destination), { recursive: true });
      require(!isSymlink(path.dirname(destination)), 'development output cannot be a symlink');
      fs.renameSync(output, destination);
      info(`development output only; cannot stage: ${destination}`);
      console.log(destination);
      return;
    }
    require(await cleanSource() === revision, 'source changed during build; candidate discarded');
    const metadata = {
      environment: args.env, backendBundle: args.bundle, target: 'all', apps, appOrigins: origins, applicationMatrix: matrix,
      source: {
        revision, tree: await run(['git', '-C', REPO_ROOT, 'rev-parse', `${revision}^{tree}`], { capture: true }),
        archiveSha256: archived.archiveDigest, clean: true,
      },
    };
    for (const category of CATEGORIES) {
      await run(['docker', 'compose', '--env-file', args.envFile, '-f', path.join(output, `docker/docker-compose-${category}.yml`), 'config', '--quiet']);
    }
    const manifest = seal(output, metadata);
    const releaseId = versionId(output, manifest);
    const destination = releasePath(releaseId);
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    // Complete durable payload before exposing it under versions; never overwrite history.
    fsyncTree(output);
    if (fs.existsSync(destination)) verify(destination, args.env);
    else {
      fs.renameSync(output, destination);
      makeReadOnly(destination);
      fsyncTree(destination);
      fsyncDir(path.dirname(destination));
    }
    info(`built immutable release; stage explicitly: ${releaseId}`);
    console.log(releaseId);
  } finally {
    fs.rmSync(scratch, { recursive: true, force: true });
  }
}

function backup() {
  for (const environment of ['dev', 'prod']) {
    const current = path.join(RELEASE_ROOT, 'builds', `current_${environment}`);
    if (!isSymlink(current)) continue;
    const version = resolveRelease(environment);
    const pins = path.join(RELEASE_ROOT, 'builds/pins');
    require(!isSymlink(pins), 'pins cannot be a symlink');
    fs.mkdirSync(pins, { recursive: true });
    fs.symlinkSync(`../versions/${path.basename(version)}`, path.join(pins, `${environment}-${crypto.randomBytes(16).toString('hex')}`));
    fsyncDir(pins);
    info(`pinned ${path.basename(version)}; current unchanged`);
  }
}

function writeOctal(buffer, offset, length, value) {
  const digits = length - 1;
  const text = Math.floor(value).toString(8).padStart(digits, '0');
  require(text.length === digits, 'tar field overflow');
  buffer.write(text, offset, 'ascii');
  buffer[offset + digits] = 0;
}

function applyChecksum(header) {
  header.fill(0x20, 148, 156);
  let sum = 0;
  for (const byte of header) sum += byte;
  const text = sum.toString(8).padStart(6, '0');
  header.write(text, 148, 'ascii');
  header[154] = 0;
  header[155] = 0x20;
}

function tarHeader({ name, mode, size, type, target = '' }) {
  const header = Buffer.alloc(512);
  Buffer.from(name).subarray(0, 100).copy(header, 0);
  writeOctal(header, 100, 8, mode);
  writeOctal(header, 108, 8, 0);
  writeOctal(header, 116, 8, 0);
  writeOctal(header, 124, 12, size);
  writeOctal(header, 136, 12, 0);
  header[156] = type.charCodeAt(0);
  if (target) Buffer.from(target).subarray(0, 100).copy(header, 157);
  header.write('ustar', 257, 'ascii');
  header[262] = 0;
  header.write('00', 263, 'ascii');
  applyChecksum(header);
  return header;
}

function paxRecord(key, value) {
  const rest = ` ${key}=${value}\n`;
  let digits = 1;
  for (;;) {
    const length = Buffer.byteLength(rest) + digits;
    if (String(length).length === digits) return Buffer.from(`${length}${rest}`);
    digits = String(length).length;
  }
}

function pad512(buffer) {
  const extra = (512 - (buffer.length % 512)) % 512;
  return extra === 0 ? buffer : Buffer.concat([buffer, Buffer.alloc(extra)]);
}

function addTree(root, arcname, entries) {
  const stat = fs.lstatSync(root);
  if (stat.isSymbolicLink()) {
    entries.push({ type: '2', name: arcname, mode: 0o777, target: fs.readlinkSync(root) });
    return;
  }
  if (stat.isDirectory()) {
    entries.push({ type: '5', name: arcname.endsWith('/') ? arcname : `${arcname}/`, mode: (stat.mode & 0o777) || 0o755 });
    for (const name of fs.readdirSync(root).sort()) addTree(path.join(root, name), `${arcname}/${name}`, entries);
    return;
  }
  require(stat.isFile(), `special file not allowed inside bundle: ${arcname}`);
  entries.push({ type: '0', name: arcname, mode: (stat.mode & 0o777) || 0o644, data: fs.readFileSync(root) });
}

function writeTar(entries) {
  const chunks = [];
  for (const entry of entries) {
    const records = [];
    if (Buffer.byteLength(entry.name) > 100) records.push(['path', entry.name]);
    if (entry.target && Buffer.byteLength(entry.target) > 100) records.push(['linkpath', entry.target]);
    if (records.length > 0) {
      const body = Buffer.concat(records.map(([key, value]) => paxRecord(key, value)));
      chunks.push(tarHeader({ name: 'PaxHeaders.0/member', mode: 0o644, size: body.length, type: 'x' }));
      chunks.push(pad512(body));
    }
    const data = entry.data ?? Buffer.alloc(0);
    chunks.push(tarHeader({
      name: entry.name, mode: entry.mode, size: entry.type === '0' ? data.length : 0, type: entry.type, target: entry.target,
    }));
    if (entry.type === '0') chunks.push(pad512(data));
  }
  chunks.push(Buffer.alloc(1024));
  return Buffer.concat(chunks);
}

function bundle(environment) {
  const version = resolveRelease(environment);
  const bundles = path.join(RELEASE_ROOT, 'bundles');
  require(!isSymlink(bundles), 'bundles cannot be a symlink');
  fs.mkdirSync(bundles, { recursive: true });
  const scratch = fs.mkdtempSync(path.join(bundles, '.bundle-'));
  try {
    const packageDir = path.join(scratch, 'namewta');
    fs.mkdirSync(path.join(packageDir, 'builds/versions'), { recursive: true });
    for (const name of ['scripts', 'skills']) copyTree(path.join(version, name), path.join(packageDir, name));
    for (const name of ['README.md', '.env.example']) copyFile(path.join(version, name), path.join(packageDir, name));
    fs.symlinkSync(`versions/${path.basename(version)}`, path.join(packageDir, 'builds', `current_${environment}`));
    const entries = [];
    addTree(packageDir, 'namewta', entries);
    addTree(version, `namewta/builds/versions/${path.basename(version)}`, entries);
    const target = path.join(scratch, 'bundle.tar.gz');
    fs.writeFileSync(target, zlib.gzipSync(writeTar(entries)));
    fsyncFile(target);
    // Never clobber an earlier bundle, even of the same release.
    const destination = path.join(bundles, `${path.basename(version)}-${crypto.randomBytes(16).toString('hex')}.tar.gz`);
    fs.renameSync(target, destination);
    fsyncDir(bundles);
    console.log(destination);
  } finally {
    fs.rmSync(scratch, { recursive: true, force: true });
  }
}

function parseOptions(tokens) {
  const options = {};
  for (let index = 0; index < tokens.length; index += 1) {
    const token = tokens[index];
    require(token.startsWith('--') && token.length > 2, `unexpected argument: ${token}`);
    const eq = token.indexOf('=');
    let key;
    let value;
    if (eq !== -1) {
      key = token.slice(2, eq);
      value = token.slice(eq + 1);
    } else {
      key = token.slice(2);
      value = tokens[index + 1];
      require(value !== undefined && !value.startsWith('--'), `missing value for --${key}`);
      index += 1;
    }
    options[key] = value;
  }
  return options;
}

function expectOnly(options, keys) {
  for (const key of Object.keys(options)) require(keys.includes(key), `unexpected argument: --${key}`);
}

function choice(value, allowed, label) {
  require(allowed.includes(value), `invalid ${label}: ${value}`);
  return value;
}

async function main() {
  const [action, ...rest] = process.argv.slice(2);
  require(action, 'missing release action');
  const options = parseOptions(rest);
  if (action === 'stage-mysql') {
    expectOnly(options, []);
    validateSql(RELEASE_ROOT);
    info('MySQL baseline verified read-only; six canonical SQL files unchanged');
    return;
  }
  if (action === 'check-apps') {
    expectOnly(options, []);
    const registered = appRegistry(RELEASE_ROOT, path.join(REPO_ROOT, 'frontend'));
    info(`explicit shipped App registration verified: ${registered.map((app) => app.id).join(', ')}`);
    return;
  }
  if (action === 'resolve') {
    expectOnly(options, ['env', 'env-file']);
    require(options.env, 'missing required arguments');
    console.log(resolveRelease(choice(options.env, ['dev', 'prod'], 'env'), options['env-file'] ? path.resolve(options['env-file']) : null));
    return;
  }
  await withLock(async () => {
    if (action === 'build') {
      expectOnly(options, ['target', 'env', 'bundle', 'env-file']);
      await build({
        target: choice(options.target ?? 'all', ['all', 'frontend', 'backend'], 'target'),
        env: choice(options.env ?? 'prod', ['dev', 'prod'], 'env'),
        bundle: choice(options.bundle ?? 'full', ['full', 'core'], 'bundle'),
        envFile: path.resolve(options['env-file'] ?? path.join(RELEASE_ROOT, '.env')),
      });
    } else if (action === 'stage') {
      expectOnly(options, ['env', 'release']);
      require(options.env && options.release, 'missing required arguments');
      await stage(choice(options.env, ['dev', 'prod'], 'env'), options.release);
    } else if (action === 'backup') {
      expectOnly(options, []);
      backup();
    } else if (action === 'bundle') {
      expectOnly(options, ['env']);
      require(options.env, 'missing required arguments');
      bundle(choice(options.env, ['dev', 'prod'], 'env'));
    } else fail(`unknown release action: ${action}`);
  });
}

if (process.argv[1] && fs.existsSync(process.argv[1]) && fs.realpathSync(process.argv[1]) === SCRIPT_PATH) {
  process.on('SIGTERM', () => onSignal('SIGTERM'));
  process.on('SIGINT', () => onSignal('SIGINT'));
  main().catch((error) => {
    console.error(`[ERROR] ${error.userFacing || error.code ? error.message : (error.stack || error)}`);
    process.exit(1);
  });
}
