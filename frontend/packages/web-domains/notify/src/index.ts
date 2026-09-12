import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import type { NotifyWebRuntime } from './runtime';
export type { NotifyWebRuntime, NotifyUserDirectory } from './runtime';

export function createNotifyWebDomain(runtime: NotifyWebRuntime): WebDomainManifest<Component> {
  return Object.freeze({
    id: 'web-domain-notify',
    domainId: 'notify',
    messages: Object.freeze([]),
    permissions: Object.freeze([
      Object.freeze({
        id: 'notify-monitor',
        permissions: Object.freeze(['notify:monitor:list', 'notify:monitor:query'])
      }),
      Object.freeze({
        id: 'notify-notice',
        permissions: Object.freeze([
          'notify:notice:list',
          'notify:notice:query',
          'notify:notice:add',
          'notify:notice:edit',
          'notify:notice:publish',
          'notify:notice:retract',
          'notify:notice:remove'
        ])
      }),
      Object.freeze({
        id: 'notify-inbox',
        permissions: Object.freeze(['notify:inbox:list', 'notify:inbox:seen', 'notify:inbox:read'])
      }),
      Object.freeze({
        id: 'notify-config',
        permissions: Object.freeze([
          'notify:config:list',
          'notify:config:query',
          'notify:config:add',
          'notify:config:edit',
          'notify:config:remove',
          'notify:config:test'
        ])
      })
    ]),
    registrations: Object.freeze([
      Object.freeze({
        id: 'notify-monitor',
        componentKey: 'notify/monitor/index',
        componentName: 'NotificationMonitor',
        load: async () => {
          const page = (await import('./NotificationPage.vue')).default;
          return defineComponent({ name: 'NotificationMonitor', setup: () => () => h(page, { runtime }) });
        }
      }),
      Object.freeze({
        id: 'notify-notice',
        componentKey: 'notify/notice/index',
        componentName: 'NotifyNotice',
        load: async () => {
          const page = (await import('./NoticePage.vue')).default;
          return defineComponent({ name: 'NotifyNotice', setup: () => () => h(page, { runtime }) });
        }
      }),
      Object.freeze({
        id: 'notify-inbox',
        componentKey: 'notify/inbox/index',
        componentName: 'NotifyInbox',
        load: async () => {
          const page = (await import('./InboxPage.vue')).default;
          return defineComponent({ name: 'NotifyInbox', setup: () => () => h(page, { runtime }) });
        }
      }),
      Object.freeze({
        id: 'notify-config',
        componentKey: 'notify/config/index',
        componentName: 'NotifyConfig',
        load: async () => {
          const page = (await import('./ConfigPage.vue')).default;
          return defineComponent({ name: 'NotifyConfig', setup: () => () => h(page, { runtime }) });
        }
      })
    ])
  });
}
