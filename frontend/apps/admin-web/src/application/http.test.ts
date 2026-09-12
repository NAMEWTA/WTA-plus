import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.hoisted(() => {
  process.env.VITE_APP_CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e';
});

const axiosHarness = vi.hoisted(() => {
  let createdConfig: unknown;
  let requestFulfilled: ((request: unknown) => unknown) | undefined;
  const requestUse = vi.fn((fulfilled: (request: unknown) => unknown) => {
    requestFulfilled = fulfilled;
  });
  let responseFulfilled: ((response: unknown) => unknown) | undefined;
  const responseUse = vi.fn((fulfilled: (response: unknown) => unknown) => {
    responseFulfilled = fulfilled;
  });
  const service = Object.assign(vi.fn(), {
    interceptors: {
      request: { use: requestUse },
      response: { use: responseUse }
    },
    post: vi.fn()
  });
  (service as typeof service & { request: typeof service }).request = service;
  const axios = {
    defaults: { headers: {} as Record<string, string> },
    create: vi.fn((config: unknown) => {
      createdConfig = config;
      return service;
    })
  };
  return {
    axios,
    getCreatedConfig: () => createdConfig,
    getRequestFulfilled: () => requestFulfilled,
    getResponseFulfilled: () => responseFulfilled,
    requestUse,
    responseUse,
    service
  };
});

const runtime = vi.hoisted(() => ({
  confirm: vi.fn(),
  logout: vi.fn(),
  replace: vi.fn(),
  currentRoute: { value: { fullPath: '/system/user?tab=roles' } },
  message: Object.assign(vi.fn(), { error: vi.fn() }),
  notificationError: vi.fn(),
  loadingService: vi.fn()
}));

const utilityHarness = vi.hoisted(() => ({
  blobValidate: vi.fn(),
  saveBlob: vi.fn(),
  tansParams: vi.fn(() => '')
}));

vi.mock('axios', () => ({ default: axiosHarness.axios }));
vi.mock('element-plus/es', () => ({
  ElLoading: { service: runtime.loadingService },
  ElMessage: runtime.message,
  ElMessageBox: { confirm: runtime.confirm },
  ElNotification: { error: runtime.notificationError }
}));
vi.mock('@/lang', () => ({ getLanguage: vi.fn(() => 'zh-CN') }));
vi.mock('@/application/host/cache', () => ({
  default: {
    session: {
      getJSON: vi.fn(),
      setJSON: vi.fn()
    }
  }
}));
vi.mock('@/router', () => ({
  default: {
    currentRoute: runtime.currentRoute,
    replace: runtime.replace
  }
}));
vi.mock('@/store/modules/user', () => ({ useUserStore: vi.fn(() => ({ logout: runtime.logout })) }));
vi.mock('@/utils/push', () => ({ closePush: vi.fn() }));
vi.mock('@/application/session', () => ({
  getToken: vi.fn(() => 'baseline-token'),
  removeToken: vi.fn(),
  setToken: vi.fn(),
  session: {
    clear: vi.fn(),
    getToken: vi.fn(() => 'baseline-token'),
    setToken: vi.fn()
  }
}));
vi.mock('@/utils/wta', () => ({
  blobValidate: utilityHarness.blobValidate,
  tansParams: utilityHarness.tansParams
}));
vi.mock('@/utils/save', () => ({ saveBlob: utilityHarness.saveBlob }));

import { download, extractErrorMessage, isRelogin } from './http';

type ResponseFulfilled = (response: unknown) => unknown;

const unauthorizedResponse = () => ({
  data: { code: 401, msg: 'expired' },
  headers: {},
  request: { responseType: 'json' }
});

const getResponseFulfilled = () => {
  const fulfilled = axiosHarness.getResponseFulfilled();
  expect(fulfilled).toBeDefined();
  return fulfilled as ResponseFulfilled;
};

describe('request 401 baseline', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    isRelogin.show = false;
    runtime.logout.mockResolvedValue(undefined);
    runtime.replace.mockResolvedValue(undefined);
  });

  it('shows one relogin prompt while concurrent 401 responses are pending', async () => {
    let rejectPrompt: (reason: Error) => void = () => undefined;
    runtime.confirm.mockReturnValue(
      new Promise((_resolve, reject) => {
        rejectPrompt = reject;
      })
    );
    const responseFulfilled = getResponseFulfilled();

    await expect(responseFulfilled(unauthorizedResponse())).rejects.toMatchObject({ kind: 'unauthorized', code: 401 });
    await expect(responseFulfilled(unauthorizedResponse())).rejects.toMatchObject({ kind: 'unauthorized', code: 401 });

    expect(runtime.confirm).toHaveBeenCalledOnce();
    expect(isRelogin.show).toBe(true);

    rejectPrompt(new Error('cancelled'));
    await vi.waitFor(() => expect(isRelogin.show).toBe(false));
    expect(runtime.logout).not.toHaveBeenCalled();
    expect(runtime.replace).not.toHaveBeenCalled();
  });

  it('logs out and redirects to login with the encoded current location after confirmation', async () => {
    runtime.confirm.mockResolvedValue(undefined);
    const responseFulfilled = getResponseFulfilled();

    await expect(responseFulfilled(unauthorizedResponse())).rejects.toMatchObject({ kind: 'unauthorized', code: 401 });
    await vi.waitFor(() => expect(runtime.logout).toHaveBeenCalledOnce());
    await vi.waitFor(() =>
      expect(runtime.replace).toHaveBeenCalledWith({
        path: '/login',
        query: { redirect: '%2Fsystem%2Fuser%3Ftab%3Droles' }
      })
    );
    expect(isRelogin.show).toBe(false);
  });

  it('keeps one recovery locked while logout is still pending', async () => {
    let finishLogout = () => undefined;
    runtime.confirm.mockResolvedValue(undefined);
    runtime.logout.mockReturnValue(new Promise<void>(resolve => (finishLogout = resolve)));
    const responseFulfilled = getResponseFulfilled();

    await expect(responseFulfilled(unauthorizedResponse())).rejects.toMatchObject({ kind: 'unauthorized', code: 401 });
    await vi.waitFor(() => expect(runtime.logout).toHaveBeenCalledOnce());
    expect(isRelogin.show).toBe(true);
    await expect(responseFulfilled(unauthorizedResponse())).rejects.toMatchObject({ kind: 'unauthorized', code: 401 });
    expect(runtime.confirm).toHaveBeenCalledOnce();
    finishLogout();
    await vi.waitFor(() => expect(isRelogin.show).toBe(false));
  });
});

describe('Admin HTTP adapter', () => {
  it('injects the configured client, token and language headers', async () => {
    expect(axiosHarness.getCreatedConfig()).toEqual(
      expect.objectContaining({
        headers: expect.objectContaining({ clientid: 'e5cd7e4891bf95d1d19206ce24a7b32e' })
      })
    );
    const fulfilled = axiosHarness.getRequestFulfilled();
    expect(fulfilled).toBeDefined();
    const request = await fulfilled?.({ method: 'get', url: '/system/user/getInfo', headers: {} });
    expect(request).toEqual(
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer baseline-token',
          'Content-Language': 'zh-CN'
        })
      })
    );
  });

  it('preserves default extraction and download messages without logging raw errors', async () => {
    await expect(extractErrorMessage({ response: { data: {} } })).resolves.toBe('系统未知错误，请反馈给管理员');

    const close = vi.fn();
    runtime.loadingService.mockReturnValue({ close });
    utilityHarness.blobValidate.mockReturnValue(false);
    axiosHarness.service.post.mockResolvedValueOnce(new Blob(['{"code":499}']));
    await download('/report', {}, 'report.xlsx');
    expect(runtime.message.error).toHaveBeenCalledWith('系统未知错误，请反馈给管理员');
    expect(close).toHaveBeenCalledOnce();

    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    try {
      axiosHarness.service.post.mockRejectedValueOnce(new Error('token-bearing transport object'));
      await download('/report', {}, 'report.xlsx');
      expect(consoleError).not.toHaveBeenCalled();
      expect(close).toHaveBeenCalledTimes(2);
    } finally {
      consoleError.mockRestore();
    }
  });
});

describe('login request configuration baseline', () => {
  it('validates ClientContext before passing the production client id to the login request', async () => {
    vi.stubEnv('VITE_APP_CLIENT_ID', 'e5cd7e4891bf95d1d19206ce24a7b32e');
    axiosHarness.service.mockClear();
    axiosHarness.service
      .mockResolvedValueOnce({ code: 200, data: { clientEnabled: true, registerEnabled: true } })
      .mockResolvedValueOnce({ code: 200, data: { captchaEnabled: false } })
      .mockResolvedValueOnce({ code: 200, data: { access_token: 'baseline-token' } });

    try {
      const { identityAccessService } = await import('@/application/services');
      await identityAccessService.getClientContext();
      await identityAccessService.getVerification();
      await identityAccessService.login({ username: 'baseline-user', password: 'baseline-password' });

      expect(axiosHarness.service.mock.calls.map(([request]) => request.url)).toEqual([
        '/auth/client/context',
        '/auth/code',
        '/auth/login'
      ]);
      expect(axiosHarness.service).toHaveBeenNthCalledWith(3, {
        url: '/auth/login',
        headers: {
          isToken: false,
          isEncrypt: false,
          repeatSubmit: false
        },
        method: 'post',
        data: {
          username: 'baseline-user',
          password: 'baseline-password',
          clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e',
          grantType: 'password'
        }
      });
    } finally {
      vi.unstubAllEnvs();
    }
  });
});
