export { createPersonRebindService, type PersonRebindService } from './service';
export { projectStatusProbe, projectStatusProbeResponse } from '../../transport';
export { projectPersonMatch, projectPersonMatchResponse } from './transport';
export type { PersonConfirmationResult, PersonMatchResult, PersonSubmissionResult } from './types';

export const profilePersonRebindResource = Object.freeze({
  controller: 'PersonRebindController',
  basePath: '/profile/person/rebind'
});
