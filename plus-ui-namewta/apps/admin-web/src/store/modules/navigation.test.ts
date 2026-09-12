import type { RouteRecordRaw } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { identityAccessService } from '@/application/services';
import { resolveAdminWebRegistration } from '@/router/adminManifestRegistry';
import { presentDuplicateRouteNameDiagnostics } from '@/router/manifestDiagnostic';
import { useNavigationStore } from './navigation';

vi.mock('@/application/services', () => ({ identityAccessService: { getMenus: vi.fn() } }));
vi.mock('@/router/adminManifestRegistry', () => ({ resolveAdminWebRegistration: vi.fn() }));
vi.mock('@/router/manifestDiagnostic', () => ({
  createManifestRouteDiagnostic: vi.fn(details => ({ name: 'ManifestRouteDiagnostic', details })),
  presentDuplicateRouteNameDiagnostics: vi.fn()
}));
vi.mock('@/components/ParentView/index.vue', () => ({ default: { name: 'ParentView' } }));
vi.mock('@/layout/components/InnerLink/index.vue', () => ({ default: { name: 'InnerLink' } }));
vi.mock('@/layout/index.vue', () => ({ default: { name: 'Layout' } }));
vi.mock('@/router', () => ({ constantRoutes: [{ path: '/constant', name: 'Constant' }] }));
vi.mock('@/store', () => ({ default: {} }));

describe('admin navigation store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    vi.mocked(resolveAdminWebRegistration).mockImplementation(componentKey =>
      componentKey === 'demo/demo/index'
        ? { componentName: 'DemoList', load: async () => ({ name: 'DemoList' }) }
        : undefined
    );
  });

  it('projects authoritative server menus through selected manifests', async () => {
    const serverRoutes: RouteRecordRaw[] = [
      {
        path: '/demo',
        name: 'Demo',
        component: 'Layout' as never,
        permissions: ['server:metadata-only'],
        children: [
          {
            path: 'nested',
            name: 'DemoParent',
            component: 'ParentView' as never,
            children: [
              {
                path: 'list',
                name: 'DemoList',
                component: 'demo/demo/index' as never
              }
            ]
          }
        ]
      }
    ];
    vi.mocked(identityAccessService.getMenus).mockResolvedValue(serverRoutes as never);

    const navigationStore = useNavigationStore();
    const generated = await navigationStore.generateRoutes();

    expect(resolveAdminWebRegistration).toHaveBeenCalledWith('demo/demo/index', 'demo');
    expect(generated[0]).toMatchObject({
      path: '/demo',
      name: 'Demo',
      permissions: ['server:metadata-only'],
      children: [{ path: 'nested/list', name: 'DemoList' }]
    });
    expect(navigationStore.getRoutes()).toEqual([
      expect.objectContaining({ path: '/constant' }),
      expect.objectContaining({ path: '/demo' })
    ]);
    expect(navigationStore.getSidebarRoutes()).toEqual([
      expect.objectContaining({ path: '/constant' }),
      expect.objectContaining({ path: '/demo' })
    ]);
    expect(navigationStore.getDefaultRoutes()).toEqual([
      expect.objectContaining({ path: '/constant' }),
      expect.objectContaining({ path: '/demo' })
    ]);
    expect(navigationStore.getTopbarRoutes()).toEqual([expect.objectContaining({ path: '/demo' })]);
  });

  it('delegates duplicate route-name presentation to the Admin diagnostic boundary', async () => {
    vi.mocked(identityAccessService.getMenus).mockResolvedValue([
      { path: '/first', name: 'Repeated', component: 'Layout' },
      { path: '/second', name: 'Repeated', component: 'Layout' }
    ] as never);

    await useNavigationStore().generateRoutes();

    expect(presentDuplicateRouteNameDiagnostics).toHaveBeenCalledWith([
      { code: 'duplicate-route-name', routeName: 'Repeated' }
    ]);
  });

  it('uses a diagnostic component when a component key is absent from selected manifests', async () => {
    vi.mocked(identityAccessService.getMenus).mockResolvedValue([
      { path: '/unknown', name: 'Unknown', component: 'unselected/report/index' }
    ] as never);

    const [route] = await useNavigationStore().generateRoutes();

    expect(route?.component).toMatchObject({
      name: 'ManifestRouteDiagnostic',
      details: {
        appId: 'admin-web',
        code: 'missing-component-key',
        componentKey: 'unselected/report/index',
        domainId: 'unselected'
      }
    });
  });
});
