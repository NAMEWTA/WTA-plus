export const OSS_DELETED_MESSAGE = '该文件已删除';

const IMAGE_SUFFIXES = new Set(['.png', '.jpg', '.jpeg']);

export type OssFilePresentation = 'deleted' | 'image' | 'text';
export type OssRowMutation = 'delete' | 'restore';

export type OssPresentationRow = {
  deleteState?: string;
  fileSuffix?: string | string[];
};

export function ossFilePresentation(row: OssPresentationRow, previewEnabled: boolean): OssFilePresentation {
  if (row.deleteState === 'PENDING') return 'deleted';
  if (previewEnabled && hasImageSuffix(row.fileSuffix)) return 'image';
  return 'text';
}

export function ossRowMutation(row: { deleteState?: string }): OssRowMutation {
  return row.deleteState === 'PENDING' ? 'restore' : 'delete';
}

export function ossDeleteTargets<T extends { deleteState?: string }>(rows: readonly T[]) {
  return {
    pending: rows.filter(row => ossRowMutation(row) === 'restore'),
    removable: rows.filter(row => ossRowMutation(row) === 'delete')
  };
}

export function ossDeleteConfirmMessage(ids: readonly (string | number)[], pendingCount: number) {
  const message = `是否确认删除OSS对象存储编号为"${ids.join(',')}"的数据项?`;
  if (pendingCount > 0) return `${message}待删除文件请使用行内恢复，本次不会再次删除。`;
  return message;
}

function hasImageSuffix(fileSuffix: string | string[] | undefined) {
  if (!fileSuffix) return false;
  const suffixes = Array.isArray(fileSuffix) ? fileSuffix : [fileSuffix];
  return suffixes.some(suffix => IMAGE_SUFFIXES.has(suffix.toLowerCase()));
}
