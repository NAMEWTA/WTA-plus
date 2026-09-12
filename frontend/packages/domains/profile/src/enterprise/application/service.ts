import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type { ApiResponse, StatusProbe } from '../../types';
import type { EnterpriseApplication, EnterpriseDraftCommand } from './types';
import { projectStatusProbeResponse } from '../../transport';

export interface EnterpriseApplicationResourceService {
  current(): Promise<ApiResponse<EnterpriseApplication | null>>;
  probe(unifiedCreditCode: string): Promise<ApiResponse<StatusProbe>>;
  save(input: EnterpriseDraftCommand): Promise<ApiResponse<EnterpriseApplication>>;
  submit(expectedVersion: number): Promise<ApiResponse<EnterpriseApplication>>;
}

export function createEnterpriseApplicationService(http: HttpClient): EnterpriseApplicationResourceService {
  const request = <T>(config: HttpRequest) => http.request<ApiResponse<T>>(config);
  const raw = (config: HttpRequest) => http.request<unknown>(config);
  return Object.freeze<EnterpriseApplicationResourceService>({
    current: () => request({ url: '/profile/enterprise/application', method: 'get' }),
    save: data => request({ url: '/profile/enterprise/application', method: 'post', data }),
    submit: expectedVersion =>
      request({ url: '/profile/enterprise/application/submit', method: 'post', data: { expectedVersion } }),
    probe: unifiedCreditCode =>
      raw({ url: '/profile/enterprise/application/probe', method: 'post', data: { unifiedCreditCode } }).then(
        projectStatusProbeResponse
      )
  });
}
