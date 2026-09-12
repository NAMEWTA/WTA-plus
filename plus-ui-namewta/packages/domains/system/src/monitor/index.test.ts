import { describe, expect, it, vi } from 'vitest';
import { createMonitorService, monitorPermissions, MonitorSecurityError } from './index';

describe('monitor transport and security contracts', () => {
  it('preserves endpoint methods and encodes every path segment', async () => {
    const request = vi.fn().mockResolvedValue({ data: {} });
    const service = createMonitorService({ request });
    await service.loginInfo.delete(['a/b', 2]);
    await service.online.removeCurrent('token/value');
    expect(request.mock.calls.map(([value]) => value)).toEqual([
      { url: '/monitor/loginInfo/a%2Fb,2', method: 'delete' },
      { url: '/monitor/online/myself/token%2Fvalue', method: 'delete' }
    ]);
  });

  it('binds the complete monitor surface to its legacy HTTP methods', async () => {
    const request = vi.fn().mockResolvedValue({ data: { rows: [], total: 0 } });
    const service = createMonitorService({ request });
    const page = { pageNum: 1, pageSize: 10 } as never;
    await service.cache.get();
    await service.loginInfo.list(page);
    await service.loginInfo.unlock('user');
    await service.loginInfo.clean();
    await service.online.list(page);
    await service.online.forceLogout('token');
    await service.online.current();
    await service.operationLogs.list(page);
    await service.operationLogs.delete(1);
    await service.operationLogs.clean();
    expect(request.mock.calls.map(([value]) => `${value.method.toUpperCase()} ${value.url}`)).toEqual([
      'GET /monitor/cache',
      'GET /monitor/loginInfo/list',
      'GET /monitor/loginInfo/unlock/user',
      'DELETE /monitor/loginInfo/clean',
      'GET /monitor/online/list',
      'DELETE /monitor/online/token',
      'GET /monitor/online',
      'GET /monitor/operlog/list',
      'DELETE /monitor/operlog/1',
      'DELETE /monitor/operlog/clean'
    ]);
  });

  it.each([
    'javascript:alert(1)',
    'data:text/html,x',
    '//evil.example/x',
    'https://user@evil.example/x',
    'https://bad host/path',
    'https://example.test/report name',
    'https://exa%mple.example/x',
    'https://example.test:99999/x',
    'https://[2001:db8::1/x',
    'https://example.test/%',
    'https://example.test/%GG',
    '/relative/%2',
    '/reports/report name',
    '/ok\\bad'
  ])('rejects malformed or unsafe URL %s for embeds and downloads', value => {
    const service = createMonitorService({ request: vi.fn() });
    expect(() => service.externalIntent('monitor-admin', value, true)).toThrow(MonitorSecurityError);
    expect(() => service.attachmentIntent(value, true, 'proof.txt')).toThrow(MonitorSecurityError);
  });

  it('accepts and normalizes browser-valid root-relative, IPv6 and HTTP(S) URLs', () => {
    const service = createMonitorService({ request: vi.fn() });
    expect(service.externalIntent('monitor-admin', '/admin/../applications?view=all#health', true).url).toBe(
      '/applications?view=all#health'
    );
    expect(service.externalIntent('snail-job', 'https://[2001:db8::1]:8443/jobs', true).url).toBe(
      'https://[2001:db8::1]:8443/jobs'
    );
    expect(service.externalIntent('monitor-admin', '/reports/report%20name', true).url).toBe('/reports/report%20name');
    expect(service.attachmentIntent('http://files.example.test/report', true, 'report.txt')).toEqual({
      target: 'notify-attachment',
      url: 'http://files.example.test/report',
      mode: 'download',
      downloadName: 'report.txt'
    });
  });

  it('fails closed before exposing a URL when permission is absent', () => {
    const service = createMonitorService({ request: vi.fn() });
    expect(() => service.externalIntent('snail-job', 'https://jobs.example.test', false)).toThrowError(/无权/);
    expect(service.externalIntent('snail-job', '/snail-job', true)).toEqual({
      target: 'snail-job',
      url: '/snail-job',
      mode: 'embed'
    });
  });

  it('maps the Nacos console to its dedicated system permission and safe URL contract', () => {
    const service = createMonitorService({ request: vi.fn() });

    expect(monitorPermissions.nacos).toBe('system:nacos:console');
    expect(() => service.externalIntent('nacos', '/nacos/', false)).toThrowError(/无权/);
    expect(service.externalIntent('nacos', '/nacos/', true)).toEqual({
      target: 'nacos',
      url: '/nacos/',
      mode: 'embed'
    });
  });

  it('projects generated operation-log rows before returning domain models', async () => {
    const service = createMonitorService({
      request: async () => ({ data: { rows: [{ operId: 9, title: 'Update' }], total: 1 } }) as never
    });

    await expect(service.operationLogs.list({ pageNum: 1, pageSize: 10 } as never)).resolves.toMatchObject({
      data: {
        rows: [{ operId: 9, title: 'Update', businessType: 0, requestMethod: '', status: 0, costTime: 0 }],
        total: 1
      }
    });
  });

  it('normalizes generated operation-log pages without data or rows', async () => {
    const responses = [{ data: { total: 2 } }, { code: 204 }];
    const service = createMonitorService({ request: async () => responses.shift() as never });

    await expect(service.operationLogs.list({ pageNum: 1, pageSize: 10 } as never)).resolves.toEqual({
      data: { rows: [], total: 2 }
    });
    await expect(service.operationLogs.list({ pageNum: 1, pageSize: 10 } as never)).resolves.toEqual({
      code: 204,
      data: { rows: [], total: 0 }
    });
  });
});
