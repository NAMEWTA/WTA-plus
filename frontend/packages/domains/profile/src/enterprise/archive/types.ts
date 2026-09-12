import type {
  Identifier,
  MaterialInput,
  MaterialReference,
  ProfileAudit,
  ProfileBinding,
  ProfileSource
} from '../../types';
import type { EnterpriseIdentity } from '../application/types';

export interface EnterpriseProfileSummary {
  bindingStatus: string | null;
  bindingUserId: Identifier | null;
  createTime: string;
  enterpriseName: string;
  enterpriseType: string;
  legalRepresentativeName: string;
  previousProfileId: Identifier | null;
  profileId: Identifier;
  status: string;
  unifiedCreditCode: string;
}

export interface EnterpriseProfileVersion extends EnterpriseIdentity {
  publishedTime: string;
  sourceId: Identifier;
  sourceType: string;
  status: string;
  versionId: Identifier;
  versionNo: number;
}

export interface EnterpriseProfileDetail {
  audits: ProfileAudit[];
  bindings: ProfileBinding[];
  currentMaterials: MaterialReference[];
  profile: EnterpriseProfileSummary;
  sources: ProfileSource[];
  versions: EnterpriseProfileVersion[];
}

export interface EnterpriseArchiveQuery {
  enterpriseName?: string;
  pageNum?: number;
  pageSize?: number;
  status?: string;
  unifiedCreditCode?: string;
}

export interface EnterpriseCreateCommand {
  bindUserId: Identifier | null;
  identity: EnterpriseIdentity;
  materials: MaterialInput[];
  reason: string;
}

export interface EnterpriseReviseCommand {
  expectedVersion: number;
  identity: EnterpriseIdentity;
  reason: string;
}
