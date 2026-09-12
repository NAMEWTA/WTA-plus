import type { ServerMenuMeta } from '@namewta/domain-admin';
import type { ServerRouteNode } from '@namewta/platform-app-runtime';
import type { Component } from 'vue';
import type { RouteRecordRaw } from 'vue-router';

export type AdminRouteComponent = Component | (() => Promise<Component>);

function requireResolvedComponent(
  component: string | AdminRouteComponent | undefined,
  path: string
): AdminRouteComponent | undefined {
  if (typeof component === 'string') throw new Error(`Server menu component is unresolved: ${path}`);
  return component;
}

export function adaptServerMenuRoutes(
  routes: readonly ServerRouteNode<AdminRouteComponent, ServerMenuMeta>[]
): RouteRecordRaw[] {
  return routes.map(route => {
    const component = requireResolvedComponent(route.component, route.path);
    const common = {
      path: route.path,
      ...(route.name !== undefined ? { name: route.name } : {}),
      ...(route.meta !== undefined ? { meta: { ...route.meta } } : {}),
      ...(route.alwaysShow !== undefined ? { alwaysShow: route.alwaysShow } : {}),
      ...(route.ext !== undefined ? { ext: route.ext } : {}),
      ...(route.hidden !== undefined ? { hidden: route.hidden } : {}),
      ...(route.permissions !== undefined ? { permissions: [...route.permissions] } : {}),
      ...(route.query !== undefined ? { query: route.query } : {})
    };
    if (route.children) {
      return {
        ...common,
        ...(component !== undefined ? { component } : {}),
        ...(route.redirect !== undefined ? { redirect: route.redirect } : {}),
        children: adaptServerMenuRoutes(route.children)
      } as RouteRecordRaw;
    }
    if (component !== undefined) return { ...common, component } as RouteRecordRaw;
    if (route.redirect !== undefined) return { ...common, redirect: route.redirect } as RouteRecordRaw;
    throw new Error(`Server menu route cannot be adapted: ${route.path}`);
  });
}
