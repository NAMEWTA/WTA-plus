import { createAxiosBrowserAdapter, extractAxiosErrorMessage, isHandledError } from '@namewta/adapter-axios-browser';
import { createBrowserCryptoAdapter } from '@namewta/adapter-crypto-browser';
import { requestRelogin } from '@namewta/platform-auth';
import { ElMessage, ElMessageBox } from 'element-plus';
import router from '@/router';
import { getToken, removeToken, session } from './session';

const errorCodes: Record<string, string> = { '401': '认证失败，请重新登录', default: '请求失败，请稍后重试' };
const encryptionEnabled = import.meta.env.VITE_APP_ENCRYPT === 'true';
const crypto = encryptionEnabled
  ? createBrowserCryptoAdapter({ publicKey: import.meta.env.VITE_APP_RSA_PUBLIC_KEY, privateKey: import.meta.env.VITE_APP_RSA_PRIVATE_KEY })
  : undefined;
const serializeParams = (params: unknown) => {
  if (!params || typeof params !== 'object') return '';
  const search = new URLSearchParams();
  Object.entries(params as Record<string, unknown>).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    if (Array.isArray(value)) value.forEach(item => search.append(key, String(item)));
    else search.set(key, String(value));
  });
  return search.toString();
};
const relogin = { show: false };
const presenter = {
  confirmSessionExpired: () => ElMessageBox.confirm('登录状态已过期，是否重新登录？', '系统提示').then(() => undefined),
  present: ({ message }: { message: string }) => ElMessage.error(message)
};

const service = createAxiosBrowserAdapter({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  client: { clientId: import.meta.env.VITE_APP_CLIENT_ID },
  crypto,
  encryptionEnabled,
  errorPresenter: presenter,
  getLanguage: () => 'zh-CN',
  getToken,
  onUnauthorized: () => requestRelogin({
    state: relogin,
    presenter,
    session: { logout: async () => session.clear() },
    navigation: {
      currentLocation: () => router.currentRoute.value.fullPath || '/',
      replaceWithLogin: async redirect => {
        await router.replace({ path: '/login', query: { redirect } });
      }
    }
  }),
  repeatSubmissions: { get: () => null, set: () => undefined },
  resolveErrorCode: code => errorCodes[String(code)],
  serializeParams,
  successCode: 200
});

export const homeHttp = service;
export const isHandledRequestError = isHandledError;
export const extractErrorMessage = (error: unknown) => extractAxiosErrorMessage(error, code => errorCodes[String(code)]);
export { removeToken };
