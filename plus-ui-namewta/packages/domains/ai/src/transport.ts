import type { OpenApiSchema } from '@namewta/api-contracts';
import type { SnailOpenApiUser } from './index';

export type AiUserTransport = OpenApiSchema<'OpenApiUserVO'>;

export function projectAiUserTransport(value: AiUserTransport): SnailOpenApiUser {
  return {
    openId: value.openId ?? '',
    externalId: value.externalId,
    nickname: value.nickname,
    created: value.created
  };
}
