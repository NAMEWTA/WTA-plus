import { describe, expect, it } from 'vitest';
import { filterIconNames, iconNames } from './iconOptions';

describe('icon select options', () => {
  it('includes local and bundled Tabler names', () => {
    expect(iconNames).toContain('dashboard');
    expect(iconNames).toContain('tabler:users');
  });

  it('filters names case-insensitively and returns all names for an empty query', () => {
    expect(filterIconNames('TABLER:USERS')).toContain('tabler:users');
    expect(filterIconNames('TABLER:USERS')).toHaveLength(4);
    expect(filterIconNames('')).toHaveLength(iconNames.length);
  });
});
