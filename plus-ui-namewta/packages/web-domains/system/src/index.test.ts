import { systemDomainModule } from '@namewta/domain-system';
import { composeAppRuntime } from '@namewta/platform-app-runtime';
import { describe, expect, expectTypeOf, it, vi } from 'vitest';
import { reactive } from 'vue';
import type { SystemPasswordPolicy, SystemPasswordViolation, SystemWebRuntime } from './runtime';
import dictPage from './dict-type/DictPage.vue?raw';
import { createLiveSystemDictRefs, createSystemWebDomain } from './index';
import openApiAdminPage from './open-api/OpenApiAdminPage.vue?raw';
import openApiWorkspace from './open-api/OpenApiWorkspace.vue?raw';
import ossPage from './oss/OssPage.vue?raw';

const runtime = { service: {} } as never;

describe('system web manifest', () => {
  it('publishes all server-facing governance keys and permission groups', () => {
    const manifest = createSystemWebDomain(runtime);
    expect(manifest.registrations.map(item => [item.componentKey, item.componentName])).toEqual([
      ['system/client/index', 'Client'],
      ['system/user/index', 'User'],
      ['system/user/authRole', 'AuthRole'],
      ['system/userType/index', 'UserType'],
      ['system/role/index', 'Role'],
      ['system/role/authUser', 'AuthUser'],
      ['system/menu/index', 'Menu'],
      ['system/dept/index', 'Dept'],
      ['system/post/index', 'Post'],
      ['system/dict/index', 'Dict'],
      ['system/config/index', 'Config'],
      ['system/openApi/index', 'OpenApi'],
      ['system/oss/index', 'Oss'],
      ['system/oss/config', 'OssConfig']
    ]);
    expect(manifest.permissions.flatMap(item => item.permissions)).toEqual(
      expect.arrayContaining([
        'system:client:list',
        'system:user:list',
        'system:user:import',
        'system:user:resetPwd',
        'system:user:temporaryPassword',
        'system:userType:list',
        'system:role:list',
        'system:menu:list',
        'system:dept:list',
        'system:post:list',
        'system:dict:list',
        'system:config:list',
        'system:openApi:self',
        'system:openApi:list',
        'system:openApi:remove',
        'system:oss:download',
        'system:ossConfig:list'
      ])
    );
  });

  it('requires the host to provide password policy validation and ephemeral clipboard access', () => {
    expectTypeOf<SystemWebRuntime['passwordPolicy']['load']>().toEqualTypeOf<() => Promise<SystemPasswordPolicy>>();
    expectTypeOf<SystemWebRuntime['passwordPolicy']['validate']>().toEqualTypeOf<
      (policy: SystemPasswordPolicy, password: string) => readonly SystemPasswordViolation[]
    >();
    expectTypeOf<SystemWebRuntime['copyText']>().toEqualTypeOf<(value: string) => Promise<void>>();
  });

  it('registers only for an explicitly selected App composition', () => {
    const manifest = createSystemWebDomain(runtime);
    const admin = composeAppRuntime({
      appId: 'admin-web',
      domainModules: [systemDomainModule],
      manifests: [manifest],
      selectedDomainIds: ['system'],
      selectedManifestIds: ['web-domain-system']
    });
    expect(admin.componentKeys()).toContain('system/user/index');
    const client = composeAppRuntime({
      appId: 'fixture-web',
      domainModules: [],
      manifests: [manifest],
      selectedDomainIds: [],
      selectedManifestIds: []
    });
    expect(client.componentKeys()).toEqual([]);
  });

  it('keeps asynchronously loaded host dictionaries live', async () => {
    const source = reactive({ status: [{ label: '启用', value: '0' }] });
    let resolve!: (value: typeof source) => void;
    const dicts = createLiveSystemDictRefs(['status'], () => new Promise(done => (resolve = done)));

    expect(dicts.status.value).toEqual([]);
    resolve(source);
    await vi.waitFor(() => expect(dicts.status.value).toEqual([{ label: '启用', value: '0' }]));
    source.status = [{ label: '停用', value: '1' }];
    expect(dicts.status.value).toEqual([{ label: '停用', value: '1' }]);
  });

  it('settles both dictionary loading states when a request rejects', () => {
    expect(dictPage).toMatch(/const getTypeList = async \(\) =>[\s\S]*?finally[\s\S]*?typeLoading\.value = false/);
    expect(dictPage).toMatch(/const getDataList = async \(\) =>[\s\S]*?finally[\s\S]*?dataLoading\.value = false/);
  });

  it('labels icon-only OSS row actions for assistive technology', () => {
    expect(ossPage).toContain('aria-label="下载"');
    expect(ossPage).toContain('aria-label="删除"');
  });

  it('keeps the target owner visible and clears the controlled one-time secret dialog on every close path', () => {
    expect(openApiAdminPage).toContain("kind: 'target-user'");
    expect(openApiAdminPage).toContain('userLabel(selectedUser)');
    expect(openApiAdminPage).toContain('system:openApi:list');
    expect(openApiWorkspace).toContain('@update:model-value="closeIssuedSecret"');
    expect(openApiWorkspace).toContain('@closed="controller.dismissIssuedSecret"');
    expect(openApiWorkspace).not.toMatch(/localStorage|sessionStorage/);
  });
});
