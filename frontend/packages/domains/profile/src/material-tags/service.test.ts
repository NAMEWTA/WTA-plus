import type { HttpRequest } from '@namewta/platform-contracts';
import { expect, it, vi } from 'vitest';
import { createMaterialTagService } from './service';

it('requests server requirements with explicit profile, document and handler selectors', async () => {
  const data = [{ materialTagCode: 'ENTERPRISE_AUTHORIZATION_LETTER', minimumCount: 1 }];
  const request = vi.fn(async (_config: HttpRequest) => ({ data }));
  const service = createMaterialTagService({ request: <T>(config: HttpRequest) => request(config) as Promise<T> });
  await expect(service.requirements('ENTERPRISE', 'CN_PASSPORT', false)).resolves.toEqual({ data });
  expect(request).toHaveBeenCalledWith({ url: '/profile/material-tags/requirements', method: 'get',
    params: { profileType: 'ENTERPRISE', documentTypeCode: 'CN_PASSPORT', handlerIsLegalRepresentative: false } });
});
