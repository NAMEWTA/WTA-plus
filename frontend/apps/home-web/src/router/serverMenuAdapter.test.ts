import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'serverMenuAdapter.ts'), 'utf8');

describe('adaptServerMenuRoutes', () => {
  it('does not treat breadcrumb noRedirect as a Vue route redirect', () => {
    expect(source).toContain("route.redirect !== 'noRedirect'");
  });
});
