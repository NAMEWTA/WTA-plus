import type { WebDomainManifest, WebRegistration } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import { requireProfileSelfWebRuntime, type ProfileSelfWebRuntime } from './runtime';

const view = (name: string, runtime: ProfileSelfWebRuntime, load: () => Promise<{ default: Component }>) =>
  load().then(module => defineComponent({ name, setup: () => () => h(module.default, { runtime }) }));

export function createProfileSelfWebDomain(runtimeInput: ProfileSelfWebRuntime | undefined): WebDomainManifest<Component> {
  const runtime = requireProfileSelfWebRuntime(runtimeInput);
  const registrations: readonly WebRegistration<Component>[] = [
    Object.freeze({ id: 'profile-self-center', componentKey: 'profile/center/index', componentName: 'ProfileCenter', load: () => view('ProfileCenter', runtime, () => import('./ProfileCenterPage.vue')) }),
    Object.freeze({ id: 'profile-self-person', componentKey: 'profile/person/application', componentName: 'PersonVerification', load: () => view('PersonVerification', runtime, () => import('./PersonVerificationPage.vue')) }),
    Object.freeze({ id: 'profile-self-enterprise', componentKey: 'profile/enterprise/application', componentName: 'EnterpriseVerification', load: () => view('EnterpriseVerification', runtime, () => import('./EnterpriseVerificationPage.vue')) })
  ];
  return Object.freeze({
    id: 'web-domain-profile-self',
    domainId: 'profile',
    messages: Object.freeze([Object.freeze({ namespace: 'profileSelf', messages: Object.freeze({ title: '档案中心' }) })]),
    permissions: Object.freeze([]),
    registrations: Object.freeze(registrations)
  });
}
