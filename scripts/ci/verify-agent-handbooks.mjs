import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';

/**
 * 校验两个产品子仓库的 manifest 与 AGENTS 索引是否一一对应。
 * 该脚本只验证导读索引，不复制或替代工程规范。
 */

const root = resolve(dirname(new URL(import.meta.url).pathname), '../..');
const products = [
  { path: join(root, 'backend'), manifest: 'pom.xml' },
  { path: join(root, 'frontend'), manifest: 'package.json' },
];
const ignored = new Set(['.git', 'node_modules', 'target', 'dist', 'specdev-worktree']);
const requiredSections = ['## Scope', '## Purpose', '## Components', '## Entry Points',
  '## Dependencies', '## Verification', '## Read Next'];
const failures = [];

function walk(directory, predicate) {
  if (!existsSync(directory)) return [];
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    if (ignored.has(entry.name)) return [];
    const path = join(directory, entry.name);
    return entry.isDirectory() ? walk(path, predicate) : predicate(path) ? [path] : [];
  });
}

for (const product of products) {
  const manifests = walk(product.path, path => path.endsWith(`/${product.manifest}`));
  const expected = new Set(manifests.map(path => dirname(path)));
  const handbooks = walk(product.path, path => path.endsWith('AGENTS.md'));
  const actual = new Set(handbooks.map(path => dirname(path)));

  for (const directory of expected) {
    if (!actual.has(directory)) {
      failures.push(`${relative(root, directory)} 缺少 AGENTS.md`);
    }
  }
  for (const directory of actual) {
    if (!expected.has(directory)) {
      failures.push(`${relative(root, directory)} 的 AGENTS.md 没有对应 ${product.manifest}`);
    }
  }

  for (const file of handbooks) {
    const directory = dirname(file);
    const source = readFileSync(file, 'utf8');
    if (!/[\u3400-\u9fff]/u.test(source)) {
      failures.push(`${relative(root, file)} 必须包含中文索引内容`);
    }
    // 旧模块保留既有详细手册；新索引必须保持渐进式披露的七个最小章节。
    const isLegacySystem = relative(root, directory) === 'backend/wta-modules/wta-system';
    if (!isLegacySystem) {
      for (const section of requiredSections) {
        if (!source.includes(section)) failures.push(`${relative(root, file)} 缺少 ${section}`);
      }
    }
  }

  const claudeFiles = walk(product.path, path => path.endsWith('CLAUDE.md'));
  for (const file of claudeFiles) failures.push(`${relative(root, file)} 不应新增 CLAUDE.md`);
  process.stdout.write(`${relative(root, product.path)}: ${manifests.length} manifest, ${handbooks.length} AGENTS.md\n`);
}

if (failures.length) {
  failures.forEach(message => process.stderr.write(`ERROR: ${message}\n`));
  process.exit(1);
}
process.stdout.write('AGENTS manifest contract passed.\n');
