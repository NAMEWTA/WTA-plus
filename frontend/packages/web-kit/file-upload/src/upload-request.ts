import type { UploadRequestHandler, UploadRequestOptions, UploadProgressEvent } from 'element-plus';
import type { UploadClient } from '@namewta/platform-contracts';

function progressEvent(percent: number): UploadProgressEvent {
  const event = new ProgressEvent('progress', { lengthComputable: true, loaded: percent, total: 100 }) as UploadProgressEvent;
  event.percent = Math.min(100, Math.max(0, percent));
  return event;
}

export function createUploadRequest(
  client: UploadClient,
  policy: string,
  onPendingChange: (delta: number) => void,
  ownerSignal?: AbortSignal
): UploadRequestHandler {
  return (options: UploadRequestOptions) => {
    const controller = new AbortController();
    const handle = new XMLHttpRequest();
    let settled = false;
    const finish = () => {
      if (settled) return false;
      settled = true;
      ownerSignal?.removeEventListener('abort', abort);
      onPendingChange(-1);
      return true;
    };
    const abort = () => { controller.abort(); finish(); };
    handle.abort = abort;
    onPendingChange(1);
    ownerSignal?.addEventListener('abort', abort, { once: true });
    if (ownerSignal?.aborted) abort();
    void (async () => {
      let result;
      try {
        if (settled) return;
        result = await client.upload(options.file, {
          signal: controller.signal,
          policy,
          onProgress: percent => { if (!settled) options.onProgress(progressEvent(percent)); }
        });
      } catch (error) {
        if (finish()) {
          const failure = new Error(error instanceof Error ? error.message : '上传文件失败');
          // Element Plus 会记录回调错误；不向其透传含请求头的底层 cause。
          options.onError(Object.assign(failure, { status: 0, method: options.method, url: options.action }));
        }
        return;
      }
      if (finish()) options.onSuccess(result);
    })();
    return handle;
  };
}
