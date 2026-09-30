import type { HttpClient } from '@namewta/platform-contracts';
import type { OidcApplicationInput, OidcApplicationQuery } from './types';
import { mapApplication, mapDelivery, mapFields, mapPage, mapProvider, responseData } from './mapper';
import { applicationInputTransport, applicationUpdateTransport } from './transport';

const base = '/oidc/admin/applications';
const key = (id: string) => encodeURIComponent(id);

/** Secret 不经日志、缓存或存储；取消信号由页面 owner 传入。 */
export function createOidcService(http: HttpClient) {
  const get = async (url: string, signal?: AbortSignal) =>
    responseData(await http.request<unknown>({ url, method: 'get', signal }));
  const post = async (url: string, data: unknown, signal?: AbortSignal) =>
    responseData(await http.request<unknown>({ url, method: 'post', data, signal }));
  return {
    list: async (params: OidcApplicationQuery, signal?: AbortSignal) =>
      mapPage(await http.request<unknown>({ url: base, method: 'get', params, signal })),
    get: async (id: string, signal?: AbortSignal) => mapApplication(await get(`${base}/${key(id)}`, signal)),
    fields: async (signal?: AbortSignal) => mapFields(await get('/oidc/admin/fields', signal)),
    provider: async (signal?: AbortSignal) => mapProvider(await get('/oidc/admin/provider', signal)),
    create: async (input: OidcApplicationInput, signal?: AbortSignal) =>
      mapDelivery(await post(`${base}/create`, applicationInputTransport(input), signal)),
    update: async (input: OidcApplicationInput & { applicationId: string; version: number }, signal?: AbortSignal) =>
      mapApplication(await post(`${base}/update`, applicationUpdateTransport(input), signal)),
    status: async (id: string, version: number, enabled: boolean, signal?: AbortSignal) => {
      await post(`${base}/${key(id)}/status`, { version, enabled }, signal);
    },
    rotateSecret: async (id: string, version: number, signal?: AbortSignal) =>
      mapDelivery(await post(`${base}/${key(id)}/rotate-secret`, { version }, signal)),
    remove: async (id: string, version: number, signal?: AbortSignal) => {
      await post(`${base}/${key(id)}/delete`, { version }, signal);
    }
  };
}
export type OidcService = ReturnType<typeof createOidcService>;
