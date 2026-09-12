import { restoreProtectedNavigation } from '@namewta/platform-app-runtime';
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
const whiteList = ['/login', '/register', '/social-callback', '/register*', '/register/*'];

const isWhiteList = (path: string) => {
  return whiteList.some(pattern => isPathMatch(pattern, path));
};

router.beforeEach(async (to, from) => {
  NProgress.start();
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title as string);
    /* has token*/
    if (to.path === '/login') {
      NProgress.done();
      return { path: '/' };
    } else if (isWhiteList(to.path)) {
      return true;
    } else {
      if (useUserStore().roles.length === 0) {
        isRelogin.show = true;
        try {
          return await restoreProtectedNavigation({
            loadIdentity: () => useUserStore().getInfo(),
            loadRoutes: () => useNavigationStore().generateRoutes(),
            isExternal: route => isHttp(route.path),
            addRoute: route => router.addRoute(route),
            createReplacement: () => {
              isRelogin.show = false;
              return {
                path: to.path,
                replace: true,
                params: to.params,
                query: to.query,
                hash: to.hash,
                name: to.name as string
              };
            }
          });
        } catch (err) {
          await useUserStore().logout();
          if (!isHandledRequestError(err)) {
            ElMessage.error(err instanceof Error ? err.message : String(err));
          }
          return { path: '/' };
        }
      } else {
        return true;
      }
    }
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      return true;
    } else {
      const redirect = encodeURIComponent(to.fullPath || '/');
      NProgress.done();
      return `/login?redirect=${redirect}`; // 否则全部重定向到登录页
    }
  }
});

router.afterEach(() => {
  NProgress.done();
});
