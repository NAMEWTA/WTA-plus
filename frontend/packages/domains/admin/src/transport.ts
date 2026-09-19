import type { OpenApiSchema } from '@namewta/api-contracts';
import { parsePasswordPolicy, type ClientAuthContext } from './password-policy';

export type ClientAuthContextTransport = OpenApiSchema<'AuthClientContextVo'>;

export function projectClientAuthContextTransport(input: unknown): ClientAuthContext {
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw new Error('客户端认证配置不可用');
  const value = input as Record<string, unknown>;
  for (const flag of [value.clientEnabled, value.registerEnabled]) {
    if (flag !== undefined && typeof flag !== 'boolean') throw new Error('客户端认证配置不可用');
  }
  return Object.freeze({
    clientEnabled: value.clientEnabled === true,
    registerEnabled: value.registerEnabled === true,
    ...(value.passwordPolicy === undefined ? {} : { passwordPolicy: parsePasswordPolicy(value.passwordPolicy) })
  });
}
