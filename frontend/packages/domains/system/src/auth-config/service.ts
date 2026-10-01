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
  providerName?: string;
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
export type OidcAuthenticationMethod = 'client_secret_basic' | 'client_secret_post';
export interface AuthProviderOption {
  id: string;
  providerKey: string;
  name: string;
  protocol: string;
  issuer: string;
  enabled: boolean;
  options: { authenticationMethod?: string };
}
export interface AuthClientOption {
  clientId: string;
  clientKey: string;
  userTypeName?: string;
  status: string;
  socialEnabled: boolean;
  registerEnabled: boolean;
  providerCount: number;
  enabledProviderCount: number;
  unavailableReason?: string;
}
export interface OidcMetadataDiagnostic {
  issuer: string;
  discoveryUrl: string;
  authorizationEndpoint: string;
  tokenEndpoint: string;
  jwksUri: string;
  userInfoEndpoint: string;
  endSessionEndpoint: string;
  authenticationMethods: string[];
  scopes: string[];
  pkceMethods: string[];
  responseTypes: string[];
  signingAlgorithms: string[];
  backchannelLogoutSupported: boolean;
  backchannelLogoutSessionSupported: boolean;
  checkedAt: string;
}
export interface AuthConnectionInfo {
  registrationId: string;
  providerName: string;
  providerKey: string;
  protocol: string;
  issuer: string;
  discoveryUrl: string;
  businessClientId: string;
  externalClientId: string;
  authenticationMethod: string;
  redirectUri: string;
  postLogoutRedirectUri: string;
  scopes: string[];
  backchannelLogoutUri: string;
  appPublicUrl: string;
  enabled: boolean;
  secretConfigured: boolean;
}
function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('登录配置响应格式无效');
  return value as Record<string, unknown>;
}
function string(value: unknown): string {
  if (typeof value !== 'string') throw new Error('配置字段格式无效');
  return value;
}
function optionalString(value: unknown): string {
  return value == null ? '' : string(value);
}
function boolean(value: unknown): boolean {
  if (typeof value !== 'boolean') throw new Error('配置状态格式无效');
  return value;
}
function strings(value: unknown): string[] {
  if (!Array.isArray(value)) throw new Error('配置列表字段格式无效');
  return value.map(string);
}
function count(value: unknown): number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) throw new Error('接入数量格式无效');
  return value;
}
export function mapAuthProviderOption(value: unknown): AuthProviderOption {
  const row = record(value);
  const publicOptions = row.options == null ? {} : record(row.options);
  return {
    id: string(row.id),
    providerKey: string(row.providerKey),
    name: string(row.name),
    protocol: string(row.protocol),
    issuer: optionalString(row.issuer),
    enabled: boolean(row.enabled),
    options:
      publicOptions.authenticationMethod == null
        ? {}
        : { authenticationMethod: string(publicOptions.authenticationMethod) }
  };
}
export function mapAuthClientOption(value: unknown): AuthClientOption {
  const row = record(value);
  return {
    clientId: string(row.clientId),
    clientKey: string(row.clientKey),
    userTypeName: optionalString(row.userTypeName),
    status: string(row.status),
    socialEnabled: boolean(row.socialEnabled),
    registerEnabled: boolean(row.registerEnabled),
    providerCount: count(row.providerCount),
    enabledProviderCount: count(row.enabledProviderCount),
    unavailableReason: optionalString(row.unavailableReason)
  };
}
export function mapOidcMetadataDiagnostic(value: unknown): OidcMetadataDiagnostic {
  const row = record(value);
  return {
    issuer: string(row.issuer),
    discoveryUrl: string(row.discoveryUrl),
    authorizationEndpoint: string(row.authorizationEndpoint),
    tokenEndpoint: string(row.tokenEndpoint),
    jwksUri: string(row.jwksUri),
    userInfoEndpoint: optionalString(row.userInfoEndpoint),
    endSessionEndpoint: optionalString(row.endSessionEndpoint),
    authenticationMethods: strings(row.authenticationMethods),
    scopes: strings(row.scopes),
    pkceMethods: strings(row.pkceMethods),
    responseTypes: strings(row.responseTypes),
    signingAlgorithms: strings(row.signingAlgorithms),
    backchannelLogoutSupported: boolean(row.backchannelLogoutSupported),
    backchannelLogoutSessionSupported: boolean(row.backchannelLogoutSessionSupported),
    checkedAt: string(row.checkedAt)
  };
}
export function mapAuthConnectionInfo(value: unknown): AuthConnectionInfo {
  const row = record(value);
  return {
    registrationId: string(row.registrationId),
    providerName: string(row.providerName),
    providerKey: string(row.providerKey),
    protocol: string(row.protocol),
    issuer: optionalString(row.issuer),
    discoveryUrl: optionalString(row.discoveryUrl),
    businessClientId: string(row.businessClientId),
    externalClientId: string(row.externalClientId),
    authenticationMethod: string(row.authenticationMethod),
    redirectUri: string(row.redirectUri),
    postLogoutRedirectUri: optionalString(row.postLogoutRedirectUri),
    scopes: strings(row.scopes),
    backchannelLogoutUri: optionalString(row.backchannelLogoutUri),
    appPublicUrl: optionalString(row.appPublicUrl),
    enabled: boolean(row.enabled),
    secretConfigured: boolean(row.secretConfigured)
  };
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
    providerName: optionalString(row.providerName),
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
        return string(await request(`${base}/${value.id ? 'edit' : 'add'}`, 'post', encode(value), signal));
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
    /** 由后端重新读取 Discovery；不复用登录流程的元数据缓存。 */
    async oidcMetadata(issuer: string, signal?: AbortSignal) {
      const response = record(
        await http.request<unknown>({
          url: '/system/auth/provider/oidc-metadata',
          method: 'get',
          params: { issuer },
          signal
        })
      );
      return mapOidcMetadataDiagnostic(response.data);
    },
    async clientOptions(query: { keyword?: string; clientIds?: string[] } = {}, signal?: AbortSignal) {
      const response = record(
        await http.request<unknown>({
          url: '/system/auth/registration/client-options',
          method: 'get',
          params: { keyword: query.keyword, clientIds: query.clientIds?.join(',') },
          signal
        })
      );
      if (!Array.isArray(response.data)) throw new Error('业务客户端列表格式无效');
      return response.data.map(mapAuthClientOption);
    },
    async providerOptions(query: { keyword?: string; selectedId?: string } = {}, signal?: AbortSignal) {
      const response = record(
        await http.request<unknown>({
          url: '/system/auth/registration/provider-options',
          method: 'get',
          params: query,
          signal
        })
      );
      if (!Array.isArray(response.data)) throw new Error('身份源选项格式无效');
      return response.data.map(mapAuthProviderOption);
    },
    async connectionInfo(id: string, signal?: AbortSignal) {
      const response = record(
        await http.request<unknown>({
          url: `/system/auth/registration/${encodeURIComponent(id)}/connection-info`,
          method: 'get',
          signal
        })
      );
      return mapAuthConnectionInfo(response.data);
    },
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
      const { secretConfigured: _readOnly, providerName: _providerName, clientSecret, ...data } = value;
      return { ...data, ...(clientSecret?.trim() ? { clientSecret: clientSecret.trim() } : {}) };
    })
  };
}
export type AuthConfigurationService = ReturnType<typeof createAuthConfigurationService>;
