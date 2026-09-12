import type { HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, it } from 'vitest';
import { createDictQueryPort, createDictTypeCatalogPort } from './public';

describe('system public dict seam', () => {
  it('projects only stable label/value fields and encodes the dictionary type', async () => {
    const requests: HttpRequest[] = [];
    const port = createDictQueryPort({
      request: async request => {
        requests.push(request);
        return {
          data: [{ dictLabel: '启用', dictValue: '0', cssClass: 'secret-css', internalId: 'private' }]
        } as never;
      }
    });

    await expect(port.get('sys/status')).resolves.toEqual([{ label: '启用', value: '0' }]);
    expect(requests).toEqual([{ url: '/system/dict/data/type/sys%2Fstatus', method: 'get' }]);
  });
});

describe('system public dict type catalog seam', () => {
  it('projects stable catalog fields and forwards only the optional Client context', async () => {
    const requests: HttpRequest[] = [];
    const port = createDictTypeCatalogPort({
      request: async request => {
        requests.push(request);
        return { data: [{ dictName: '状态', dictType: 'sys_status', internalId: 9 }] } as never;
      }
    });

    await expect(port.list('client/admin')).resolves.toEqual([{ name: '状态', type: 'sys_status' }]);
    expect(requests).toEqual([
      { url: '/system/dict/type/optionselect', method: 'get', params: { clientId: 'client/admin' } }
    ]);
  });
});
