import { describe, expect, it } from 'vitest';
import {
  OSS_DELETED_MESSAGE,
  ossDeleteConfirmMessage,
  ossDeleteTargets,
  ossFilePresentation,
  ossRowMutation
} from './presentation';

describe('OSS file presentation', () => {
  it('shows a deleted file as text instead of an image, even when preview is enabled', () => {
    const row = { deleteState: 'PENDING', fileSuffix: '.PNG' };
    expect(ossFilePresentation(row, true)).toBe('deleted');
    expect(ossFilePresentation(row, false)).toBe('deleted');
    expect(OSS_DELETED_MESSAGE).toBe('该文件已删除');
  });

  it('previews only active image files while preview is enabled', () => {
    expect(ossFilePresentation({ deleteState: 'ACTIVE', fileSuffix: '.png' }, true)).toBe('image');
    expect(ossFilePresentation({ deleteState: 'ACTIVE', fileSuffix: ['.jpg'] }, true)).toBe('image');
    expect(ossFilePresentation({ deleteState: 'ACTIVE', fileSuffix: '.png' }, false)).toBe('text');
    expect(ossFilePresentation({ deleteState: 'ACTIVE', fileSuffix: '.pdf' }, true)).toBe('text');
    expect(ossFilePresentation({ deleteState: 'ACTIVE' }, true)).toBe('text');
  });

  it('replaces delete with restore only for pending rows', () => {
    expect(ossRowMutation({ deleteState: 'PENDING' })).toBe('restore');
    expect(ossRowMutation({ deleteState: 'ACTIVE' })).toBe('delete');
  });

  it('keeps pending rows out of a bulk delete and says to restore them', () => {
    const rows = [{ ossId: 1, deleteState: 'PENDING' }, { ossId: 2, deleteState: 'ACTIVE' }];
    const { pending, removable } = ossDeleteTargets(rows);
    expect(pending.map(row => row.ossId)).toEqual([1]);
    expect(removable.map(row => row.ossId)).toEqual([2]);
    expect(ossDeleteConfirmMessage([2], pending.length)).toBe(
      '是否确认删除OSS对象存储编号为"2"的数据项?待删除文件请使用行内恢复，本次不会再次删除。'
    );
    expect(ossDeleteConfirmMessage([2], 0)).toBe('是否确认删除OSS对象存储编号为"2"的数据项?');
  });
});
