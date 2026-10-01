import { describe, expect, it, vi } from 'vitest';
import { createAuthConfigurationService, mapAuthRegistration } from './service';
const registration = {
  id: '123',
  providerId: '10',
  businessClientId: 'home',
  externalClientId: 'external-home',
  secretConfigured: true,
  redirectUri: 'https://home.example/social-callback',
  postLogoutRedirectUri: '',
  scopes: ['openid'],
  firstLoginPolicy: 'AUTO_REGISTER',
  enabled: true,
  version: 4,
  options: {}
};
describe('external login configuration transport', () => {
  it('never projects a returned secret and keeps persisted credentials when editing without a replacement', async () => {
    const mapped = mapAuthRegistration({ ...registration, clientSecret: 'never-return' });
    expect(mapped).not.toHaveProperty('clientSecret');
    const request = vi.fn().mockResolvedValue({ code: 200, data: '123' });
    const service = createAuthConfigurationService({ request });
    await service.registrations.save({ ...mapped, clientSecret: '' });
    expect(request.mock.calls[0][0]).toMatchObject({
      url: '/system/auth/registration/edit',
      method: 'post',
      data: { id: '123', version: 4 }
    });
    expect(request.mock.calls[0][0].data).not.toHaveProperty('clientSecret');
    expect(request.mock.calls[0][0].data).not.toHaveProperty('secretConfigured');
    await service.registrations.save({ ...mapped, clientSecret: 'rotated' });
    expect(request.mock.calls[1][0].data.clientSecret).toBe('rotated');
  });
  it('uses versioned delete and explicit cache refresh mutations', async () => {
    const request = vi.fn().mockResolvedValue({ code: 200 });
    const service = createAuthConfigurationService({ request });
    await service.registrations.remove(mapAuthRegistration(registration));
    await service.registrations.refresh();
    expect(request.mock.calls[0][0]).toMatchObject({
      url: '/system/auth/registration/remove',
      method: 'post',
      data: { id: '123', version: 4 }
    });
    expect(request.mock.calls[1][0]).toMatchObject({ url: '/system/auth/registration/refresh', method: 'post' });
  });
});
