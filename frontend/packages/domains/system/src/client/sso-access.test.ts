import { describe, expect, it } from 'vitest';
import { isSsoRegistered, ssoAccessState } from './sso-access';

describe('ssoAccessState', () => {
  it('is bound only when enabled, sso/both, and registered redirects exist', () => {
    expect(
      ssoAccessState({
        ssoEnabled: true,
        ssoAuthMode: 'both',
        ssoRedirectUris: 'http://127.0.0.1:4174/sso/callback'
      })
    ).toBe('bound');
    expect(
      ssoAccessState({ ssoEnabled: true, ssoAuthMode: 'both', ssoRedirectUris: '' })
    ).toBe('missing');
    expect(
      ssoAccessState({
        ssoEnabled: false,
        ssoAuthMode: 'both',
        ssoRedirectUris: 'http://127.0.0.1:4174/sso/callback'
      })
    ).toBe('missing');
  });

  it('treats blank redirects as unregistered', () => {
    expect(isSsoRegistered({ ssoRedirectUris: '  ' })).toBe(false);
    expect(isSsoRegistered({ ssoRedirectUris: 'http://127.0.0.1:4175/sso/callback' })).toBe(true);
  });
});
