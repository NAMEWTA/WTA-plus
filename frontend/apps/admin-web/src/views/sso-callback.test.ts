import { createRenderer, nextTick, ssrContextKey } from 'vue';
import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import Callback from './sso-callback.vue';
import { useUserStore } from '@/store/modules/user';

const fixture = vi.hoisted(() => ({
  token: 'old-token', exchange: vi.fn(), replace: vi.fn(), getInfo: vi.fn(), logout: vi.fn(),
  navigationLoaded: true, resetRoutes: vi.fn()
}));
vi.mock('vue-router', () => ({ useRouter: () => ({ replace: fixture.replace }) }));
vi.mock('@/application/sso', () => ({ adminSso: { handleCallback: fixture.exchange }, adminSsoRedirectUri: vi.fn() }));
vi.mock('@/application/services', () => ({ identityAccessService: fixture }));
vi.mock('@/application/session', () => ({
  getToken: () => fixture.token, removeToken: () => { fixture.token = ''; },
  session: { getToken: () => fixture.token, setToken: (token: string) => { fixture.token = token; } }
}));
vi.mock('@/application/http', () => ({ adminHttp: { cancelPending: vi.fn() }, isRelogin: { show: false } }));
vi.mock('@/store/modules/navigation', () => ({ useNavigationStore: () => ({ resetRoutes: fixture.resetRoutes }) }));
vi.mock('@/utils/push', () => ({ closePush: vi.fn() }));
vi.mock('@/store/modules/notice', () => ({ useNoticeStore: () => ({ clearNotice: vi.fn() }) }));
vi.mock('@/store/modules/tagsView', () => ({ useTagsViewStore: () => ({ resetSession: vi.fn() }) }));

type HostNode = { parent?: HostNode; children: HostNode[] };
const node = (): HostNode => ({ children: [] });
const renderer = createRenderer<HostNode, HostNode>({
  createElement: node, createText: node, createComment: node,
  setText: () => {}, setElementText: () => {}, patchProp: () => {},
  insert: (child, parent) => { child.parent = parent; parent.children.push(child); },
  remove: child => { if (child.parent) child.parent.children = child.parent.children.filter(item => item !== child); },
  parentNode: child => child.parent ?? null, nextSibling: () => null
});
let unmount: (() => void) | undefined;
afterEach(() => { unmount?.(); unmount = undefined; vi.unstubAllGlobals(); });
beforeEach(() => {
  setActivePinia(createPinia()); vi.clearAllMocks(); fixture.token = 'old-token'; fixture.navigationLoaded = true;
  fixture.resetRoutes.mockImplementation(() => { fixture.navigationLoaded = false; });
  fixture.getInfo.mockResolvedValue({ user: { userId: '7', userName: 'old-user', nickName: 'Old user', avatarUrl: '' }, roles: ['old-role'], permissions: ['old:read'] });
  fixture.logout.mockResolvedValue(undefined);
  fixture.replace.mockResolvedValue(undefined);
  vi.stubGlobal('window', { location: { search: '?code=once&state=owned', pathname: '/sso/callback', hash: '' }, history: { state: {}, replaceState: vi.fn() } });
});

async function mountPending() {
  const user = useUserStore();
  await user.getInfo();
  let complete!: (result: { accessToken: string; returnTo: string }) => void;
  const pending = new Promise<{ accessToken: string; returnTo: string }>(resolve => { complete = resolve; });
  fixture.exchange.mockReturnValue(pending);
  const app = renderer.createApp({ ...Callback, render: () => null });
  app.provide(ssrContextKey, { modules: new Set<string>() });
  app.mount(node());
  unmount = () => app.unmount();
  return { user, complete: async () => { complete({ accessToken: 'sso-token', returnTo: '/profile' }); await pending; await nextTick(); } };
}

describe('adminSso callback session ownership', () => {
  it('replaces the old identity and navigation before accepting the adminSso token', async () => {
    const { user, complete } = await mountPending();
    await complete();
    expect(fixture.token).toBe('sso-token'); expect(user.token).toBe('sso-token');
    expect(user.identityLoaded).toBe(false); expect(user.permissions).toEqual([]); expect(user.roles).toEqual([]);
    expect(user.sessionGeneration).toBe(1); expect(fixture.navigationLoaded).toBe(false);
    expect(fixture.replace).toHaveBeenCalledWith('/profile');
  });

  it('does not accept a token after the callback page unmounts', async () => {
    const { complete } = await mountPending();
    unmount?.(); unmount = undefined;
    await complete();
    expect(fixture.token).toBe('old-token'); expect(fixture.replace).not.toHaveBeenCalled();
    expect(fixture.resetRoutes).not.toHaveBeenCalled();
  });

  it('does not revive a session after logout while the token exchange is pending', async () => {
    const { user, complete } = await mountPending();
    await user.logout(); await complete();
    expect(fixture.token).toBe(''); expect(user.token).toBe(''); expect(fixture.replace).not.toHaveBeenCalled();
  });

  it('does not overwrite a newer token even if the store generation has not changed', async () => {
    const { user, complete } = await mountPending();
    fixture.token = 'newer-token'; user.token = 'newer-token'; await complete();
    expect(fixture.token).toBe('newer-token'); expect(fixture.replace).not.toHaveBeenCalled();
    expect(fixture.resetRoutes).not.toHaveBeenCalled();
  });
});
