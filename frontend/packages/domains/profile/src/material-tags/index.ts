export { createMaterialTagService, type MaterialTagService } from './service';
export type { MaterialNode, MaterialNodeCommand, MaterialNodeType, MaterialRequirement, MaterialScope } from './types';

export const profileMaterialTagsResource = Object.freeze({
  controller: 'MaterialTagController',
  basePath: '/profile/material-tags'
});
