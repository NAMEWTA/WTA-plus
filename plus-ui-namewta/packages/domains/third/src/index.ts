import type { DomainModule } from '@namewta/platform-app-runtime';
import type { ApiErrorInfo, HttpClient } from '@namewta/platform-contracts';

export interface ApiResponse<T = unknown> { code?: number; data?: T; msg?: string; error?: ApiErrorInfo }
export interface Provider {
  providerId: string | number; providerCode: string; providerName: string; baseUrl: string; status: string;
  timeoutConnectMs: number; timeoutReadMs: number; rateLimit: number; concurrencyLimit: number;
  sharedHeadersJson?: string; remark?: string; version?: number; createTime?: string; updateTime?: string;
}
export interface ProviderForm extends Partial<Provider> { providerCode: string; providerName: string; baseUrl: string }
export interface Endpoint {
  endpointId: string | number; providerId: string | number; providerCode: string; endpointCode: string; endpointName: string;
  httpMethod: string; relativePath: string; requestMode: string; responseMode: string; pathSchemaJson?: string;
  querySchemaJson?: string; headerSchemaJson?: string; bodySchemaJson?: string; responseSchemaJson?: string;
  overrideJson?: string; status: string; idempotent: boolean; rateLimit: number; concurrencyLimit: number;
  retryCount: number; sensitiveFieldsJson?: string; adapterCode?: string; version?: number;
}
export interface EndpointForm extends Partial<Endpoint> { providerId: string | number; providerCode: string; endpointCode: string; endpointName: string; httpMethod: string; relativePath: string; requestMode: string; responseMode: string }
export interface CredentialSummary {
  credentialId: string | number; providerCode: string; endpointCode?: string; scopeType: string; credentialType: string;
  kekVersion: string; expiresAt?: string; version: number; enabled: string;
}
export interface CredentialForm { credentialId?: string | number; providerCode: string; endpointCode?: string; credentialType: string; secretJson: string; enabled: boolean; expiresAt?: string }
export interface Invocation { invocationId: string | number; requestId: string; providerCode: string; endpointCode: string; attemptCount: number; logicalStatus: string; failureCategory?: string; httpStatus?: number; durationMs: number; sanitizedRequestJson?: string; sanitizedResponseJson?: string; createTime: string }
export interface Statistic { providerCode: string; endpointCode?: string; statDate: string; attemptCount: number; successCount: number; failureCount: number; timeoutCount: number; rejectedCount: number; quotaValue: number }

export interface ThirdService {
  listProviders(keyword?: string): Promise<ApiResponse<Provider[]>>; getProvider(id: string | number): Promise<ApiResponse<Provider>>;
  saveProvider(data: ProviderForm): Promise<ApiResponse>; changeProviderStatus(id: string | number, status: string): Promise<ApiResponse>; deleteProvider(id: string | number): Promise<ApiResponse>;
  listEndpoints(providerId?: string | number, keyword?: string): Promise<ApiResponse<Endpoint[]>>; getEndpoint(id: string | number): Promise<ApiResponse<Endpoint>>;
  saveEndpoint(data: EndpointForm): Promise<ApiResponse>; changeEndpointStatus(id: string | number, status: string): Promise<ApiResponse>; deleteEndpoint(id: string | number): Promise<ApiResponse>;
  listCredentials(providerCode: string, endpointCode?: string): Promise<ApiResponse<CredentialSummary[]>>; saveCredential(data: CredentialForm): Promise<ApiResponse>; deleteCredential(id: string | number): Promise<ApiResponse>;
  listInvocations(providerCode: string): Promise<ApiResponse<Invocation[]>>; listStatistics(providerCode: string): Promise<ApiResponse<Statistic[]>>;
}

export const thirdDomainModule: DomainModule = Object.freeze({ id: 'third', backendModules: ['wta-third'], capabilities: ['third-provider', 'third-endpoint', 'third-credential', 'third-observability'] });
const id = (value: string | number) => encodeURIComponent(String(value));

export function createThirdService(http: HttpClient): ThirdService {
  const call = <T>(config: Parameters<HttpClient['request']>[0]) => http.request<ApiResponse<T>>(config);
  return Object.freeze({
    listProviders: keyword => call<Provider[]>({ url: '/third/provider/list', method: 'get', params: { keyword } }),
    getProvider: value => call<Provider>({ url: `/third/provider/${id(value)}`, method: 'get' }),
    saveProvider: data => call<void>({ url: '/third/provider', method: 'post', data }),
    changeProviderStatus: (value, status) => call<void>({ url: `/third/provider/${id(value)}/status`, method: 'post', params: { status } }),
    deleteProvider: value => call<void>({ url: `/third/provider/${id(value)}/remove`, method: 'post' }),
    listEndpoints: (providerId, keyword) => call<Endpoint[]>({ url: '/third/endpoint/list', method: 'get', params: { providerId, keyword } }),
    getEndpoint: value => call<Endpoint>({ url: `/third/endpoint/${id(value)}`, method: 'get' }),
    saveEndpoint: data => call<void>({ url: '/third/endpoint', method: 'post', data }),
    changeEndpointStatus: (value, status) => call<void>({ url: `/third/endpoint/${id(value)}/status`, method: 'post', params: { status } }),
    deleteEndpoint: value => call<void>({ url: `/third/endpoint/${id(value)}/remove`, method: 'post' }),
    listCredentials: (providerCode, endpointCode) => call<CredentialSummary[]>({ url: '/third/credential/list', method: 'get', params: { providerCode, endpointCode } }),
    saveCredential: data => call<void>({ url: '/third/credential', method: 'post', data }),
    deleteCredential: value => call<void>({ url: `/third/credential/${id(value)}/remove`, method: 'post' }),
    listInvocations: providerCode => call<Invocation[]>({ url: '/third/invocation/list', method: 'get', params: { providerCode } }),
    listStatistics: providerCode => call<Statistic[]>({ url: '/third/statistics/list', method: 'get', params: { providerCode } })
  });
}
