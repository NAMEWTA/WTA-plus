import type { HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, it } from 'vitest';
import { createMenuQueryPort } from './public';

describe('system public menu seam', () => {
  it('preserves the explicit Client scope and propagates failures', async () => {
    const requests: HttpRequest[] = [];
    const failure = new Error('menu unavailable');
    const port = createMenuQueryPort({
      request: async request => {
        requests.push(request);
        if ((request.params as { clientId?: string } | undefined)?.clientId === 'failed-client') throw failure;
        return {
          data: [
            {
              id: 1,
              label: '系统管理',
              component: 'must-not-cross',
              secret: 'must-not-cross',
              children: [{ id: 2, label: '字典管理', path: '/system/dict', permissions: ['system:dict:list'] }]
            }
          ]
        } as never;
      }
    });

    await expect(port.options('client/admin')).resolves.toEqual([
      { id: 1, label: '系统管理', children: [{ id: 2, label: '字典管理' }] }
    ]);
    await expect(port.options('failed-client')).rejects.toBe(failure);
    expect(requests).toEqual([
      { url: '/system/menu/treeselect', method: 'get', params: { clientId: 'client/admin' } },
      { url: '/system/menu/treeselect', method: 'get', params: { clientId: 'failed-client' } }
    ]);
  });
});
