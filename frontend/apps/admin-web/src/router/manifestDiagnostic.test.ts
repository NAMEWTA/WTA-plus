import { describe, expect, it } from 'vitest';
import {
  createManifestRouteDiagnostic,
  formatDuplicateRouteNameDiagnostic,
  formatManifestRouteDiagnostic
} from './manifestDiagnostic';

describe('manifest route diagnostic', () => {
  it('keeps the original backend component key and stable inferred ownership visible', () => {
    const details = {
      appId: 'admin-web',
      code: 'missing-component-key' as const,
      componentKey: 'system/user/not-a-facade',
      domainId: 'system'
    };
    expect(formatManifestRouteDiagnostic(details)).toBe(
      '页面组件不可用 [missing-component-key] app=admin-web domain=system key=system/user/not-a-facade'
    );
    expect(createManifestRouteDiagnostic(details)).toMatchObject({ name: 'ManifestRouteDiagnostic' });
  });

  it('formats duplicate route names at the Admin presentation boundary', () => {
    expect(formatDuplicateRouteNameDiagnostic({ code: 'duplicate-route-name', routeName: 'Repeated' })).toBe(
      '路由名称: [Repeated] 重复, 会造成 404'
    );
  });
});
