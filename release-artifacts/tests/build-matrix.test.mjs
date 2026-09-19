import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const required = ['wta-system', 'wta-common-notify', 'wta-common-oss', 'wta-third', 'wta-sso', 'wta-notify', 'wta-profile-person', 'wta-profile-enterprise'];
const optional = ['wta-job', 'wta-ai','wta-common-ai', 'wta-demo', 'wta-workflow'];

function verify(mode, modules, outsideRepository = false) {
  const directory = mkdtempSync(path.join(tmpdir(), 'namewta-bundle-test-'));
  try {
    const lib = path.join(directory, 'BOOT-INF/lib');
    mkdirSync(lib, { recursive: true });
    for (const name of modules) writeFileSync(path.join(lib, `${name}-6.0.0.jar`), 'owned composition fixture');
    const artifact = path.join(directory, 'admin.jar');
    execFileSync('jar', ['cf', artifact, '-C', directory, 'BOOT-INF']);
    return spawnSync('bash', [path.join(root, 'scripts/ci/verify-admin-bundle.sh'), mode], {
      cwd: outsideRepository ? directory : root, env: { ...process.env, ADMIN_ARTIFACT: artifact }, encoding: 'utf8'
    });
  } finally { rmSync(directory, { recursive: true, force: true }); }
}

for (const mode of ['full', 'core']) {
  const modules = [...required, ...(mode === 'full' ? optional : [])];
  test(`${mode} accepts the independently declared product composition`, () => {
    const result = verify(mode, modules);
    assert.equal(result.status, 0, result.stderr);
  });
  for (const missing of required) {
    test(`${mode} rejects a missing ${missing}`, () => {
      const result = verify(mode, modules.filter(name => name !== missing));
      assert.notEqual(result.status, 0);
      assert.match(result.stderr, new RegExp(missing));
    });
  }
}

for (const module of optional) {
  test(`core rejects the excluded ${module}`, () => assert.notEqual(verify('core', [...required, module]).status, 0));
  test(`full requires ${module}`, () => assert.notEqual(verify('full', [...required, ...optional.filter(name => name !== module)]).status, 0));
}

test('a similarly prefixed library cannot impersonate a required module', () => {
  assert.notEqual(verify('core', [...required.filter(name => name !== 'wta-system'), 'wta-system-impostor']).status, 0);
});

test('neither bundle may include retired vendor starters', () => {
  for (const mode of ['full', 'core']) {
    const modules = [...required, ...(mode === 'full' ? optional : []), 'snail-ai-openapi-starter'];
    const result = verify(mode, modules);
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /retired Snail AI vendor/);
  }
});

test('external service test images match the current deployment Compose contract', () => {
  const compose = readFileSync(path.join(root, 'release-artifacts/docker/docker-compose-infrastructure.yml'), 'utf8');
  const script = readFileSync(path.join(root, 'scripts/ci/run-external-services.sh'), 'utf8');
  for (const service of ['redis', 'mysql', 'minio']) {
    const image = compose.match(new RegExp(`^  ${service}:\\n    image: ([^\\n]+)`, 'm'))?.[1];
    assert.ok(image, `${service} must declare a concrete image`);
    assert.ok(script.includes(image), `${service} CI image must equal ${image}`);
  }
});

test('network name collision does not delete containers or networks owned by another run', () => {
  const directory = mkdtempSync(path.join(tmpdir(), 'namewta-ci-owner-test-'));
  try {
    const log = path.join(directory, 'calls');
    writeFileSync(path.join(directory, 'docker'), '#!/usr/bin/env bash\nprintf "%s\\n" "$*" >> "$OWNED_DOCKER_LOG"\nexit 1\n', { mode: 0o755 });
    const result = spawnSync('bash', ['scripts/ci/run-external-services.sh'], {
      cwd: root, env: { ...process.env, PATH: `${directory}:${process.env.PATH}`, OWNED_DOCKER_LOG: log }, encoding: 'utf8'
    });
    assert.notEqual(result.status, 0);
    assert.doesNotMatch(readFileSync(log, 'utf8'), /^(?:rm|network rm) /m);
  } finally { rmSync(directory, { recursive: true, force: true }); }
});

test('explicit JAR verification works from an isolated directory without Git metadata', () => {
  const result = verify('core', required, true);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /core bundle contents verified/);
});
