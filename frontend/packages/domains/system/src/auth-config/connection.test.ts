import { describe, expect, it } from 'vitest';
import { authCallbackUrls, normalizeAuthPublicUrl, oidcAuthenticationMethod, parseAuthOptions } from './connection';

describe('external application configuration', () => {
  it('preserves shared-domain path prefixes and supports an independently hosted app', () => {
    expect(authCallbackUrls(' https://apps.example.test/home/ ')).toEqual({
      redirectUri: 'https://apps.example.test/home/social-callback',
      postLogoutRedirectUri: 'https://apps.example.test/home/logout/callback'
    });
    expect(authCallbackUrls('https://home.example.test').redirectUri).toBe('https://home.example.test/social-callback');
    expect(normalizeAuthPublicUrl('http://127.0.0.1:19442/home/prod-api/')).toBe(
      'http://127.0.0.1:19442/home/prod-api'
    );
  });
  it.each([
    'javascript:alert(1)',
    '/relative',
    'https://user:password@app.example',
    'https://app.example?code=one',
    'https://app.example/#token'
  ])('rejects a public address containing ambiguous or private information: %s', value => {
    expect(() => authCallbackUrls(value)).toThrow();
  });
  it('uses per-app authentication overrides without discarding the provider default', () => {
    expect(oidcAuthenticationMethod({}, {})).toBe('client_secret_basic');
    expect(oidcAuthenticationMethod({ authenticationMethod: 'client_secret_post' }, {})).toBe('client_secret_post');
    expect(
      oidcAuthenticationMethod(
        { authenticationMethod: 'client_secret_post' },
        { authenticationMethod: 'client_secret_basic' }
      )
    ).toBe('client_secret_basic');
    expect(() => oidcAuthenticationMethod({}, { authenticationMethod: 'unsupported' })).toThrow('仅支持');
  });
  it('preserves unrelated public protocol options and rejects non-string JSON fields', () => {
    expect(parseAuthOptions('{"tenantId":"tenant-one","serverUrl":"https://id.example"}')).toEqual({
      tenantId: 'tenant-one',
      serverUrl: 'https://id.example'
    });
    for (const value of ['[]', 'null', '{"nested":{}}', '{"enabled":true}', 'invalid'])
      expect(() => parseAuthOptions(value)).toThrow('JSON 对象');
  });
});
