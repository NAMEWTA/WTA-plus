import type { HttpClient } from '@namewta/platform-contracts';
import { responseData } from './mapper';

export interface OidcServiceSettings {
  enabled: boolean;
  issuer: string;
  ssoWebUrl: string;
  allowHttp: boolean;
  codeTtlSeconds: number;
  accessTtlSeconds: number;
  interactionTtlSeconds: number;
  sso: {
    enabled: boolean;
    webOrigin: string;
    webBasePath: string;
    cookieName: string;
    cookieSecure: boolean;
    codeTtlSeconds: number;
    sessionTtlSeconds: number;
  };
}
export interface OidcServiceConfiguration {
  configured: boolean;
  version: number;
  restartRequired: boolean;
  active: OidcServiceSettings;
  saved: OidcServiceSettings;
  signingKeyReady: boolean;
  stateKeyReady: boolean;
}
export interface OidcServiceKey {
  keyId: string;
  kind: 'SIGNING' | 'STATE';
  active: boolean;
  createdAt: string;
}
export interface OidcLogoutDelivery {
  logoutOutboxId: string;
  clientId: string;
  status: string;
  attempts: number;
  nextAttemptAt: string;
  lastError: string;
  createTime: string;
}
function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('OIDC 配置响应无效');
  return value as Record<string, unknown>;
}
function text(value: unknown): string {
  if (typeof value !== 'string') throw new Error('OIDC 配置文本无效');
  return value;
}
function flag(value: unknown): boolean {
  if (typeof value !== 'boolean') throw new Error('OIDC 配置状态无效');
  return value;
}
function count(value: unknown): number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) throw new Error('OIDC 配置数值无效');
  return value;
}
function settings(value: unknown): OidcServiceSettings {
  const row = record(value);
  const sso = record(row.sso);
  return {
    enabled: flag(row.enabled),
    issuer: text(row.issuer),
    ssoWebUrl: text(row.ssoWebUrl),
    allowHttp: flag(row.allowHttp),
    codeTtlSeconds: count(row.codeTtlSeconds),
    accessTtlSeconds: count(row.accessTtlSeconds),
    interactionTtlSeconds: count(row.interactionTtlSeconds),
    sso: {
      enabled: flag(sso.enabled),
      webOrigin: text(sso.webOrigin),
      webBasePath: text(sso.webBasePath),
      cookieName: text(sso.cookieName),
      cookieSecure: flag(sso.cookieSecure),
      codeTtlSeconds: count(sso.codeTtlSeconds),
      sessionTtlSeconds: count(sso.sessionTtlSeconds)
    }
  };
}
export function mapOidcServiceConfiguration(value: unknown): OidcServiceConfiguration {
  const row = record(value);
  return {
    configured: flag(row.configured),
    version: count(row.version),
    restartRequired: flag(row.restartRequired),
    active: settings(row.active),
    saved: settings(row.saved),
    signingKeyReady: flag(row.signingKeyReady),
    stateKeyReady: flag(row.stateKeyReady)
  };
}
export function createOidcConfigurationService(http: HttpClient) {
  const get = async (url: string, signal?: AbortSignal) =>
    responseData(await http.request<unknown>({ url, method: 'get', signal }));
  const post = async (url: string, data?: unknown, signal?: AbortSignal) =>
    responseData(await http.request<unknown>({ url, method: 'post', data, signal }));
  return {
    async get(signal?: AbortSignal) {
      return mapOidcServiceConfiguration(await get('/oidc/admin/service', signal));
    },
    async save(version: number, settings: OidcServiceSettings, signal?: AbortSignal) {
      return mapOidcServiceConfiguration(await post('/oidc/admin/service', { version, settings }, signal));
    },
    async prepareRestart(signal?: AbortSignal) {
      await post('/oidc/admin/service/prepare-restart', undefined, signal);
    },
    async keys(signal?: AbortSignal): Promise<OidcServiceKey[]> {
      const rows = await get('/oidc/admin/keys', signal);
      if (!Array.isArray(rows)) throw new Error('密钥列表无效');
      return rows.map(item => {
        const row = record(item);
        if (row.kind !== 'SIGNING' && row.kind !== 'STATE') throw new Error('密钥类型无效');
        return { keyId: text(row.keyId), kind: row.kind, active: flag(row.active), createdAt: text(row.createdAt) };
      });
    },
    async generate(kind: OidcServiceKey['kind'], signal?: AbortSignal) {
      await post('/oidc/admin/keys/generate', { kind }, signal);
    },
    async importKey(kind: OidcServiceKey['kind'], material: string, signal?: AbortSignal) {
      await post('/oidc/admin/keys/import', { kind, material }, signal);
    },
    async deliveries(pageNum: number, pageSize: number, signal?: AbortSignal) {
      const data = record(
        await http.request<unknown>({
          url: '/oidc/admin/logout-deliveries',
          method: 'get',
          params: { pageNum, pageSize },
          signal
        })
      );
      if (!Array.isArray(data.rows)) throw new Error('退出通知列表无效');
      return {
        total: count(data.total),
        rows: data.rows.map((item): OidcLogoutDelivery => {
          const row = record(item);
          return {
            logoutOutboxId: text(row.logoutOutboxId),
            clientId: text(row.clientId),
            status: text(row.status),
            attempts: count(row.attempts),
            nextAttemptAt: row.nextAttemptAt == null ? '' : text(row.nextAttemptAt),
            lastError: row.lastError == null ? '' : text(row.lastError),
            createTime: text(row.createTime)
          };
        })
      };
    },
    async retry(id: string, signal?: AbortSignal) {
      await post(`/oidc/admin/logout-deliveries/${encodeURIComponent(id)}/retry`, undefined, signal);
    }
  };
}
