import { describe, expect, it } from 'vitest';
import detail from './EnterpriseProfileDetailPanel.vue?raw';
import page from './EnterpriseProfilePage.vue?raw';
import review from './EnterpriseProfileReviewPage.vue?raw';

describe('enterprise profile page contracts', () => {
  it('keeps legal representative and responsible account as distinct concepts', () => {
    expect(page).toContain('legalRepresentativeName');
    expect(page).toContain('认证负责人账户');
    expect(detail).toContain('法定代表人是企业法定字段');
    expect(detail).toContain('认证负责人是当前绑定的系统账户');
    expect(detail).toContain('runtime.findUsers');
    expect(detail).toContain("findUsers('ENTERPRISE'");
    expect(detail).toContain('currentVersion.legalDocumentNumber');
    expect(detail).toContain('currentVersion.businessScope');
    expect(detail).toContain('currentVersion.registeredCapital');
  });

  it('makes direct create functional with tagged host uploads', () => {
    expect(page).toContain('runtime.fileUpload');
    expect(page).toContain(':file-size="10"');
    expect(page).toContain('materialNodeId');
    expect(page).toContain("materialTags.tree('ENTERPRISE')");
    expect(page).not.toContain('materials: []');
  });

  it('keeps detail commands reasoned, confirmed, append-only and revoked read-only', () => {
    expect(detail).toContain('该企业档案已注销，全部历史只读');
    expect(detail).toContain('请填写操作原因');
    expect(detail).toContain('runtime.confirm');
    expect(detail).toContain('不可变来源');
    expect(detail).not.toMatch(/\bdelete\b|\bexport\b|console\./i);
  });

  it('uses review-only endpoints and maps override decisions to the backend contract', () => {
    expect(review).toContain('archive.review(applicationId.value)');
    expect(review).toContain('archive.reviewMaterial(applicationId.value');
    expect(review).toContain("decision.value === 'APPROVE' ? 'APPROVED' : 'REJECTED'");
    expect(review).toContain('completeWorkflowTask');
    expect(review).not.toMatch(/console\./);
  });
});
