import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import type { Component } from 'vue';
import { createEnterpriseWebContribution } from './enterprise/registration';
import { createMaterialTagWebContribution } from './material-tag/registration';
import { createPersonWebContribution } from './person/registration';
import { requireProfileWebRuntime, type ProfileWebRuntime } from './runtime';
export { createProfileSelfWebDomain, requireProfileSelfWebRuntime, type ProfileSelfWebRuntime } from './self';

export {
  requireProfileWebRuntime,
  type ProfileUserOption,
  type ProfileWebRuntime,
  type ProfileWorkflowCommand
} from './runtime';

export function createProfileWebDomain(runtimeInput: ProfileWebRuntime | undefined): WebDomainManifest<Component> {
  const runtime = requireProfileWebRuntime(runtimeInput);
  const materialTag = createMaterialTagWebContribution(runtime);
  const person = createPersonWebContribution(runtime);
  const enterprise = createEnterpriseWebContribution(runtime);
  return Object.freeze({
    id: 'web-domain-profile',
    domainId: 'profile',
    messages: Object.freeze([
      Object.freeze({
        namespace: 'profileAdmin',
        messages: Object.freeze({ title: '档案管理', person: '个人档案', enterprise: '企业档案', material: '材料标签' })
      })
    ]),
    permissions: Object.freeze([...materialTag.permissions, ...person.permissions, ...enterprise.permissions]),
    registrations: Object.freeze([...materialTag.registrations, ...person.registrations, ...enterprise.registrations])
  });
}
