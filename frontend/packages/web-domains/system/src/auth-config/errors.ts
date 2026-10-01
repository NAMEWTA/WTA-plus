/** 传输适配器已把服务端业务错误转为用户消息；这里保留可读原因，不输出原始响应。 */
export function authConfigError(error: unknown, fallback: string): string {
  if (
    error &&
    typeof error === 'object' &&
    'message' in error &&
    typeof error.message === 'string' &&
    error.message.trim()
  ) {
    return error.message;
  }
  return fallback;
}
