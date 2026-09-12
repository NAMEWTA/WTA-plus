import type { ApiResponse } from './types';

export function unavailableProfileResponse(): never {
  throw new Error('Profile 响应不可用');
}

export function profileRecord(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return unavailableProfileResponse();
  return value as Record<string, unknown>;
}

export function strictProfileRecord(value: unknown, keys: readonly string[]): Record<string, unknown> {
  const source = profileRecord(value);
  if (Object.keys(source).some(key => !keys.includes(key))) return unavailableProfileResponse();
  return source;
}

export function requiredProfileString(value: unknown): string {
  if (typeof value !== 'string' || value.length === 0) return unavailableProfileResponse();
  return value;
}

export function projectProfileResponse<T>(value: unknown, project: (data: unknown) => T): ApiResponse<T> {
  const source = profileRecord(value);
  if (source.code !== undefined && !Number.isInteger(source.code)) return unavailableProfileResponse();
  if (source.msg !== undefined && source.msg !== null && typeof source.msg !== 'string') {
    return unavailableProfileResponse();
  }
  const result: ApiResponse<T> = { data: project(source.data) };
  if (typeof source.code === 'number') result.code = source.code;
  if (typeof source.msg === 'string') result.msg = source.msg;
  return Object.freeze(result);
}
