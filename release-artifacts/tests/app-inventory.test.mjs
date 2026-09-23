import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

test('every shipped App has a complete explicit release registration', () => {
  const registry = JSON.parse(fs.readFileSync(path.join(root, 'apps.json')));
  assert.deepEqual(registry.apps.filter((app) => app.shipped).map((app) => app.id).sort(), ['admin-web', 'home-web', 'sso-web']);
  const result = spawnSync('bash', [path.join(root, 'scripts/release-manage.sh'), 'check-apps'], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
});

test('SSO has its own ingress and proxies the real same-origin SSO API namespace', () => {
  const registry = JSON.parse(fs.readFileSync(path.join(root, 'apps.json')));
  const sso = registry.apps.find((app) => app.id === 'sso-web');
  assert.equal(sso.ingress, 'dedicated');
  const template = fs.readFileSync(path.join(root, sso.nginxTemplate), 'utf8');
  assert.match(template, /location \^~ \/sso\//);
  assert.match(template, /location = \/healthz/);
  const compose = fs.readFileSync(path.join(root, 'docker/docker-compose-frontend.yml'), 'utf8');
  assert.match(compose, /^  namewta-nginx-sso-web:$/m);
  assert.match(compose, /^  namewta-nginx-sso-web-tls:$/m);
  for (const name of ['nginx-lb-http.conf.template', 'nginx-lb-tls.conf.template']) {
    const lb = fs.readFileSync(path.join(root, 'docker/frontend/nginx/lb', name), 'utf8');
    assert.doesNotMatch(lb, /app_sso_web|APP_SSO_WEB_PREFIX/);
  }
});

// The compilers are controlled fixtures; release validation, source snapshots and filesystem state are real.
const { fixture, snapshot } = await import('./fixtures/atomic-release-fixture.mjs');
const scenarios = [
  ['unregistered App', /unregistered or missing frontend App/, (f) => {
    const app = path.join(f.root, 'frontend/apps/rogue-web'); fs.mkdirSync(app);
    fs.writeFileSync(path.join(app, 'package.json'), JSON.stringify({ name: '@namewta/rogue-web' })); f.commit();
  }],
  ['unregistered Compose service', /unregistered or unshipped App Compose service/, (f) => {
    const file = path.join(f.release, 'docker/docker-compose-frontend.yml');
    fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace('\nnetworks:\n', '\n  namewta-nginx-rogue-web:\n    image: nginx:1.31.1\n\nnetworks:\n')); f.commit();
  }],
  ['missing SSO template', /missing nginx template/, (f) => {
    fs.unlinkSync(path.join(f.release, 'docker/frontend/nginx/apps/nginx-sso-web.conf.template')); f.commit();
  }],
  ['wrong template mount', /Compose App binding drift/, (f) => {
    const file = path.join(f.release, 'docker/docker-compose-frontend.yml');
    fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace('./frontend/nginx/apps/nginx-sso-web.conf.template:', './frontend/nginx/apps/nginx-home-web.conf.template:')); f.commit();
  }],
  ['wrong SSO port default', /Compose App binding drift/, (f) => {
    const file = path.join(f.release, 'docker/docker-compose-frontend.yml');
    fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace('SSO_WEB_PORT:-41083', 'SSO_WEB_PORT:-41099')); f.commit();
  }],
  ['missing SSO Origin binding', /Compose App binding drift/, (f) => {
    const file = path.join(f.release, 'docker/docker-compose-frontend.yml');
    fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace(/^      APP_ORIGIN:.*\n/m, '')); f.commit();
  }],
  ['missing prefix', /SSO_WEB_PREFIX missing/, (f) => {
    const file = path.join(f.release, '.env'); fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace(/^SSO_WEB_PREFIX=.*\n/m, ''));
  }],
  ['same SSO Origin', /independent Web Origin/, (f) => {
    const file = path.join(f.release, '.env'); fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace(/^SSO_WEB_ORIGIN=.*$/m, 'SSO_WEB_ORIGIN=https://localhost:4441'));
  }],
  ['same SSO hostname with a different port', /separate hostname/, (f) => {
    const file = path.join(f.release, '.env'); fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace(/^SSO_WEB_ORIGIN=.*$/m, 'SSO_WEB_ORIGIN=https://localhost:9999'));
  }],
  ['production HTTP Origin', /require HTTPS/, (f) => {
    const file = path.join(f.release, '.env'); fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace(/^SSO_WEB_ORIGIN=.*$/m, 'SSO_WEB_ORIGIN=http://sso.localhost:4443'));
  }],
  ['SSO API/static prefix collision', /prefix collides/, (f) => {
    const file = path.join(f.release, '.env'); fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace(/^SSO_WEB_PREFIX=.*$/m, 'SSO_WEB_PREFIX=sso'));
  }],
  ['invalid API route', /unsupported API route/, (f) => {
    const file = path.join(f.release, 'apps.json'), registry = JSON.parse(fs.readFileSync(file));
    registry.apps[0].apiPath = '/unregistered-api'; fs.writeFileSync(file, JSON.stringify(registry)); f.commit();
  }]
];
for (const [name, diagnostic, mutate] of scenarios) {
  test(`invalid release registration/config: ${name} cannot change selected assets`, () => {
    const f = fixture();
    try {
      const id = f.build(); f.stage(id);
      const version = snapshot(f.version(id));
      mutate(f);
      const contexts = snapshot(path.join(f.release, 'docker'));
      const result = f.cli('build', '--target', 'all', '--env', 'prod');
      assert.notEqual(result.status, 0);
      assert.match(result.stderr, diagnostic);
      assert.equal(fs.readlinkSync(f.current), 'versions/' + id);
      assert.deepEqual(snapshot(f.version(id)), version);
      assert.deepEqual(snapshot(path.join(f.release, 'docker')), contexts);
    } finally { f.dispose(); }
  });
}

test('SSO registration preserves runtime configuration and is idempotent, including port changes', () => {
  const f = fixture();
  try {
    const runtime = fs.readFileSync(path.join(f.release, '.env'));
    const generate = () => spawnSync(process.execPath, [path.join(f.release, 'skills/wta-namewta-nginx-config/scripts/add_app.mjs'),
      '--repo-root', f.root, '--app', 'sso-web', '--prefix', 'sso-app', '--port', '41084'], { encoding: 'utf8' });
    let result = generate(); assert.equal(result.status, 0, result.stderr);
    const first = snapshot(f.release);
    result = generate(); assert.equal(result.status, 0, result.stderr);
    assert.deepEqual(snapshot(f.release), first);
    assert.deepEqual(fs.readFileSync(path.join(f.release, '.env')), runtime);
    result = f.cli('check-apps'); assert.equal(result.status, 0, result.stderr);
    fs.unlinkSync(path.join(f.release, 'docker/frontend/nginx/apps/nginx-sso-web.conf.template'));
    const missing = snapshot(f.release);
    result = generate(); assert.notEqual(result.status, 0); assert.match(result.stderr, /SSO 专用模板缺失/);
    assert.deepEqual(snapshot(f.release), missing);
  } finally { f.dispose(); }
});

test('runtime Origin changes are rejected before the selected release reaches Docker', () => {
  const f = fixture();
  try {
    const id = f.build(); f.stage(id);
    const file = path.join(f.release, '.env');
    fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace('https://sso.localhost:4443', 'https://other-sso.localhost:4443'));
    const calls = path.join(f.scratch, 'docker-calls.jsonl');
    const result = spawnSync('bash', [path.join(f.release, 'scripts/docker-manage.sh'), 'up', 'frontend'], {
      encoding: 'utf8', env: { ...f.env, DOCKER_CALLS: calls }
    });
    assert.notEqual(result.status, 0); assert.match(result.stderr, /runtime origins differ/);
    const records = fs.existsSync(calls) ? fs.readFileSync(calls, 'utf8').trim().split('\n').map(JSON.parse) : [];
    assert.equal(records.some(row => row.args.includes('-f')), false);
    assert.equal(fs.readlinkSync(f.current), 'versions/' + id);
  } finally { f.dispose(); }
});
