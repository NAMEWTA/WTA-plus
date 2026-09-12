import type { DomainModule } from '@namewta/platform-app-runtime';

export { profilePermissions } from './permissions';
export {
  createProfileService,
  type EnterpriseApplicationService,
  type MaterialReferenceService,
  type PersonApplicationService,
  type ProfileService
} from './service';
export {
  projectPersonMatch,
  projectPersonMatchResponse,
  projectStatusProbe,
  projectStatusProbeResponse
} from './transport';
export type {
  AccountCandidate,
  ApiResponse,
  AssignCommand,
  BindingCommand,
  DecisionCommand,
  Identifier,
  MaterialInput,
  MaterialOwnerKey,
  MaterialOwnerType,
  MaterialReference,
  OssAccessUrl,
  PageResult,
  ProfileAudit,
  ProfileBinding,
  ProfileCommandResult,
  ProfileSource,
  ProfileType,
  ReviewContext,
  RevokeCommand,
  StatusProbe
} from './types';
export {
  createMaterialTagService,
  profileMaterialTagsResource,
  type MaterialNode,
  type MaterialNodeCommand,
  type MaterialNodeType,
  type MaterialScope,
  type MaterialTagService
} from './material-tags';
export {
  createPersonApplicationService,
  profilePersonApplicationResource,
  type PersonApplication,
  type PersonApplicationResourceService,
  type PersonDraftCommand,
  type PersonIdentity
} from './person/application';
export {
  createPersonRebindService,
  profilePersonRebindResource,
  type PersonConfirmationResult,
  type PersonMatchResult,
  type PersonRebindService,
  type PersonSubmissionResult
} from './person/rebind';
export {
  createPersonMaterialService,
  profilePersonMaterialsResource,
  type PersonMaterialService
} from './person/materials';
export {
  createPersonArchiveService,
  profilePersonArchiveResource,
  type PersonArchiveQuery,
  type PersonArchiveService,
  type PersonCreateCommand,
  type PersonProfileDetail,
  type PersonProfileSummary,
  type PersonProfileVersion,
  type PersonReviseCommand
} from './person/archive';
export {
  createEnterpriseApplicationService,
  profileEnterpriseApplicationResource,
  type EnterpriseApplication,
  type EnterpriseApplicationResourceService,
  type EnterpriseDraftCommand,
  type EnterpriseIdentity
} from './enterprise/application';
export {
  createEnterpriseTransferService,
  profileEnterpriseTransferResource,
  type EnterpriseTransferConfirmCommand,
  type EnterpriseTransferResult,
  type EnterpriseTransferSendCommand,
  type EnterpriseTransferService
} from './enterprise/transfer';
export {
  createEnterpriseMaterialService,
  profileEnterpriseMaterialsResource,
  type EnterpriseMaterialService
} from './enterprise/materials';
export {
  createEnterpriseArchiveService,
  profileEnterpriseArchiveResource,
  type EnterpriseArchiveQuery,
  type EnterpriseArchiveService,
  type EnterpriseCreateCommand,
  type EnterpriseProfileDetail,
  type EnterpriseProfileSummary,
  type EnterpriseProfileVersion,
  type EnterpriseReviseCommand
} from './enterprise/archive';

export const profileDomainModule: DomainModule = Object.freeze({
  id: 'profile',
  backendModules: Object.freeze(['wta-profile']),
  capabilities: Object.freeze([
    'material-tag',
    'person-application',
    'person-rebind',
    'person-material',
    'person-archive',
    'enterprise-application',
    'enterprise-transfer',
    'enterprise-material',
    'enterprise-archive'
  ])
});
