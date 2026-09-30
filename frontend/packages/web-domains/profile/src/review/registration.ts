import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import { requireProfileReviewWebRuntime, type ProfileReviewWebRuntime } from './runtime';
export function createProfileReviewWebDomain(input: ProfileReviewWebRuntime | undefined): WebDomainManifest<Component> {
  const runtime = requireProfileReviewWebRuntime(input);
  return Object.freeze({
    id: 'web-domain-profile-review',
    domainId: 'profile',
    messages: Object.freeze([]),
    permissions: Object.freeze([
      {
        id: 'profile-task-review',
        permissions: Object.freeze(['profile:person:task-review', 'profile:enterprise:task-review'])
      }
    ]),
    registrations: Object.freeze([
      {
        id: 'profile-person-review',
        componentKey: 'profile/person/review',
        componentName: 'PersonProfileReview',
        load: () =>
          import('../person/PersonProfileReviewPage.vue').then(module =>
            defineComponent({ name: 'PersonProfileReview', setup: () => () => h(module.default, { runtime }) })
          )
      },
      {
        id: 'profile-enterprise-review',
        componentKey: 'profile/enterprise/review',
        componentName: 'EnterpriseProfileReview',
        load: () =>
          import('../enterprise/EnterpriseProfileReviewPage.vue').then(module =>
            defineComponent({ name: 'EnterpriseProfileReview', setup: () => () => h(module.default, { runtime }) })
          )
      }
    ])
  });
}
