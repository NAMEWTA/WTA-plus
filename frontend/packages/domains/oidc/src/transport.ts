import type { OpenApiSchema } from '@namewta/api-contracts';
import type { OidcApplicationInput } from './types';

/** 只发送管理表单允许写入的字段，额外的读取属性不会进入请求。 */
export function applicationInputTransport(input: OidcApplicationInput): OpenApiSchema<'OidcApplicationBo'> {
  return {
    name: input.name,
    redirectUris: [...input.redirectUris],
    postLogoutRedirectUris: [...input.postLogoutRedirectUris],
    allowedFields: [...input.allowedFields],
    clientAuthenticationMethod: input.clientAuthenticationMethod,
    pkceRequired: input.pkceRequired
  };
}

/** 更新附带原始字符串主键和读取版本，不经过有损 Number 转换。 */
export function applicationUpdateTransport(
  input: OidcApplicationInput & { applicationId: string; version: number }
): OpenApiSchema<'OidcApplicationBo'> {
  return { ...applicationInputTransport(input), applicationId: input.applicationId, version: input.version };
}
