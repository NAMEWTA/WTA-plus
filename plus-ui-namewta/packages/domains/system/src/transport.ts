import type { OpenApiSchema } from '@namewta/api-contracts';
import type { ResetPasswordCandidate, TemporaryPassword } from './user/types';

export { projectUserSummary as projectSystemUserTransport } from './user/public';
export type { SystemUserTransport } from './user/public';

export type ResetPasswordCandidateTransport = OpenApiSchema<'ResetPasswordCandidateVo'>;
export type TemporaryPasswordTransport = OpenApiSchema<'TemporaryPasswordVo'>;

const unavailable = (): never => {
  throw new Error('用户凭据响应不可用');
};

const requiredPassword = (value: unknown): string =>
  typeof value === 'string' && value.length > 0 ? value : unavailable();

export function projectResetPasswordCandidateTransport(value: unknown): ResetPasswordCandidate {
  if (!value || typeof value !== 'object' || Array.isArray(value)) unavailable();
  return Object.freeze({ password: requiredPassword((value as ResetPasswordCandidateTransport).password) });
}

export function projectTemporaryPasswordTransport(value: unknown): TemporaryPassword {
  if (!value || typeof value !== 'object' || Array.isArray(value)) unavailable();
  const transport = value as TemporaryPasswordTransport;
  if (!Number.isInteger(transport.expiresInSeconds) || (transport.expiresInSeconds as number) <= 0) unavailable();
  return Object.freeze({
    password: requiredPassword(transport.password),
    expiresInSeconds: transport.expiresInSeconds as number
  });
}
