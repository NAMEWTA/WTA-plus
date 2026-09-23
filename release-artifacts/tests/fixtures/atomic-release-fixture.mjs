import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const releaseRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repoRoot = path.dirname(releaseRoot);
function thaw(directory) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const file = path.join(directory, entry.name);
    if (entry.isSymbolicLink()) continue;
    fs.chmodSync(file, entry.isDirectory() ? 0o755 : 0o644);
    if (entry.isDirectory()) thaw(file);
  }
  fs.chmodSync(directory, 0o755);
}

const sha = (value) => crypto.createHash('sha256').update(value).digest('hex');
const write = (file, text, mode) => {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, text, mode ? { mode } : undefined);
};

// Real Git archive / Bash entry / Node state machine / jar verifier; compilers and Docker
// config are bounded stubs. These synthetic fixture commits never alter the project history.
export function fixture() {
  const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'wta-release-contract-'));
  const root = path.join(scratch, 'repo');
  const release = path.join(root, 'release-artifacts');
  fs.mkdirSync(release, { recursive: true });
  for (const name of ['scripts', 'docker', 'skills']) {
    fs.cpSync(path.join(releaseRoot, name), path.join(release, name), { recursive: true,
      filter: (source) => !source.includes('/__pycache__') && !source.endsWith('/app.jar') });
  }
  for (const name of ['README.md', '.env.example', 'apps.json']) fs.copyFileSync(path.join(releaseRoot, name), path.join(release, name));
  write(path.join(root, '.gitignore'), 'release-artifacts/builds/\nrelease-artifacts/bundles/\nrelease-artifacts/.env\n**/target/\n**/dist/\n**/__pycache__/\n');
  write(path.join(root, 'marker.txt'), 'version A');
  fs.cpSync(path.join(repoRoot, 'scripts/ci/verify-admin-bundle.sh'), path.join(root, 'scripts/ci/verify-admin-bundle.sh'), { recursive: true });
  for (const app of ['admin-web', 'home-web', 'sso-web']) write(path.join(root, 'frontend/apps', app, 'package.json'), JSON.stringify({ name: '@namewta/' + app, scripts: { 'build:dev': 'vite', 'build:prod': 'vite' } }));
  write(path.join(release, '.env'), 'ADMIN_WEB_PREFIX=admin-app\nHOME_WEB_PREFIX=home-app\nSSO_WEB_PREFIX=sso-app\nADMIN_WEB_ORIGIN=https://localhost:4441\nHOME_WEB_ORIGIN=https://localhost:4442\nSSO_WEB_ORIGIN=https://sso.localhost:4443\nRUNTIME_CANARY=must-not-be-packaged\n');
  write(path.join(root, 'backend/mvnw'), `#!/usr/bin/env node
const fs = require('node:fs');
const path = require('node:path');
const { crc32 } = require('node:zlib');
const root = process.cwd();
if (process.env.BUILD_WAIT) {
  fs.writeFileSync(process.env.BUILD_WAIT, String(process.pid));
  const pause = new Int32Array(new SharedArrayBuffer(4));
  while (!fs.existsSync(process.env.BUILD_WAIT + '.continue')) Atomics.wait(pause, 0, 0, 20);
}
const marker = fs.readFileSync(path.resolve(root, '../marker.txt'));
const base = ['wta-system','wta-common-notify','wta-common-oss','wta-third','wta-sso','wta-notify','wta-profile-person','wta-profile-enterprise'];
const extra = process.argv.some((value) => value.includes('bundle-core')) ? [] : ['wta-job','wta-ai','wta-common-ai','wta-demo','wta-workflow'];
function zipStore(entries) {
  const locals = [];
  const centrals = [];
  let offset = 0;
  for (const [name, data] of entries) {
    const bytes = Buffer.isBuffer(data) ? data : Buffer.from(data);
    const crc = crc32(bytes) >>> 0;
    const nameBytes = Buffer.from(name);
    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt16LE(0, 8);
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(bytes.length, 18);
    local.writeUInt32LE(bytes.length, 22);
    local.writeUInt16LE(nameBytes.length, 26);
    const central = Buffer.alloc(46);
    central.writeUInt32LE(0x02014b50, 0);
    central.writeUInt16LE(20, 4);
    central.writeUInt16LE(20, 6);
    central.writeUInt16LE(0, 10);
    central.writeUInt32LE(crc, 16);
    central.writeUInt32LE(bytes.length, 20);
    central.writeUInt32LE(bytes.length, 24);
    central.writeUInt16LE(nameBytes.length, 28);
    central.writeUInt32LE(offset, 42);
    locals.push(Buffer.concat([local, nameBytes, bytes]));
    centrals.push(Buffer.concat([central, nameBytes]));
    offset += local.length + nameBytes.length + bytes.length;
  }
  const centralBuf = Buffer.concat(centrals);
  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(0x06054b50, 0);
  eocd.writeUInt16LE(entries.length, 8);
  eocd.writeUInt16LE(entries.length, 10);
  eocd.writeUInt32LE(centralBuf.length, 12);
  eocd.writeUInt32LE(offset, 16);
  return Buffer.concat([...locals, centralBuf, eocd]);
}
for (const name of ['wta-admin','wta-monitor-admin','wta-snailjob-server']) {
  const jar = path.join(root, name === 'wta-admin' ? '' : 'wta-extend', name, 'target', name + '.jar');
  fs.mkdirSync(path.dirname(jar), { recursive: true });
  const entries = [
    ['META-INF/MANIFEST.MF', 'Main-Class: org.springframework.boot.loader.launch.JarLauncher\\nStart-Class: fixture.Main\\n'],
    ['BOOT-INF/classes/marker.txt', marker],
  ];
  if (name === 'wta-admin') for (const lib of base.concat(extra)) entries.push(['BOOT-INF/lib/' + lib + '-1.0.jar', 'fixture']);
  fs.writeFileSync(jar, zipStore(entries));
}
`, 0o755);
  const bin = path.join(scratch, 'bin');
  write(path.join(bin, 'pnpm'), `#!/usr/bin/env node
const fs = require('node:fs');
const path = require('node:path');
const argv = process.argv.slice(2);
if (argv.includes('--filter')) {
  const app = argv[argv.indexOf('--filter') + 1].split('/').at(-1);
  const dist = path.join('apps', app, 'dist');
  fs.mkdirSync(dist, { recursive: true });
  fs.writeFileSync(path.join(dist, 'index.html'), fs.readFileSync(path.resolve(process.cwd(), '../marker.txt'), 'utf8'));
  fs.writeFileSync(path.join(dist, 'build-mode.json'), JSON.stringify({ app, mode: argv.at(-1) === 'build:prod' ? 'production' : 'development' }));
}
`, 0o755);
  write(path.join(bin, 'docker'), `#!/usr/bin/env node
const fs = require('node:fs');
const path = require('node:path');
const argv = process.argv.slice(2);
if (process.env.DOCKER_CALLS) {
  fs.appendFileSync(process.env.DOCKER_CALLS, JSON.stringify({
    args: argv,
    image: process.env.NAMEWTA_ADMIN_IMAGE ?? null,
    data: process.env.NAMEWTA_DATA_ROOT ?? null,
    cors: process.env.WEB_CORS_ALLOWED_ORIGINS ?? null,
    sso: process.env.SSO_WEB_ORIGIN ?? null,
    ssoBase: process.env.SSO_WEB_BASE_PATH ?? null,
  }) + '\\n');
}
// Switch the pointer between Compose calls to prove the manager pins one resolved version.
if (argv.includes('-f') && process.env.SWITCH_TO) {
  const current = process.env.CURRENT_POINTER;
  if (path.basename(fs.readlinkSync(current)) !== process.env.SWITCH_TO) {
    const tmp = path.join(path.dirname(current), '.test-pointer');
    fs.symlinkSync('versions/' + process.env.SWITCH_TO, tmp);
    fs.renameSync(tmp, current);
  }
}
console.log(argv.join(' '));
`, 0o755);
  const env = { ...process.env, PATH: bin + ':' + process.env.PATH };
  // Avoid unrelated host prefix/image overrides affecting isolated fixtures.
  for (const key of Object.keys(env)) if (key.endsWith('_PREFIX')) delete env[key];
  const git = (...args) => execFileSync('git', ['-C', root, ...args], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
  git('init', '-q'); git('config', 'user.name', 'release fixture'); git('config', 'user.email', 'fixture@example.invalid');
  const commit = () => { git('add', '.'); git('-c', 'core.hooksPath=/dev/null', 'commit', '-qm', 'synthetic release fixture'); return git('rev-parse', 'HEAD'); };
  commit();
  const cli = (...args) => spawnSync('bash', [path.join(release, 'scripts/release-manage.sh'), ...args], { env, encoding: 'utf8' });
  const ok = (...args) => { const result = cli(...args); assert.equal(result.status, 0, result.stderr); return result.stdout.trim().split('\n').at(-1); };
  const build = () => ok('build', '--target', 'all', '--env', 'prod');
  const stage = (id) => ok('stage', '--env', 'prod', '--release', id);
  const version = (id) => path.join(release, 'builds/versions', id);
  const current = path.join(release, 'builds/current_prod');
  const dispose = () => { thaw(scratch); fs.rmSync(scratch, { recursive: true, force: true }); };
  return { scratch, root, release, env, git, commit, cli, ok, build, stage, version, current, dispose };
}

export function snapshot(root) {
  const found = {};
  const walk = (directory) => {
    for (const name of fs.readdirSync(directory).sort()) {
      const file = path.join(directory, name), stat = fs.lstatSync(file);
      if (stat.isSymbolicLink()) found[path.relative(root, file)] = 'link:' + fs.readlinkSync(file);
      else if (stat.isDirectory()) walk(file);
      else found[path.relative(root, file)] = sha(fs.readFileSync(file));
    }
  };
  walk(root);
  return found;
}
