#!/usr/bin/env node
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { isPlaceholder, parseArgs, readJson, requireArg } from './lib.mjs';

function sameOrderedValues(actual, expected) {
  return Array.isArray(actual) && Array.isArray(expected)
    && actual.length === expected.length
    && actual.every((value, index) => value === expected[index]);
}

function isSha256(value, { prefixed = false } = {}) {
  const pattern = prefixed ? /^sha256:[0-9a-f]{64}$/u : /^[0-9a-f]{64}$/u;
  return typeof value === 'string' && pattern.test(value);
}

function verifyReleaseState(state, profile, errors) {
  if (state.schemaVersion !== 2) {
    errors.push('v2 发布候选必须使用 deployment-state schemaVersion=2');
    return;
  }

  const compose = state.compose;
  if (compose?.identityConfirmed !== true) errors.push('候选 Compose identityConfirmed 必须为 true');
  if (compose?.rendered !== true) errors.push('候选 Compose rendered 必须为 true');
  if (!compose?.project) errors.push('候选缺少 Compose project');
  if (!Array.isArray(compose?.files) || compose.files.length === 0) errors.push('候选缺少 Compose 文件列表');
  if (!compose?.envFile) errors.push('候选缺少 Compose envFile');

  if (profile) {
    const expected = profile.release?.compose;
    if (compose?.project !== expected?.project) errors.push(`Compose project 不一致：${compose?.project ?? '未记录'}`);
    if (!sameOrderedValues(compose?.files, expected?.files)) errors.push('Compose 文件顺序与 profile 不一致');
    if (compose?.envFile !== expected?.envFile) errors.push('Compose envFile 与 profile 不一致');
  }

  const backendNames = ['backend1', 'backend2'];
  const imageIds = [];
  const environmentDigests = [];
  const maxRestartCount = profile?.release?.rollout?.maxRestartCount ?? 0;
  for (const name of backendNames) {
    const service = state.services?.[name];
    if (!service) continue;
    if (profile && service.image !== profile.release?.images?.admin) errors.push(`服务 ${name} 镜像标签与 profile 不一致`);
    if (!isSha256(service.imageId, { prefixed: true })) errors.push(`服务 ${name} 缺少合法 image ID`);
    else imageIds.push(service.imageId);
    if (!Number.isInteger(service.restartCount) || service.restartCount < 0 || service.restartCount > maxRestartCount) {
      errors.push(`服务 ${name} restartCount 超过允许值 ${maxRestartCount}`);
    }
    if (!service.environmentDigest) errors.push(`服务 ${name} 缺少环境摘要`);
    else environmentDigests.push(service.environmentDigest);
  }
  if (imageIds.length === 2 && new Set(imageIds).size !== 1) errors.push('两套后端实例的 image ID 不一致');
  if (environmentDigests.length === 2 && new Set(environmentDigests).size !== 1) errors.push('两套后端实例的环境摘要未收敛');

  const requiredTargets = new Set(['backend1', 'backend2', 'ingress']);
  let authenticatedProbeCount = 0;
  for (const [name, probe] of Object.entries(state.semanticProbes ?? {})) {
    if (probe.required === false) continue;
    requiredTargets.delete(probe.target);
    if (probe.authenticated === true) authenticatedProbeCount += 1;
    if (probe.ok !== true) errors.push(`语义探针 ${name} 未通过`);
    if (!Array.isArray(probe.expectedHttpStatuses) || !probe.expectedHttpStatuses.includes(probe.httpStatus)) {
      errors.push(`语义探针 ${name} 的 HTTP 状态不在允许集合`);
    }
    if (!Array.isArray(probe.expectedBusinessCodes) || !probe.expectedBusinessCodes.includes(probe.businessCode)) {
      errors.push(`语义探针 ${name} 的业务码不在允许集合`);
    }
  }
  for (const target of requiredTargets) errors.push(`缺少 ${target} 语义探针`);
  if (authenticatedProbeCount < 1) errors.push('至少需要一个认证语义探针');

  const frontend = state.frontend;
  if (!isSha256(frontend?.indexSha256)) errors.push('前端缺少合法 index SHA-256');
  if (profile) {
    for (const key of ['contextPath', 'baseApi', 'assetPrefix']) {
      if (frontend?.[key] !== profile.release?.frontend?.[key]) errors.push(`前端 ${key} 与 profile 不一致`);
    }
  }
  if (!Array.isArray(frontend?.observedAssetPaths) || frontend.observedAssetPaths.length === 0) {
    errors.push('前端未记录构建资源路径');
  } else if (frontend.observedAssetPaths.some((item) => !item.startsWith(frontend.assetPrefix ?? ''))) {
    errors.push('前端资源路径不符合 assetPrefix');
  }
  const stableWindow = frontend?.stableWindow;
  const requiredSuccesses = profile?.release?.rollout?.requiredConsecutiveSuccesses ?? stableWindow?.requiredConsecutiveSuccesses;
  if (!Number.isInteger(requiredSuccesses) || requiredSuccesses < 1) errors.push('前端稳定窗口缺少连续成功阈值');
  if (!Number.isInteger(stableWindow?.observedConsecutiveSuccesses)
    || stableWindow.observedConsecutiveSuccesses < requiredSuccesses) {
    errors.push(`前端连续成功不足 ${requiredSuccesses} 次`);
  }
  if (profile && stableWindow?.timeoutSeconds !== profile.release?.rollout?.timeoutSeconds) {
    errors.push('前端稳定窗口 timeoutSeconds 与 profile 不一致');
  }
  if (!Number.isFinite(stableWindow?.elapsedSeconds) || stableWindow.elapsedSeconds < 0
    || stableWindow.elapsedSeconds > stableWindow.timeoutSeconds) {
    errors.push('前端稳定窗口 elapsedSeconds 无效或超时');
  }

  const openApi = state.capabilities?.openApi;
  const expectedOpenApi = profile?.capabilities?.openApi;
  if (openApi?.kek !== undefined) errors.push('deployment state 不得包含 OpenAPI KEK 正文');
  if (profile && openApi?.enabled !== expectedOpenApi?.enabled) errors.push('OpenAPI 启用状态与 profile 不一致');
  if (openApi?.enabled) {
    if (profile && openApi.kekVersion !== expectedOpenApi?.kekVersion) errors.push('OpenAPI KEK 版本与 profile 不一致');
    if (openApi.kekPresent !== true) errors.push('OpenAPI KEK 存在性验证未通过');
    const digests = openApi.backendDigests ?? [];
    if (digests.length !== 2 || new Set(digests).size !== 1) errors.push('两套后端实例的 OpenAPI 配置摘要未收敛');
  }

  const attempts = state.rollout?.attempts ?? [];
  const passedIndex = (target) => attempts.findIndex((item) => item.target === target && item.result === 'passed');
  const backend1Index = passedIndex('backend1');
  const backend2Index = passedIndex('backend2');
  const frontendIndex = passedIndex('frontend');
  if (backend1Index < 0 || backend2Index < 0 || frontendIndex < 0
    || !(backend1Index < backend2Index && backend2Index < frontendIndex)) {
    errors.push('rollout 必须按 backend1 -> backend2 -> frontend 记录通过尝试');
  }
  if (!Array.isArray(state.rollout?.retainedArtifacts) || state.rollout.retainedArtifacts.length === 0) {
    errors.push('rollout 必须记录保留的回滚或失败资产');
  }

  if (!state.rollback?.previousReleaseId || isPlaceholder(state.rollback.previousReleaseId)) errors.push('缺少可执行回滚的 previousReleaseId');
  if (!state.rollback?.command || isPlaceholder(state.rollback.command)) errors.push('缺少可执行回滚命令');

  if (state.database?.migrationPerformed === true) {
    const backup = state.database?.backup;
    const backupVerified = backup?.status === 'verified' && Boolean(backup.evidence);
    if (profile?.deployment?.environment === 'prod' && state.database?.waiver?.authorized) {
      errors.push('生产环境不得使用数据库无备份 waiver');
    }
    if (!backupVerified) {
      const waiver = state.database?.waiver;
      const preflight = waiver?.preflight;
      const waiverValid = profile?.deployment?.environment === 'dev'
        && waiver?.authorized === true
        && Boolean(waiver.source)
        && Boolean(waiver.scope)
        && preflight?.zeroRow === true
        && preflight?.objectIdentity === true
        && preflight?.conflictsAbsent === true
        && waiver?.recoveryMode === 'forward-only';
      if (profile?.deployment?.environment === 'prod') {
        errors.push('生产环境数据库迁移必须有已验证备份');
      } else if (!waiverValid) {
        errors.push('数据库迁移必须有已验证备份，或满足 dev 明确 waiver 与完整 preflight');
      }
    }
  }
}

export function verifyState(state, { profile } = {}) {
  const errors = [];
  const requiredServices = state.requiredServices ?? ['mysql', 'redis', 'minio', 'backend1', 'backend2', 'frontend', 'ingress'];
  for (const name of requiredServices) {
    const service = state.services?.[name];
    if (!service) errors.push(`缺少服务状态：${name}`);
    else if (!['healthy', 'running'].includes(service.status)) errors.push(`服务 ${name} 状态异常：${service.status}`);
  }

  const publishedPorts = state.publishedPorts ?? [];
  const owners = new Map();
  for (const item of publishedPorts) {
    if (owners.has(item.port)) errors.push(`端口 ${item.port} 同时属于 ${owners.get(item.port)} 与 ${item.owner}`);
    owners.set(item.port, item.owner);
  }

  if (state.oss?.enabledDefaultCount !== 1) errors.push('启用的默认 OSS 配置必须恰好一个');
  if (state.oss?.privateDefaultCount !== 1) errors.push('默认 OSS 配置必须为 PRIVATE（access_policy=0）');
  if (state.oss?.anonymousDenied !== true) errors.push('MinIO 私有探针必须拒绝匿名访问');
  if (state.oss?.signedUrlWorks !== true) errors.push('MinIO 短时签名链接验证未通过');
  if (state.oss?.signedUrlExpired !== true) errors.push('MinIO 签名链接过期验证未通过');

  if (state.nacos?.enabled) {
    const digests = state.nacos.backendDigests ?? [];
    if (digests.length < 2 || new Set(digests).size !== 1) errors.push('两套后端实例的 Nacos 配置摘要未收敛');
    if ((state.nacos.adminUserCount ?? 0) < 1) errors.push('Nacos 缺少启用的管理员账号');
    if ((state.nacos.configReaderUserCount ?? 0) < 1) errors.push('Nacos 缺少独立的应用只读配置账号');
  }

  for (const [name, endpoint] of Object.entries(state.endpoints ?? {})) {
    if (endpoint.required !== false && !endpoint.ok) errors.push(`入口 ${name} 验证失败：${endpoint.status ?? '无状态'}`);
  }
  if (state.schemaVersion === 2 || profile?.schemaVersion === 2) verifyReleaseState(state, profile, errors);
  return errors;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const args = parseArgs(process.argv.slice(2));
    const stateFile = requireArg(args, 'state');
    const errors = verifyState(readJson(stateFile));
    if (errors.length) {
      for (const error of errors) console.error(`[失败] ${error}`);
      process.exitCode = 1;
    } else {
      console.log(`[通过] 部署状态满足全部门禁：${stateFile}`);
    }
  } catch (error) {
    console.error(`[错误] ${error.message}`);
    process.exitCode = 1;
  }
}
