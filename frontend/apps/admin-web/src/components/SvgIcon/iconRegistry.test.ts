import { describe, expect, it, vi } from 'vitest';
import { getIcon } from '@iconify/vue';
import { FALLBACK_ICON, resolveIcon, tablerIconNames } from './iconRegistry';

describe('icon registry', () => {
  it('resolves local sprite icons before the bundled Tabler collection', () => {
    expect(resolveIcon('dashboard')).toEqual({ kind: 'local', value: '#icon-dashboard', source: 'dashboard' });
  });

  it('resolves unprefixed and prefixed Tabler names offline', () => {
    expect(resolveIcon('users')).toEqual({ kind: 'iconify', value: 'tabler:users', source: 'users' });
    expect(resolveIcon('tabler:users')).toEqual({ kind: 'iconify', value: 'tabler:users', source: 'tabler:users' });
    expect(tablerIconNames).toContain('tabler:users');
    expect(getIcon('tabler:users')).toBeTruthy();
  });

  it('resolves legacy menu aliases to installed iconify icons', () => {
    expect(resolveIcon('id-card')).toEqual({ kind: 'iconify', value: 'tabler:id-badge', source: 'id-card' });
  });

  it('keeps external Iconify names available for explicitly prefixed values', () => {
    expect(resolveIcon('mdi:account')).toEqual({ kind: 'iconify', value: 'mdi:account', source: 'mdi:account' });
  });

  it('uses a stable offline fallback for empty and unknown values', () => {
    expect(resolveIcon('#')).toEqual({ kind: 'iconify', value: FALLBACK_ICON, source: '#', fallback: true });
    expect(resolveIcon('tabler:not-a-real-icon')).toEqual({
      kind: 'iconify',
      value: FALLBACK_ICON,
      source: 'tabler:not-a-real-icon',
      fallback: true
    });
    expect(resolveIcon('missing-icon')).toEqual({
      kind: 'iconify',
      value: FALLBACK_ICON,
      source: 'missing-icon',
      fallback: true
    });
  });

  it('warns once per missing source in development', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    resolveIcon('another-missing-icon');
    resolveIcon('another-missing-icon');
    expect(warn).toHaveBeenCalledTimes(import.meta.env.DEV ? 1 : 0);
    warn.mockRestore();
  });
});
