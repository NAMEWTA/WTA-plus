import type { ServerMenuMeta } from '@namewta/domain-admin';
import type { ServerRouteNode } from '@namewta/platform-app-runtime';
import type { Component } from 'vue';
import { describe, expect, it } from 'vitest';
import { adaptServerMenuRoutes, type AdminRouteComponent } from './serverMenuAdapter';

describe('Admin server menu adapter', () => {
  it('recursively adapts the terminal-neutral projection without mutating it', () => {
    const layout: Component = { name: 'Layout' };
    const page: Component = { name: 'UserList' };
    const source: readonly ServerRouteNode<AdminRouteComponent, ServerMenuMeta>[] = Object.freeze([
      Object.freeze({
        path: '/system',
        name: 'System',
        component: layout,
        hidden: false,
        permissions: Object.freeze(['system:user:list']),
        meta: Object.freeze({ title: '系统管理', noCache: false }),
        children: Object.freeze([
          Object.freeze({ path: 'user', name: 'User', component: page, query: '{"scope":"all"}' })
        ])
      })
    ]);

    expect(adaptServerMenuRoutes(source)).toEqual([
      {
        path: '/system',
        name: 'System',
        component: layout,
        hidden: false,
        permissions: ['system:user:list'],
        meta: { title: '系统管理', noCache: false },
        children: [
          { path: 'user', name: 'User', component: page, query: '{"scope":"all"}' }
        ]
      }
    ]);
    expect(Object.isFrozen(source)).toBe(true);
    expect(Object.isFrozen(source[0]?.children)).toBe(true);
  });
});
