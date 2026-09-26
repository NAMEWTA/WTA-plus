export const OSS_DELETED_MESSAGE = '该文件已删除';
export const OSS_DELETING_MESSAGE = '文件清理结果待确认，暂不能操作';

const IMAGE_SUFFIXES = new Set(['.png', '.jpg', '.jpeg']);

export type OssFilePresentation = 'deleted' | 'deleting' | 'image' | 'text';
export type OssRowMutation = 'delete' | 'restore' | 'none';

export type OssPresentationRow = {
  deleteState?: string;
  fileSuffix?: string | string[];
};

export function ossFilePresentation(row: OssPresentationRow, previewEnabled: boolean): OssFilePresentation {
  if (row.deleteState === 'DELETING') return 'deleting';
  if (row.deleteState === 'PENDING') return 'deleted';
  if (previewEnabled && hasImageSuffix(row.fileSuffix)) return 'image';
  return 'text';
}

export function ossRowMutation(row: { deleteState?: string }): OssRowMutation {
  if (row.deleteState === 'DELETING') return 'none';
  return row.deleteState === 'PENDING' ? 'restore' : 'delete';
}

export function ossDeleteTargets<T extends { deleteState?: string }>(rows: readonly T[]) {
  return {
    deleting: rows.filter(row => ossRowMutation(row) === 'none'),
    pending: rows.filter(row => ossRowMutation(row) === 'restore'),
    removable: rows.filter(row => ossRowMutation(row) === 'delete')
  };
}

export function ossDeleteConfirmMessage(ids: readonly (string | number)[], pendingCount: number, deletingCount = 0) {
  const message = `是否确认删除OSS对象存储编号为"${ids.join(',')}"的数据项?`;
  const pending = pendingCount > 0 ? '待删除文件请使用行内恢复，本次不会再次删除。' : '';
  const deleting = deletingCount > 0 ? '清理结果待确认的文件本次不操作。' : '';
  return message + pending + deleting;
}

function hasImageSuffix(fileSuffix: string | string[] | undefined) {
  if (!fileSuffix) return false;
  const suffixes = Array.isArray(fileSuffix) ? fileSuffix : [fileSuffix];
  return suffixes.some(suffix => IMAGE_SUFFIXES.has(suffix.toLowerCase()));
}
