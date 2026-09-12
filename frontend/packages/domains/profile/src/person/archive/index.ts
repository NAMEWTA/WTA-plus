export { createPersonArchiveService, type PersonArchiveService } from './service';
export type {
  PersonArchiveQuery,
  PersonCreateCommand,
  PersonProfileDetail,
  PersonProfileSummary,
  PersonProfileVersion,
  PersonReviseCommand
} from './types';

export const profilePersonArchiveResource = Object.freeze({
  controller: 'PersonAdminController',
  basePath: '/profile/person/archive'
});
