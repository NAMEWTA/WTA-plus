import type { PersonProfileDetail } from '@namewta/domain-profile';
import { describe, expect, it, vi } from 'vitest';
import {
  bindingAction,
  flattenPersonMaterialOptions,
  isPersonIdentityComplete,
  personActionMatrix,
  safeErrorMessage
} from './logic';

const detail = (status = 'ACTIVE'): PersonProfileDetail =>
  ({
    profile: { status },
    bindings: [{ status: 'ACTIVE' }],
    versions: [],
    sources: [],
    audits: [],
    currentMaterials: []
  }) as never;

describe('person profile action matrix', () => {
  it('projects closed capabilities without hidden companion permissions', () => {
    const hasPermission = vi.fn(permission => permission === 'profile:person:manage');
    expect(personActionMatrix(detail(), hasPermission)).toEqual({
      assign: false,
      manageBinding: true,
      revise: false,
      revoke: false
    });
  });

  it('makes a revoked profile fully read-only even with every capability', () => {
    expect(personActionMatrix(detail('REVOKED'), () => true)).toEqual({
      assign: false,
      manageBinding: false,
      revise: false,
      revoke: false
    });
  });

  it('maps binding status to an explicit append-only command and keeps errors generic', () => {
    expect(bindingAction('ACTIVE')).toBe('SUSPEND');
    expect(bindingAction('SUSPENDED')).toBe('RESUME');
    expect(bindingAction('UNBOUND')).toBe('UNBIND');
    expect(safeErrorMessage({ documentNumber: 'sensitive' })).toBe('操作失败，请刷新后重试');
    expect(safeErrorMessage(new Error('身份证 110101199001011234 已存在'))).toBe('操作失败，请刷新后重试');
  });

  it('does not expose a binding command after the latest binding is unbound', () => {
    const unbound = detail();
    unbound.bindings = [{ status: 'UNBOUND' }] as never;
    expect(personActionMatrix(unbound, () => true).manageBinding).toBe(false);
  });

  it('requires every identity field and projects enabled person/common material tags', () => {
    expect(
      isPersonIdentityComplete({
        fullName: '张三',
        documentTypeCode: 'CN_RESIDENT_ID',
        documentNumber: '110101199001011234',
        gender: '0',
        birthDate: '1990-01-01',
        validFrom: '2020-01-01',
        validUntil: '2040-01-01'
      })
    ).toBe(true);
    expect(
      flattenPersonMaterialOptions([
        {
          materialNodeId: 1,
          parentId: 0,
          nodeType: 'CATEGORY',
          nodeDepth: 1,
          scope: 'PERSON',
          materialTagCode: null,
          nodeName: '个人',
          systemRequired: false,
          enabled: true,
          orderNum: 1,
          version: 1,
          children: [
            {
              materialNodeId: 2,
              parentId: 1,
              nodeType: 'TAG',
              nodeDepth: 2,
              scope: 'PERSON',
              materialTagCode: 'ID_FRONT',
              nodeName: '证件人像面',
              systemRequired: true,
              enabled: true,
              orderNum: 1,
              version: 1,
              children: []
            }
          ]
        }
      ])
    ).toEqual([{ label: '个人 / 证件人像面', materialNodeId: 2 }]);
  });
});
