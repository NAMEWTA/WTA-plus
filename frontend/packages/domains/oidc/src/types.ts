/** 管理端应用模型，与第一方 sys_client 的角色/登录域目录分离。 */
export interface OidcApplication {
  applicationId: string;
  name: string;
  clientId: string;
  redirectUris: string[];
  postLogoutRedirectUris: string[];
  allowedFields: string[];
  clientAuthenticationMethod: 'client_secret_basic' | 'client_secret_post';
  pkceRequired: boolean;
  enabled: boolean;
  version: number;
  createTime?: string;
}

export interface OidcApplicationInput {
  name: string;
  redirectUris: string[];
  postLogoutRedirectUris: string[];
  allowedFields: string[];
  clientAuthenticationMethod: OidcApplication['clientAuthenticationMethod'];
  pkceRequired: boolean;
}

export interface OidcField {
  key: string;
  label: string;
  scope: string;
  sensitive: boolean;
  defaultEnabled: boolean;
  group: string;
}

export interface OidcProvider {
  issuer: string;
  discoveryUrl: string;
  ready: boolean;
  enabled: boolean;
  scopes: string[];
}

/** 明文只存在于当前交付窗口；关闭窗口后丢弃。 */
export interface OidcSecretDelivery {
  application: OidcApplication;
  clientSecret: string;
}

export interface OidcApplicationPage {
  rows: OidcApplication[];
  total: number;
}
export interface OidcApplicationQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
}

/** 批量勾选不包含完整证件等敏感字段，敏感字段必须逐项点击。 */
export function selectOrdinaryFields(
  selected: readonly string[],
  fields: readonly OidcField[],
  checked: boolean
): string[] {
  const result = new Set(selected);
  for (const field of fields) {
    if (!checked) result.delete(field.key);
    else if (!field.sensitive) result.add(field.key);
  }
  return [...result];
}

export function applicationScopes(
  application: Pick<OidcApplication, 'allowedFields'>,
  fields: readonly OidcField[]
): string[] {
  const allowed = new Set(application.allowedFields);
  return [
    'openid',
    ...new Set(
      fields
        .filter(field => allowed.has(field.key))
        .map(field => field.scope)
        .filter(scope => scope !== 'openid')
    )
  ];
}

export function redirectLines(value: string): string[] {
  return value
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean);
}
