import type { OpenApiSchema } from '@namewta/api-contracts';
import { parsePasswordPolicy, type ClientAuthContext } from './password-policy';

export type ClientAuthContextTransport = OpenApiSchema<'AuthClientContextVo'>;

export function projectClientAuthContextTransport(value: ClientAuthContextTransport): ClientAuthContext {
  return Object.freeze({
    clientEnabled: value.clientEnabled === true,
    registerEnabled: value.registerEnabled === true,
    ...(value.passwordPolicy === undefined ? {} : { passwordPolicy: parsePasswordPolicy(value.passwordPolicy) })
  });
}
