import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { basename, dirname, isAbsolute, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

/** 校验模块能继承有效导读及本地链接；独有硬约束的保留仍需逐文件评审。 */
const ignored = new Set(['.git', 'node_modules', 'target', 'dist', 'coverage', 'specdev-worktree']);

function walk(directory) {
  if (!existsSync(directory)) return [];
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    if (ignored.has(entry.name)) return [];
    const path = join(directory, entry.name);
    return entry.isDirectory() ? walk(path) : entry.isFile() ? [path] : [];
  });
}

export function verifyHandbooks(workspaceRoot) {
  const root = resolve(workspaceRoot);
  const failures = [];
  const products = [];
  for (const [name, manifest] of [['backend', 'pom.xml'], ['frontend', 'package.json']]) {
    const directory = join(root, name);
    const files = walk(directory);
    const manifests = files.filter(path => basename(path) === manifest);
    const handbooks = files.filter(path => basename(path) === 'AGENTS.md');
    const owners = new Set(handbooks);
    const assignments = [];
    if (!manifests.includes(join(directory, manifest))) failures.push(`${name} 缺少工作区 ${manifest}`);
    for (const path of manifests) {
      let current = dirname(path);
      let owner;
      // 产品根也是 manifest owner；不越过它借用仓根手册掩盖缺失导航。
      while (true) {
        const candidate = join(current, 'AGENTS.md');
        if (owners.has(candidate)) { owner = candidate; break; }
        if (current === directory) break;
        current = dirname(current);
      }
      if (!owner) failures.push(`${relative(root, path)} 没有适用的产品 AGENTS.md`);
      else assignments.push({ manifest: relative(root, path), handbook: relative(root, owner) });
    }
    for (const path of handbooks) {
      const source = readFileSync(path, 'utf8');
      if (!/^#\s+\S.*$/mu.test(source) || !/[\u3400-\u9fff]/u.test(source)) {
        failures.push(`${relative(root, path)} 必须包含标题和中文导读`);
      }
      // 仅校验 Markdown inline 链接落点，不声称解析完整 Markdown 或验证锚点。
      for (const match of source.matchAll(/\]\((?:<([^>]+)>|([^\s)]+))(?:\s+"[^"]*")?\)/gu)) {
        const link = match[1] ?? match[2];
        if (/^[a-z][a-z\d+.-]*:/iu.test(link) || link.startsWith('//')) continue;
        const location = link.split('#', 1)[0];
        if (!location) continue;
        let target;
        try { target = resolve(dirname(path), decodeURIComponent(location)); }
        catch { failures.push(`${relative(root, path)} 链接编码无效：${link}`); continue; }
        const local = relative(root, target);
        if (isAbsolute(local) || local === '..' || local.startsWith(`..${sep}`) || !existsSync(target)) {
          failures.push(`${relative(root, path)} 本地链接无效：${link}`);
        } else if (!statSync(target).isFile() && !statSync(target).isDirectory()) {
          failures.push(`${relative(root, path)} 链接不是文件或目录：${link}`);
        }
      }
    }
    for (const path of files.filter(path => basename(path) === 'CLAUDE.md')) {
      failures.push(`${relative(root, path)} 不应新增 CLAUDE.md`);
    }
    products.push({ product: name, manifests: manifests.length, handbooks: handbooks.length, assignments });
  }
  return { products, failures };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const args = process.argv.slice(2);
  if (args.length && (args.length !== 2 || args[0] !== '--root')) {
    process.stderr.write('Usage: node verify-agent-handbooks.mjs [--root <workspace>]\n');
    process.exitCode = 2;
  } else {
    const root = args[1] ?? resolve(dirname(fileURLToPath(import.meta.url)), '../..');
    const result = verifyHandbooks(root);
    for (const product of result.products) {
      process.stdout.write(`${product.product}: ${product.manifests} manifest, ${product.handbooks} AGENTS.md, ${product.assignments.length} assigned\n`);
    }
    for (const failure of result.failures) process.stderr.write(`ERROR: ${failure}\n`);
    process.exitCode = result.failures.length ? 1 : 0;
    if (!result.failures.length) process.stdout.write('AGENTS inheritance and local navigation passed.\n');
  }
}
