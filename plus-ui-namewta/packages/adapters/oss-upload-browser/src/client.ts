import type { UploadClient, UploadIdentifier, UploadItem, UploadResult } from '@namewta/platform-contracts';
import { createOssFileFingerprint } from './fingerprint';
import { createIndexedDbResumeStore } from './resume-store';
import { transferToOss } from './transport';
import type {
  OssCompletedPart,
  OssPresignedRequest,
  OssResumeRecord,
  OssSignedPart,
  OssUploadBrowserDependencies,
  OssUploadGateway,
  OssUploadInitResponse,
  OssUploadResumeResponse
} from './types';

const DEFAULT_POLICY = 'general';
const SIGN_WINDOW = 8;
const MAX_PART_ATTEMPTS = 3;
const STALE_RESUME_CODES = [
  'SESSION_NOT_FOUND',
  'SESSION_EXPIRED',
  'INVALID_STATE',
  'FINGERPRINT_MISMATCH',
  'SESSION_OWNER_MISMATCH'
];
const STALE_RESUME_MESSAGES = ['上传会话不存在', '上传会话已过期', '上传会话不可再使用', '文件指纹不匹配', '上传会话不属于'];

export interface OssUploadBrowserOptions extends OssUploadBrowserDependencies {
  clientId: string;
  getToken: () => string | null | undefined;
  gateway: OssUploadGateway;
}

export class OssUploadError extends Error {
  constructor(
    message: string,
    readonly code: 'aborted' | 'contract' | 'network' | 'server' | 'unknown'
  ) {
    super(message);
    this.name = 'OssUploadError';
  }
}

function requireData<T>(value: T | undefined | null, message: string): T {
  if (value === undefined || value === null) throw new OssUploadError(message, 'contract');
  return value;
}

function requireOssId(value: unknown, message: string): string {
  if ((typeof value !== 'string' && typeof value !== 'number') || !String(value).trim()) {
    throw new OssUploadError(message, 'contract');
  }
  return String(value);
}

function requireClientId(value: string): string {
  if (!value.trim()) throw new Error('OSS upload clientId is required');
  return value.trim();
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError';
}

export function getUploadErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message.trim()) return error.message;
  return fallback;
}

async function resumeKey(clientId: string, getToken: () => string | null | undefined, fingerprint: string, policy: string) {
  const identity = `${clientId}:${getToken() || 'anonymous'}:${policy}:${fingerprint}`;
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(identity));
  return Array.from(new Uint8Array(digest), value => value.toString(16).padStart(2, '0')).join('');
}

async function resolveUploadResult(
  gateway: OssUploadGateway,
  file: File,
  ossId: string
): Promise<UploadResult> {
  const download = await gateway.downloadUrl(ossId).catch(() => undefined);
  return { id: ossId, name: file.name, url: download?.data?.url || URL.createObjectURL(file) };
}

async function safeRemoveResume(store: OssUploadBrowserDependencies['resumeStore'], key: string) {
  try {
    await store?.remove(key);
  } catch {
    // IndexedDB 不可用时不阻断当前上传。
  }
}

async function findResume(
  gateway: OssUploadGateway,
  store: OssUploadBrowserDependencies['resumeStore'],
  file: File,
  fingerprint: string,
  storageKey: string
): Promise<OssUploadResumeResponse | undefined> {
  let record: OssResumeRecord | undefined;
  try {
    record = await store?.get(storageKey);
  } catch {
    return undefined;
  }
  if (!record || Date.parse(record.expiresAt) <= Date.now()) {
    await safeRemoveResume(store, storageKey);
    return undefined;
  }
  try {
    const response = await gateway.resumeUpload(record.uploadToken, fingerprint);
    const session = response.data;
    if (!session || session.fileName !== file.name || session.fileSize !== file.size) {
      await safeRemoveResume(store, storageKey);
      return undefined;
    }
    return session;
  } catch (error) {
    const message = error instanceof Error ? error.message : '';
    if (
      STALE_RESUME_CODES.some(code => message.includes(code)) ||
      STALE_RESUME_MESSAGES.some(text => message.includes(text))
    ) {
      await safeRemoveResume(store, storageKey);
      return undefined;
    }
    throw error;
  }
}

async function initialize(
  gateway: OssUploadGateway,
  store: OssUploadBrowserDependencies['resumeStore'],
  file: File,
  fingerprint: string,
  storageKey: string,
  policy: string
): Promise<OssUploadInitResponse> {
  const response = await gateway.initUpload({
    policy,
    fileName: file.name,
    fileSize: file.size,
    contentType: file.type || 'application/octet-stream',
    fingerprint
  });
  const session = requireData(response.data, '初始化 OSS 上传失败');
  try {
    await store?.put({
      fingerprint: storageKey,
      uploadToken: session.uploadToken,
      expiresAt: session.expiresAt,
      fileName: file.name,
      fileSize: file.size,
      contentType: file.type || 'application/octet-stream'
    });
  } catch {
    // IndexedDB 不可用时仍允许当前页面完成上传。
  }
  return session;
}

async function uploadSingle(
  transfer: NonNullable<OssUploadBrowserDependencies['transfer']>,
  file: File,
  session: OssUploadInitResponse,
  signal: AbortSignal,
  onProgress?: (percent: number) => void
) {
  const request = requireData(session.presignedRequest, 'SINGLE 上传未返回签名请求');
  await transfer(request, file, signal, progress => onProgress?.((progress.loaded / file.size) * 100));
  return [] as OssCompletedPart[];
}

async function uploadSignedPart(
  gateway: OssUploadGateway,
  transfer: NonNullable<OssUploadBrowserDependencies['transfer']>,
  file: File,
  uploadToken: string,
  partSize: number,
  signed: OssSignedPart,
  loadedByPart: Map<number, number>,
  signal: AbortSignal,
  onProgress?: (percent: number) => void
): Promise<OssCompletedPart> {
  const start = (signed.partNumber - 1) * partSize;
  const body = file.slice(start, Math.min(file.size, start + partSize));
  let request = signed;
  let lastError: unknown;
  for (let attempt = 1; attempt <= MAX_PART_ATTEMPTS; attempt++) {
    try {
      const eTag = await transfer(request, body, signal, progress => {
        loadedByPart.set(signed.partNumber, progress.loaded);
        const loaded = Array.from(loadedByPart.values()).reduce((total, value) => total + value, 0);
        onProgress?.((loaded / file.size) * 100);
      });
      if (!eTag) throw new OssUploadError('OSS 响应未暴露 ETag', 'contract');
      loadedByPart.set(signed.partNumber, body.size);
      return { partNumber: signed.partNumber, eTag };
    } catch (error) {
      if (signal.aborted || isAbortError(error)) throw error;
      lastError = error;
      loadedByPart.set(signed.partNumber, 0);
      if (attempt < MAX_PART_ATTEMPTS) {
        const response = await gateway.signParts(uploadToken, [signed.partNumber]);
        request = requireData(response.data?.parts?.[0], 'Part 重新签名失败');
      }
    }
  }
  throw lastError;
}

async function uploadMultipart(
  gateway: OssUploadGateway,
  transfer: NonNullable<OssUploadBrowserDependencies['transfer']>,
  file: File,
  session: OssUploadInitResponse | OssUploadResumeResponse,
  signal: AbortSignal,
  onProgress?: (percent: number) => void
) {
  const partSize = requireData(session.partSize, 'Multipart partSize 缺失');
  const partCount = requireData(session.partCount, 'Multipart partCount 缺失');
  const uploaded = 'uploadedParts' in session ? session.uploadedParts : [];
  const completed = new Map<number, OssCompletedPart>(uploaded.map(part => [part.partNumber, { partNumber: part.partNumber, eTag: part.eTag }]));
  const loadedByPart = new Map<number, number>(uploaded.map(part => [part.partNumber, part.size]));
  const missing = Array.from({ length: partCount }, (_, index) => index + 1).filter(part => !completed.has(part));
  let reportedProgress = (Array.from(loadedByPart.values()).reduce((total, value) => total + value, 0) / file.size) * 100;
  onProgress?.(reportedProgress);
  const stableProgress = (percent: number) => {
    reportedProgress = Math.max(reportedProgress, percent);
    onProgress?.(reportedProgress);
  };

  for (let offset = 0; offset < missing.length; offset += SIGN_WINDOW) {
    const numbers = missing.slice(offset, offset + SIGN_WINDOW);
    const response = await gateway.signParts(session.uploadToken, numbers);
    const signedParts = requireData(response.data?.parts, 'Part 签名失败');
    const settled = await Promise.allSettled(signedParts.map(part => uploadSignedPart(gateway, transfer, file, session.uploadToken, partSize, part, loadedByPart, signal, stableProgress)));
    const failure = settled.find(item => item.status === 'rejected');
    settled.forEach(item => {
      if (item.status === 'fulfilled') completed.set(item.value.partNumber, item.value);
    });
    if (failure?.status === 'rejected') throw failure.reason;
  }
  const ordered = Array.from(completed.values());
  // 保持 Chrome 87 兼容性，避免依赖 ES2023 的 toSorted。
  for (let index = 1; index < ordered.length; index++) {
    const current = ordered[index];
    let position = index - 1;
    while (position >= 0 && ordered[position].partNumber > current.partNumber) {
      ordered[position + 1] = ordered[position];
      position -= 1;
    }
    ordered[position + 1] = current;
  }
  return ordered;
}

export function createOssUploadClient(options: OssUploadBrowserOptions): UploadClient {
  const clientId = requireClientId(options.clientId);
  const gateway = options.gateway;
  const fingerprint = options.fingerprint || createOssFileFingerprint;
  const resumeStore = options.resumeStore || createIndexedDbResumeStore();
  const transfer = options.transfer || transferToOss;

  return {
    async upload(file, uploadOptions) {
      const fingerprintValue = await fingerprint(file);
      const policy = uploadOptions.policy || DEFAULT_POLICY;
      const storageKey = await resumeKey(clientId, options.getToken, fingerprintValue, policy);
      let uploadToken: string | undefined;
      try {
        const session = (await findResume(gateway, resumeStore, file, fingerprintValue, storageKey)) || (await initialize(gateway, resumeStore, file, fingerprintValue, storageKey, policy));
        uploadToken = session.uploadToken;
        if ('state' in session && (session as OssUploadResumeResponse).state === 'COMPLETED') {
          const completedId = requireOssId((session as OssUploadResumeResponse).completedOssId, '已完成的 OSS 上传缺少 ossId');
          await safeRemoveResume(resumeStore, storageKey);
          return resolveUploadResult(gateway, file, completedId);
        }
        const parts = session.mode === 'SINGLE' ? await uploadSingle(transfer, file, session as OssUploadInitResponse, uploadOptions.signal, uploadOptions.onProgress) : await uploadMultipart(gateway, transfer, file, session, uploadOptions.signal, uploadOptions.onProgress);
        const complete = await gateway.completeUpload(session.uploadToken, parts);
        const id = requireOssId(complete.data, '完成 OSS 上传失败');
        await safeRemoveResume(resumeStore, storageKey);
        return resolveUploadResult(gateway, file, id);
      } catch (error) {
        if (uploadOptions.signal.aborted && uploadToken) {
          await gateway.abortUpload(uploadToken).catch(() => undefined);
          await safeRemoveResume(resumeStore, storageKey);
        }
        if (isAbortError(error)) throw new OssUploadError('上传已取消', 'aborted');
        throw error;
      }
    },
    async resolve(ids: readonly UploadIdentifier[]): Promise<readonly UploadItem[]> {
      if (!ids.length) return [];
      const response = await gateway.listByIds(ids);
      return response.data.map(item => ({ id: item.ossId, name: item.originalName, url: item.url }));
    },
    async remove(id: UploadIdentifier): Promise<void> {
      const normalized = requireOssId(id, '删除文件缺少 ossId');
      await gateway.delete(normalized);
    }
  };
}
