import { describe, expect, it, vi } from 'vitest';
import { createDemoWebDomain, type DemoWebRuntime } from './index';
import type { RichTextService } from '@namewta/domain-demo';

const runtime = (): DemoWebRuntime => ({
  service: {
    richText: { list: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn(), assets: { upload: vi.fn(), resolve: vi.fn() } } as RichTextService,
    addDemo: vi.fn(),
    addTree: vi.fn(),
    deleteDemo: vi.fn(),
    deleteTree: vi.fn(),
    getDemo: vi.fn(),
    getTree: vi.fn(),
    listDemo: vi.fn(),
    listTree: vi.fn(),
    updateDemo: vi.fn(),
    updateTree: vi.fn()
  },
  confirm: vi.fn(),
  download: vi.fn(),
  success: vi.fn()
});

describe('demo web domain public manifest', () => {
  it('publishes stable backend keys and keep-alive component names', () => {
    const manifest = createDemoWebDomain(runtime());

    expect(manifest).toMatchObject({ id: 'web-domain-demo', domainId: 'demo' });
    expect(manifest.messages).toEqual([
      { namespace: 'demo', messages: { tableTitle: '测试单列表', treeTitle: '测试树列表' } }
    ]);
    expect(manifest.permissions).toEqual([
      {
        id: 'demo-table',
        permissions: ['demo:demo:list', 'demo:demo:add', 'demo:demo:edit', 'demo:demo:remove', 'demo:demo:export']
      },
      { id: 'demo-tree', permissions: ['demo:tree:list', 'demo:tree:add', 'demo:tree:edit', 'demo:tree:remove'] },
      { id: 'demo-rich-text', permissions: ['demo:richtext:list', 'demo:richtext:query', 'demo:richtext:add', 'demo:richtext:edit', 'demo:richtext:remove', 'common:richtext:upload'] }
    ]);
    expect(manifest.registrations).toMatchObject([
      { id: 'demo-table', componentKey: 'demo/demo/index', componentName: 'Demo' },
      { id: 'demo-tree', componentKey: 'demo/tree/index', componentName: 'Tree' },
      { id: 'demo-rich-text', componentKey: 'demo/rich-text/index', componentName: 'RichTextDemo' }
    ]);
    expect(Object.isFrozen(manifest.registrations)).toBe(true);
    expect(manifest.registrations.every(Object.isFrozen)).toBe(true);
    expect(Object.isFrozen(manifest.messages[0].messages)).toBe(true);
    expect(Object.isFrozen(manifest.permissions[0].permissions)).toBe(true);
  });

  it('fails closed when composition omits the injected runtime', () => {
    expect(() => createDemoWebDomain(undefined as unknown as DemoWebRuntime)).toThrow('DemoWebRuntime is required');
  });
});
