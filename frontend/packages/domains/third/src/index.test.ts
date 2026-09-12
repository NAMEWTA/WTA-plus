import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, it } from 'vitest';
import { createThirdService } from './index';

describe('third service contracts', () => {
  it('uses the POST status and remove routes exposed by wta-third', async () => {
    const requests: HttpRequest[] = [];
    const service = createThirdService(fixtureHttp(requests));

    await service.changeProviderStatus('provider/1', '1');
    await service.deleteProvider('provider/1');
    await service.changeEndpointStatus(2, '0');
    await service.deleteEndpoint(2);
    await service.deleteCredential(3);

    expect(requests).toEqual([
      { url: '/third/provider/provider%2F1/status', method: 'post', params: { status: '1' } },
      { url: '/third/provider/provider%2F1/remove', method: 'post' },
      { url: '/third/endpoint/2/status', method: 'post', params: { status: '0' } },
      { url: '/third/endpoint/2/remove', method: 'post' },
      { url: '/third/credential/3/remove', method: 'post' }
    ]);
  });
});

function fixtureHttp(requests: HttpRequest[]): HttpClient {
  return {
    async request<T>(request: HttpRequest): Promise<T> {
      requests.push(request);
      return { code: 200, msg: 'ok' } as T;
    }
  };
}
