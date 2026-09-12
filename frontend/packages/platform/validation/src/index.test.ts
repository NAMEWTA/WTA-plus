import { describe, expect, it } from 'vitest';
import { isValidFormat, validateFormat } from './index';

describe('validation formats', () => {
  it('validates contact formats', () => {
    expect(isValidFormat('user@example.com', 'EMAIL')).toBe(true);
    expect(isValidFormat('13800138000', 'MAINLAND_MOBILE')).toBe(true);
    expect(isValidFormat('123', 'MAINLAND_MOBILE')).toBe(false);
  });

  it('validates mainland identity checksum and dates', () => {
    expect(isValidFormat('11010519491231002X', 'MAINLAND_ID_CARD')).toBe(true);
    expect(validateFormat('110105194912310020', 'MAINLAND_ID_CARD').issue?.code).toBe('validation.idCard.mainland.checksum');
    expect(validateFormat('11010519990230002X', 'MAINLAND_ID_CARD').issue?.code).toBe('validation.idCard.mainland.date');
  });

  it('validates unified social credit code checksum', () => {
    expect(isValidFormat('91110000123456789X', 'UNIFIED_SOCIAL_CREDIT_CODE')).toBe(false);
    expect(validateFormat('91110000123456789X', 'UNIFIED_SOCIAL_CREDIT_CODE').issue?.code).toBe('validation.creditCode.checksum');
  });
});
