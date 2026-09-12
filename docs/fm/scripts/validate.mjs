import { readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const failures = [];

function files(directory) {
  return readdirSync(directory).flatMap(name => {
    const path = join(directory, name);
    return statSync(path).isDirectory() ? files(path) : [path];
  });
}

const templates = files(root)
  .filter(path => path.endsWith('.ftl'))
  .map(path => relative(root, path).split('\\').join('/'))
  .sort();
const catalog = JSON.parse(readFileSync(join(root, 'catalog.json'), 'utf8'));
const catalogTemplates = catalog.templates.map(item => item.source).sort();

if (catalog.schemaVersion !== 4) {
  failures.push(`catalog.json schemaVersion 必须为 4，当前为 ${catalog.schemaVersion}`);
}

if (JSON.stringify(templates) !== JSON.stringify(catalogTemplates)) {
  failures.push('catalog.json 与实际 .ftl 文件清单不一致');
}

if (new Set(catalogTemplates).size !== catalogTemplates.length) {
  failures.push('catalog.json 中存在重复的 source');
}

for (const item of catalog.templates) {
  if (!item.target || !Array.isArray(item.categories) || item.categories.length === 0) {
    failures.push(`模板 ${item.source} 必须声明 target 和至少一个 category`);
  }
  if (!Array.isArray(item.lifecycle) || item.lifecycle.length === 0) {
    failures.push(`模板 ${item.source} 必须声明 lifecycle`);
  }
  if (item.architectureModes && !Array.isArray(item.architectureModes)) {
    failures.push(`模板 ${item.source} 的 architectureModes 必须为数组`);
  }
  if (item.source.startsWith('java/layered/') &&
      JSON.stringify(item.architectureModes || []) !== JSON.stringify(['layered'])) {
    failures.push(`分层模板 ${item.source} 必须声明 architectureModes: [layered]`);
  }
  if (item.source.startsWith('java/layered/') &&
      JSON.stringify(item.lifecycle || []) !== JSON.stringify(['new'])) {
    failures.push(`分层模板 ${item.source} 只能声明 lifecycle: [new]`);
  }
  if (item.source.startsWith('java/') && !item.source.startsWith('java/layered/') &&
      ['java/controller.java.ftl', 'java/mapper.java.ftl', 'java/service.java.ftl', 'java/serviceImpl.java.ftl'].includes(item.source) &&
      JSON.stringify(item.lifecycle || []) !== JSON.stringify(['legacy'])) {
    failures.push(`classic 模板 ${item.source} 只能声明 lifecycle: [legacy]`);
  }
}

const sqlTemplates = catalog.templates.filter(item => item.source.startsWith('sql/'));
if (sqlTemplates.length !== 1 || sqlTemplates[0]?.source !== 'sql/mysql.sql.ftl') {
  failures.push('SQL 模板只允许 sql/mysql.sql.ftl');
} else {
  const expectedTarget = 'release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql';
  if (sqlTemplates[0].target !== expectedTarget || sqlTemplates[0].writeMode !== 'merge') {
    failures.push(`sql/mysql.sql.ftl 必须以 merge 方式合并到 ${expectedTarget}`);
  }
}

const unsupportedDialect = /(^|\/)(oracle|postgres|postgresql|sqlserver)(\.|\/)/i;
if (templates.some(source => unsupportedDialect.test(source))) {
  failures.push('当前项目不支持 Oracle、PostgreSQL 或 SQL Server 模板');
}

const mysqlSource = readFileSync(join(root, 'sql/mysql.sql.ftl'), 'utf8');
for (const token of ['client_id', '${clientPk}', 'query_param', 'active_menu', 'ext', '60-namewta-dml.sql']) {
  if (!mysqlSource.includes(token)) {
    failures.push(`sql/mysql.sql.ftl 缺少当前 sys_menu 契约字段或合并说明: ${token}`);
  }
}

const vueSource = templates
  .filter(path => path.startsWith('vue/'))
  .map(path => readFileSync(join(root, path), 'utf8'))
  .join('\n');
for (const forbidden of ["@/api", "@/hooks", "@/plugins", "@/utils", 'AxiosPromise', 'requestDownload']) {
  if (vueSource.includes(forbidden)) failures.push(`Vue 模板仍包含 App 私有依赖: ${forbidden}`);
}

const allSource = templates.map(path => readFileSync(join(root, path), 'utf8')).join('\n');
for (const forbidden of ["method: 'put'", "method: 'delete'", "method: 'patch'", '@PutMapping', '@DeleteMapping']) {
  if (allSource.includes(forbidden)) failures.push(`CRUD 模板仍包含禁用 method: ${forbidden}`);
}

for (const path of templates.filter(path => path.startsWith('java/') && path.endsWith('.ftl'))) {
  const source = readFileSync(join(root, path), 'utf8');
  if (!source.includes('/**') || !/[\u3400-\u9fff]/u.test(source)) {
    failures.push(`${path} 必须生成中文 Javadoc`);
  }
}

const controller = readFileSync(join(root, 'java/controller.java.ftl'), 'utf8');
const postMappings = [...controller.matchAll(/@PostMapping(?:\([^\n]*\))?/g)].length;
const logs = [...controller.matchAll(/@Log\(/g)].length;
if (postMappings !== logs) failures.push(`Java POST/@Log 数量不一致: POST=${postMappings}, Log=${logs}`);

const layeredContracts = {
  'java/layered/controller.java.ftl': [
    ['.usecase.', 'Controller 必须依赖 UseCase'],
    ['${controllerSurface}', 'Controller 模板必须使用 controllerSurface'],
    ['@Log(', 'layered Controller 必须生成操作日志'],
    ['pageQuery.getPageNum()', 'Controller 必须在入口解包分页参数'],
  ],
  'java/layered/usecase.java.ftl': [
    ['UseCase', 'UseCase 模板必须生成 UseCase 合同'],
  ],
  'java/layered/usecaseImpl.java.ftl': [
    ['private final ${ClassName}Service', 'UseCaseImpl 必须编排 Service'],
    ['@DSTransactional', 'UseCaseImpl 必须保留事务命令入口'],
  ],
  'java/layered/service.java.ftl': [
    ['.dao.', 'Service 必须依赖 DAO'],
    ['class ${ClassName}Service', 'Service 模板必须生成业务 Service'],
    ['PageResult<${ClassName}Row>', 'Service 必须消费 DAO 收敛后的 PageResult<Row>'],
    ['page.getRows()', 'Service 必须在业务层完成 Row 到 VO 的转换'],
  ],
  'java/layered/dao.java.ftl': [
    ['.mapper.', 'DAO 必须依赖 Mapper'],
    ['@Repository', 'DAO 模板必须标记 Repository'],
    ['selectPageByCondition', 'DAO 模板必须通过显式 Mapper 查询方法访问数据库'],
    ['new PageQuery(pageSize, pageNum)', 'DAO 必须在持久化边界构造 PageQuery'],
    ['PageResult<${ClassName}Row>', 'DAO 必须向 Service 收敛为 PageResult<Row>'],
  ],
  'java/layered/mapper.java.ftl': [
    ['.domain.model.read.', 'layered Mapper 必须依赖 Read Model'],
    ['BaseMapperPlus<${ClassName}, ${ClassName}Row>', 'layered Mapper 必须以 Row 作为读类型'],
  ],
};
for (const [path, contracts] of Object.entries(layeredContracts)) {
  const source = readFileSync(join(root, path), 'utf8');
  for (const [token, message] of contracts) {
    if (!source.includes(token)) failures.push(`${path} ${message}`);
  }
}

const layeredForbidden = {
  'java/layered/controller.java.ftl': ['.service.', 'Mapper'],
  'java/layered/usecase.java.ftl': ['PageQuery', 'Page<', 'com.baomidou.mybatisplus'],
  'java/layered/usecaseImpl.java.ftl': ['.dao.', '.mapper.', 'IService', 'BaseMapper', 'PageQuery', 'Page<', 'com.baomidou.mybatisplus'],
  'java/layered/service.java.ftl': ['.mapper.', 'BaseMapper', 'QueryWrapper', 'LambdaQueryWrapper', 'PageQuery', 'Page<', 'com.baomidou.mybatisplus'],
  'java/layered/dao.java.ftl': ['UseCase', 'Service', 'Gateway', 'ProfileService'],
  'java/layered/mapper.java.ftl': ['domain.vo.', '.service.', 'UseCase', 'Dao'],
};
for (const [path, tokens] of Object.entries(layeredForbidden)) {
  const source = readFileSync(join(root, path), 'utf8');
  for (const token of tokens) {
    if (source.includes(token)) failures.push(`${path} 不得包含越层依赖: ${token}`);
  }
}

for (const path of templates.filter(path => path.startsWith('agents/'))) {
  const source = readFileSync(join(root, path), 'utf8');
  for (const token of ['## Scope', '## Purpose', '## Components', '## Entry Points', '## Dependencies', '## Verification', '## Read Next']) {
    if (!source.includes(token)) failures.push(`${path} 缺少 AGENTS 最小索引章节: ${token}`);
  }
}

const resourceFieldPredicate = '<#if column.list || column.pk || column.insert || column.edit>';
for (const path of ['java/vo.java.ftl', 'react/types.ts.ftl', 'vue/transport.ts.ftl', 'vue/types.ts.ftl']) {
  if (!readFileSync(join(root, path), 'utf8').includes(resourceFieldPredicate)) {
    failures.push(`${path} 未覆盖列表、主键和可编辑资源字段`);
  }
}

const vueTypes = readFileSync(join(root, 'vue/types.ts.ftl'), 'utf8');
if (!vueTypes.includes('<#if column.insert || column.edit || column.pk>')) {
  failures.push('vue/types.ts.ftl 的 Form 未覆盖主键字段');
}

const composables = readFileSync(join(root, 'vue/composables.ts.ftl'), 'utf8');
if (/`(?:begin|end)\$\{propName\}`/.test(composables)) {
  failures.push('vue/composables.ts.ftl 包含未转义的 TypeScript 模板插值');
}

for (const token of ['pendingCount', 'useLatestRequest', 'onScopeDispose']) {
  if (!composables.includes(token)) {
    failures.push(`vue/composables.ts.ftl 缺少并发或生命周期合同: ${token}`);
  }
}
if (composables.includes('export function buildTree')) {
  failures.push('vue/composables.ts.ftl 不得在 web-domain 重复实现领域构树');
}

const vueTransport = readFileSync(join(root, 'vue/transport.ts.ftl'), 'utf8');
for (const token of ['expected an object', 'build${BusinessName}Tree', 'duplicate id', 'orphan id', 'cycle at']) {
  if (!vueTransport.includes(token)) {
    failures.push(`vue/transport.ts.ftl 缺少 transport/tree 失败关闭合同: ${token}`);
  }
}

const vueRuntime = readFileSync(join(root, 'vue/runtime.ts.ftl'), 'utf8');
for (const token of ['require${BusinessName}WebRuntime', "typeof runtime.service?.list !== 'function'", "typeof runtime.error !== 'function'"]) {
  if (!vueRuntime.includes(token)) {
    failures.push(`vue/runtime.ts.ftl 缺少 runtime 失败关闭合同: ${token}`);
  }
}

for (const page of ['vue/index.vue.ftl', 'vue/index-tree.vue.ftl']) {
  const source = readFileSync(join(root, page), 'utf8');
  for (const token of ['useLatestRequest', 'buttonLoading.value) return', 'aria-label=']) {
    if (!source.includes(token)) failures.push(`${page} 缺少异步、重复提交或可访问性合同: ${token}`);
  }
}

if (vueTypes.includes('export type Identifier') || vueTypes.includes('export interface ApiResponse')) {
  failures.push('vue/types.ts.ftl 不得在每个资源复制包级共享合同');
}

if (failures.length) {
  failures.forEach(message => process.stderr.write(`ERROR: ${message}\n`));
  process.exit(1);
}

process.stdout.write(`Validated ${templates.length} FreeMarker templates.\n`);
