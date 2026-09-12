import type { CryptoPort, ErrorPresenter, HttpRequest } from '@namewta/platform-contracts';
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
    encryptionEnabled: false,
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

  it('fails closed for missing Client or CryptoPort and trims a valid Client id', () => {
    expect(() => createAxiosBrowserAdapter(adapterOptions({ client: { clientId: '   ' } }).options)).toThrow(
      'ClientContext.clientId is required'
    );
    expect(() => createAxiosBrowserAdapter(adapterOptions({ encryptionEnabled: true }).options)).toThrow(
      'CryptoPort is required when encryption is enabled'
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

  it('encrypts opted-in requests and forwards typed per-request timeout', async () => {
    const crypto: CryptoPort = {
      decryptResponse: vi.fn(),
      encryptRequest: vi.fn(() => ({ data: 'ciphertext', encryptedKey: 'wrapped-key' }))
    };
    const client = createAxiosBrowserAdapter(adapterOptions({ crypto, encryptionEnabled: true }).options);
    const encrypted = (await getRequestInterceptor()({
      data: { password: 'secret' },
      headers: { isEncrypt: 'true', repeatSubmit: false },
      method: 'post',
      url: '/auth/login'
    })) as { data: unknown; headers: Record<string, unknown> };
    expect(crypto.encryptRequest).toHaveBeenCalledWith('{"password":"secret"}');
    expect(encrypted).toMatchObject({ data: 'ciphertext', headers: { 'encrypt-key': 'wrapped-key' } });

    axiosHarness.service.request.mockResolvedValue({ code: 200 });
    const typedRequest: HttpRequest = { method: 'get', timeout: 1250, url: '/slow' };
    await client.request(typedRequest);
    expect(axiosHarness.service.request).toHaveBeenCalledWith(typedRequest);
  });
});

describe('axios browser response boundary', () => {
  beforeEach(() => vi.clearAllMocks());

  it('returns success and classifies business, server and warning failures', async () => {
    const { errorPresenter, options } = adapterOptions();
    createAxiosBrowserAdapter(options);
    const intercept = getResponseInterceptor();
    expect(intercept(response({ code: 200, value: 'ok' }))).toEqual({ code: 200, value: 'ok' });

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

  it('uses only response headers to decide response decryption', async () => {
    const crypto: CryptoPort = {
      decryptResponse: vi.fn(() => ({ code: 200, value: 'clear' })),
      encryptRequest: vi.fn()
    };
    createAxiosBrowserAdapter(adapterOptions({ crypto, encryptionEnabled: false }).options);
    let intercept = getResponseInterceptor();
    expect(intercept(response('ciphertext', { 'encrypt-key': 'wrapped-key' }))).toEqual({
      code: 200,
      value: 'clear'
    });
    expect(crypto.decryptResponse).toHaveBeenCalledWith('ciphertext', 'wrapped-key');

    expect(intercept(response('plain text', {}, {}, 'text'))).toBe('plain text');
    expect(crypto.decryptResponse).toHaveBeenCalledTimes(1);

    createAxiosBrowserAdapter(adapterOptions({ crypto, encryptionEnabled: true }).options);
    intercept = getResponseInterceptor();
    expect(intercept(response({ code: 200, value: 'plain' }, {}, { isEncrypt: true }))).toEqual({
      code: 200,
      value: 'plain'
    });
    await expect(
      Promise.resolve().then(() => intercept(response('ciphertext', { 'encrypt-key': 123 })))
    ).rejects.toMatchObject({ kind: 'encryption' });

    const cause = new Error('bad rsa key');
    vi.mocked(crypto.decryptResponse).mockImplementation(() => {
      throw cause;
    });
    await expect(
      Promise.resolve().then(() => intercept(response('ciphertext', { 'encrypt-key': 'bad-key' })))
    ).rejects.toMatchObject({
      kind: 'encryption',
      cause: { message: 'bad rsa key', name: 'Error' }
    });

    createAxiosBrowserAdapter(adapterOptions({ encryptionEnabled: false }).options);
    intercept = getResponseInterceptor();
    await expect(
      Promise.resolve().then(() => intercept(response('ciphertext', { 'encrypt-key': 'wrapped-key' })))
    ).rejects.toMatchObject({
      kind: 'encryption',
      message: 'CryptoPort is required for encrypted responses',
      isHandled: false
    });
  });

  it('fails closed when binary responses carry an encryption key', async () => {
    const crypto: CryptoPort = {
      decryptResponse: vi.fn(),
      encryptRequest: vi.fn()
    };
    createAxiosBrowserAdapter(adapterOptions({ crypto, encryptionEnabled: true }).options);
    const intercept = getResponseInterceptor();

    for (const [payload, responseType] of [
      [new Blob(['sensitive-blob']), 'blob'],
      [new TextEncoder().encode('sensitive-buffer').buffer, 'arraybuffer']
    ] as const) {
      const error = await Promise.resolve()
        .then(() => intercept(response(payload, { 'encrypt-key': 'wrapped-key' }, {}, responseType)))
        .catch(value => value);
      expect(error).toMatchObject({
        kind: 'encryption',
        message: 'Encrypted binary responses are unsupported',
        isHandled: false
      });
      expect(JSON.stringify(error)).not.toContain('sensitive');
    }
    expect(crypto.decryptResponse).not.toHaveBeenCalled();
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
    axiosHarness.service.post.mockResolvedValue(bytes);
    const save = vi.fn();
    const close = vi.fn();
    await downloadWithAxios(downloadOptions({ client, close, isValid: () => true, save }));
    expect(save).toHaveBeenCalledWith(expect.any(Blob), 'report.xlsx');
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
      axiosHarness.service.post.mockResolvedValueOnce(new Blob([JSON.stringify(payload)]));
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
