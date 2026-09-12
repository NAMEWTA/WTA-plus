import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import type { ThirdWebRuntime } from './runtime';
import { ThirdPage } from './pages';

export type { ThirdWebRuntime } from './runtime';
type ThirdPageKind = 'providers' | 'endpoints' | 'invocations' | 'statistics';
const view = (name: string, kind: ThirdPageKind, runtime: ThirdWebRuntime) => defineComponent({ name, setup: () => () => h(ThirdPage, { runtime, kind }) });
export function createThirdWebDomain(runtime: ThirdWebRuntime): WebDomainManifest<Component> {
  const registrations = [
    ['third-provider', 'third/provider/index', 'ThirdProvider', view('ThirdProvider', 'providers', runtime)],
    ['third-endpoint', 'third/endpoint/index', 'ThirdEndpoint', view('ThirdEndpoint', 'endpoints', runtime)],
    ['third-invocation', 'third/invocation/index', 'ThirdInvocation', view('ThirdInvocation', 'invocations', runtime)],
    ['third-statistics', 'third/statistics/index', 'ThirdStatistics', view('ThirdStatistics', 'statistics', runtime)]
  ] as const;
  return Object.freeze({ id: 'web-domain-third', domainId: 'third', messages: Object.freeze([{ namespace: 'thirdAdmin', messages: Object.freeze({ title: '三方接口管理' }) }]), permissions: Object.freeze([
    Object.freeze({ id: 'third-provider', permissions: Object.freeze(['third:provider:list', 'third:provider:query', 'third:provider:add', 'third:provider:edit', 'third:provider:remove', 'third:credential:list', 'third:credential:add', 'third:credential:remove']) }),
    Object.freeze({ id: 'third-endpoint', permissions: Object.freeze(['third:endpoint:list', 'third:endpoint:query', 'third:endpoint:add', 'third:endpoint:edit', 'third:endpoint:remove']) }),
    Object.freeze({ id: 'third-invocation', permissions: Object.freeze(['third:invocation:list']) }),
    Object.freeze({ id: 'third-statistics', permissions: Object.freeze(['third:statistics:list']) })
  ]), registrations: Object.freeze(registrations.map(([id, componentKey, componentName, component]) => Object.freeze({ id, componentKey, componentName, load: async () => component }))) });
}
