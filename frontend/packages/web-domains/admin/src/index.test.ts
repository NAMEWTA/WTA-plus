import type { IdentityAccessService } from '@namewta/domain-admin';
import { describe, expect, it, vi } from 'vitest';
import { createAdminWebDomain, type IdentityAccessWebRuntime } from './index';

const runtime = (): IdentityAccessWebRuntime => ({
  service: {
    client: { clientId: 'client-proof' },
    login: vi.fn(),
    prepareLogin: vi.fn()
  } as unknown as IdentityAccessService,
  onAuthenticated: vi.fn()
});

describe('identity access web domain', () => {
  it('publishes a frozen injectable login manifest', () => {
    const manifest = createAdminWebDomain(runtime());

    expect(manifest).toMatchObject({ id: 'web-domain-admin', domainId: 'admin' });
    expect(manifest.messages).toEqual([
      {
        namespace: 'admin',
        messages: { title: '客户服务入口', unavailable: '客户端认证配置不可用，无法登录' }
      }
    ]);
    expect(manifest.permissions).toEqual([{ id: 'identity-login', permissions: [] }]);
    expect(manifest.registrations).toMatchObject([
      {
        id: 'identity-login',
        componentKey: 'identity-access/login/index',
        componentName: 'IdentityLogin'
      }
    ]);
    expect(Object.isFrozen(manifest)).toBe(true);
    expect(Object.isFrozen(manifest.registrations[0])).toBe(true);
    expect(Object.isFrozen(manifest.messages[0].messages)).toBe(true);
  });

  it('fails closed when the App omits its injected runtime', () => {
    expect(() => createAdminWebDomain(undefined as unknown as IdentityAccessWebRuntime)).toThrow(
      'IdentityAccessWebRuntime is required'
    );
  });
});
