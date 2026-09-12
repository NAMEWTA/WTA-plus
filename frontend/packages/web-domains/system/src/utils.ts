export const MenuTypeEnum = Object.freeze({ M: 'M', C: 'C', F: 'F' } as const);

export function parseTime(time: unknown, pattern = '{y}-{m}-{d} {h}:{i}:{s}') {
  if (!time) return null;
  const date =
    time instanceof Date
      ? time
      : new Date(typeof time === 'string' ? time.replace(/-/g, '/').replace('T', ' ') : (time as number));
  const values: Record<string, number> = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  };
  return pattern.replace(/{(y|m|d|h|i|s|a)+}/g, (match, key: string) => {
    if (key === 'a') return ['日', '一', '二', '三', '四', '五', '六'][values[key]] ?? '';
    return String(values[key]).padStart(match.length > 0 ? 2 : 1, '0');
  });
}

export function selectDictLabel(data: readonly { label: string; value: string }[], value: string | number) {
  return data.find(item => item.value === String(value))?.label ?? String(value ?? '');
}

export function handleTree<T extends object>(data: T[], id = 'id', parentId = 'parentId', children = 'children'): T[] {
  const byId = new Map<unknown, T>();
  for (const item of data) {
    const record = item as Record<string, unknown>;
    record[children] = [];
    byId.set(record[id], item);
  }
  const roots: T[] = [];
  for (const item of data) {
    const record = item as Record<string, unknown>;
    const parent = byId.get(record[parentId]);
    if (parent) ((parent as Record<string, unknown>)[children] as T[]).push(item);
    else roots.push(item);
  }
  return roots;
}
