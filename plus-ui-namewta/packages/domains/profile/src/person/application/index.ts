export { createPersonApplicationService, type PersonApplicationResourceService } from './service';
export type { PersonApplication, PersonDraftCommand, PersonIdentity } from './types';

export const profilePersonApplicationResource = Object.freeze({
  controller: 'PersonApplicationController',
  basePath: '/profile/person/application'
});
