import { existsSync, readFileSync } from 'node:fs';
import { dirname, extname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const skillRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const workspaceRoot = resolve(skillRoot, '../../..');
const failures = [];

function fail(message) {
  failures.push(message);
}

function read(path) {
  if (!existsSync(path)) {
    fail(`缺少文件: ${path}`);
    return '';
  }
  return readFileSync(path, 'utf8');
}

const requiredFiles = [
  'SKILL.md',
  'references/vertical-slice.md',
  'references/contract-mapping.md',
  'references/verification-matrix.md',
  'references/frontend/architecture.md',
  'references/frontend/coding-style.md',
  'references/frontend/comments.md',
  'references/frontend/crud-resource-slices.md',
  'references/frontend/implementation.md',
  'references/frontend/naming-and-layout.md',
  'references/frontend/permission-routing.md',
  'references/backend/architecture.md',
  'references/backend/framework-usage.md',
  'references/backend/implementation.md',
  'references/backend/mapper-and-sql.md',
  'references/backend/module-layout.md',
  'references/backend/verification.md',
  'scripts/validate-skill.mjs',
  'scripts/validate-module-mode.mjs',
];

for (const relativePath of requiredFiles) read(join(skillRoot, relativePath));

const skillSource = read(join(skillRoot, 'SKILL.md'));
const frontmatterMatch = skillSource.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n/);
if (!frontmatterMatch) {
  fail('SKILL.md 缺少有效 frontmatter');
} else {
  const keys = frontmatterMatch[1]
    .split('\n')
    .filter(line => line.trim())
    .map(line => line.slice(0, line.indexOf(':')).trim());
  if (JSON.stringify(keys.sort()) !== JSON.stringify(['description', 'name'])) {
    fail(`frontmatter 只允许 name 和 description，当前为: ${keys.join(', ')}`);
  }
  if (!frontmatterMatch[1].includes('name: namewta-fullstack-development')) {
    fail('frontmatter name 必须为 namewta-fullstack-development');
  }
}

const markdownFiles = requiredFiles.filter(path => extname(path) === '.md');
const allMarkdown = markdownFiles.map(relativePath => read(join(skillRoot, relativePath))).join('\n');
const requiredTokens = [
  '需求/菜单',
  '数据库与初始化数据',
  'Controller/API/权限',
  'domain transport/model/service',
  'web-domain 页面/runtime/manifest',
  'App services/路由/权限投影',
  'dictType + dictValue -> dictLabel',
  'GET',
  'POST',
  '@Log',
  'legacy',
  'classic',
  'layered',
  'Controller/Listener/API Adapter -> UseCase -> Service -> DAO -> Mapper -> XML',
  'BaseMapperPlus',
  '50-namewta-ddl.sql',
  '60-namewta-dml.sql',
  'package.json#exports',
  'manifest',
  'pnpm architecture:check',
  './mvnw test',
  'validate-module-mode.mjs',
  'docs/fm/vue',
  'docs/fm/java',
  'getInfo -> getRouters -> addRoute -> replace',
  'componentKey',
  'local-name',
  'tabler:name',
  'sys_menu.icon',
  '失败关闭',
];
for (const token of requiredTokens) {
  if (!allMarkdown.includes(token)) fail(`统一全栈 Skill 缺少合同: ${token}`);
}

for (const relativePath of markdownFiles) {
  const source = read(join(skillRoot, relativePath));
  const linkPattern = /\[[^\]]*\]\(([^)]+)\)/g;
  for (const match of source.matchAll(linkPattern)) {
    const target = match[1].split('#')[0];
    if (!target || /^(https?:|mailto:)/.test(target)) continue;
    const resolved = resolve(dirname(join(skillRoot, relativePath)), target);
    if (!existsSync(resolved)) fail(`${relativePath} 包含失效链接: ${match[1]}`);
  }
}

const expectedTemplates = [
  'java/domain.java.ftl',
  'java/bo.java.ftl',
  'java/vo.java.ftl',
  'java/read.java.ftl',
  'java/mapper.java.ftl',
  'java/layered/mapper.java.ftl',
  'java/service.java.ftl',
  'java/serviceImpl.java.ftl',
  'java/controller.java.ftl',
  'java/layered/controller.java.ftl',
  'java/layered/usecase.java.ftl',
  'java/layered/usecaseImpl.java.ftl',
  'java/layered/service.java.ftl',
  'java/layered/dao.java.ftl',
  'xml/mapper.xml.ftl',
  'sql/mysql.sql.ftl',
];
for (const template of expectedTemplates) read(join(workspaceRoot, 'docs/fm', template));

const frontendAnchors = [
  'plus-ui-namewta/packages/domains',
  'plus-ui-namewta/packages/web-domains',
  'plus-ui-namewta/packages/api-contracts',
  'plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts',
  'plus-ui-namewta/tooling/architecture/test/domain-layout.test.mjs',
];
for (const anchor of frontendAnchors) {
  if (!existsSync(join(workspaceRoot, anchor))) fail(`缺少前端架构证据: ${anchor}`);
}

const backendAnchors = [
  'wta-vue-plus-namewta/pom.xml',
  'wta-vue-plus-namewta/wta-modules/wta-system/src/main/java',
  'wta-vue-plus-namewta/wta-common',
  'release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql',
  'release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql',
];
for (const anchor of backendAnchors) {
  if (!existsSync(join(workspaceRoot, anchor))) fail(`缺少后端/交付证据: ${anchor}`);
}

const retiredEntrypoints = ['plus-ui-' + 'frontend-conventions', 'wta-' + 'backend-development'];
if (retiredEntrypoints.some(entry => allMarkdown.includes(entry))) {
  fail('统一 Skill 内仍残留已删除的前端/后端入口名称');
}

if (failures.length) {
  failures.forEach(message => process.stderr.write(`ERROR: ${message}\n`));
  process.exit(1);
}

process.stdout.write(`Validated unified full-stack Skill (${requiredFiles.length} files, ${expectedTemplates.length} templates).\n`);
