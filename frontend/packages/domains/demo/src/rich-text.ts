import type { HttpClient } from '@namewta/platform-contracts';
import type { ApiResponse, PageQuery, PageResult } from './index';

export interface RichTextSummary {
  richTextId: string;
  title: string;
  version: number;
  updateTime?: string;
}

export interface RichTextRecord extends RichTextSummary {
  html: string;
}

export interface RichTextForm {
  title: string;
  html: string;
  version?: number;
}

export type RichTextAssetKind = 'image' | 'audio' | 'video' | 'attachment';

export interface RichTextAssetAccess {
  ossId: string;
  status: 'available' | 'unavailable';
  url?: string;
  expiresAt?: string | null;
  fileName?: string;
  contentType?: string;
}

export interface RichTextAssetsPort {
  upload(
    file: File,
    kind: RichTextAssetKind,
    options: { signal: AbortSignal; onProgress?: (percent: number) => void }
  ): Promise<{ ossId: string; fileName: string }>;
  resolve(ossIds: readonly string[], options: { signal: AbortSignal; richTextId?: string }): Promise<readonly RichTextAssetAccess[]>;
}

export interface RichTextService {
  list(query: PageQuery): Promise<ApiResponse<PageResult<RichTextSummary>>>;
  get(id: string): Promise<ApiResponse<RichTextRecord>>;
  create(data: RichTextForm): Promise<ApiResponse<RichTextRecord>>;
  update(id: string, data: RichTextForm & { version: number }): Promise<ApiResponse<RichTextRecord>>;
  remove(id: string, version: number): Promise<ApiResponse<void>>;
  assets: RichTextAssetsPort;
}

const encodeId = (id: string): string => encodeURIComponent(id);

export function createRichTextService(http: HttpClient, assets: RichTextAssetsPort): RichTextService {
  return Object.freeze({
    list: (query: PageQuery) =>
      http.request<ApiResponse<PageResult<RichTextSummary>>>({ url: '/demo/rich-text/list', method: 'get', params: query }),
    get: (id: string) => http.request<ApiResponse<RichTextRecord>>({ url: `/demo/rich-text/${encodeId(id)}`, method: 'get' }),
    create: (data: RichTextForm) => http.request<ApiResponse<RichTextRecord>>({ url: '/demo/rich-text/create', method: 'post', data }),
    update: (id: string, data: RichTextForm & { version: number }) =>
      http.request<ApiResponse<RichTextRecord>>({ url: `/demo/rich-text/${encodeId(id)}/update`, method: 'post', data }),
    remove: (id: string, version: number) =>
      http.request<ApiResponse<void>>({ url: `/demo/rich-text/${encodeId(id)}/remove`, method: 'post', data: { version } }),
    assets
  });
}
