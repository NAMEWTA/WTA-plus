import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { verifyState } from './verify-deployment-state.mjs';
import { verifyFrontendHtml } from './verify-frontend-artifact.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const skillDir = path.resolve(scriptDir, '..');
const imageId = `sha256:${'a'.repeat(64)}`;
const indexSha256 = 'b'.repeat(64);

function profile() {
  const value = JSON.parse(fs.readFileSync(path.join(skillDir, 'assets/templates/deployment-profile.json.template'), 'utf8'));
  value.deployment.releaseId = '20260901-namewta-candidate';
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
    capturedAt: '2026-09-01T17:49:30+08:00',
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
      frontend: service('nginx:1.29-alpine'),
      ingress: service('nginx:1.29-alpine'),
    },
    publishedPorts: [{ port: 40080, owner: 'ingress' }, { port: 43306, owner: 'mysql' }],
    endpoints: { admin: { required: true, ok: true, status: 200 } },
    semanticProbes: {
      backend1Route: probe('backend1', 401),
      backend2Route: probe('backend2', 401),
      ingressRoute: probe('ingress', 401),
      credentialEmpty: probe('ingress', 404, { authenticated: true }),
      interfacesEmpty: probe('ingress', 200, { authenticated: true }),
    },
    frontend: {
      indexSha256,
      contextPath: candidateProfile.release.frontend.contextPath,
      baseApi: candidateProfile.release.frontend.baseApi,
      assetPrefix: candidateProfile.release.frontend.assetPrefix,
      observedAssetPaths: ['/namewta/assets/index-a1b2c3.js', '/namewta/assets/index-a1b2c3.css'],
      stableWindow: {
        requiredConsecutiveSuccesses: candidateProfile.release.rollout.requiredConsecutiveSuccesses,
        observedConsecutiveSuccesses: candidateProfile.release.rollout.requiredConsecutiveSuccesses,
        timeoutSeconds: candidateProfile.release.rollout.timeoutSeconds,
        elapsedSeconds: 4,
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
        '/data/namewta-data/releases/previous',
        '/data/namewta-data/releases/failed-candidate',
      ],
    },
    database: {
      migrationPerformed: false,
      backup: { status: 'not-required', evidence: 'No database changes in this release' },
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

test('完整 v2 候选通过所有发布门禁', () => {
  const candidateProfile = profile();
  assert.deepEqual(verifyState(state(candidateProfile), { profile: candidateProfile }), []);
});

test('拒绝错误 Compose 身份、文件顺序和未渲染配置', () => {
  const candidateProfile = profile();
  const candidateState = state(candidateProfile);
  candidateState.compose.project = 'wrong-project';
  candidateState.compose.files.reverse();
  candidateState.compose.rendered = false;
  const errors = verifyState(candidateState, { profile: candidateProfile }).join('\n');
  assert.match(errors, /Compose project/u);
  assert.match(errors, /文件顺序/u);
  assert.match(errors, /rendered/u);
});

test('拒绝后端重启、镜像和运行配置漂移', () => {
  const candidateProfile = profile();
  const candidateState = state(candidateProfile);
  candidateState.services.backend1.restartCount = -1;
  candidateState.services.backend2.imageId = `sha256:${'c'.repeat(64)}`;
  candidateState.services.backend2.environmentDigest = 'different-env';
  const errors = verifyState(candidateState, { profile: candidateProfile }).join('\n');
  assert.match(errors, /restartCount/u);
  assert.match(errors, /image ID/u);
  assert.match(errors, /环境摘要/u);
});

test('业务语义必须覆盖双实例、入口和认证探针', () => {
  const candidateProfile = profile();
  const candidateState = state(candidateProfile);
  delete candidateState.semanticProbes.backend2Route;
  candidateState.semanticProbes.credentialEmpty.businessCode = 200;
  candidateState.semanticProbes.credentialEmpty.authenticated = false;
  candidateState.semanticProbes.interfacesEmpty.authenticated = false;
  const errors = verifyState(candidateState, { profile: candidateProfile }).join('\n');
  assert.match(errors, /backend2/u);
  assert.match(errors, /业务码/u);
  assert.match(errors, /认证语义探针/u);
});

test('允许窗口内瞬时 502，但连续成功不足时拒绝发布', () => {
  const candidateProfile = profile();
  const candidateState = state(candidateProfile);
  assert.deepEqual(verifyState(candidateState, { profile: candidateProfile }), []);
  candidateState.frontend.stableWindow.observedConsecutiveSuccesses = 1;
  const errors = verifyState(candidateState, { profile: candidateProfile }).join('\n');
  assert.match(errors, /连续成功/u);
  assert.doesNotMatch(errors, /502/u);
});

test('数据库迁移默认要求备份，dev waiver 必须完整且 prod 永远拒绝', () => {
  const devProfile = profile();
  const candidateState = state(devProfile);
  candidateState.database.migrationPerformed = true;
  candidateState.database.backup = { status: 'missing', evidence: '' };
  let errors = verifyState(candidateState, { profile: devProfile }).join('\n');
  assert.match(errors, /备份/u);

  candidateState.database.waiver = {
    authorized: true,
    source: 'USER-DECISION:target-dev-no-backup',
    scope: 'target development database only',
    preflight: { zeroRow: true, objectIdentity: true, conflictsAbsent: true },
    recoveryMode: 'forward-only',
  };
  assert.deepEqual(verifyState(candidateState, { profile: devProfile }), []);

  const prodProfile = profile();
  prodProfile.deployment.environment = 'prod';
  errors = verifyState(candidateState, { profile: prodProfile }).join('\n');
  assert.match(errors, /生产环境/u);
  candidateState.database.waiver = null;
  errors = verifyState(candidateState, { profile: prodProfile }).join('\n');
  assert.match(errors, /必须有已验证备份/u);
});

test('OpenAPI 能力只比较启用状态、版本、存在性和双实例摘要', () => {
  const candidateProfile = profile();
  const candidateState = state(candidateProfile);
  candidateState.capabilities.openApi.kekPresent = false;
  candidateState.capabilities.openApi.backendDigests[1] = 'different';
  candidateState.capabilities.openApi.kek = 'must-not-live-in-state';
  const errors = verifyState(candidateState, { profile: candidateProfile }).join('\n');
  assert.match(errors, /KEK/u);
  assert.match(errors, /OpenAPI 配置摘要/u);
  assert.match(errors, /不得包含 OpenAPI KEK/u);
});

test('前端 HTML 只接受配置前缀下的 Vite 资源', () => {
  const candidateProfile = profile();
  const validHtml = '<!doctype html><link rel="stylesheet" href="/namewta/assets/index.css"><script type="module" src="/namewta/assets/index.js"></script>';
  assert.deepEqual(verifyFrontendHtml(validHtml, candidateProfile).errors, []);

  const invalid = verifyFrontendHtml('<script type="module" src="/assets/index.js"></script>', candidateProfile);
  assert.match(invalid.errors.join('\n'), /assetPrefix/u);
});

test('候选与前端 CLI 对有效 fixture 返回 0', (context) => {
  const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'namewta-candidate-'));
  context.after(() => fs.rmSync(temporary, { recursive: true, force: true }));
  const profileFile = path.join(temporary, 'profile.json');
  const stateFile = path.join(temporary, 'state.json');
  const indexFile = path.join(temporary, 'index.html');
  const candidateProfile = profile();
  fs.writeFileSync(profileFile, JSON.stringify(candidateProfile));
  fs.writeFileSync(stateFile, JSON.stringify(state(candidateProfile)));
  fs.writeFileSync(indexFile, '<link href="/namewta/assets/index.css"><script src="/namewta/assets/index.js"></script>');

  const candidate = spawnSync(process.execPath, [path.join(scriptDir, 'verify-release-candidate.mjs'), '--profile', profileFile, '--state', stateFile], { encoding: 'utf8' });
  assert.equal(candidate.status, 0, candidate.stderr);
  const frontend = spawnSync(process.execPath, [path.join(scriptDir, 'verify-frontend-artifact.mjs'), '--profile', profileFile, '--index', indexFile], { encoding: 'utf8' });
  assert.equal(frontend.status, 0, frontend.stderr);
  assert.match(frontend.stdout, /SHA-256/u);
});
