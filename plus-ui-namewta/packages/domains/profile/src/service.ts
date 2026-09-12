import type { HttpClient } from '@namewta/platform-contracts';
import type {
  EnterpriseTransferConfirmCommand,
  EnterpriseTransferResult,
  EnterpriseTransferSendCommand
} from './enterprise/transfer/types';
import type { PersonIdentity } from './person/application/types';
import type { PersonConfirmationResult, PersonMatchResult, PersonSubmissionResult } from './person/rebind/types';
import type { ApiResponse, StatusProbe } from './types';
import {
  createEnterpriseApplicationService,
  type EnterpriseApplicationResourceService
} from './enterprise/application/service';
import { createEnterpriseArchiveService, type EnterpriseArchiveService } from './enterprise/archive/service';
import { createEnterpriseMaterialService, type EnterpriseMaterialService } from './enterprise/materials/service';
import { createEnterpriseTransferService, type EnterpriseTransferService } from './enterprise/transfer/service';
import { createMaterialTagService, type MaterialTagService } from './material-tags/service';
import { createPersonApplicationService, type PersonApplicationResourceService } from './person/application/service';
import { createPersonArchiveService, type PersonArchiveService } from './person/archive/service';
import { createPersonMaterialService, type PersonMaterialService } from './person/materials/service';
import { createPersonRebindService, type PersonRebindService } from './person/rebind/service';

export type MaterialReferenceService = PersonMaterialService;

export interface PersonApplicationService extends PersonApplicationResourceService {
  confirmRebind(identity: PersonIdentity, expectedVersion: number): Promise<ApiResponse<PersonConfirmationResult>>;
  matchRebind(identity: PersonIdentity): Promise<ApiResponse<PersonMatchResult>>;
  probeRebind(input: { documentNumber: string; documentTypeCode: string }): Promise<ApiResponse<StatusProbe>>;
  submitRebind(expectedVersion: number): Promise<ApiResponse<PersonSubmissionResult>>;
  unbind(): Promise<ApiResponse<StatusProbe>>;
}

export interface EnterpriseApplicationService extends EnterpriseApplicationResourceService {
  confirmTransfer(input: EnterpriseTransferConfirmCommand): Promise<ApiResponse<EnterpriseTransferResult>>;
  sendTransfer(input: EnterpriseTransferSendCommand): Promise<ApiResponse<EnterpriseTransferResult>>;
  unbind(): Promise<ApiResponse<EnterpriseTransferResult>>;
}

export interface ProfileService {
  readonly enterprise: {
    readonly application: EnterpriseApplicationService;
    readonly archive: EnterpriseArchiveService;
    readonly materials: EnterpriseMaterialService;
    readonly transfer: EnterpriseTransferService;
  };
  readonly materialTags: MaterialTagService;
  readonly person: {
    readonly application: PersonApplicationService;
    readonly archive: PersonArchiveService;
    readonly materials: PersonMaterialService;
    readonly rebind: PersonRebindService;
  };
}

export function createProfileService(http: HttpClient): ProfileService {
  const personApplicationResource = createPersonApplicationService(http);
  const personRebind = createPersonRebindService(http);
  const personApplication = Object.freeze<PersonApplicationService>({
    ...personApplicationResource,
    probeRebind: personRebind.probe,
    matchRebind: personRebind.match,
    confirmRebind: personRebind.confirm,
    submitRebind: personRebind.submit,
    unbind: personRebind.unbind
  });
  const enterpriseApplicationResource = createEnterpriseApplicationService(http);
  const enterpriseTransfer = createEnterpriseTransferService(http);
  const enterpriseApplication = Object.freeze<EnterpriseApplicationService>({
    ...enterpriseApplicationResource,
    sendTransfer: enterpriseTransfer.send,
    confirmTransfer: enterpriseTransfer.confirm,
    unbind: enterpriseTransfer.unbind
  });

  return Object.freeze<ProfileService>({
    materialTags: createMaterialTagService(http),
    person: Object.freeze({
      application: personApplication,
      archive: createPersonArchiveService(http),
      materials: createPersonMaterialService(http),
      rebind: personRebind
    }),
    enterprise: Object.freeze({
      application: enterpriseApplication,
      archive: createEnterpriseArchiveService(http),
      materials: createEnterpriseMaterialService(http),
      transfer: enterpriseTransfer
    })
  });
}
