export const ssoLoginMethods = ['password'] as const;

export interface SsoAuthorizeQuery {
  clientId: string;
  codeChallenge: string;
  codeChallengeMethod: string;
  redirectUri: string;
  responseType: string;
  state: string;
}

export function parseAuthorizeQuery(search: string): SsoAuthorizeQuery {
  const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search);
  return {
    clientId: params.get('client_id') ?? '',
    codeChallenge: params.get('code_challenge') ?? '',
    codeChallengeMethod: params.get('code_challenge_method') ?? '',
    redirectUri: params.get('redirect_uri') ?? '',
    responseType: params.get('response_type') ?? 'code',
    state: params.get('state') ?? ''
  };
}

export function apiUrl(path: string): string {
  const base = String(import.meta.env.VITE_SSO_API ?? '').replace(/\/$/, '');
  return `${base}${path}`;
}

async function readJson<T>(response: Response): Promise<T> {
  return (await response.json()) as T;
}

export async function fetchSession(): Promise<boolean> {
  const response = await fetch(apiUrl('/sso/session'), { credentials: 'include' });
  const body = await readJson<{ code?: number }>(response);
  return body.code === 200;
}

export async function loginWithPassword(username: string, password: string): Promise<void> {
  const response = await fetch(apiUrl('/sso/login'), {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const body = await readJson<{ code?: number; msg?: string }>(response);
  if (body.code !== 200) throw new Error(body.msg || '登录失败');
}

export async function requestAuthorize(query: SsoAuthorizeQuery): Promise<{ loginRequired: boolean; redirectUri?: string }> {
  const params = new URLSearchParams({
    response_type: query.responseType,
    client_id: query.clientId,
    redirect_uri: query.redirectUri,
    state: query.state,
    code_challenge: query.codeChallenge,
    code_challenge_method: query.codeChallengeMethod
  });
  const response = await fetch(`${apiUrl('/sso/oauth2/authorize')}?${params.toString()}`, { credentials: 'include' });
  const body = await readJson<{ code?: number; data?: { loginRequired?: boolean; redirectUri?: string }; msg?: string }>(
    response
  );
  if (body.code !== 200) throw new Error(body.msg || '授权失败');
  return {
    loginRequired: body.data?.loginRequired === true,
    redirectUri: body.data?.redirectUri
  };
}
