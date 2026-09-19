import type { ClientContext, ErrorPresenter, HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { requireClientContext } from '@namewta/platform-contracts';
import {
  createTransportError,
  isHandledError,
  isTransportError,
  normalizeTransportMessage,
  payloadErrorMessage
} from '@namewta/platform-http';
import axios, { type AxiosHeaderValue, type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';

type AxiosBrowserRequest = HttpRequest & Pick<AxiosRequestConfig<unknown>, 'adapter' | 'transformRequest'>;
type AxiosBrowserPostOptions = Omit<AxiosBrowserRequest, 'url' | 'method' | 'data'>;

interface AxiosBrowserClient extends HttpClient {
  cancelPending(): void;
  (config: AxiosBrowserRequest): Promise<unknown>;
  interceptors: AxiosInstance['interceptors'];
  post(url: string, data: unknown, config?: AxiosBrowserPostOptions): Promise<unknown>;
  request<T>(config: AxiosBrowserRequest): Promise<T>;
}

function transportRecord(value: unknown): Record<string, unknown> | undefined {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : undefined;
}

function axiosRequest(config: AxiosBrowserRequest): AxiosRequestConfig<unknown> {
  const headers: Partial<Record<string, AxiosHeaderValue>> = {};
  for (const [name, value] of Object.entries(config.headers ?? {})) {
    if (typeof value === 'string' || typeof value === 'boolean' || typeof value === 'number') {
      headers[name] = value;
    } else if (value === null || value === undefined) {
      headers[name] = value === null ? null : undefined;
    } else if (Array.isArray(value) && value.every((item: unknown) => typeof item === 'string')) {
      headers[name] = value;
    } else {
      throw new Error('请求头格式不可用');
    }
  }
  return { ...config, headers };
}

export interface RepeatSubmission {
  data: unknown;
  time: number;
  url?: string;
}

export interface RepeatSubmissionStore {
  get(): RepeatSubmission | null | undefined;
  set(value: RepeatSubmission): void;
}

export interface AxiosBrowserOptions {
  baseURL: string;
  client: ClientContext;
  errorPresenter: ErrorPresenter;
  getLanguage(): string;
  getToken(): string | null;
  now?: () => number;
  onUnauthorized(): Promise<void> | void;
  repeatSubmissions: RepeatSubmissionStore;
  resolveErrorCode(code: unknown): string | undefined;
  serializeParams(params: unknown): string;
  successCode: number;
  timeout?: number;
}

async function responseDataMessage(data: unknown, resolveCode: (code: unknown) => string | undefined) {
  if (data instanceof Blob) return responseDataMessage(await data.text(), resolveCode);
  if (data instanceof ArrayBuffer) return responseDataMessage(new TextDecoder().decode(data), resolveCode);
  if (typeof data === 'string') {
    const text = data.trim();
    if (!text) return undefined;
    try {
      return responseDataMessage(JSON.parse(text), resolveCode);
    } catch {
      return text;
    }
  }
  return payloadErrorMessage(data, resolveCode);
}

export async function extractAxiosErrorMessage(
  error: unknown,
  resolveCode: (code: unknown) => string | undefined
): Promise<string | undefined> {
  if (isTransportError(error)) return error.message;
  const candidate = transportRecord(error);
  return (
    (await responseDataMessage(transportRecord(candidate?.response)?.data, resolveCode)) ??
    normalizeTransportMessage(typeof candidate?.message === 'string' ? candidate.message : undefined)
  );
}

function presentSafely(
  presenter: ErrorPresenter,
  kind: 'business' | 'network' | 'server' | 'warning',
  message: string
): boolean {
  try {
    presenter.present({ kind, message });
    return true;
  } catch {
    // Presentation is secondary to the stable transport rejection.
    return false;
  }
}

function recoverUnauthorizedSafely(recover: () => Promise<void> | void): boolean {
  try {
    const result = recover();
    if (result) {
      void result.catch((): void => undefined);
      return false;
    }
    return true;
  } catch {
    // Recovery failures must not replace the unauthorized transport error.
    return false;
  }
}

export function createAxiosBrowserAdapter(options: AxiosBrowserOptions): AxiosBrowserClient {
  let requestScope = new AbortController();
  const client = requireClientContext(options.client);
  const service = axios.create({
    baseURL: options.baseURL,
    timeout: options.timeout ?? 50000,
    headers: { 'Content-Type': 'application/json;charset=utf-8', clientid: client.clientId },
    transitional: { clarifyTimeoutError: true }
  });

  service.interceptors.request.use((config: InternalAxiosRequestConfig<unknown>) => {
    const headers = config.headers;
    // 浏览器为 FormData 写入带 boundary 的 multipart 头，不能沿用默认 JSON。
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) headers['Content-Type'] = undefined;
    headers['Content-Language'] = options.getLanguage();
    const token = options.getToken();
    if (token && headers.isToken !== false && headers.isToken !== 'false') headers.Authorization = `Bearer ${token}`;
    if (config.method === 'get' && config.params) {
      const query = options.serializeParams(config.params);
      config.url = `${config.url}?${query}`.replace(/[?&]$/, '');
      config.params = {};
    }
    const repeatDisabled = headers.repeatSubmit === false || headers.repeatSubmit === 'false';
    if (!repeatDisabled && (config.method === 'post' || config.method === 'put')) {
      const current = {
        url: config.url,
        data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
        time: (options.now ?? Date.now)()
      };
      const previous = options.repeatSubmissions.get();
      if (
        previous &&
        previous.data === current.data &&
        current.time - previous.time < 500 &&
        previous.url === current.url
      )
        return Promise.reject(new Error('数据正在处理，请勿重复提交'));
      options.repeatSubmissions.set(current);
    }
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) delete headers['Content-Type'];
    return config;
  });

  service.interceptors.response.use(
    (response: AxiosResponse<unknown>) => {
      if (response.config?.signal?.aborted) {
        return Promise.reject(createTransportError({ kind: 'network', message: '请求已取消', code: 'ERR_CANCELED', handled: true }));
      }
      const responseType = transportRecord(response.request)?.responseType ?? response.config?.responseType;
      const data = transportRecord(response.data);
      const code = Number(data?.code || options.successCode);
      const message =
        (typeof data?.msg === 'string' && data.msg) ||
        options.resolveErrorCode(code) ||
        options.resolveErrorCode('default') ||
        '';
      if (responseType === 'blob' || responseType === 'arraybuffer') return response;
      if (code === 401) {
        const handled = recoverUnauthorizedSafely(options.onUnauthorized);
        return Promise.reject(createTransportError({ kind: 'unauthorized', message, code, handled }));
      }
      if (code !== options.successCode) {
        const kind = code === 500 ? 'server' : code === 601 ? 'warning' : 'business';
        const handled = presentSafely(options.errorPresenter, kind, message);
        const error = createTransportError({ kind, message, code, handled });
        return Promise.reject(error);
      }
      return response;
    },
    async (error: unknown) => {
      if (isTransportError(error)) return Promise.reject(error);
      const candidate = transportRecord(error);
      if (candidate?.code === 'ERR_CANCELED') {
        return Promise.reject(createTransportError({ kind: 'network', message: '请求已取消', code: 'ERR_CANCELED', handled: true }));
      }
      if (transportRecord(candidate?.response)?.status === 401) {
        const handled = recoverUnauthorizedSafely(options.onUnauthorized);
        return Promise.reject(createTransportError({ kind: 'unauthorized', message: '登录状态已过期', code: 401, cause: error, handled }));
      }
      const message =
        (await extractAxiosErrorMessage(error, options.resolveErrorCode)) || options.resolveErrorCode('default') || '';
      const code = typeof candidate?.code === 'string' || typeof candidate?.code === 'number' ? candidate.code : undefined;
      const handled = presentSafely(options.errorPresenter, 'network', message);
      const transportError = createTransportError({ kind: 'network', message, code, cause: error, handled });
      return Promise.reject(transportError);
    }
  );
  const withRequestScope = <T>(config: AxiosBrowserRequest, send: (value: AxiosBrowserRequest) => Promise<T>): Promise<T> => {
    const scope = requestScope.signal;
    const caller = config.signal;
    if (!caller) return send({ ...config, signal: scope });
    // 两个 owner 都能取消；完成后移除监听，不要求浏览器提供 AbortSignal.any。
    const combined = new AbortController();
    const abort = () => combined.abort();
    scope.addEventListener('abort', abort, { once: true });
    caller.addEventListener('abort', abort, { once: true });
    if (scope.aborted || caller.aborted) combined.abort();
    const release = () => {
      scope.removeEventListener('abort', abort);
      caller.removeEventListener('abort', abort);
    };
    try {
      return send({ ...config, signal: combined.signal }).finally(release);
    } catch (error) {
      release();
      return Promise.reject(error);
    }
  };
  // Axios 拦截器保留响应合同，平台适配器在出口统一取 data。
  const request = <T>(config: AxiosBrowserRequest): Promise<T> =>
    withRequestScope(config, async value => (await service.request<T, AxiosResponse<T>, unknown>(axiosRequest(value))).data);
  return Object.assign((config: AxiosBrowserRequest) => request(config), {
    interceptors: service.interceptors,
    request,
    post: (url: string, data: unknown, config: AxiosBrowserPostOptions = {}) =>
      withRequestScope({ ...config, url, data, method: 'post' }, async value =>
        (await service.post<unknown, AxiosResponse<unknown>, unknown>(url, data, axiosRequest(value))).data),
    cancelPending: () => {
      requestScope.abort();
      requestScope = new AbortController();
    }
  });
}

export { isHandledError };

export interface DownloadOptions {
  client: AxiosBrowserClient;
  close(): void;
  fileName: string;
  isValid(data: unknown): boolean;
  onError(error: unknown): void;
  params: unknown;
  presentError(message: string): void;
  resolveErrorCode(code: unknown): string | undefined;
  save(data: Blob, fileName: string): void;
  serializeParams(params: unknown): string;
  url: string;
}

function downloadBlob(data: unknown): Blob {
  if (data instanceof Blob) return data;
  if (typeof data === 'string' || data instanceof ArrayBuffer) return new Blob([data]);
  if (ArrayBuffer.isView(data)) {
    const bytes = new Uint8Array(data.byteLength);
    bytes.set(new Uint8Array(data.buffer, data.byteOffset, data.byteLength));
    return new Blob([bytes]);
  }
  throw new Error('下载响应不可用');
}

export async function downloadWithAxios(options: DownloadOptions): Promise<void> {
  try {
    const data = await options.client.post(options.url, options.params, {
      transformRequest: [(params: unknown) => options.serializeParams(params)],
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      responseType: 'blob'
    });
    const blob = downloadBlob(data);
    if (options.isValid(data)) options.save(blob, options.fileName);
    else {
      const payload = transportRecord(JSON.parse(await blob.text()));
      options.presentError(
        options.resolveErrorCode(payload?.code) ||
          (typeof payload?.msg === 'string' ? payload.msg : '') ||
          options.resolveErrorCode('default') ||
          '系统未知错误'
      );
    }
  } catch (error) {
    options.onError(error);
  } finally {
    options.close();
  }
}
