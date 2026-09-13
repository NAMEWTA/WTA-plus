import type { ClientVO } from './types';

export type SsoAccessState = 'bound' | 'missing';

/**
 * Own-app bind success vs red 「没有接入」. Registered means SSO 管理 has exact redirects.
 */
export function ssoAccessState(
  client: Pick<ClientVO, 'ssoEnabled' | 'ssoAuthMode' | 'ssoRedirectUris'>
): SsoAccessState {
  const redirects = (client.ssoRedirectUris ?? '').trim();
  const mode = client.ssoAuthMode;
  if (client.ssoEnabled === true && redirects.length > 0 && (mode === 'sso' || mode === 'both')) {
    return 'bound';
  }
  return 'missing';
}

export function isSsoRegistered(client: Pick<ClientVO, 'ssoRedirectUris'>): boolean {
  return (client.ssoRedirectUris ?? '').trim().length > 0;
}
