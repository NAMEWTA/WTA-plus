import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { projectOperationLogTransport, type OperationLogTransport } from './transport';

export * from './transport';
import type {
  ApiResponse,
  CacheVO,
  Identifier,
  IdentifierList,
  LoginInfoQuery,
  LoginInfoVO,
  OnlineQuery,
  OnlineVO,
  OperLogQuery,
  OperLogVO,
  PageResult
} from './types';

export * from './types';

export type ExternalMonitorTarget = 'monitor-admin' | 'snail-job' | 'snail-ai' | 'nacos';
export interface NavigationIntent {
  readonly target: ExternalMonitorTarget | 'notify-attachment';
  readonly url: string;
  readonly mode: 'embed' | 'download';
  readonly downloadName?: string;
}
export type MonitorSecurityCode = 'permission-denied' | 'unsafe-url' | 'missing-url';
export class MonitorSecurityError extends Error {
  constructor(
    readonly code: MonitorSecurityCode,
    message: string
  ) {
    super(message);
    this.name = 'MonitorSecurityError';
  }
}

const targetPermissions: Readonly<Record<ExternalMonitorTarget, string>> = Object.freeze({
  'monitor-admin': 'monitor:admin:list',
  'snail-job': 'monitor:snailjob:list',
  'snail-ai': 'monitor:snailai:list',
  nacos: 'system:nacos:console'
});
const segment = (value: IdentifierList) =>
  (Array.isArray(value) ? value : [value]).map(item => encodeURIComponent(String(item))).join(',');
const relativeUrlBase = 'https://monitor.invalid';
const invalidPercentEncoding = /%(?![\dA-Fa-f]{2})/;
const unsafeWhitespace = /\s/u;
const safeUrl = (rawUrl: string | undefined) => {
  if (!rawUrl) throw new MonitorSecurityError('missing-url', '未配置运维入口地址');
  const hasControlCharacter = [...rawUrl].some(
    character => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127
  );
  if (
    rawUrl !== rawUrl.trim() ||
    unsafeWhitespace.test(rawUrl) ||
    rawUrl.includes('\\') ||
    hasControlCharacter ||
    invalidPercentEncoding.test(rawUrl)
  )
    throw new MonitorSecurityError('unsafe-url', '运维入口地址不安全');
  try {
    if (rawUrl.startsWith('/')) {
      if (rawUrl.startsWith('//')) throw new MonitorSecurityError('unsafe-url', '运维入口地址不安全');
      const parsed = new URL(rawUrl, relativeUrlBase);
      if (parsed.origin !== relativeUrlBase) throw new MonitorSecurityError('unsafe-url', '运维入口地址不安全');
      return `${parsed.pathname}${parsed.search}${parsed.hash}`;
    }
    const parsed = new URL(rawUrl);
    if (!['http:', 'https:'].includes(parsed.protocol) || !parsed.hostname || parsed.username || parsed.password) {
      throw new MonitorSecurityError('unsafe-url', '运维入口地址不安全');
    }
    return parsed.href;
  } catch (error) {
    if (error instanceof MonitorSecurityError) throw error;
    throw new MonitorSecurityError('unsafe-url', '运维入口地址不安全');
  }
};

export interface MonitorService {
  readonly cache: { get(): Promise<ApiResponse<CacheVO>> };
  readonly loginInfo: {
    list(query: LoginInfoQuery): Promise<ApiResponse<PageResult<LoginInfoVO>>>;
    delete(ids: IdentifierList): Promise<ApiResponse>;
    unlock(userNames: string | readonly string[]): Promise<ApiResponse>;
    clean(): Promise<ApiResponse>;
  };
  readonly online: {
    list(query: OnlineQuery): Promise<ApiResponse<PageResult<OnlineVO>>>;
    forceLogout(tokenId: string): Promise<ApiResponse>;
    current(): Promise<ApiResponse<PageResult<OnlineVO>>>;
    removeCurrent(tokenId: string): Promise<ApiResponse>;
  };
  readonly operationLogs: {
    list(query: OperLogQuery): Promise<ApiResponse<PageResult<OperLogVO>>>;
    delete(ids: IdentifierList): Promise<ApiResponse>;
    clean(): Promise<ApiResponse>;
  };
  externalIntent(target: ExternalMonitorTarget, rawUrl: string | undefined, allowed: boolean): NavigationIntent;
  attachmentIntent(rawUrl: string | undefined, allowed: boolean, downloadName?: string): NavigationIntent;
}

export function createMonitorService(http: HttpClient): MonitorService {
  const request = <T = unknown>(config: HttpRequest) => http.request<ApiResponse<T>>(config);
  const requirePermission = (allowed: boolean) => {
    if (!allowed) throw new MonitorSecurityError('permission-denied', '无权访问运维入口');
  };
  return Object.freeze({
    cache: Object.freeze({ get: () => request<CacheVO>({ url: '/monitor/cache', method: 'get' }) }),
    loginInfo: Object.freeze({
      list: (params: LoginInfoQuery) =>
        request<PageResult<LoginInfoVO>>({ url: '/monitor/loginInfo/list', method: 'get', params }),
      delete: (ids: IdentifierList) => request({ url: '/monitor/loginInfo/' + segment(ids), method: 'delete' }),
      unlock: (names: string | readonly string[]) =>
        request({ url: '/monitor/loginInfo/unlock/' + segment(names), method: 'get' }),
      clean: () => request({ url: '/monitor/loginInfo/clean', method: 'delete' })
    }),
    online: Object.freeze({
      list: (params: OnlineQuery) =>
        request<PageResult<OnlineVO>>({ url: '/monitor/online/list', method: 'get', params }),
      forceLogout: (tokenId: string) => request({ url: '/monitor/online/' + segment(tokenId), method: 'delete' }),
      current: () => request<PageResult<OnlineVO>>({ url: '/monitor/online', method: 'get' }),
      removeCurrent: (tokenId: string) =>
        request({ url: '/monitor/online/myself/' + segment(tokenId), method: 'delete' })
    }),
    operationLogs: Object.freeze({
      list: async (params: OperLogQuery) => {
        const response = await request<PageResult<OperationLogTransport>>({
          url: '/monitor/operlog/list',
          method: 'get',
          params
        });
        const { data, ...metadata } = response;
        return {
          ...metadata,
          data: {
            rows: Array.isArray(data?.rows) ? data.rows.map(projectOperationLogTransport) : [],
            total: typeof data?.total === 'number' ? data.total : 0
          }
        };
      },
      delete: (ids: IdentifierList) => request({ url: '/monitor/operlog/' + segment(ids), method: 'delete' }),
      clean: () => request({ url: '/monitor/operlog/clean', method: 'delete' })
    }),
    externalIntent(target: ExternalMonitorTarget, rawUrl: string | undefined, allowed: boolean) {
      requirePermission(allowed);
      return Object.freeze({ target, url: safeUrl(rawUrl), mode: 'embed' as const });
    },
    attachmentIntent(rawUrl: string | undefined, allowed: boolean, downloadName?: string) {
      requirePermission(allowed);
      return Object.freeze({
        target: 'notify-attachment' as const,
        url: safeUrl(rawUrl),
        mode: 'download' as const,
        downloadName
      });
    }
  });
}

export const monitorPermissions = Object.freeze({ ...targetPermissions });
