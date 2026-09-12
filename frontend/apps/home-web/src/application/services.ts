import { createIdentityAccessService } from '@namewta/domain-admin';
import { createProfileService } from '@namewta/domain-profile';
import { createSystemService } from '@namewta/domain-system';
import { homeHttp } from './http';
import { session } from './session';

const domainHttp = { request: <T>(config: Parameters<typeof homeHttp.request>[0]) => homeHttp.request<T>(config) };
export const systemService = createSystemService(domainHttp);
export const identityAccessService = createIdentityAccessService({
  client: { clientId: import.meta.env.VITE_APP_CLIENT_ID },
  encryptLoginRequest: import.meta.env.VITE_APP_ENCRYPT === 'true',
  http: domainHttp,
  identity: systemService.identity,
  session
});
export const profileService = createProfileService(domainHttp);
