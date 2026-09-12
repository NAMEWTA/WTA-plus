import { localIconNameSet, tablerIconNames } from '@/components/SvgIcon/iconRegistry';

export const iconNames = Object.freeze([...localIconNameSet, ...tablerIconNames]);

export function filterIconNames(query: string): string[] {
  const value = query.trim().toLowerCase();
  if (!value) return [...iconNames];
  return iconNames.filter(iconName => iconName.toLowerCase().includes(value));
}
