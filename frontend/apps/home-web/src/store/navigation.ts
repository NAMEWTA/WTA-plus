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

  const generateRoutes = async () => {
    const menus = await identityAccessService.getMenus();
    const projected = projectServerRoutes<HomeRouteComponent, ServerMenuMeta>({
      appId: 'home-web',
      routes: menus as readonly ServerMenuNode[],
      resolveRegistration: ({ componentKey, domainId }) => resolveHomeWebRegistration(componentKey, domainId),
      createDiagnostic: createHomeManifestDiagnostic
    });
    routes.value = adaptServerMenuRoutes(projected);
    return routes.value;
  };

  return { routes, generateRoutes };
});
