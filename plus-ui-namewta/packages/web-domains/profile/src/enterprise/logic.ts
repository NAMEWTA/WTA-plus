import {
  profilePermissions,
  type EnterpriseIdentity,
  type EnterpriseProfileDetail,
  type MaterialNode
} from '@namewta/domain-profile';
import type { ProfileWebRuntime } from '../runtime';

export interface EnterpriseActionMatrix {
  assign: boolean;
  manageBinding: boolean;
  revise: boolean;
  revoke: boolean;
}

export function enterpriseActionMatrix(
  detail: EnterpriseProfileDetail,
  hasPermission: ProfileWebRuntime['hasPermission']
): EnterpriseActionMatrix {
  const writable = detail.profile.status !== 'REVOKED';
  return Object.freeze({
    assign: writable && hasPermission(profilePermissions.enterprise.override),
    manageBinding:
      writable &&
      hasPermission(profilePermissions.enterprise.manage) &&
      detail.bindings.some(binding => binding.status === 'ACTIVE' || binding.status === 'SUSPENDED'),
    revise: writable && hasPermission(profilePermissions.enterprise.override),
    revoke: writable && hasPermission(profilePermissions.enterprise.override)
  });
}

export function enterpriseBindingAction(status: string): 'RESUME' | 'SUSPEND' | 'UNBIND' {
  return status === 'SUSPENDED' ? 'RESUME' : status === 'ACTIVE' ? 'SUSPEND' : 'UNBIND';
}

export function isEnterpriseIdentityComplete(identity: EnterpriseIdentity): boolean {
  return [
    identity.enterpriseName,
    identity.unifiedCreditCode,
    identity.enterpriseType,
    identity.legalRepresentativeName,
    identity.legalDocumentTypeCode,
    identity.legalDocumentNumber,
    identity.establishedDate,
    identity.registeredAddress,
    identity.businessScope
  ].every(value => value.trim().length > 0);
}

export interface EnterpriseMaterialOption {
  label: string;
  materialNodeId: MaterialNode['materialNodeId'];
}

export function flattenEnterpriseMaterialOptions(
  nodes: readonly MaterialNode[],
  parents: readonly string[] = []
): EnterpriseMaterialOption[] {
  return nodes.flatMap<EnterpriseMaterialOption>(node => {
    const path = [...parents, node.nodeName];
    const current =
      node.nodeType === 'TAG' && node.enabled && (node.scope === 'ENTERPRISE' || node.scope === 'COMMON')
        ? [{ label: path.join(' / '), materialNodeId: node.materialNodeId }]
        : [];
    return [...current, ...flattenEnterpriseMaterialOptions(node.children, path)];
  });
}

export function safeEnterpriseErrorMessage(_error: unknown): string {
  return '操作失败，请刷新后重试';
}
