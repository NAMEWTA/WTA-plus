import type { ServerMenuMeta } from '@namewta/domain-admin';
import type { ServerRouteNode } from '@namewta/platform-app-runtime';
import type { Component } from 'vue';
import type { RouteRecordRaw } from 'vue-router';
import { resolveHomeWebRegistration } from './homeManifestRegistry';

export type HomeRouteComponent = Component | (() => Promise<Component>);

function requireResolvedComponent(component: string | HomeRouteComponent | undefined, path: string): HomeRouteComponent | undefined {
  if (typeof component === 'string') throw new Error(`Server menu component is unresolved: ${path}`);
  return component;
}

export function adaptServerMenuRoutes(routes: readonly ServerRouteNode<HomeRouteComponent, ServerMenuMeta>[]): RouteRecordRaw[] {
  return routes.map(route => {
    const component = requireResolvedComponent(
      typeof route.component === 'string'
        ? resolveHomeWebRegistration(route.component, route.component.split('/')[0])?.load
        : route.component,
      route.path
    );
    const base = {
      path: route.path,
      ...(route.name ? { name: route.name } : {}),
      ...(route.meta ? { meta: route.meta } : {}),
      ...(route.hidden !== undefined ? { hidden: route.hidden } : {}),
      ...(route.alwaysShow !== undefined ? { alwaysShow: route.alwaysShow } : {}),
      ...(route.ext !== undefined ? { ext: route.ext } : {}),
      ...(route.permissions !== undefined ? { permissions: [...route.permissions] } : {}),
      ...(route.redirect ? { redirect: route.redirect } : {}),
      ...(route.query ? { query: route.query } : {})
    };
    if (route.children) {
      return { ...base, ...(component ? { component } : {}), children: adaptServerMenuRoutes(route.children) } as RouteRecordRaw;
    }
    if (component) return { ...base, component } as RouteRecordRaw;
    if (route.redirect) return base as RouteRecordRaw;
    throw new Error(`Server menu route cannot be adapted: ${route.path}`);
  });
}
