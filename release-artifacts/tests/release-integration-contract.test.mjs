import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const releaseRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const workspaceRoot = path.resolve(releaseRoot, '..');
const backendRoot = path.join(workspaceRoot, 'backend');
const sqlRoot = path.join(releaseRoot, 'docker/infrastructure/mysql/init');
const sqlFiles = [
  '10-cde-base-ddl.sql',
  '20-cde-job.sql',
  '30-cde-workflow.sql',
  '40-cde-ai.sql',
  '50-cde-base-dml.sql',
  '60-cde-nacos.sql',
];

function read(relativePath) {
  return fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8');
}

test('发布资产是当前唯一的 MySQL 与工作流事实源', () => {
  assert.equal(fs.existsSync(path.join(backendRoot, 'script')), false);
  assert.deepEqual(
    fs.readdirSync(sqlRoot).filter((name) => /^\d{2}-.*\.sql$/.test(name)).sort(),
    sqlFiles,
  );
  assert.deepEqual(
    fs.readdirSync(path.join(releaseRoot, 'workflow/leave')).sort(),
    Array.from({ length: 6 }, (_, index) => `leave${index + 1}.json`),
  );

  const tracked = execFileSync('git', [
    'ls-files',
    '--',
    'release-artifacts/docker/infrastructure/mysql/init',
    'release-artifacts/workflow/leave',
  ], { cwd: workspaceRoot, encoding: 'utf8' });
  for (const filename of sqlFiles) assert.match(tracked, new RegExp(`${filename}\\n`));
  for (let index = 1; index <= 6; index += 1) {
    assert.match(tracked, new RegExp(`leave${index}\\.json\\n`));
  }
});

test('当前治理声明直接维护基座并要求按标签升级已有库', () => {
  const currentAuthority = [
    'README.md',
    'docs/namewta-enhancements.md',
    'release-artifacts/README.md',
    '.agents/skills/namewta-fullstack-development/SKILL.md',
    '.agents/skills/engineering-standards/references/java/persistence-transactions-and-ddl.md',
    '.agents/skills/deploy-namewta-environment/references/upgrade-and-rollback.md',
  ].map(read).join('\n');

  assert.match(currentAuthority, /六份.*完整基座/s);
  assert.match(currentAuthority, /直接修改/);
  assert.match(currentAuthority, /源 Git Tag/);
  assert.match(currentAuthority, /目标 Git Tag/);
  assert.match(currentAuthority, /已有.*不得重放.*基座/s);
  assert.doesNotMatch(currentAuthority, /script\/sql\/namewta|两个文件均为 append-only|只允许在文件末尾追加/);
});

test('CI 复用受保护初始化器并把唯一 SQL 根传给后端测试', () => {
  const external = read('scripts/ci/run-external-services.sh');
  const workflow = read('.github/workflows/quality-gates.yml');
  const verifier = read('release-artifacts/scripts/verify-release.sh');

  assert.match(external, /init-mysql-container\.sh/);
  assert.match(external, /namewta\.sql\.root/);
  assert.match(external, /mysql:8\.4\.9/);
  assert.match(read('release-artifacts/scripts/init-mysql-container.sh'), /EXPECTED_TABLES=103/);
  assert.match(workflow, /release-contracts:/);
  assert.match(workflow, /verify-release\.sh/);
  assert.match(verifier, /tests\/\*\.test\.mjs/);
});

test('私密发布报告目录保持未跟踪', () => {
  execFileSync('git', ['check-ignore', '--quiet', '--', 'temp/release/deployment.md'], {
    cwd: workspaceRoot,
  });
});
