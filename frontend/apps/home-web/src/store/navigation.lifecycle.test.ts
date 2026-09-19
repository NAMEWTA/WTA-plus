import { createPinia, setActivePinia } from 'pinia';
import { createMemoryHistory, createRouter } from 'vue-router';
import { describe, expect, it, vi } from 'vitest';
import { identityAccessService } from '@/application/services';
import { useNavigationStore } from './navigation';

vi.mock('@/application/services', () => ({ identityAccessService: { getMenus: vi.fn() } }));
vi.mock('@/router/homeManifestRegistry', () => ({ resolveHomeWebRegistration: () => ({ componentName: 'Owned', load: async () => ({}) }) }));
vi.mock('@/router/manifestDiagnostic', () => ({ createHomeManifestDiagnostic: () => ({}) }));

describe('Home navigation ownership', () => {
  it('reclaims the session routes while preserving the static Home shell', async () => {
    setActivePinia(createPinia());
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', name: 'Home', component: {} }] });
    vi.mocked(identityAccessService.getMenus).mockResolvedValue([{ path: '/owned', name: 'Owned', component: 'profile/center/index' }]);
    const navigation = useNavigationStore();
    for (const route of await navigation.generateRoutes()) navigation.registerRoute(route, value => router.addRoute('Home', value));
    navigation.finishRecovery();
    expect(router.hasRoute('Owned')).toBe(true);
    expect(navigation.navigationLoaded).toBe(true);
    navigation.resetRoutes(); navigation.resetRoutes();
    expect(router.hasRoute('Owned')).toBe(false);
    expect(router.hasRoute('Home')).toBe(true);
    expect(navigation.routes).toEqual([]);
    let finish!: (value: never[]) => void;
    vi.mocked(identityAccessService.getMenus).mockReturnValue(new Promise(resolve => { finish = resolve; }));
    const pending = navigation.generateRoutes().catch(error => error);
    navigation.resetRoutes(); finish([]);
    expect(await pending).toBeInstanceOf(Error);
    expect(navigation.navigationLoaded).toBe(false);
  });
});
