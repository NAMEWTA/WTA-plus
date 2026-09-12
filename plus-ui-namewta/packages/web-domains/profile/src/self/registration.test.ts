import { describe, expect, it, vi } from 'vitest';
import { createProfileSelfWebDomain } from './registration';
import type { ProfileSelfWebRuntime } from './runtime';

const runtime = (): ProfileSelfWebRuntime => ({
  confirm: vi.fn(async () => undefined),
  error: vi.fn(),
  hasPermission: () => true,
  service: {} as ProfileSelfWebRuntime['service'],
  success: vi.fn(),
  warning: vi.fn()
});

describe('profile self web manifest', () => {
  it('registers only the user center and its two verification pages', () => {
    const manifest = createProfileSelfWebDomain(runtime());

    expect(manifest.id).toBe('web-domain-profile-self');
    expect(manifest.registrations.map(item => item.componentKey)).toEqual([
      'profile/center/index',
      'profile/person/application',
      'profile/enterprise/application'
    ]);
  });
});
