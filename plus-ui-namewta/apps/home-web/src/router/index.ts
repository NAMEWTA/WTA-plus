import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { ElMessage } from 'element-plus';
import { resolveHomeWebRegistration } from './homeManifestRegistry';
import HomeShell from '@/layout/HomeShell.vue';
import PortalPage from '@/views/PortalPage.vue';
import RegisterPage from '@/views/RegisterPage.vue';
import { useUserStore } from '@/store/user';
import { isHandledRequestError } from '@/application/http';
import { getToken } from '@/application/session';
import { useNavigationStore } from '@/store/navigation';
import { restoreProtectedNavigation } from '@namewta/platform-app-runtime';

const loginRegistration = resolveHomeWebRegistration('identity-access/login/index', 'identity-access');
const routes: RouteRecordRaw[] = [
  { path: '/', component: HomeShell, name: 'Home', children: [{ path: '', component: PortalPage, name: 'Portal' }, { path: 'login', component: loginRegistration?.load ?? PortalPage, name: 'Login' }, { path: 'register', component: RegisterPage, name: 'Register' }] },
  { path: '/:pathMatch(.*)*', component: HomeShell, children: [{ path: '', component: PortalPage }] }
];
const router = createRouter({ history: createWebHistory(import.meta.env.VITE_APP_CONTEXT_PATH), routes, scrollBehavior: () => ({ top: 0 }) });
router.beforeEach(async to => {
  const user = useUserStore();
  if (!getToken()) { if (to.path === '/login' || to.path === '/register' || to.path === '/') return true; return { path: '/', query: { redirect: to.fullPath } }; }
  if (to.path === '/login' || to.path === '/register' || to.path === '/') return { path: '/profile' };
  if (user.roles.length === 0) {
    try {
      return await restoreProtectedNavigation({
        loadIdentity: () => user.getInfo(),
        loadRoutes: () => useNavigationStore().generateRoutes(),
        isExternal: route => /^https?:\/\//.test(route.path),
        addRoute: route => router.addRoute('Home', route as RouteRecordRaw),
        createReplacement: () => ({ path: to.path, query: to.query, hash: to.hash, replace: true })
      });
    } catch (error) {
      await user.logout();
      if (!isHandledRequestError(error)) ElMessage.error(error instanceof Error ? error.message : String(error));
      return { path: '/', replace: true };
    }
  }
  return true;
});
export default router;
