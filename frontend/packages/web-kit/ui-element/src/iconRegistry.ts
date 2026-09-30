import { icons as tablerCollection } from '@iconify-json/tabler';
import { addCollection } from '@iconify/vue';

export const FALLBACK_ICON = 'tabler:help-circle';
export const tablerIconNames = Object.freeze(Object.keys(tablerCollection.icons).map(name => `tabler:${name}`));
const tablerIcons = new Set(Object.keys(tablerCollection.icons));
const aliases: Readonly<Record<string, string>> = { 'id-card': 'tabler:id-badge' };
export type ResolvedIcon = { kind: 'local' | 'iconify'; value: string; source: string; fallback?: boolean };

/** 宿主在启动时显式安装离线集合，不在共享模块导入时注册。 */
export function initializeIcons(): void {
  addCollection(tablerCollection);
}

/** 本地 sprite 由宿主声明；两个 App 共用字符串解析和离线 fallback 合同。 */
export function createIconResolver(localNames: readonly string[] = [], diagnostic?: (name: string) => void) {
  const local = new Set(localNames);
  const warned = new Set<string>();
  return (iconClass?: string): ResolvedIcon => {
    const source = iconClass?.replace(/^i-/, '').trim() || '';
    if (local.has(source)) return { kind: 'local', value: `#icon-${source}`, source };
    if (aliases[source]) return { kind: 'iconify', value: aliases[source], source };
    if (source.includes(':') && !source.startsWith('tabler:')) return { kind: 'iconify', value: source, source };
    const name = source.replace(/^tabler:/, '');
    if (tablerIcons.has(name)) return { kind: 'iconify', value: `tabler:${name}`, source };
    if (source && source !== '#' && !warned.has(source)) {
      warned.add(source);
      diagnostic?.(source);
    }
    return { kind: 'iconify', value: FALLBACK_ICON, source, fallback: true };
  };
}
