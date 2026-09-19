import type { ServerMenuMeta, ServerMenuNode } from '@namewta/domain-admin';
import { projectServerRoutes } from '@namewta/platform-app-runtime';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { RouteRecordRaw } from 'vue-router';
import { identityAccessService } from '@/application/services';
import { createHomeManifestDiagnostic } from '@/router/manifestDiagnostic';
import { resolveHomeWebRegistration } from '@/router/homeManifestRegistry';
import { adaptServerMenuRoutes, type HomeRouteComponent } from '@/router/serverMenuAdapter';

export const useNavigationStore = defineStore('home-navigation', () => {
  const routes = ref<RouteRecordRaw[]>([]);
  const navigationLoaded = ref(false);
  let generation = 0;
  const removeRoutes: Array<() => void> = [];

  const resetRoutes = () => {
    generation++;
    navigationLoaded.value = false;
    for (const remove of removeRoutes.splice(0).toReversed()) remove();
    routes.value = [];
  };
  const registerRoute = (route: RouteRecordRaw, install: (route: RouteRecordRaw) => () => void) => {
    removeRoutes.push(install(route));
  };
  const finishRecovery = () => { navigationLoaded.value = true; };

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
    routes.value = generated;
    return generated;
  };

  return { routes, navigationLoaded, generateRoutes, resetRoutes, registerRoute, finishRecovery };
});
