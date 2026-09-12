import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, expectTypeOf, it, vi } from 'vitest';
import {
  createOpenApiService,
  groupOpenApiCatalog,
  projectOpenApiCatalogItem,
  projectOpenApiCredentialIssued,
  projectOpenApiCredentialSummary,
  type OpenApiCredentialSummary,
  type OpenApiResponse
} from './index';

const summary = {
  credentialId: '91',
  ownerUserId: '41',
  appKey: 'app-key',
  appName: 'billing',
  status: '0',
  expiresAt: null,
  remark: 'automation',
  createTime: '2026-08-31T10:00:00',
  updateTime: '2026-08-31T10:00:00'
};

const catalogItem = {
  interfaceId: 'order.query',
  summary: 'Order query',
  method: 'POST',
  path: '/orders/query',
  accessRule: {
    permissions: [{ values: ['order:list'], mode: 'AND', orRoles: ['admin'] }],
    roles: [{ values: ['operator'], mode: 'OR' }]
  },
  parameters: [{ name: 'tenantId', location: 'header', required: true, schema: '{"type":"string"}' }],
  requestSchema: '{"type":"object"}',
  responseSchema: '{"type":"object"}',
  curlExample: 'curl example',
  javaExample: 'java example'
};

describe('OpenAPI domain transport boundary', () => {
  it('narrows credential and complete catalog metadata into immutable values', () => {
    const projectedSummary = projectOpenApiCredentialSummary(summary);
    const projectedItem = projectOpenApiCatalogItem(catalogItem);

    expect(projectedSummary).toEqual(summary);
    expect(projectedItem).toEqual(catalogItem);
    expect(Object.isFrozen(projectedSummary)).toBe(true);
    expect(Object.isFrozen(projectedItem.accessRule.permissions[0])).toBe(true);
    expect(Object.isFrozen(projectedItem.parameters)).toBe(true);
  });

  it.each([
    [{ ...summary, status: 'unknown' }],
    [{ ...summary, appSecret: 'must-not-be-present' }],
    [{ ...summary, credentialId: 1.5 }],
    [{ ...summary, expiresAt: 42 }]
  ])('rejects malformed or sensitive summary responses', value => {
    expect(() => projectOpenApiCredentialSummary(value)).toThrow('OpenAPI 响应不可用');
  });

  it('allows the secret only in a one-time create/reset result', () => {
    expect(projectOpenApiCredentialIssued({ ...summary, appSecret: 'one-time-secret' })).toEqual({
      ...summary,
      appSecret: 'one-time-secret'
    });
    expect(() => projectOpenApiCredentialIssued({ ...summary, appSecret: '' })).toThrow('OpenAPI 响应不可用');
  });

  it.each([
    [{ ...catalogItem, method: 'CONNECT' }],
    [{ ...catalogItem, accessRule: { permissions: [{ values: [], mode: 'AND', orRoles: [] }], roles: [] } }],
    [{ ...catalogItem, parameters: [{ name: 'id', location: '', required: true, schema: '{}' }] }],
    [{ ...catalogItem, responseSchema: { type: 'object' } }]
  ])('rejects malformed catalog metadata', value => {
    expect(() => projectOpenApiCatalogItem(value)).toThrow('OpenAPI 响应不可用');
  });

  it('groups catalog entries by the first path segment without changing item identity', () => {
    const orders = projectOpenApiCatalogItem(catalogItem);
    const users = projectOpenApiCatalogItem({ ...catalogItem, interfaceId: 'user.get', path: '/users/{id}' });
    const groups = groupOpenApiCatalog([orders, users]);

    expect(groups.map(group => ({ key: group.key, count: group.items.length }))).toEqual([
      { key: 'orders', count: 1 },
      { key: 'users', count: 1 }
    ]);
    expect(groups[0]?.items[0]).toBe(orders);
  });
});

describe('OpenAPI scope-specific HTTP contracts', () => {
  it('uses exact self URLs, GET queries and POST lifecycle commands', async () => {
    const requests: HttpRequest[] = [];
    const http = fixtureHttp(requests);
    const self = createOpenApiService(http).currentUser;

    await self.getCredential();
    await self.createCredential({
      appName: 'billing',
      expiresAt: null,
      remark: null,
      ownerUserId: '99',
      appSecret: 'must-not-be-sent'
    } as never);
    await self.resetCredential();
    await self.enableCredential();
    await self.disableCredential();
    await self.deleteCredential();
    await self.listInterfaces();
    await self.getInterface('order/query');

    expect(requests).toEqual([
      { url: '/system/openApi/self/credential', method: 'get' },
      {
        url: '/system/openApi/self/credential/create',
        method: 'post',
        headers: { 'Cache-Control': 'no-store', repeatSubmit: false },
        data: { appName: 'billing', expiresAt: null, remark: null }
      },
      {
        url: '/system/openApi/self/credential/reset',
        method: 'post',
        headers: { 'Cache-Control': 'no-store', repeatSubmit: false }
      },
      { url: '/system/openApi/self/credential/enable', method: 'post' },
      { url: '/system/openApi/self/credential/disable', method: 'post' },
      { url: '/system/openApi/self/credential/delete', method: 'post' },
      { url: '/system/openApi/self/interfaces', method: 'get' },
      { url: '/system/openApi/self/interfaces/order%2Fquery', method: 'get' }
    ]);
  });

  it('exposes different compile-time signatures for current and target user scopes', () => {
    const service = createOpenApiService({ request: vi.fn() });
    expectTypeOf(service.currentUser.getCredential).toEqualTypeOf<
      () => Promise<OpenApiResponse<OpenApiCredentialSummary>>
    >();
    expectTypeOf(service.targetUser.getCredential).parameter(0).toEqualTypeOf<string | number>();
  });

  it('requires an explicit target user for every admin operation', async () => {
    const requests: HttpRequest[] = [];
    const target = createOpenApiService(fixtureHttp(requests)).targetUser;
    const input = { appName: 'billing' };

    await target.listUsers({ keyword: 'demo', limit: 20 });
    await target.getCredential('41');
    await target.createCredential('41', input);
    await target.resetCredential('41');
    await target.enableCredential('41');
    await target.disableCredential('41');
    await target.deleteCredential('41');
    await target.listInterfaces('41');
    await target.getInterface('41', 'order/query');

    expect(requests).toEqual([
      { url: '/system/openApi/users', method: 'get', params: { keyword: 'demo', limit: 20 } },
      { url: '/system/openApi/users/41/credential', method: 'get' },
      {
        url: '/system/openApi/users/41/credential/create',
        method: 'post',
        headers: { 'Cache-Control': 'no-store', repeatSubmit: false },
        data: input
      },
      {
        url: '/system/openApi/users/41/credential/reset',
        method: 'post',
        headers: { 'Cache-Control': 'no-store', repeatSubmit: false }
      },
      { url: '/system/openApi/users/41/credential/enable', method: 'post' },
      { url: '/system/openApi/users/41/credential/disable', method: 'post' },
      { url: '/system/openApi/users/41/credential/delete', method: 'post' },
      { url: '/system/openApi/users/41/interfaces', method: 'get' },
      { url: '/system/openApi/users/41/interfaces/order%2Fquery', method: 'get' }
    ]);
  });

  it('preserves discriminable transport errors', async () => {
    const forbidden = Object.freeze({ kind: 'business', code: 403, message: 'forbidden' });
    const http: HttpClient = { request: vi.fn(async () => Promise.reject(forbidden)) };

    await expect(createOpenApiService(http).currentUser.getCredential()).rejects.toBe(forbidden);
  });
});

function fixtureHttp(requests: HttpRequest[]): HttpClient {
  return {
    async request<T>(request: HttpRequest): Promise<T> {
      requests.push(request);
      let data: unknown = summary;
      if (request.url.endsWith('/users'))
        data = [{ userId: '41', userName: 'demo', nickName: 'Demo', credential: summary }];
      else if (request.url.endsWith('/interfaces')) data = [catalogItem];
      else if (request.url.includes('/interfaces/')) data = catalogItem;
      else if (request.url.endsWith('/create') || request.url.endsWith('/reset')) {
        data = { ...summary, appSecret: 'one-time-secret' };
      } else if (request.url.endsWith('/delete')) data = null;
      return { code: 200, msg: 'ok', data } as T;
    }
  };
}
