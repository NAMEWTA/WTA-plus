import { projectSystemUserTransport } from '@namewta/domain-system';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { identityAccessService } from '@/application/services';
import { getToken, removeToken, session } from '@/application/session';
import { homeHttp, relogin } from '@/application/http';
import { useNavigationStore } from './navigation';

export const useUserStore = defineStore('home-user', () => {
  const token = ref(getToken());
  const roles = ref<string[]>([]);
  const permissions = ref<string[]>([]);
  const nickname = ref('');
  const userId = ref<string | number>('');
  const identityLoaded = ref(false);
  const sessionGeneration = ref(0);
  let logoutAttempt: Promise<void> | undefined;

  const login = async (input: Parameters<typeof identityAccessService.login>[0]) => {
    const current = ++sessionGeneration.value;
    const value = await identityAccessService.login(input);
    if (current !== sessionGeneration.value) return;
    identityLoaded.value = false;
    useNavigationStore().resetRoutes();
    token.value = value.accessToken;
  };
  const getInfo = async () => {
    const current = sessionGeneration.value;
    const requestedToken = getToken();
    token.value = getToken();
    const info = await identityAccessService.getInfo();
    if (current !== sessionGeneration.value || requestedToken !== getToken()) return;
    const user = projectSystemUserTransport(info.user);
    roles.value = [...info.roles];
    permissions.value = [...info.permissions];
    nickname.value = user.nickName || user.userName || '';
    userId.value = user.userId;
    identityLoaded.value = true;
  };
  const clearLocalSession = () => {
    sessionGeneration.value++;
    token.value = '';
    roles.value = [];
    permissions.value = [];
    nickname.value = '';
    userId.value = '';
    identityLoaded.value = false;
    removeToken();
    homeHttp.cancelPending();
    useNavigationStore().resetRoutes();
  };
  const logout = (): Promise<void> => {
    if (logoutAttempt) return logoutAttempt;
    const current = ++sessionGeneration.value;
    const previousToken = getToken();
    const wasRecovering = relogin.show;
    relogin.show = true;
    homeHttp.cancelPending();
    const attempt = (async () => {
      try {
        await identityAccessService.logout();
      } finally {
        if (current === sessionGeneration.value && (!getToken() || getToken() === previousToken)) clearLocalSession();
        relogin.show = wasRecovering;
      }
    })();
    logoutAttempt = attempt;
    void attempt.finally(() => { if (logoutAttempt === attempt) logoutAttempt = undefined; }).catch(() => undefined);
    return attempt;
  };
  return { token, roles, permissions, nickname, userId, identityLoaded, sessionGeneration, login, getInfo, logout, clearLocalSession, session };
});
