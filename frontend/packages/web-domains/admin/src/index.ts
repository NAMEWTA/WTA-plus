import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import type { IdentityAccessWebRuntime } from './runtime';
import { requireIdentityAccessWebRuntime } from './runtime';

export type { IdentityAccessWebRuntime } from './runtime';
export { createSocialWebRuntime } from './socialRuntime';
export type { SocialWebRuntime, SocialWebPorts } from './socialRuntime';
export { default as SocialCallbackPage } from './auth/SocialCallbackPage.vue';
export { default as SocialBindingsPage } from './auth/SocialBindingsPage.vue';
export { default as LogoutCallbackPage } from './auth/LogoutCallbackPage.vue';

export const identityAccessWebMessages = Object.freeze({
  title: '客户服务入口',
  unavailable: '客户端认证配置不可用，无法登录'
});

async function runtimeView(runtime: IdentityAccessWebRuntime): Promise<Component> {
  const page = (await import('./auth/LoginPage.vue')).default;
  return defineComponent({
    name: 'IdentityLogin',
    setup: () => () => h(page, { runtime })
  });
}

export function createAdminWebDomain(runtimeInput: IdentityAccessWebRuntime | undefined): WebDomainManifest<Component> {
  const runtime = requireIdentityAccessWebRuntime(runtimeInput);
  return Object.freeze({
    id: 'web-domain-admin',
    domainId: 'admin',
    messages: Object.freeze([
      Object.freeze({
        namespace: 'admin',
        messages: identityAccessWebMessages
      })
    ]),
    permissions: Object.freeze([Object.freeze({ id: 'identity-login', permissions: Object.freeze([] as string[]) })]),
    registrations: Object.freeze([
      Object.freeze({
        id: 'identity-login',
        componentKey: 'identity-access/login/index',
        componentName: 'IdentityLogin',
        load: () => runtimeView(runtime)
      })
    ])
  });
}
