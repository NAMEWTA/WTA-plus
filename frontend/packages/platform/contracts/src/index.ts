export interface ClientContext {
  clientId: string;
}

export interface StoragePort<T> {
  get(): T | null;
  remove(): void;
  set(value: T): void;
}

export type TokenStorage = StoragePort<string>;

export interface SessionStore {
  clear(): void;
  getToken(): string | null;
  setToken(token: string): void;
}

export type HttpMethod = 'delete' | 'get' | 'patch' | 'post' | 'put';

export interface HttpRequest {
  data?: unknown;
  headers?: Readonly<Record<string, unknown>>;
  method: HttpMethod;
  params?: unknown;
  responseType?: 'arraybuffer' | 'blob' | 'json' | 'text';
  timeout?: number;
  /** 调用者取消只结束本次请求；宿主退出还可取消整个会话范围。 */
  signal?: AbortSignal;
  url: string;
}

export interface HttpClient {
  request<T>(request: HttpRequest): Promise<T>;
}

export type ErrorKind = 'business' | 'network' | 'server' | 'warning';

export interface PresentedError {
  kind: ErrorKind;
  message: string;
}

export interface ApiErrorInfo {
  code: string;
  args?: Readonly<Record<string, unknown>>;
  field?: string;
  violations?: readonly ApiErrorInfo[];
}

export interface ErrorPresenter {
  confirmSessionExpired(): Promise<void>;
  present(error: PresentedError): void;
}

export interface NavigationPort {
  currentLocation(): string;
  replaceWithLogin(redirect: string): Promise<void> | void;
}

export interface SessionPort {
  logout(): Promise<void>;
}

export type UploadIdentifier = string | number;

export interface UploadItem {
  id?: UploadIdentifier;
  name: string;
  url: string;
  uid?: string | number;
}

export interface UploadResult {
  id: string;
  name: string;
  /** 空串表示对象已完成上传，但暂时无法获取预览地址；可通过 resolve 重试。 */
  url: string;
}

export interface UploadClient {
  upload(
    file: File,
    options: {
      policy?: string;
      signal: AbortSignal;
      onProgress?: (percent: number) => void;
    }
  ): Promise<UploadResult>;
  resolve(ids: readonly UploadIdentifier[]): Promise<readonly UploadItem[]>;
  remove(id: UploadIdentifier): Promise<void>;
}

export function requireClientContext(context: ClientContext): ClientContext {
  if (typeof context.clientId !== 'string' || context.clientId.trim() === '') {
    throw new Error('ClientContext.clientId is required');
  }
  return { clientId: context.clientId.trim() };
}
