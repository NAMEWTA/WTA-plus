import type { HttpClient, HttpRequest, SessionStore } from '@namewta/platform-contracts';
import { describe, expect, it, vi } from 'vitest';
import { createClientSessionKey, createIdentityAccessService, adminDomainModule, IdentityAccessError } from './index';

const passwordPolicy = Object.freeze({
  minimumLength: 8,
  maximumLength: 30,
  requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'],
  allowedSpecialCharacters: '@$!%*?&'
});

const createHarness = (responses: Readonly<Record<string, unknown>>) => {
  const identityCalls: string[] = [];
  const requests: HttpRequest[] = [];
  const http: HttpClient = {
    async request<T>(request: HttpRequest): Promise<T> {
      requests.push(request);
      return responses[request.url] as T;
    }
  };
  const session: SessionStore = {
    clear: vi.fn(),
    getToken: vi.fn(() => null),
    setToken: vi.fn()
  };
  const identity = {
    async loadInfo() {
      identityCalls.push('info');
      return (responses['/system/user/getInfo'] as { data?: unknown } | undefined)?.data;
    },
    async loadMenus() {
      identityCalls.push('menus');
      return (responses['/system/menu/getRouters'] as { data?: unknown } | undefined)?.data;
    }
  };
  return { http, identity, identityCalls, requests, session };
};

describe('identity access domain', () => {
  it('publishes frozen headless metadata', () => {
    expect(adminDomainModule).toEqual({
      id: 'admin',
      backendModules: ['wta-admin'],
      capabilities: [
        'client-context',
        'password-login',
        'registration',
        'oauth-callback',
        'identity-info',
        'server-menu',
        'isolated-session'
      ]
    });
    expect(Object.isFrozen(adminDomainModule)).toBe(true);
    expect(Object.isFrozen(adminDomainModule.backendModules)).toBe(true);
  });

  it('rejects an invalid injected ClientContext before any auth request', async () => {
    const harness = createHarness({});

    expect(() =>
      createIdentityAccessService({
        client: { clientId: '   ' },
        http: harness.http,
        identity: harness.identity,
        session: harness.session
      })
    ).toThrow('ClientContext.clientId is required');
    expect(harness.requests).toEqual([]);
  });

  it('loads strict client context before requesting a verification code', async () => {
    const harness = createHarness({
      '/auth/client/context': { code: 200, data: { clientEnabled: true, registerEnabled: false } },
      '/auth/code': { code: 200, data: { captchaEnabled: false } }
    });
    const service = createIdentityAccessService({
      client: { clientId: ' client-proof ' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await expect(service.prepareLogin()).resolves.toEqual({
      context: { clientEnabled: true, registerEnabled: false },
      verification: { captchaEnabled: false }
    });
    expect(harness.requests).toEqual([
      { url: '/auth/client/context', method: 'get', headers: { isToken: false } },
      { url: '/auth/code', method: 'get', headers: { isToken: false }, timeout: 20000 }
    ]);
  });

  it('accepts a complete enabled captcha response and preserves its challenge', async () => {
    const harness = createHarness({
      '/auth/client/context': { code: 200, data: { clientEnabled: true, registerEnabled: false } },
      '/auth/code': { code: 200, data: { captchaEnabled: true, img: ' captcha-image ', uuid: ' captcha-uuid ' } }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await expect(service.prepareLogin()).resolves.toMatchObject({
      verification: { captchaEnabled: true, img: 'captcha-image', uuid: 'captcha-uuid' }
    });
  });

  it.each([
    { captchaEnabled: true },
    { captchaEnabled: true, img: '', uuid: 'captcha-uuid' },
    { captchaEnabled: true, img: 'captcha-image', uuid: '   ' },
    { captchaEnabled: true, img: 42, uuid: 'captcha-uuid' }
  ])('fails closed when an enabled captcha challenge is malformed: %j', async verification => {
    const harness = createHarness({
      '/auth/client/context': { code: 200, data: { clientEnabled: true, registerEnabled: false } },
      '/auth/code': { code: 200, data: verification }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await expect(service.prepareLogin()).rejects.toMatchObject({ code: 'invalid-verification-response' });
    await expect(service.login({ username: 'user', password: 'secret' })).rejects.toMatchObject({
      code: 'client-context-unavailable'
    });
    expect(harness.requests.map(request => request.url)).toEqual(['/auth/client/context', '/auth/code']);
  });

  it.each([
    { clientEnabled: 'true', registerEnabled: true },
    { clientEnabled: true },
    { clientEnabled: false, registerEnabled: true }
  ])('fails closed before code/login when client context is invalid: %j', async data => {
    const harness = createHarness({ '/auth/client/context': { code: 200, data } });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await expect(service.prepareLogin()).rejects.toBeInstanceOf(IdentityAccessError);
    expect(harness.requests.map(request => request.url)).toEqual(['/auth/client/context']);
    await expect(service.login({ username: 'user', password: 'secret' })).rejects.toMatchObject({
      code: 'client-context-unavailable'
    });
    expect(harness.requests.map(request => request.url)).toEqual(['/auth/client/context']);
  });

  it('uses the validated clientId in login payload and writes only the injected session', async () => {
    const harness = createHarness({
      '/auth/client/context': {
        code: 200,
        data: { clientEnabled: true, registerEnabled: true, passwordPolicy }
      },
      '/auth/code': { code: 200, data: { captchaEnabled: false } },
      '/auth/login': { code: 200, data: { access_token: 'client-token' } }
    });
    const service = createIdentityAccessService({
      client: { clientId: ' client-proof ' },
      encryptLoginRequest: false,
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await service.prepareLogin();
    await expect(service.login({ username: 'user', password: 'secret' })).resolves.toEqual({
      accessToken: 'client-token'
    });
    expect(harness.requests.at(-1)).toEqual({
      url: '/auth/login',
      method: 'post',
      headers: { isToken: false, isEncrypt: false, repeatSubmit: false },
      data: { username: 'user', password: 'secret', clientId: 'client-proof', grantType: 'password' }
    });
    expect(harness.session.setToken).toHaveBeenCalledWith('client-token');
  });

  it('creates an explicit per-App and per-client token namespace', () => {
    expect(createClientSessionKey('fixture-web', ' fixture-proof ')).toBe(
      'namewta:fixture-web:fixture-proof:access-token'
    );
    expect(createClientSessionKey('fixture-web', 'client/with space')).toBe(
      'namewta:fixture-web:client%2Fwith%20space:access-token'
    );
    expect(() => createClientSessionKey('', 'client-proof')).toThrow('App id is required');
    expect(createClientSessionKey('fixture-web', 'fixture-proof')).not.toBe('Admin-Token');
  });

  it('owns registration, identity, menu, logout, and OAuth use cases behind ClientContext', async () => {
    const harness = createHarness({
      '/auth/client/context': {
        code: 200,
        data: { clientEnabled: true, registerEnabled: true, passwordPolicy }
      },
      '/auth/code': { code: 200, data: { captchaEnabled: false } },
      '/auth/register': { code: 200, data: {} },
      '/system/user/getInfo': {
        code: 200,
        data: { user: { userId: 1 }, roles: ['operator'], permissions: ['system:user:list'] }
      },
      '/system/menu/getRouters': { code: 200, data: [{ path: '/system', component: 'Layout' }] },
      '/auth/logout': { code: 200, data: {} },
      '/auth/social/callback': { code: 200, data: {} }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await service.prepareLogin();
    await service.register({
      username: 'new-user',
      password: 'ValidPass!9',
      confirmPassword: 'ValidPass!9'
    });
    await expect(service.getInfo()).resolves.toMatchObject({ roles: ['operator'], permissions: ['system:user:list'] });
    await expect(service.getMenus()).resolves.toEqual([{ path: '/system', component: 'Layout' }]);
    await service.socialCallback({ code: 'oauth-code', state: 'oauth-state' });
    await service.logout();

    expect(harness.requests.map(request => request.url)).toEqual([
      '/auth/client/context',
      '/auth/code',
      '/auth/register',
      '/auth/social/callback',
      '/auth/logout'
    ]);
    expect(harness.identityCalls).toEqual(['info', 'menus']);
    expect(harness.session.clear).toHaveBeenCalledOnce();
  });

  it('recursively validates and freezes server menus before exposing them', async () => {
    const harness = createHarness({
      '/system/menu/getRouters': {
        data: [
          {
            path: '/system',
            component: 'Layout',
            name: null,
            redirect: null,
            alwaysShow: null,
            permissions: null,
            meta: { activeMenu: null, link: null, title: '系统管理', noCache: false },
            children: [
              {
                path: 'user',
                name: 'User',
                component: 'system/user/index',
                query: null,
                meta: null,
                children: null
              }
            ]
          }
        ]
      }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    const menus = await service.getMenus();

    expect(menus).toEqual([
      {
        path: '/system',
        component: 'Layout',
        meta: { title: '系统管理', noCache: false },
        children: [{ path: 'user', name: 'User', component: 'system/user/index' }]
      }
    ]);
    expect(Object.isFrozen(menus)).toBe(true);
    expect(Object.isFrozen(menus[0])).toBe(true);
    expect(Object.isFrozen(menus[0]?.children)).toBe(true);
    expect(Object.isFrozen(menus[0]?.children?.[0])).toBe(true);
  });

  it('normalizes nullable optional menu transport fields from the Java backend', async () => {
    const harness = createHarness({
      '/system/menu/getRouters': {
        data: [
          {
            path: '/system',
            component: 'Layout',
            redirect: null,
            permissions: null,
            meta: {
              title: '绯荤粺绠＄悊',
              activeMenu: null,
              icon: null,
              link: null,
              noCache: null
            },
            children: [{ path: 'user', component: 'system/user/index', children: null, meta: null }]
          }
        ]
      }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await expect(service.getMenus()).resolves.toEqual([
      {
        path: '/system',
        component: 'Layout',
        meta: { title: '绯荤粺绠＄悊' },
        children: [{ path: 'user', component: 'system/user/index' }]
      }
    ]);
  });

  it.each([
    { data: [{ path: '/system', children: [{ path: 42, component: 'system/user/index' }] }] },
    { data: [{ path: '/system', children: {} }] },
    { data: [{ path: '/system', meta: { noCache: 'false' } }] },
    { data: [{ path: null, component: 'Layout' }] },
    { data: [{ path: '   ', component: 'Layout' }] }
  ])('fails closed for a malformed nested server menu: $data', async ({ data }) => {
    const harness = createHarness({ '/system/menu/getRouters': { data } });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await expect(service.getMenus()).rejects.toMatchObject({ code: 'invalid-menu-response' });
  });

  it('fails registration closed when the validated Client disables it', async () => {
    const harness = createHarness({
      '/auth/client/context': { code: 200, data: { clientEnabled: true, registerEnabled: false } },
      '/auth/code': { code: 200, data: { captchaEnabled: false } }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await service.prepareLogin();
    await expect(
      service.register({ username: 'new-user', password: 'secret', confirmPassword: 'secret' })
    ).rejects.toMatchObject({ code: 'registration-disabled' });
    expect(harness.requests.map(request => request.url)).toEqual(['/auth/client/context', '/auth/code']);
  });

  it('fails registration closed when the public password policy is missing', async () => {
    const harness = createHarness({
      '/auth/client/context': { code: 200, data: { clientEnabled: true, registerEnabled: true } },
      '/auth/code': { code: 200, data: { captchaEnabled: false } }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await service.prepareLogin();
    await expect(
      service.register({ username: 'new-user', password: 'ValidPass!9', confirmPassword: 'ValidPass!9' })
    ).rejects.toMatchObject({ code: 'password-policy-unavailable' });
    expect(harness.requests.map(request => request.url)).toEqual(['/auth/client/context', '/auth/code']);
  });

  it('returns stable policy violations without sending a weak registration request', async () => {
    const harness = createHarness({
      '/auth/client/context': {
        code: 200,
        data: { clientEnabled: true, registerEnabled: true, passwordPolicy }
      },
      '/auth/code': { code: 200, data: { captchaEnabled: false } }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await service.prepareLogin();
    await expect(
      service.register({ username: 'new-user', password: 'weak', confirmPassword: 'weak' })
    ).rejects.toMatchObject({
      code: 'password-policy-violation',
      violations: [
        { reason: 'PASSWORD_TOO_SHORT' },
        { reason: 'PASSWORD_MISSING_UPPERCASE' },
        { reason: 'PASSWORD_MISSING_DIGIT' },
        { reason: 'PASSWORD_MISSING_SPECIAL' }
      ]
    });
    expect(harness.requests.map(request => request.url)).toEqual(['/auth/client/context', '/auth/code']);
  });

  it('validates ClientContext before social login and uses the same injected clientId', async () => {
    const harness = createHarness({
      '/auth/client/context': { code: 200, data: { clientEnabled: true, registerEnabled: false } },
      '/auth/login': { code: 200, data: { access_token: 'social-token' } }
    });
    const service = createIdentityAccessService({
      client: { clientId: ' social-client ' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await service.socialLogin({ socialCode: 'code', socialState: 'state', source: 'gitee' });

    expect(harness.requests).toEqual([
      { url: '/auth/client/context', method: 'get', headers: { isToken: false } },
      {
        url: '/auth/login',
        method: 'post',
        headers: { isToken: false, isEncrypt: true, repeatSubmit: false },
        data: {
          socialCode: 'code',
          socialState: 'state',
          source: 'gitee',
          clientId: 'social-client',
          grantType: 'social'
        }
      }
    ]);
  });

  it('owns AuthController social binding and unbinding requests', async () => {
    const harness = createHarness({
      '/auth/binding/github': { code: 200, data: 'https://example.test/oauth' },
      '/auth/unlock/auth%2F1': { code: 200, data: {} }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await service.social.bindingUrl('github');
    await service.social.unlock('auth/1');

    expect(harness.requests).toEqual([
      { url: '/auth/binding/github', method: 'get' },
      { url: '/auth/unlock/auth%2F1', method: 'delete' }
    ]);
  });

  it('revokes a previous prepared state before revalidating ClientContext', async () => {
    const requests: HttpRequest[] = [];
    let contextCalls = 0;
    const http: HttpClient = {
      async request<T>(request: HttpRequest): Promise<T> {
        requests.push(request);
        if (request.url === '/auth/client/context') {
          contextCalls += 1;
          return {
            code: 200,
            data:
              contextCalls === 1
                ? { clientEnabled: true, registerEnabled: true }
                : { clientEnabled: false, registerEnabled: true }
          } as T;
        }
        return { code: 200, data: { captchaEnabled: false } } as T;
      }
    };
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http,
      identity: createHarness({}).identity,
      session: createHarness({}).session
    });

    await service.prepareLogin();
    await expect(service.getClientContext()).rejects.toMatchObject({ code: 'client-context-unavailable' });
    await expect(service.login({ username: 'user', password: 'secret' })).rejects.toMatchObject({
      code: 'client-context-unavailable'
    });
    expect(requests.map(request => request.url)).toEqual([
      '/auth/client/context',
      '/auth/code',
      '/auth/client/context'
    ]);
  });

  it('preserves an OAuth callback token through the injected session boundary', async () => {
    const harness = createHarness({
      '/auth/client/context': { code: 200, data: { clientEnabled: true, registerEnabled: false } },
      '/auth/social/callback': { code: 200, data: { access_token: 'rotated-token' }, msg: 'linked' }
    });
    const service = createIdentityAccessService({
      client: { clientId: 'client-proof' },
      http: harness.http,
      identity: harness.identity,
      session: harness.session
    });

    await expect(service.socialCallback({ code: 'code', state: 'state' })).resolves.toEqual({
      accessToken: 'rotated-token',
      message: 'linked'
    });
    expect(harness.session.setToken).toHaveBeenCalledWith('rotated-token');
  });
});
