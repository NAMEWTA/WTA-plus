import type { OssAccessUrl, ProfileService } from '@namewta/domain-profile';
/** 审核端只需任务范围内的资料与操作，无需上传、用户目录或管理表单端口。 */
export interface ProfileReviewWebRuntime {
  service: ProfileService;
  hasPermission(permission: string): boolean;
  confirm(message: string): Promise<void>;
  error(message: string): void;
  success(message: string): void;
  warning(message: string): void;
  downloadMaterial(access: OssAccessUrl): Promise<void> | void;
  closeCurrentPage(): Promise<void> | void;
}
export function requireProfileReviewWebRuntime(runtime: ProfileReviewWebRuntime | undefined): ProfileReviewWebRuntime {
  if (
    !runtime ||
    !runtime.service?.person?.taskReview ||
    !runtime.service?.enterprise?.taskReview ||
    ['hasPermission', 'confirm', 'error', 'success', 'warning', 'downloadMaterial', 'closeCurrentPage'].some(
      key => typeof Reflect.get(runtime, key) !== 'function'
    )
  )
    throw new Error('ProfileReviewWebRuntime is required');
  return runtime;
}
