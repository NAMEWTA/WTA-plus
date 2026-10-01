import { describe, expect, it } from 'vitest';
import source from './login.vue?raw';
describe('configured external login entry', () => {
  it('renders only configured providers while keeping password login', () => {
    expect(source).toContain('v-for="provider in providers"');
    expect(source).toContain('provider.name');
    expect(source).toContain('createAppSocialRuntime().start');
    expect(source).not.toContain("authMode !== 'sso'");
    expect(source).not.toContain('adminSso');
  });
});
