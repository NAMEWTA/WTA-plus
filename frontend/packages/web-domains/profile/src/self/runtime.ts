import type { ProfileService } from '@namewta/domain-profile';

export interface ProfileSelfWebRuntime {
  confirm(message: string): Promise<void>;
  error(message: string): void;
  hasPermission(permission: string): boolean;
  service: ProfileService;
  success(message: string): void;
  warning(message: string): void;
}

export function requireProfileSelfWebRuntime(runtime: ProfileSelfWebRuntime | undefined): ProfileSelfWebRuntime {
  if (!runtime) throw new Error('ProfileSelfWebRuntime is required');
  return runtime;
}
