import { createSocialWebRuntime } from '@namewta/web-domain-admin';
import { useUserStore } from '@/store/modules/user';
import { identityAccessService } from './services';
import { session } from './session';

export function createAppSocialRuntime() {
  const user = useUserStore();
  return createSocialWebRuntime({
    service: identityAccessService,
    storage: window.sessionStorage,
    namespace: 'admin-web:' + import.meta.env.VITE_APP_CLIENT_ID,
    defaultReturnPath: '/index',
    bindingReturnPath: '/user/profile?tab=thirdParty',
    owner: () => session.getToken() || '',
    snapshot: () => String(user.sessionGeneration) + ':' + (session.getToken() || ''),
    navigateExternal: url => window.location.assign(url),
    navigate: async path => {
      const { default: router } = await import('@/router');
      await router.replace(path);
    },
    acceptToken: token => {
      user.clearLocalSession();
      session.setToken(token);
      user.token = token;
    },
    clearSession: () => user.clearLocalSession()
  });
}
