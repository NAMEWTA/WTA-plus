import type { UploadIdentifier, UploadItem, UploadResult } from '@namewta/platform-contracts';

export interface OssPresignedRequest {
  method: string;
  url: string;
  requiredHeaders: Record<string, string>;
  expiresAt: string;
}

export type OssUploadMode = 'SINGLE' | 'MULTIPART';
export type OssUploadState = 'INITIALIZED' | 'UPLOADING' | 'COMPLETING' | 'COMPLETED' | 'ABORTED' | 'EXPIRED';

export interface OssUploadInitRequest {
  policy: string;
  fileName: string;
  fileSize: number;
  contentType: string;
  fingerprint: string;
}

export interface OssUploadInitResponse {
  uploadToken: string;
  mode: OssUploadMode;
  expiresAt: string;
  presignedRequest?: OssPresignedRequest;
  partSize?: number;
  partCount?: number;
}

export interface OssUploadedPart {
  partNumber: number;
  eTag: string;
  size: number;
}

export interface OssUploadResumeResponse {
  uploadToken: string;
  mode: OssUploadMode;
  state: OssUploadState;
  completedOssId: string | null;
  fileName: string;
  fileSize: number;
  contentType: string;
  partSize: number;
  partCount: number;
  expiresAt: string;
  presignedRequest?: OssPresignedRequest;
  uploadedParts: OssUploadedPart[];
}

export interface OssSignedPart extends OssPresignedRequest {
  partNumber: number;
}

export interface OssCompletedPart {
  partNumber: number;
  eTag: string;
}

export interface OssRecord {
  ossId: UploadIdentifier;
  originalName: string;
  url: string;
}

export interface OssApiResponse<T> {
  data: T;
}

export interface OssUploadGateway {
  initUpload(request: OssUploadInitRequest): Promise<OssApiResponse<OssUploadInitResponse>>;
  resumeUpload(token: string, fingerprint: string): Promise<OssApiResponse<OssUploadResumeResponse>>;
  signParts(token: string, partNumbers: number[]): Promise<OssApiResponse<{ parts: OssSignedPart[] }>>;
  completeUpload(token: string, parts: OssCompletedPart[]): Promise<OssApiResponse<unknown>>;
  abortUpload(token: string): Promise<unknown>;
  downloadUrl(id: UploadIdentifier): Promise<OssApiResponse<{ url: string }>>;
  listByIds(ids: UploadIdentifier | readonly UploadIdentifier[]): Promise<OssApiResponse<OssRecord[]>>;
  delete(id: UploadIdentifier): Promise<unknown>;
}

export interface OssUploadBrowserDependencies {
  fingerprint?: (file: File) => Promise<string>;
  resumeStore?: OssResumeStore;
  transfer?: OssTransfer;
}

export interface OssResumeRecord {
  fingerprint: string;
  uploadToken: string;
  expiresAt: string;
  fileName: string;
  fileSize: number;
  contentType: string;
}

export interface OssResumeStore {
  get(fingerprint: string): Promise<OssResumeRecord | undefined>;
  put(record: OssResumeRecord): Promise<unknown>;
  remove(fingerprint: string): Promise<unknown>;
}

export interface OssTransfer {
  (request: OssPresignedRequest, body: Blob, signal: AbortSignal, onProgress?: (progress: { loaded: number; total: number }) => void): Promise<string | undefined>;
}

export type { UploadItem, UploadResult };
