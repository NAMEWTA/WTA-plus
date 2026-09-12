import {
  profilePermissions,
  type MaterialNode,
  type PersonIdentity,
  type PersonProfileDetail
} from '@namewta/domain-profile';
import type { ProfileWebRuntime } from '../runtime';

export interface PersonActionMatrix {
  assign: boolean;
  manageBinding: boolean;
  revise: boolean;
  revoke: boolean;
}

export function personActionMatrix(
  detail: PersonProfileDetail,
  hasPermission: ProfileWebRuntime['hasPermission']
): PersonActionMatrix {
  const writable = detail.profile.status !== 'REVOKED';
  return Object.freeze({
    assign: writable && hasPermission(profilePermissions.person.override),
    manageBinding:
      writable &&
      hasPermission(profilePermissions.person.manage) &&
      detail.bindings.some(binding => binding.status === 'ACTIVE' || binding.status === 'SUSPENDED'),
    revise: writable && hasPermission(profilePermissions.person.override),
    revoke: writable && hasPermission(profilePermissions.person.override)
  });
}

export function bindingAction(status: string): 'RESUME' | 'SUSPEND' | 'UNBIND' {
  return status === 'SUSPENDED' ? 'RESUME' : status === 'ACTIVE' ? 'SUSPEND' : 'UNBIND';
}

export function isPersonIdentityComplete(identity: PersonIdentity): boolean {
  return Object.values(identity).every(value => value.trim().length > 0);
}

export interface PersonMaterialOption {
  label: string;
  materialNodeId: MaterialNode['materialNodeId'];
}

export function flattenPersonMaterialOptions(nodes: readonly MaterialNode[], parents: readonly string[] = []) {
  return nodes.flatMap<PersonMaterialOption>(node => {
    const path = [...parents, node.nodeName];
    const current =
      node.nodeType === 'TAG' && node.enabled && (node.scope === 'PERSON' || node.scope === 'COMMON')
        ? [{ label: path.join(' / '), materialNodeId: node.materialNodeId }]
        : [];
    return [...current, ...flattenPersonMaterialOptions(node.children, path)];
  });
}

export function safeErrorMessage(_error: unknown): string {
  return '操作失败，请刷新后重试';
}
