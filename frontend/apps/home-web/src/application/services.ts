import { createOssUploadClient } from '@namewta/adapter-oss-upload-browser';
import { createIdentityAccessService } from '@namewta/domain-admin';
import { createProfileService } from '@namewta/domain-profile';
import { createSystemService } from '@namewta/domain-system';
import { homeHttp } from './http';
import { session } from './session';

const domainHttp = { request: <T>(config: Parameters<typeof homeHttp.request>[0]) => homeHttp.request<T>(config) };
export const systemService = createSystemService(domainHttp);
export const identityAccessService = createIdentityAccessService({
  client: { clientId: import.meta.env.VITE_APP_CLIENT_ID },
  http: domainHttp,
  identity: systemService.identity,
  session
});
export const profileService = createProfileService(domainHttp);

const materialUploadClient = createOssUploadClient({
  clientId: import.meta.env.VITE_APP_CLIENT_ID,
  getToken: session.getToken,
  gateway: {
    ...systemService.resources.oss,
    // Home 没有 OSS 管理下载权限；登记后由 Profile owner 接口签发访问地址。
    downloadUrl: async () => ({ data: { url: '', accessType: 'PRIVATE', expiresAt: null, fileName: '' } })
  }
});

export async function uploadProfileMaterial(file: File, options: { signal: AbortSignal; onProgress(percent: number): void }) {
  const result = await materialUploadClient.upload(file, { ...options, policy: 'general' });
  return { ossId: String(result.id) };
}
