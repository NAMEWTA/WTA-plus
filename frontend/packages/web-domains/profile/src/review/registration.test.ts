import { createProfileService } from '@namewta/domain-profile';
import { describe, expect, it, vi } from 'vitest';
import { createProfileReviewWebDomain } from './registration';
describe('participant review manifest', () => {
  it('publishes only task review pages without requiring management upload and directory ports', () => {
    const runtime = {
      service: createProfileService({ request: vi.fn() }),
      hasPermission: () => true,
      confirm: vi.fn(),
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn(),
      downloadMaterial: vi.fn(),
      closeCurrentPage: vi.fn()
    };
    const manifest = createProfileReviewWebDomain(runtime);
    expect(manifest.registrations.map(value => value.componentKey)).toEqual([
      'profile/person/review',
      'profile/enterprise/review'
    ]);
    expect(manifest.permissions.flatMap(value => value.permissions)).toEqual([
      'profile:person:task-review',
      'profile:enterprise:task-review'
    ]);
  });
});
