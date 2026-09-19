import { onScopeDispose, ref } from 'vue';
import type { UploadRequestOptions } from 'element-plus';
import type { SystemWebRuntime } from '../runtime';

/** 当前导入对话框拥有取消与反馈；HTTP 鉴权仍由宿主统一处理。 */
export function useUserImport(importUsers: SystemWebRuntime['importUsers'], updateSupport: () => boolean) {
  const busy = ref(false);
  const errorMessage = ref('');
  let controller: AbortController | undefined;
  let active = true;
  const cancel = () => {
    controller?.abort();
    controller = undefined;
    busy.value = false;
  };
  const request = (options: UploadRequestOptions): XMLHttpRequest => {
    const handle = new XMLHttpRequest();
    if (busy.value || !active) return handle;
    const attempt = new AbortController();
    controller = attempt;
    handle.abort = () => { if (controller === attempt) cancel(); };
    busy.value = true;
    errorMessage.value = '';
    const current = () => active && controller === attempt && !attempt.signal.aborted;
    void (async () => {
      let message: string;
      try {
        message = await importUsers(options.file, updateSupport(), attempt.signal);
      } catch (error) {
        if (current()) {
          errorMessage.value = error instanceof Error ? error.message : '用户导入失败，请重试';
          // Element Plus 会记录回调错误；不向其透传含请求头的底层 cause。
          options.onError(Object.assign(new Error(errorMessage.value), {
            status: 0, method: 'post', url: '/system/user/importData'
          }));
        }
        return;
      } finally {
        if (current()) busy.value = false;
      }
      if (current()) options.onSuccess({ message });
      if (controller === attempt) controller = undefined;
    })();
    return handle;
  };
  onScopeDispose(() => { active = false; cancel(); });
  return { busy, errorMessage, request, cancel };
}
