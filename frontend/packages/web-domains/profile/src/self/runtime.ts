import type { ProfileService } from '@namewta/domain-profile';

export interface ProfileSelfWebRuntime {
  confirm(message: string): Promise<void>;
  error(message: string): void;
  hasPermission(permission: string): boolean;
  service: ProfileService;
  success(message: string): void;
  /** 仅上传私有材料；归属登记与访问仍由 Profile 服务校验。 */
  uploadMaterial(file: File, options: { signal: AbortSignal; onProgress(percent: number): void }): Promise<{ ossId: string }>;
  warning(message: string): void;
}

export function requireProfileSelfWebRuntime(runtime: ProfileSelfWebRuntime | undefined): ProfileSelfWebRuntime {
  if (!runtime) throw new Error('ProfileSelfWebRuntime is required');
  return runtime;
}
