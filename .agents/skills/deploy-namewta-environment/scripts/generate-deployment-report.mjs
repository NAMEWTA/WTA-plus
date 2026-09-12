#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  isPlaceholder, markdownCell, parseArgs, readEnv, readJson, renderTemplate, requireArg,
  secretsFromEnv, validateProfile, writePrivateFile
} from './lib.mjs';
import { verifyState } from './verify-deployment-state.mjs';

function credential(value, managedText = '未提供；请补充本地敏感输入') {
  return typeof value === 'string' && value.trim() && !isPlaceholder(value) ? value : managedText;
}

function credentialSourceLabel(source) {
  const value = String(source ?? '').trim();
  if (!value) return '未指定凭据来源';
  if (/fileterm/iu.test(value)) return 'Fileterm 托管连接';
  if (/user-provided|用户提供/iu.test(value)) return '用户提供的 SSH 凭据';
  return value;
}

function sshCredential(profile, secrets) {
  return credential(
    secrets.sshPassword,
    `未提供；凭据来源：${credentialSourceLabel(profile.server.credentialSource)}，请在 deployment-secrets.json 或 SSH_PASSWORD 中补充`
  );
}

function statusLabel(value) {
  return {
    healthy: '健康',
    running: '运行中',
    passed: '通过',
    failed: '失败',
    skipped: '已跳过',
    pending: '待处理'
  }[value] ?? value;
}

function modeLabel(value) {
  return {
    audit: '审计',
    takeover: '接管',
    'fresh-dev': '全新开发环境',
    'release-prod': '生产发布',
    upgrade: '升级',
    rollback: '回滚'
  }[value] ?? value;
}

function environmentLabel(value) {
  return { dev: '开发环境', prod: '生产环境' }[value] ?? value;
}

function mergeSecrets(base, overrides) {
  const merged = { ...base };
  for (const [key, value] of Object.entries(overrides ?? {})) {
    if (value === undefined || value === null) continue;
    if (isPlaceholder(value) && merged[key]) continue;
    merged[key] = value;
  }
  return merged;
}

function serviceRows(state) {
  const rows = Object.entries(state.services ?? {}).map(([name, item]) =>
    `| ${markdownCell(name)} | ${markdownCell(statusLabel(item.status))} | ${markdownCell(item.version ?? item.image ?? '未记录')} | ${markdownCell(item.imageId ?? '不适用')} | ${markdownCell(item.restartCount ?? '未记录')} | ${markdownCell(item.environmentDigest ?? '未记录')} |`
  );
  return ['| 服务 | 状态 | 版本或镜像 | 镜像 ID | 重启数 | 环境摘要 |', '|---|---|---|---|---|---|', ...rows].join('\n');
}

function composeSection(profile, state) {
  const compose = state.compose ?? {};
  const files = Array.isArray(compose.files) && compose.files.length
    ? compose.files.map((item, index) => `${index + 1}. \`${markdownCell(item)}\``).join('\n')
    : '- 未记录';
  return [
    `- 身份已确认：\`${compose.identityConfirmed === true}\``,
    `- Compose 项目：\`${markdownCell(compose.project ?? '未记录')}\``,
    `- 环境文件：\`${markdownCell(compose.envFile ?? '未记录')}\``,
    `- 配置已渲染：\`${compose.rendered === true}\``,
    `- 后端目标服务：\`${markdownCell(profile.release?.compose?.backendServices?.join(', ') ?? 'v1 未声明')}\``,
    `- 前端目标服务：\`${markdownCell(profile.release?.compose?.frontendServices?.join(', ') ?? 'v1 未声明')}\``,
    '',
    'Compose 文件（顺序即执行身份）：',
    '',
    files
  ].join('\n');
}

function artifactSection(profile, state) {
  const openApi = state.capabilities?.openApi ?? {};
  return [
    `- 后端候选镜像：\`${markdownCell(profile.release?.images?.admin ?? 'v1 未声明')}\``,
    `- 前端 index SHA-256：\`${markdownCell(state.frontend?.indexSha256 ?? '未记录')}\``,
    `- 前端资源：\`${markdownCell(state.frontend?.observedAssetPaths?.join(', ') ?? '未记录')}\``,
    `- OpenAPI：启用=\`${openApi.enabled === true}\`，密钥版本=\`${markdownCell(openApi.kekVersion ?? '未记录')}\`，密钥已提供=\`${openApi.kekPresent === true}\``,
    `- OpenAPI 双实例摘要：\`${markdownCell(openApi.backendDigests?.join(', ') ?? '未记录')}\``
  ].join('\n');
}

function rolloutSection(state) {
  const rows = (state.rollout?.attempts ?? []).map((item, index) => {
    const identity = item.imageId ?? item.artifactSha256 ?? item.image ?? '未记录';
    return `| ${index + 1} | ${markdownCell(item.target)} | ${markdownCell(statusLabel(item.result))} | ${markdownCell(identity)} | ${markdownCell(item.restartCount ?? '不适用')} | ${markdownCell(item.reason ?? '无')} | ${markdownCell(item.recovery ?? '不适用')} |`;
  });
  return [
    '| 顺序 | 目标 | 结果 | 镜像或构件身份 | 重启数 | 原因 | 恢复结果 |',
    '|---|---|---|---|---|---|---|',
    ...(rows.length ? rows : ['| - | 未记录 | 未记录 | 未记录 | 未记录 | 未记录 | 未记录 |'])
  ].join('\n');
}

function frontendSection(state) {
  const frontend = state.frontend ?? {};
  const stable = frontend.stableWindow ?? {};
  return [
    `- 上下文路径：\`${markdownCell(frontend.contextPath ?? '未记录')}\``,
    `- 基础 API：\`${markdownCell(frontend.baseApi ?? '未记录')}\``,
    `- 静态资源前缀：\`${markdownCell(frontend.assetPrefix ?? '未记录')}\``,
    `- 连续成功：\`${markdownCell(stable.observedConsecutiveSuccesses ?? '未记录')} / ${markdownCell(stable.requiredConsecutiveSuccesses ?? '未记录')}\``,
    `- 窗口：\`${markdownCell(stable.elapsedSeconds ?? '未记录')}s / ${markdownCell(stable.timeoutSeconds ?? '未记录')}s\``,
    `- 瞬时失败：\`${markdownCell(JSON.stringify(stable.transientFailures ?? []))}\``
  ].join('\n');
}

function dataProtectionSection(state) {
  const database = state.database ?? {};
  const backup = database.backup ?? {};
  const waiver = database.waiver;
  const lines = [
    `- 执行数据库迁移：\`${database.migrationPerformed === true}\``,
    `- 备份状态：\`${markdownCell(backup.status ?? '未记录')}\``,
    `- 备份证据：\`${markdownCell(backup.evidence ?? '未记录')}\``
  ];
  if (!waiver) return [...lines, '- 无备份 waiver：`未使用`'].join('\n');
  return [
    ...lines,
    `- 无备份 waiver：授权=\`${waiver.authorized === true}\`，来源=\`${markdownCell(waiver.source ?? '未记录')}\`，范围=\`${markdownCell(waiver.scope ?? '未记录')}\``,
    `- Waiver preflight：zeroRow=\`${waiver.preflight?.zeroRow === true}\`，objectIdentity=\`${waiver.preflight?.objectIdentity === true}\`，conflictsAbsent=\`${waiver.preflight?.conflictsAbsent === true}\``,
    `- 恢复模式：\`${markdownCell(waiver.recoveryMode ?? '未记录')}\``
  ].join('\n');
}

function retainedArtifactSection(state) {
  const items = state.rollout?.retainedArtifacts ?? [];
  return items.length ? items.map((item) => `- \`${markdownCell(item)}\``).join('\n') : '- 未记录';
}

try {
  const args = parseArgs(process.argv.slice(2));
  const profile = readJson(requireArg(args, 'profile'));
  const state = readJson(requireArg(args, 'state'));
  const output = requireArg(args, 'output');
  const profileErrors = validateProfile(profile);
  if (profileErrors.length) throw new Error(profileErrors.join('；'));

  let secrets = {};
  if (args['env-file']) secrets = secretsFromEnv(readEnv(path.resolve(args['env-file'])));
  if (args.secrets) secrets = mergeSecrets(secrets, readJson(path.resolve(args.secrets)));

  const strictReport = profile.schemaVersion === 2 && state.schemaVersion === 2;
  const verificationErrors = strictReport ? verifyState(state, { profile }) : verifyState(state);
  const compatibilityNotice = strictReport
    ? null
    : '当前为兼容审计：严格发布报告要求 profile/state 均为 schemaVersion=2；升级前不得据此宣称发布通过。';
  const host = profile.server.host;
  const ports = profile.network.ports;
  const base = `${profile.network.tlsEnabled ? 'https' : 'http'}://${profile.network.serverName}:${profile.network.tlsEnabled ? ports.ingressHttps : ports.ingressHttp}`;
  const endpointRows = [
    ['统一入口', `${base}/${profile.routes.adminPrefix}/`, ports.ingressHttp],
    ['后端实例 1', `http://${host}:${ports.backend1}`, ports.backend1],
    ['后端实例 2', `http://${host}:${ports.backend2}`, ports.backend2],
    ['MySQL', `${host}:${ports.mysql}`, ports.mysql],
    ['Redis', `${host}:${ports.redis}`, ports.redis],
    ['MinIO API', `${host}:${ports.minioApi}`, ports.minioApi],
    ['MinIO 控制台', `${host}:${ports.minioConsole}`, ports.minioConsole],
    ['监控', `${base}${profile.routes.monitor}`, ports.monitor],
    ['Grafana', `http://${host}:${ports.grafana}`, ports.grafana],
    ['Nacos', `${base}${profile.routes.nacos}`, '仅内部容器端口']
  ].map(([name, address, port]) => `| ${name} | ${address} | ${port} |`);

  const credentialRows = [
    ['服务器 SSH', profile.server.sshUser, sshCredential(profile, secrets)],
    ['MySQL root', 'root', credential(secrets.mysqlRootPassword)],
    ['MySQL 应用', profile.services.mysqlUser, credential(secrets.mysqlAppPassword)],
    ['Redis', '无独立用户名', credential(secrets.redisPassword)],
    ['MinIO', secrets.minioRootUser ?? '未记录', credential(secrets.minioRootPassword)],
    ['Nacos 数据库', secrets.nacosDatabaseUser ?? '未记录', credential(secrets.nacosDatabasePassword)],
    ['Nacos 管理员', secrets.nacosUsername ?? '未记录', credential(secrets.nacosPassword)],
    ['Nacos 配置只读账号', secrets.nacosConfigReaderUsername ?? '未记录', credential(secrets.nacosConfigReaderPassword)],
    ['Nacos 服务鉴权令牌', '不适用', credential(secrets.nacosAuthToken)],
    ['Nacos 服务身份', credential(secrets.nacosIdentityKey), credential(secrets.nacosIdentityValue)],
    ['监控平台', secrets.monitorUsername ?? '未记录', credential(secrets.monitorPassword)],
    ['Grafana', secrets.grafanaUsername ?? '未记录', credential(secrets.grafanaPassword)],
    ['业务管理端', '由 sys_user 管理', '密码为不可逆摘要，不在部署文件保存明文'],
    ['SnailJob / SnailAI', '由各服务运行配置管理', '当前发布 env 未提供独立控制台密码，生产交付前必须核对']
  ].map((row) => `| ${row.map(markdownCell).join(' | ')} |`);

  const values = {
    deploymentName: profile.deployment.name,
    generatedAt: new Date().toISOString(),
    mode: modeLabel(profile.deployment.mode),
    environment: environmentLabel(profile.deployment.environment),
    releaseId: profile.deployment.releaseId,
    verificationStatus: compatibilityNotice
      ? '兼容审计（必须升级 v2）'
      : (verificationErrors.length ? '未通过' : (state.risks?.length ? '有条件通过（存在待复核项）' : '通过')),
    serverSection: [
      `- SSH 服务器：\`${profile.server.sshUser}@${host}:${profile.server.sshPort}\``,
      `- 授权根目录：\`${profile.server.root}\``,
      `- Docker 网络：\`${profile.network.dockerNetwork}\``,
      `- Compose 目录：\`${profile.server.root}/compose\``,
      `- 持久化目录：\`${profile.server.root}/data/{mysql,redis,minio,nacos,loki,grafana,prometheus}\``,
      '- 敏感文件权限：`0600`'
    ].join('\n'),
    endpointSection: ['| 用途 | 地址 | 端口 |', '|---|---|---|', ...endpointRows].join('\n'),
    credentialSection: ['> 本文件包含敏感信息，仅限本机保存，权限必须为 `0600`。', '', '| 系统 | 账号 | 密码或保管位置 |', '|---|---|---|', ...credentialRows].join('\n'),
    composeSection: composeSection(profile, state),
    serviceSection: serviceRows(state),
    artifactSection: artifactSection(profile, state),
    rolloutSection: rolloutSection(state),
    frontendSection: frontendSection(state),
    dataProtectionSection: dataProtectionSection(state),
    retainedArtifactSection: retainedArtifactSection(state),
    verificationSection: [
      compatibilityNotice ? `- ${compatibilityNotice}` : null,
      ...(verificationErrors.length
        ? verificationErrors.map((item) => `- 未通过：${item}`)
        : ['- 全部自动化状态门禁通过。']),
      '- 详细采集时间：`' + markdownCell(state.capturedAt ?? '未记录') + '`'
    ].filter(Boolean).join('\n'),
    evidenceSection: [
      `- 当日目录：\`${markdownCell(state.evidence?.logDirectory ?? `${profile.server.root}/log/${new Date().toISOString().slice(0, 10)}`)}\``,
      `- 日志文件：\`${markdownCell((state.evidence?.logFiles ?? []).join(', ') || '未记录')}\``,
      `- 截图文件：\`${markdownCell((state.evidence?.screenshots ?? []).join(', ') || '未记录')}\``
    ].join('\n'),
    upgradeSection: [
      '1. 记录当前清单、Schema、配置摘要与活动指针。',
      '2. 备份配置、数据库、对象存储元数据和当前发布，并验证备份。',
      '3. 构建前后端，生成清单与 SHA-256，传输到新暂存目录。',
      '4. 先执行兼容迁移，再按 server1 -> server2 滚动后端，最后原子切换前端。',
      '5. 重跑登录、业务 API、OSS 签名、Nacos、入口和监控门禁。',
      '6. 提升活动指针并生成新的升级记录。'
    ].join('\n'),
    rollbackSection: [
      `- 上一发布：\`${markdownCell(state.rollback?.previousReleaseId ?? '未登记')}\``,
      `- 回滚命令：\`${markdownCell(state.rollback?.command ?? '未登记，发布前必须补齐')}\``,
      '- 按失败实例或前端静态目录独立恢复；未失败组件不扩大回滚。',
      '- Schema 兼容时先回滚应用与配置；恢复数据必须单独授权。',
      '- 保留失败候选、旧镜像和 rollback 目录；清理必须独立授权。',
      '- 禁止执行 `docker compose down -v`。'
    ].join('\n'),
    riskSection: (state.risks?.length ? state.risks : ['当前无已登记风险']).map((item) => `- ${markdownCell(item)}`).join('\n')
  };

  const scriptDir = path.dirname(fileURLToPath(import.meta.url));
  const template = fs.readFileSync(path.resolve(scriptDir, '../assets/templates/deployment-report.md.template'), 'utf8');
  writePrivateFile(output, renderTemplate(template, values));
  console.log(`[生成] 私密部署交接文档：${output}（权限 0600，内容未回显）`);
  if (verificationErrors.length) process.exitCode = 2;
} catch (error) {
  console.error(`[错误] ${error.message}`);
  process.exitCode = 1;
}
