import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import type { AiWebRuntime } from './runtime';
import { requireAiWebRuntime } from './runtime';

export type { AiWebRuntime } from './runtime';

export async function loadAiChatPage(runtimeInput: AiWebRuntime | undefined): Promise<Component> {
  const runtime = requireAiWebRuntime(runtimeInput);
  const page = (await import('./snail-ai/AiChatPage.vue')).default;
  return defineComponent({
    name: 'AiChatPage',
    setup: () => () => h(page, { runtime })
  });
}

export function createAiWebDomain(runtimeInput: AiWebRuntime | undefined): WebDomainManifest<Component> {
  const runtime = requireAiWebRuntime(runtimeInput);
  return Object.freeze({
    id: 'web-domain-ai',
    domainId: 'ai',
    messages: Object.freeze([
      Object.freeze({
        namespace: 'ai',
        messages: Object.freeze({ chatTitle: 'Snail AI', retry: '重新加载' })
      })
    ]),
    permissions: Object.freeze([]),
    registrations: Object.freeze([
      Object.freeze({
        id: 'embedded-chat',
        componentKey: 'ai/chat/index',
        componentName: 'AiChatPage',
        load: () => loadAiChatPage(runtime)
      })
    ])
  });
}
