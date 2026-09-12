import { icons as tablerCollection } from '@iconify-json/tabler';
import { addCollection } from '@iconify/vue';

import localIconNames from '@/components/IconSelect/requireIcons';

export const TABLER_PREFIX = 'tabler';
export const FALLBACK_ICON = `${TABLER_PREFIX}:help-circle`;

// Keep menu icon names stable when an existing menu uses a legacy alias.
const ICON_ALIASES: Readonly<Record<string, string>> = Object.freeze({
  'id-card': `${TABLER_PREFIX}:id-badge`
});

const localIcons = new Set(localIconNames);
const tablerIcons = new Set(Object.keys(tablerCollection.icons));
const warnedIcons = new Set<string>();

addCollection(tablerCollection);

export type ResolvedIcon =
  | { kind: 'local'; value: string; source: string }
  | { kind: 'iconify'; value: string; source: string; fallback?: boolean };

function warnMissingIcon(source: string): void {
  if (!import.meta.env.DEV || warnedIcons.has(source)) return;
  warnedIcons.add(source);
  console.warn(`[SvgIcon] icon "${source}" was not found; using ${FALLBACK_ICON}`);
}

export function resolveIcon(iconClass?: string): ResolvedIcon {
  const source = iconClass?.replace(/^i-/, '').trim() || '';
  if (!source || source === '#') {
    return { kind: 'iconify', value: FALLBACK_ICON, source, fallback: true };
  }

  const alias = ICON_ALIASES[source];
  if (alias) return { kind: 'iconify', value: alias, source };

  if (source.includes(':')) {
    const [prefix, name] = source.split(':', 2);
    if (prefix === TABLER_PREFIX && !tablerIcons.has(name)) {
      warnMissingIcon(source);
      return { kind: 'iconify', value: FALLBACK_ICON, source, fallback: true };
    }
    return { kind: 'iconify', value: source, source };
  }

  if (localIcons.has(source)) return { kind: 'local', value: `#icon-${source}`, source };
  if (tablerIcons.has(source)) return { kind: 'iconify', value: `${TABLER_PREFIX}:${source}`, source };

  warnMissingIcon(source);
  return { kind: 'iconify', value: FALLBACK_ICON, source, fallback: true };
}

export const tablerIconNames = Object.freeze(
  Object.keys(tablerCollection.icons).map(name => `${TABLER_PREFIX}:${name}`)
);

export const localIconNameSet = localIconNames;
