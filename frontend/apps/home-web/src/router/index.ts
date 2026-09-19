import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { ElMessage } from 'element-plus';
import { resolveHomeWebRegistration } from './homeManifestRegistry';
import HomeShell from '@/layout/HomeShell.vue';
import PortalPage from '@/views/PortalPage.vue';
import RegisterPage from '@/views/RegisterPage.vue';
import SsoCallbackPage from '@/views/SsoCallbackPage.vue';
import { useUserStore } from '@/store/user';
import { isHandledRequestError, relogin } from '@/application/http';
import { getToken } from '@/application/session';
import { useNavigationStore } from '@/store/navigation';
import { findDuplicateRouteNames, restoreProtectedNavigation } from '@namewta/platform-app-runtime';

const loginRegistration = resolveHomeWebRegistration('identity-access/login/index', 'identity-access');
const routes: RouteRecordRaw[] = [
  { path: '/', component: HomeShell, name: 'Home', children: [{ path: '', component: PortalPage, name: 'Portal' }, { path: 'login', component: loginRegistration?.load ?? PortalPage, name: 'Login' }, { path: 'register', component: RegisterPage, name: 'Register' }, { path: 'sso/callback', component: SsoCallbackPage, name: 'SsoCallback' }] },
  { path: '/:pathMatch(.*)*', component: HomeShell, children: [{ path: '', component: PortalPage }] }
];
const router = createRouter({ history: createWebHistory(import.meta.env.VITE_APP_CONTEXT_PATH), routes, scrollBehavior: () => ({ top: 0 }) });
let recovery: { token: string; generation: number; promise: Promise<void> } | undefined;
router.beforeEach(async to => {
  const user = useUserStore();
  const navigation = useNavigationStore();
  const token = getToken();
  if (!token) {
    if (['/login', '/register', '/', '/sso/callback'].includes(to.path)) return true;
    return { path: '/', query: { redirect: to.fullPath } };
  }
  if (['/login', '/register', '/'].includes(to.path)) return { path: '/profile' };
  if (user.identityLoaded && navigation.navigationLoaded && user.token === token) return true;
  const generation = user.sessionGeneration;
  const isCurrent = () => getToken() === token && user.sessionGeneration === generation;
  if (!recovery || recovery.token !== token || recovery.generation !== generation) {
    relogin.navigationPending = true;
    const promise = restoreProtectedNavigation({
      loadIdentity: () => user.getInfo(),
      loadRoutes: () => navigation.generateRoutes(),
      isCurrent,
      isExternal: route => /^https?:\/\//.test(route.path),
      addRoute: route => {
        const existing = router.getRoutes().map(value => ({ name: value.name }));
        if (findDuplicateRouteNames([existing, [route]]).length) throw new Error('菜单路由名称冲突');
        navigation.registerRoute(route as RouteRecordRaw, value => router.addRoute('Home', value));
      },
      createReplacement: () => { navigation.finishRecovery(); }
    }).finally(() => {
      if (recovery?.promise === promise) {
        relogin.navigationPending = false;
        recovery = undefined;
      }
    });
    recovery = { token, generation, promise };
  }
  try {
    await recovery.promise;
    if (!isCurrent()) return false;
    return { path: to.path, query: to.query, hash: to.hash, replace: true };
  } catch (error) {
    if (!isCurrent()) return false;
    await user.logout().catch(() => undefined);
    if (!isHandledRequestError(error)) ElMessage.error(error instanceof Error ? error.message : String(error));
    return { path: '/', replace: true };
  }
});

export default router;
