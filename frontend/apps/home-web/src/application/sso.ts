import { createSsoAuth } from '@namewta/platform-auth';
import { homeHttp } from './http';

const pendingKey = 'namewta-home-sso-pending';

function readPending() {
  try {
    const raw = sessionStorage.getItem(pendingKey);
    return raw ? (JSON.parse(raw) as { clientId: string; redirectUri: string; state: string; verifier: string }) : null;
  } catch {
    return null;
  }
}

export const homeSso = createSsoAuth({
  exchangeToken: async input => {
    const response = await homeHttp.request<{ code?: number; data?: { access_token?: string; client_id?: string } }>({
      url: '/sso/oauth2/token',
      method: 'post',
      headers: { isToken: false, isEncrypt: false },
      data: {
        grant_type: 'authorization_code',
        code: input.code,
        redirect_uri: input.redirectUri,
        client_id: input.clientId,
        code_verifier: input.codeVerifier
      }
    });
    const accessToken = response.data?.access_token;
    if (!accessToken) throw new Error('SSO 换票失败');
    return { accessToken, clientId: response.data?.client_id };
  },
  navigate: url => {
    window.location.assign(url);
  },
  randomBytes: size => crypto.getRandomValues(new Uint8Array(size)),
  sha256: async bytes => new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)),
  storage: {
    clear: () => sessionStorage.removeItem(pendingKey),
    load: readPending,
    save: value => sessionStorage.setItem(pendingKey, JSON.stringify(value))
  }
});

export function homeSsoRedirectUri(): string {
  return `${window.location.origin}/sso/callback`;
}
