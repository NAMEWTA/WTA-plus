import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'SsoCallbackPage.vue'), 'utf8');

describe('home SSO callback', () => {
  it('writes the business token into both session and the user store', () => {
    expect(source).toContain('session.setToken(result.accessToken)');
    expect(source).toContain('userStore.token = result.accessToken');
    expect(source).toContain('router.replace(result.returnTo)');
  });
});
