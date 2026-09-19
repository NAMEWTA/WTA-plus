import { createHash, randomBytes } from 'node:crypto';
import { describe, expect, it, vi } from 'vitest';
import { buildSsoCallbackUri, createSsoAuth, readSsoTokenResponse, safeSsoReturnTo } from './index';

function fixture() {
  let stored: unknown = null;
  let assigned = '';
  const exchange = vi.fn(async () => ({ accessToken: 'owned-token', clientId: 'business-client' }));
  const sso = createSsoAuth({
    exchangeToken: exchange,
    navigate: url => { assigned = url; },
    randomBytes: size => new Uint8Array(randomBytes(size)),
    sha256: bytes => new Uint8Array(createHash('sha256').update(bytes).digest()),
    storage: {
      clear: () => { stored = null; },
      load: () => stored,
      save: value => { stored = value; }
    }
  });
  const input = { authorizeUrl: 'https://sso.example/authorize', clientId: 'business-client', redirectUri: 'https://app.example/admin/sso/callback', returnTo: '/system/user?tab=roles#selected' };
  return {
    sso, input, exchange,
    stored: () => stored,
    callback: () => `?code=owned-code&state=${new URL(assigned).searchParams.get('state')}`,
    assigned: () => assigned,
    setStored: (value: unknown) => { stored = value; }
  };
}

describe('SSO one-time callback and return path', () => {
  it('builds callbacks under the normalized current App context and keeps query/hash return paths', () => {
    expect(buildSsoCallbackUri('https://app.example', '/admin/')).toBe('https://app.example/admin/sso/callback');
    expect(buildSsoCallbackUri('https://app.example', 'home')).toBe('https://app.example/home/sso/callback');
    expect(buildSsoCallbackUri('https://app.example', '/')).toBe('https://app.example/sso/callback');
    expect(() => buildSsoCallbackUri('https://app.example', '../home')).toThrow();
    expect(safeSsoReturnTo('/system/user?tab=roles#selected')).toBe('/system/user?tab=roles#selected');
    expect(safeSsoReturnTo('/system/../profile?tab=mine')).toBe('/profile?tab=mine');
  });

  it('classifies expiry and never exposes raw backend failures or malformed tokens', () => {
    expect(() => readSsoTokenResponse({ code: 500, msg: '授权码已过期' })).toThrow('登录授权已过期，请重新授权。');
    expect(() => readSsoTokenResponse({ code: 500, msg: 'canary-token' })).toThrow('未能完成登录，请重新授权。');
    expect(() => readSsoTokenResponse({ code: 200, data: { access_token: 'owned-token' } })).toThrow();
    expect(() => readSsoTokenResponse(null)).toThrow();
  });

  it('preserves the App return path and consumes pending state before waiting for exchange', async () => {
    const f = fixture();
    await f.sso.startSsoLogin(f.input);
    let finish: (value: { accessToken: string; clientId: string }) => void = () => undefined;
    f.exchange.mockImplementation(() => new Promise(resolve => { finish = resolve; }));
    const first = f.sso.handleCallback(f.callback());
    const pendingDuringExchange = f.stored();
    finish({ accessToken: 'owned-token', clientId: 'business-client' });
    expect(await first).toMatchObject({ returnTo: f.input.returnTo });
    expect(pendingDuringExchange).toBeNull();
    await expect(f.sso.handleCallback(f.callback())).rejects.toThrow();
    expect(f.exchange).toHaveBeenCalledOnce();
  });

  it('clears failed exchange credentials and uses fresh state and verifier on explicit restart', async () => {
    const f = fixture();
    await f.sso.startSsoLogin(f.input);
    const original = f.stored();
    const originalUrl = f.assigned();
    f.exchange.mockRejectedValueOnce(new Error('canary-code-verifier-token'));
    await expect(f.sso.handleCallback(f.callback())).rejects.not.toThrow('canary-code-verifier-token');
    expect(f.stored()).toBeNull();
    await f.sso.startSsoLogin(f.input);
    expect(f.stored()).not.toEqual(original);
    expect(new URL(f.assigned()).searchParams.get('state')).not.toBe(new URL(originalUrl).searchParams.get('state'));
    expect(new URL(f.assigned()).searchParams.get('code_challenge')).not.toBe(new URL(originalUrl).searchParams.get('code_challenge'));
  });

  it.each(['https://outside.example/', '//outside.example/', '/\\outside.example/', '/%2f%2foutside.example/', '/\n/elsewhere'])('rejects unsafe returnTo %j before navigating', async returnTo => {
    const f = fixture();
    await expect(f.sso.startSsoLogin({ ...f.input, returnTo })).rejects.toThrow();
    expect(f.assigned()).toBe('');
    expect(f.stored()).toBeNull();
  });

  it.each(['&code=second', '&state=second', '&error=access_denied'])('rejects conflicting callback fields %s without exchanging', async suffix => {
    const f = fixture();
    await f.sso.startSsoLogin(f.input);
    await expect(f.sso.handleCallback(f.callback() + suffix)).rejects.toThrow();
    expect(f.exchange).not.toHaveBeenCalled();
    expect(f.stored()).toBeNull();
  });

  it('rejects a token for another Client and malformed browser state', async () => {
    const f = fixture();
    await f.sso.startSsoLogin(f.input);
    f.exchange.mockResolvedValueOnce({ accessToken: 'owned-token', clientId: 'other-client' });
    await expect(f.sso.handleCallback(f.callback())).rejects.toThrow();
    expect(f.stored()).toBeNull();
    f.setStored({ state: 'forged', verifier: 42, returnTo: '//outside.example/' });
    await expect(f.sso.handleCallback('?code=owned-code&state=forged')).rejects.toThrow();
    expect(f.exchange).toHaveBeenCalledOnce();
  });
});
