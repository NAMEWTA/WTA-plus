export { createPersonMaterialService, type PersonMaterialService } from './service';
export type { Identifier, MaterialInput, MaterialOwnerType, MaterialReference, OssAccessUrl } from './types';

export const profilePersonMaterialsResource = Object.freeze({
  controller: 'PersonMaterialController',
  basePath: '/profile/person/materials'
});
