import type { RouteRecordRaw } from 'vue-router';

/** 服务端为顶级页面添加无 meta 的布局壳；展示唯一叶子菜单，与 Admin 的折叠规则一致。 */
export function singleVisibleMenuChild(item: RouteRecordRaw): RouteRecordRaw | undefined {
  const visible = (item.children ?? []).filter(child => !child.hidden);
  const child = visible[0];
  if (item.alwaysShow || visible.length !== 1 || !child) return undefined;
  return (child.children ?? []).some(node => !node.hidden) ? undefined : child;
}

export function homeMenuPath(base: string, child: string): string {
  return (child.startsWith('/') ? child : `${base}/${child}`).replace(/\/+/g, '/');
}
