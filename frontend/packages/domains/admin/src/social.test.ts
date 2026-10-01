import { describe, expect, it, vi } from 'vitest';
import { createSocialService, readSocialLogin, readSocialProviders } from './social';
describe('external identity API', () => {
  it('projects provider display fields and rejects malformed configuration', () => {
    expect(
      readSocialProviders([
        { providerKey: 'corp', name: '公司登录', icon: 'tabler:key', clientSecret: 'must-not-project' }
      ])
    ).toEqual([{ providerKey: 'corp', name: '公司登录', icon: 'tabler:key' }]);
    expect(() => readSocialProviders([{ name: 'missing key' }])).toThrow();
  });
  it('keeps profile completion and binding-required responses tokenless', () => {
    expect(
      readSocialLogin(
        { nextAction: 'COMPLETE_PROFILE', registrationTicket: 'ticket', requiredFields: ['phoneNumber'] },
        'home'
      )
    ).toEqual({ nextAction: 'COMPLETE_PROFILE', registrationTicket: 'ticket', requiredFields: ['phoneNumber'] });
    expect(
      readSocialLogin({ nextAction: 'BIND_REQUIRED', message: '请绑定', access_token: 'ignored' }, 'admin')
    ).toEqual({ nextAction: 'BIND_REQUIRED', message: '请绑定' });
    expect(() => readSocialLogin({ access_token: 'token', client_id: 'other' }, 'home')).toThrow();
  });
  it('sends explicit login/binding purpose and preserves the transaction key during registration', async () => {
    const request = vi
      .fn()
      .mockResolvedValueOnce({
        data: { authorizationUrl: 'https://sso.example/authorize', state: 's', transactionKey: 't', expiresIn: 300 }
      })
      .mockResolvedValueOnce({ data: {} })
      .mockResolvedValueOnce({ data: { access_token: 'business', client_id: 'home', nextAction: 'LOGIN_COMPLETE' } });
    const service = createSocialService({ request }, 'home');
    await service.authorize({ providerKey: 'corp', purpose: 'BIND', returnPath: '/account/bindings' });
    expect(request.mock.calls[0][0]).toMatchObject({
      url: '/auth/social/authorize',
      data: { clientId: 'home', purpose: 'BIND' }
    });
    expect(request.mock.calls[0][0].headers.isToken).toBeUndefined();
    await service.bind({ source: 'corp', socialCode: 'c', socialState: 's', transactionKey: 't' });
    expect(request.mock.calls[1][0]).toMatchObject({ url: '/auth/social/callback', data: { transactionKey: 't' } });
    expect(
      await service.register({ phoneNumber: '13800138000', registrationTicket: 'ticket', transactionKey: 't' })
    ).toMatchObject({ accessToken: 'business' });
    expect(request.mock.calls[2][0]).toMatchObject({
      url: '/auth/social/register',
      headers: { isToken: false },
      data: { transactionKey: 't', registrationTicket: 'ticket' }
    });
  });
});
