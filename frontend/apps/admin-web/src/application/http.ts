import type { LoadingInstance } from 'element-plus';
import {
  createAxiosBrowserAdapter,
  downloadWithAxios,
  extractAxiosErrorMessage,
  isHandledError
} from '@namewta/adapter-axios-browser';
import { requestRelogin } from '@namewta/platform-auth';
import cache from '@/application/host/cache';
import { getToken } from '@/application/session';
import { HttpStatus } from '@/enums/RespEnum';
import { getLanguage } from '@/lang';
import router from '@/router';
import { errorCode } from '@/utils/errorCode';
import { blobValidate, tansParams } from '@/utils/wta';
import { saveBlob } from '@/utils/save';

let downloadLoadingInstance: LoadingInstance | undefined;
// Navigation bootstrap owns failure cleanup; it must not compete with the expiry dialog.
export const isRelogin = { show: false, navigationPending: false };

const resolveErrorCode = (code: unknown) => errorCode[String(code)];

const errorPresenter = {
  confirmSessionExpired: () =>
    ElMessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => undefined),
  present: ({ kind, message }: { kind: 'business' | 'network' | 'server' | 'warning'; message: string }) => {
    if (kind === 'business') ElNotification.error({ title: message });
    else
      ElMessage({
        message,
        type: kind === 'warning' ? 'warning' : 'error',
        ...(kind === 'network' && { duration: 5000 })
      });
  }
};

const service = createAxiosBrowserAdapter({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  client: { clientId: import.meta.env.VITE_APP_CLIENT_ID },
  errorPresenter,
  getLanguage,
  getToken,
  onUnauthorized: () =>
    isRelogin.navigationPending ? undefined : requestRelogin({
      state: isRelogin,
      presenter: errorPresenter,
      session: {
        logout: async () => {
          const { useUserStore } = await import('@/store/modules/user');
          await useUserStore().logout();
        }
      },
      navigation: {
        currentLocation: () => router.currentRoute.value.fullPath || '/',
        replaceWithLogin: async redirect => {
          await router.replace({ path: '/login', query: { redirect } });
        }
      }
    }),
  repeatSubmissions: {
    get: () => cache.session.getJSON('sessionObj'),
    set: value => cache.session.setJSON('sessionObj', value)
  },
  resolveErrorCode,
  serializeParams: tansParams,
  successCode: HttpStatus.SUCCESS
});

export const adminHttp = service;

export const isHandledRequestError = isHandledError;
export const extractErrorMessage = async (error: unknown) =>
  (await extractAxiosErrorMessage(error, resolveErrorCode)) || resolveErrorCode('default');
export const globalHeaders = () => ({
  Authorization: `Bearer ${getToken()}`,
  clientid: import.meta.env.VITE_APP_CLIENT_ID
});

export function download(url: string, params: unknown, fileName: string) {
  downloadLoadingInstance = ElLoading.service({ text: '正在下载数据，请稍候', background: 'rgba(0, 0, 0, 0.7)' });
  return downloadWithAxios({
    client: service,
    url,
    params,
    fileName,
    serializeParams: tansParams,
    isValid: blobValidate,
    save: saveBlob,
    resolveErrorCode,
    presentError: message => ElMessage.error(message),
    onError: () => undefined,
    close: () => downloadLoadingInstance?.close()
  });
}
