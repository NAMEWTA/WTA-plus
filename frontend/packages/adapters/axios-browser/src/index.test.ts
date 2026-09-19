import type { ErrorPresenter, HttpRequest } from '@namewta/platform-contracts';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const axiosHarness = vi.hoisted(() => {
  let createdConfig: unknown;
  let requestFulfilled: ((config: Record<string, unknown>) => unknown) | undefined;
  let responseFulfilled: ((response: Record<string, unknown>) => unknown) | undefined;
  let responseRejected: ((error: unknown) => unknown) | undefined;
  const service = Object.assign(vi.fn(), {
    request: vi.fn(),
    post: vi.fn(),
    interceptors: {
      request: {
        use: vi.fn((fulfilled: (config: Record<string, unknown>) => unknown) => {
          requestFulfilled = fulfilled;
        })
      },
      response: {
        use: vi.fn(
          (fulfilled: (response: Record<string, unknown>) => unknown, rejected: (error: unknown) => unknown) => {
            responseFulfilled = fulfilled;
            responseRejected = rejected;
          }
        )
      }
    }
  });
  const axios = {
    create: vi.fn((config: unknown) => {
      createdConfig = config;
      return service;
    })
  };
  return {
    axios,
    createdConfig: () => createdConfig,
    requestFulfilled: () => requestFulfilled,
    responseFulfilled: () => responseFulfilled,
    responseRejected: () => responseRejected,
    service
  };
});

vi.mock('axios', () => ({ default: axiosHarness.axios }));

import type { AxiosBrowserOptions, DownloadOptions, RepeatSubmission } from './index';
import { createAxiosBrowserAdapter, downloadWithAxios, extractAxiosErrorMessage } from './index';

const getRequestInterceptor = () => {
  const interceptor = axiosHarness.requestFulfilled();
  expect(interceptor).toBeDefined();
  return interceptor!;
};

const getResponseInterceptor = () => {
  const interceptor = axiosHarness.responseFulfilled();
  expect(interceptor).toBeDefined();
  return interceptor!;
};

const getResponseErrorInterceptor = () => {
  const interceptor = axiosHarness.responseRejected();
  expect(interceptor).toBeDefined();
  return interceptor!;
};

function adapterOptions(overrides: Partial<AxiosBrowserOptions> = {}) {
  let repeatSubmission: RepeatSubmission | null = null;
  const errorPresenter: ErrorPresenter = {
    confirmSessionExpired: vi.fn().mockResolvedValue(undefined),
    present: vi.fn()
  };
  const options: AxiosBrowserOptions = {
    baseURL: '/prod-api',
    client: { clientId: ' client-a ' },
    errorPresenter,
    getLanguage: () => 'zh-CN',
    getToken: () => 'access-token',
    now: () => 1000,
    onUnauthorized: vi.fn(),
    repeatSubmissions: {
      get: () => repeatSubmission,
      set: value => {
        repeatSubmission = value;
      }
    },
    resolveErrorCode: code =>
      ({ '401': 'unauthorized', '404': 'not-found', '500': 'server-default', default: 'default-error' })[String(code)],
    serializeParams: () => 'page=1&size=10&',
    successCode: 200,
    ...overrides
  };
  return { errorPresenter, options };
}

const response = (
  data: unknown,
  headers: Record<string, unknown> = {},
  requestHeaders: Record<string, unknown> = {},
  responseType = 'json'
) => ({
  config: { headers: requestHeaders, responseType },
  data,
  headers,
  request: { responseType }
});

describe('axios browser request boundary', () => {
  beforeEach(() => vi.clearAllMocks());

  it('fails closed for missing Client and trims a valid Client id', () => {
    expect(() => createAxiosBrowserAdapter(adapterOptions({ client: { clientId: '   ' } }).options)).toThrow(
      'ClientContext.clientId is required'
    );

    createAxiosBrowserAdapter(adapterOptions().options);
    expect(axiosHarness.createdConfig()).toEqual(
      expect.objectContaining({ headers: expect.objectContaining({ clientid: 'client-a' }) })
    );
  });

  it('injects language/token, supports suppression, serializes queries and strips FormData content type', async () => {
    createAxiosBrowserAdapter(adapterOptions().options);
    const intercept = getRequestInterceptor();
    const request = (await intercept({ headers: {}, method: 'get', params: { page: 1 }, url: '/users' })) as {
      headers: Record<string, unknown>;
      params: unknown;
      url: string;
    };
    expect(request).toMatchObject({
      headers: { Authorization: 'Bearer access-token', 'Content-Language': 'zh-CN' },
      params: {},
      url: '/users?page=1&size=10'
    });

    const suppressed = (await intercept({ headers: { isToken: false }, method: 'get', url: '/public' })) as {
      headers: Record<string, unknown>;
    };
    expect(suppressed.headers.Authorization).toBeUndefined();

    const form = new FormData();
    const formRequest = (await intercept({
      data: form,
      headers: { 'Content-Type': 'multipart/form-data' },
      method: 'post',
      url: '/upload'
    })) as { headers: Record<string, unknown> };
    expect(formRequest.headers['Content-Type']).toBeUndefined();
  });

  it('rejects repeated submissions inside 500ms and accepts the boundary', async () => {
    let now = 1000;
    const { options } = adapterOptions({ now: () => now });
    createAxiosBrowserAdapter(options);
    const intercept = getRequestInterceptor();
    const config = { data: { value: 1 }, headers: {}, method: 'post', url: '/save' };
    await expect(Promise.resolve(intercept({ ...config, headers: {} }))).resolves.toMatchObject({ url: '/save' });
    now = 1499;
    await expect(Promise.resolve(intercept({ ...config, headers: {} }))).rejects.toThrow('数据正在处理，请勿重复提交');
    now = 1500;
    await expect(Promise.resolve(intercept({ ...config, headers: {} }))).resolves.toMatchObject({ url: '/save' });
  });

  it('lets the browser supply a multipart boundary while preserving the actual File', async () => {
    createAxiosBrowserAdapter(adapterOptions().options);
    const data = new FormData(); const file = new File(['owned bytes'], 'owned.xlsx'); data.append('file', file);
    const result = await getRequestInterceptor()({ data, headers: { 'Content-Type': 'application/json', repeatSubmit: false }, method: 'post', url: '/import' }) as { data: FormData; headers: Record<string, unknown> };
    expect(result.headers['Content-Type']).toBeUndefined(); expect(result.data.get('file')).toBe(file);
  });

  it('combines caller cancellation with session cancellation and removes completed listeners', async () => {
    for (const cancel of ['caller', 'session'] as const) {
      const client = createAxiosBrowserAdapter(adapterOptions().options);
      const caller = new AbortController(); const remove = vi.spyOn(caller.signal, 'removeEventListener');
      let complete!: (value: unknown) => void;
      axiosHarness.service.request.mockImplementationOnce(() => new Promise(resolve => { complete = resolve; }));
      const request = client.request({ url: '/import', method: 'post', signal: caller.signal });
      const config = axiosHarness.service.request.mock.calls.at(-1)![0] as HttpRequest;
      expect(config.signal).not.toBe(caller.signal);
      if (cancel === 'caller') caller.abort(); else client.cancelPending();
      expect(config.signal?.aborted).toBe(true);
      complete({ data: { code: 200 } }); await request;
      expect(remove).toHaveBeenCalledWith('abort', expect.any(Function));
    }
  });

  it('preserves the JSON request body and forwards typed per-request timeout', async () => {
    const client = createAxiosBrowserAdapter(adapterOptions().options);
    const body = { password: 'owned-password-canary' };
    const request = (await getRequestInterceptor()({
      data: body, headers: { repeatSubmit: false }, method: 'post', url: '/auth/login'
    })) as { data: unknown; headers: Record<string, unknown> };
    expect(request.data).toBe(body);
    expect(request.headers).not.toHaveProperty('encrypt-key');

    axiosHarness.service.request.mockResolvedValue({ data: { code: 200 } });
    const typedRequest: HttpRequest = { method: 'get', timeout: 1250, url: '/slow' };
    await client.request(typedRequest);
    expect(axiosHarness.service.request).toHaveBeenCalledWith(expect.objectContaining(typedRequest));
    expect(axiosHarness.service.request.mock.calls[0]?.[0].signal).toBeInstanceOf(AbortSignal);
  });

});

describe('axios browser response boundary', () => {
  beforeEach(() => vi.clearAllMocks());

  it('routes a real HTTP 401 through the same session recovery as a business 401', async () => {
    const onUnauthorized = vi.fn(); const { options, errorPresenter } = adapterOptions({ onUnauthorized });
    createAxiosBrowserAdapter(options);
    await expect(getResponseErrorInterceptor()({ response: { status: 401 }, message: 'Unauthorized' }))
      .rejects.toMatchObject({ kind: 'unauthorized', code: 401 });
    expect(onUnauthorized).toHaveBeenCalledOnce(); expect(errorPresenter.present).not.toHaveBeenCalled();
  });

  it('returns success and classifies business, server and warning failures', async () => {
    const { errorPresenter, options } = adapterOptions();
    createAxiosBrowserAdapter(options);
    const intercept = getResponseInterceptor();
    const success = response({ code: 200, value: 'ok' });
    expect(intercept(success)).toBe(success);

    await expect(Promise.resolve(intercept(response({ code: 400, msg: 'business-error' })))).rejects.toMatchObject({
      kind: 'business',
      code: 400,
      message: 'business-error',
      isHandled: true
    });
    await expect(Promise.resolve(intercept(response({ code: 500 })))).rejects.toMatchObject({
      kind: 'server',
      code: 500,
      message: 'server-default'
    });
    await expect(Promise.resolve(intercept(response({ code: 601, msg: 'warning-error' })))).rejects.toMatchObject({
      kind: 'warning',
      code: 601
    });
    expect(errorPresenter.present).toHaveBeenCalledTimes(3);
  });

  it('does not let presenter or recovery callback failures replace classified errors', async () => {
    const presenterFailure = new Error('presenter unavailable');
    const { options } = adapterOptions({
      errorPresenter: {
        confirmSessionExpired: vi.fn().mockRejectedValue(presenterFailure),
        present: vi.fn(() => {
          throw presenterFailure;
        })
      },
      onUnauthorized: () => {
        throw new Error('recovery unavailable');
      }
    });
    createAxiosBrowserAdapter(options);
    const intercept = getResponseInterceptor();

    await expect(Promise.resolve(intercept(response({ code: 400, msg: 'business-error' })))).rejects.toMatchObject({
      kind: 'business',
      message: 'business-error',
      isHandled: false
    });
    await expect(Promise.resolve(intercept(response({ code: 401 })))).rejects.toMatchObject({
      kind: 'unauthorized',
      code: 401,
      isHandled: false
    });
    await expect(getResponseErrorInterceptor()({ message: 'Network Error' })).rejects.toMatchObject({
      kind: 'network',
      message: '后端接口连接异常',
      isHandled: false
    });
  });

  it('does not report asynchronous unauthorized recovery as already handled', async () => {
    const onUnauthorized = vi.fn(() => Promise.reject(new Error('logout failed')));
    createAxiosBrowserAdapter(adapterOptions({ onUnauthorized }).options);

    await expect(Promise.resolve(getResponseInterceptor()(response({ code: 401 })))).rejects.toMatchObject({
      kind: 'unauthorized',
      code: 401,
      isHandled: false
    });
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it('returns JSON and text without an application encryption layer', () => {
    createAxiosBrowserAdapter(adapterOptions().options);
    const intercept = getResponseInterceptor();
    const json = { code: 200, data: { access_token: 'owned-token' } };
    const jsonResponse = response(json); const textResponse = response('plain text', {}, {}, 'text');
    expect(intercept(jsonResponse)).toBe(jsonResponse);
    expect(intercept(textResponse)).toBe(textResponse);
  });

  it('preserves binary response bytes without JSON conversion', () => {
    createAxiosBrowserAdapter(adapterOptions().options);
    const intercept = getResponseInterceptor();
    for (const [payload, responseType] of [
      [new Blob(['owned-download']), 'blob'],
      [new TextEncoder().encode('owned-download').buffer, 'arraybuffer']
    ] as const) {
      const binaryResponse = response(payload, {}, {}, responseType);
      expect(intercept(binaryResponse)).toBe(binaryResponse);
    }
  });

  it('classifies network errors with a sanitized cause and code', async () => {
    const { errorPresenter, options } = adapterOptions();
    createAxiosBrowserAdapter(options);
    const rawError = { code: 'ECONNABORTED', message: 'timeout of 50000ms exceeded' };
    await expect(getResponseErrorInterceptor()(rawError)).rejects.toMatchObject({
      kind: 'network',
      code: 'ECONNABORTED',
      message: '系统接口请求超时',
      cause: { code: 'ECONNABORTED', message: 'timeout of 50000ms exceeded' },
      isHandled: true
    });
    expect(errorPresenter.present).toHaveBeenCalledWith({ kind: 'network', message: '系统接口请求超时' });
  });
});

describe('axios browser error and download compatibility', () => {
  it('classifies network and structured response failures', async () => {
    const resolve = (code: unknown) => (code === 500 ? 'server-error' : undefined);
    await expect(extractAxiosErrorMessage({ message: 'Network Error' }, resolve)).resolves.toBe('后端接口连接异常');
    await expect(extractAxiosErrorMessage({ response: { data: { code: 500 } } }, resolve)).resolves.toBe(
      'server-error'
    );
  });

  it('downloads valid data and always closes its owner', async () => {
    const { options } = adapterOptions();
    const client = createAxiosBrowserAdapter(options);
    const bytes = new Uint8Array([1, 2, 3]);
    axiosHarness.service.post.mockResolvedValue({ data: bytes });
    const save = vi.fn();
    const close = vi.fn();
    await downloadWithAxios(downloadOptions({ client, close, isValid: () => true, save }));
    expect(save).toHaveBeenCalledWith(expect.any(Blob), 'report.xlsx');
    expect(new Uint8Array(await (save.mock.calls[0]![0] as Blob).arrayBuffer())).toEqual(bytes);
    expect(close).toHaveBeenCalledOnce();
  });

  it('preserves code, message and default precedence for download errors', async () => {
    const { options } = adapterOptions();
    const client = createAxiosBrowserAdapter(options);
    const presentError = vi.fn();
    const close = vi.fn();
    for (const [payload, expected] of [
      [{ code: 404, msg: 'payload-message' }, 'not-found'],
      [{ code: 499, msg: 'payload-message' }, 'payload-message'],
      [{ code: 499 }, 'default-error']
    ] as const) {
      axiosHarness.service.post.mockResolvedValueOnce({ data: new Blob([JSON.stringify(payload)]) });
      await downloadWithAxios(downloadOptions({ client, close, isValid: () => false, presentError }));
      expect(presentError).toHaveBeenLastCalledWith(expected);
    }
    expect(close).toHaveBeenCalledTimes(3);
  });

  it('reports download failures without skipping final close', async () => {
    const { options } = adapterOptions();
    const client = createAxiosBrowserAdapter(options);
    const failure = new Error('download failed');
    axiosHarness.service.post.mockRejectedValue(failure);
    const onError = vi.fn();
    const close = vi.fn();
    await downloadWithAxios(downloadOptions({ client, close, onError }));
    expect(onError).toHaveBeenCalledWith(failure);
    expect(close).toHaveBeenCalledOnce();
  });
});

function downloadOptions(overrides: Partial<DownloadOptions> = {}): DownloadOptions {
  const { options } = adapterOptions();
  return {
    client: createAxiosBrowserAdapter(options),
    close: vi.fn(),
    fileName: 'report.xlsx',
    isValid: () => true,
    onError: vi.fn(),
    params: { page: 1 },
    presentError: vi.fn(),
    resolveErrorCode: options.resolveErrorCode,
    save: vi.fn(),
    serializeParams: () => 'page=1',
    url: '/download',
    ...overrides
  };
}


describe('unknown axios failures', () => {
  it.each([null, undefined, 1, 'failure', { message: 123 }, { message: {} }, { response: null }])('keeps a stable rejection for %j', async failure => {
    const { options, errorPresenter } = adapterOptions();
    createAxiosBrowserAdapter(options);
    await expect(extractAxiosErrorMessage(failure, () => undefined)).resolves.toBeUndefined();
    await expect(getResponseErrorInterceptor()(failure)).rejects.toMatchObject({ kind: 'network', message: 'default-error' });
    expect(errorPresenter.present).toHaveBeenCalledWith({ kind: 'network', message: 'default-error' });
  });

  it.each([null, undefined, {}, 123])('rejects invalid download bytes and closes the owner: %j', async payload => {
    const { options } = adapterOptions(); const client = createAxiosBrowserAdapter(options);
    axiosHarness.service.post.mockResolvedValueOnce({ data: payload });
    const save = vi.fn(); const onError = vi.fn(); const close = vi.fn();
    await downloadWithAxios(downloadOptions({ client, save, onError, close, isValid: () => true }));
    expect(save).not.toHaveBeenCalled(); expect(onError).toHaveBeenCalledWith(expect.objectContaining({ message: '下载响应不可用' }));
    expect(close).toHaveBeenCalledOnce();
  });
});


describe('download byte projection', () => {
  it('copies only the selected view bytes', async () => {
    const client = createAxiosBrowserAdapter(adapterOptions().options);
    const buffer = new Uint8Array([99, 1, 2, 3, 88]).buffer;
    axiosHarness.service.post.mockResolvedValueOnce({ data: new DataView(buffer, 1, 3) });
    const save = vi.fn(); const onError = vi.fn();
    await downloadWithAxios(downloadOptions({ client, save, onError }));
    expect(onError).not.toHaveBeenCalled();
    expect(new Uint8Array(await (save.mock.calls[0]![0] as Blob).arrayBuffer())).toEqual(new Uint8Array([1, 2, 3]));
  });
  it.each([null, [], 123])('falls back safely for non-object download error JSON: %j', async payload => {
    const client = createAxiosBrowserAdapter(adapterOptions().options);
    axiosHarness.service.post.mockResolvedValueOnce({ data: new Blob([JSON.stringify(payload)]) });
    const presentError = vi.fn(); const onError = vi.fn(); const close = vi.fn();
    await downloadWithAxios(downloadOptions({ client, presentError, onError, close, isValid: () => false }));
    expect(presentError).toHaveBeenCalledWith('default-error'); expect(onError).not.toHaveBeenCalled();
    expect(close).toHaveBeenCalledOnce();
  });
});
