import type { DomainModule } from '@namewta/platform-app-runtime';
import type { HttpClient } from '@namewta/platform-contracts';
import { projectAiUserTransport, type AiUserTransport } from './transport';

export * from './transport';

export interface SnailOpenApiUser {
  openId: string;
  nickname?: string;
  externalId?: string;
  created?: boolean;
}

export interface AiApiResponse<T = unknown> {
  code?: number;
  data?: T;
  msg?: string;
}

export interface AiService {
  registerCurrentSnailUser(): Promise<AiApiResponse<SnailOpenApiUser>>;
}

export const aiDomainModule: DomainModule = Object.freeze({
  id: 'ai',
  backendModules: Object.freeze(['wta-ai']),
  capabilities: Object.freeze(['embedded-chat'])
});

export function createAiService(http: HttpClient): AiService {
  return Object.freeze({
    registerCurrentSnailUser: async () => {
      const response = await http.request<AiApiResponse<AiUserTransport>>({
        url: '/snail-ai/user/register',
        method: 'post',
        headers: { repeatSubmit: false }
      });
      return response.data
        ? { ...response, data: projectAiUserTransport(response.data) }
        : (response as AiApiResponse<SnailOpenApiUser>);
    }
  });
}
