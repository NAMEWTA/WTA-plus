import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { validateProfile } from './lib.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const templateFile = path.resolve(scriptDir, '../assets/templates/deployment-profile.json.template');

function legacyProfile() {
  const profile = JSON.parse(fs.readFileSync(templateFile, 'utf8'));
  profile.schemaVersion = 1;
  profile.deployment.releaseId = '20260901-namewta-legacy';
  delete profile.release;
  delete profile.capabilities;
  return profile;
}

function releaseProfile() {
  const profile = legacyProfile();
  profile.schemaVersion = 2;
  profile.deployment.releaseId = '20260901-namewta-v2';
  profile.release = {
    compose: {
      identityConfirmed: true,
      project: 'namewta-backend',
      files: [
        '/data/namewta-data/compose/docker-compose-backend.yml',
        '/data/namewta-data/compose/overrides/nacos-enabled.yml',
        '/data/namewta-data/compose/overrides/snail-enabled.yml',
        '/data/namewta-data/compose/overrides/openapi-runtime.yml',
      ],
      envFile: '/data/namewta-data/namewta-release.env',
      backendServices: ['namewta-server1', 'namewta-server2'],
      frontendServices: ['namewta-nginx-admin-web', 'namewta-nginx-lb'],
    },
    images: { admin: 'namewta/namewta-admin:6.0.0-openapi-ae3689e' },
    frontend: {
      buildEnvironment: 'prod',
      contextPath: '/namewta/',
      baseApi: '/namewta/prod-api',
      assetPrefix: '/namewta/assets/',
    },
    rollout: {
      maxRestartCount: 0,
      probeIntervalSeconds: 2,
      timeoutSeconds: 120,
      requiredConsecutiveSuccesses: 3,
    },
  };
  profile.capabilities = {
    openApi: {
      enabled: true,
      kekVersion: 'v1',
      kekPresenceRequired: true,
      secretSource: 'deployment-secrets',
    },
  };
  return profile;
}

test('schema v2 固定完整发布身份并通过校验', () => {
  assert.deepEqual(validateProfile(releaseProfile()), []);
});

test('schema v1 保持可读取兼容', () => {
  assert.deepEqual(validateProfile(legacyProfile()), []);
});

test('schema v2 拒绝未确认身份和授权根目录外的 Compose 文件', () => {
  const profile = releaseProfile();
  profile.release.compose.identityConfirmed = false;
  profile.release.compose.files[0] = '/tmp/docker-compose-backend.yml';
  const errors = validateProfile(profile).join('\n');
  assert.match(errors, /identityConfirmed/u);
  assert.match(errors, /授权根目录/u);
});

test('schema v2 拒绝不一致的 Compose project 与重复服务', () => {
  const profile = releaseProfile();
  profile.release.compose.project = 'unknown-backend';
  profile.release.compose.backendServices[1] = profile.release.compose.backendServices[0];
  const errors = validateProfile(profile).join('\n');
  assert.match(errors, /composeProjects/u);
  assert.match(errors, /backendServices/u);
});

test('schema v2 拒绝错误前端路径和无效稳定窗口', () => {
  const profile = releaseProfile();
  profile.release.frontend.contextPath = '/';
  profile.release.frontend.baseApi = '/prod-api';
  profile.release.frontend.assetPrefix = '/assets/';
  profile.release.rollout.requiredConsecutiveSuccesses = 0;
  profile.release.rollout.timeoutSeconds = 0;
  const errors = validateProfile(profile).join('\n');
  assert.match(errors, /contextPath/u);
  assert.match(errors, /baseApi/u);
  assert.match(errors, /assetPrefix/u);
  assert.match(errors, /requiredConsecutiveSuccesses/u);
  assert.match(errors, /timeoutSeconds/u);
});

test('启用 OpenAPI 时只要求 KEK 元数据而不允许 secret 进入 profile', () => {
  const profile = releaseProfile();
  profile.capabilities.openApi.kekVersion = '';
  profile.capabilities.openApi.kekPresenceRequired = false;
  profile.capabilities.openApi.kek = 'must-not-live-in-profile';
  const errors = validateProfile(profile).join('\n');
  assert.match(errors, /kekVersion/u);
  assert.match(errors, /kekPresenceRequired/u);
  assert.match(errors, /不得包含 OpenAPI KEK/u);
});
