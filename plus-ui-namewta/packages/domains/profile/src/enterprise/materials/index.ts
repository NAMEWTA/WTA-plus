export { createEnterpriseMaterialService, type EnterpriseMaterialService } from './service';
export type { Identifier, MaterialInput, MaterialOwnerType, MaterialReference, OssAccessUrl } from './types';

export const profileEnterpriseMaterialsResource = Object.freeze({
  controller: 'EnterpriseMaterialController',
  basePath: '/profile/enterprise/materials'
});
