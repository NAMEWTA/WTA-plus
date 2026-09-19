import type { RouteRecordRaw } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
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

  it('removes only retired AI menus before every projection without mutating the server response', async () => {
    const menus = [
      {
        path: '/ai',
        name: 'Ai',
        component: 'Layout',
        children: [{ path: 'chat', name: 'OldChat', component: 'ai/chat/index' }]
      },
      {
        path: '/monitor',
        name: 'Monitor',
        component: 'Layout',
        children: [
          { path: 'snailai', name: 'OldConsole', component: 'monitor/snailai/index' },
          { path: 'report', name: 'UnknownReport', component: 'monitor/report/index' },
          { path: 'kept', name: 'KeptPage', component: 'demo/demo/index' }
        ]
      },
      { path: '/future', name: 'UnknownAi', component: 'ai/future/index' }
    ];
    const original = structuredClone(menus);
    vi.mocked(identityAccessService.getMenus).mockResolvedValue(menus);
    const navigation = useNavigationStore();
    const routes = await navigation.generateRoutes();
    expect(routes.map(route => route.name)).toEqual(['Monitor', 'UnknownAi']);
    expect(routes[0]?.children?.map(route => route.name)).toEqual(['UnknownReport', 'KeptPage']);
    expect(routes[0]?.children?.[0]?.component).toMatchObject({ name: 'ManifestRouteDiagnostic' });
    expect(routes[1]?.component).toMatchObject({ name: 'ManifestRouteDiagnostic' });
    for (const projection of [
      navigation.getRoutes(),
      navigation.getSidebarRoutes(),
      navigation.getDefaultRoutes(),
      navigation.getTopbarRoutes()
    ]) {
      expect(JSON.stringify(projection)).not.toMatch(/OldChat|OldConsole/);
    }
    expect(menus).toEqual(original);
    expect(resolveAdminWebRegistration).not.toHaveBeenCalledWith('ai/chat/index', expect.anything());
    expect(resolveAdminWebRegistration).not.toHaveBeenCalledWith('monitor/snailai/index', expect.anything());
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

  it('keeps an unknown page diagnostic when all of its children are retired', async () => {
    vi.mocked(identityAccessService.getMenus).mockResolvedValue([
      {
        path: '/unknown',
        name: 'Unknown',
        component: 'unselected/report/index',
        children: [{ path: 'old-ai', component: 'ai/chat/index' }]
      }
    ]);
    const [route] = await useNavigationStore().generateRoutes();
    expect(route?.component).toMatchObject({ name: 'ManifestRouteDiagnostic' });
    expect(route?.children ?? []).toEqual([]);
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

  it('removes only its installed routes and invalidates a late menu response', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/constant', name: 'Constant', component: {} }]
    });
    vi.mocked(identityAccessService.getMenus).mockResolvedValue([
      { path: '/owned', name: 'Owned', component: 'demo/demo/index' }
    ]);
    const navigation = useNavigationStore();
    for (const route of await navigation.generateRoutes())
      navigation.registerRoute(route, value => router.addRoute(value));
    navigation.finishRecovery();
    expect(router.hasRoute('Owned')).toBe(true);
    expect(navigation.navigationLoaded).toBe(true);
    navigation.resetRoutes();
    navigation.resetRoutes();
    expect(router.hasRoute('Owned')).toBe(false);
    expect(router.hasRoute('Constant')).toBe(true);
    expect(navigation.navigationLoaded).toBe(false);
    let finish!: (value: never[]) => void;
    vi.mocked(identityAccessService.getMenus).mockReturnValue(
      new Promise(resolve => {
        finish = resolve;
      })
    );
    const pending = navigation.generateRoutes().catch(error => error);
    navigation.resetRoutes();
    finish([]);
    expect(await pending).toBeInstanceOf(Error);
    expect(navigation.routes).toEqual([]);
  });
});
