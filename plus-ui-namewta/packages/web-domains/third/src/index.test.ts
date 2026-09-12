import { describe, expect, it } from 'vitest';
import { createThirdWebDomain } from './index';

describe('third admin manifest', () => {
  it('exposes four menu page registrations and keeps credentials nested', () => {
    const manifest = createThirdWebDomain({ service: {} as never });
    expect(manifest.registrations.map(registration => registration.componentKey)).toEqual([
      'third/provider/index',
      'third/endpoint/index',
      'third/invocation/index',
      'third/statistics/index'
    ]);
    expect(manifest.registrations.some(registration => registration.componentKey.includes('credential'))).toBe(false);
    expect(manifest.permissions.find(permission => permission.id === 'third-provider')?.permissions).toContain('third:credential:add');
  });
});
