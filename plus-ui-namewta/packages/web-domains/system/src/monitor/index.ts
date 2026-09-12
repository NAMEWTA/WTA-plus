import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import type { MonitorWebRuntime } from './runtime';
export { createLiveMonitorDictRefs, type MonitorDictOption, type MonitorWebRuntime } from './runtime';

async function runtimeView(name: string, runtime: MonitorWebRuntime, load: () => Promise<{ default: Component }>) {
  const page = (await load()).default;
  return defineComponent({ name, setup: () => () => h(page, { runtime }) });
}
export function createMonitorWebDomain(runtime: MonitorWebRuntime): WebDomainManifest<Component> {
  const registrations = [
    [
      'system-monitor-online',
      'monitor/online/index',
      'Online',
      () => runtimeView('Online', runtime, () => import('./online/OnlinePage.vue'))
    ],
    [
      'system-monitor-cache',
      'monitor/cache/index',
      'Cache',
      () => runtimeView('Cache', runtime, () => import('./cache/CachePage.vue'))
    ],
    [
      'system-monitor-operlog',
      'monitor/operlog/index',
      'Operlog',
      () => runtimeView('Operlog', runtime, () => import('./operlog/OperationLogPage.vue'))
    ],
    [
      'system-monitor-logininfo',
      'monitor/logininfo/index',
      'LoginInfo',
      () => runtimeView('LoginInfo', runtime, () => import('./login-info/LoginInfoPage.vue'))
    ],
  ] as const;
  const permissions = {
    online: ['list', 'query', 'batchLogout', 'forceLogout'],
    cache: ['list'],
    operlog: ['list', 'query', 'remove', 'export'],
    logininfo: ['list', 'query', 'remove', 'unlock', 'export']
  } as const;
  return Object.freeze({
    id: 'web-domain-system-monitor',
    domainId: 'system',
    messages: Object.freeze([]),
    permissions: Object.freeze(
      Object.entries(permissions).map(([slice, actions]) =>
        Object.freeze({
          id: `monitor-${slice}`,
          permissions: Object.freeze(actions.map(action => `monitor:${slice}:${action}`))
        })
      ),
    ),
    registrations: Object.freeze(
      registrations.map(([id, componentKey, componentName, load]) =>
        Object.freeze({ id, componentKey, componentName, load })
      )
    )
  });
}
