import { describe, expect, it } from 'vitest';
import { restoreProtectedNavigation } from './navigationRecovery';

describe('protected navigation recovery', () => {
  it('restores getInfo -> getRouters -> addRoute -> replace with the loaded route values', async () => {
    const events: string[] = [];
    const routes = [{ path: '/internal' }, { path: 'https://external.example' }];

    const result = await restoreProtectedNavigation({
      loadIdentity: async () => {
        events.push('getInfo');
      },
      loadRoutes: async () => {
        events.push('getRouters');
        return routes;
      },
      isExternal: route => route.path.startsWith('http'),
      addRoute: route => events.push(`addRoute:${route.path}`),
      createReplacement: () => {
        events.push('replace');
        return { path: '/internal', replace: true };
      }
    });

    expect(events).toEqual(['getInfo', 'getRouters', 'addRoute:/internal', 'replace']);
    expect(result).toEqual({ path: '/internal', replace: true });
  });
});
