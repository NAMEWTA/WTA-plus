import type { UploadIdentifier, UploadItem } from '@namewta/platform-contracts';
import type { UploadValue } from './types';

export interface NormalizedUploadValue {
  ids: UploadIdentifier[];
  items: UploadItem[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

export function isUploadIdentifier(value: unknown): value is UploadIdentifier {
  return (typeof value === 'string' || typeof value === 'number') && String(value).trim() !== '';
}

function itemFromUnknown(value: unknown): UploadItem | undefined {
  if (!isRecord(value)) return undefined;
  const id = value.id ?? value.ossId;
  if (!isUploadIdentifier(id)) return undefined;
  return {
    id,
    name: typeof value.name === 'string' ? value.name : typeof value.originalName === 'string' ? value.originalName : String(id),
    url: typeof value.url === 'string' ? value.url : ''
  };
}

export function normalizeUploadValue(value: UploadValue): NormalizedUploadValue {
  if (value === null || value === undefined || value === '') return { ids: [], items: [] };
  if (isRecord(value)) {
    const item = itemFromUnknown(value);
    return item && isUploadIdentifier(item.id) ? { ids: [item.id], items: [item] } : { ids: [], items: [] };
  }
  if (!Array.isArray(value)) return isUploadIdentifier(value) ? { ids: [value], items: [] } : { ids: [], items: [] };

  const items: UploadItem[] = [];
  const ids: UploadIdentifier[] = [];
  value.forEach(item => {
    const normalized = typeof item === 'string' || typeof item === 'number' ? item : itemFromUnknown(item);
    if (isUploadIdentifier(normalized)) ids.push(normalized);
    else if (normalized) {
      items.push(normalized);
      ids.push(normalized.id as UploadIdentifier);
    }
  });
  return { ids, items };
}

export function serializeUploadItems(items: readonly unknown[], separator = ','): string {
  return items
    .map(item => (isRecord(item) ? item.id ?? item.ossId : undefined))
    .filter(isUploadIdentifier)
    .map(item => String(item))
    .join(separator);
}

export function withUploadUid(item: UploadItem, index: number): UploadItem & { uid: number } {
  const uid = typeof item.uid === 'number' && Number.isFinite(item.uid) ? item.uid : Number(item.uid) || Date.now() + index;
  return { ...item, uid };
}
