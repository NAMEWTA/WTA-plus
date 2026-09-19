import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { verifyHandbooks } from './verify-agent-handbooks.mjs';

function fixture(t) {
  const root = mkdtempSync(join(tmpdir(), 'handbook workspace '));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const write = (path, source) => {
    const target = join(root, path);
    mkdirSync(dirname(target), { recursive: true });
    writeFileSync(target, source);
  };
  write('backend/pom.xml', '<project/>');
  write('frontend/package.json', '{}');
  write('backend/AGENTS.md', '# 后端导航\n\n[构建](pom.xml)\n');
  write('frontend/AGENTS.md', '# 前端导航\n\n[构建](package.json)\n');
  return { root, write };
}

test('modules inherit the nearest guide; meaningful local guides remain owners', t => {
  const { root, write } = fixture(t);
  write('backend/common/pom.xml', '<project/>');
  write('backend/common/AGENTS.md', '# 公共模块导航\n[父导航](../AGENTS.md)\n');
  write('backend/common/leaf/pom.xml', '<project/>');
  write('backend/special/pom.xml', '<project/>');
  write('backend/special/AGENTS.md', '# 特殊模块\n独有约束保留。\n');
  write('frontend/packages/validation/package.json', '{}');
  const result = verifyHandbooks(root);
  assert.deepEqual(result.failures, []);
  assert.equal(result.products[0].assignments.find(row => row.manifest.endsWith('leaf/pom.xml')).handbook,
    'backend/common/AGENTS.md');
  assert.equal(result.products[0].assignments.find(row => row.manifest.endsWith('special/pom.xml')).handbook,
    'backend/special/AGENTS.md');
  assert.equal(result.products[1].assignments.find(row => row.manifest.includes('validation')).handbook,
    'frontend/AGENTS.md');
});

test('missing product navigation fails even when the workspace root has a guide', t => {
  const { root, write } = fixture(t);
  rmSync(join(root, 'backend/AGENTS.md'));
  write('AGENTS.md', '# 仓根规范\n');
  write('backend/leaf/pom.xml', '<project/>');
  assert.equal(verifyHandbooks(root).failures.filter(message => message.includes('没有适用')).length, 2);
});

test('missing, out-of-workspace and malformed local links fail', t => {
  const { root, write } = fixture(t);
  write('backend/AGENTS.md', '# 中文导航\n[缺失](missing.md)\n[越界](../../outside.md)\n[编码](%ZZ.md)\n');
  assert.equal(verifyHandbooks(root).failures.length, 3);
});

test('encoded paths and links with spaces work without enforcing template sections', t => {
  const { root, write } = fixture(t);
  write('docs/a b.md', '# 中文参考\n');
  write('backend/AGENTS.md', '# 中文导航\n[资料](../docs/a%20b.md#参考)\n[资料](<../docs/a b.md>)\n[网站](https://example.test)\n');
  assert.deepEqual(verifyHandbooks(root).failures, []);
});

test('empty or non-Chinese guides and nested CLAUDE copies fail', t => {
  const { root, write } = fixture(t);
  write('backend/AGENTS.md', '# Backend only\n');
  write('frontend/AGENTS.md', '中文但没有标题\n');
  write('frontend/CLAUDE.md', 'duplicate');
  assert.equal(verifyHandbooks(root).failures.length, 3);
});

test('missing manifests cannot produce a zero-input green result', t => {
  const { root } = fixture(t);
  rmSync(join(root, 'backend/pom.xml'));
  rmSync(join(root, 'frontend/package.json'));
  assert.equal(verifyHandbooks(root).failures.filter(message => message.includes('缺少工作区')).length, 2);
});

test('dependency and build outputs are excluded; similar filenames are not handbooks', t => {
  const { root, write } = fixture(t);
  for (const path of ['backend/target', 'frontend/node_modules', 'frontend/dist', 'frontend/coverage']) {
    write(`${path}/AGENTS.md`, 'generated');
    write(`${path}/CLAUDE.md`, 'generated');
  }
  write('backend/NOT-AGENTS.md', 'not a handbook');
  assert.deepEqual(verifyHandbooks(root).failures, []);
});

test('CLI handles a workspace with spaces and returns failure for broken navigation', t => {
  const { root, write } = fixture(t);
  const script = fileURLToPath(new URL('./verify-agent-handbooks.mjs', import.meta.url));
  const run = () => spawnSync(process.execPath, [script, '--root', root], { encoding: 'utf8' });
  assert.equal(run().status, 0);
  write('backend/AGENTS.md', '# 中文导航\n[丢失](missing.md)\n');
  const failure = run();
  assert.equal(failure.status, 1);
  assert.match(failure.stderr, /本地链接无效/);
  assert.equal(spawnSync(process.execPath, [script, '--unknown'], { encoding: 'utf8' }).status, 2);
});
