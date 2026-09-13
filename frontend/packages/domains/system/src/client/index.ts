export type { ClientForm, ClientQuery, ClientVO } from './types';
export { isSsoRegistered, ssoAccessState, type SsoAccessState } from './sso-access';

export const systemClientResource = Object.freeze({ controller: 'SysClientController', basePath: '/system/client' });
export const systemSsoAppResource = Object.freeze({ controller: 'SysSsoAppController', basePath: '/system/ssoApp' });
