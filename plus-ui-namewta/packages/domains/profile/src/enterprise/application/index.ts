export { createEnterpriseApplicationService, type EnterpriseApplicationResourceService } from './service';
export type { EnterpriseApplication, EnterpriseDraftCommand, EnterpriseIdentity } from './types';

export const profileEnterpriseApplicationResource = Object.freeze({
  controller: 'EnterpriseApplicationController',
  basePath: '/profile/enterprise/application'
});
