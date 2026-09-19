import { describe, expect, it } from 'vitest';
import { accountFromPreset } from './account-presets';

describe('账号预设草稿隔离', () => {
  it('切换供应商和重新打开不会携带前一草稿的凭据或启用状态', () => {
    const first = accountFromPreset('MAIL', 'mail-qq');
    first.mailPass = 'owned-draft-secret';
    first.mailUser = 'owned@example.test';
    first.enabled = 'Y';
    for (const next of [accountFromPreset('MAIL', 'mail-163'), accountFromPreset('MAIL', 'mail-qq'), accountFromPreset('SMS', 'sms-tencent')]) {
      expect(next.enabled).toBe('N');
      expect(next.mailPass).toBe('');
      expect(next.accessKeySecret).toBe('');
      expect(next.mailUser).toBeUndefined();
    }
    expect(first.mailPass).toBe('owned-draft-secret');
  });

  it('跨渠道或未知预设保持空白草稿，不隐式改选其他供应商', () => {
    for (const key of ['sms-tencent', 'unknown']) {
      const account = accountFromPreset('MAIL', key);
      expect(account.configKey).toBe('');
      expect(account.host).toBeUndefined();
      expect(account.supplier).toBeUndefined();
    }
  });
});
