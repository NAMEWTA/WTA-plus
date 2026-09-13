import { describe, expect, it, vi } from 'vitest';
import { loginWithPassword, parseAuthorizeQuery, ssoLoginMethods } from './ssoApi';

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
      return {
        json: async () => ({ code: 200 })
      } as Response;
    });
    vi.stubGlobal('fetch', fetchMock);
    await loginWithPassword('WTA', 'admin123');
    expect(fetchMock).toHaveBeenCalledOnce();
    vi.unstubAllGlobals();
  });

  it('parses authorize query without holding a secret', () => {
    const query = parseAuthorizeQuery(
      '?response_type=code&client_id=admin&redirect_uri=http://127.0.0.1:4174/sso/callback&state=abc&code_challenge=xyz&code_challenge_method=S256'
    );
    expect(query.clientId).toBe('admin');
    expect(query).not.toHaveProperty('sso_secret');
  });
});
