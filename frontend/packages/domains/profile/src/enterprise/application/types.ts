import type { Identifier } from '../../types';

export interface EnterpriseIdentity {
  businessScope: string;
  businessTermFrom: string;
  businessTermUntil: string;
  contactName: string;
  contactPhone: string;
  email: string;
  enterpriseName: string;
  enterpriseType: string;
  establishedDate: string;
  industryCode: string;
  legalDocumentNumber: string;
  legalDocumentTypeCode: string;
  legalRepresentativeName: string;
  registeredAddress: string;
  registeredCapital: number;
  unifiedCreditCode: string;
  website: string;
}

export interface EnterpriseApplication extends EnterpriseIdentity {
  enterpriseApplicationId: Identifier;
  finishedTime: string | null;
  handlerIsLegalRepresentative: boolean;
  providerCode: string;
  snapshotVersion: number;
  status: string;
  submittedTime: string | null;
  version: number;
}

export interface EnterpriseDraftCommand extends EnterpriseIdentity {
  expectedVersion: number;
  handlerIsLegalRepresentative: boolean;
}
