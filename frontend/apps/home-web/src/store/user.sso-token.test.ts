import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'user.ts'), 'utf8');

describe('home user store SSO session', () => {
  it('hydrates pinia token from Home-Token before getInfo', () => {
    expect(source).toContain('token.value = getToken()');
    expect(source).toContain('identityAccessService.getInfo()');
  });
});
