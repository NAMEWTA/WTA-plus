import { findDuplicateRouteNames, restoreProtectedNavigation } from '@namewta/platform-app-runtime';
import { ElMessage } from 'element-plus/es';
import * as NProgressModule from 'nprogress';
import 'nprogress/nprogress.css';
import { isHandledRequestError, isRelogin } from '@/application/http';
import { getToken } from '@/application/session';
import { useNavigationStore } from '@/store/modules/navigation';
import { useSettingsStore } from '@/store/modules/settings';
import { useUserStore } from '@/store/modules/user';
import { isHttp, isPathMatch } from '@/utils/validate';
import router from './router';

const NProgress = ('default' in NProgressModule ? NProgressModule.default : NProgressModule) as typeof NProgressModule;

NProgress.configure({ showSpinner: false });
const whiteList = ['/login', '/register', '/social-callback', '/sso/callback', '/register*', '/register/*'];

const isWhiteList = (path: string) => {
  return whiteList.some(pattern => isPathMatch(pattern, path));
};

let recovery: { token: string; generation: number; promise: Promise<void> } | undefined;

router.beforeEach(async to => {
  NProgress.start();
  const user = useUserStore();
  const navigation = useNavigationStore();
  const token = getToken();
  if (!token) {
    if (isWhiteList(to.path)) return true;
    NProgress.done();
    return { path: '/login', query: { redirect: encodeURIComponent(to.fullPath || '/') } };
  }
  if (to.meta.title) useSettingsStore().setTitle(to.meta.title as string);
  if (to.path === '/login') { NProgress.done(); return { path: '/' }; }
  if (isWhiteList(to.path)) return true;
  if (user.identityLoaded && navigation.navigationLoaded && user.token === token) return true;
  const generation = user.sessionGeneration;
  const isCurrent = () => getToken() === token && user.sessionGeneration === generation;
  if (!recovery || recovery.token !== token || recovery.generation !== generation) {
    isRelogin.navigationPending = true;
    const promise = restoreProtectedNavigation({
      loadIdentity: () => user.getInfo(),
      loadRoutes: () => navigation.generateRoutes(),
      isCurrent,
      isExternal: route => isHttp(route.path),
      addRoute: route => {
        const existing = router.getRoutes().map(value => ({ name: value.name }));
        if (findDuplicateRouteNames([existing, [route]]).length) throw new Error('菜单路由名称冲突');
        navigation.registerRoute(route, value => router.addRoute(value));
      },
      createReplacement: () => { navigation.finishRecovery(); }
    }).finally(() => {
      if (recovery?.promise === promise) {
        isRelogin.navigationPending = false;
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
    return { path: '/login', query: { redirect: encodeURIComponent(to.fullPath || '/') } };
  } finally {
    NProgress.done();
  }
});

router.afterEach(() => {
  NProgress.done();
});
