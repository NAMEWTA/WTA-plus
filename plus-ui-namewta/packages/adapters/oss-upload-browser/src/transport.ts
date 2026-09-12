import type { OssPresignedRequest, OssTransfer } from './types';

export const transferToOss: OssTransfer = (request: OssPresignedRequest, body, signal, onProgress) =>
  new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const abort = () => xhr.abort();
    const cleanup = () => signal.removeEventListener('abort', abort);

    xhr.open(request.method, request.url, true);
    Object.entries(request.requiredHeaders || {}).forEach(([name, value]) => xhr.setRequestHeader(name, value));
    xhr.upload.onprogress = event => {
      if (event.lengthComputable) onProgress?.({ loaded: event.loaded, total: event.total });
    };
    xhr.onload = () => {
      cleanup();
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(xhr.getResponseHeader('ETag') || undefined);
      } else {
        reject(new Error(`OSS 请求失败: HTTP ${xhr.status}`));
      }
    };
    xhr.onerror = () => {
      cleanup();
      reject(new Error('OSS 网络请求失败，请检查对象存储 Bucket 的 CORS 是否允许当前前端 Origin、PUT 方法及签名请求头'));
    };
    xhr.onabort = () => {
      cleanup();
      reject(new DOMException('上传已取消', 'AbortError'));
    };
    signal.addEventListener('abort', abort, { once: true });
    if (signal.aborted) {
      abort();
      return;
    }
    xhr.send(body);
  });
