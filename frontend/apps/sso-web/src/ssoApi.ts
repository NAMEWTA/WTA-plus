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
  for (const key of ['client_id', 'redirect_uri', 'state', 'code_challenge', 'code_challenge_method']) {
    if (params.getAll(key).length !== 1 || !params.get(key)) throw new Error('授权请求无效，请从原应用重新登录。');
  }
  if (params.getAll('response_type').length > 1) throw new Error('授权请求无效，请从原应用重新登录。');
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

async function readJson(response: Response): Promise<Record<string, unknown>> {
  if (!response.ok) throw new Error('SSO 服务暂不可用，请重试。');
  const body: unknown = await response.json();
  if (typeof body !== 'object' || body === null || Array.isArray(body)) throw new Error('SSO 服务暂不可用，请重试。');
  return body as Record<string, unknown>;
}

export async function fetchSession(): Promise<boolean> {
  const response = await fetch(apiUrl('/sso/session'), { credentials: 'include' });
  const body = await readJson(response);
  if (body.code !== 200 && body.msg !== '未登录') throw new Error('无法检查登录状态，请重试。');
  return body.code === 200;
}

export async function loginWithPassword(username: string, password: string): Promise<void> {
  const response = await fetch(apiUrl('/sso/login'), {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const body = await readJson(response);
  if (body.code !== 200) throw new Error('登录失败，请检查用户名和密码后重试。');
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
  const body = await readJson(response);
  if (body.code !== 200) throw new Error('授权未完成，请重试或从原应用重新登录。');
  const data = body.data;
  if (typeof data !== 'object' || data === null || !('loginRequired' in data) || typeof data.loginRequired !== 'boolean') {
    throw new Error('授权未完成，请从原应用重新登录。');
  }
  const redirectUri = 'redirectUri' in data && typeof data.redirectUri === 'string' ? data.redirectUri : undefined;
  if (!data.loginRequired && !redirectUri) throw new Error('授权未完成，请从原应用重新登录。');
  return {
    loginRequired: data.loginRequired,
    redirectUri
  };
}
