import { describe, expect, it, vi } from 'vitest';
import {
  createAuthConfigurationService,
  mapAuthRegistration,
  mapOidcMetadataDiagnostic,
  mapAuthClientOption,
  mapAuthProviderOption
} from './service';
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
  it('uses registration-scoped provider choices and projects only the authentication method option', async () => {
    const row = {
      id: '42',
      providerKey: 'company',
      name: 'Company',
      protocol: 'OIDC',
      issuer: 'https://id.example',
      enabled: false,
      options: { authenticationMethod: 'client_secret_post', arbitraryOption: 'not-needed', clientSecret: 'discard' },
      version: 9
    };
    const request = vi.fn().mockResolvedValue({ data: [row] });
    const result = await createAuthConfigurationService({ request }).providerOptions({
      keyword: 'Company',
      selectedId: '42'
    });
    expect(request).toHaveBeenCalledTimes(1);
    expect(request).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/system/auth/registration/provider-options',
        method: 'get',
        params: { keyword: 'Company', selectedId: '42' }
      })
    );
    expect(result[0].options).toEqual({ authenticationMethod: 'client_secret_post' });
    expect(result[0]).not.toHaveProperty('version');
    expect(mapAuthProviderOption({ ...row, options: null }).options).toEqual({});
    expect(() => mapAuthProviderOption({ ...row, enabled: 'false' })).toThrow('状态');
  });
  it('never projects a returned secret and keeps persisted credentials when editing without a replacement', async () => {
    const mapped = mapAuthRegistration({ ...registration, clientSecret: 'never-return' });
    expect(mapped).not.toHaveProperty('clientSecret');
    const request = vi.fn().mockResolvedValue({ code: 200, data: '123' });
    const service = createAuthConfigurationService({ request });
    expect(await service.registrations.save({ ...mapped, clientSecret: '' })).toBe('123');
    expect(request.mock.calls[0][0]).toMatchObject({
      url: '/system/auth/registration/edit',
      method: 'post',
      data: { id: '123', version: 4 }
    });
    expect(request.mock.calls[0][0].data).not.toHaveProperty('clientSecret');
    expect(request.mock.calls[0][0].data).not.toHaveProperty('secretConfigured');
    expect(request.mock.calls[0][0].data).not.toHaveProperty('providerName');
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
  it('queries client options once using OAuth string IDs and never a numeric database client key', async () => {
    const request = vi.fn().mockResolvedValue({
      data: [
        {
          clientId: 'oauth-home',
          clientKey: 'home',
          status: '0',
          socialEnabled: true,
          registerEnabled: true,
          providerCount: 2,
          enabledProviderCount: 1,
          unavailableReason: null
        }
      ]
    });
    const rows = await createAuthConfigurationService({ request }).clientOptions({
      clientIds: ['oauth-home', 'oauth-admin']
    });
    expect(rows[0]).toMatchObject({ clientId: 'oauth-home', providerCount: 2, enabledProviderCount: 1 });
    expect(request).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/system/auth/registration/client-options',
        method: 'get',
        params: { keyword: undefined, clientIds: 'oauth-home,oauth-admin' }
      })
    );
    expect(() => mapAuthClientOption({ ...rows[0], enabledProviderCount: '1' })).toThrow('数量');
  });
  it('returns copyable connection data without projecting an accidental secret field', async () => {
    const request = vi.fn().mockResolvedValue({
      data: {
        registrationId: '123',
        providerName: '企业身份',
        providerKey: 'corporate',
        protocol: 'OIDC',
        issuer: 'https://id.example',
        discoveryUrl: 'https://id.example/.well-known/openid-configuration',
        businessClientId: 'oauth-home',
        externalClientId: 'external-home',
        authenticationMethod: 'client_secret_post',
        redirectUri: 'https://apps.example/home/social-callback',
        postLogoutRedirectUri: null,
        scopes: ['openid', 'profile'],
        backchannelLogoutUri: '',
        enabled: true,
        secretConfigured: true,
        clientSecret: 'discard'
      }
    });
    const result = await createAuthConfigurationService({ request }).connectionInfo('123');
    expect(result).not.toHaveProperty('clientSecret');
    expect(result.postLogoutRedirectUri).toBe('');
    expect(result.backchannelLogoutUri).toBe('');
    expect(request).toHaveBeenCalledWith(
      expect.objectContaining({ url: '/system/auth/registration/123/connection-info', method: 'get' })
    );
  });
  it('retains unknown optional Discovery capabilities and does not manufacture support', async () => {
    const metadata = {
      issuer: 'https://id.example',
      discoveryUrl: 'https://id.example/.well-known/openid-configuration',
      authorizationEndpoint: 'https://id.example/authorize',
      tokenEndpoint: 'https://id.example/token',
      jwksUri: 'https://id.example/jwks',
      userInfoEndpoint: null,
      endSessionEndpoint: null,
      authenticationMethods: [],
      scopes: [],
      pkceMethods: [],
      responseTypes: [],
      signingAlgorithms: [],
      backchannelLogoutSupported: false,
      backchannelLogoutSessionSupported: false,
      checkedAt: '2026-10-01T12:00:00Z'
    };
    const request = vi.fn().mockResolvedValue({ data: metadata });
    const result = await createAuthConfigurationService({ request }).oidcMetadata(metadata.issuer);
    expect(result.endSessionEndpoint).toBe('');
    expect(result.pkceMethods).toEqual([]);
    expect(result.backchannelLogoutSupported).toBe(false);
    expect(request).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/system/auth/provider/oidc-metadata',
        method: 'get',
        params: { issuer: metadata.issuer }
      })
    );
    expect(() => mapOidcMetadataDiagnostic({ ...metadata, backchannelLogoutSupported: 'false' })).toThrow('状态');
  });
});
