import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type { ApiResponse, StatusProbe } from '../../types';
import type { PersonIdentity } from '../application/types';
import type { PersonConfirmationResult, PersonMatchResult, PersonSubmissionResult } from './types';
import { projectStatusProbeResponse } from '../../transport';
import { projectPersonMatchResponse } from './transport';

export interface PersonRebindService {
  confirm(identity: PersonIdentity, expectedVersion: number): Promise<ApiResponse<PersonConfirmationResult>>;
  match(identity: PersonIdentity): Promise<ApiResponse<PersonMatchResult>>;
  probe(input: { documentNumber: string; documentTypeCode: string }): Promise<ApiResponse<StatusProbe>>;
  submit(expectedVersion: number): Promise<ApiResponse<PersonSubmissionResult>>;
  unbind(): Promise<ApiResponse<StatusProbe>>;
}

export function createPersonRebindService(http: HttpClient): PersonRebindService {
  const request = <T>(config: HttpRequest) => http.request<ApiResponse<T>>(config);
  const raw = (config: HttpRequest) => http.request<unknown>(config);
  return Object.freeze<PersonRebindService>({
    probe: data => raw({ url: '/profile/person/rebind/probe', method: 'post', data }).then(projectStatusProbeResponse),
    match: identity =>
      raw({ url: '/profile/person/rebind/match', method: 'post', data: { identity } }).then(projectPersonMatchResponse),
    confirm: (identity, expectedVersion) =>
      request({ url: '/profile/person/rebind/confirm', method: 'post', data: { identity, expectedVersion } }),
    submit: expectedVersion =>
      request({ url: '/profile/person/rebind/submit', method: 'post', data: { expectedVersion } }),
    unbind: () => request({ url: '/profile/person/rebind/unbind', method: 'post' })
  });
}
