import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type { OpenApiSchema } from '@namewta/api-contracts';
import type { ApiResponse } from '../../types';
import type {
  EnterpriseTransferConfirmCommand,
  EnterpriseTransferResult,
  EnterpriseTransferSendCommand
} from './types';

export interface EnterpriseTransferService {
  confirm(input: EnterpriseTransferConfirmCommand, options?: Pick<HttpRequest, 'signal'>): Promise<ApiResponse<EnterpriseTransferResult>>;
  send(input: EnterpriseTransferSendCommand, options?: Pick<HttpRequest, 'signal'>): Promise<ApiResponse<EnterpriseTransferResult>>;
  unbind(): Promise<ApiResponse<EnterpriseTransferResult>>;
}

export function createEnterpriseTransferService(http: HttpClient): EnterpriseTransferService {
  const request = async (config: HttpRequest): Promise<ApiResponse<EnterpriseTransferResult>> => {
    const result = await http.request<ApiResponse<OpenApiSchema<'EnterpriseTransferVo'>>>(config);
    const status = result.data?.status;
    switch (status) {
      case 'QUEUED': case 'TRANSFERRED': case 'NOT_AVAILABLE': case 'EXPIRED': case 'FAILED': case 'UNBOUND':
        return { ...result, data: { status, challengeId: result.data.challengeId ?? null, expiresInSeconds: result.data.expiresInSeconds ?? null } };
      default: throw new Error('企业转移返回了无法识别的状态');
    }
  };
  return Object.freeze<EnterpriseTransferService>({
    send: (data, options = {}) => request({ ...options, url: '/profile/enterprise/transfer/send', method: 'post', data: data satisfies OpenApiSchema<'EnterpriseTransferSendBo'> }),
    confirm: (data, options = {}) => request({ ...options, url: '/profile/enterprise/transfer/confirm', method: 'post', data: data satisfies OpenApiSchema<'EnterpriseTransferConfirmBo'> }),
    unbind: () => request({ url: '/profile/enterprise/transfer/unbind', method: 'post' })
  });
}
