import { describe, expect, it } from 'vitest';
import { assembleServerRoutes, findDuplicateRouteNames, projectServerRoutes } from './routeAssembler';

describe('server route assembler', () => {
  it('maps selected manifest registrations without filtering server menu metadata', () => {
    const view = { kind: 'demo-view' };
    const routes = assembleServerRoutes({
      appId: 'admin-web',
      routes: [
        {
          path: '/demo',
          component: 'Layout',
          permissions: ['server:already-filtered'],
          children: [{ path: 'table', component: 'demo/demo/index', name: 'Demo' }]
        }
      ],
      specialComponents: { Layout: { kind: 'layout' } },
      resolveRegistration: ({ componentKey, domainId }) =>
        componentKey === 'demo/demo/index' && domainId === 'demo' ? { componentName: 'Demo', load: view } : undefined,
      createDiagnostic: details => ({ kind: 'diagnostic', details })
    });

    expect(routes).toEqual([
      {
        path: '/demo',
        component: { kind: 'layout' },
        permissions: ['server:already-filtered'],
        children: [{ path: 'table', component: view, name: 'Demo' }]
      }
    ]);
  });

  it('turns an arbitrary missing component into a stable app/domain/key diagnostic', () => {
    const routes = assembleServerRoutes({
      appId: 'admin-web',
      routes: [{ path: '/typo', component: 'system/user/not-a-facade' }],
      resolveRegistration: () => undefined,
      createDiagnostic: details => details
    });

    expect(routes[0]?.component).toEqual({
      appId: 'admin-web',
      code: 'missing-component-key',
      componentKey: 'system/user/not-a-facade',
      domainId: 'system'
    });
  });

  it.each([
    {
      flattenParentView: false,
      expectedChildren: [
        {
          path: 'group',
          name: 'Group',
          component: { kind: 'parent-view' },
          children: [
            {
              path: 'leaf',
              name: 'Leaf',
              component: { kind: 'demo-view' },
              permissions: ['server:metadata-only']
            }
          ]
        }
      ]
    },
    {
      flattenParentView: true,
      expectedChildren: [
        {
          path: 'group/leaf',
          name: 'Leaf',
          component: { kind: 'demo-view' },
          permissions: ['server:metadata-only']
        }
      ]
    }
  ])('projects ParentView routes without mutating server input: $flattenParentView', entry => {
    const source = [
      {
        path: '/root',
        component: 'Layout',
        children: [
          {
            path: 'group',
            name: 'Group',
            component: 'ParentView',
            children: [
              {
                path: 'leaf',
                name: 'Leaf',
                component: 'demo/demo/index',
                permissions: ['server:metadata-only']
              }
            ]
          }
        ]
      }
    ] as const;
    const original = structuredClone(source);

    const routes = projectServerRoutes({
      appId: 'admin-web',
      routes: source,
      flattenParentView: entry.flattenParentView,
      specialComponents: { Layout: { kind: 'layout' }, ParentView: { kind: 'parent-view' } },
      resolveRegistration: ({ componentKey }) =>
        componentKey === 'demo/demo/index' ? { componentName: 'Demo', load: { kind: 'demo-view' } } : undefined,
      createDiagnostic: details => ({ kind: 'diagnostic', details })
    });

    expect(routes).toEqual([{ path: '/root', component: { kind: 'layout' }, children: entry.expectedChildren }]);
    expect(source).toEqual(original);
  });

  it('removes empty or malformed children and their redirect without inventing a page', () => {
    const routes = projectServerRoutes<unknown>({
      appId: 'admin-web',
      routes: [
        { path: '/empty', component: 'Layout', redirect: '/hidden', children: [] },
        { path: '/malformed', component: 'Layout', redirect: '/hidden', children: {} as never }
      ],
      specialComponents: { Layout: { kind: 'layout' } },
      resolveRegistration: () => undefined,
      createDiagnostic: details => ({ kind: 'diagnostic', details })
    });

    expect(routes).toEqual([
      { path: '/empty', component: { kind: 'layout' } },
      { path: '/malformed', component: { kind: 'layout' } }
    ]);
  });

  it('returns stable unique diagnostics for duplicate non-empty route names', () => {
    expect(
      findDuplicateRouteNames([
        [
          { path: '/first', name: 'Shared' },
          { path: '/nested', children: [{ path: 'leaf', name: 'Nested' }] }
        ],
        [
          { path: '/second', name: 'Shared' },
          { path: '/third', name: 'Shared' },
          { path: '/nested-again', name: 'Nested' },
          { path: '/blank', name: ' ' }
        ]
      ])
    ).toEqual([
      { code: 'duplicate-route-name', routeName: 'Shared' },
      { code: 'duplicate-route-name', routeName: 'Nested' }
    ]);
  });
});
