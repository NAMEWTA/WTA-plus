import type { ApiErrorInfo, HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, expectTypeOf, it, vi } from 'vitest';
import {
  createSystemResourceService,
  ResourceContractError,
  ResourceSecurityError,
  type SocialAuthVO
} from './resource-service';

describe('system resource transports', () => {
  it('preserves the complete resource method and path matrix', async () => {
    const requests: HttpRequest[] = [];
    const http: HttpClient = {
      request: vi.fn(async request => {
        requests.push(request);
        if (request.url.includes('/download-url')) {
          return {
            code: 200,
            data: {
              accessType: 'PRIVATE',
              url: 'https://files.example.test/file',
              fileName: 'file',
              expiresAt: 'later'
            }
          } as never;
        }
        if (request.url === '/resource/oss/list') {
          return { code: 200, data: { rows: [], total: 0 } } as never;
        }
        if (request.url === '/resource/oss/config/list') {
          return { code: 200, data: { rows: [], total: 0 } } as never;
        }
        if (request.url === '/resource/oss/config/config%2F1') {
          return { code: 200, data: { accessPolicy: '0' } } as never;
        }
        if (request.url.includes('/listByIds/')) return { code: 200, data: [] } as never;
        if (request.url.endsWith('/parts/sign')) return { code: 200, data: { parts: [] } } as never;
        if (request.url.endsWith('/parts')) {
          return {
            code: 200,
            data: {
              uploadToken: 'token',
              mode: 'MULTIPART',
              state: 'UPLOADING',
              completedOssId: null,
              fileName: 'file',
              fileSize: 1,
              contentType: 'text/plain',
              partSize: 1,
              partCount: 1,
              expiresAt: 'later',
              uploadedParts: []
            }
          } as never;
        }
        if (request.url === '/resource/oss/uploads') {
          return { code: 200, data: { uploadToken: 'token', mode: 'SINGLE', expiresAt: 'later' } } as never;
        }
        return { code: 200, data: {} } as never;
      })
    };
    const service = createSystemResourceService(http);
    const query = { pageNum: 1, pageSize: 10 };
    const form = { marker: 'resource' } as never;
    const ossConfigForm = { marker: 'resource', accessPolicy: 'PRIVATE' } as never;

    await service.dictData.byType('sys/status');
    await service.dictData.list(query as never);
    await service.dictData.get('data/1');
    await service.dictData.add(form);
    await service.dictData.update(form);
    await service.dictData.delete(['data/1', 2]);
    await service.dictTypes.list(query as never);
    await service.dictTypes.get('type/1');
    await service.dictTypes.add(form);
    await service.dictTypes.update(form);
    await service.dictTypes.delete(['type/1', 2]);
    await service.dictTypes.refreshCache();
    await service.dictTypes.options();
    await service.configs.list(query as never);
    await service.configs.get('config/1');
    await service.configs.byKey('site/name');
    await service.configs.add(form);
    await service.configs.update(form);
    await service.configs.updateByKey('site/name', 'NAMEWTA');
    await service.configs.delete(['config/1', 2]);
    await service.configs.refreshCache();
    await service.oss.list(query as never);
    await service.oss.listByIds(['oss/1', 2]);
    await service.oss.initUpload(form);
    await service.oss.signParts('upload/token', [1, 2]);
    await service.oss.resumeUpload('upload/token', 'fingerprint');
    await service.oss.completeUpload('upload/token', [{ partNumber: 1, eTag: 'etag' }]);
    await service.oss.abortUpload('upload/token');
    await service.oss.downloadUrl('oss/1');
    await service.oss.delete(['oss/1', 2]);
    await service.ossConfigs.list(query as never);
    await service.ossConfigs.get('config/1');
    await service.ossConfigs.add(ossConfigForm);
    await service.ossConfigs.update(ossConfigForm);
    await service.ossConfigs.delete(['config/1', 2]);
    await service.ossConfigs.changeStatus('oss/1', '0', 'primary');
    await service.social.list();

    expect(requests).toEqual([
      { url: '/system/dict/data/type/sys%2Fstatus', method: 'get' },
      { url: '/system/dict/data/list', method: 'get', params: query },
      { url: '/system/dict/data/data%2F1', method: 'get' },
      { url: '/system/dict/data', method: 'post', data: form },
      { url: '/system/dict/data', method: 'put', data: form },
      { url: '/system/dict/data/data%2F1,2', method: 'delete' },
      { url: '/system/dict/type/list', method: 'get', params: query },
      { url: '/system/dict/type/type%2F1', method: 'get' },
      { url: '/system/dict/type', method: 'post', data: form },
      { url: '/system/dict/type', method: 'put', data: form },
      { url: '/system/dict/type/type%2F1,2', method: 'delete' },
      { url: '/system/dict/type/refreshCache', method: 'delete' },
      { url: '/system/dict/type/optionselect', method: 'get' },
      { url: '/system/config/list', method: 'get', params: query },
      { url: '/system/config/config%2F1', method: 'get' },
      { url: '/system/config/configKey/site%2Fname', method: 'get' },
      { url: '/system/config', method: 'post', data: form },
      { url: '/system/config', method: 'put', data: form },
      { url: '/system/config/updateByKey', method: 'put', data: { configKey: 'site/name', configValue: 'NAMEWTA' } },
      { url: '/system/config/config%2F1,2', method: 'delete' },
      { url: '/system/config/refreshCache', method: 'delete' },
      { url: '/resource/oss/list', method: 'get', params: query },
      { url: '/resource/oss/listByIds/oss%2F1,2', method: 'get' },
      { url: '/resource/oss/uploads', method: 'post', data: form },
      { url: '/resource/oss/uploads/upload%2Ftoken/parts/sign', method: 'post', data: { partNumbers: [1, 2] } },
      { url: '/resource/oss/uploads/upload%2Ftoken/parts', method: 'get', params: { fingerprint: 'fingerprint' } },
      {
        url: '/resource/oss/uploads/upload%2Ftoken/complete',
        method: 'post',
        data: { parts: [{ partNumber: 1, eTag: 'etag' }] }
      },
      { url: '/resource/oss/uploads/upload%2Ftoken', method: 'delete' },
      { url: '/resource/oss/oss%2F1/download-url', method: 'get' },
      { url: '/resource/oss/oss%2F1,2', method: 'delete' },
      { url: '/resource/oss/config/list', method: 'get', params: query },
      { url: '/resource/oss/config/config%2F1', method: 'get' },
      { url: '/resource/oss/config', method: 'post', data: { marker: 'resource', accessPolicy: '0' } },
      { url: '/resource/oss/config/edit', method: 'post', data: { marker: 'resource', accessPolicy: '0' } },
      { url: '/resource/oss/config/remove/config%2F1,2', method: 'post' },
      {
        url: '/resource/oss/config/changeStatus',
        method: 'post',
        data: { ossConfigId: 'oss/1', status: '0', configKey: 'primary' }
      },
      { url: '/system/social/list', method: 'get' }
    ]);
  });

  it('fails closed on unsafe OSS response URLs without exposing request details', async () => {
    const request: HttpClient['request'] = async () =>
      ({ data: { accessType: 'PRIVATE', url: 'javascript:alert(1)', fileName: 'x', expiresAt: 'later' } }) as never;
    const service = createSystemResourceService({ request });
    await expect(service.oss.downloadUrl('1')).rejects.toEqual(
      expect.objectContaining({ code: 'unsafe-resource-url', name: ResourceSecurityError.name })
    );
  });

  it('projects public and private access responses without guessing from URL query parameters', async () => {
    const responses = [
      {
        accessType: 'PUBLIC',
        url: 'https://cdn.example.test/stable-file',
        fileName: 'public.txt',
        expiresAt: null
      },
      {
        accessType: 'PRIVATE',
        url: 'https://files.example.test/private-file?opaque=value',
        fileName: 'private.txt',
        expiresAt: '2026-09-01T03:40:00Z'
      }
    ];
    const request: HttpClient['request'] = vi.fn(async () => ({ data: responses.shift() }) as never);
    const service = createSystemResourceService({ request });

    await expect(service.oss.downloadUrl(1)).resolves.toMatchObject({
      data: { accessType: 'PUBLIC', expiresAt: null }
    });
    await expect(service.oss.downloadUrl(2)).resolves.toMatchObject({
      data: { accessType: 'PRIVATE', expiresAt: '2026-09-01T03:40:00Z' }
    });
  });

  it.each([
    { accessType: 'CUSTOM', expiresAt: null },
    { accessType: 'PUBLIC', expiresAt: 'later' },
    { accessType: 'PRIVATE', expiresAt: null }
  ])('rejects inconsistent OSS access contract %#', async value => {
    const request: HttpClient['request'] = async () =>
      ({ data: { ...value, url: 'https://files.example.test/file', fileName: 'file' } }) as never;
    const service = createSystemResourceService({ request });

    await expect(service.oss.downloadUrl(1)).rejects.toBeInstanceOf(ResourceContractError);
  });

  it('maps semantic access policies to the backend physical encoding and fails closed on retired values', async () => {
    const requests: HttpRequest[] = [];
    const request: HttpClient['request'] = async config => {
      requests.push(config);
      if (config.url.endsWith('/list')) {
        return { data: { rows: [{ accessPolicy: '0' }, { accessPolicy: '2' }], total: 2 } } as never;
      }
      return { data: {} } as never;
    };
    const service = createSystemResourceService({ request });

    const list = await service.ossConfigs.list({} as never);
    expect(list.data.rows.map(item => item.accessPolicy)).toEqual(['PRIVATE', 'PUBLIC_READ']);
    await service.ossConfigs.add({ accessPolicy: 'PUBLIC_READ' } as never);
    expect(requests.at(-1)?.data).toEqual({ accessPolicy: '2' });

    const retired = createSystemResourceService({
      request: async () => ({ data: { rows: [{ accessPolicy: '1' }], total: 1 } }) as never
    });
    await expect(retired.ossConfigs.list({} as never)).rejects.toBeInstanceOf(ResourceContractError);
  });

  it('rejects unsafe upload URLs before they reach browser upload code', async () => {
    const request: HttpClient['request'] = async () =>
      ({
        data: {
          uploadToken: 'token',
          mode: 'SINGLE',
          expiresAt: 'later',
          presignedRequest: {
            method: 'PUT',
            url: 'https://token:secret@files.example.test/file',
            requiredHeaders: { Authorization: 'must-not-escape' },
            expiresAt: 'later'
          }
        }
      }) as never;
    const service = createSystemResourceService({ request });

    const error = await service.oss.initUpload({} as never).catch(value => value);
    expect(error).toEqual(expect.objectContaining({ code: 'unsafe-resource-url', message: '资源地址不可用' }));
    expect(JSON.stringify(error)).not.toMatch(/Authorization|must-not-escape|token:secret/);
  });

  it('does not resurrect a stored OSS URL when authorization lookup fails', async () => {
    const failure = new Error('authorization unavailable');
    const request: HttpClient['request'] = async config => {
      if (config.url.includes('/download-url')) throw failure;
      return { data: [{ ossId: 1, url: 'https://legacy.example.test/file' }] } as never;
    };
    const service = createSystemResourceService({ request });

    await expect(service.oss.listByIds(1)).rejects.toBe(failure);
  });

  it('exposes the concrete system social list result type', () => {
    const service = createSystemResourceService({ request: vi.fn() });
    expectTypeOf(service.social.list).returns.resolves.toEqualTypeOf<{
      code?: number;
      data: SocialAuthVO[];
      msg?: string;
      error?: ApiErrorInfo;
    }>();
  });
});
