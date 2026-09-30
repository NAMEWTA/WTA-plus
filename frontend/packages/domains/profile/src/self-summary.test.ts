import { describe, expect, it } from 'vitest';
import { projectEnterpriseSummaryResponse, projectPersonSummaryResponse } from './self-summary';

describe('owner certification summary projection', () => {
  it('keeps a completed profile when no editable application exists and excludes internal identity keys', () => {
    const response = projectPersonSummaryResponse({
      code: 200,
      data: {
        status: 'VERIFIED',
        returnReason: null,
        currentApplication: null,
        certifiedProfile: {
          profileId: '5',
          verifiedAt: '2026-09-30T12:00:00Z',
          identity: {
            fullName: '张三',
            documentNumber: '110101199001011237',
            documentTypeCode: 'CN_RESIDENT_ID',
            gender: 'MALE',
            identityKey: 'internal-secret'
          }
        }
      }
    });
    expect(response.data.status).toBe('VERIFIED');
    expect(response.data.currentApplication).toBeNull();
    expect(response.data.certifiedProfile?.identity.fullName).toBe('张三');
    expect(response.data.certifiedProfile?.identity).not.toHaveProperty('identityKey');
  });
  it('retains return reason and draft version for resubmission', () => {
    const response = projectPersonSummaryResponse({
      data: {
        status: 'BACK',
        returnReason: '请补充证件反面',
        currentApplication: {
          personApplicationId: '8',
          fullName: '张三',
          status: 'BACK',
          version: 4,
          snapshotVersion: 2
        },
        certifiedProfile: null
      }
    });
    expect(response.data.returnReason).toBe('请补充证件反面');
    expect(response.data.currentApplication?.version).toBe(4);
    expect(response.data.currentApplication?.snapshotVersion).toBe(2);
  });
  it('accepts a certified enterprise without the non-versioned handler flag', () => {
    const response = projectEnterpriseSummaryResponse({
      data: {
        status: 'VERIFIED',
        returnReason: null,
        currentApplication: null,
        certifiedProfile: {
          profileId: '11',
          verifiedAt: null,
          identity: { enterpriseName: '示例企业', registeredCapital: 100 }
        }
      }
    });
    expect(response.data.certifiedProfile?.identity.enterpriseName).toBe('示例企业');
    expect(response.data.certifiedProfile?.identity).not.toHaveProperty('handlerIsLegalRepresentative');
  });
  it.each(['UNKNOWN', null, 1])('rejects an unknown certification state %s', status => {
    expect(() => projectPersonSummaryResponse({ data: { status } })).toThrow('Profile 响应不可用');
  });
});
