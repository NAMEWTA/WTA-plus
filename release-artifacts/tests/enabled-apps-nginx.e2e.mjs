// Explicit local runtime check: cached Nginx image, disposable containers, no published ports.
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { activeFrontendFiles } from '../scripts/active-apps.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const shipped = JSON.parse(fs.readFileSync(path.join(root, 'apps.json'))).apps.filter((app) => app.shipped);
const image = process.env.NAMEWTA_NGINX_TEST_IMAGE || 'nginx:1.31.1';

for (const selected of [['admin-web'], ['admin-web', 'home-web'], ['admin-web', 'sso-web'], shipped.map((app) => app.id)]) {
  for (const protocol of ['http', 'tls']) {
    test(`${selected.join('+')} ${protocol}: Nginx loads with no DNS or certificate for disabled Apps`, () => {
      const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'namewta-enabled-nginx-'));
      const name = `namewta-enabled-nginx-${crypto.randomBytes(8).toString('hex')}`;
      let started = false;
      try {
        const files = activeFrontendFiles(root, shipped, selected);
        const template = files[`docker/frontend/nginx/lb/nginx-lb-${protocol}-active.conf.template`];
        const variables = { LB_SERVER_NAME: 'apps.localhost' };
        for (const app of shipped.filter((app) => selected.includes(app.id))) variables[`APP_${app.prefixEnv}`] = app.id;
        const config = template.replace(/\$\{([A-Z_]+)\}/g, (_token, key) => {
          assert.ok(Object.hasOwn(variables, key), `unexpected runtime dependency ${key}`);
          return variables[key];
        });
        fs.writeFileSync(path.join(directory, 'default.conf'), config);
        const mounts = ['--mount', `type=bind,src=${path.join(directory, 'default.conf')},dst=/etc/nginx/conf.d/default.conf,readonly`];
        if (protocol === 'tls') {
          const cert = spawnSync('openssl', ['req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-days', '1',
            '-subj', '/CN=apps.localhost', '-keyout', path.join(directory, 'privkey.pem'),
            '-out', path.join(directory, 'fullchain.pem')], { encoding: 'utf8' });
          assert.equal(cert.status, 0, 'failed to generate owned LB certificate');
          fs.chmodSync(path.join(directory, 'privkey.pem'), 0o600);
          mounts.push('--mount', `type=bind,src=${directory},dst=/etc/nginx/cert/lb,readonly`);
        }
        // Non-App routes are unchanged. Only active App service names exist in this fixture.
        const hosts = ['namewta-monitor-admin', 'namewta-snailjob-server', 'nacos',
          ...shipped.filter((app) => selected.includes(app.id)).map((app) => app.composeService)];
        started = true;
        const result = spawnSync('docker', ['run', '--rm', '--pull', 'never', '--name', name,
          '--label', 'namewta.test=enabled-apps', '--network', 'none', '--read-only',
          '--tmpfs', '/run', '--tmpfs', '/var/cache/nginx',
          ...hosts.flatMap((host) => ['--add-host', `${host}:127.0.0.1`]), ...mounts,
          '--entrypoint', 'nginx', image, '-t'], { encoding: 'utf8', timeout: 30_000 });
        assert.equal(result.status, 0, `${result.stderr}\n${result.error?.message ?? ''}`);
        assert.match(result.stderr, /test is successful/);
      } finally {
        if (started) spawnSync('docker', ['rm', '-f', name], { stdio: 'ignore', timeout: 10_000 });
        fs.rmSync(directory, { recursive: true, force: true });
      }
    });
  }
}
