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

export function createPersonWebContribution(runtimeInput: ProfileWebRuntime | undefined): {
  permissions: readonly WebPermissionContribution[];
  registrations: readonly WebRegistration<Component>[];
} {
  const runtime = requireProfileWebRuntime(runtimeInput);
  const registrations = [
    ['profile-person', 'profile/person/index', 'PersonProfile', () => import('./PersonProfilePage.vue')],
    [
      'profile-person-detail',
      'profile/person/detail',
      'PersonProfileDetail',
      () => import('./PersonProfileDetailPage.vue')
    ],
    [
      'profile-person-review',
      'profile/person/review',
      'PersonProfileReview',
      () => import('./PersonProfileReviewPage.vue')
    ]
  ] as const;
  return Object.freeze({
    permissions: Object.freeze([
      Object.freeze({ id: 'profile-person', permissions: Object.freeze(Object.values(profilePermissions.person)) })
    ]),
    registrations: Object.freeze(
      registrations.map(([id, componentKey, componentName, load]) =>
        Object.freeze({ id, componentKey, componentName, load: () => runtimeView(componentName, runtime, load) })
      )
    )
  });
}
