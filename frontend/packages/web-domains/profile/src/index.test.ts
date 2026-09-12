import type { Component } from 'vue';
import { createProfileService, profileDomainModule, profilePermissions } from '@namewta/domain-profile';
import { composeAppRuntime } from '@namewta/platform-app-runtime';
import { describe, expect, expectTypeOf, it } from 'vitest';
import type { ProfileWebRuntime } from './runtime';
import { createEnterpriseWebContribution } from './enterprise/registration';
import { createProfileWebDomain } from './index';
import { createMaterialTagWebContribution } from './material-tag/registration';
import { createPersonWebContribution } from './person/registration';

const runtime: ProfileWebRuntime = {
  closeCurrentPage: () => {},
  completeWorkflowTask: async () => {},
  confirm: async () => {},
  downloadMaterial: () => {},
  error: () => {},
  fileUpload: {} as Component,
  findUsers: async () => [],
  hasPermission: () => true,
  service: createProfileService({ request: async <T>() => ({ data: null }) as T }),
  success: () => {},
  warning: () => {}
};

describe('profile web manifest', () => {
  it('fails closed when the host runtime is missing', () => {
    expect(() => createProfileWebDomain(undefined)).toThrow('ProfileWebRuntime is required');
    expect(() => createMaterialTagWebContribution(undefined)).toThrow('ProfileWebRuntime is required');
    expect(() => createPersonWebContribution(undefined)).toThrow('ProfileWebRuntime is required');
    expect(() => createEnterpriseWebContribution(undefined)).toThrow('ProfileWebRuntime is required');
    expect(() => createProfileWebDomain({ ...runtime, confirm: undefined } as never)).toThrow(
      'ProfileWebRuntime is required'
    );
    expect(() => createProfileWebDomain({ ...runtime, service: {} } as never)).toThrow('ProfileWebRuntime is required');
  });

  it('freezes menu, detail and workflow component keys before page tickets start', () => {
    const manifest = createProfileWebDomain(runtime);
    expect(manifest.registrations.map(item => [item.componentKey, item.componentName])).toEqual([
      ['profile/materialTag/index', 'ProfileMaterialTag'],
      ['profile/person/index', 'PersonProfile'],
      ['profile/person/detail', 'PersonProfileDetail'],
      ['profile/person/review', 'PersonProfileReview'],
      ['profile/enterprise/index', 'EnterpriseProfile'],
      ['profile/enterprise/detail', 'EnterpriseProfileDetail'],
      ['profile/enterprise/review', 'EnterpriseProfileReview']
    ]);
    expect(manifest.permissions.flatMap(item => item.permissions)).toEqual([
      ...Object.values(profilePermissions.materialTag),
      ...Object.values(profilePermissions.person),
      ...Object.values(profilePermissions.enterprise)
    ]);
  });

  it('registers only when an App explicitly selects profile', () => {
    const manifest = createProfileWebDomain(runtime);
    const admin = composeAppRuntime({
      appId: 'admin-web',
      domainModules: [profileDomainModule],
      manifests: [manifest],
      selectedDomainIds: ['profile'],
      selectedManifestIds: ['web-domain-profile']
    });
    expect(admin.componentKeys()).toContain('profile/person/index');
    const client = composeAppRuntime({
      appId: 'fixture-web',
      domainModules: [],
      manifests: [manifest],
      selectedDomainIds: [],
      selectedManifestIds: []
    });
    expect(client.componentKeys()).toEqual([]);
  });

  it('requires host-owned authorization, download, user lookup and workflow actions', () => {
    expectTypeOf<ProfileWebRuntime['hasPermission']>().toEqualTypeOf<(permission: string) => boolean>();
    expectTypeOf<Parameters<ProfileWebRuntime['findUsers']>>().toEqualTypeOf<['ENTERPRISE' | 'PERSON', string]>();
    expectTypeOf<ProfileWebRuntime['findUsers']>().returns.toEqualTypeOf<
      Promise<readonly { label: string; userId: string | number }[]>
    >();
    expectTypeOf<ProfileWebRuntime['completeWorkflowTask']>().returns.toEqualTypeOf<Promise<void>>();
  });
});
