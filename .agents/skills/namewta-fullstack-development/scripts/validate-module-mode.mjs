import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { extname, join, relative, resolve } from 'node:path';
import process from 'node:process';

/**
 * 对后端 layered 模块执行轻量目录和 import 方向检查。
 * 该脚本不替代 Java 编译、架构测试或 SQL 集成测试；它只提供快速反馈。
 */

const args = process.argv.slice(2);
const moduleArg = args.find(arg => !arg.startsWith('--'));
const modeArg = args.find(arg => arg.startsWith('--mode='))?.split('=', 2)[1]
  ?? (args.includes('--mode') ? args[args.indexOf('--mode') + 1] : undefined)
  ?? 'layered';

if (!moduleArg) {
  process.stderr.write('用法: node validate-module-mode.mjs <模块路径> [--mode layered|classic]\n');
  process.exit(2);
}

const moduleRoot = resolve(process.cwd(), moduleArg);
const javaRoot = join(moduleRoot, 'src/main/java');
const resourcesRoot = join(moduleRoot, 'src/main/resources');
const failures = [];

function fail(message) {
  failures.push(message);
}

function collectJavaFiles(directory) {
  if (!existsSync(directory)) return [];
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? collectJavaFiles(path) : extname(entry.name) === '.java' ? [path] : [];
  });
}

if (!existsSync(moduleRoot)) fail(`模块路径不存在: ${moduleRoot}`);
if (!existsSync(join(moduleRoot, 'pom.xml'))) fail(`模块缺少 pom.xml: ${moduleRoot}`);

if (modeArg === 'classic') {
  for (const required of ['src/main/java', 'src/main/resources']) {
    if (!existsSync(join(moduleRoot, required))) fail(`classic 模块缺少目录: ${required}`);
  }
  if (failures.length) {
    failures.forEach(message => process.stderr.write(`ERROR: ${message}\n`));
    process.exit(1);
  }
  process.stdout.write(`Validated classic module: ${moduleArg}\n`);
  process.exit(0);
}

if (modeArg !== 'layered') {
  fail(`不支持的模块模式: ${modeArg}`);
}

if (!existsSync(resourcesRoot)) fail(`layered 模块缺少资源目录: ${resourcesRoot}`);

const javaFiles = collectJavaFiles(javaRoot);
// 只检查 Java 代码 token，避免注释、字符串和文本块伪装成 import/注解。
// 保留换行和字符位置，以便诊断仍能对应原始源码。
function codeOnly(source) {
  return source.replace(/\/\*[\s\S]*?\*\/|\/\/[^\r\n]*|"""(?:\\[\s\S]|(?!""")[^\\])*"""|"(?:\\[\s\S]|[^"\\])*"|'(?:\\[\s\S]|[^'\\])*'/g,
    token => token.replace(/[^\r\n]/g, ' '));
}
const sources = new Map(javaFiles.map(file => [file, codeOnly(readFileSync(file, 'utf8'))]));
const packageLines = javaFiles
  .map(file => sources.get(file).match(/^\s*package\s+([\w.]+);/m)?.[1])
  .filter(Boolean);
const packageSegments = packageLines.map(value => value.split('.'));
const baseSegments = packageSegments.length > 0 ? [...packageSegments[0]] : [];
for (const segments of packageSegments.slice(1)) {
  while (baseSegments.length > 0 && !baseSegments.every((segment, index) => segments[index] === segment)) {
    baseSegments.pop();
  }
}
const basePackage = baseSegments.join('.');

if (!basePackage) fail(`找不到 Java package: ${javaRoot}`);

const packageRoot = basePackage
  ? basePackage.split('.').reduce((path, segment) => join(path, segment), javaRoot)
  : javaRoot;
const packageSegmentCount = basePackage ? basePackage.split('.').length : 0;
for (const required of ['controller', 'usecase', 'service', 'dao', 'mapper']) {
  if (!existsSync(join(packageRoot, required))) fail(`layered 模块缺少目录: ${relative(moduleRoot, join(packageRoot, required))}`);
}

const forbiddenByLayer = {
  entry: [
    /\.dao\./,
    /\.mapper\./,
    /\.service\./,
    /\.domain\.model\.read\./,
    /org\.namewta\.system\.api\./,
    /org\.namewta\.workflow\.api\.WorkflowService/,
    /org\.namewta\.common\.(json|redis|notify|oss)\./,
  ],
  usecase: [
    /\.dao\./,
    /\.mapper\./,
    /\.gateway\./,
    /\.store\./,
    /\.provider\./,
    /\.service\.impl\./,
    /org\.namewta\.common\.mybatis\./,
    /com\.baomidou\.(?!dynamic\.datasource\.annotation\.DSTransactional\b)/,
    /tools\.jackson\./,
    /org\.namewta\.system\.api\./,
    /org\.namewta\.workflow\.api\./,
    /org\.namewta\.common\.(satoken|redis|notify|oss|log)\./,
  ],
  service: [
    /\.service\./,
    /\.mapper\./,
    /com\.baomidou\.mybatisplus\./,
    /org\.mybatis\./,
    /\bIService\b/,
    /\bServiceImpl\b/,
    /\.service\.impl\./,
    /org\.namewta\.common\.mybatis\.(?!utils\.IdGeneratorUtil\b)/,
    /tools\.jackson\./,
  ],
  dao: [
    /\.dao\./,
    /\.usecase\./,
    /\.service\./,
    /\.gateway\./,
    /\.store\./,
    /\.provider\./,
    /org\.namewta\.system\.api\./,
    /org\.namewta\.workflow\.api\./,
  ],
  mapper: [
    /\.dao\./,
    /\.usecase\./,
    /\.service\./,
    /\.gateway\./,
    /\.store\./,
    /\.provider\./,
  ],
  port: [
    /\.controller\./,
    /\.listener\./,
    /\.usecase\./,
    /\.service\./,
    /\.dao\./,
    /\.mapper\./,
    /\.adapter\./,
    /org\.springframework\./,
    /com\.baomidou\./,
    /org\.mybatis\./,
    /org\.namewta\.common\.(mybatis|redis|notify|oss|satoken|web)\./,
  ],
  adapter: [
    /\.controller\./,
    /\.listener\./,
    /\.usecase\./,
    /\.service\./,
    /\.dao\./,
    /\.mapper\./,
  ],
  support: [
    /\.controller\./,
    /\.listener\./,
    /\.usecase\./,
    /\.service\./,
    /\.dao\./,
    /\.mapper\./,
    /\.adapter\./,
    /\.gateway\./,
    /\.provider\./,
    /\.store\./,
    /org\.springframework\./,
    /com\.baomidou\.(mybatis|dynamic)/,
    /org\.mybatis\./,
    /org\.namewta\.common\.mybatis\./,
    /org\.namewta\.common\.(redis|notify|oss|satoken|web)\./,
  ],
  domain: [
    /\.controller\./,
    /\.listener\./,
    /\.usecase\./,
    /\.service\./,
    /\.dao\./,
    /\.mapper\./,
    /org\.springframework\./,
    /com\.baomidou\.mybatisplus\.(core|extension)\./,
    /org\.mybatis\./,
    /org\.namewta\.common\.mybatis\.(?!core\.domain\.BaseEntity)/,
    /org\.namewta\.common\.(redis|notify|oss|satoken|web)\./,
  ],
};

const requiredChain = [
  ['entry', 'usecase', /\.usecase\./],
  ['usecase', 'service', /\.service\./],
  ['service', 'dao', /\.dao\./],
  ['dao', 'mapper', /\.mapper\./],
];
const observedImports = new Map(requiredChain.map(([from]) => [from, false]));

// 仅保留 Profile 当前迁移窗口的两个聚合读模型兼容路径；不把例外扩展到新模块。
const legacyReadModelAllowList = new Set([
  'org/namewta/profile/person/domain/model/read/PersonAdminRows.java',
  'org/namewta/profile/enterprise/domain/model/read/EnterpriseAdminRows.java',
]);

/**
 * 判断 import 是否属于当前模块或项目边界。第三方框架也必须参与分层检查，
 * 否则 `com.baomidou.*` 等基础设施依赖会绕过静态门禁。
 */
function isBoundaryImport(imported) {
  return Boolean(basePackage && imported.startsWith(`${basePackage}.`))
    || /^(org\.namewta\.|com\.baomidou\.|org\.mybatis\.|org\.springframework\.|tools\.jackson\.)/.test(imported);
}

const serviceTypeNames = new Set(
  javaFiles
    .filter(file => {
      const path = relative(javaRoot, file).replaceAll('\\', '/');
      const segments = path.split('/');
      return segments.length === packageSegmentCount + 2 && segments[packageSegmentCount] === 'service';
    })
    .map(file => sources.get(file).match(/\b(?:class|interface|record|enum)\s+(\w+)/)?.[1])
    .filter(Boolean),
);

for (const file of javaFiles) {
  const source = sources.get(file);
  const relativePath = relative(javaRoot, file).replaceAll('\\', '/');
  const segments = relativePath.split('/');
  const root = segments[packageSegmentCount];
  const isApiAdapter = (root === 'adapter' && segments[packageSegmentCount + 1] === 'api')
    || (root === 'api' && segments[packageSegmentCount + 1] === 'adapter');
  const layer = root === 'controller' || root === 'listener' || isApiAdapter ? 'entry' : root;
  const patterns = forbiddenByLayer[layer];

  if (layer === 'service') {
    const filename = segments.at(-1) ?? '';
    if (!/Service\.java$/.test(filename) || /ServiceImpl\.java$/.test(filename)) {
      fail(`${relativePath}: layered service 目录只能放语义明确的 *Service，不得放 ServiceImpl/Provider/Gateway/Store 等适配实现`);
    }
    const sourceWithoutImports = source.replace(/^\s*import\s+[^;]+;\s*$/gm, '');
    for (const serviceType of serviceTypeNames) {
      if (serviceType !== filename.replace(/\.java$/, '')
          && new RegExp(`\\b${serviceType}\\s+\\w+\\b`).test(sourceWithoutImports)) {
        fail(`${relativePath}: Service 不得调用同层 Service/服务合同 ${serviceType}`);
      }
    }
  }

  if (layer === 'support') {
    if (/@(?:Component|Service|Repository|Configuration|Bean)\b/.test(source)) {
      fail(`${relativePath}: support 只能是纯辅助，不得声明 Spring Bean`);
    }
  }

  if (layer === 'domain' && segments[packageSegmentCount + 1] === 'model'
      && segments[packageSegmentCount + 2] === 'read') {
    const filename = segments.at(-1) ?? '';
    const isLegacyReadModel = legacyReadModelAllowList.has(relativePath);
    if ((!/(?:Row|Projection)\.java$/.test(filename) || /Rows\.java$/.test(filename))
        && !isLegacyReadModel) {
      fail(`${relativePath}: domain/model/read 类型必须使用单数 *Row 或 *Projection 命名`);
    }
  }

  if (['entry', 'service', 'dao', 'mapper', 'support', 'port', 'adapter', 'domain'].includes(layer)
      && /@(?:com\.baomidou\.dynamic\.datasource\.annotation\.)?DSTransactional\b/.test(source)) {
    fail(`${relativePath}: @DSTransactional 事务边界必须位于 UseCase`);
  }
  if (/@(?:org\.springframework\.transaction\.annotation\.)?Transactional\b/.test(source)) {
    fail(`${relativePath}: 必须使用 @DSTransactional，Spring @Transactional 不属于 layered 新模块事务合同`);
  }

  if (!patterns) continue;

  const imports = [...source.matchAll(/^\s*import\s+(?:static\s+)?([^;]+);/gm)].map(match => match[1].trim());
  for (const [from, , pattern] of requiredChain) {
    if (layer === from && imports.some(imported => basePackage && imported.startsWith(`${basePackage}.`) && pattern.test(imported))) {
      observedImports.set(from, true);
    }
  }
  for (const imported of imports) {
    const matched = patterns.find(pattern => {
      if (pattern.test(imported)) {
        return isBoundaryImport(imported);
      }
      return false;
    });
    if (matched) {
      fail(`${relativePath}: ${layer} 层禁止依赖 ${imported}`);
    }
  }
}

for (const [from, to] of requiredChain) {
  if (!observedImports.get(from)) fail(`layered 模块未发现 ${from} -> ${to} 的实际依赖`);
}

if (failures.length) {
  failures.forEach(message => process.stderr.write(`ERROR: ${message}\n`));
  process.exit(1);
}

process.stdout.write(`Validated layered module: ${moduleArg} (${javaFiles.length} Java files)\n`);
