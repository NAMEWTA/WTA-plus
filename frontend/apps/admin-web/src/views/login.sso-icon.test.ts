import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'login.vue'), 'utf8');

describe('admin-web login SSO slot', () => {
  it('places a circle svg-icon first in the social row', () => {
    const testId = source.indexOf('data-testid="sso-first-provider"');
    const wechat = source.indexOf('icon-class="wechat"');
    const github = source.indexOf('icon-class="github"');
    expect(testId).toBeGreaterThan(-1);
    expect(wechat).toBeGreaterThan(testId);
    expect(github).toBeGreaterThan(wechat);
    const slot = source.slice(Math.max(0, testId - 250), wechat);
    expect(slot).toMatch(/\bcircle\b/);
    expect(slot).toContain('icon-class="wta"');
    expect(slot).toContain('<svg-icon');
    expect(slot).not.toMatch(/type="primary"/);
    expect(slot).not.toMatch(/>\s*WTA SSO\s*</);
  });
});
