import assert from 'node:assert/strict';
import { mkdtemp, readFile, stat, rm, readdir } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { initializeOidcKeys } from './init-keys.mjs';

test('只生成数据库凭据保护根密钥，重复初始化拒绝覆盖', async () => {
  const root = await mkdtemp(join(tmpdir(), 'oidc-keys-test-'));
  try {
    const target = join(root, 'keys');
    await initializeOidcKeys(target);
    assert.deepEqual(await readdir(target), ['auth.env']);
    const original = await readFile(join(target, 'auth.env'), 'utf8');
    assert.equal(Buffer.from(original.match(/^AUTH_CONFIG_ROOT_KEY=(.+)$/m)[1], 'base64').length, 32);
    assert.equal((await stat(target)).mode & 0o777, 0o700);
    assert.equal((await stat(join(target, 'auth.env'))).mode & 0o777, 0o600);
    await assert.rejects(initializeOidcKeys(target), /已存在/);
    assert.equal(await readFile(join(target, 'auth.env'), 'utf8'), original);
    await initializeOidcKeys(join(root, 'other'));
    assert.notEqual(await readFile(join(root, 'other/auth.env'), 'utf8'), original);
  } finally { await rm(root, { recursive: true, force: true }); }
});
