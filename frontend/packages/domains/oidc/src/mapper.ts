import type { OpenApiSchema } from '@namewta/api-contracts';
import type { OidcApplication, OidcApplicationPage, OidcField, OidcProvider, OidcSecretDelivery } from './types';

function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('OIDC 响应格式无效');
  return value as Record<string, unknown>;
}
function string(value: unknown): string {
  if (typeof value !== 'string') throw new Error('OIDC 文本字段无效');
  return value;
}
function boolean(value: unknown): boolean {
  if (typeof value !== 'boolean') throw new Error('OIDC 状态字段无效');
  return value;
}
function count(value: unknown): number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) throw new Error('OIDC 数值字段无效');
  return value;
}
function array(value: unknown): unknown[] {
  if (!Array.isArray(value)) throw new Error('OIDC 列表字段无效');
  return value;
}
function strings(value: unknown): string[] {
  return array(value).map(string);
}
export function responseData(value: unknown): unknown {
  const response = record(value);
  if (response.code !== undefined && response.code !== 200) throw new Error('OIDC 操作未完成');
  return response.data;
}
export function mapApplication(value: unknown): OidcApplication {
  const row = record(value);
  const id = typeof row.applicationId === 'number' ? String(count(row.applicationId)) : string(row.applicationId);
  if (!/^\d+$/.test(id) || /^0+$/.test(id)) throw new Error('OIDC 应用标识无效');
  const method = row.clientAuthenticationMethod;
  if (method !== 'client_secret_basic' && method !== 'client_secret_post') throw new Error('OIDC 客户端认证方式无效');
  const application: OidcApplication = {
    applicationId: id,
    name: string(row.name),
    clientId: string(row.clientId),
    redirectUris: strings(row.redirectUris),
    postLogoutRedirectUris: strings(row.postLogoutRedirectUris),
    ...(row.backchannelLogoutUri != null ? { backchannelLogoutUri: string(row.backchannelLogoutUri) } : {}),
    ...(row.backchannelLogoutSessionRequired != null
      ? { backchannelLogoutSessionRequired: boolean(row.backchannelLogoutSessionRequired) }
      : {}),
    allowedFields: strings(row.allowedFields),
    clientAuthenticationMethod: method,
    pkceRequired: boolean(row.pkceRequired),
    enabled: boolean(row.enabled),
    version: count(row.version),
    createTime: row.createTime == null ? undefined : string(row.createTime)
  };
  return application satisfies OpenApiSchema<'OidcApplicationVo'>;
}
export function mapPage(value: unknown): OidcApplicationPage {
  const response = record(value);
  if (response.code !== undefined && response.code !== 200) throw new Error('OIDC 查询未完成');
  return {
    rows: array(response.rows).map(mapApplication),
    total: count(response.total)
  } satisfies OpenApiSchema<'PageResultOidcApplicationVo'>;
}
export function mapFields(value: unknown): OidcField[] {
  return array(value).map(item => {
    const field = record(item);
    return {
      key: string(field.key),
      label: string(field.label),
      scope: string(field.scope),
      sensitive: boolean(field.sensitive),
      defaultEnabled: boolean(field.defaultEnabled),
      group: string(field.group)
    } satisfies OpenApiSchema<'OidcFieldVo'>;
  });
}
export function mapProvider(value: unknown): OidcProvider {
  const provider = record(value);
  return {
    issuer: string(provider.issuer),
    discoveryUrl: string(provider.discoveryUrl),
    ready: boolean(provider.ready),
    enabled: boolean(provider.enabled),
    scopes: strings(provider.scopes)
  } satisfies OpenApiSchema<'OidcProviderVo'>;
}
export function mapDelivery(value: unknown): OidcSecretDelivery {
  const delivery = record(value);
  return {
    application: mapApplication(delivery.application),
    clientSecret: string(delivery.clientSecret)
  } satisfies OpenApiSchema<'OidcApplicationSecretVo'>;
}
