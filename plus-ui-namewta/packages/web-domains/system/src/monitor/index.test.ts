import { describe, expect, it, vi } from 'vitest';
import { createMonitorWebDomain } from './index';

const runtime = {
  service: {} as never,
  confirm: vi.fn(),
  success: vi.fn(),
  error: vi.fn(),
  loading: vi.fn(),
  closeLoading: vi.fn(),
  download: vi.fn(),
  openDownload: vi.fn(),
  dicts: vi.fn(() => ({})),
  hasPermission: vi.fn(() => true)
};

describe('monitor web manifest', () => {
  it('registers the exact legacy component keys and complete permission inventory', () => {
    const manifest = createMonitorWebDomain(runtime);
    expect(manifest.registrations.map(item => item.componentKey)).toEqual([
      'monitor/online/index',
      'monitor/cache/index',
      'monitor/operlog/index',
      'monitor/logininfo/index',
    ]);
    expect(manifest.permissions.flatMap(item => item.permissions)).toEqual(
      expect.arrayContaining([
        'monitor:online:forceLogout',
        'monitor:online:batchLogout',
        'monitor:operlog:export',
        'monitor:logininfo:unlock',
      ])
    );
  });
});
