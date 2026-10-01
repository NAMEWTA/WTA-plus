import { describe, expect, it } from 'vitest';
import source from './LoginPage.vue?raw';
describe('configured provider controls', () => {
  it('uses shared offline icons and leaves username/password visible', () => {
    expect(source).toContain('v-for="provider in providers"');
    expect(source).toContain('provider.icon');
    expect(source).toContain('provider.name');
    expect(source).toContain('label="用户名"');
    expect(source).toContain('label="密码"');
    expect(source).not.toContain('authMode');
    expect(source).not.toContain('startSsoLogin');
  });
});
