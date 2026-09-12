export { createEnterpriseArchiveService, type EnterpriseArchiveService } from './service';
export type {
  EnterpriseArchiveQuery,
  EnterpriseCreateCommand,
  EnterpriseProfileDetail,
  EnterpriseProfileSummary,
  EnterpriseProfileVersion,
  EnterpriseReviseCommand
} from './types';

export const profileEnterpriseArchiveResource = Object.freeze({
  controller: 'EnterpriseAdminController',
  basePath: '/profile/enterprise/archive'
});
