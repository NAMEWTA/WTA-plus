import type { HttpClient } from '@namewta/platform-contracts';
import { readLegacyAuthImportItems, type LegacyAuthImportInput } from './legacy';

export interface AuthProviderConfig {
  id?: string;
  providerKey: string;
  name: string;
  icon: string;
  protocol: string;
  issuer: string;
  enabled: boolean;
  version: number;
  options: Record<string, string>;
}
export interface AuthRegistrationConfig {
  id?: string;
  providerId: string;
  businessClientId: string;
  externalClientId: string;
  clientSecret?: string;
  secretConfigured?: boolean;
  redirectUri: string;
  postLogoutRedirectUri: string;
  scopes: string[];
  firstLoginPolicy: 'BIND_ONLY' | 'AUTO_REGISTER';
  enabled: boolean;
  version: number;
  options: Record<string, string>;
}
export interface AuthConfigQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  providerKey?: string;
  providerId?: string;
  businessClientId?: string;
  enabled?: boolean;
}
function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('登录配置响应格式无效');
  return value as Record<string, unknown>;
}
function string(value: unknown): string {
  if (typeof value !== 'string') throw new Error('配置字段格式无效');
  return value;
}
function options(value: unknown): Record<string, string> {
  if (value == null) return {};
  return Object.fromEntries(Object.entries(record(value)).map(([key, item]) => [key, string(item)]));
}
function common(row: Record<string, unknown>) {
  if (typeof row.enabled !== 'boolean' || typeof row.version !== 'number') throw new Error('配置状态格式无效');
  return { id: string(row.id), enabled: row.enabled, version: row.version, options: options(row.options) };
}
export function mapAuthProvider(value: unknown): AuthProviderConfig {
  const row = record(value);
  return {
    ...common(row),
    providerKey: string(row.providerKey),
    name: string(row.name),
    icon: row.icon == null ? '' : string(row.icon),
    protocol: string(row.protocol),
    issuer: row.issuer == null ? '' : string(row.issuer)
  };
}
export function mapAuthRegistration(value: unknown): AuthRegistrationConfig {
  const row = record(value);
  if (!Array.isArray(row.scopes) || !['BIND_ONLY', 'AUTO_REGISTER'].includes(String(row.firstLoginPolicy)))
    throw new Error('接入策略格式无效');
  return {
    ...common(row),
    providerId: string(row.providerId),
    businessClientId: string(row.businessClientId),
    externalClientId: string(row.externalClientId),
    secretConfigured: row.secretConfigured === true,
    redirectUri: string(row.redirectUri),
    postLogoutRedirectUri: row.postLogoutRedirectUri == null ? '' : string(row.postLogoutRedirectUri),
    scopes: row.scopes.map(string),
    firstLoginPolicy: row.firstLoginPolicy as AuthRegistrationConfig['firstLoginPolicy']
  };
}
export function createAuthConfigurationService(http: HttpClient) {
  function resource<T extends { id?: string; version: number }>(
    base: string,
    map: (value: unknown) => T,
    encode: (value: T) => unknown
  ) {
    const request = async (url: string, method: 'get' | 'post', data?: unknown, signal?: AbortSignal) =>
      record(await http.request<unknown>({ url, method, data, signal })).data;
    return {
      async list(params: AuthConfigQuery, signal?: AbortSignal) {
        const data = record(
          record(await http.request<unknown>({ url: `${base}/list`, method: 'get', params, signal })).data
        );
        if (!Array.isArray(data.rows) || typeof data.total !== 'number') throw new Error('配置列表格式无效');
        return { rows: data.rows.map(map), total: data.total };
      },
      async get(id: string, signal?: AbortSignal) {
        return map(await request(`${base}/${encodeURIComponent(id)}`, 'get', undefined, signal));
      },
      async save(value: T, signal?: AbortSignal) {
        await request(`${base}/${value.id ? 'edit' : 'add'}`, 'post', encode(value), signal);
      },
      async remove(value: T, signal?: AbortSignal) {
        await request(`${base}/remove`, 'post', { id: value.id, version: value.version }, signal);
      },
      async refresh(signal?: AbortSignal) {
        await request(`${base}/refresh`, 'post', undefined, signal);
      }
    };
  }
  return {
    async importLegacy(input: LegacyAuthImportInput, signal?: AbortSignal) {
      const response = record(
        await http.request<unknown>({
          url: '/system/auth/registration/importLegacy',
          method: 'post',
          data: input,
          signal
        })
      );
      return readLegacyAuthImportItems(response.data);
    },
    providers: resource('/system/auth/provider', mapAuthProvider, value => ({ ...value })),
    registrations: resource('/system/auth/registration', mapAuthRegistration, value => {
      const { secretConfigured: _readOnly, clientSecret, ...data } = value;
      return { ...data, ...(clientSecret?.trim() ? { clientSecret: clientSecret.trim() } : {}) };
    })
  };
}
export type AuthConfigurationService = ReturnType<typeof createAuthConfigurationService>;
