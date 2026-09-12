import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type { ApiResponse } from '../../types';
import type {
  EnterpriseTransferConfirmCommand,
  EnterpriseTransferResult,
  EnterpriseTransferSendCommand
} from './types';

export interface EnterpriseTransferService {
  confirm(input: EnterpriseTransferConfirmCommand): Promise<ApiResponse<EnterpriseTransferResult>>;
  send(input: EnterpriseTransferSendCommand): Promise<ApiResponse<EnterpriseTransferResult>>;
  unbind(): Promise<ApiResponse<EnterpriseTransferResult>>;
}

export function createEnterpriseTransferService(http: HttpClient): EnterpriseTransferService {
  const request = <T>(config: HttpRequest) => http.request<ApiResponse<T>>(config);
  return Object.freeze<EnterpriseTransferService>({
    send: data => request({ url: '/profile/enterprise/transfer/send', method: 'post', data }),
    confirm: data => request({ url: '/profile/enterprise/transfer/confirm', method: 'post', data }),
    unbind: () => request({ url: '/profile/enterprise/transfer/unbind', method: 'post' })
  });
}
