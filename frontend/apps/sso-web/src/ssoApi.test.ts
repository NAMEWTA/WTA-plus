import { afterEach, describe, expect, it, vi } from 'vitest';
import { loginWithPassword, parseAuthorizeQuery, requestAuthorize, ssoLoginMethods } from './ssoApi';

afterEach(() => vi.unstubAllGlobals());

describe('sso-web login surface', () => {
  it('only accepts warehouse password login', () => {
    expect(ssoLoginMethods).toEqual(['password']);
    expect(ssoLoginMethods).not.toContain('github');
    expect(ssoLoginMethods).not.toContain('wechat');
  });

  it('posts warehouse password login with cookie credentials', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toContain('/sso/login');
      expect(init?.method).toBe('POST');
      expect(init?.credentials).toBe('include');
      expect(init?.body).toBe(JSON.stringify({ username: 'WTA', password: 'admin123' }));
      return new Response(JSON.stringify({ code: 200 }));
    });
    vi.stubGlobal('fetch', fetchMock);
    await loginWithPassword('WTA', 'admin123');
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it('preserves opaque state once through URLSearchParams and rejects duplicate input', async () => {
    const state = '  +&%中文/尾部  ';
    const params = new URLSearchParams({ client_id: 'admin', redirect_uri: 'https://app.example/admin/sso/callback?tenant=x%26y',
      state, code_challenge: 'owned-challenge', code_challenge_method: 'S256' });
    const query = parseAuthorizeQuery(params.toString());
    expect(query.state).toBe(state);
    vi.stubGlobal('fetch', vi.fn(async (input: string) => {
      expect(new URL(input, 'https://sso.example').searchParams.get('state')).toBe(state);
      return new Response(JSON.stringify({ code: 200, data: { loginRequired: true } }));
    }));
    await expect(requestAuthorize(query)).resolves.toEqual({ loginRequired: true, redirectUri: undefined });
    expect(() => parseAuthorizeQuery(`${params}&state=second`)).toThrow();
  });

  it('never reflects backend credentials in user-facing failures', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({ code: 500, msg: 'canary-code-verifier-token' }))));
    await expect(loginWithPassword('owned-user', 'owned-password')).rejects.toThrow('登录失败，请检查用户名和密码后重试。');
  });

  it('parses authorize query without holding a secret', () => {
    const query = parseAuthorizeQuery(
      '?response_type=code&client_id=admin&redirect_uri=http://127.0.0.1:4174/sso/callback&state=abc&code_challenge=xyz&code_challenge_method=S256'
    );
    expect(query.clientId).toBe('admin');
    expect(query).not.toHaveProperty('sso_secret');
  });
});
