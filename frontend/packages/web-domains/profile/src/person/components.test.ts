import { describe, expect, it } from 'vitest';
import review from '../review/ProfileReviewPage.vue?raw';
import detail from './PersonProfileDetailPanel.vue?raw';
import page from './PersonProfilePage.vue?raw';

describe('person profile page contracts', () => {
  it('keeps direct create usable with tagged uploads and host user lookup', () => {
    expect(page).toContain('runtime.fileUpload');
    expect(page).toContain(':file-size="10"');
    expect(page).toContain('materialNodeId');
    expect(page).toContain('runtime.findUsers');
    expect(page).not.toContain('materials: []');
  });

  it('keeps high-risk detail commands confirmed, reasoned and append-only', () => {
    expect(detail).toContain('该档案已注销，全部历史只读');
    expect(detail).toContain('请填写操作原因');
    expect(detail).toContain('runtime.confirm');
    expect(detail).toContain('runtime.findUsers');
    expect(detail).toContain("findUsers('PERSON'");
    expect(detail).toContain('不可变来源');
    expect(detail).toContain('currentVersion.validUntil');
    expect(detail).not.toMatch(/\bdelete\b|\bexport\b|console\./i);
  });

  it('loads workflow review and materials from the closed review capability', () => {
    expect(review).toContain('archive.review(applicationId.value)');
    expect(review).toContain('archive.reviewMaterial(context.value.applicationId');
    expect(review).toContain('taskReview.decide(taskId.value');
    expect(review).toContain('taskReview.context(taskId.value)');
    expect(review).not.toContain('<pre>');
    expect(review).toContain('decision: overrideDecision.value');
    expect(review).toContain('拒绝（终态）');
    expect(review).toContain('archive.decide(context.value.applicationId');
    expect(review).not.toMatch(/console\./);
  });
});
