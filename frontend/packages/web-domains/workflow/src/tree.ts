export function handleTree<T extends object>(data: T[] | undefined, idKey: keyof T, parentKey: keyof T): T[] {
  const rows = (data ?? []).map(item => ({ ...item }));
  const byId = new Map(rows.map(item => [String(item[idKey]), item]));
  const roots: T[] = [];
  for (const item of rows) {
    const parent = byId.get(String(item[parentKey]));
    if (!parent || parent === item) roots.push(item);
    else {
      const record = parent as T & { children?: T[] };
      const children = Array.isArray(record.children) ? record.children : [];
      record.children = [...children, item];
    }
  }
  return roots;
}
