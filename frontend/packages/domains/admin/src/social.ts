import type { HttpClient } from '@namewta/platform-contracts';

export interface SocialProvider {
  providerKey: string;
  name: string;
  icon: string;
}
export type SocialPurpose = 'LOGIN' | 'BIND';
export interface SocialAuthorization {
  authorizationUrl: string;
  state: string;
  transactionKey: string;
  expiresIn: number;
}
export interface SocialExchange {
  source: string;
  socialCode: string;
  socialState: string;
  transactionKey: string;
}
export type SocialLoginResult =
  | {
      nextAction: 'LOGIN_COMPLETE';
      accessToken: string;
      clientId: string;
      authSource: string;
      globalLogoutAvailable: boolean;
    }
  | { nextAction: 'COMPLETE_PROFILE'; registrationTicket: string; requiredFields: string[] }
  | { nextAction: 'BIND_REQUIRED'; message: string };
export interface SocialBinding {
  id: string;
  providerKey: string;
  name: string;
  userName: string;
  createTime: string;
}
export interface SocialSession {
  authSource: string;
  /** 兼容旧服务端；表示能发起中央退出，不证明其他 App 已收到通知。 */
  globalLogoutAvailable: boolean;
  rpInitiatedLogoutAvailable?: boolean;
  backchannelSessionLinked?: boolean;
}

function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('第三方登录响应格式无效');
  return value as Record<string, unknown>;
}
function required(value: unknown): string {
  if (typeof value !== 'string' || !value.trim()) throw new Error('第三方登录响应不完整');
  return value;
}
function optional(value: unknown): string {
  return typeof value === 'string' ? value : '';
}
export function readSocialProviders(value: unknown): SocialProvider[] {
  if (value == null) return [];
  if (!Array.isArray(value)) throw new Error('第三方登录入口格式无效');
  return value.map(item => {
    const row = record(item);
    return { providerKey: required(row.providerKey), name: required(row.name), icon: optional(row.icon) };
  });
}
export function readSocialLogin(value: unknown, clientId: string): SocialLoginResult {
  const row = record(value);
  if (row.nextAction === 'BIND_REQUIRED')
    return { nextAction: 'BIND_REQUIRED', message: optional(row.message) || '请先登录已有账号，再绑定此第三方账号。' };
  if (row.nextAction === 'COMPLETE_PROFILE') {
    if (!Array.isArray(row.requiredFields) || row.requiredFields.some(field => field !== 'phoneNumber'))
      throw new Error('暂不支持此补充资料要求');
    return {
      nextAction: 'COMPLETE_PROFILE',
      registrationTicket: required(row.registrationTicket),
      requiredFields: [...row.requiredFields]
    };
  }
  if (required(row.client_id) !== clientId) throw new Error('登录响应客户端不匹配');
  return {
    nextAction: 'LOGIN_COMPLETE',
    accessToken: required(row.access_token),
    clientId,
    authSource: optional(row.authSource) || 'OIDC',
    globalLogoutAvailable: row.globalLogoutAvailable === true
  };
}

/** 外部身份只在服务端换票；调用方确认会话仍有效后再保存业务 token。 */
export function createSocialService(http: HttpClient, clientId: string) {
  const request = async (url: string, method: 'get' | 'post', data?: unknown, anonymous = false) => {
    const response = record(
      await http.request<unknown>({
        url,
        method,
        ...(data === undefined ? {} : { data }),
        headers: { ...(anonymous ? { isToken: false } : {}), repeatSubmit: false },
        timeout: 20000
      })
    );
    return response.data;
  };
  return {
    async authorize(input: {
      providerKey: string;
      purpose: SocialPurpose;
      returnPath: string;
    }): Promise<SocialAuthorization> {
      const row = record(
        await request('/auth/social/authorize', 'post', { ...input, clientId }, input.purpose === 'LOGIN')
      );
      const url = new URL(required(row.authorizationUrl));
      if (!['https:', 'http:'].includes(url.protocol)) throw new Error('授权地址不可用');
      if (typeof row.expiresIn !== 'number' || row.expiresIn <= 0) throw new Error('授权事务已失效');
      return {
        authorizationUrl: url.href,
        state: required(row.state),
        transactionKey: required(row.transactionKey),
        expiresIn: row.expiresIn
      };
    },
    async login(input: SocialExchange) {
      return readSocialLogin(
        await request('/auth/login', 'post', { ...input, clientId, grantType: 'social' }, true),
        clientId
      );
    },
    async bind(input: SocialExchange) {
      await request('/auth/social/callback', 'post', { ...input, clientId, grantType: 'social' });
    },
    async register(input: { registrationTicket: string; transactionKey: string; phoneNumber: string }) {
      return readSocialLogin(await request('/auth/social/register', 'post', { ...input, clientId }, true), clientId);
    },
    async bindings(): Promise<SocialBinding[]> {
      const value = await request('/auth/social/bindings', 'get');
      if (!Array.isArray(value)) throw new Error('绑定列表格式无效');
      return value.map(item => {
        const row = record(item);
        return {
          id: required(String(row.id ?? '')),
          providerKey: required(row.providerKey),
          name: optional(row.name) || required(row.providerKey),
          userName: optional(row.userName),
          createTime: optional(row.createTime)
        };
      });
    },
    async unbind(id: string) {
      await request('/auth/social/unbind', 'post', { id });
    },
    async session(): Promise<SocialSession> {
      const row = record(await request('/auth/social/session', 'get'));
      return {
        authSource: optional(row.authSource) || 'LOCAL',
        globalLogoutAvailable: row.globalLogoutAvailable === true,
        rpInitiatedLogoutAvailable:
          row.rpInitiatedLogoutAvailable === undefined
            ? row.globalLogoutAvailable === true
            : row.rpInitiatedLogoutAvailable === true,
        backchannelSessionLinked: row.backchannelSessionLinked === true
      };
    },
    async logout(): Promise<{ endSessionUrl: string; state: string }> {
      const row = record(await request('/auth/social/logout', 'post'));
      const url = new URL(required(row.endSessionUrl));
      if (!['https:', 'http:'].includes(url.protocol)) throw new Error('退出地址不可用');
      return { endSessionUrl: url.href, state: required(row.state) };
    }
  };
}
export type SocialService = ReturnType<typeof createSocialService>;
