import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { execFileSync, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const localConfig = 'backend/wta-admin/src/main/resources/application-local.yml';

test('machine-local backend credentials are not tracked and stay ignored', () => {
  const tracked = spawnSync('git', ['ls-files', '--error-unmatch', '--', localConfig], { cwd: root, encoding: 'utf8' });
  assert.equal(tracked.status, 1, 'The machine-local backend configuration must not be versioned');
  const ignored = execFileSync('git', ['check-ignore', '--no-index', localConfig], { cwd: root, encoding: 'utf8' }).trim();
  assert.equal(ignored, localConfig);
  const example = readFileSync(new URL('../../backend/wta-admin/src/main/resources/application-local.yml.example', import.meta.url), 'utf8');
  for (const line of example.split('\n').filter(line => /^\s*password:/.test(line))) {
    assert.match(line, /password: \$\{[A-Z_]+(?::)?\}$/, 'Example passwords must come from the environment');
  }
});

test('shared CORS configuration cannot silently enable arbitrary credentialed origins', () => {
  const config = readFileSync(new URL('../../backend/wta-admin/src/main/resources/application.yml', import.meta.url), 'utf8');
  assert.match(config, /allowed-origins:.*\$\{WEB_CORS_ALLOWED_ORIGINS:\}/);
  assert.doesNotMatch(config, /WEB_CORS_ALLOWED_ORIGINS:\*/);
});
