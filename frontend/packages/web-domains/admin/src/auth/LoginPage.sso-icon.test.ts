import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'LoginPage.vue'), 'utf8');

describe('web-domain login SSO slot', () => {
  it('renders WTA SSO as the first circle svg-icon control', () => {
    const testId = source.indexOf('data-testid="sso-first-provider"');
    expect(testId).toBeGreaterThan(-1);
    const slot = source.slice(Math.max(0, testId - 250), source.indexOf('</el-button>', testId) + 12);
    expect(slot).toMatch(/\bcircle\b/);
    expect(slot).toContain('<svg');
    expect(slot).toContain('M1.5 4h4.2');
    expect(slot).not.toMatch(/type="primary"/);
    expect(slot).not.toMatch(/>\s*WTA SSO\s*</);
    const socialRow = source.indexOf('identity-login__social-row');
    expect(socialRow).toBeGreaterThan(-1);
    expect(socialRow).toBeLessThan(testId);
  });
});
