import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type { OpenApiCredentialCreateInput, OpenApiIdentifier, OpenApiService, OpenApiUserQuery } from './types';
import {
  projectOpenApiCatalog,
  projectOpenApiCatalogItem,
  projectOpenApiCredentialIssued,
  projectOpenApiCredentialSummary,
  projectOpenApiEmpty,
  projectOpenApiResponse,
  projectOpenApiUsers
} from './transport';

const BASE_PATH = '/system/openApi';
const NO_STORE = Object.freeze({ 'Cache-Control': 'no-store', repeatSubmit: false });

function userSegment(value: OpenApiIdentifier): string {
  const text = String(value);
  if (!/^[1-9]\d*$/.test(text)) throw new TypeError('OpenAPI target userId is invalid');
  return encodeURIComponent(text);
}

function interfaceSegment(value: string): string {
  if (typeof value !== 'string' || value.trim() === '') throw new TypeError('OpenAPI interfaceId is required');
  return encodeURIComponent(value);
}

function createInput(value: OpenApiCredentialCreateInput): OpenApiCredentialCreateInput {
  if (!value || typeof value !== 'object') throw new TypeError('OpenAPI credential input is invalid');
  const appName = typeof value.appName === 'string' ? value.appName.trim() : '';
  if (appName === '' || appName.length > 100) throw new TypeError('OpenAPI appName is invalid');
  if (value.expiresAt !== undefined && value.expiresAt !== null && typeof value.expiresAt !== 'string') {
    throw new TypeError('OpenAPI expiresAt is invalid');
  }
  if (value.remark !== undefined && value.remark !== null && typeof value.remark !== 'string') {
    throw new TypeError('OpenAPI remark is invalid');
  }
  if (typeof value.remark === 'string' && value.remark.length > 500) {
    throw new TypeError('OpenAPI remark is invalid');
  }
  return Object.freeze({
    appName,
    ...(value.expiresAt !== undefined ? { expiresAt: value.expiresAt } : {}),
    ...(value.remark !== undefined ? { remark: value.remark } : {})
  });
}

function userQuery(value: OpenApiUserQuery | undefined): OpenApiUserQuery | undefined {
  if (value === undefined) return undefined;
  if (value.keyword !== undefined && typeof value.keyword !== 'string') {
    throw new TypeError('OpenAPI user keyword is invalid');
  }
  const keyword = value.keyword?.trim();
  if (value.limit !== undefined && (!Number.isInteger(value.limit) || value.limit < 1 || value.limit > 100)) {
    throw new TypeError('OpenAPI user limit is invalid');
  }
  return Object.freeze({
    ...(keyword ? { keyword } : {}),
    ...(value.limit !== undefined ? { limit: value.limit } : {})
  });
}

export function createOpenApiService(http: HttpClient): OpenApiService {
  const request = async <T>(config: HttpRequest, project: (value: unknown) => T) => {
    const response = await http.request<unknown>(config);
    return projectOpenApiResponse(response, project);
  };
  const issuedHeaders = NO_STORE;
  const userPath = (userId: OpenApiIdentifier) => `${BASE_PATH}/users/${userSegment(userId)}`;

  return Object.freeze({
    currentUser: Object.freeze({
      getCredential: () =>
        request({ url: `${BASE_PATH}/self/credential`, method: 'get' }, projectOpenApiCredentialSummary),
      createCredential: (input: OpenApiCredentialCreateInput) =>
        request(
          {
            url: `${BASE_PATH}/self/credential/create`,
            method: 'post',
            headers: issuedHeaders,
            data: createInput(input)
          },
          projectOpenApiCredentialIssued
        ),
      resetCredential: () =>
        request(
          { url: `${BASE_PATH}/self/credential/reset`, method: 'post', headers: issuedHeaders },
          projectOpenApiCredentialIssued
        ),
      enableCredential: () =>
        request({ url: `${BASE_PATH}/self/credential/enable`, method: 'post' }, projectOpenApiCredentialSummary),
      disableCredential: () =>
        request({ url: `${BASE_PATH}/self/credential/disable`, method: 'post' }, projectOpenApiCredentialSummary),
      deleteCredential: () =>
        request({ url: `${BASE_PATH}/self/credential/delete`, method: 'post' }, projectOpenApiEmpty),
      listInterfaces: () => request({ url: `${BASE_PATH}/self/interfaces`, method: 'get' }, projectOpenApiCatalog),
      getInterface: (interfaceId: string) =>
        request(
          { url: `${BASE_PATH}/self/interfaces/${interfaceSegment(interfaceId)}`, method: 'get' },
          projectOpenApiCatalogItem
        )
    }),
    targetUser: Object.freeze({
      listUsers: (query?: OpenApiUserQuery) =>
        request({ url: `${BASE_PATH}/users`, method: 'get', params: userQuery(query) }, projectOpenApiUsers),
      getCredential: (userId: OpenApiIdentifier) =>
        request({ url: `${userPath(userId)}/credential`, method: 'get' }, projectOpenApiCredentialSummary),
      createCredential: (userId: OpenApiIdentifier, input: OpenApiCredentialCreateInput) =>
        request(
          {
            url: `${userPath(userId)}/credential/create`,
            method: 'post',
            headers: issuedHeaders,
            data: createInput(input)
          },
          projectOpenApiCredentialIssued
        ),
      resetCredential: (userId: OpenApiIdentifier) =>
        request(
          { url: `${userPath(userId)}/credential/reset`, method: 'post', headers: issuedHeaders },
          projectOpenApiCredentialIssued
        ),
      enableCredential: (userId: OpenApiIdentifier) =>
        request({ url: `${userPath(userId)}/credential/enable`, method: 'post' }, projectOpenApiCredentialSummary),
      disableCredential: (userId: OpenApiIdentifier) =>
        request({ url: `${userPath(userId)}/credential/disable`, method: 'post' }, projectOpenApiCredentialSummary),
      deleteCredential: (userId: OpenApiIdentifier) =>
        request({ url: `${userPath(userId)}/credential/delete`, method: 'post' }, projectOpenApiEmpty),
      listInterfaces: (userId: OpenApiIdentifier) =>
        request({ url: `${userPath(userId)}/interfaces`, method: 'get' }, projectOpenApiCatalog),
      getInterface: (userId: OpenApiIdentifier, interfaceId: string) =>
        request(
          { url: `${userPath(userId)}/interfaces/${interfaceSegment(interfaceId)}`, method: 'get' },
          projectOpenApiCatalogItem
        )
    })
  });
}
