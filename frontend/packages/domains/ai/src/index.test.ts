import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, expectTypeOf, it } from 'vitest';
import { aiDomainModule, createAiService, type AiApiResponse, type SnailOpenApiUser } from './index';

describe('AI domain', () => {
  it('publishes the single traceable headless capability', () => {
    expect(aiDomainModule).toEqual({
      id: 'ai',
      backendModules: ['wta-ai'],
      capabilities: ['embedded-chat']
    });
  });

  it('preserves the current-user registration transport exactly', async () => {
    const requests: HttpRequest[] = [];
    const client: HttpClient = {
      async request<T>(request: HttpRequest): Promise<T> {
        requests.push(request);
        return { code: 200, data: { openId: 'open-user' } } as T;
      }
    };
    const service = createAiService(client);

    await expect(service.registerCurrentSnailUser()).resolves.toEqual({
      code: 200,
      data: { openId: 'open-user' }
    });
    expect(requests).toEqual([
      { url: '/snail-ai/user/register', method: 'post', headers: { repeatSubmit: false } }
    ]);
    expectTypeOf(service.registerCurrentSnailUser).toEqualTypeOf<
      () => Promise<AiApiResponse<SnailOpenApiUser>>
    >();
  });

  it('propagates registration failures without logging or inventing a user', async () => {
    const failure = new Error('registration unavailable');
    const service = createAiService({ request: async () => Promise.reject(failure) });

    await expect(service.registerCurrentSnailUser()).rejects.toBe(failure);
  });
});
