import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(fileURLToPath(new URL('.', import.meta.url)), '../../../../');
const skillRoot = join(root, '.agents', 'skills');
const failures = [];
const fail = message => failures.push(message);
const canonical = ['engineering-standards', 'namewta-fullstack-development', 'wta-common-modules-guide', 'wta-module-guide', 'deploy-namewta-environment'];
for (const name of canonical) {
  const file = join(skillRoot, name, 'SKILL.md');
  if (!existsSync(file)) { fail(`缺少正式 Skill: ${name}`); continue; }
  const source = readFileSync(file, 'utf8');
  if (!/^---\r?\n[\s\S]*?\r?\n---\r?\n/.test(source)) fail(`${name}/SKILL.md 缺少 frontmatter`);
}
const markdownFiles = [];
function walk(directory) {
  if (!existsSync(directory)) return;
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) walk(path);
    else if (entry.name.endsWith('.md')) markdownFiles.push(path);
  }
}
walk(skillRoot);
const all = markdownFiles.map(file => readFileSync(file, 'utf8')).join('\n');
const canonicalText = markdownFiles
  .filter(file => {
    const normalized = file.replaceAll('\\', '/');
    return canonical
      .filter(name => name !== 'deploy-namewta-environment')
      .some(name => normalized.includes(`/.agents/skills/${name}/`));
  })
  .map(file => readFileSync(file, 'utf8'))
  .join('\n');
const forbidden = [
  'temp/relase',
  'wta-' + 'profile-module-guide',
  'wta-' + 'system-module-guide',
  'wta-' + 'workflow-module-guide',
  'client-web',
];
for (const value of forbidden) {
  if (canonicalText.includes(value)) fail(`Skills 残留过时事实: ${value}`);
}
if (!all.includes('wta-notify')) fail('Skills 未登记 wta-notify');
for (const required of ['BaseMapperPlus', 'WorkflowService', 'NotificationApplicationService', 'UseCase -> Service -> DAO -> Mapper', '50-namewta-ddl.sql', '60-namewta-dml.sql', '需求/菜单', 'domain transport/model/service', 'web-domain 页面/runtime/manifest', 'getInfo -> getRouters -> addRoute -> replace', 'local-name', 'tabler:name', 'sys_menu.icon']) {
  if (!all.includes(required)) fail(`Skills 缺少当前合同: ${required}`);
}
if (!existsSync(join(root, 'temp', 'release'))) fail('缺少统一私密发布目录 temp/release');
if (existsSync(join(root, 'temp', 'relase'))) fail('旧私密发布目录 temp/relase 仍存在');
if (failures.length) {
  for (const failure of failures) process.stderr.write(`ERROR: ${failure}\n`);
  process.exit(1);
}
process.stdout.write(`Validated ${canonical.length} canonical Skills and ${markdownFiles.length} markdown references.\n`);
