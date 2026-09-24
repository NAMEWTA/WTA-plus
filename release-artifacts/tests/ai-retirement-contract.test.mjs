import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');

test('release inventory contains the three retained Java applications', () => {
  const inventory = execFileSync(process.execPath, ['--input-type=module', '-e', `
import { BACKENDS } from './release-artifacts/scripts/release-state.mjs';
console.log(JSON.stringify(Object.keys(BACKENDS)));
`], { cwd: root, encoding: 'utf8' });
  assert.deepEqual(JSON.parse(inventory), ['wta-admin', 'wta-monitor-admin', 'wta-snailjob-server']);
  assert.equal(fs.existsSync(path.join(root, 'backend/wta-extend/wta-snailai-server/pom.xml')), false);
  assert.match(read('backend/pom.xml'), /spring-ai-bom/);
  assert.match(read('backend/wta-common/pom.xml'), /<module>wta-common-mcp<\/module>/);
  for (const module of ['wta-modules/wta-ai', 'wta-common/wta-common-ai']) {
    assert.ok(fs.existsSync(path.join(root, `backend/${module}/pom.xml`)));
    assert.ok(fs.existsSync(path.join(root, `backend/${module}/README.md`)));
  }
});

test('new baseline keeps six SQL slots and retires vendor tables and menu seeds', () => {
  const sqlRoot = 'release-artifacts/docker/infrastructure/mysql/init/';
  assert.deepEqual(fs.readdirSync(path.join(root, sqlRoot)).filter(name => name.endsWith('.sql')).sort(), [
    '10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql', '40-cde-ai.sql', '50-cde-base-dml.sql', '60-cde-nacos.sql',
  ]);
  assert.equal(read(sqlRoot + '40-cde-ai.sql').trim(), 'SET NAMES utf8mb4;');
  const dml = read(sqlRoot + '50-cde-base-dml.sql');
  assert.doesNotMatch(dml, /ai\/chat\/index|monitor\/snailai\/index/);
  assert.match(dml, /monitor\/snailjob\/index/);
  const tables = ['10-cde-base-ddl.sql', '20-cde-job.sql', '30-cde-workflow.sql', '40-cde-ai.sql']
    .flatMap(name => [...read(sqlRoot + name).matchAll(/create\s+table\s+(?:if\s+not\s+exists\s+)?`?(\w+)/gi)].map(match => match[1]));
  assert.equal(new Set(tables).size, 104);
  assert.equal(tables.some(name => name.startsWith('sai_')), false);
});

test('runtime declarations have no retired service, callback or upstream dependency', () => {
  for (const relative of [
    'release-artifacts/docker/docker-compose-backend.yml',
    'release-artifacts/docker/backend/images/wta-admin/Dockerfile',
    'backend/wta-admin/Dockerfile',
    'release-artifacts/docker/frontend/nginx/lb/nginx-lb-http.conf.template',
    'release-artifacts/docker/frontend/nginx/lb/nginx-lb-tls.conf.template',
    'release-artifacts/docker/observability/alloy/config.alloy',
  ]) assert.doesNotMatch(read(relative), /snail[-_]?ai|38080|4308[01]|48900|48888/i, relative);
  for (const mode of ['http', 'tls']) {
    const config = read(`release-artifacts/docker/frontend/nginx/lb/nginx-lb-${mode}.conf.template`);
    assert.match(config, /location \/snail-job\//);
    assert.match(config, /location \/nacos\//);
    assert.match(config, /location \/admin\//);
  }
});
