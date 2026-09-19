import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, it } from 'vitest';
import { createDemoService, demoDomainModule, type DemoForm, type TreeForm } from './index';

describe('demo domain', () => {
  it('publishes traceable headless domain metadata', () => {
    expect(demoDomainModule).toEqual({
      id: 'demo',
      backendModules: ['wta-demo'],
      capabilities: ['demo-table', 'demo-tree', 'demo-rich-text']
    });
  });

  it('preserves table and tree transport paths, methods, params, and data', async () => {
    const requests: HttpRequest[] = [];
    const client: HttpClient = {
      async request<T>(request: HttpRequest): Promise<T> {
        requests.push(request);
        return { code: 200, data: null } as T;
      }
    };
    const service = createDemoService(client);
    const demoForm: DemoForm = { id: 7, testKey: 'key' };
    const treeForm: TreeForm = { id: 8, parentId: 0, treeName: 'root' };

    await service.listDemo({ pageNum: 2, pageSize: 20, testKey: 'key' });
    await service.getDemo('demo/7');
    await service.addDemo(demoForm);
    await service.updateDemo(demoForm);
    await service.deleteDemo(['demo/7', 'demo 8', 'demo,9']);
    await service.listTree({ parentId: 0 });
    await service.getTree('tree/8');
    await service.addTree(treeForm);
    await service.updateTree(treeForm);
    await service.deleteTree(['tree/8', 'tree 9']);

    expect(requests).toEqual([
      { url: '/demo/demo/list', method: 'get', params: { pageNum: 2, pageSize: 20, testKey: 'key' } },
      { url: '/demo/demo/demo%2F7', method: 'get' },
      { url: '/demo/demo', method: 'post', data: demoForm },
      { url: '/demo/demo/update', method: 'post', data: demoForm },
      { url: '/demo/demo/demo%2F7,demo%208,demo%2C9', method: 'post' },
      { url: '/demo/tree/list', method: 'get', params: { parentId: 0 } },
      { url: '/demo/tree/tree%2F8', method: 'get' },
      { url: '/demo/tree', method: 'post', data: treeForm },
      { url: '/demo/tree/update', method: 'post', data: treeForm },
      { url: '/demo/tree/tree%2F8,tree%209', method: 'post' }
    ]);
  });

  it('projects generated demo responses before returning domain models', async () => {
    const service = createDemoService({
      request: async request =>
        (request.url.endsWith('/list')
          ? { data: { rows: [{ id: 7, testKey: 'key' }], total: 1 } }
          : { data: { id: 8, value: 'detail' } }) as never
    });

    await expect(service.listDemo({ pageNum: 1, pageSize: 10 })).resolves.toMatchObject({
      data: { rows: [{ id: 7, testKey: 'key', deptId: '', userId: '', orderNum: 0, value: '' }], total: 1 }
    });
    await expect(service.getDemo(8)).resolves.toMatchObject({
      data: { id: 8, value: 'detail', deptId: '', userId: '', orderNum: 0, testKey: '' }
    });
  });
});
