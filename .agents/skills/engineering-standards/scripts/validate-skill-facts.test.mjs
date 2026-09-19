import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const root = resolve(fileURLToPath(new URL('.', import.meta.url)), '../../../../');
const script = '.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs';

function fixture(t) {
  const directory = mkdtempSync(join(tmpdir(), 'wta-skill-facts-'));
  t.after(() => rmSync(directory, { recursive: true, force: true }));
  cpSync(join(root, '.agents/skills'), join(directory, '.agents/skills'), { recursive: true });
  cpSync(join(root, '.gitignore'), join(directory, '.gitignore'));
  const init = spawnSync('git', ['init', '--quiet', directory], { encoding: 'utf8' });
  assert.equal(init.status, 0, init.stderr);
  return directory;
}

function validate(directory) {
  return spawnSync(process.execPath, [join(directory, script)], { cwd: directory, encoding: 'utf8' });
}

test('fresh repository passes without creating private release output', t => {
  const directory = fixture(t);
  const result = validate(directory);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(existsSync(join(directory, 'temp')), false);
});

test('unprotected private output fails even before it exists', t => {
  const directory = fixture(t);
  writeFileSync(join(directory, '.gitignore'), '');
  const result = validate(directory);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /必须受 Git ignore 保护/);
});

test('force-tracked private output is rejected even with ignore rules', t => {
  const directory = fixture(t);
  mkdirSync(join(directory, 'temp/release'), { recursive: true });
  writeFileSync(join(directory, 'temp/release/deployment.md'), 'fixture only');
  const add = spawnSync('git', ['add', '--force', 'temp/release/deployment.md'], { cwd: directory, encoding: 'utf8' });
  assert.equal(add.status, 0, add.stderr);
  const result = validate(directory);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /不得包含跟踪文件/);
});

test('obsolete private output spelling is rejected', t => {
  const directory = fixture(t);
  mkdirSync(join(directory, 'temp/relase'), { recursive: true });
  const result = validate(directory);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /旧私密发布目录/);
});

test('missing report output owner is rejected', t => {
  const directory = fixture(t);
  rmSync(join(directory, '.agents/skills/deploy-namewta-environment/scripts/lib.mjs'));
  const result = validate(directory);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /部署工具 owner/);
});
