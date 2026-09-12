import { describe, expect, it } from 'vitest';
import { projectPersonMatchResponse, projectStatusProbeResponse } from './transport';

describe('profile privacy transport', () => {
  it('projects ordinary probes to status only', () => {
    expect(projectStatusProbeResponse({ code: 200, data: { status: 'AVAILABLE' }, msg: 'ok' })).toEqual({
      code: 200,
      data: { status: 'AVAILABLE' },
      msg: 'ok'
    });
  });

  it.each([
    { status: 'BOUND', profileId: '91' },
    { status: 'BOUND', fullName: 'sensitive' },
    { status: 'BOUND', documentNumber: 'sensitive' },
    { status: 'BOUND', phone: '13800000000' }
  ])('rejects identity disclosure in an ordinary probe', data => {
    expect(() => projectStatusProbeResponse({ code: 200, data })).toThrow('Profile 响应不可用');
  });

  it('allows only the masked system phone after a complete personal identity match', () => {
    expect(projectPersonMatchResponse({ data: { status: 'MATCHED', maskedPhone: '138****0000' } }).data).toEqual({
      status: 'MATCHED',
      maskedPhone: '138****0000'
    });
    expect(() =>
      projectPersonMatchResponse({ data: { status: 'MATCHED', maskedPhone: '138****0000', userId: '42' } })
    ).toThrow('Profile 响应不可用');
  });
});
