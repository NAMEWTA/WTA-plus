import { createIconResolver, FALLBACK_ICON, initializeIcons, tablerIconNames } from '@namewta/web-kit-ui-element/icons';
import localIconNames from '@/components/IconSelect/requireIcons';
export { FALLBACK_ICON, tablerIconNames };
export const TABLER_PREFIX = 'tabler';
export const localIconNameSet = localIconNames;
initializeIcons();
export const resolveIcon = createIconResolver(
  localIconNames,
  import.meta.env.DEV
    ? source => console.warn(`[SvgIcon] icon "${source}" was not found; using ${FALLBACK_ICON}`)
    : undefined
);
