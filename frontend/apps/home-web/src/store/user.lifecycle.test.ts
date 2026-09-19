import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const fixture = vi.hoisted(() => ({
  token: 'owned-session', getInfo: vi.fn(), logout: vi.fn(), login: vi.fn(), cancelPending: vi.fn(), resetRoutes: vi.fn()
}));
vi.mock('@/application/services', () => ({ identityAccessService: fixture }));
vi.mock('@/application/session', () => ({
  getToken: () => fixture.token, removeToken: () => { fixture.token = ''; }, session: {}
}));
vi.mock('@/application/http', () => ({ homeHttp: { cancelPending: fixture.cancelPending }, relogin: { show: false } }));
vi.mock('@/store/navigation', () => ({ useNavigationStore: () => ({ resetRoutes: fixture.resetRoutes }) }));
import { useUserStore } from './user';

const info = { user: { userId: '7', userName: 'owned-user', nickName: 'Owned user' }, roles: [], permissions: ['owned:read'] };

describe('Home session lifecycle', () => {
  beforeEach(() => {
    setActivePinia(createPinia()); vi.clearAllMocks(); fixture.token = 'owned-session';
    fixture.getInfo.mockResolvedValue(info); fixture.logout.mockResolvedValue(undefined);
  });

  it('marks an empty-role identity as loaded', async () => {
    const user = useUserStore(); await user.getInfo();
    expect(user.roles).toEqual([]); expect(user).toHaveProperty('identityLoaded', true);
  });

  it('keeps the existing finally token cleanup and clears remaining identity/routes on failure', async () => {
    const user = useUserStore(); await user.getInfo();
    fixture.logout.mockRejectedValue(new Error('owned offline'));
    await user.logout().catch(() => undefined);
    expect(fixture.token).toBe(''); expect(user.token).toBe('');
    expect(user.permissions).toEqual([]); expect(user.roles).toEqual([]);
    expect(user.nickname).toBe(''); expect(user.userId).toBe('');
    expect(user).toHaveProperty('identityLoaded', false);
    expect(fixture.resetRoutes).toHaveBeenCalled(); expect(fixture.cancelPending).toHaveBeenCalled();
  });

  it('does not restore old identity after logout completed', async () => {
    let complete!: (value: typeof info) => void;
    fixture.getInfo.mockReturnValue(new Promise(resolve => { complete = resolve; }));
    const user = useUserStore(); const pending = user.getInfo().catch(() => undefined);
    await user.logout(); complete(info); await pending;
    expect(user.permissions).toEqual([]); expect(user.nickname).toBe('');
    expect(user).toHaveProperty('identityLoaded', false);
  });
});


describe('Home user transport boundary', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks(); fixture.token = 'owned-session'; });
  it.each([null, {}, { userId: null }, { userId: '7', nickName: {} }])('does not load malformed identity %j', async user => {
    fixture.getInfo.mockResolvedValue({ ...info, user });
    const store = useUserStore();
    await expect(store.getInfo()).rejects.toThrow('用户响应不可用');
    expect(store.identityLoaded).toBe(false); expect(store.permissions).toEqual([]);
    expect(store.userId).toBe(''); expect(store.nickname).toBe('');
  });
  it('preserves nullable names without assigning undefined to the store', async () => {
    fixture.getInfo.mockResolvedValue({ ...info, user: { userId: '7', nickName: null, userName: null } });
    const store = useUserStore(); await store.getInfo();
    expect(store.nickname).toBe(''); expect(store.userId).toBe('7'); expect(store.identityLoaded).toBe(true);
  });
});
