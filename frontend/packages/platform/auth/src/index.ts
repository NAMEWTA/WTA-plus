import type { ErrorPresenter, NavigationPort, SessionPort } from '@namewta/platform-contracts';

export interface SsoPendingAuth {
  clientId: string;
  redirectUri: string;
  state: string;
  verifier: string;
}

export interface SsoStateStore {
  clear(): void;
  load(): SsoPendingAuth | null;
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
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('client_id', input.clientId);
  url.searchParams.set('redirect_uri', input.redirectUri);
  url.searchParams.set('state', input.state);
  url.searchParams.set('code_challenge', input.codeChallenge);
  url.searchParams.set('code_challenge_method', 'S256');
  return url.toString();
}

export function createSsoAuth(ports: SsoAuthPorts) {
  return {
    async startSsoLogin(input: { authorizeUrl: string; clientId: string; redirectUri: string }): Promise<void> {
      if (!input.authorizeUrl) throw new Error('缺少 SSO 授权地址');
      const verifier = randomVerifier(ports.randomBytes);
      const digest = await ports.sha256(encoder.encode(verifier));
      const challenge = toBase64Url(digest);
      const state = toBase64Url(ports.randomBytes(16));
      ports.storage.save({
        clientId: input.clientId,
        redirectUri: input.redirectUri,
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
    async handleCallback(search: string): Promise<{ accessToken: string; clientId?: string }> {
      const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search);
      const code = params.get('code');
      const state = params.get('state');
      const pending = ports.storage.load();
      if (!pending || !state || state !== pending.state || !code) {
        ports.storage.clear();
        throw new Error('SSO 回调 state 无效');
      }
      const token = await ports.exchangeToken({
        clientId: pending.clientId,
        code,
        codeVerifier: pending.verifier,
        redirectUri: pending.redirectUri
      });
      ports.storage.clear();
      return token;
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
      await session.logout();
      await navigation.replaceWithLogin(encodeURIComponent(navigation.currentLocation() || '/'));
    } catch {
      // Cancellation and unavailable UI/session adapters both end the recovery attempt.
    } finally {
      state.show = false;
    }
  })();
}
