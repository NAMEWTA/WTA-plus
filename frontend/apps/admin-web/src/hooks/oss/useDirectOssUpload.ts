import type { UploadProgressEvent, UploadRequestOptions } from 'element-plus';
import { ossUploadClient } from '@/application/services';

export interface DirectUploadOptions {
  signal: AbortSignal;
  onProgress?: (percent: number) => void;
  policy?: string;
}

export function getDirectOssUploadErrorMessage(error: unknown, fallback: string): string | undefined {
  if ((error as { isHandled?: boolean } | undefined)?.isHandled) return undefined;
  if (error instanceof Error && error.message.trim()) return error.message;
  return fallback;
}

export async function uploadDirectToOss(file: File, options: DirectUploadOptions) {
  const result = await ossUploadClient.upload(file, options);
  return { ossId: result.id, fileName: result.name, url: result.url };
}

function progressEvent(percent: number) {
  const event = new ProgressEvent('progress', {
    lengthComputable: true,
    loaded: percent,
    total: 100
  }) as UploadProgressEvent;
  event.percent = Math.min(100, Math.max(0, percent));
  return event;
}

export function directOssUploadRequest(options: UploadRequestOptions, policy = 'general'): XMLHttpRequest {
  const controller = new AbortController();
  const handle = new XMLHttpRequest();
  handle.abort = () => controller.abort();
  void uploadDirectToOss(options.file, {
    signal: controller.signal,
    policy,
    onProgress: percent => options.onProgress(progressEvent(percent))
  }).then(options.onSuccess, error => options.onError(error));
  return handle;
}

export const createDirectOssUploadRequest = (policy: string) => (options: UploadRequestOptions) =>
  directOssUploadRequest(options, policy);
