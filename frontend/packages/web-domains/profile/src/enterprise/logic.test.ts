import type { EnterpriseProfileDetail } from '@namewta/domain-profile';
import { describe, expect, it, vi } from 'vitest';
import {
  enterpriseActionMatrix,
  enterpriseBindingAction,
  flattenEnterpriseMaterialOptions,
  isEnterpriseIdentityComplete,
  safeEnterpriseErrorMessage
} from './logic';

const detail = (status = 'ACTIVE'): EnterpriseProfileDetail =>
  ({
    profile: { status },
    bindings: [{ status: 'ACTIVE' }],
    versions: [],
    sources: [],
    audits: [],
    currentMaterials: []
  }) as never;

describe('enterprise profile action matrix', () => {
  it('projects each closed capability independently', () => {
    const hasPermission = vi.fn(permission => permission === 'profile:enterprise:manage');
    expect(enterpriseActionMatrix(detail(), hasPermission)).toEqual({
      assign: false,
      manageBinding: true,
      revise: false,
      revoke: false
    });
  });

  it('makes revoked profiles and unbound binding history read-only', () => {
    expect(enterpriseActionMatrix(detail('REVOKED'), () => true)).toEqual({
      assign: false,
      manageBinding: false,
      revise: false,
      revoke: false
    });
    const unbound = detail();
    unbound.bindings = [{ status: 'UNBOUND' }] as never;
    expect(enterpriseActionMatrix(unbound, () => true).manageBinding).toBe(false);
  });

  it('keeps legal identity validation separate from the responsible account', () => {
    expect(
      isEnterpriseIdentityComplete({
        enterpriseName: '示例科技有限公司',
        unifiedCreditCode: '91110000123456789X',
        enterpriseType: 'LIMITED_COMPANY',
        legalRepresentativeName: '张三',
        legalDocumentTypeCode: 'CN_RESIDENT_ID',
        legalDocumentNumber: '110101199001011234',
        establishedDate: '2020-01-01',
        businessTermFrom: '',
        businessTermUntil: '',
        registeredAddress: '北京市朝阳区',
        businessScope: '技术服务',
        contactName: '',
        contactPhone: '',
        email: '',
        registeredCapital: 0,
        industryCode: '',
        website: ''
      })
    ).toBe(true);
  });

  it('maps binding commands, filters material scopes and never reflects raw errors', () => {
    expect(enterpriseBindingAction('ACTIVE')).toBe('SUSPEND');
    expect(enterpriseBindingAction('SUSPENDED')).toBe('RESUME');
    expect(
      flattenEnterpriseMaterialOptions([
        {
          materialNodeId: 1,
          parentId: 0,
          nodeType: 'TAG',
          nodeDepth: 1,
          scope: 'ENTERPRISE',
          materialTagCode: 'LICENSE',
          nodeName: '营业执照',
          systemRequired: true,
          enabled: true,
          orderNum: 1,
          version: 1,
          children: []
        }
      ])
    ).toEqual([{ label: '营业执照', materialNodeId: 1 }]);
    expect(safeEnterpriseErrorMessage(new Error('信用代码 91110000123456789X 已存在'))).toBe('操作失败，请刷新后重试');
  });
});
