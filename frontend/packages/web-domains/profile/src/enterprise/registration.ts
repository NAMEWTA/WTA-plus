import type { WebPermissionContribution, WebRegistration } from '@namewta/platform-app-runtime';
import { profilePermissions } from '@namewta/domain-profile';
import { defineComponent, h, type Component } from 'vue';
import { requireProfileWebRuntime, type ProfileWebRuntime } from '../runtime';

async function runtimeView(
  name: string,
  runtime: ProfileWebRuntime,
  load: () => Promise<{ default: Component }>
): Promise<Component> {
  const page = (await load()).default;
  return defineComponent({ name, setup: () => () => h(page, { runtime }) });
}

export function createEnterpriseWebContribution(runtimeInput: ProfileWebRuntime | undefined): {
  permissions: readonly WebPermissionContribution[];
  registrations: readonly WebRegistration<Component>[];
} {
  const runtime = requireProfileWebRuntime(runtimeInput);
  const registrations = [
    [
      'profile-enterprise',
      'profile/enterprise/index',
      'EnterpriseProfile',
      () => import('./EnterpriseProfilePage.vue')
    ],
    [
      'profile-enterprise-detail',
      'profile/enterprise/detail',
      'EnterpriseProfileDetail',
      () => import('./EnterpriseProfileDetailPage.vue')
    ],
    [
      'profile-enterprise-review',
      'profile/enterprise/review',
      'EnterpriseProfileReview',
      () => import('./EnterpriseProfileReviewPage.vue')
    ]
  ] as const;
  return Object.freeze({
    permissions: Object.freeze([
      Object.freeze({
        id: 'profile-enterprise',
        permissions: Object.freeze(Object.values(profilePermissions.enterprise))
      })
    ]),
    registrations: Object.freeze(
      registrations.map(([id, componentKey, componentName, load]) =>
        Object.freeze({ id, componentKey, componentName, load: () => runtimeView(componentName, runtime, load) })
      )
    )
  });
}
