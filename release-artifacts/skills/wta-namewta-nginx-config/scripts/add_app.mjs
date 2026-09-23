#!/usr/bin/env node
// 将一个真实 plus-ui App 注册到 release-artifacts 的 LB + 独立 Nginx 体系。

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const RESERVED_PREFIXES = new Set([
  'admin', 'monitor', 'snail-job', 'snail-ai', 'dev-api', 'prod-api', 'actuator',
]);
const RESERVED_PORTS = new Set([
  40080, 40443, 42080, 42081, 43000, 43080, 43081, 43306, 46379, 47888, 48080, 48081,
  48800, 48888, 48900, 49000, 49001, 49002, 49003, 49090, 49091, 49200,
]);
// 锚点文本必须和现有 Nginx 模板一致，不能改成 .mjs。
const UPSTREAM_MARKER = '# APP_UPSTREAMS: add_app.py 在此处追加 App upstream。';
const ROUTE_MARKER = '    # APP_ROUTES: add_app.py 在此处追加 App 路由。';

function ancestor(file, levels) {
  let directory = path.resolve(file);
  for (let index = 0; index < levels; index += 1) directory = path.dirname(directory);
  return directory;
}

function releaseRoot(repo) {
  const nested = path.join(repo, 'release-artifacts');
  if (fs.existsSync(nested) && fs.statSync(nested).isDirectory()) return nested;
  if (fs.existsSync(path.join(repo, 'docker/docker-compose-frontend.yml'))) return repo;
  return nested;
}

const dockerRoot = (repo) => path.join(releaseRoot(repo), 'docker');
const composePath = (repo) => path.join(dockerRoot(repo), 'docker-compose-frontend.yml');
const nginxRoot = (repo) => path.join(dockerRoot(repo), 'frontend/nginx');

function lbTemplates(repo) {
  const root = path.join(nginxRoot(repo), 'lb');
  return [path.join(root, 'nginx-lb-http.conf.template'), path.join(root, 'nginx-lb-tls.conf.template')];
}

function envKey(app, suffix) {
  return `${app.replace(/[^A-Za-z0-9]/g, '_').toUpperCase()}_${suffix}`;
}

function upstreamKey(app) {
  return app.toLowerCase().replace(/[^a-z0-9]/g, '_').replace(/^_+|_+$/g, '');
}

function generateSensitivePrefix() {
  const alphabet = 'abcdefghijklmnopqrstuvwxyz0123456789';
  return Array.from({ length: 10 }, () => alphabet[crypto.randomInt(alphabet.length)]).join('');
}

function parseEnv(file) {
  if (!fs.existsSync(file)) return {};
  const result = {};
  for (const raw of fs.readFileSync(file, 'utf8').split(/\r\n|\n|\r/)) {
    const line = raw.trim();
    if (!line || line.startsWith('#') || !line.includes('=')) continue;
    const index = line.indexOf('=');
    result[line.slice(0, index).trim()] = line.slice(index + 1).trim().replace(/^['"]+|['"]+$/g, '');
  }
  return result;
}

function usedPorts(repo, excludeKey = null) {
  const text = fs.readFileSync(composePath(repo), 'utf8');
  const values = new Set(RESERVED_PORTS);
  for (const match of text.matchAll(/\$\{([A-Z0-9_]+):-([0-9]+)\}:[0-9]+/g)) {
    if (match[1] !== excludeKey) values.add(Number(match[2]));
  }
  return values;
}

function allocatePort(repo) {
  const used = usedPorts(repo);
  let port = 41080;
  while (used.has(port)) port += 1;
  if (port >= 42000) throw new Error('41080-41999 App 端口段已用完');
  return port;
}

function configuredApps(repo) {
  const document = JSON.parse(fs.readFileSync(path.join(releaseRoot(repo), 'apps.json'), 'utf8'));
  return document.apps.filter((app) => app.shipped).map((app) => app.id).sort();
}

function quote(value) {
  return `'${String(value).replaceAll('\\', '\\\\').replaceAll("'", "\\'")}'`;
}

function pad(value, width) {
  const text = String(value);
  return text.length >= width ? text : text.padEnd(width, ' ');
}

class Writer {
  constructor(dryRun) {
    this.dryRun = dryRun;
    this.changes = [];
  }

  write(file, content) {
    const current = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : null;
    if (current === content) {
      console.log(`[SKIP] ${file}`);
      return;
    }
    this.changes.push(file);
    if (this.dryRun) {
      console.log(`[DRY] write ${file}`);
      return;
    }
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, content);
    console.log(`[WRITE] ${file}`);
  }

  ensureDir(file) {
    if (this.dryRun) {
      console.log(`[DRY] mkdir ${file}`);
      return;
    }
    fs.mkdirSync(file, { recursive: true });
  }
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function upsertEnv(file, key, value, writer) {
  const text = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '';
  const line = `${key}=${value}`;
  const pattern = new RegExp(`^${escapeRegExp(key)}=.*$`, 'm');
  const updated = pattern.test(text)
    ? text.replace(new RegExp(`^${escapeRegExp(key)}=.*$`, 'gm'), line)
    : `${text}${!text || text.endsWith('\n') ? '' : '\n'}${line}\n`;
  writer.write(file, updated);
}

function patchCompose(repo, app, port, registration, writer) {
  const file = composePath(repo);
  let text = fs.readFileSync(file, 'utf8');
  const prefixKey = envKey(app, 'PREFIX');
  const portKey = envKey(app, 'PORT');
  const lbEnvKey = `APP_${prefixKey}`;
  if (registration.ingress === 'lb' && !text.includes(`      ${lbEnvKey}:`)) {
    const anchor = '      APP_ADMIN_WEB_PREFIX: "${ADMIN_WEB_PREFIX:?ADMIN_WEB_PREFIX is required}"';
    if (text.split(anchor).length - 1 !== 2) throw new Error('Compose LB env 锚点数量异常，拒绝自动修改');
    const envLine = `      ${lbEnvKey}: "\${${prefixKey}:?${prefixKey} is required}"`;
    text = text.replaceAll(anchor, `${anchor}\n${envLine}`);
  }

  const serviceName = `namewta-nginx-${app}`;
  if (!text.includes(`  ${serviceName}:\n`)) {
    const marker = '\nnetworks:\n';
    if (!text.includes(marker)) throw new Error('Compose 缺少 networks 锚点，拒绝自动修改');
    const service = `
  ${serviceName}:
    image: "\${NGINX_IMAGE:-nginx:1.31.1}"
    container_name: namewta-nginx-${app}
    environment:
      TZ: Asia/Shanghai
      APP_PREFIX: "\${${prefixKey}:?${prefixKey} is required}"
      BACKEND_SERVER1: "\${BACKEND_SERVER1:-namewta-server1:8080}"
      BACKEND_SERVER2: "\${BACKEND_SERVER2:-namewta-server2:8080}"
      NGINX_ENVSUBST_FILTER: "^(APP_|BACKEND_|LB_)"
    ports:
      - "\${NAMEWTA_BIND_HOST:-127.0.0.1}:\${${portKey}:-${port}}:80"
    volumes:
      - ./frontend/nginx/apps/nginx-${app}.conf.template:/etc/nginx/templates/default.conf.template:ro
      - ./frontend/nginx/html/${app}:/usr/share/nginx/html:ro
      - \${NAMEWTA_DATA_ROOT:-./runtime}/nginx/log/${app}:/var/log/nginx
    healthcheck:
      test: ["CMD", "curl", "--fail", "--silent", "http://127.0.0.1/healthz"]
      interval: 15s
      timeout: 5s
      retries: 3
    logging: *nginx-logging
    restart: unless-stopped
    networks: [namewta]
`;
    text = text.replace(marker, `${service}${marker}`);
  }

  const tls = registration.tls;
  if (tls && !text.includes(`  ${tls.composeService}:\n`)) {
    const service = `
  ${tls.composeService}:
    image: "\${NGINX_IMAGE:-nginx:1.31.1}"
    container_name: ${tls.composeService}
    profiles: [tls]
    environment:
      TZ: Asia/Shanghai
      APP_PREFIX: "\${${prefixKey}:?${prefixKey} is required}"
      BACKEND_SERVER1: "\${BACKEND_SERVER1:-namewta-server1:8080}"
      BACKEND_SERVER2: "\${BACKEND_SERVER2:-namewta-server2:8080}"
      NGINX_ENVSUBST_FILTER: "^(APP_|BACKEND_|LB_)"
    ports:
      - "\${NAMEWTA_BIND_HOST:-127.0.0.1}:\${${tls.portEnv}:-${tls.defaultPort}}:443"
    volumes:
      - ./${tls.nginxTemplate.replace(/^docker\//, '')}:/etc/nginx/templates/default.conf.template:ro
      - ./frontend/nginx/html/${app}:/usr/share/nginx/html:ro
      - \${NAMEWTA_CERT_ROOT:-./frontend/nginx/cert}/${app}:/etc/nginx/cert/${app}:ro
      - \${NAMEWTA_DATA_ROOT:-./runtime}/nginx/log/${app}-tls:/var/log/nginx
    healthcheck:
      test: ["CMD", "curl", "--fail", "--silent", "http://127.0.0.1/healthz"]
      interval: 15s
      timeout: 5s
      retries: 3
    logging: *nginx-logging
    restart: unless-stopped
    networks: [namewta]
`;
    text = text.replace('\nnetworks:\n', `${service}\nnetworks:\n`);
  }

  const bindAppEnvironment = (block) => {
    if (!/^      APP_PREFIX:/m.test(block)) return block;
    const bindings = {
      BACKEND_SERVER1: '"${BACKEND_SERVER1:-namewta-server1:8080}"',
      BACKEND_SERVER2: '"${BACKEND_SERVER2:-namewta-server2:8080}"',
    };
    if (registration.apiKind === 'sso' && block.includes(`\${${prefixKey}:?`)) {
      bindings.APP_ORIGIN = `"\${${registration.originEnv}:?${registration.originEnv} is required}"`;
    }
    let next = block;
    for (const [key, fallback] of Object.entries(bindings)) {
      const pattern = new RegExp(`^      ${key}: ([^\\n]+)\\n`, 'gm');
      const existing = [...next.matchAll(pattern)].map((match) => match[1]);
      if (new Set(existing).size > 1) throw new Error(`Compose contains conflicting ${key} bindings`);
      const value = existing.length ? existing[0] : fallback;
      next = next.replace(pattern, '');
      next = next.replace(/(^      APP_PREFIX: [^\n]+\n)/m, (item) => `${item}      ${key}: ${value}\n`);
    }
    return next;
  };
  text = text.replace(/^  namewta-nginx-[a-z0-9-]+:\n[\s\S]*?(?=^  [a-z0-9-]+:\n|^networks:\n|(?![\s\S]))/gm, bindAppEnvironment);
  text = text.replace(new RegExp(`\\$\\{${escapeRegExp(portKey)}:-\\d+\\}`, 'g'), `\${${portKey}:-${port}}`);
  writer.write(file, text);
}

function patchLbTemplate(file, app, writer) {
  let text = fs.readFileSync(file, 'utf8');
  const key = upstreamKey(app);
  const prefixKey = envKey(app, 'PREFIX');
  const lbPrefixKey = `APP_${prefixKey}`;
  if (!text.includes(`upstream app_${key} `)) {
    if (!text.includes(UPSTREAM_MARKER)) throw new Error(`${file} 缺少 upstream 锚点`);
    const upstream = `upstream app_${key} {\n    server namewta-nginx-${app}:80;\n    keepalive 32;\n}\n\n`;
    text = text.replace(UPSTREAM_MARKER, `${upstream}${UPSTREAM_MARKER}`);
  }
  if (!text.includes(`location /\${${lbPrefixKey}}/`)) {
    if (!text.includes(ROUTE_MARKER)) throw new Error(`${file} 缺少 route 锚点`);
    const route = `    location = /\${${lbPrefixKey}} {\n        return 302 /\${${lbPrefixKey}}/;\n    }\n\n    location /\${${lbPrefixKey}}/ {\n        proxy_pass http://app_${key}/;\n        proxy_http_version 1.1;\n        proxy_set_header Host $http_host;\n        proxy_set_header X-Real-IP $remote_addr;\n        proxy_set_header X-Forwarded-For $remote_addr;\n        proxy_set_header Forwarded "";\n        proxy_set_header X-Forwarded-Proto $scheme;\n        proxy_set_header X-Forwarded-Prefix /\${${lbPrefixKey}};\n        proxy_set_header Upgrade $http_upgrade;\n        proxy_set_header Connection $connection_upgrade;\n        proxy_read_timeout 86400s;\n        proxy_buffering off;\n    }\n\n`;
    text = text.replace(ROUTE_MARKER, `${route}${ROUTE_MARKER}`);
  }
  writer.write(file, text);
}

function createAppAssets(repo, app, writer) {
  const nginx = nginxRoot(repo);
  const target = path.join(nginx, 'apps', `nginx-${app}.conf.template`);
  if (!fs.existsSync(target)) {
    writer.write(target, fs.readFileSync(path.join(nginx, 'apps/nginx-admin-web.conf.template'), 'utf8'));
  }
  const gitignore = '*\n!.gitignore\n';
  writer.write(path.join(nginx, 'html', app, '.gitignore'), gitignore);
  writer.write(path.join(nginx, 'cert', app, '.gitignore'), gitignore);
  writer.ensureDir(path.join(nginx, 'log', app));
}

function updateTemplateContracts(repo, registry, writer) {
  for (const app of registry.apps) {
    const template = path.join(releaseRoot(repo), app.nginxTemplate);
    if (app.apiKind !== 'business' || !fs.existsSync(template) || !fs.statSync(template).isFile()) continue;
    let text = fs.readFileSync(template, 'utf8');
    text = text.replaceAll('server namewta-server1:8080', 'server ${BACKEND_SERVER1}');
    text = text.replaceAll('server namewta-server2:8080', 'server ${BACKEND_SERVER2}');
    if (!text.includes('log_format app_access ')) {
      text = `log_format app_access '$remote_addr $request_method $uri $status';\n\n${text}`;
      text = text.replace('    index index.html;', '    index index.html;\n    access_log /var/log/nginx/access.log app_access;');
    }
    if (!text.includes('location = /healthz')) {
      text = text.replace('    location ~ ^(/[^/]*)?/actuator', `    location = /healthz {
        access_log off;
        default_type text/plain;
        return 200 'ok\\n';
    }

    location ~ ^(/[^/]*)?/actuator`);
    }
    writer.write(template, text);
  }
  // OAuth callback queries contain one-time codes; access logs retain only the URI path.
  for (const template of lbTemplates(repo)) {
    let text = fs.readFileSync(template, 'utf8');
    if (!text.includes('log_format lb_access ')) {
      text = `log_format lb_access '$remote_addr $request_method $uri $status';\n\n${text}`;
      text = text.replace('    server_tokens off;', '    server_tokens off;\n    access_log /var/log/nginx/access.log lb_access;');
    }
    writer.write(template, text);
  }
}

function checkPrefixCollision(repo, prefix, currentKey) {
  for (const file of [path.join(releaseRoot(repo), '.env.example'), path.join(releaseRoot(repo), '.env')]) {
    for (const [key, value] of Object.entries(parseEnv(file))) {
      if (key.endsWith('_PREFIX') && key !== currentKey && value === prefix) {
        throw new Error(`前缀 ${quote(prefix)} 已被 ${key} 使用`);
      }
    }
  }
}

function commandList(repo) {
  const example = parseEnv(path.join(releaseRoot(repo), '.env.example'));
  const local = parseEnv(path.join(releaseRoot(repo), '.env'));
  console.log(`Compose: ${composePath(repo)}`);
  console.log('App 台账:');
  for (const app of configuredApps(repo)) {
    const prefixKey = envKey(app, 'PREFIX');
    const portKey = envKey(app, 'PORT');
    const prefix = local[prefixKey] ?? example[prefixKey] ?? '<missing>';
    const port = local[portKey] ?? example[portKey] ?? '<missing>';
    console.log(`  ${pad(app, 24)} prefix=${pad(quote(prefix), 32)} port=${port}`);
  }
  console.log(`下一个端口: ${allocatePort(repo)}`);
}

function usage(message) {
  const error = new Error(message);
  error.usage = true;
  throw error;
}

function parseArgs(argv) {
  const args = { list: false, sensitive: false, dryRun: false, repoRoot: null, app: null, prefix: null, port: null };
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    const take = () => {
      const value = argv[index + 1];
      if (value === undefined || value.startsWith('--')) usage(`missing value for ${token}`);
      index += 1;
      return value;
    };
    if (token === '--list') args.list = true;
    else if (token === '--sensitive') args.sensitive = true;
    else if (token === '--dry-run') args.dryRun = true;
    else if (token === '--repo-root') args.repoRoot = take();
    else if (token === '--app') args.app = take();
    else if (token === '--prefix') args.prefix = take();
    else if (token === '--port') {
      const value = take();
      if (!/^[0-9]+$/.test(value)) usage(`argument --port: invalid int value: '${value}'`);
      args.port = Number(value);
    } else usage(`unrecognized arguments: ${token}`);
  }
  return args;
}

function main() {
  const script = fileURLToPath(import.meta.url);
  const args = parseArgs(process.argv.slice(2));
  const repo = path.resolve(args.repoRoot ?? ancestor(script, 5));
  if (args.list) {
    commandList(repo);
    return;
  }
  if (!args.app) usage('--app 必填（或使用 --list）');
  if (!/^[a-z0-9][a-z0-9-]*$/.test(args.app)) usage('--app 只允许小写字母、数字和连字符');
  const packageJson = path.join(repo, 'frontend/apps', args.app, 'package.json');
  if (!fs.existsSync(packageJson) || !fs.statSync(packageJson).isFile()) usage(`App 尚不可构建或不存在 package.json: ${packageJson}`);
  if (args.sensitive && args.prefix) usage('--sensitive 与 --prefix 不能同时使用');
  const prefix = args.sensitive ? generateSensitivePrefix() : args.prefix;
  if (!prefix) usage('必须提供 --prefix 或 --sensitive');
  if (!/^[A-Za-z0-9][A-Za-z0-9-]*$/.test(prefix)) usage('prefix 只允许字母、数字和连字符');
  if (RESERVED_PREFIXES.has(prefix)) usage(`prefix ${quote(prefix)} 是保留路由`);

  const prefixKey = envKey(args.app, 'PREFIX');
  const portKey = envKey(args.app, 'PORT');
  checkPrefixCollision(repo, prefix, prefixKey);
  const registryFile = path.join(releaseRoot(repo), 'apps.json');
  const registry = JSON.parse(fs.readFileSync(registryFile, 'utf8'));
  let registration = registry.apps.find((row) => row.id === args.app) ?? null;
  const port = args.port ?? (registration ? registration.defaultPort : allocatePort(repo));
  if (port < 41080 || port >= 42000 || usedPorts(repo, portKey).has(port)) {
    usage(`端口 ${port} 不可用；App 端口必须位于 41080-41999 且未占用`);
  }
  if (!registration) {
    const packageName = JSON.parse(fs.readFileSync(packageJson, 'utf8')).name;
    registration = {
      id: args.app, package: packageName, shipped: true, prefixEnv: prefixKey,
      originEnv: envKey(args.app, 'ORIGIN'), portEnv: portKey, defaultPort: port, apiKind: 'business',
      callbackPath: '/sso/callback', composeService: `namewta-nginx-${args.app}`,
      nginxTemplate: `docker/frontend/nginx/apps/nginx-${args.app}.conf.template`, ingress: 'lb', healthPath: '/healthz',
    };
    registry.apps.push(registration);
  }
  registration.defaultPort = port;
  for (const row of registry.apps) row.apiPath = row.apiKind === 'sso' ? '/sso' : '/{prefix}/{environment}-api';
  if (registration.apiKind === 'sso' && prefix === 'sso') usage('SSO static prefix cannot collide with the /sso API namespace');
  if (registration.apiKind === 'sso') {
    for (const endpoint of [registration, ...(registration.tls ? [registration.tls] : [])]) {
      if (!fs.existsSync(path.join(releaseRoot(repo), endpoint.nginxTemplate))) {
        usage('SSO 专用模板缺失：先恢复已登记模板，不能用业务 App 模板代替');
      }
    }
  }

  console.log(`App=${args.app} prefix=/${prefix}/ port=${port} env=${prefixKey},${portKey}${args.sensitive ? ' [sensitive]' : ''}`);
  const writer = new Writer(args.dryRun);
  writer.write(registryFile, `${JSON.stringify(registry, null, 2)}\n`);
  updateTemplateContracts(repo, registry, writer);
  createAppAssets(repo, args.app, writer);
  patchCompose(repo, args.app, port, registration, writer);
  if (registration.ingress === 'lb') {
    for (const template of lbTemplates(repo)) patchLbTemplate(template, args.app, writer);
  }
  const examplePrefix = args.sensitive ? 'replace-with-private-prefix' : prefix;
  const exampleFile = path.join(releaseRoot(repo), '.env.example');
  upsertEnv(exampleFile, prefixKey, examplePrefix, writer);
  upsertEnv(exampleFile, portKey, String(port), writer);
  for (const row of registry.apps) {
    const example = parseEnv(exampleFile);
    if (!(row.originEnv in example)) upsertEnv(exampleFile, row.originEnv, `https://${row.id}.example.invalid`, writer);
    if (row.tls) upsertEnv(exampleFile, row.tls.portEnv, String(row.tls.defaultPort), writer);
  }
  // Runtime configuration is operator-owned; registering an App must never rewrite it.
  console.log(`[NEXT] 在未入库的运行 env 中设置 ${prefixKey}、${portKey} 和 ${registration.originEnv}`);
  console.log(`完成：${writer.changes.length} 个文件需要变更`);
  console.log('下一步：运行 release-artifacts/scripts/verify-release.sh 和 Compose config');
}

try {
  main();
} catch (error) {
  console.error(error.usage ? error.message : (error.stack || error.message));
  process.exit(error.usage ? 2 : 1);
}
