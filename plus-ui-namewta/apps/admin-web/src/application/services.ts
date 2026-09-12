import { createOssUploadClient, transferToOss, type OssTransfer } from '@namewta/adapter-oss-upload-browser';
import { createIdentityAccessService } from '@namewta/domain-admin';
import { createAiService } from '@namewta/domain-ai';
import { createDemoService, createRichTextService, type RichTextAssetAccess, type RichTextAssetKind } from '@namewta/domain-demo';
import { createNotificationService } from '@namewta/domain-notify';
import { createProfileService } from '@namewta/domain-profile';
import { createOpenApiService, createSystemService } from '@namewta/domain-system';
import { createMonitorService } from '@namewta/domain-system/monitor';
import { createThirdService } from '@namewta/domain-third';
import { createWorkflowDefinitionService } from '@namewta/domain-workflow';
import { adminHttp } from './http';
import { session } from './session';
import type { NotifyUserCandidatePage } from '@namewta/domain-notify';

type AdminDomainRequest = Parameters<typeof adminHttp.request>[0];

// 延迟读取适配器，避免 HTTP 恢复逻辑、Router 清单与服务组合形成初始化环。
const domainHttp = {
  request: <T>(config: AdminDomainRequest) => adminHttp.request<T>(config)
};

function createDevelopmentOssTransfer(): OssTransfer | undefined {
  const proxyPrefix = import.meta.env.VITE_APP_OSS_PROXY_PREFIX?.trim();
  if (!import.meta.env.DEV || !proxyPrefix || typeof window === 'undefined') return undefined;

  const normalizedPrefix = `/${proxyPrefix.replace(/^\/+|\/+$/g, '')}`;
  return (request, body, signal, onProgress) => {
    const signedUrl = new URL(request.url);
    const proxyUrl = new URL(`${normalizedPrefix}${signedUrl.pathname}${signedUrl.search}`, window.location.origin);
    return transferToOss({ ...request, url: proxyUrl.toString() }, body, signal, onProgress);
  };
}

export const systemService = createSystemService(domainHttp);
export const openApiService = createOpenApiService(domainHttp);

export const ossUploadClient = createOssUploadClient({
  clientId: import.meta.env.VITE_APP_CLIENT_ID,
  gateway: systemService.resources.oss,
  getToken: session.getToken,
  transfer: createDevelopmentOssTransfer()
});

export const identityAccessService = createIdentityAccessService({
  client: { clientId: import.meta.env.VITE_APP_CLIENT_ID },
  encryptLoginRequest: import.meta.env.VITE_APP_ENCRYPT === 'true',
  http: domainHttp,
  identity: systemService.identity,
  session
});

export const workflowService = createWorkflowDefinitionService(domainHttp);
export const profileService = createProfileService(domainHttp);
const richTextAssets = {
  upload: async (file: File, kind: RichTextAssetKind, options: { signal: AbortSignal; onProgress?: (percent: number) => void }) => {
    const policy = `richtext-${kind === 'attachment' ? 'file' : kind}`;
    const result = await ossUploadClient.upload(file, { policy, signal: options.signal, onProgress: options.onProgress });
    return { ossId: String(result.id), fileName: result.name };
  },
  resolve: async (ossIds: readonly string[], options: { signal: AbortSignal; richTextId?: string }) => {
    const response = await domainHttp.request<{ data?: readonly RichTextAssetAccess[] }>({
      url: '/demo/rich-text/assets',
      method: 'get',
      params: { ossIds: ossIds.join(','), richTextId: options.richTextId },
    });
    return Array.isArray(response.data) ? response.data : [];
  }
};
export const demoService = createDemoService(domainHttp, createRichTextService(domainHttp, richTextAssets));
export const notificationService = createNotificationService(domainHttp);

export const notificationDirectory = {
  searchUsers: (keyword: string, page = 1, pageSize = 20) =>
    keyword.trim()
      ? domainHttp.request<{ data: NotifyUserCandidatePage }>({
          url: '/notify/recipients/search',
          method: 'get',
          params: { pageNum: page, pageSize, keyword: keyword.trim() }
        })
      : Promise.resolve({ data: { rows: [], total: 0 } }),
  usersByIds: (ids: readonly (string | number)[]) =>
    domainHttp.request<{ data: Array<{ userId: string | number; userName?: string; nickName?: string; phoneNumber?: string; status?: string }> }>({
      url: '/notify/recipients/by-ids',
      method: 'get',
      params: { userIds: ids.join(',') }
    }),
  userTypes: () => systemService.userTypes.options()
};
export const monitorService = createMonitorService(domainHttp);
export const aiService = createAiService(domainHttp);
export const thirdService = createThirdService(domainHttp);
