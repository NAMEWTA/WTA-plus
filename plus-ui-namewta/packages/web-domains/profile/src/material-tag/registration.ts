import type { WebPermissionContribution, WebRegistration } from '@namewta/platform-app-runtime';
import { profilePermissions } from '@namewta/domain-profile';
import { defineComponent, h, type Component } from 'vue';
import { requireProfileWebRuntime, type ProfileWebRuntime } from '../runtime';

async function runtimeView(runtime: ProfileWebRuntime): Promise<Component> {
  const page = (await import('./MaterialTagPage.vue')).default;
  return defineComponent({ name: 'ProfileMaterialTag', setup: () => () => h(page, { runtime }) });
}

export function createMaterialTagWebContribution(runtimeInput: ProfileWebRuntime | undefined): {
  permissions: readonly WebPermissionContribution[];
  registrations: readonly WebRegistration<Component>[];
} {
  const runtime = requireProfileWebRuntime(runtimeInput);
  return Object.freeze({
    permissions: Object.freeze([
      Object.freeze({
        id: 'profile-material-tag',
        permissions: Object.freeze(Object.values(profilePermissions.materialTag))
      })
    ]),
    registrations: Object.freeze([
      Object.freeze({
        id: 'profile-material-tag',
        componentKey: 'profile/materialTag/index',
        componentName: 'ProfileMaterialTag',
        load: () => runtimeView(runtime)
      })
    ])
  });
}
