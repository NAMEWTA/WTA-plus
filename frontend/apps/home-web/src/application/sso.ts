import { buildSsoCallbackUri, createSsoAuth, readSsoTokenResponse, SsoCallbackError } from '@namewta/platform-auth';

const pendingKey = 'namewta-home-sso-pending';

function readPending(): unknown {
  try {
    const raw = sessionStorage.getItem(pendingKey);
    const value: unknown = raw ? JSON.parse(raw) : null;
    return value;
  } catch { return null; }
}

export const homeSso = createSsoAuth({
  exchangeToken: async input => {
    // 一次性换票由本页呈现固定错误；不把含code/verifier的请求交给全局弹窗或自动重试。
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), 10_000);
    try {
      const base = String(import.meta.env.VITE_APP_BASE_API ?? '').replace(/\/$/, '');
      const response = await fetch(`${base}/sso/oauth2/token`, {
        method: 'POST', credentials: 'omit', signal: controller.signal,
        headers: { 'Content-Type': 'application/json', clientid: input.clientId },
        body: JSON.stringify({ grant_type: 'authorization_code', code: input.code, redirect_uri: input.redirectUri,
          client_id: input.clientId, code_verifier: input.codeVerifier })
      });
      if (!response.ok) throw new SsoCallbackError('network');
      const body: unknown = await response.json();
      return readSsoTokenResponse(body);
    } finally { window.clearTimeout(timer); }
  },
  navigate: url => { window.location.assign(url); },
  randomBytes: size => crypto.getRandomValues(new Uint8Array(size)),
  sha256: async bytes => new Uint8Array(await crypto.subtle.digest('SHA-256', new Uint8Array(bytes))),
  storage: {
    clear: () => sessionStorage.removeItem(pendingKey),
    load: readPending,
    save: value => sessionStorage.setItem(pendingKey, JSON.stringify(value))
  }
});

export function homeSsoRedirectUri(): string {
  return buildSsoCallbackUri(window.location.origin, import.meta.env.VITE_APP_CONTEXT_PATH || '/');
}
