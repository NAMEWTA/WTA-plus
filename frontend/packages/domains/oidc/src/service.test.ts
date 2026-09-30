import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, it } from 'vitest';
import { mapApplication } from './mapper';
import { createOidcService } from './service';
import { applicationScopes, selectOrdinaryFields, type OidcApplication, type OidcField } from './types';

const application: OidcApplication = {
  applicationId: '9007199254740993',
  name: '协作平台',
  clientId: 'oidc_abc',
  redirectUris: ['https://app.example/callback'],
  postLogoutRedirectUris: [],
  allowedFields: ['nickname', 'email'],
  clientAuthenticationMethod: 'client_secret_basic',
  pkceRequired: true,
  enabled: true,
  version: 0
};
const field = (key: string, scope: string, sensitive = false): OidcField => ({
  key,
  label: key,
  scope,
  sensitive,
  defaultEnabled: false,
  group: '个人档案'
});
describe('OIDC 应用管理合同', () => {
  it('保留长整型字符串，拒绝已丢精度的数字和不完整策略', () => {
    expect(mapApplication(application).applicationId).toBe('9007199254740993');
    expect(() => mapApplication({ ...application, applicationId: Number(application.applicationId) })).toThrow();
    expect(() => mapApplication({ ...application, pkceRequired: 'false' })).toThrow();
    expect(() => mapApplication({ ...application, allowedFields: null })).toThrow();
  });
  it('常用字段批选不会授权完整证件，已显式选中的敏感项保持', () => {
    const fields = [field('full_name', 'wta_person'), field('document_number', 'wta_person', true)];
    expect(selectOrdinaryFields([], fields, true)).toEqual(['full_name']);
    expect(selectOrdinaryFields(['document_number'], fields, true)).toEqual(['document_number', 'full_name']);
    expect(selectOrdinaryFields(['document_number', 'email'], fields, false)).toEqual(['email']);
  });
  it('接入范围仅从已允许字段推导，并始终包含 openid', () => {
    const fields = [field('nickname', 'profile'), field('email', 'email'), field('phone_number', 'phone')];
    expect(applicationScopes(application, fields)).toEqual(['openid', 'profile', 'email']);
  });
  it('查询使用 GET，创建/轮换使用 POST，密钥不追加到后续查询', async () => {
    const requests: HttpRequest[] = [];
    const http: HttpClient = {
      request: async <T>(request: HttpRequest): Promise<T> => {
        requests.push(request);
        if (request.url.endsWith('/create') || request.url.endsWith('/rotate-secret'))
          return { code: 200, data: { application, clientSecret: 'one-time-secret' } } as T;
        return { code: 200, rows: [application], total: 1 } as T;
      }
    };
    const service = createOidcService(http);
    const delivery = await service.create(application);
    expect(delivery.clientSecret).toBe('one-time-secret');
    await service.rotateSecret(application.applicationId, 0);
    const page = await service.list({ pageNum: 1, pageSize: 20 });
    expect(page.rows[0]?.clientId).toBe(application.clientId);
    expect(requests.map(request => request.method)).toEqual(['post', 'post', 'get']);
    expect(requests[1]?.data).toEqual({ version: 0 });
    expect(JSON.stringify(requests)).not.toContain('one-time-secret');
  });
  it('响应失败不被当作可展示的密钥交付', async () => {
    const http: HttpClient = {
      request: async <T>(): Promise<T> => ({ code: 500, data: { application, clientSecret: 'unexpected' } }) as T
    };
    await expect(createOidcService(http).create(application)).rejects.toThrow('OIDC 操作未完成');
  });
});
