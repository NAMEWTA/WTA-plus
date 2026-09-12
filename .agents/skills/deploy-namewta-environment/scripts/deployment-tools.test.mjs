import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { validateProfile } from './lib.mjs';
import { verifyState } from './verify-deployment-state.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const skillDir = path.resolve(scriptDir, '..');

function assertPrivateMode(file) {
  if (process.platform === 'win32') return;
  assert.equal(fs.statSync(file).mode & 0o777, 0o600);
}

function validProfile() {
  const profile = JSON.parse(fs.readFileSync(path.join(skillDir, 'assets/templates/deployment-profile.json.template'), 'utf8'));
  profile.deployment.releaseId = '20260901-namewta-001';
  profile.release.compose.identityConfirmed = true;
  return profile;
}

function validSecrets() {
  return {
    sshPassword: '',
    mysqlRootPassword: 'mysql-root-secret',
    mysqlAppPassword: 'mysql-app-secret',
    redisPassword: 'redis-secret',
    minioRootUser: 'namewta',
    minioRootPassword: 'minio-secret',
    nacosDatabaseUser: 'nacos',
    nacosDatabasePassword: 'nacos-db-secret',
    nacosUsername: 'nacos',
    nacosPassword: 'nacos-secret',
    nacosAuthToken: 'token-secret',
    nacosIdentityKey: 'identity-key',
    nacosIdentityValue: 'identity-value',
    monitorUsername: 'monitor',
    monitorPassword: 'monitor-secret',
    grafanaUsername: 'admin',
    grafanaPassword: 'grafana-secret'
  };
}

function validState() {
  const service = { status: 'healthy', image: 'namewta/test:6.0.0' };
  return {
    capturedAt: '2026-09-01T12:00:00+08:00',
    services: {
      mysql: service, redis: service, minio: service, backend1: service,
      backend2: service, frontend: service, ingress: service
    },
    publishedPorts: [{ port: 40080, owner: 'ingress' }, { port: 43306, owner: 'mysql' }],
    endpoints: { admin: { ok: true, status: 200 } },
    oss: {
      enabledDefaultCount: 1, privateDefaultCount: 1, anonymousDenied: true,
      signedUrlWorks: true, signedUrlExpired: true
    },
    nacos: { enabled: true, adminUserCount: 1, configReaderUserCount: 1, backendDigests: ['abc', 'abc'] },
    rollback: { previousReleaseId: '20260831', command: 'bash rollback.sh 20260831' },
    risks: []
  };
}

test('有效配置档案通过校验', () => {
  assert.deepEqual(validateProfile(validProfile()), []);
});

test('拒绝占位发布编号、危险根目录和端口冲突', () => {
  const profile = validProfile();
  profile.deployment.releaseId = '请替换为不可变发布编号';
  profile.server.root = '/';
  profile.network.ports.redis = profile.network.ports.mysql;
  const errors = validateProfile(profile).join('\n');
  assert.match(errors, /占位值/u);
  assert.match(errors, /不安全/u);
  assert.match(errors, /重复使用/u);
});

test('部署状态校验覆盖私有 OSS 与 Nacos 收敛', () => {
  assert.deepEqual(verifyState(validState()), []);
  const state = validState();
  state.oss.privateDefaultCount = 0;
  state.nacos.backendDigests = ['abc', 'def'];
  const errors = verifyState(state).join('\n');
  assert.match(errors, /PRIVATE/u);
  assert.match(errors, /未收敛/u);
});

test('本地配置和私密报告可生成且权限为 0600', (context) => {
  const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'namewta-skill-'));
  context.after(() => fs.rmSync(temporary, { recursive: true, force: true }));
  const profileFile = path.join(temporary, 'profile.json');
  const secretsFile = path.join(temporary, 'secrets.json');
  const stateFile = path.join(temporary, 'state.json');
  const renderedDir = path.join(temporary, 'rendered');
  const reportFile = path.join(temporary, 'deployment.md');
  fs.writeFileSync(profileFile, JSON.stringify(validProfile()));
  fs.writeFileSync(secretsFile, JSON.stringify(validSecrets()));
  fs.writeFileSync(stateFile, JSON.stringify(validState()));

  const render = spawnSync(process.execPath, [path.join(scriptDir, 'render-local-config.mjs'), '--profile', profileFile, '--secrets', secretsFile, '--output', renderedDir], { encoding: 'utf8' });
  assert.equal(render.status, 0, render.stderr);
  for (const name of ['release.env', 'application-local.yml', 'admin-web.env.development.local']) {
    const file = path.join(renderedDir, name);
    assertPrivateMode(file);
  }
  assert.match(fs.readFileSync(path.join(renderedDir, 'application-local.yml'), 'utf8'), /mysql-app-secret/u);

  const report = spawnSync(process.execPath, [path.join(scriptDir, 'generate-deployment-report.mjs'), '--profile', profileFile, '--secrets', secretsFile, '--state', stateFile, '--output', reportFile], { encoding: 'utf8' });
  assert.equal(report.status, 0, report.stderr);
  assertPrivateMode(reportFile);
  const content = fs.readFileSync(reportFile, 'utf8');
  assert.match(content, /部署交接/u);
  assert.match(content, /mysql-app-secret/u);
  assert.doesNotMatch(report.stdout, /mysql-app-secret/u);
});
