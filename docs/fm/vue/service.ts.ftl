import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type {
  ApiResponse,
  Identifier,
  IdentifierList<#if !table.tree>,
  PageResult
</#if>
} from '../types';
import type {
  ${BusinessName}Form,
  ${BusinessName}Query,
  ${BusinessName}VO
} from './types';
import {
<#if table.tree>
  build${BusinessName}Tree,
</#if>
  project${BusinessName}Transport,
  type ${BusinessName}Transport
} from './transport';

export interface ${BusinessName}Service {
  list(query?: ${BusinessName}Query): Promise<ApiResponse<<#if table.tree>${BusinessName}VO[]<#else>PageResult<${BusinessName}VO></#if>>>;
  get(id: Identifier): Promise<ApiResponse<${BusinessName}VO>>;
  add(data: ${BusinessName}Form): Promise<ApiResponse>;
  update(data: ${BusinessName}Form): Promise<ApiResponse>;
  delete(ids: IdentifierList): Promise<ApiResponse>;
<#if enableStatus>
  changeStatus(id: Identifier, status: ${statusColumn.tsType}): Promise<ApiResponse>;
</#if>
<#if enableSort>
  updateSort(id: Identifier, sortValue: ${sortColumn.tsType}): Promise<ApiResponse>;
</#if>
}

const segment = (value: IdentifierList): string =>
  (Array.isArray(value) ? value : [value]).map(item => encodeURIComponent(String(item))).join(',');

export function create${BusinessName}Service(http: HttpClient): ${BusinessName}Service {
  const request = <T = unknown>(config: HttpRequest): Promise<ApiResponse<T>> => http.request<ApiResponse<T>>(config);

  return Object.freeze({
    list: async (params?: ${BusinessName}Query) => {
      const response = await request<<#if table.tree>${BusinessName}Transport[]<#else>PageResult<${BusinessName}Transport></#if>>({
        url: '/${moduleName}/${businessName}/list',
        method: 'get',
        params
      });
<#if table.tree>
      return {
        ...response,
        data: build${BusinessName}Tree((response.data ?? []).map(project${BusinessName}Transport))
      };
<#else>
      return {
        ...response,
        data: {
          rows: (response.data?.rows ?? []).map(project${BusinessName}Transport),
          total: response.data?.total ?? 0
        }
      };
</#if>
    },
    get: async (id: Identifier) => {
      const response = await request<${BusinessName}Transport>({
        url: '/${moduleName}/${businessName}/' + segment(id),
        method: 'get'
      });
      return response.data !== undefined
        ? { ...response, data: project${BusinessName}Transport(response.data) }
        : (response as ApiResponse<${BusinessName}VO>);
    },
    add: (data: ${BusinessName}Form) => request({ url: '/${moduleName}/${businessName}', method: 'post', data }),
    update: (data: ${BusinessName}Form) =>
      request({ url: '/${moduleName}/${businessName}/edit', method: 'post', data }),
    delete: (ids: IdentifierList) =>
      request({ url: '/${moduleName}/${businessName}/remove/' + segment(ids), method: 'post' }),
<#if enableStatus>
    changeStatus: (id: Identifier, status: ${statusColumn.tsType}) =>
      request({
        url: '/${moduleName}/${businessName}/changeStatus',
        method: 'post',
        data: { ${pkColumn.javaField}: id, ${statusField}: status }
      }),
</#if>
<#if enableSort>
    updateSort: (id: Identifier, sortValue: ${sortColumn.tsType}) =>
      request({
        url: '/${moduleName}/${businessName}/updateSort',
        method: 'post',
        data: { ${pkColumn.javaField}: id, ${sortField}: sortValue }
      }),
</#if>
  });
}
