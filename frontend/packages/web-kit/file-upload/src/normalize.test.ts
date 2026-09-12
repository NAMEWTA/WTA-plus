import { describe, expect, it } from 'vitest';
import { normalizeUploadValue, serializeUploadItems } from './normalize';

describe('file upload value normalization', () => {
  it('handles null and empty values without attempting identifier conversion', () => {
    expect(normalizeUploadValue(null)).toEqual({ ids: [], items: [] });
    expect(normalizeUploadValue('')).toEqual({ ids: [], items: [] });
  });

  it('accepts numeric identifiers and legacy ossId objects', () => {
    expect(normalizeUploadValue([1001, { ossId: '1002', originalName: 'report.pdf', url: '/report.pdf' }])).toEqual({
      ids: [1001, '1002'],
      items: [{ id: '1002', name: 'report.pdf', url: '/report.pdf' }]
    });
    expect(normalizeUploadValue({ ossId: 1003, originalName: 'legacy.pdf', url: '/legacy.pdf' })).toEqual({
      ids: [1003],
      items: [{ id: 1003, name: 'legacy.pdf', url: '/legacy.pdf' }]
    });
  });

  it('filters malformed items and serializes only usable identifiers', () => {
    expect(normalizeUploadValue([null, {}, { ossId: null }, { id: '1003', name: 'x', url: '' }])).toEqual({
      ids: ['1003'],
      items: [{ id: '1003', name: 'x', url: '' }]
    });
    expect(serializeUploadItems([{ id: 0, name: 'zero', url: '' }, { id: '1004', name: 'x', url: '' }])).toBe('0,1004');
  });
});
