import { describe, expect, it } from 'vitest';
import { parseAuthorizeQuery, ssoLoginMethods } from './ssoApi';

describe('sso-web login surface', () => {
  it('only accepts warehouse password login', () => {
    expect(ssoLoginMethods).toEqual(['password']);
    expect(ssoLoginMethods).not.toContain('github');
    expect(ssoLoginMethods).not.toContain('wechat');
  });

  it('parses authorize query without holding a secret', () => {
    const query = parseAuthorizeQuery(
      '?response_type=code&client_id=admin&redirect_uri=http://127.0.0.1:4174/sso/callback&state=abc&code_challenge=xyz&code_challenge_method=S256'
    );
    expect(query.clientId).toBe('admin');
    expect(query).not.toHaveProperty('sso_secret');
  });
});
