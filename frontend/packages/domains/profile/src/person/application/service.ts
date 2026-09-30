import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type { ApiResponse } from '../../types';
import type { PersonApplication, PersonDraftCommand } from './types';
import { projectPersonSummaryResponse, type PersonCertificationSummary } from '../../self-summary';

export interface PersonApplicationResourceService {
  current(): Promise<ApiResponse<PersonApplication | null>>;
  summary(): Promise<ApiResponse<PersonCertificationSummary>>;
  save(input: PersonDraftCommand): Promise<ApiResponse<PersonApplication>>;
  submit(expectedVersion: number): Promise<ApiResponse<PersonApplication>>;
}

export function createPersonApplicationService(http: HttpClient): PersonApplicationResourceService {
  const request = <T>(config: HttpRequest) => http.request<ApiResponse<T>>(config);
  return Object.freeze<PersonApplicationResourceService>({
    current: () => request({ url: '/profile/person/application', method: 'get' }),
    summary: () =>
      http
        .request<unknown>({ url: '/profile/person/application/summary', method: 'get' })
        .then(projectPersonSummaryResponse),
    save: data => request({ url: '/profile/person/application', method: 'post', data }),
    submit: expectedVersion =>
      request({ url: '/profile/person/application/submit', method: 'post', data: { expectedVersion } })
  });
}
