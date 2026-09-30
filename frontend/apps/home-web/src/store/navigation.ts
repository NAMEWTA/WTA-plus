import type { ServerMenuMeta, ServerMenuNode } from '@namewta/domain-admin';
import type { RouteRecordRaw } from 'vue-router';
import { projectServerRoutes } from '@namewta/platform-app-runtime';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { identityAccessService } from '@/application/services';
import { resolveHomeWebRegistration } from '@/router/homeManifestRegistry';
import { createHomeManifestDiagnostic } from '@/router/manifestDiagnostic';
import { adaptServerMenuRoutes, type HomeRouteComponent } from '@/router/serverMenuAdapter';

export const useNavigationStore = defineStore('home-navigation', () => {
  const routes = ref<RouteRecordRaw[]>([]);
  const navigationLoaded = ref(false);
  const componentPaths = ref<Record<string, string>>({});
  let generation = 0;
  const removeRoutes: Array<() => void> = [];

  const resetRoutes = () => {
    generation++;
    navigationLoaded.value = false;
    for (const remove of removeRoutes.splice(0).toReversed()) remove();
    routes.value = [];
    componentPaths.value = {};
  };
  const registerRoute = (route: RouteRecordRaw, install: (route: RouteRecordRaw) => () => void) => {
    removeRoutes.push(install(route));
  };
  const finishRecovery = () => {
    navigationLoaded.value = true;
  };

  const generateRoutes = async () => {
    resetRoutes();
    const current = generation;
    const menus = await identityAccessService.getMenus();
    if (current !== generation) throw new Error('Session changed during menu recovery');
    const projected = projectServerRoutes<HomeRouteComponent, ServerMenuMeta>({
      appId: 'home-web',
      routes: menus as readonly ServerMenuNode[],
      resolveRegistration: ({ componentKey, domainId }) => resolveHomeWebRegistration(componentKey, domainId),
      createDiagnostic: createHomeManifestDiagnostic
    });
    const generated = adaptServerMenuRoutes(projected);
    const paths: Record<string, string> = {};
    const collect = (nodes: readonly ServerMenuNode[], parent = '') => {
      for (const node of nodes) {
        const path = node.path.startsWith('/') ? node.path : `${parent}/${node.path}`.replace(/\/+/g, '/');
        if (typeof node.component === 'string') paths[node.component] = path;
        if (node.children) collect(node.children, path);
      }
    };
    collect(menus);
    componentPaths.value = paths;
    routes.value = generated;
    return generated;
  };

  return { routes, navigationLoaded, componentPaths, generateRoutes, resetRoutes, registerRoute, finishRecovery };
});
