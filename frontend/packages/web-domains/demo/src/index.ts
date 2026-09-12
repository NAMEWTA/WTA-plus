import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import type { DemoWebRuntime } from './runtime';
import { requireDemoWebRuntime } from './runtime';

export type { DemoWebRuntime } from './runtime';

async function runtimeView(
  name: string,
  runtime: DemoWebRuntime,
  load: () => Promise<{ default: Component }>
): Promise<Component> {
  const page = (await load()).default;
  return defineComponent({
    name,
    setup: () => () => h(page, { runtime })
  });
}

export const loadDemoPage = (runtime: DemoWebRuntime) =>
  runtimeView('Demo', requireDemoWebRuntime(runtime), () => import('./test-demo/DemoPage.vue'));

export const loadTreePage = (runtime: DemoWebRuntime) =>
  runtimeView('Tree', requireDemoWebRuntime(runtime), () => import('./test-tree/TreePage.vue'));

export const loadRichTextPage = (runtime: DemoWebRuntime) =>
  runtimeView('RichTextDemo', requireDemoWebRuntime(runtime), () => import('./test-rich-text/RichTextPage.vue'));

export function createDemoWebDomain(runtimeInput: DemoWebRuntime | undefined): WebDomainManifest<Component> {
  const runtime = requireDemoWebRuntime(runtimeInput);
  return Object.freeze({
    id: 'web-domain-demo',
    domainId: 'demo',
    messages: Object.freeze([
      Object.freeze({
        namespace: 'demo',
        messages: Object.freeze({ tableTitle: '测试单列表', treeTitle: '测试树列表' })
      })
    ]),
    permissions: Object.freeze([
      Object.freeze({
        id: 'demo-table',
        permissions: Object.freeze([
          'demo:demo:list',
          'demo:demo:add',
          'demo:demo:edit',
          'demo:demo:remove',
          'demo:demo:export'
        ])
      }),
      Object.freeze({
        id: 'demo-tree',
        permissions: Object.freeze(['demo:tree:list', 'demo:tree:add', 'demo:tree:edit', 'demo:tree:remove'])
      }),
      Object.freeze({
        id: 'demo-rich-text',
        permissions: Object.freeze([
          'demo:richtext:list', 'demo:richtext:query', 'demo:richtext:add', 'demo:richtext:edit', 'demo:richtext:remove', 'common:richtext:upload'
        ])
      })
    ]),
    registrations: Object.freeze([
      Object.freeze({
        id: 'demo-table',
        componentKey: 'demo/demo/index',
        componentName: 'Demo',
        load: () => loadDemoPage(runtime)
      }),
      Object.freeze({
        id: 'demo-tree',
        componentKey: 'demo/tree/index',
        componentName: 'Tree',
        load: () => loadTreePage(runtime)
      }),
      Object.freeze({
        id: 'demo-rich-text',
        componentKey: 'demo/rich-text/index',
        componentName: 'RichTextDemo',
        load: () => loadRichTextPage(runtime)
      })
    ])
  });
}
