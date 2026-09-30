import { createOidcService } from '@namewta/domain-oidc';
import { oidcDomainModule } from '@namewta/domain-oidc';
import { composeAppRuntime } from '@namewta/platform-app-runtime';
import { describe, expect, it, vi } from 'vitest';
import { createOidcWebDomain } from './index';

describe('OIDC 管理清单', () => {
  it('缺失宿主时在组合阶段拒绝，注册本身不会查询或复制资料', () => {
    expect(() => createOidcWebDomain(undefined!)).toThrow('宿主');
    const request = vi.fn();
    const copyText = vi.fn();
    const manifest = createOidcWebDomain({
      service: createOidcService({ request }),
      hasPermission: () => true,
      confirm: async () => {},
      copyText,
      success: () => {}
    });
    const runtime = composeAppRuntime({
      appId: 'test-admin',
      domainModules: [oidcDomainModule],
      manifests: [manifest],
      selectedDomainIds: ['oidc'],
      selectedManifestIds: ['web-domain-oidc']
    });
    expect(runtime.resolve({ domainId: 'oidc', componentKey: 'oidc/application/index' }).componentName).toBe(
      'OidcApplication'
    );
    expect(request).not.toHaveBeenCalled();
    expect(copyText).not.toHaveBeenCalled();
    expect(manifest.permissions[0]?.permissions).toContain('oidc:application:rotate');
  });
});
