import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

test('public entry replaces forwarding chains while internal app proxies append', () => {
  for (const kind of ['http', 'tls']) {
    const source = fs.readFileSync(path.join(root, `docker/frontend/nginx/lb/nginx-lb-${kind}.conf.template`), 'utf8');
    const routes = [...source.matchAll(/location[^\n]*\{([\s\S]*?)\n    \}/g)]
      .filter(([, route]) => route.includes('proxy_pass'));
    assert.equal(routes.length, 6);
    for (const [, route] of routes) {
      assert.match(route, /proxy_set_header X-Forwarded-For \$remote_addr;/);
      assert.match(route, /proxy_set_header Forwarded "";/);
      assert.doesNotMatch(route, /\$proxy_add_x_forwarded_for/);
    }
  }
  for (const app of ['admin', 'home']) {
    const source = fs.readFileSync(path.join(root, `docker/frontend/nginx/apps/nginx-${app}-web.conf.template`), 'utf8');
    assert.match(source, /proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;/);
  }
});

test('generated public app routes also replace client supplied XFF', () => {
  const generator = fs.readFileSync(path.join(root, 'skills/wta-namewta-nginx-config/scripts/add_app.py'), 'utf8');
  assert.match(generator, /proxy_set_header X-Forwarded-For \$remote_addr;/);
  assert.doesNotMatch(generator, /proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;/);
});
