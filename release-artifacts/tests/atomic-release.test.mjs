import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync, spawn, spawnSync } from 'node:child_process';

import { fixture, snapshot } from './fixtures/atomic-release-fixture.mjs';
const sha = (value) => crypto.createHash('sha256').update(value).digest('hex');
const write = (file, text) => {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, text);
};

function reseal(f, id, mutate) {
  const original = f.version(id);
  const scratch = path.join(f.release, 'builds/corruption-fixture');
  fs.cpSync(original, scratch, { recursive: true });
  // Explicit fault injection in an owned fixture, preserving executable modes.
  for (const [name] of Object.entries(snapshot(scratch))) {
    const file = path.join(scratch, name); fs.chmodSync(file, fs.statSync(file).mode | 0o200);
  }
  for (const entry of fs.globSync('**', { cwd: scratch, withFileTypes: true })) {
    if (entry.isDirectory()) fs.chmodSync(path.join(entry.parentPath, entry.name), 0o755);
  }
  fs.chmodSync(scratch, 0o755);
  const manifestFile = path.join(scratch, 'release-manifest.json');
  const manifest = JSON.parse(fs.readFileSync(manifestFile));
  mutate(scratch, manifest);
  fs.writeFileSync(manifestFile, JSON.stringify(manifest));
  const next = 'prod-' + manifest.source.revision.slice(0, 12) + '-' + sha(fs.readFileSync(manifestFile));
  const sealPermissions = (directory) => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const file = path.join(directory, entry.name);
      if (entry.isSymbolicLink()) continue;
      if (entry.isDirectory()) sealPermissions(file);
      else fs.chmodSync(file, fs.statSync(file).mode & ~0o222);
    }
    fs.chmodSync(directory, 0o555);
  };
  fs.renameSync(scratch, f.version(next));
  sealPermissions(f.version(next));
  return next;
}

test('complete CLI build proves one archived revision, leaves current unchanged, stages and restores history', () => {
  const f = fixture();
  try {
    write(path.join(f.root, 'frontend/apps/admin-web/dist/index.html'), 'ignored stale output');
    write(path.join(f.root, 'backend/wta-admin/target/wta-admin.jar'), 'ignored stale jar');
    const a = f.build();
    assert.equal(fs.readFileSync(path.join(f.version(a), 'docker/frontend/nginx/html/admin-web/index.html'), 'utf8'), 'version A');
    assert.equal(fs.existsSync(f.current), false);
    const manifest = JSON.parse(fs.readFileSync(path.join(f.version(a), 'release-manifest.json')));
    assert.equal(manifest.source.revision, f.git('rev-parse', 'HEAD'));
    for (const row of manifest.files) {
      assert.equal(row.sourceRevision, manifest.source.revision);
      assert.equal(sha(fs.readFileSync(path.join(f.version(a), row.path))), row.sha256);
    }
    assert.equal(manifest.target, 'all');
    assert.equal(manifest.source.clean, true);
    assert.equal(fs.statSync(f.version(a)).mode & 0o222, 0);
    f.stage(a);
    const previous = snapshot(f.version(a));
    write(path.join(f.root, 'marker.txt'), 'version B'); f.commit();
    const b = f.build();
    assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
    f.stage(b);
    assert.equal(fs.readFileSync(path.join(f.current, 'docker/frontend/nginx/html/admin-web/index.html'), 'utf8'), 'version B');
    f.stage(a);
    assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
    assert.deepEqual(snapshot(f.version(a)), previous);
    assert.ok(fs.existsSync(f.version(b)));
    f.ok('backup');
    assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
    assert.equal(fs.readdirSync(path.join(f.release, 'builds/pins')).length, 1);
  } finally { f.dispose(); }
});

test('dirty/untracked source and inherited ignored output cannot become a deployable release', () => {
  const f = fixture();
  try {
    const a = f.build(); f.stage(a);
    write(path.join(f.root, 'untracked.txt'), 'new');
    let result = f.cli('build', '--target', 'all');
    assert.notEqual(result.status, 0); assert.match(result.stderr, /clean tracked and untracked/);
    fs.unlinkSync(path.join(f.root, 'untracked.txt'));
    write(path.join(f.root, 'marker.txt'), 'dirty');
    result = f.cli('build', '--target', 'all');
    assert.notEqual(result.status, 0);
    assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
    const development = f.ok('build', '--target', 'backend');
    assert.match(development, /builds\/development\/backend-/);
    assert.equal(fs.existsSync(path.join(development, 'release-manifest.json')), false);
    assert.notEqual(f.cli('stage', '--env', 'prod', '--release', path.basename(development)).status, 0);
    assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
  } finally { f.dispose(); }
});

for (const fault of ['missing-template', 'bad-sql', 'bad-jar', 'mixed-source', 'partial', 'symlink', 'extra-file']) {
  test(`stage rejects ${fault} without changing current, history, or mutable Docker contexts`, () => {
    const f = fixture();
    try {
      const a = f.build(); f.stage(a);
      const contexts = snapshot(path.join(f.release, 'docker'));
      const previous = snapshot(f.version(a));
      const bad = reseal(f, a, (directory, manifest) => {
        let relative;
        if (fault === 'missing-template') relative = 'docker/frontend/nginx/apps/nginx-home-web.conf.template';
        if (fault === 'bad-sql') relative = 'docker/infrastructure/mysql/init/40-cde-ai.sql';
        if (fault === 'bad-jar') relative = 'docker/backend/images/wta-admin/app.jar';
        if (relative) {
          const target = path.join(directory, relative);
          if (fault === 'missing-template') {
            fs.unlinkSync(target); manifest.files = manifest.files.filter((row) => row.path !== relative);
          } else {
            fs.writeFileSync(target, fault === 'bad-sql' ? '' : 'not a zip');
            const row = manifest.files.find((row) => row.path === relative);
            row.sha256 = sha(fs.readFileSync(target)); row.size = fs.statSync(target).size;
          }
        }
        if (fault === 'mixed-source') manifest.files[0].sourceRevision = 'f'.repeat(40);
        if (fault === 'partial') manifest.target = 'frontend';
        if (fault === 'symlink') {
          const target = path.join(directory, manifest.files[0].path); fs.unlinkSync(target); fs.symlinkSync('/etc/hosts', target);
        }
        if (fault === 'extra-file') write(path.join(directory, 'unlisted.txt'), 'unexpected');
      });
      const result = f.cli('stage', '--env', 'prod', '--release', bad);
      assert.notEqual(result.status, 0, result.stderr);
      const expectedError = { 'missing-template': /missing nginx template/, 'bad-sql': /empty or truncated SQL/,
        'bad-jar': /not a zip/, 'mixed-source': /mixed source/, partial: /only clean complete builds/,
        symlink: /symlink not allowed/, 'extra-file': /inventory changed/ };
      assert.match(result.stderr, expectedError[fault]);
      assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
      assert.deepEqual(snapshot(path.join(f.release, 'docker')), contexts);
      assert.deepEqual(snapshot(f.version(a)), previous);
    } finally { f.dispose(); }
  });
}

test('stage detects raw digest changes and refuses legacy current directories and escaped version paths', () => {
  const f = fixture();
  try {
    const a = f.build();
    const html = path.join(f.version(a), 'docker/frontend/nginx/html/admin-web/index.html');
    fs.chmodSync(html, 0o644); write(html, 'tampered'); fs.chmodSync(html, 0o444);
    assert.match(f.cli('stage', '--env', 'prod', '--release', a).stderr, /digest mismatch/);
    assert.notEqual(f.cli('stage', '--env', 'prod', '--release', '../foreign').status, 0);
    write(path.join(f.root, 'marker.txt'), 'version B'); f.commit();
    const b = f.build();
    fs.mkdirSync(f.current); write(path.join(f.current, 'owned-by-other'), 'keep');
    assert.notEqual(f.cli('stage', '--env', 'prod', '--release', b).status, 0);
    assert.equal(fs.readFileSync(path.join(f.current, 'owned-by-other'), 'utf8'), 'keep');
  } finally { f.dispose(); }
});

const delay = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));
async function waitFile(file, child) {
  for (let i = 0; i < 500; i++) {
    if (fs.existsSync(file)) return;
    assert.equal(child.exitCode, null, 'build exited before fault seam');
    await delay(20);
  }
  assert.fail('build did not reach controlled fixture compiler');
}

test('interruption kills the owned compiler, keeps other scratch/history, releases lock, and allows retry', async () => {
  const f = fixture(); let child;
  try {
    const a = f.build(); f.stage(a);
    const marker = path.join(f.scratch, 'waiting');
    const foreign = path.join(f.release, 'builds/.build-not-owned/keep'); write(foreign, 'keep');
    child = spawn('bash', [path.join(f.release, 'scripts/release-manage.sh'), 'build', '--target', 'all'], {
      env: { ...f.env, BUILD_WAIT: marker }, stdio: ['ignore', 'ignore', 'pipe'],
    });
    let stderr = ''; child.stderr.on('data', (chunk) => { stderr += chunk; });
    const ended = new Promise((resolve) => child.on('exit', resolve));
    await waitFile(marker, child);
    const contending = f.cli('build', '--target', 'all');
    assert.notEqual(contending.status, 0); assert.match(contending.stderr, /owns the lock/);
    child.kill('SIGTERM'); await ended;
    assert.notEqual(child.exitCode, 0); assert.match(stderr, /interrupted/);
    assert.throws(() => process.kill(Number(fs.readFileSync(marker)), 0), { code: 'ESRCH' });
    assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
    assert.equal(fs.readFileSync(foreign, 'utf8'), 'keep');
    assert.deepEqual(fs.readdirSync(path.join(f.release, 'builds')).filter((p) => p.startsWith('.build-')), ['.build-not-owned']);
    f.build();
  } finally { if (child && child.exitCode === null) child.kill('SIGTERM'); f.dispose(); }
});

test('the archive remains the compiler input during a transient edit restored before build end', async () => {
  const f = fixture(); let child;
  try {
    const marker = path.join(f.scratch, 'waiting');
    child = spawn('bash', [path.join(f.release, 'scripts/release-manage.sh'), 'build', '--target', 'all'], {
      env: { ...f.env, BUILD_WAIT: marker }, stdio: ['ignore', 'pipe', 'pipe'],
    });
    let output = '', errors = '';
    child.stdout.on('data', (chunk) => { output += chunk; }); child.stderr.on('data', (chunk) => { errors += chunk; });
    const ended = new Promise((resolve) => child.on('exit', resolve));
    await waitFile(marker, child);
    write(path.join(f.root, 'marker.txt'), 'transient workspace mutation');
    write(marker + '.continue', 'go');
    // Read the fixture compiler's archived input path via /proc before allowing completion.
    const pid = Number(fs.readFileSync(marker));
    if (fs.existsSync('/proc/' + pid + '/cwd')) assert.match(fs.readlinkSync('/proc/' + pid + '/cwd'), /\.build-.*\/source\/backend/);
    write(path.join(f.root, 'marker.txt'), 'version A');
    await ended;
    assert.equal(child.exitCode, 0, errors);
    const id = output.trim().split('\n').at(-1);
    assert.equal(fs.readFileSync(path.join(f.version(id), 'docker/frontend/nginx/html/admin-web/index.html'), 'utf8'), 'version A');
  } finally { if (child && child.exitCode === null) child.kill('SIGTERM'); f.dispose(); }
});

test('Docker consumer pins one version across all categories, forwards logs, and explicitly rebuilds/recreates', () => {
  const f = fixture();
  try {
    const a = f.build(); f.stage(a);
    write(path.join(f.root, 'marker.txt'), 'version B'); f.commit(); const b = f.build();
    const calls = path.join(f.scratch, 'docker-calls.jsonl');
    const manager = (...args) => spawnSync('bash', [path.join(f.release, 'scripts/docker-manage.sh'), ...args], {
      encoding: 'utf8', env: { ...f.env, DOCKER_CALLS: calls, SWITCH_TO: b, CURRENT_POINTER: f.current },
    });
    let result = manager('up', 'all', '--nacos'); assert.equal(result.status, 0, result.stderr);
    let records = fs.readFileSync(calls, 'utf8').trim().split('\n').map(JSON.parse).filter((r) => r.args.includes('-f'));
    assert.equal(records.length, 4);
    for (const record of records) {
      assert.ok(record.args[record.args.indexOf('-f') + 1].startsWith(f.version(a) + '/docker/'));
      assert.ok(record.args.includes('--force-recreate') && record.args.includes('--build'));
      assert.equal(record.image, 'namewta/namewta-admin:' + a);
      assert.equal(record.data, path.join(f.release, 'docker/runtime'));
      assert.equal(record.cors, 'https://localhost:4441,https://localhost:4442,https://sso.localhost:4443');
      assert.equal(record.sso, 'https://sso.localhost:4443');
      assert.equal(record.ssoBase, '/sso-app/');
    }
    const backend = records.find((r) => r.args.includes('namewta-backend'));
    assert.ok(backend.args.includes(path.join(f.version(a), 'docker/overrides/nacos-enabled.yml')));
    assert.equal(fs.readlinkSync(f.current), 'versions/' + b);
    result = manager('logs', 'backend', 'namewta-server1'); assert.equal(result.status, 0, result.stderr);
    assert.match(result.stdout, /logs -f --tail 200 namewta-server1/);
    write(path.join(f.release, '.env'), 'ADMIN_WEB_PREFIX=admin-app\nHOME_WEB_PREFIX=home-app\nSSO_WEB_PREFIX=sso-app\nADMIN_WEB_ORIGIN=https://localhost:4441\nHOME_WEB_ORIGIN=https://localhost:4442\nSSO_WEB_ORIGIN=https://sso.localhost:4443\nNAMEWTA_DATA_ROOT="runtime with spaces"\n');
    result = manager('config', 'frontend'); assert.equal(result.status, 0, result.stderr);
    const last = fs.readFileSync(calls, 'utf8').trim().split('\n').map(JSON.parse).at(-1);
    assert.equal(last.data, path.join(f.release, 'docker/runtime with spaces'));
    write(path.join(f.release, '.env'), 'ADMIN_WEB_PREFIX=changed\nHOME_WEB_PREFIX=home-app\nSSO_WEB_PREFIX=sso-app\nADMIN_WEB_ORIGIN=https://localhost:4441\nHOME_WEB_ORIGIN=https://localhost:4442\nSSO_WEB_ORIGIN=https://sso.localhost:4443\n');
    result = manager('up', 'frontend'); assert.notEqual(result.status, 0);
    assert.match(result.stderr, /runtime prefixes differ/);
  } finally { f.dispose(); }
});

test('bundle contains only the verified selected release; runtime credentials and old contexts stay out', () => {
  const f = fixture();
  try {
    const a = f.build(); f.stage(a);
    write(path.join(f.release, 'docker/frontend/nginx/html/admin-web/old-canary.txt'), 'must-not-be-packaged');
    const archive = f.ok('bundle', '--env', 'prod');
    const extracted = path.join(f.scratch, 'extracted'); fs.mkdirSync(extracted);
    execFileSync('tar', ['-xzf', archive, '-C', extracted]);
    assert.equal(fs.existsSync(path.join(extracted, 'namewta/.env')), false);
    assert.equal(fs.existsSync(path.join(extracted, 'namewta/docker')), false);
    const stored = path.join(extracted, 'namewta/builds/versions', a);
    assert.deepEqual(snapshot(stored), snapshot(f.version(a)));
    const result = spawnSync('bash', [path.join(extracted, 'namewta/scripts/release-manage.sh'), 'resolve', '--env', 'prod'], { encoding: 'utf8' });
    assert.equal(result.status, 0, result.stderr);
    assert.equal(result.stdout.trim(), stored);
    assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
  } finally { f.dispose(); }
});

test('interruption immediately before the one pointer rename preserves current and clears only its pending link', async () => {
  const f = fixture(); let child;
  try {
    const a = f.build(); f.stage(a);
    write(path.join(f.root, 'marker.txt'), 'version B'); f.commit(); const b = f.build();
    const before = snapshot(f.version(a));
    const contexts = snapshot(path.join(f.release, 'docker'));
    const marker = path.join(f.scratch, 'rename-wait');
    const wrapper = `import os,runpy,sys,time
from pathlib import Path
marker=sys.argv[1]
def hold(source,target):
 Path(marker).write_text(str(source))
 while True:time.sleep(.02)
os.replace=hold
script=sys.argv[2];sys.argv=[script,'stage','--env','prod','--release',sys.argv[3]]
runpy.run_path(script,run_name='__main__')
`;
    // Fault injection wraps only the OS rename seam; the production CLI parser/lock/verify/cleanup run unchanged.
    child = spawn('python3', ['-c', wrapper, marker, path.join(f.release, 'scripts/release-state.py'), b], { stdio: 'ignore' });
    const ended = new Promise((resolve) => child.on('exit', resolve));
    await waitFile(marker, child);
    assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
    const pending = fs.readFileSync(marker, 'utf8'); assert.ok(fs.lstatSync(pending).isSymbolicLink());
    child.kill('SIGTERM'); await ended;
    assert.notEqual(child.exitCode, 0);
    assert.equal(fs.readlinkSync(f.current), 'versions/' + a);
    assert.equal(fs.existsSync(pending), false);
    assert.deepEqual(snapshot(f.version(a)), before);
    assert.deepEqual(snapshot(path.join(f.release, 'docker')), contexts);
    f.stage(b);
    assert.equal(fs.readlinkSync(f.current), 'versions/' + b);
  } finally { if (child && child.exitCode === null) child.kill('SIGTERM'); f.dispose(); }
});

test('the dev/core branch preserves bundle selection and rejects a production pointer mismatch', () => {
  const f = fixture();
  try {
    const id = f.ok('build', '--target', 'all', '--env', 'dev', '--bundle', 'core');
    const manifest = JSON.parse(fs.readFileSync(path.join(f.version(id), 'release-manifest.json')));
    assert.equal(manifest.backendBundle, 'core');
    assert.equal(manifest.environment, 'dev');
    assert.match(f.cli('stage', '--env', 'prod', '--release', id).stderr, /environment mismatch/);
    f.ok('stage', '--env', 'dev', '--release', id);
    assert.equal(fs.readlinkSync(path.join(f.release, 'builds/current_dev')), 'versions/' + id);
  } finally { f.dispose(); }
});
