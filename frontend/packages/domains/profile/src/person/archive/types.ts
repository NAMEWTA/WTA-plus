import type {
  Identifier,
  MaterialInput,
  MaterialReference,
  ProfileAudit,
  ProfileBinding,
  ProfileSource
} from '../../types';
import type { PersonIdentity } from '../application/types';

export interface PersonProfileSummary {
  birthDate: string;
  bindingStatus: string | null;
  bindingUserId: Identifier | null;
  createTime: string;
  documentNumber: string;
  documentTypeCode: string;
  fullName: string;
  gender: string;
  previousProfileId: Identifier | null;
  profileId: Identifier;
  status: string;
}

export interface PersonProfileVersion extends PersonIdentity {
  publishedTime: string;
  sourceId: Identifier;
  sourceType: string;
  status: string;
  versionId: Identifier;
  versionNo: number;
}

export interface PersonProfileDetail {
  audits: ProfileAudit[];
  bindings: ProfileBinding[];
  currentMaterials: MaterialReference[];
  profile: PersonProfileSummary;
  sources: ProfileSource[];
  versions: PersonProfileVersion[];
}

export interface PersonArchiveQuery {
  documentNumber?: string;
  fullName?: string;
  pageNum?: number;
  pageSize?: number;
  status?: string;
}

export interface PersonCreateCommand {
  bindUserId: Identifier | null;
  identity: PersonIdentity;
  materials: MaterialInput[];
  reason: string;
}

export interface PersonReviseCommand {
  expectedVersion: number;
  identity: PersonIdentity;
  reason: string;
}
