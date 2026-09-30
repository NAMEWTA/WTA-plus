import type { EnterpriseApplication, EnterpriseIdentity } from './enterprise/application/types';
import type { PersonApplication, PersonIdentity } from './person/application/types';
import type { Identifier } from './types';
import { profileRecord, projectProfileResponse, unavailableProfileResponse } from './transport-support';

export type CertificationStatus = 'UNVERIFIED' | 'DRAFT' | 'WAITING' | 'BACK' | 'CANCEL' | 'VERIFIED';
export interface CertificationSummary<TApplication, TIdentity> {
  status: CertificationStatus;
  returnReason: string | null;
  currentApplication: TApplication | null;
  certifiedProfile: { profileId: Identifier; verifiedAt: string | null; identity: TIdentity } | null;
}
export type PersonCertificationSummary = CertificationSummary<PersonApplication, PersonIdentity>;
export type EnterpriseCertificationSummary = CertificationSummary<EnterpriseApplication, EnterpriseIdentity>;

const string = (value: unknown): string =>
  value == null ? '' : typeof value === 'string' ? value : unavailableProfileResponse();
const nullableString = (value: unknown) => (value == null ? null : string(value));
const number = (value: unknown): number =>
  typeof value === 'number' && Number.isFinite(value) ? value : unavailableProfileResponse();
const identifier = (value: unknown): Identifier =>
  (typeof value === 'string' && value.length > 0) || (typeof value === 'number' && Number.isSafeInteger(value))
    ? value
    : unavailableProfileResponse();
export function projectPersonIdentity(value: unknown): PersonIdentity {
  const source = profileRecord(value);
  return {
    fullName: string(source.fullName),
    documentTypeCode: string(source.documentTypeCode),
    documentNumber: string(source.documentNumber),
    gender: string(source.gender),
    birthDate: string(source.birthDate),
    validFrom: string(source.validFrom),
    validUntil: string(source.validUntil)
  };
}
export function projectEnterpriseIdentity(value: unknown): EnterpriseIdentity {
  const source = profileRecord(value);
  return {
    enterpriseName: string(source.enterpriseName),
    unifiedCreditCode: string(source.unifiedCreditCode),
    enterpriseType: string(source.enterpriseType),
    establishedDate: string(source.establishedDate),
    businessTermFrom: string(source.businessTermFrom),
    businessTermUntil: string(source.businessTermUntil),
    registeredAddress: string(source.registeredAddress),
    businessScope: string(source.businessScope),
    legalRepresentativeName: string(source.legalRepresentativeName),
    legalDocumentTypeCode: string(source.legalDocumentTypeCode),
    legalDocumentNumber: string(source.legalDocumentNumber),
    contactName: string(source.contactName),
    contactPhone: string(source.contactPhone),
    email: string(source.email),
    registeredCapital: source.registeredCapital == null ? 0 : number(source.registeredCapital),
    industryCode: string(source.industryCode),
    website: string(source.website)
  };
}
function applicationFields(source: Record<string, unknown>) {
  return {
    status: string(source.status),
    providerCode: string(source.providerCode),
    version: number(source.version),
    snapshotVersion: number(source.snapshotVersion),
    submittedTime: nullableString(source.submittedTime),
    finishedTime: nullableString(source.finishedTime)
  };
}
function summary<TApplication, TIdentity>(
  value: unknown,
  identity: (value: unknown) => TIdentity,
  application: (source: Record<string, unknown>) => TApplication
): CertificationSummary<TApplication, TIdentity> {
  const source = profileRecord(value);
  const status = source.status;
  if (
    status !== 'UNVERIFIED' &&
    status !== 'DRAFT' &&
    status !== 'WAITING' &&
    status !== 'BACK' &&
    status !== 'CANCEL' &&
    status !== 'VERIFIED'
  )
    return unavailableProfileResponse();
  const certified = source.certifiedProfile == null ? null : profileRecord(source.certifiedProfile);
  return {
    status,
    returnReason: nullableString(source.returnReason),
    currentApplication:
      source.currentApplication == null ? null : application(profileRecord(source.currentApplication)),
    certifiedProfile: certified
      ? {
          profileId: identifier(certified.profileId),
          verifiedAt: nullableString(certified.verifiedAt),
          identity: identity(certified.identity)
        }
      : null
  };
}
export const projectPersonSummaryResponse = (value: unknown) =>
  projectProfileResponse(value, data =>
    summary(data, projectPersonIdentity, source => ({
      ...projectPersonIdentity(source),
      ...applicationFields(source),
      personApplicationId: identifier(source.personApplicationId)
    }))
  );
export const projectEnterpriseSummaryResponse = (value: unknown) =>
  projectProfileResponse(value, data =>
    summary(data, projectEnterpriseIdentity, source => {
      if (typeof source.handlerIsLegalRepresentative !== 'boolean') return unavailableProfileResponse();
      return {
        ...projectEnterpriseIdentity(source),
        ...applicationFields(source),
        enterpriseApplicationId: identifier(source.enterpriseApplicationId),
        handlerIsLegalRepresentative: source.handlerIsLegalRepresentative
      };
    })
  );
