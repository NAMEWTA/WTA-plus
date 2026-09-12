import { describe, expect, it, vi } from 'vitest';
import { requestRelogin } from './index';

describe('requestRelogin', () => {
  it('keeps the singleton locked through confirm, logout and navigation', async () => {
    let confirm = () => undefined;
    let finishLogout = () => undefined;
    let finishNavigation = () => undefined;
    const prompt = new Promise<void>(resolve => (confirm = resolve));
    const logout = new Promise<void>(resolve => (finishLogout = resolve));
    const navigationDone = new Promise<void>(resolve => (finishNavigation = resolve));
    const presenter = { confirmSessionExpired: vi.fn(() => prompt), present: vi.fn() };
    const session = { logout: vi.fn(() => logout) };
    const navigation = {
      currentLocation: () => '/system/user?tab=roles',
      replaceWithLogin: vi.fn(() => navigationDone)
    };
    const state = { show: false };
    requestRelogin({ navigation, presenter, session, state });
    requestRelogin({ navigation, presenter, session, state });
    expect(presenter.confirmSessionExpired).toHaveBeenCalledOnce();
    confirm();
    await vi.waitFor(() => expect(session.logout).toHaveBeenCalledOnce());
    expect(state.show).toBe(true);
    requestRelogin({ navigation, presenter, session, state });
    expect(presenter.confirmSessionExpired).toHaveBeenCalledOnce();
    finishLogout();
    await vi.waitFor(() => expect(navigation.replaceWithLogin).toHaveBeenCalledWith('%2Fsystem%2Fuser%3Ftab%3Droles'));
    expect(state.show).toBe(true);
    requestRelogin({ navigation, presenter, session, state });
    expect(presenter.confirmSessionExpired).toHaveBeenCalledOnce();
    finishNavigation();
    await vi.waitFor(() => expect(state.show).toBe(false));
    expect(session.logout).toHaveBeenCalledOnce();
  });

  it('releases the singleton after synchronous presenter failure', async () => {
    const presenter = {
      confirmSessionExpired: vi.fn(() => {
        throw new Error('presenter unavailable');
      }),
      present: vi.fn()
    };
    const session = { logout: vi.fn() };
    const navigation = { currentLocation: () => '/', replaceWithLogin: vi.fn() };
    const state = { show: false };

    expect(() => requestRelogin({ navigation, presenter, session, state })).not.toThrow();
    await vi.waitFor(() => expect(state.show).toBe(false));
    expect(session.logout).not.toHaveBeenCalled();
  });
});
