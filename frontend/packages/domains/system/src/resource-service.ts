import type { OpenApiSchema } from '@namewta/api-contracts';
import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type { ApiResponse, PageResult } from './types';
import type { OssDiagnosticFact, OssStorageDiagnostic } from './oss-config/types';
import type {
  ConfigForm,
  ConfigQuery,
  ConfigVO,
  DictDataForm,
  DictDataQuery,
  DictDataVO,
  DictTypeForm,
  DictTypeQuery,
  DictTypeVO,
  OssCompletedPart,
  OssConfigAccessPolicy,
  OssConfigForm,
  OssConfigQuery,
  OssConfigVO,
  OssDownloadUrl,
  OssQuery,
  OssSignedPart,
  OssUploadInitRequest,
  OssUploadInitResponse,
  OssUploadResumeResponse,
  OssVO,
  ResourceIdentifier,
  ResourceIdentifierList,
  SocialAuthVO
} from './resource-types';

export * from './resource-types';

const segment = (value: ResourceIdentifierList) =>
  (Array.isArray(value) ? value : [value]).map(item => encodeURIComponent(String(item))).join(',');

export class ResourceSecurityError extends Error {
  readonly code = 'unsafe-resource-url';
  constructor() {
    super('资源地址不可用');
    this.name = 'ResourceSecurityError';
  }
}

export class ResourceContractError extends Error {
  readonly code = 'invalid-resource-contract';
  constructor() {
    super('资源响应不可用');
    this.name = 'ResourceContractError';
  }
}

function requireSafeUrl(value: string) {
  try {
    const url = new URL(value);
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) throw new ResourceSecurityError();
    return value;
  } catch (error) {
    if (error instanceof ResourceSecurityError) throw error;
    throw new ResourceSecurityError();
  }
}

function validatePresigned<T extends { url: string }>(request: T): T {
  requireSafeUrl(request.url);
  return request;
}

type OssAccessUrlTransport = OpenApiSchema<'OssAccessUrl'>;
type OssConfigTransport = OpenApiSchema<'SysOssConfigVo'>;
type PhysicalOssAccessPolicy = '0' | '2';

function projectAccessUrl(value: OssAccessUrlTransport): OssDownloadUrl {
  const accessType = value?.accessType;
  const expiresAt = (value as { expiresAt?: string | null } | undefined)?.expiresAt ?? null;
  if (accessType !== 'PUBLIC' && accessType !== 'PRIVATE') throw new ResourceContractError();
  if (typeof value.url !== 'string' || typeof value.fileName !== 'string') throw new ResourceContractError();
  if ((accessType === 'PUBLIC' && expiresAt !== null) || (accessType === 'PRIVATE' && !expiresAt)) {
    throw new ResourceContractError();
  }
  requireSafeUrl(value.url);
  return Object.freeze({ accessType, url: value.url, expiresAt, fileName: value.fileName });
}

function projectAccessPolicy(value: unknown): OssConfigAccessPolicy {
  if (value === '0') return 'PRIVATE';
  if (value === '2') return 'PUBLIC_READ';
  throw new ResourceContractError();
}

function encodeAccessPolicy(value: OssConfigAccessPolicy): PhysicalOssAccessPolicy {
  if (value === 'PRIVATE') return '0';
  if (value === 'PUBLIC_READ') return '2';
  throw new ResourceContractError();
}

function projectOssConfig(value: OssConfigTransport): OssConfigVO {
  return { ...(value as OssConfigVO), accessPolicy: projectAccessPolicy(value.accessPolicy) };
}

function encodeOssConfig(value: OssConfigForm) {
  return { ...value, accessPolicy: encodeAccessPolicy(value.accessPolicy) };
}

const diagnosticStatus = ['SERVING', 'NOT_SERVING'] as const;
const diagnosticReason = [
  'READY', 'CONFIG_MISSING', 'INVALID_ACCESS_POLICY', 'DOMAIN_REQUIRED', 'DIAGNOSTIC_OBJECT_MISSING',
  'DIAGNOSTIC_CONFIG_INVALID', 'DIAGNOSTIC_UNVERIFIED', 'PROVIDER_MISMATCH', 'DISCOVERY_FAILED', 'STALE'
] as const;
const diagnosticSubject = [
  'POLICY_READ', 'POLICY_WRITE', 'ACL_LIST', 'ACL_WRITE_RISK', 'OBJECT_HEAD', 'OBJECT_GET'
] as const;
const diagnosticObservation = ['ALLOWED', 'DENIED', 'UNKNOWN'] as const;
const diagnosticSource = ['BUCKET_POLICY', 'BUCKET_ACL', 'ANONYMOUS_HEAD', 'ANONYMOUS_GET'] as const;
const diagnosticScope = ['BUCKET', 'OBJECT'] as const;
const diagnosticBasis = [
  'POLICY_ALLOW', 'POLICY_DENY', 'NO_SUCH_POLICY', 'POLICY_UNREADABLE', 'COMPLEX_POLICY',
  'INVALID_POLICY', 'ACL_GRANT', 'ACL_NO_GRANT', 'ACL_UNREADABLE', 'HTTP_SUCCESS', 'HTTP_DENIED',
  'HTTP_NOT_FOUND', 'REDIRECT', 'HTTP_ERROR', 'TIMEOUT', 'NETWORK_ERROR', 'INTERRUPTED',
  'NOT_EVALUATED', 'UNSUPPORTED'
] as const;

function diagnosticEnum<T extends string>(value: unknown, allowed: readonly T[]): T {
  if (typeof value !== 'string' || !allowed.includes(value as T)) throw new ResourceContractError();
  return value as T;
}

function diagnosticTime(value: unknown): string {
  if (typeof value !== 'string' || !Number.isFinite(Date.parse(value))) throw new ResourceContractError();
  return value;
}

function diagnosticRecord(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) throw new ResourceContractError();
  return value as Record<string, unknown>;
}

function projectDiagnostic(value: unknown): OssStorageDiagnostic {
  const row = diagnosticRecord(value);
  if (!Array.isArray(row.facts)) throw new ResourceContractError();
  const facts: OssDiagnosticFact[] = row.facts.map((item: unknown) => {
    const fact = diagnosticRecord(item);
    return Object.freeze({
      subject: diagnosticEnum(fact.subject, diagnosticSubject),
      observation: diagnosticEnum(fact.observation, diagnosticObservation),
      source: diagnosticEnum(fact.source, diagnosticSource),
      scope: diagnosticEnum(fact.scope, diagnosticScope),
      basis: diagnosticEnum(fact.basis, diagnosticBasis),
      observedAt: diagnosticTime(fact.observedAt)
    });
  });
  return Object.freeze({
    status: diagnosticEnum(row.status, diagnosticStatus),
    reason: diagnosticEnum(row.reason, diagnosticReason),
    checkedAt: diagnosticTime(row.checkedAt),
    facts: Object.freeze(facts)
  });
}

export interface SystemResourceService {
  dictData: ReturnType<typeof createDictDataService>;
  dictTypes: ReturnType<typeof createDictTypeService>;
  configs: ReturnType<typeof createConfigService>;
  oss: ReturnType<typeof createOssService>;
  ossConfigs: ReturnType<typeof createOssConfigService>;
  social: {
    list(): Promise<ApiResponse<SocialAuthVO[]>>;
  };
}

type Request = <T = unknown>(config: HttpRequest) => Promise<ApiResponse<T>>;

function createDictDataService(request: Request) {
  return Object.freeze({
    byType: (type: string) => request<DictDataVO[]>({ url: '/system/dict/data/type/' + segment(type), method: 'get' }),
    list: (params: DictDataQuery) =>
      request<PageResult<DictDataVO>>({ url: '/system/dict/data/list', method: 'get', params }),
    get: (id: ResourceIdentifier) => request<DictDataVO>({ url: '/system/dict/data/' + segment(id), method: 'get' }),
    add: (data: DictDataForm) => request({ url: '/system/dict/data', method: 'post', data }),
    update: (data: DictDataForm) => request({ url: '/system/dict/data/update', method: 'post', data }),
    delete: (ids: ResourceIdentifierList) => request({ url: '/system/dict/data/' + segment(ids), method: 'post' })
  });
}

function createDictTypeService(request: Request) {
  return Object.freeze({
    list: (params: DictTypeQuery) =>
      request<PageResult<DictTypeVO>>({ url: '/system/dict/type/list', method: 'get', params }),
    get: (id: ResourceIdentifier) => request<DictTypeVO>({ url: '/system/dict/type/' + segment(id), method: 'get' }),
    add: (data: DictTypeForm) => request({ url: '/system/dict/type', method: 'post', data }),
    update: (data: DictTypeForm) => request({ url: '/system/dict/type/update', method: 'post', data }),
    delete: (ids: ResourceIdentifierList) => request({ url: '/system/dict/type/' + segment(ids), method: 'post' }),
    refreshCache: () => request({ url: '/system/dict/type/refreshCache', method: 'post' }),
    options: () => request<DictTypeVO[]>({ url: '/system/dict/type/optionselect', method: 'get' })
  });
}

function createConfigService(request: Request) {
  return Object.freeze({
    list: (params: ConfigQuery) => request<PageResult<ConfigVO>>({ url: '/system/config/list', method: 'get', params }),
    get: (id: ResourceIdentifier) => request<ConfigVO>({ url: '/system/config/' + segment(id), method: 'get' }),
    byKey: (key: string) => request<string>({ url: '/system/config/configKey/' + segment(key), method: 'get' }),
    add: (data: ConfigForm) => request({ url: '/system/config', method: 'post', data }),
    update: (data: ConfigForm) => request({ url: '/system/config/update', method: 'post', data }),
    updateByKey: (key: string, value: unknown) =>
      request({ url: '/system/config/updateByKey', method: 'post', data: { configKey: key, configValue: value } }),
    delete: (ids: ResourceIdentifierList) => request({ url: '/system/config/' + segment(ids), method: 'post' }),
    refreshCache: () => request({ url: '/system/config/refreshCache', method: 'post' })
  });
}

function createOssService(request: Request) {
  const downloadUrl = async (id: ResourceIdentifier) => {
    const response = await request<OssAccessUrlTransport>({
      url: `/resource/oss/${segment(id)}/download-url`,
      method: 'get'
    });
    return { ...response, data: projectAccessUrl(response.data) };
  };
  return Object.freeze({
    list: async (params: OssQuery) => {
      const response = await request<PageResult<OssVO>>({ url: '/resource/oss/list', method: 'get', params });
      return { ...response, data: { ...response.data, rows: response.data.rows.map(item => ({ ...item, url: '' })) } };
    },
    listByIds: async (ids: ResourceIdentifierList) => {
      const response = await request<OssVO[]>({ url: '/resource/oss/listByIds/' + segment(ids), method: 'get' });
      const data = await Promise.all(
        response.data.map(async item => {
          const resolved = await downloadUrl(item.ossId);
          return { ...item, url: resolved.data.url };
        })
      );
      return { ...response, data };
    },
    initUpload: async (data: OssUploadInitRequest) => {
      const response = await request<OssUploadInitResponse>({ url: '/resource/oss/uploads', method: 'post', data });
      if (response.data.presignedRequest) validatePresigned(response.data.presignedRequest);
      return response;
    },
    signParts: async (token: string, partNumbers: number[]) => {
      const response = await request<{ parts: OssSignedPart[] }>({
        url: `/resource/oss/uploads/${segment(token)}/parts/sign`,
        method: 'post',
        data: { partNumbers }
      });
      response.data.parts.forEach(validatePresigned);
      return response;
    },
    resumeUpload: async (token: string, fingerprint: string) => {
      const response = await request<OssUploadResumeResponse>({
        url: `/resource/oss/uploads/${segment(token)}/parts`,
        method: 'get',
        params: { fingerprint }
      });
      if (response.data.presignedRequest) validatePresigned(response.data.presignedRequest);
      return response;
    },
    completeUpload: (token: string, parts: OssCompletedPart[] = []) =>
      request<string>({ url: `/resource/oss/uploads/${segment(token)}/complete`, method: 'post', data: { parts } }),
    abortUpload: (token: string) => request({ url: `/resource/oss/uploads/${segment(token)}`, method: 'post' }),
    downloadUrl,
    delete: (ids: ResourceIdentifierList) => request({ url: '/resource/oss/' + segment(ids), method: 'post' }),
    restore: (ids: ResourceIdentifierList) =>
      request({ url: '/resource/oss/' + segment(ids) + '/restore', method: 'post' }),
    publish: (id: ResourceIdentifier, targetConfigKey: string) =>
      request<string>({ url: `/resource/oss/${segment(id)}/publish`, method: 'post', data: { targetConfigKey } }),
    unpublish: (id: ResourceIdentifier) =>
      request({ url: `/resource/oss/${segment(id)}/unpublish`, method: 'post' })
  });
}

function createOssConfigService(request: Request) {
  return Object.freeze({
    diagnose: async (id: ResourceIdentifier, signal?: AbortSignal) => {
      const response = await request<unknown>({
        url: '/resource/oss/config/diagnose/' + segment(id),
        method: 'post',
        ...(signal ? { signal } : {})
      });
      return { ...response, data: projectDiagnostic(response.data) };
    },
    list: async (params: OssConfigQuery) => {
      const response = await request<PageResult<OssConfigTransport>>({
        url: '/resource/oss/config/list',
        method: 'get',
        params
      });
      return {
        ...response,
        data: { ...response.data, rows: response.data.rows.map(projectOssConfig) }
      };
    },
    get: async (id: ResourceIdentifier) => {
      const response = await request<OssConfigTransport>({
        url: '/resource/oss/config/' + segment(id),
        method: 'get'
      });
      return { ...response, data: projectOssConfig(response.data) };
    },
    add: (data: OssConfigForm) => request({ url: '/resource/oss/config', method: 'post', data: encodeOssConfig(data) }),
    update: (data: OssConfigForm) =>
      request({ url: '/resource/oss/config/edit', method: 'post', data: encodeOssConfig(data) }),
    delete: (ids: ResourceIdentifierList) =>
      request({ url: '/resource/oss/config/remove/' + segment(ids), method: 'post' }),
    changeStatus: (ossConfigId: ResourceIdentifier, status: string, configKey: string) =>
      request({ url: '/resource/oss/config/changeStatus', method: 'post', data: { ossConfigId, status, configKey } })
  });
}

export function createSystemResourceService(http: HttpClient): SystemResourceService {
  const request: Request = config => http.request(config);
  return Object.freeze({
    dictData: createDictDataService(request),
    dictTypes: createDictTypeService(request),
    configs: createConfigService(request),
    oss: createOssService(request),
    ossConfigs: createOssConfigService(request),
    social: Object.freeze({
      list: () => request<SocialAuthVO[]>({ url: '/system/social/list', method: 'get' })
    })
  });
}
