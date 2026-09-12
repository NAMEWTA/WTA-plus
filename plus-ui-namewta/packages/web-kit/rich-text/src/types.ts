export type RichTextAssetKind = 'image' | 'audio' | 'video' | 'attachment';

export interface RichTextAssetAccess {
  ossId: string;
  status: 'available' | 'unavailable';
  url?: string;
  expiresAt?: string | null;
  fileName?: string;
  contentType?: string;
}

export interface RichTextUploadedAsset {
  ossId: string;
  fileName: string;
  contentType?: string;
}

export interface RichTextAssetsPort {
  upload(file: File, kind: RichTextAssetKind, options: { signal: AbortSignal; onProgress?: (percent: number) => void }): Promise<RichTextUploadedAsset>;
  resolve(ossIds: readonly string[], options: { signal: AbortSignal; richTextId?: string }): Promise<readonly RichTextAssetAccess[]>;
}

export interface RichTextEditorState {
  pending: number;
  failed: number;
  valid: boolean;
}
