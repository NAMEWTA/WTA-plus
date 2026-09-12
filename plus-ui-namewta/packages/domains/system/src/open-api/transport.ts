import type {
  OpenApiAccessRule,
  OpenApiCatalogGroup,
  OpenApiCatalogItem,
  OpenApiCredentialIssued,
  OpenApiCredentialState,
  OpenApiCredentialSummary,
  OpenApiCredentialUserSummary,
  OpenApiHttpMethod,
  OpenApiIdentifier,
  OpenApiParameter,
  OpenApiPermissionRule,
  OpenApiResponse,
  OpenApiRoleRule
} from './types';

type UnknownRecord = Record<string, unknown>;

const unavailable = (): never => {
  throw new Error('OpenAPI 响应不可用');
};

function record(value: unknown): UnknownRecord {
  if (!value || typeof value !== 'object' || Array.isArray(value)) unavailable();
  return value as UnknownRecord;
}

function requiredString(value: unknown): string {
  if (typeof value === 'string' && value.trim() !== '') return value;
  return unavailable();
}

function nullableString(value: unknown): string | null {
  if (value === null) return null;
  return requiredString(value);
}

function identifier(value: unknown): OpenApiIdentifier {
  if (typeof value === 'number' && Number.isSafeInteger(value) && value > 0) return value;
  if (typeof value === 'string' && /^[1-9]\d*$/.test(value)) return value;
  return unavailable();
}

function stringList(value: unknown, allowEmpty: boolean): readonly string[] {
  if (Array.isArray(value) && (allowEmpty || value.length > 0)) return Object.freeze(value.map(requiredString));
  return unavailable();
}

function ruleMode(value: unknown): 'AND' | 'OR' {
  if (value === 'AND' || value === 'OR') return value;
  return unavailable();
}

function permissionRule(value: unknown): OpenApiPermissionRule {
  const source = record(value);
  return Object.freeze({
    values: stringList(source.values, false),
    mode: ruleMode(source.mode),
    orRoles: stringList(source.orRoles, true)
  });
}

function roleRule(value: unknown): OpenApiRoleRule {
  const source = record(value);
  return Object.freeze({ values: stringList(source.values, false), mode: ruleMode(source.mode) });
}

function accessRule(value: unknown): OpenApiAccessRule {
  const source = record(value);
  const permissions = source.permissions;
  const roles = source.roles;
  if (!Array.isArray(permissions) || !Array.isArray(roles)) return unavailable();
  return Object.freeze({
    permissions: Object.freeze(permissions.map(permissionRule)),
    roles: Object.freeze(roles.map(roleRule))
  });
}

function parameter(value: unknown): OpenApiParameter {
  const source = record(value);
  const location = source.location;
  const required = source.required;
  if (location !== 'header' && location !== 'path' && location !== 'query') return unavailable();
  if (typeof required !== 'boolean') return unavailable();
  return Object.freeze({
    name: requiredString(source.name),
    location,
    required,
    schema: requiredString(source.schema)
  });
}

function httpMethod(value: unknown): OpenApiHttpMethod {
  switch (value) {
    case 'DELETE':
    case 'GET':
    case 'HEAD':
    case 'OPTIONS':
    case 'PATCH':
    case 'POST':
    case 'PUT':
    case 'TRACE':
      return value;
    default:
      return unavailable();
  }
}

function credentialStatus(value: unknown): '0' | '1' {
  if (value === '0' || value === '1') return value;
  return unavailable();
}

function projectCredential(value: unknown, includeSecret: false): OpenApiCredentialSummary;
function projectCredential(value: unknown, includeSecret: true): OpenApiCredentialIssued;
function projectCredential(value: unknown, includeSecret: boolean): OpenApiCredentialSummary | OpenApiCredentialIssued {
  const source = record(value);
  const secretKeys = Object.keys(source).filter(key => key.toLowerCase().includes('secret'));
  if (includeSecret ? secretKeys.some(key => key !== 'appSecret') : secretKeys.length > 0) unavailable();
  const common: OpenApiCredentialSummary = Object.freeze({
    credentialId: identifier(source.credentialId),
    ownerUserId: identifier(source.ownerUserId),
    appKey: requiredString(source.appKey),
    appName: requiredString(source.appName),
    status: credentialStatus(source.status),
    expiresAt: nullableString(source.expiresAt),
    remark: source.remark === null ? null : requiredString(source.remark),
    createTime: nullableString(source.createTime),
    updateTime: nullableString(source.updateTime)
  });
  if (!includeSecret) return common;
  return Object.freeze({ ...common, appSecret: requiredString(source.appSecret) });
}

export function projectOpenApiCredentialSummary(value: unknown): OpenApiCredentialSummary {
  return projectCredential(value, false);
}

export function projectOpenApiCredentialIssued(value: unknown): OpenApiCredentialIssued {
  return projectCredential(value, true);
}

export function projectOpenApiCredentialUserSummary(value: unknown): OpenApiCredentialUserSummary {
  const source = record(value);
  return Object.freeze({
    userId: identifier(source.userId),
    userName: requiredString(source.userName),
    nickName: requiredString(source.nickName),
    credential: source.credential === null ? null : projectOpenApiCredentialSummary(source.credential)
  });
}

export function projectOpenApiCatalogItem(value: unknown): OpenApiCatalogItem {
  const source = record(value);
  const parameters = source.parameters;
  if (!Array.isArray(parameters)) return unavailable();
  return Object.freeze({
    interfaceId: requiredString(source.interfaceId),
    summary: requiredString(source.summary),
    method: httpMethod(source.method),
    path: requiredString(source.path),
    accessRule: accessRule(source.accessRule),
    parameters: Object.freeze(parameters.map(parameter)),
    requestSchema: source.requestSchema === null ? null : requiredString(source.requestSchema),
    responseSchema: requiredString(source.responseSchema),
    curlExample: requiredString(source.curlExample),
    javaExample: requiredString(source.javaExample)
  });
}

export function projectOpenApiResponse<T>(value: unknown, projectData: (data: unknown) => T): OpenApiResponse<T> {
  const source = record(value);
  if (!Number.isInteger(source.code)) unavailable();
  if (source.msg !== undefined && source.msg !== null && typeof source.msg !== 'string') unavailable();
  const response: { code: number; data: T; msg?: string } = {
    code: source.code as number,
    data: projectData(source.data)
  };
  if (typeof source.msg === 'string') response.msg = source.msg;
  return Object.freeze(response);
}

export function groupOpenApiCatalog(items: readonly OpenApiCatalogItem[]): readonly OpenApiCatalogGroup[] {
  const grouped = new Map<string, OpenApiCatalogItem[]>();
  for (const item of items) {
    const key = item.path.split('/').find(Boolean) ?? 'root';
    const group = grouped.get(key);
    if (group) group.push(item);
    else grouped.set(key, [item]);
  }
  return Object.freeze(
    [...grouped].map(([key, groupItems]) => Object.freeze({ key, label: key, items: Object.freeze([...groupItems]) }))
  );
}

export function resolveOpenApiCredentialState(
  credential: OpenApiCredentialSummary,
  now: number = Date.now()
): OpenApiCredentialState {
  if (credential.expiresAt !== null && Date.parse(credential.expiresAt) <= now) return 'expired';
  return credential.status === '0' ? 'enabled' : 'disabled';
}

export const projectOpenApiCatalog = (value: unknown): readonly OpenApiCatalogItem[] => {
  if (Array.isArray(value)) return Object.freeze(value.map(projectOpenApiCatalogItem));
  return unavailable();
};

export const projectOpenApiUsers = (value: unknown): readonly OpenApiCredentialUserSummary[] => {
  if (Array.isArray(value)) return Object.freeze(value.map(projectOpenApiCredentialUserSummary));
  return unavailable();
};

export const projectOpenApiEmpty = (value: unknown): null => {
  if (value !== null && value !== undefined) unavailable();
  return null;
};
