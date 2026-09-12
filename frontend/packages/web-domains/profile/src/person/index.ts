export { default as PersonProfilePage } from './PersonProfilePage.vue';
export { default as PersonProfileDetailPage } from './PersonProfileDetailPage.vue';
export { default as PersonProfileReviewPage } from './PersonProfileReviewPage.vue';
export {
  bindingAction,
  flattenPersonMaterialOptions,
  isPersonIdentityComplete,
  personActionMatrix,
  safeErrorMessage,
  type PersonActionMatrix,
  type PersonMaterialOption
} from './logic';
export { createPersonWebContribution } from './registration';
