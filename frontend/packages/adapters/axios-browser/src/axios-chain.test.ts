import type { ErrorPresenter } from '@namewta/platform-contracts';
import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import type { AxiosBrowserOptions, RepeatSubmission } from './index';
import { createAxiosBrowserAdapter } from './index';

function chainOptions(overrides: Partial<AxiosBrowserOptions> = {}): AxiosBrowserOptions {
  let repeatSubmission: RepeatSubmission | null = null;
  const presenter: ErrorPresenter = {
    confirmSessionExpired: vi.fn().mockResolvedValue(undefined),
    present: vi.fn()
  };
  return {
    baseURL: '/prod-api',
    client: { clientId: 'client-a' },
    errorPresenter: presenter,
    getLanguage: () => 'zh-CN',
    getToken: () => 'secret-token',
    onUnauthorized: vi.fn(),
    repeatSubmissions: {
      get: () => repeatSubmission,
      set: value => {
        repeatSubmission = value;
      }
    },
    resolveErrorCode: code => ({ '401': 'unauthorized', default: 'default-error' })[String(code)],
    serializeParams: () => '',
    successCode: 200,
    ...overrides
  };
}

function requestThrough(options: AxiosBrowserOptions, config: Parameters<ReturnType<typeof createAxiosBrowserAdapter>['request']>[0]) {
  const client = createAxiosBrowserAdapter(options);
  return client.request(config);
}

describe('axios production interceptor chain', () => {
  it('sanitizes a failed login request without retaining its password or transport config', async () => {
    const error = await requestThrough(chainOptions(), {
      adapter: (config: InternalAxiosRequestConfig<unknown>) => Promise.reject(new AxiosError('Network Error', 'ERR_NETWORK', config)),
      data: { password: 'owned-password-canary' }, headers: { repeatSubmit: false }, method: 'post', url: '/auth/login'
    }).catch(value => value);
    expect(error).toMatchObject({ kind: 'network', code: 'ERR_NETWORK', isHandled: true });
    expect(JSON.stringify(error)).not.toContain('owned-password-canary');
    expect((error as { cause?: unknown }).cause).not.toHaveProperty('config');
  });

  it('does not expose authorization headers through a network error cause', async () => {
    const error = await requestThrough(chainOptions(), {
      adapter: (config: InternalAxiosRequestConfig<unknown>) =>
        Promise.reject(new AxiosError('Network Error', 'ERR_NETWORK', config)),
      method: 'get',
      url: '/private'
    }).catch(value => value);

    expect(error).toMatchObject({
      kind: 'network',
      code: 'ERR_NETWORK',
      cause: { code: 'ERR_NETWORK', message: 'Network Error', name: 'AxiosError' }
    });
    const cause = (error as { cause?: unknown }).cause;
    expect(JSON.stringify(error)).not.toContain('secret-token');
    expect(cause).not.toHaveProperty('config');
    expect(JSON.stringify(cause)).not.toContain('Authorization');
  });

  it('preserves server errors and reports them once', async () => {
    const errorPresenter = { confirmSessionExpired: vi.fn(), present: vi.fn() };
    const error = await requestThrough(chainOptions({ errorPresenter }), {
      adapter: (config: InternalAxiosRequestConfig<unknown>) => Promise.resolve({
        config, data: { code: 500, msg: 'request failed' }, headers: {}, status: 200, statusText: 'OK'
      }), method: 'post', url: '/auth/register', data: { username: 'owned-user' }
    }).catch(value => value);
    expect(error).toMatchObject({ kind: 'server', message: 'request failed', isHandled: true });
    expect(errorPresenter.present).toHaveBeenCalledOnce();
  });

  it('sends ordinary JSON and accepts an ordinary JSON response', async () => {
    const requestAdapter = vi.fn((config: InternalAxiosRequestConfig<unknown>) => Promise.resolve({
      config, data: { code: 200, value: 'plain' }, headers: {}, status: 200, statusText: 'OK'
    }));
    const body = { password: 'owned-password-canary' };
    await expect(requestThrough(chainOptions(), {
      adapter: requestAdapter, data: body, headers: { repeatSubmit: false }, method: 'post', url: '/auth/login'
    })).resolves.toEqual({ code: 200, value: 'plain' });
    const sentRequest = requestAdapter.mock.calls[0]![0];
    expect(typeof sentRequest.data).toBe('string');
    expect(JSON.parse(String(sentRequest.data))).toEqual(body);
    expect(sentRequest.headers).not.toHaveProperty('encrypt-key');
    expect(sentRequest.headers).not.toHaveProperty('isEncrypt');
    expect(sentRequest.headers.Authorization).toBe('Bearer secret-token');
  });

  it('returns binary downloads unchanged', async () => {
    for (const [data, responseType] of [
      [new Blob(['owned-download']), 'blob'],
      [new TextEncoder().encode('owned-download').buffer, 'arraybuffer']
    ] as const) {
      await expect(requestThrough(chainOptions(), {
        adapter: (config: InternalAxiosRequestConfig<unknown>) => Promise.resolve({
          config, data, headers: {}, status: 200, statusText: 'OK'
        }), method: 'get', responseType, url: '/download'
      })).resolves.toBe(data);
    }
  });

  it('reports an asynchronously rejected unauthorized recovery as unhandled', async () => {
    const onUnauthorized = vi.fn(() => Promise.reject(new Error('logout failed')));
    const error = await requestThrough(chainOptions({ onUnauthorized }), {
      adapter: (config: InternalAxiosRequestConfig<unknown>) =>
        Promise.resolve({ config, data: { code: 401 }, headers: {}, status: 200, statusText: 'OK' }),
      method: 'get',
      url: '/private'
    }).catch(value => value);

    expect(error).toMatchObject({ kind: 'unauthorized', code: 401, isHandled: false });
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });
});

describe('session request cancellation', () => {
  it('cancels an already queued request before dispatch without a notification', async () => {
    const options = chainOptions();
    const client = createAxiosBrowserAdapter(options);
    const adapter = vi.fn();
    const config = { adapter, method: 'get' as const, url: '/owned-queued' };
    const pending = client.request(config).catch(error => error);
    client.cancelPending();
    expect(await pending).toMatchObject({ code: 'ERR_CANCELED', isHandled: true });
    expect(adapter).not.toHaveBeenCalled();
    expect(options.errorPresenter.present).not.toHaveBeenCalled();
  });

  it('prevents an old response from triggering recovery and allows the new request scope', async () => {
    const options = chainOptions();
    const client = createAxiosBrowserAdapter(options);
    let started!: () => void;
    let finish!: (value: AxiosResponse<unknown>) => void;
    let requestConfig!: InternalAxiosRequestConfig<unknown>;
    const ready = new Promise<void>(resolve => { started = resolve; });
    const pending = client.request({
      method: 'get', url: '/owned-old',
      adapter: (config: InternalAxiosRequestConfig<unknown>) => {
        requestConfig = config; started(); return new Promise<AxiosResponse<unknown>>(resolve => { finish = resolve; });
      }
    }).catch(error => error);
    await ready;
    client.cancelPending();
    finish({ config: requestConfig, data: { code: 401 }, headers: {}, status: 200, statusText: 'OK' });
    expect(await pending).toMatchObject({ code: 'ERR_CANCELED', isHandled: true });
    expect(options.onUnauthorized).not.toHaveBeenCalled();
    expect(options.errorPresenter.present).not.toHaveBeenCalled();
    const fresh = { method: 'get' as const, url: '/owned-new',
      adapter: (config: InternalAxiosRequestConfig<unknown>) => Promise.resolve({ config, data: { code: 200 }, headers: {}, status: 200, statusText: 'OK' }) };
    await expect(client.request(fresh)).resolves.toEqual({ code: 200 });
  });
});


describe('typed Axios request adaptation', () => {
  it('preserves valid header values and typed response unwrapping', async () => {
    const client = createAxiosBrowserAdapter(chainOptions());
    const adapter = vi.fn(async (config: InternalAxiosRequestConfig<unknown>) => ({
      config, data: { code: 200, result: 'owned' }, headers: {}, status: 200, statusText: 'OK'
    }));
    const response = await client.request<{ code: number; result: string }>({
      method: 'get', url: '/headers', adapter,
      headers: { optional: undefined, omitted: null, disabled: false, count: 7, enabled: true, multi: ['first', 'second'] }
    });
    expect(response).toEqual({ code: 200, result: 'owned' });
    expect(adapter.mock.calls[0]![0].headers).toMatchObject({ count: '7', enabled: 'true', multi: ['first', 'second'], disabled: false });
  });

  it.each([{}, ['valid', 7]])('rejects invalid headers before dispatch: %j', async value => {
    const client = createAxiosBrowserAdapter(chainOptions()); const adapter = vi.fn();
    await expect(client.request({ method: 'get', url: '/headers', adapter, headers: { invalid: value } })).rejects.toThrow('请求头格式不可用');
    expect(adapter).not.toHaveBeenCalled();
  });

  it.each([null, 'plain text', { code: 200, data: null }])('preserves legal response payload %j', async data => {
    await expect(requestThrough(chainOptions(), {
      method: 'get', url: '/nullable', adapter: async config => ({ config, data, headers: {}, status: 200, statusText: 'OK' })
    })).resolves.toEqual(data);
  });
});
