import type { Identifier } from '../../types';

export interface PersonIdentity {
  birthDate: string;
  documentNumber: string;
  documentTypeCode: string;
  fullName: string;
  gender: string;
  validFrom: string;
  validUntil: string;
}

export interface PersonApplication extends PersonIdentity {
  finishedTime: string | null;
  personApplicationId: Identifier;
  providerCode: string;
  snapshotVersion: number;
  status: string;
  submittedTime: string | null;
  version: number;
}

export interface PersonDraftCommand extends PersonIdentity {
  expectedVersion: number;
}
