import assert from 'node:assert/strict';
import { createPrivateKey, createPublicKey, sign, verify } from 'node:crypto';
import { mkdtemp, readFile, stat, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { initializeOidcKeys } from './init-keys.mjs';

test('生成持久RS256材料和独立状态密钥，重复初始化拒绝覆盖', async () => {
  const root = await mkdtemp(join(tmpdir(), 'oidc-keys-test-'));
  try {
    const target = join(root, 'keys');
    const result = await initializeOidcKeys(target);
    const original = await readFile(join(target, 'jwks.json'), 'utf8');
    const jwk = JSON.parse(original).keys[0];
    assert.equal(jwk.kid, result.kid);
    const privateKey = createPrivateKey({ key: jwk, format: 'jwk' });
    assert.equal(privateKey.asymmetricKeyDetails.modulusLength, 3072);
    const body = Buffer.from('test-user');
    assert.equal(verify('RSA-SHA256', body, createPublicKey(privateKey), sign('RSA-SHA256', body, privateKey)), true);
    const env = await readFile(join(target, 'oidc.env'), 'utf8');
    assert.equal(Buffer.from(env.match(/^OIDC_STATE_ENCRYPTION_KEY=(.+)$/m)[1], 'base64').length, 32);
    assert.equal((await stat(join(target, 'jwks.json'))).mode & 0o777, 0o600);
    await assert.rejects(initializeOidcKeys(target), /已存在/);
    assert.equal(await readFile(join(target, 'jwks.json'), 'utf8'), original);
  } finally { await rm(root, { recursive: true, force: true }); }
});
