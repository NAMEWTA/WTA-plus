import { describe, expect, it } from 'vitest';
import type { HttpClient } from '@namewta/platform-contracts';
import { createEnterpriseTransferService } from './service';

const service = (data: unknown) => createEnterpriseTransferService({ request: async <T>() => ({ code: 200, data }) as T } satisfies HttpClient);

describe('enterprise transfer transport mapping', () => {
  it.each(['QUEUED', 'TRANSFERRED', 'NOT_AVAILABLE', 'EXPIRED', 'FAILED', 'UNBOUND'])('preserves the %s business state', async status => {
    const result = await service({ status, challengeId: 'owned', expiresInSeconds: 18 }).confirm({ challengeId: 'owned', code: '123456' });
    expect(result.data).toEqual({ status, challengeId: 'owned', expiresInSeconds: 18 });
  });
  it.each([null, {}, { status: 'SENT' }, { status: 'ACCEPTED' }])('rejects an invalid or obsolete transfer response', async data => {
    await expect(service(data).send({ fullName: 'Owned', documentLastFour: '3001', phone: '13800138000' })).rejects.toThrow('无法识别的状态');
  });
});
