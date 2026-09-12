import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import type { SystemWebRuntime } from './runtime';

export {
  createLiveSystemDictRefs,
  type SystemDictOption,
  type SystemPasswordCharacterClass,
  type SystemPasswordPolicy,
  type SystemPasswordViolation,
  type SystemPasswordViolationReason,
  type SystemWebRuntime
} from './runtime';
export {
  createLiveMonitorDictRefs,
  createMonitorWebDomain,
  type MonitorWebRuntime,
  type MonitorDictOption
} from './monitor/index';
export {
  OpenApiWorkspace,
  createOpenApiWorkspaceController,
  createOpenApiWorkspaceState,
  type OpenApiWorkspaceRuntime,
  type OpenApiWorkspaceScope
} from './open-api';

async function runtimeView(
  name: string,
  runtime: SystemWebRuntime,
  load: () => Promise<{ default: Component }>
): Promise<Component> {
  const page = (await load()).default;
  return defineComponent({ name, setup: () => () => h(page, { runtime }) });
}

export function createSystemWebDomain(runtime: SystemWebRuntime): WebDomainManifest<Component> {
  const registrations = [
    ['system-client', 'system/client/index', 'Client', () => import('./client/ClientPage.vue')],
    ['system-user', 'system/user/index', 'User', () => import('./user/UserPage.vue')],
    ['system-user-auth-role', 'system/user/authRole', 'AuthRole', () => import('./user/UserAuthRolePage.vue')],
    ['system-user-type', 'system/userType/index', 'UserType', () => import('./user-type/UserTypePage.vue')],
    ['system-role', 'system/role/index', 'Role', () => import('./role/RolePage.vue')],
    ['system-role-auth-user', 'system/role/authUser', 'AuthUser', () => import('./role/RoleAuthUserPage.vue')],
    ['system-menu', 'system/menu/index', 'Menu', () => import('./menu/MenuPage.vue')],
    ['system-dept', 'system/dept/index', 'Dept', () => import('./dept/DepartmentPage.vue')],
    ['system-post', 'system/post/index', 'Post', () => import('./post/PostPage.vue')],
    ['system-dict', 'system/dict/index', 'Dict', () => import('./dict-type/DictPage.vue')],
    ['system-config', 'system/config/index', 'Config', () => import('./config/ConfigPage.vue')],
    ['system-open-api', 'system/openApi/index', 'OpenApi', () => import('./open-api/OpenApiAdminPage.vue')],
    ['system-oss', 'system/oss/index', 'Oss', () => import('./oss/OssPage.vue')],
    ['system-oss-config', 'system/oss/config', 'OssConfig', () => import('./oss-config/OssConfigPage.vue')]
  ] as const;
  return Object.freeze({
    id: 'web-domain-system',
    domainId: 'system',
    messages: Object.freeze([
      Object.freeze({
        namespace: 'systemAdmin',
        messages: Object.freeze({ title: '系统管理', client: '客户端', organization: '组织机构' })
      })
    ]),
    permissions: Object.freeze(
      Object.entries({
        client: ['list', 'query', 'add', 'edit', 'remove', 'export'],
        user: ['list', 'query', 'add', 'edit', 'remove', 'export', 'import', 'resetPwd', 'temporaryPassword'],
        userType: ['list', 'query', 'add', 'edit', 'remove', 'export'],
        role: ['list', 'query', 'add', 'edit', 'remove', 'export'],
        menu: ['list', 'query', 'add', 'edit', 'remove'],
        dept: ['list', 'query', 'add', 'edit', 'remove'],
        post: ['list', 'query', 'add', 'edit', 'remove', 'export'],
        dict: ['list', 'query', 'add', 'edit', 'remove', 'export'],
        config: ['list', 'query', 'add', 'edit', 'remove', 'export'],
        openApi: ['self', 'list', 'query', 'add', 'edit', 'remove'],
        oss: ['list', 'query', 'upload', 'download', 'edit', 'remove'],
        ossConfig: ['list', 'query', 'add', 'edit', 'remove']
      }).map(([slice, actions]) =>
        Object.freeze({
          id: `system-${slice}`,
          permissions: Object.freeze(actions.map(action => `system:${slice}:${action}`))
        })
      )
    ),
    registrations: Object.freeze(
      registrations.map(([id, componentKey, componentName, load]) =>
        Object.freeze({ id, componentKey, componentName, load: () => runtimeView(componentName, runtime, load) })
      )
    )
  });
}
