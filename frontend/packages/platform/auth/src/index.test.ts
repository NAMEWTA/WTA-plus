import { createHash, randomBytes } from 'node:crypto';
import { describe, expect, it, vi } from 'vitest';
import { computeS256Challenge, createSsoAuth, requestRelogin } from './index';
import type { SsoPendingAuth } from './index';

describe('requestRelogin', () => {
  it('keeps the singleton locked through confirm, logout and navigation', async () => {
    let confirm: () => void = () => undefined;
    let finishLogout: () => void = () => undefined;
    let finishNavigation: () => void = () => undefined;
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

describe('createSsoAuth', () => {
  const sha256 = (bytes: Uint8Array) => new Uint8Array(createHash('sha256').update(bytes).digest());

  it('computes RFC 7636 S256 challenge', () => {
    expect(computeS256Challenge('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk', sha256)).toBe(
      'E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM'
    );
  });

  it('starts authorize with PKCE and rejects tampered state', async () => {
    let assigned = '';
    let stored: SsoPendingAuth | null = null;
    const sso = createSsoAuth({
      exchangeToken: async input => ({ accessToken: `tok-${input.code}`, clientId: input.clientId }),
      navigate: url => {
        assigned = url;
      },
      randomBytes: size => new Uint8Array(randomBytes(size)),
      sha256,
      storage: {
        clear: () => {
          stored = null;
        },
        load: () => stored,
        save: value => {
          stored = value;
        }
      }
    });
    await sso.startSsoLogin({
      authorizeUrl: 'http://127.0.0.1:4176/authorize',
      clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e',
      redirectUri: 'http://127.0.0.1:4174/sso/callback'
    });
    const started = new URL(assigned);
    expect(started.origin + started.pathname).toBe('http://127.0.0.1:4176/authorize');
    expect(started.searchParams.get('code_challenge_method')).toBe('S256');
    expect(started.searchParams.get('code_challenge')).toBe(computeS256Challenge(stored!.verifier, sha256));
    await expect(sso.handleCallback('?code=abc&state=tampered')).rejects.toMatchObject({ kind: 'state' });
    stored = {
      clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e',
      redirectUri: 'http://127.0.0.1:4174/sso/callback',
      state: 'good',
      returnTo: '/',
      verifier: 'dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk'
    };
    await expect(sso.handleCallback('?code=issued-code&state=good')).resolves.toEqual({
      accessToken: 'tok-issued-code',
      returnTo: '/',
      clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e'
    });
  });
});

describe('failed remote logout recovery', () => {
  it('navigates after local teardown even when the remote attempt fails', async () => {
    const state = { show: false };
    const session = { logout: vi.fn().mockRejectedValue(new Error('owned remote timeout')) };
    const navigation = { currentLocation: () => '/owned?view=1', replaceWithLogin: vi.fn() };
    const presenter = { confirmSessionExpired: vi.fn().mockResolvedValue(undefined), present: vi.fn() };
    requestRelogin({ state, session, navigation, presenter });
    requestRelogin({ state, session, navigation, presenter });
    await vi.waitFor(() => expect(navigation.replaceWithLogin).toHaveBeenCalledWith('%2Fowned%3Fview%3D1'));
    expect(session.logout).toHaveBeenCalledOnce();
    expect(presenter.confirmSessionExpired).toHaveBeenCalledOnce();
    expect(state.show).toBe(false);
  });
});
