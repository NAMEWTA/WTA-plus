import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const releaseRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const workspaceRoot = path.resolve(releaseRoot, '..');
const sqlRoot = path.join(releaseRoot, 'docker/infrastructure/mysql/init');
const sqlFiles = [
  '10-cde-base-ddl.sql',
  '20-cde-job.sql',
  '30-cde-workflow.sql',
  '40-cde-ai.sql',
  '50-cde-base-dml.sql',
  '60-cde-nacos.sql',
];

function digest(filename) {
  return crypto.createHash('sha256').update(fs.readFileSync(path.join(sqlRoot, filename))).digest('hex');
}

test('the six ordered MySQL files are non-empty UTF-8 baselines tracked by Git', () => {
  const actual = fs.readdirSync(sqlRoot)
    .filter((name) => /^\d{2}-.*\.sql$/.test(name))
    .sort();
  assert.deepEqual(actual, sqlFiles);

  const tracked = execFileSync('git', ['ls-files', '--', 'release-artifacts/docker/infrastructure/mysql/init'], {
    cwd: workspaceRoot,
    encoding: 'utf8',
  });
  for (const filename of sqlFiles) {
    const sql = fs.readFileSync(path.join(sqlRoot, filename), 'utf8');
    assert.ok(sql.length > 0, `${filename} must not be empty`);
    assert.match(sql, /^SET NAMES utf8mb4;/, `${filename} must declare utf8mb4`);
    assert.match(tracked, new RegExp(`${filename.replaceAll('.', '\\.')}\\n`));
  }
});

test('stage-mysql is idempotent and never rewrites the canonical SQL files', () => {
  const before = Object.fromEntries(sqlFiles.map((filename) => [filename, digest(filename)]));
  execFileSync('bash', [path.join(releaseRoot, 'scripts/release-manage.sh'), 'stage-mysql']);
  execFileSync('bash', [path.join(releaseRoot, 'scripts/release-manage.sh'), 'stage-mysql']);
  const after = Object.fromEntries(sqlFiles.map((filename) => [filename, digest(filename)]));
  assert.deepEqual(after, before);
});

test('release secrets and generated artifacts remain ignored', () => {
  const ignored = [
    'release-artifacts/.env',
    'release-artifacts/runtime/state.json',
    'release-artifacts/builds/generated.jar',
    'release-artifacts/bundles/generated.tar.gz',
    'release-artifacts/docker/backend/images/wta-admin/app.jar',
    'release-artifacts/docker/frontend/nginx/log/access.log',
  ];
  for (const relativePath of ignored) {
    execFileSync('git', ['check-ignore', '--quiet', '--', relativePath], { cwd: workspaceRoot });
  }

  for (const filename of sqlFiles) {
    assert.throws(
      () => execFileSync('git', ['check-ignore', '--quiet', '--', `release-artifacts/docker/infrastructure/mysql/init/${filename}`], { cwd: workspaceRoot }),
      /Command failed/,
    );
  }
});
