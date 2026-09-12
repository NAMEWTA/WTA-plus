import type { UploadRequestHandler, UploadRequestOptions, UploadProgressEvent } from 'element-plus';
import type { UploadClient } from '@namewta/platform-contracts';

function progressEvent(percent: number): UploadProgressEvent {
  const event = new ProgressEvent('progress', { lengthComputable: true, loaded: percent, total: 100 }) as UploadProgressEvent;
  event.percent = Math.min(100, Math.max(0, percent));
  return event;
}

export function createUploadRequest(client: UploadClient, policy: string, onPendingChange: (delta: number) => void): UploadRequestHandler {
  return (options: UploadRequestOptions) => {
    const controller = new AbortController();
    const handle = new XMLHttpRequest();
    handle.abort = () => controller.abort();
    onPendingChange(1);
    void client
      .upload(options.file, {
        signal: controller.signal,
        policy,
        onProgress: percent => options.onProgress(progressEvent(percent))
      })
      .then(result => options.onSuccess(result), error => options.onError(error))
      .finally(() => onPendingChange(-1));
    return handle;
  };
}
