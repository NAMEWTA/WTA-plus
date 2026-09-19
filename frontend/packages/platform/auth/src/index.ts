import type { ErrorPresenter, NavigationPort, SessionPort } from '@namewta/platform-contracts';

export interface SsoPendingAuth {
  clientId: string;
  redirectUri: string;
  returnTo: string;
  state: string;
  verifier: string;
}

export interface SsoStateStore {
  clear(): void;
  load(): unknown;
  save(value: SsoPendingAuth): void;
}

export interface SsoAuthPorts {
  exchangeToken(input: {
    clientId: string;
    code: string;
    codeVerifier: string;
    redirectUri: string;
  }): Promise<{ accessToken: string; clientId?: string }>;
  navigate(url: string): void;
  randomBytes(size: number): Uint8Array;
  sha256(bytes: Uint8Array): Uint8Array | Promise<Uint8Array>;
  storage: SsoStateStore;
}

const encoder = new TextEncoder();
const BASE64URL = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';

function toBase64Url(bytes: Uint8Array): string {
  let output = '';
  for (let index = 0; index < bytes.length; index += 3) {
    const remaining = bytes.length - index;
    const a = bytes[index];
    const b = remaining > 1 ? bytes[index + 1] : 0;
    const c = remaining > 2 ? bytes[index + 2] : 0;
    output += BASE64URL[(a >> 2) & 63];
    output += BASE64URL[((a & 3) << 4) | ((b >> 4) & 15)];
    if (remaining > 1) output += BASE64URL[((b & 15) << 2) | ((c >> 6) & 3)];
    if (remaining > 2) output += BASE64URL[c & 63];
  }
  return output;
}

export function computeS256Challenge(verifier: string, sha256: (bytes: Uint8Array) => Uint8Array): string {
  return toBase64Url(sha256(encoder.encode(verifier)));
}

function randomVerifier(randomBytes: (size: number) => Uint8Array): string {
  return toBase64Url(randomBytes(32));
}

export function buildSsoAuthorizeUrl(input: {
  authorizeUrl: string;
  clientId: string;
  codeChallenge: string;
  redirectUri: string;
  state: string;
}): string {
  const url = new URL(input.authorizeUrl);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.hash) {
    throw new SsoCallbackError('exchange');
  }
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('client_id', input.clientId);
  url.searchParams.set('redirect_uri', input.redirectUri);
  url.searchParams.set('state', input.state);
  url.searchParams.set('code_challenge', input.codeChallenge);
  url.searchParams.set('code_challenge_method', 'S256');
  return url.toString();
}

export type SsoFailureKind = 'state' | 'expired' | 'network' | 'exchange' | 'return-path';

const ssoFailureMessages: Record<SsoFailureKind, string> = {
  state: '登录校验已失效，请重新授权。',
  expired: '登录授权已过期，请重新授权。',
  network: '暂时无法确认登录结果，请重新授权。',
  exchange: '未能完成登录，请重新授权。',
  'return-path': '返回地址无效，请从当前应用重新登录。'
};

/** 只携带固定文案和安全的应用内路径，不保留网络错误中的凭据或请求正文。 */
export class SsoCallbackError extends Error {
  constructor(readonly kind: SsoFailureKind, readonly returnTo = '/') {
    super(ssoFailureMessages[kind]);
    this.name = 'SsoCallbackError';
  }
}

/** 返回值交给当前App的Router，其base负责限定最终应用路径。 */
export function safeSsoReturnTo(value: string): string {
  if (!value.startsWith('/') || value.startsWith('//') || value.includes('\\')
    || [...value].some(character => character.charCodeAt(0) <= 32 || character.charCodeAt(0) === 127)) {
    throw new SsoCallbackError('return-path');
  }
  const url = new URL(value, 'https://app.invalid');
  if (url.origin !== 'https://app.invalid' || /%(?:2f|5c|0[0-9a-f]|1[0-9a-f]|7f)/i.test(url.pathname)) {
    throw new SsoCallbackError('return-path');
  }
  if (url.pathname === '/login' || url.pathname === '/sso/callback') return '/';
  return url.pathname + url.search + url.hash;
}

export function buildSsoCallbackUri(origin: string, contextPath: string): string {
  const value = contextPath.trim().replace(/^\/+|\/+$/g, '');
  if (value && !/^[A-Za-z0-9_-]+(?:\/[A-Za-z0-9_-]+)*$/.test(value)) throw new SsoCallbackError('return-path');
  const url = new URL(origin);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.pathname !== '/' || url.search || url.hash) {
    throw new SsoCallbackError('return-path');
  }
  return new URL(`${value ? `/${value}` : ''}/sso/callback`, url).toString();
}

function responseObject(value: unknown): Record<string, unknown> | undefined {
  return typeof value === 'object' && value !== null && !Array.isArray(value) ? value as Record<string, unknown> : undefined;
}

export function readSsoTokenResponse(value: unknown): { accessToken: string; clientId: string } {
  const body = responseObject(value);
  if (body?.code !== 200) throw new SsoCallbackError(body?.msg === '授权码已过期' ? 'expired' : 'exchange');
  const data = responseObject(body.data);
  if (typeof data?.access_token !== 'string' || !data.access_token || typeof data.client_id !== 'string' || !data.client_id) {
    throw new SsoCallbackError('exchange');
  }
  return { accessToken: data.access_token, clientId: data.client_id };
}

function readPendingAuth(value: unknown): SsoPendingAuth | null {
  const pending = responseObject(value);
  if (!pending || typeof pending.clientId !== 'string' || !pending.clientId
    || typeof pending.redirectUri !== 'string' || !pending.redirectUri
    || typeof pending.state !== 'string' || !pending.state
    || typeof pending.verifier !== 'string' || !/^[A-Za-z0-9._~-]{43,128}$/.test(pending.verifier)
    || typeof pending.returnTo !== 'string') return null;
  try {
    return { clientId: pending.clientId, redirectUri: pending.redirectUri, state: pending.state,
      verifier: pending.verifier, returnTo: safeSsoReturnTo(pending.returnTo) };
  } catch { return null; }
}

export function createSsoAuth(ports: SsoAuthPorts) {
  return {
    async startSsoLogin(input: { authorizeUrl: string; clientId: string; redirectUri: string; returnTo?: string }): Promise<void> {
      ports.storage.clear();
      if (!input.authorizeUrl) throw new Error('缺少 SSO 授权地址');
      const returnTo = safeSsoReturnTo(input.returnTo ?? '/');
      const verifier = randomVerifier(ports.randomBytes);
      const digest = await ports.sha256(encoder.encode(verifier));
      const challenge = toBase64Url(digest);
      const state = toBase64Url(ports.randomBytes(16));
      ports.storage.save({
        clientId: input.clientId,
        redirectUri: input.redirectUri,
        returnTo,
        state,
        verifier
      });
      ports.navigate(
        buildSsoAuthorizeUrl({
          authorizeUrl: input.authorizeUrl,
          clientId: input.clientId,
          codeChallenge: challenge,
          redirectUri: input.redirectUri,
          state
        })
      );
    },
    async handleCallback(search: string): Promise<{ accessToken: string; clientId?: string; returnTo: string }> {
      const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search);
      const code = params.get('code');
      const state = params.get('state');
      let pending: SsoPendingAuth | null;
      try {
        pending = readPendingAuth(ports.storage.load());
      } catch {
        pending = null;
      } finally {
        // 消费发生在任何await之前；网络结果不明时也不允许重用原code/verifier。
        ports.storage.clear();
      }
      if (!pending || !state || state !== pending.state || !code || params.getAll('code').length !== 1
        || params.getAll('state').length !== 1 || params.has('error') || params.has('error_description') || params.has('error_uri')) {
        throw new SsoCallbackError('state', pending?.returnTo);
      }
      try {
        const token = await ports.exchangeToken({ clientId: pending.clientId, code, codeVerifier: pending.verifier, redirectUri: pending.redirectUri });
        if (!token.accessToken || token.clientId !== pending.clientId) throw new SsoCallbackError('exchange');
        return { ...token, returnTo: pending.returnTo };
      } catch (error) {
        throw new SsoCallbackError(error instanceof SsoCallbackError ? error.kind : 'network', pending.returnTo);
      }
    }
  };
}

export interface ReloginState {
  show: boolean;
}

export interface ReloginDependencies {
  navigation: NavigationPort;
  presenter: ErrorPresenter;
  session: SessionPort;
  state: ReloginState;
}

export function requestRelogin({ navigation, presenter, session, state }: ReloginDependencies): void {
  if (state.show) return;
  state.show = true;
  void (async () => {
    try {
      await presenter.confirmSessionExpired();
      const redirect = encodeURIComponent(navigation.currentLocation() || '/');
      try {
        await session.logout();
      } finally {
        await navigation.replaceWithLogin(redirect);
      }
    } catch {
      // Cancellation and unavailable UI/session adapters both end the recovery attempt.
    } finally {
      state.show = false;
    }
  })();
}
