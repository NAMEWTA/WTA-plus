import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const fixture = vi.hoisted(() => ({
  token: 'owned-session', getInfo: vi.fn(), logout: vi.fn(), login: vi.fn(),
  closePush: vi.fn(), cancelPending: vi.fn(), clearNotice: vi.fn(), resetRoutes: vi.fn(), resetSession: vi.fn()
}));
vi.mock('@/application/services', () => ({ identityAccessService: fixture }));
vi.mock('@/application/session', () => ({
  getToken: () => fixture.token,
  removeToken: () => { fixture.token = ''; }
}));
vi.mock('@/application/http', () => ({ adminHttp: { request: vi.fn().mockResolvedValue({}), cancelPending: fixture.cancelPending }, isRelogin: { show: false } }));
vi.mock('@/utils/push', () => ({ closePush: fixture.closePush }));
vi.mock('@/store/modules/navigation', () => ({ useNavigationStore: () => ({ resetRoutes: fixture.resetRoutes }) }));
vi.mock('@/store/modules/notice', () => ({ useNoticeStore: () => ({ clearNotice: fixture.clearNotice }) }));
vi.mock('@/store/modules/tagsView', () => ({ useTagsViewStore: () => ({ resetSession: fixture.resetSession }) }));
import { useUserStore } from './user';

const info = { user: { userId: '7', userName: 'owned-user', nickName: 'Owned user', avatarUrl: '' }, roles: [], permissions: ['owned:read'] };

describe('Admin session lifecycle', () => {
  beforeEach(() => {
    setActivePinia(createPinia()); vi.clearAllMocks(); fixture.token = 'owned-session';
    fixture.getInfo.mockResolvedValue(info); fixture.logout.mockResolvedValue(undefined);
  });

  it('represents a loaded identity with empty roles without inventing a role', async () => {
    const user = useUserStore(); await user.getInfo();
    expect(user.roles).toEqual([]);
    expect(user).toHaveProperty('identityLoaded', true);
  });

  it('clears every local identity and owned resource even when remote logout rejects', async () => {
    const user = useUserStore(); await user.getInfo();
    fixture.logout.mockRejectedValue(new Error('owned offline'));
    await user.logout().catch(() => undefined);
    expect(fixture.token).toBe(''); expect(user.token).toBe('');
    expect(user.roles).toEqual([]); expect(user.permissions).toEqual([]);
    expect(user.nickname).toBe(''); expect(user.userId).toBe(''); expect(user.avatar).toBe('');
    expect(user).toHaveProperty('identityLoaded', false);
    expect(fixture.resetRoutes).toHaveBeenCalled(); expect(fixture.cancelPending).toHaveBeenCalled();
    expect(fixture.closePush).toHaveBeenCalled(); expect(fixture.clearNotice).toHaveBeenCalled();
    expect(fixture.resetSession).toHaveBeenCalled();
  });

  it('ignores an identity response that completes after logout', async () => {
    let complete!: (value: typeof info) => void;
    fixture.getInfo.mockReturnValue(new Promise(resolve => { complete = resolve; }));
    const user = useUserStore(); const pending = user.getInfo().catch(() => undefined);
    await user.logout(); complete(info); await pending;
    expect(user.permissions).toEqual([]); expect(user.nickname).toBe('');
    expect(user).toHaveProperty('identityLoaded', false);
  });

  it('coalesces repeated logout while the remote attempt is pending', async () => {
    let finish!: () => void;
    fixture.logout.mockReturnValue(new Promise<void>(resolve => { finish = resolve; }));
    const user = useUserStore(); const first = user.logout(); const second = user.logout();
    const calls = fixture.logout.mock.calls.length;
    finish(); await first; await second;
    expect(calls).toBe(1);
  });
});
