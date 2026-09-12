import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const releaseRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const workspaceRoot = path.resolve(releaseRoot, '..');
const sqlRoot = path.join(releaseRoot, 'docker/infrastructure/mysql/init');
const backendTestRoot = path.join(workspaceRoot, 'backend/wta-admin/src/test');

function javaFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const absolute = path.join(directory, entry.name);
    if (entry.isDirectory()) return javaFiles(absolute);
    return entry.name.endsWith('.java') ? [absolute] : [];
  });
}

test('NAMEWTA structure and data contracts live in the canonical 50 and 60 files', () => {
  const ddl = fs.readFileSync(path.join(sqlRoot, '50-namewta-ddl.sql'), 'utf8');
  const dml = fs.readFileSync(path.join(sqlRoot, '60-namewta-dml.sql'), 'utf8');

  for (const marker of [
    'NAMEWTA-OPENAPI-CREDENTIAL-DDL-001',
    'NAMEWTA-OSS-ACCESS-DDL-001',
    'NAMEWTA-ADMIN-RUNTIME-RECONCILE-DDL-001',
  ]) {
    const declaration = new RegExp(`^-- (?:逻辑标识：)?${marker}$`, 'm');
    assert.match(ddl, declaration);
    assert.doesNotMatch(dml, declaration);
  }
  for (const marker of [
    'NAMEWTA-OPENAPI-CREDENTIAL-DML-001',
    'NAMEWTA-OSS-ACCESS-DML-001',
    'NAMEWTA-ADMIN-RUNTIME-RECONCILE-DML-001',
  ]) {
    const declaration = new RegExp(`^-- (?:逻辑标识：)?${marker}$`, 'm');
    assert.match(dml, declaration);
    assert.doesNotMatch(ddl, declaration);
  }
  assert.match(dml, /^-- 变更标识：NAMEWTA-THIRD-MENU-DML-001$/m);
  assert.doesNotMatch(ddl, /NAMEWTA-THIRD-MENU-DML-001/);
});

test('backend SQL consumers use the explicit canonical baseline seam', () => {
  const sources = javaFiles(backendTestRoot).map((filename) => fs.readFileSync(filename, 'utf8')).join('\n');
  assert.doesNotMatch(sources, /script[\\/]sql/);
  assert.match(sources, /namewta\.sql\.root/);
  assert.match(sources, /release-artifacts.*docker.*infrastructure.*mysql.*init/s);
  assert.match(sources, /MySQL 基座目录不存在/);
});
