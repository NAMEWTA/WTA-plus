import type { ApiErrorInfo } from '@namewta/platform-contracts';

export type Identifier = string | number;
export type ProfileType = 'ENTERPRISE' | 'PERSON';
export type MaterialOwnerType = 'SOURCE' | 'SUBMISSION' | 'VERSION' | 'WORKING';

export interface ApiResponse<T = null> {
  code?: number;
  data: T;
  msg?: string;
  error?: ApiErrorInfo;
}

export interface PageResult<T> {
  rows: T[];
  total: number;
}

export interface OssAccessUrl {
  accessType: string;
  expiresAt: string | null;
  fileName: string;
  url: string;
}

export interface AccountCandidate {
  nickName: string;
  userId: Identifier;
  userName: string;
}

export interface MaterialOwnerKey {
  ownerId: Identifier;
  ownerType: MaterialOwnerType;
  profileType: ProfileType;
}

export interface MaterialReference {
  attached: boolean;
  attachedTime: string;
  detachedTime: string | null;
  fileExtension: string;
  fileName: string;
  fileSize: number;
  immutableEvidence: boolean;
  materialNodeId: Identifier;
  materialRefId: Identifier;
  materialTagCode: string;
  materialTagName: string;
  mimeType: string;
  ossId: Identifier;
  owner: MaterialOwnerKey;
  version: number;
}

export interface MaterialInput {
  materialNodeId: Identifier;
  ossId: Identifier;
}

export interface StatusProbe {
  status: string;
}

export interface ProfileBinding {
  bindingId: Identifier;
  bindingVersion: number;
  boundTime: string;
  sourceId: Identifier | null;
  sourceType: string;
  status: string;
  unboundTime: string | null;
  userId: Identifier;
}

export interface ProfileSource {
  fieldSnapshotJson: string;
  occurredTime: string;
  operatorUserId: Identifier;
  reason: string;
  sourceId: Identifier;
  sourceType: string;
}

export interface ProfileAudit {
  afterStatus: string | null;
  auditId: Identifier;
  beforeStatus: string | null;
  capability: string;
  failureCategory: string | null;
  occurredTime: string;
  operationType: string;
  operatorUserId: Identifier;
  reason: string;
  result: string;
}

export interface ReviewContext {
  applicantUserId: Identifier;
  applicationId: Identifier;
  decisionVersion: number;
  fieldSnapshotJson: string;
  materials: MaterialReference[];
  status: string;
  submissionId: Identifier;
  submissionSeq: number;
  submittedTime: string;
  version: number;
}

export interface ProfileCommandResult extends StatusProbe {
  bindingId: Identifier | null;
  profileId: Identifier;
  version: number;
  versionId: Identifier | null;
}

export interface DecisionCommand {
  decision: string;
  reason: string;
}

export interface BindingCommand {
  action: 'RESUME' | 'SUSPEND' | 'UNBIND';
  expectedBindingVersion: number;
  reason: string;
}

export interface AssignCommand {
  reason: string;
  userId: Identifier;
}

export interface RevokeCommand {
  expectedVersion: number;
  reason: string;
}
