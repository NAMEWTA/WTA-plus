import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import type { OidcWebRuntime } from './runtime';

export type { OidcWebRuntime } from './runtime';

export function createOidcWebDomain(runtime: OidcWebRuntime): WebDomainManifest<Component> {
  if (
    !runtime?.service ||
    typeof runtime.hasPermission !== 'function' ||
    typeof runtime.copyText !== 'function' ||
    typeof runtime.confirm !== 'function' ||
    typeof runtime.success !== 'function'
  )
    throw new Error('OIDC 管理宿主配置不完整');
  return Object.freeze({
    id: 'web-domain-oidc',
    domainId: 'oidc',
    messages: Object.freeze([{ namespace: 'oidcAdmin', messages: Object.freeze({ title: '单点登录' }) }]),
    permissions: Object.freeze([
      {
        id: 'oidc-application',
        permissions: Object.freeze([
          'oidc:application:list',
          'oidc:application:query',
          'oidc:application:add',
          'oidc:application:edit',
          'oidc:application:remove',
          'oidc:application:rotate'
        ])
      },
      {
        id: 'oidc-service',
        permissions: Object.freeze([
          'oidc:service:query',
          'oidc:service:edit',
          'oidc:key:manage',
          'oidc:logout:query',
          'oidc:logout:retry'
        ])
      }
    ]),
    registrations: Object.freeze([
      {
        id: 'oidc-service',
        componentKey: 'oidc/service',
        componentName: 'OidcService',
        load: async () => {
          const { default: page } = await import('./OidcServicePage.vue');
          return defineComponent({ name: 'OidcService', setup: () => () => h(page, { runtime }) });
        }
      },
      {
        id: 'oidc-application',
        componentKey: 'oidc/application/index',
        componentName: 'OidcApplication',
        load: async () => {
          const { default: page } = await import('./OidcApplicationPage.vue');
          return defineComponent({ name: 'OidcApplication', setup: () => () => h(page, { runtime }) });
        }
      }
    ])
  });
}
