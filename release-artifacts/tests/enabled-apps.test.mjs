import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { ACTIVE_FRONTEND_COMPOSE, activeFrontendFiles, enabledApps } from '../scripts/active-apps.mjs';
import { fixture } from './fixtures/atomic-release-fixture.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const shipped = JSON.parse(fs.readFileSync(path.join(root, 'apps.json'))).apps.filter((app) => app.shipped);
const selections = [['admin-web'], ['admin-web', 'home-web'], ['admin-web', 'sso-web'], shipped.map((app) => app.id)];
const composeAvailable = spawnSync('docker', ['compose', 'version'], { encoding: 'utf8' }).status === 0;

function select(f, selected) {
  const file = path.join(f.release, '.env');
  const disabledKeys = shipped.filter((app) => !selected.includes(app.id)).flatMap((app) => [app.prefixEnv, app.originEnv]);
  const lines = fs.readFileSync(file, 'utf8').split('\n')
    .filter((line) => !disabledKeys.some((key) => line.startsWith(`${key}=`)));
  fs.writeFileSync(file, `${lines.join('\n')}\nNAMEWTA_ENABLED_APPS=${selected.join(',')}\nLB_SERVER_NAME=apps.localhost\n`);
}

test('selection defaults to shipped inventory and requires unique known Apps including Admin', () => {
  assert.deepEqual(enabledApps(shipped), shipped.map((app) => app.id));
  assert.deepEqual(enabledApps(shipped, ' home-web , admin-web '), ['admin-web', 'home-web']);
  for (const value of ['', 'home-web', 'sso-web', 'admin-web,unknown', 'admin-web,admin-web', 'admin-web,']) {
    assert.throws(() => enabledApps(shipped, value), /NAMEWTA_ENABLED_APPS/);
  }
});

for (const selected of selections) {
  test(`${selected.join('+')}: sealed deployment contains only enabled services, origins, routes and TLS mounts`, () => {
    const f = fixture();
    try {
      select(f, selected);
      const id = f.build();
      const version = f.version(id);
      const manifest = JSON.parse(fs.readFileSync(path.join(version, 'release-manifest.json')));
      assert.deepEqual(manifest.enabledApps, selected);
      assert.deepEqual(Object.keys(manifest.apps), shipped.map((app) => app.id), 'build inventory stays complete');
      assert.deepEqual(Object.keys(manifest.appOrigins), selected);
      assert.deepEqual(Object.keys(manifest.applicationMatrix), selected);
      assert.deepEqual(manifest.corsAllowedOrigins, Object.values(manifest.appOrigins).sort());
      const compose = fs.readFileSync(path.join(version, ACTIVE_FRONTEND_COMPOSE), 'utf8');
      for (const app of shipped) {
        assert.ok(fs.existsSync(path.join(version, 'docker/frontend/nginx/html', app.id, 'index.html')));
        for (const endpoint of [app, ...(app.tls ? [app.tls] : [])]) {
          assert.equal(new RegExp(`^  ${endpoint.composeService}:$`, 'm').test(compose), selected.includes(app.id));
        }
        for (const protocol of ['http', 'tls']) {
          const lb = fs.readFileSync(path.join(version, `docker/frontend/nginx/lb/nginx-lb-${protocol}-active.conf.template`), 'utf8');
          assert.equal(lb.includes(`server ${app.composeService}:`), selected.includes(app.id));
          assert.equal(lb.includes(`\${APP_${app.prefixEnv}}`), selected.includes(app.id));
          if (app.apiKind === 'sso') {
            assert.equal(lb.includes('location ^~ /sso/'), selected.includes(app.id));
            assert.equal(lb.includes('openid-configuration'), selected.includes(app.id));
          }
        }
        if (!selected.includes(app.id)) {
          assert.ok(!compose.includes(app.prefixEnv));
          assert.ok(!compose.includes(app.originEnv));
          assert.ok(!compose.includes(`/cert/${app.id}`));
        }
      }
      f.stage(id);
      const calls = path.join(f.scratch, 'docker-calls.jsonl');
      const result = spawnSync('bash', [path.join(f.release, 'scripts/docker-manage.sh'), 'config', 'frontend', '--profile', 'tls'], {
        encoding: 'utf8', env: { ...f.env, DOCKER_CALLS: calls },
      });
      assert.equal(result.status, 0, result.stderr);
      const call = fs.readFileSync(calls, 'utf8').trim().split('\n').map(JSON.parse).at(-1);
      assert.ok(call.args.includes(path.join(version, ACTIVE_FRONTEND_COMPOSE)));
      assert.equal(call.enabledApps, selected.join(','));
      assert.equal(call.sso, selected.includes('sso-web') ? manifest.appOrigins['sso-web'] : '');
      assert.equal(call.ssoBase, selected.includes('sso-web') ? '/sso-app/' : '');
      assert.equal(call.cors, manifest.corsAllowedOrigins.join(','));
    } finally { f.dispose(); }
  });

  test(`${selected.join('+')}: real Compose parser accepts HTTP and TLS without disabled App configuration`, { skip: !composeAvailable }, () => {
    const f = fixture();
    try {
      select(f, selected);
      for (const [relative, content] of Object.entries(activeFrontendFiles(f.release, shipped, selected))) {
        fs.writeFileSync(path.join(f.release, relative), content);
      }
      for (const args of [[], ['--profile', 'tls']]) {
        const result = spawnSync('docker', ['compose', '--env-file', path.join(f.release, '.env'),
          '-f', path.join(f.release, ACTIVE_FRONTEND_COMPOSE), ...args, 'config', '--format', 'json'], {
          env: process.env, encoding: 'utf8',
        });
        assert.equal(result.status, 0, result.stderr);
        const services = JSON.parse(result.stdout).services;
        for (const app of shipped) {
          assert.equal(Object.hasOwn(services, app.composeService), selected.includes(app.id));
          if (app.tls) assert.equal(Object.hasOwn(services, app.tls.composeService), selected.includes(app.id) && args.length > 0);
        }
      }
    } finally { f.dispose(); }
  });
}

test('runtime enabled App drift fails before any Compose operation', () => {
  const f = fixture();
  try {
    select(f, ['admin-web']);
    const id = f.build();
    f.stage(id);
    const file = path.join(f.release, '.env');
    fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace('NAMEWTA_ENABLED_APPS=admin-web', 'NAMEWTA_ENABLED_APPS=admin-web,home-web'));
    const calls = path.join(f.scratch, 'docker-calls.jsonl');
    const result = spawnSync('bash', [path.join(f.release, 'scripts/docker-manage.sh'), 'up', 'frontend'], {
      encoding: 'utf8', env: { ...f.env, DOCKER_CALLS: calls },
    });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /runtime enabled Apps differ/);
    const records = fs.readFileSync(calls, 'utf8').trim().split('\n').map(JSON.parse);
    assert.ok(records.every((call) => !call.args.includes('-f')));
  } finally { f.dispose(); }
});

test('business backend nodes share database, Redis namespace and root-key configuration', () => {
  const backend = fs.readFileSync(path.join(root, 'docker/docker-compose-backend.yml'), 'utf8');
  const common = backend.split('\nservices:\n')[0];
  assert.equal((backend.match(/<<: \*admin-environment/g) ?? []).length, 2);
  assert.match(common, /SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL:.*MYSQL_DATABASE/);
  assert.match(common, /SPRING_DATA_REDIS_DATABASE: "\$\{REDIS_DATABASE:-0\}"/);
  assert.match(common, /REDISSON_KEYPREFIX: "\$\{REDIS_KEY_PREFIX:-WTA\}"/);
  assert.match(common, /AUTH_CONFIG_ROOT_KEY: "\$\{AUTH_CONFIG_ROOT_KEY:-\}"/);
});
