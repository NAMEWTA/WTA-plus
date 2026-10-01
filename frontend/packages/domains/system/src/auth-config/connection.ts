import type { OidcAuthenticationMethod } from './service';

/** 公共部署地址只能包含协议、主机与路径，保留同域部署的静态/API 前缀。 */
export function normalizeAuthPublicUrl(value: string): string {
  let url: URL;
  try {
    url = new URL(value.trim());
  } catch {
    throw new Error('请输入完整的 HTTP(S) 公共访问地址');
  }
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    throw new Error('公共访问地址不能包含账号、查询参数或片段');
  }
  return url.href.replace(/\/+$/, '');
}

export function authCallbackUrls(appPublicUrl: string) {
  const base = normalizeAuthPublicUrl(appPublicUrl);
  return { redirectUri: `${base}/social-callback`, postLogoutRedirectUri: `${base}/logout/callback` };
}

/** 显式接入选项优先于身份源默认值；未声明时沿用现有 Basic 合同。 */
export function oidcAuthenticationMethod(
  provider: Record<string, string>,
  registration: Record<string, string>
): OidcAuthenticationMethod {
  const value = registration.authenticationMethod ?? provider.authenticationMethod ?? 'client_secret_basic';
  if (value !== 'client_secret_basic' && value !== 'client_secret_post')
    throw new Error('客户端认证方式仅支持 client_secret_basic 或 client_secret_post');
  return value;
}

export function parseAuthOptions(value: string): Record<string, string> {
  let parsed: unknown;
  try {
    parsed = JSON.parse(value);
  } catch {
    throw new Error('高级扩展参数必须是字符串值的 JSON 对象');
  }
  if (
    !parsed ||
    typeof parsed !== 'object' ||
    Array.isArray(parsed) ||
    Object.values(parsed).some(item => typeof item !== 'string')
  ) {
    throw new Error('高级扩展参数必须是字符串值的 JSON 对象');
  }
  return parsed as Record<string, string>;
}
