import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const skillDir = path.resolve(scriptDir, '..');
const imageId = `sha256:${'a'.repeat(64)}`;
const indexSha256 = 'b'.repeat(64);
const openApiKek = 'openapi-kek-secret-value';

function assertPrivateMode(file) {
  if (process.platform === 'win32') return;
  assert.equal(fs.statSync(file).mode & 0o777, 0o600);
}

function profile() {
  const value = JSON.parse(fs.readFileSync(path.join(skillDir, 'assets/templates/deployment-profile.json.template'), 'utf8'));
  value.deployment.releaseId = '20260901-namewta-report';
  value.release.compose.identityConfirmed = true;
  value.release.images.admin = 'namewta/namewta-admin:6.0.0-openapi-ae3689e';
  value.capabilities.openApi = {
    enabled: true,
    kekVersion: 'v1',
    kekPresenceRequired: true,
    secretSource: 'deployment-secrets',
  };
  return value;
}

function secrets() {
  return {
    sshPassword: 'ssh-secret',
    mysqlRootPassword: 'mysql-root-secret',
    mysqlAppPassword: 'mysql-app-secret',
    redisPassword: 'redis-secret',
    minioRootUser: 'namewta',
    minioRootPassword: 'minio-secret',
    nacosDatabaseUser: 'nacos',
    nacosDatabasePassword: 'nacos-db-secret',
    nacosUsername: 'nacos',
    nacosPassword: 'nacos-secret',
    nacosConfigReaderUsername: 'nacos-reader',
    nacosConfigReaderPassword: 'nacos-reader-secret',
    nacosAuthToken: 'token-secret',
    nacosIdentityKey: 'identity-key',
    nacosIdentityValue: 'identity-value',
    monitorUsername: 'monitor',
    monitorPassword: 'monitor-secret',
    grafanaUsername: 'admin',
    grafanaPassword: 'grafana-secret',
    openApiKek,
  };
}

function service(image = 'namewta/test:6.0.0') {
  return { status: 'healthy', image };
}

function probe(target, businessCode, { authenticated = false } = {}) {
  return {
    target,
    required: true,
    authenticated,
    ok: true,
    httpStatus: 200,
    businessCode,
    expectedHttpStatuses: [200],
    expectedBusinessCodes: [businessCode],
  };
}

function state(candidateProfile = profile()) {
  const backend = {
    status: 'healthy',
    image: candidateProfile.release.images.admin,
    imageId,
    restartCount: 0,
    environmentDigest: 'openapi-env-digest',
  };
  return {
    schemaVersion: 2,
    capturedAt: '2026-09-01T18:40:00+08:00',
    requiredServices: ['mysql', 'redis', 'minio', 'backend1', 'backend2', 'frontend', 'ingress'],
    compose: {
      identityConfirmed: true,
      project: candidateProfile.release.compose.project,
      files: [...candidateProfile.release.compose.files],
      envFile: candidateProfile.release.compose.envFile,
      rendered: true,
    },
    services: {
      mysql: service(),
      redis: service(),
      minio: service(),
      backend1: { ...backend },
      backend2: { ...backend },
      frontend: service('nginx:1.31.1'),
      ingress: service('nginx:1.31.1'),
    },
    publishedPorts: [{ port: 40080, owner: 'ingress' }, { port: 43306, owner: 'mysql' }],
    endpoints: { admin: { required: true, ok: true, status: 200 } },
    semanticProbes: {
      backend1Route: probe('backend1', 401),
      backend2Route: probe('backend2', 401),
      ingressRoute: probe('ingress', 401),
      authenticatedCatalog: probe('ingress', 200, { authenticated: true }),
    },
    frontend: {
      indexSha256,
      contextPath: candidateProfile.release.frontend.contextPath,
      baseApi: candidateProfile.release.frontend.baseApi,
      assetPrefix: candidateProfile.release.frontend.assetPrefix,
      observedAssetPaths: ['/namewta/assets/index-a1b2c3.js', '/namewta/assets/index-a1b2c3.css'],
      stableWindow: {
        requiredConsecutiveSuccesses: 3,
        observedConsecutiveSuccesses: 3,
        timeoutSeconds: 120,
        elapsedSeconds: 6,
        transientFailures: [{ httpStatus: 502, atSecond: 0 }],
      },
    },
    capabilities: {
      openApi: {
        enabled: true,
        kekVersion: 'v1',
        kekPresent: true,
        backendDigests: ['openapi-config-digest', 'openapi-config-digest'],
      },
    },
    rollout: {
      attempts: [
        { target: 'backend1', result: 'passed', imageId, restartCount: 0 },
        { target: 'backend2', result: 'passed', imageId, restartCount: 0 },
        { target: 'frontend', result: 'passed', artifactSha256: indexSha256 },
      ],
      retainedArtifacts: [
        '/data/namewta-data/releases/20260831-namewta',
        '/data/namewta-data/releases/failed-candidate',
      ],
    },
    database: {
      migrationPerformed: true,
      backup: { status: 'verified', evidence: '/data/namewta-data/backups/20260901.sql.gz' },
      waiver: null,
    },
    oss: {
      enabledDefaultCount: 1,
      privateDefaultCount: 1,
      anonymousDenied: true,
      signedUrlWorks: true,
      signedUrlExpired: true,
    },
    nacos: {
      enabled: true,
      adminUserCount: 1,
      configReaderUserCount: 1,
      backendDigests: ['nacos-digest', 'nacos-digest'],
    },
    rollback: {
      previousReleaseId: '20260831-namewta',
      command: 'bash rollback.sh 20260831-namewta',
    },
    risks: [],
  };
}

function fixture(context) {
  const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'namewta-report-'));
  context.after(() => fs.rmSync(temporary, { recursive: true, force: true }));
  const candidateProfile = profile();
  const profileFile = path.join(temporary, 'profile.json');
  const secretsFile = path.join(temporary, 'secrets.json');
  const stateFile = path.join(temporary, 'state.json');
  fs.writeFileSync(profileFile, JSON.stringify(candidateProfile));
  fs.writeFileSync(secretsFile, JSON.stringify(secrets()));
  fs.writeFileSync(stateFile, JSON.stringify(state(candidateProfile)));
  return { temporary, candidateProfile, profileFile, secretsFile, stateFile };
}

test('渲染 production 前端与 OpenAPI release env，文件均为 0600 且 stdout 不泄密', (context) => {
  const current = fixture(context);
  const output = path.join(current.temporary, 'rendered');
  const result = spawnSync(process.execPath, [path.join(scriptDir, 'render-local-config.mjs'), '--profile', current.profileFile, '--secrets', current.secretsFile, '--output', output], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);

  const names = ['release.env', 'application-local.yml', 'admin-web.env.development.local', 'admin-web.env.production.local'];
  for (const name of names) assertPrivateMode(path.join(output, name));

  const releaseEnv = fs.readFileSync(path.join(output, 'release.env'), 'utf8');
  assert.match(releaseEnv, /^NAMEWTA_ADMIN_IMAGE=namewta\/namewta-admin:6\.0\.0-openapi-ae3689e$/mu);
  assert.match(releaseEnv, /^OPENAPI_ENABLED=true$/mu);
  assert.match(releaseEnv, /^OPENAPI_KEK_VERSION=v1$/mu);
  assert.match(releaseEnv, new RegExp(`^OPENAPI_KEK=${openApiKek}$`, 'mu'));

  const frontendEnv = fs.readFileSync(path.join(output, 'admin-web.env.production.local'), 'utf8');
  assert.match(frontendEnv, /^VITE_APP_CONTEXT_PATH=\/namewta\/$/mu);
  assert.match(frontendEnv, /^VITE_APP_BASE_API=\/namewta\/prod-api$/mu);
  assert.doesNotMatch(result.stdout, new RegExp(openApiKek, 'u'));
  assert.doesNotMatch(result.stdout, /mysql-app-secret/u);
});

test('启用 OpenAPI 时缺少 KEK 必须在生成任何文件前失败', (context) => {
  const current = fixture(context);
  const localSecrets = secrets();
  delete localSecrets.openApiKek;
  fs.writeFileSync(current.secretsFile, JSON.stringify(localSecrets));
  const output = path.join(current.temporary, 'missing-kek');
  const result = spawnSync(process.execPath, [path.join(scriptDir, 'render-local-config.mjs'), '--profile', current.profileFile, '--secrets', current.secretsFile, '--output', output], { encoding: 'utf8' });
  assert.equal(result.status, 1);
  assert.match(result.stderr, /OpenAPI KEK/u);
  assert.equal(fs.existsSync(path.join(output, 'release.env')), false);
});

test('schema v1 profile 继续支持本地配置渲染', (context) => {
  const current = fixture(context);
  const legacyProfile = profile();
  legacyProfile.schemaVersion = 1;
  delete legacyProfile.release;
  delete legacyProfile.capabilities;
  fs.writeFileSync(current.profileFile, JSON.stringify(legacyProfile));
  const output = path.join(current.temporary, 'legacy-rendered');
  const result = spawnSync(process.execPath, [path.join(scriptDir, 'render-local-config.mjs'), '--profile', current.profileFile, '--secrets', current.secretsFile, '--output', output], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  const releaseEnv = fs.readFileSync(path.join(output, 'release.env'), 'utf8');
  assert.match(releaseEnv, /^OPENAPI_ENABLED=false$/mu);
  const frontendEnv = fs.readFileSync(path.join(output, 'admin-web.env.production.local'), 'utf8');
  assert.match(frontendEnv, /^VITE_APP_CONTEXT_PATH=\/namewta\/$/mu);
  assert.match(frontendEnv, /^VITE_APP_BASE_API=\/namewta\/dev-api$/mu);
});

test('严格 v2 报告记录 Compose、构件、尝试、前端、数据保护和恢复资产', (context) => {
  const current = fixture(context);
  const reportFile = path.join(current.temporary, 'deployment.md');
  const result = spawnSync(process.execPath, [path.join(scriptDir, 'generate-deployment-report.mjs'), '--profile', current.profileFile, '--secrets', current.secretsFile, '--state', current.stateFile, '--output', reportFile], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  assertPrivateMode(reportFile);
  const report = fs.readFileSync(reportFile, 'utf8');
  for (const expected of [
    'Compose 身份', '服务器 SSH', 'ssh-secret', 'Nacos 配置只读账号', 'namewta-backend', 'namewta/namewta-admin:6.0.0-openapi-ae3689e',
    imageId, indexSha256, 'backend1', 'backend2', 'frontend', '/namewta/prod-api',
    '/data/namewta-data/backups/20260901.sql.gz', '/data/namewta-data/releases/failed-candidate',
  ]) assert.match(report, new RegExp(expected.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&'), 'u'));
  assert.doesNotMatch(report, new RegExp(openApiKek, 'u'));
  assert.doesNotMatch(result.stdout, new RegExp(openApiKek, 'u'));
  assert.doesNotMatch(report, /Project|Env file|Rollout|Context path|Base API|Asset prefix|Image ID|healthy|running|passed/u);
  assert.doesNotMatch(result.stdout, /ssh-secret/u);
});

test('环境文件中的 SSH 密码不会被 secrets 模板占位值覆盖', (context) => {
  const current = fixture(context);
  const localSecrets = secrets();
  localSecrets.sshPassword = '请仅在本地替换；将写入 0600 私密交接报告';
  fs.writeFileSync(current.secretsFile, JSON.stringify(localSecrets));
  const envFile = path.join(current.temporary, 'release.env');
  fs.writeFileSync(envFile, 'SSH_PASSWORD=ssh-from-env\n');
  const reportFile = path.join(current.temporary, 'env-credential-report.md');
  const result = spawnSync(process.execPath, [
    path.join(scriptDir, 'generate-deployment-report.mjs'),
    '--profile', current.profileFile,
    '--secrets', current.secretsFile,
    '--env-file', envFile,
    '--state', current.stateFile,
    '--output', reportFile
  ], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  const report = fs.readFileSync(reportFile, 'utf8');
  assert.match(report, /\| 服务器 SSH \| root \| ssh-from-env \|/u);
  assert.doesNotMatch(report, /请仅在本地替换/u);
});

test('报告使用 profile 严格拒绝错误 Compose 候选并返回 2', (context) => {
  const current = fixture(context);
  const invalidState = state(current.candidateProfile);
  invalidState.compose.project = 'wrong-project';
  fs.writeFileSync(current.stateFile, JSON.stringify(invalidState));
  const reportFile = path.join(current.temporary, 'invalid-deployment.md');
  const result = spawnSync(process.execPath, [path.join(scriptDir, 'generate-deployment-report.mjs'), '--profile', current.profileFile, '--state', current.stateFile, '--output', reportFile], { encoding: 'utf8' });
  assert.equal(result.status, 2, result.stderr);
  assert.match(fs.readFileSync(reportFile, 'utf8'), /Compose project 不一致/u);
});

test('旧 state 只生成带升级提示的兼容审计', (context) => {
  const current = fixture(context);
  const legacyState = state(current.candidateProfile);
  delete legacyState.schemaVersion;
  fs.writeFileSync(current.stateFile, JSON.stringify(legacyState));
  const reportFile = path.join(current.temporary, 'legacy-audit.md');
  const result = spawnSync(process.execPath, [path.join(scriptDir, 'generate-deployment-report.mjs'), '--profile', current.profileFile, '--state', current.stateFile, '--output', reportFile], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  const report = fs.readFileSync(reportFile, 'utf8');
  assert.match(report, /兼容审计（必须升级 v2）/u);
  assert.match(report, /不得据此宣称发布通过/u);
});
