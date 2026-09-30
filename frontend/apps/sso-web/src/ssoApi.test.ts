import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  fetchOidcContext,
  loginWithPassword,
  oidcResumeUrl,
  parseAuthorizeQuery,
  parseOidcRequest,
  requestAuthorize,
  ssoLoginMethods
} from './ssoApi';

afterEach(() => vi.unstubAllGlobals());

describe('sso-web login surface', () => {
  it('OIDC 只接受一个随机事务，不接受跳转地址或重复参数', () => {
    const request = 'a'.repeat(43);
    expect(parseOidcRequest(`?oidc_request=${request}`)).toBe(request);
    expect(parseOidcRequest('?client_id=admin')).toBeUndefined();
    expect(() => parseOidcRequest('?oidc_request=https://evil.example')).toThrow();
    expect(() => parseOidcRequest(`?oidc_request=${request}&oidc_request=${request}`)).toThrow();
    expect(oidcResumeUrl(request)).toContain(`/oidc/authorize?oidc_request=${request}`);
  });

  it('OIDC 登录上下文使用 Cookie 并验证强制重登标志', async () => {
    const fetchMock = vi.fn(async (_input: string, init?: RequestInit) => {
      expect(init?.credentials).toBe('include');
      return new Response(JSON.stringify({ code: 200, data: { applicationName: '协作平台', forceLogin: true } }));
    });
    vi.stubGlobal('fetch', fetchMock);
    await expect(fetchOidcContext('a'.repeat(43))).resolves.toEqual({ applicationName: '协作平台', forceLogin: true });
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () => new Response(JSON.stringify({ code: 200, data: { applicationName: 'x', forceLogin: 'false' } }))
      )
    );
    await expect(fetchOidcContext('a'.repeat(43))).rejects.toThrow();
  });
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
    const params = new URLSearchParams({
      client_id: 'admin',
      redirect_uri: 'https://app.example/admin/sso/callback?tenant=x%26y',
      state,
      code_challenge: 'owned-challenge',
      code_challenge_method: 'S256'
    });
    const query = parseAuthorizeQuery(params.toString());
    expect(query.state).toBe(state);
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: string) => {
        expect(new URL(input, 'https://sso.example').searchParams.get('state')).toBe(state);
        return new Response(JSON.stringify({ code: 200, data: { loginRequired: true } }));
      })
    );
    await expect(requestAuthorize(query)).resolves.toEqual({ loginRequired: true, redirectUri: undefined });
    expect(() => parseAuthorizeQuery(`${params}&state=second`)).toThrow();
  });

  it('never reflects backend credentials in user-facing failures', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(JSON.stringify({ code: 500, msg: 'canary-code-verifier-token' })))
    );
    await expect(loginWithPassword('owned-user', 'owned-password')).rejects.toThrow(
      '登录失败，请检查用户名和密码后重试。'
    );
  });

  it('parses authorize query without holding a secret', () => {
    const query = parseAuthorizeQuery(
      '?response_type=code&client_id=admin&redirect_uri=http://127.0.0.1:4174/sso/callback&state=abc&code_challenge=xyz&code_challenge_method=S256'
    );
    expect(query.clientId).toBe('admin');
    expect(query).not.toHaveProperty('sso_secret');
  });
});
