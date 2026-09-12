import type { UploadClient, UploadIdentifier, UploadItem, UploadResult } from '@namewta/platform-contracts';

export interface UploadFeedback {
  closeLoading(): void;
  error(message: string): void;
  loading(message: string): void;
}

export interface FileUploadProps {
  client: UploadClient;
  modelValue?: UploadValue;
  limit?: number;
  fileSize?: number;
  fileType?: string[];
  isShowTip?: boolean;
  disabled?: boolean;
  ossExt?: unknown;
  policy?: string;
  separator?: string;
  feedback?: UploadFeedback;
}

export interface ImageUploadProps {
  client: UploadClient;
  modelValue?: UploadValue;
  limit?: number;
  fileSize?: number;
  fileType?: string[];
  isShowTip?: boolean;
  compressSupport?: boolean;
  compressTargetSize?: number;
  compress?: (file: File, targetSizeKb: number) => File | Promise<File>;
  ossExt?: unknown;
  policy?: string;
  separator?: string;
  feedback?: UploadFeedback;
}

export type UploadValue = UploadIdentifier | readonly unknown[] | Record<string, unknown> | null | undefined | '';
export type { UploadClient, UploadIdentifier, UploadItem, UploadResult };
