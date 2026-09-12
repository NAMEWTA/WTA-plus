import fs from 'node:fs';
import path from 'node:path';

export const MODES = new Set(['audit', 'takeover', 'fresh-dev', 'release-prod', 'upgrade', 'rollback']);
export const ENVIRONMENTS = new Set(['dev', 'prod']);

export function parseArgs(argv) {
  const result = {};
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    if (!token.startsWith('--')) throw new Error(`无法识别的参数：${token}`);
    const key = token.slice(2);
    const value = argv[index + 1];
    if (!value || value.startsWith('--')) throw new Error(`参数 --${key} 缺少值`);
    result[key] = value;
    index += 1;
  }
  return result;
}

export function requireArg(args, key) {
  if (!args[key]) throw new Error(`缺少必需参数 --${key}`);
  return path.resolve(args[key]);
}

export function readJson(file) {
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch (error) {
    throw new Error(`无法读取 JSON 文件 ${file}：${error.message}`);
  }
}

export function readEnv(file) {
  const result = {};
  const text = fs.readFileSync(file, 'utf8');
  for (const rawLine of text.split(/\r?\n/u)) {
    let line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    if (line.startsWith('export ')) line = line.slice(7).trim();
    const separator = line.indexOf('=');
    if (separator <= 0) continue;
    const key = line.slice(0, separator).trim();
    let value = line.slice(separator + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    result[key] = value;
  }
  return result;
}

export function get(object, dottedPath) {
  return dottedPath.split('.').reduce((current, key) => current?.[key], object);
}

export function isPlaceholder(value) {
  if (typeof value !== 'string') return false;
  return /请替换|请仅在本地替换|replace-|managed-by|由 Fileterm 托管或/u.test(value);
}

function isPathInside(root, candidate) {
  if (typeof root !== 'string' || typeof candidate !== 'string' || !path.posix.isAbsolute(candidate)) return false;
  const relative = path.posix.relative(root, candidate);
  return relative !== '' && relative !== '..' && !relative.startsWith('../') && !path.posix.isAbsolute(relative);
}

function validateUniqueStringList(value, key, errors, { minimum = 1 } = {}) {
  if (!Array.isArray(value) || value.length < minimum || value.some((item) => typeof item !== 'string' || !item)) {
    errors.push(`${key} 必须包含至少 ${minimum} 个非空字符串`);
    return [];
  }
  if (new Set(value).size !== value.length) errors.push(`${key} 不得包含重复值`);
  return value;
}

function validateReleaseProfile(profile, errors) {
  const compose = profile.release?.compose;
  const frontend = profile.release?.frontend;
  const rollout = profile.release?.rollout;
  const openApi = profile.capabilities?.openApi;
  const root = profile.server?.root;

  if (compose?.identityConfirmed !== true) errors.push('release.compose.identityConfirmed 必须显式为 true');
  if (!compose?.project) errors.push('缺少字段 release.compose.project');
  else if (!profile.ownership?.composeProjects?.includes(compose.project)) {
    errors.push('release.compose.project 必须存在于 ownership.composeProjects');
  }

  const composeFiles = validateUniqueStringList(compose?.files, 'release.compose.files', errors);
  for (const file of composeFiles) {
    if (isPlaceholder(file)) errors.push(`release.compose.files 仍含占位值：${file}`);
    else if (!isPathInside(root, file)) errors.push(`Compose 文件必须位于授权根目录 ${root}：${file}`);
  }
  if (!compose?.envFile) errors.push('缺少字段 release.compose.envFile');
  else if (isPlaceholder(compose.envFile) || !isPathInside(root, compose.envFile)) {
    errors.push(`release.compose.envFile 必须位于授权根目录 ${root}`);
  }
  validateUniqueStringList(compose?.backendServices, 'release.compose.backendServices', errors, { minimum: 2 });
  validateUniqueStringList(compose?.frontendServices, 'release.compose.frontendServices', errors);

  const adminImage = profile.release?.images?.admin;
  if (!adminImage || isPlaceholder(adminImage) || !adminImage.includes(':')) {
    errors.push('release.images.admin 必须是带不可变标签的非占位镜像');
  }

  if (!ENVIRONMENTS.has(frontend?.buildEnvironment)) {
    errors.push('release.frontend.buildEnvironment 必须为 dev 或 prod');
  }
  const expectedContext = `/${profile.routes?.adminPrefix}/`;
  if (frontend?.contextPath !== expectedContext) {
    errors.push(`release.frontend.contextPath 必须为 ${expectedContext}`);
  }
  const expectedBaseApi = `${expectedContext}${frontend?.buildEnvironment}-api`;
  if (frontend?.baseApi !== expectedBaseApi) {
    errors.push(`release.frontend.baseApi 必须为 ${expectedBaseApi}`);
  }
  const expectedAssetPrefix = `${expectedContext}assets/`;
  if (frontend?.assetPrefix !== expectedAssetPrefix) {
    errors.push(`release.frontend.assetPrefix 必须为 ${expectedAssetPrefix}`);
  }

  if (!Number.isInteger(rollout?.maxRestartCount) || rollout.maxRestartCount < 0) {
    errors.push('release.rollout.maxRestartCount 必须是非负整数');
  }
  for (const key of ['probeIntervalSeconds', 'timeoutSeconds', 'requiredConsecutiveSuccesses']) {
    if (!Number.isInteger(rollout?.[key]) || rollout[key] < 1) {
      errors.push(`release.rollout.${key} 必须是正整数`);
    }
  }
  if (Number.isInteger(rollout?.timeoutSeconds) && Number.isInteger(rollout?.probeIntervalSeconds)
    && Number.isInteger(rollout?.requiredConsecutiveSuccesses)
    && rollout.timeoutSeconds < rollout.probeIntervalSeconds * rollout.requiredConsecutiveSuccesses) {
    errors.push('release.rollout.timeoutSeconds 必须覆盖完整连续成功窗口');
  }

  if (openApi?.kek !== undefined) errors.push('profile 不得包含 OpenAPI KEK 正文');
  if (typeof openApi?.enabled !== 'boolean') errors.push('capabilities.openApi.enabled 必须是布尔值');
  if (openApi?.enabled) {
    if (!openApi.kekVersion || isPlaceholder(openApi.kekVersion)) errors.push('启用 OpenAPI 时 capabilities.openApi.kekVersion 必填');
    if (openApi.kekPresenceRequired !== true) errors.push('启用 OpenAPI 时 capabilities.openApi.kekPresenceRequired 必须为 true');
    if (!openApi.secretSource || isPlaceholder(openApi.secretSource)) errors.push('启用 OpenAPI 时 capabilities.openApi.secretSource 必填');
  }
}

export function validateProfile(profile) {
  const errors = [];
  const required = [
    'deployment.name', 'deployment.mode', 'deployment.environment', 'deployment.releaseId',
    'server.host', 'server.sshPort', 'server.sshUser', 'server.root',
    'network.dockerNetwork', 'network.serverName', 'network.ports',
    'routes.adminPrefix', 'services.mysqlDatabase', 'services.mysqlUser',
    'services.minioBucket', 'services.minioEndpoint', 'services.minioDiagnosticObject'
  ];
  for (const key of required) {
    const value = get(profile, key);
    if (value === undefined || value === null || value === '') errors.push(`缺少字段 ${key}`);
  }

  if (![1, 2].includes(profile.schemaVersion)) errors.push('schemaVersion 必须为 1 或 2');
  if (!MODES.has(profile.deployment?.mode)) errors.push(`不支持的部署模式：${profile.deployment?.mode}`);
  if (!ENVIRONMENTS.has(profile.deployment?.environment)) errors.push(`不支持的环境：${profile.deployment?.environment}`);
  if (isPlaceholder(profile.deployment?.releaseId)) errors.push('deployment.releaseId 仍是占位值');

  const root = profile.server?.root;
  if (typeof root === 'string') {
    if (!path.posix.isAbsolute(root)) errors.push('server.root 必须是绝对路径');
    if (['/', '/root', '/home', '/Users'].includes(root) || root.includes('$') || root.includes('..')) {
      errors.push(`server.root 不安全：${root}`);
    }
  }

  const sshPort = profile.server?.sshPort;
  if (!Number.isInteger(sshPort) || sshPort < 1 || sshPort > 65535) errors.push('server.sshPort 必须是 1-65535 的整数');

  const ports = profile.network?.ports ?? {};
  const used = new Map();
  for (const [name, port] of Object.entries(ports)) {
    if (!Number.isInteger(port) || port < 1 || port > 65535) {
      errors.push(`端口 ${name} 必须是 1-65535 的整数`);
      continue;
    }
    if (used.has(port)) errors.push(`端口 ${port} 被 ${used.get(port)} 和 ${name} 重复使用`);
    used.set(port, name);
  }

  if (profile.services?.nacosInternalOnly && (Object.values(ports).includes(8848) || Object.values(ports).includes(9848))) {
    errors.push('nacosInternalOnly=true 时不得发布宿主机 8848/9848');
  }
  if (!/^[A-Za-z0-9][A-Za-z0-9-]*$/u.test(profile.routes?.adminPrefix ?? '')) {
    errors.push('routes.adminPrefix 只能包含字母、数字和连字符，且不含首尾斜杠');
  }
  if (profile.schemaVersion === 2) validateReleaseProfile(profile, errors);
  return errors;
}

export function validateSecrets(secrets, { allowManagedSsh = true } = {}) {
  const required = [
    'mysqlRootPassword', 'mysqlAppPassword', 'redisPassword', 'minioRootUser',
    'minioRootPassword', 'monitorUsername', 'monitorPassword', 'grafanaUsername', 'grafanaPassword'
  ];
  const errors = [];
  for (const key of required) {
    if (!secrets[key] || isPlaceholder(secrets[key])) errors.push(`敏感字段 ${key} 缺失或仍是占位值`);
  }
  if (!allowManagedSsh && (!secrets.sshPassword || isPlaceholder(secrets.sshPassword))) {
    errors.push('敏感字段 sshPassword 缺失或仍是占位值');
  }
  return errors;
}

export function secretsFromEnv(env) {
  return {
    sshPassword: env.SSH_PASSWORD,
    mysqlRootPassword: env.MYSQL_ROOT_PASSWORD,
    mysqlAppPassword: env.MYSQL_APP_PASSWORD,
    redisPassword: env.REDIS_PASSWORD,
    minioRootUser: env.MINIO_ROOT_USER,
    minioRootPassword: env.MINIO_ROOT_PASSWORD,
    nacosDatabaseUser: env.NACOS_DB_USER,
    nacosDatabasePassword: env.NACOS_DB_PASSWORD,
    nacosUsername: env.NACOS_CONFIG_USERNAME,
    nacosPassword: env.NACOS_CONFIG_PASSWORD,
    nacosConfigReaderUsername: env.NACOS_CONFIG_READER_USERNAME,
    nacosConfigReaderPassword: env.NACOS_CONFIG_READER_PASSWORD,
    nacosAuthToken: env.NACOS_AUTH_TOKEN,
    nacosIdentityKey: env.NACOS_AUTH_IDENTITY_KEY,
    nacosIdentityValue: env.NACOS_AUTH_IDENTITY_VALUE,
    monitorUsername: env.MONITOR_USERNAME,
    monitorPassword: env.MONITOR_PASSWORD,
    grafanaUsername: env.GRAFANA_ADMIN_USER,
    grafanaPassword: env.GRAFANA_ADMIN_PASSWORD
  };
}

export function renderTemplate(template, values) {
  const missing = new Set();
  const rendered = template.replace(/\{\{([A-Za-z0-9]+)\}\}/gu, (_, key) => {
    if (values[key] === undefined || values[key] === null) {
      missing.add(key);
      return `{{${key}}}`;
    }
    return String(values[key]);
  });
  if (missing.size) throw new Error(`模板缺少变量：${[...missing].join(', ')}`);
  return rendered;
}

export function writePrivateFile(file, content) {
  fs.mkdirSync(path.dirname(file), { recursive: true, mode: 0o700 });
  fs.writeFileSync(file, content, { encoding: 'utf8', mode: 0o600 });
  fs.chmodSync(file, 0o600);
}

export function yamlSingleQuote(value) {
  return String(value).replaceAll("'", "''");
}

export function markdownCell(value) {
  return String(value ?? '未提供').replaceAll('|', '\\|').replaceAll('\n', '<br>');
}
