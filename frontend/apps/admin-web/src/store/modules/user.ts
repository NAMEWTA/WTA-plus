import type { PasswordLoginInput } from '@namewta/domain-admin';
import type { UserVO } from '@namewta/domain-system';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { adminHttp, isRelogin } from '@/application/http';
import { identityAccessService } from '@/application/services';
import { getToken, removeToken } from '@/application/session';
import defAva from '@/assets/images/profile.jpg';
import { closePush } from '@/utils/push';
import { useNavigationStore } from './navigation';
import { useNoticeStore } from './notice';
import { useTagsViewStore } from './tagsView';

export interface AdminLoginInput extends PasswordLoginInput {
  rememberMe?: boolean;
}

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken());
  const name = ref('');
  const nickname = ref('');
  const userId = ref<string | number>('');
  const avatar = ref('');
  const roles = ref<Array<string>>([]); // 用户角色编码集合 → 判断路由权限
  const permissions = ref<Array<string>>([]); // 用户权限编码集合 → 判断按钮权限
  const identityLoaded = ref(false);
  const sessionGeneration = ref(0);
  let logoutAttempt: Promise<void> | undefined;

  /**
   * 登录
   * @param userInfo
   * @returns
   */
  const login = async (userInfo: AdminLoginInput): Promise<void> => {
    const current = ++sessionGeneration.value;
    const session = await identityAccessService.login(userInfo);
    if (current !== sessionGeneration.value) return;
    identityLoaded.value = false;
    useNavigationStore().resetRoutes();
    token.value = session.accessToken;
  };

  // 获取用户信息
  const getInfo = async (): Promise<void> => {
    const current = sessionGeneration.value;
    const requestedToken = getToken();
    token.value = requestedToken;
    const data = await identityAccessService.getInfo();
    if (current !== sessionGeneration.value || requestedToken !== getToken()) return;
    const user = data.user as UserVO;
    const profile = user.avatarUrl == '' || user.avatarUrl == null ? defAva : user.avatarUrl;

    roles.value = [...data.roles];
    permissions.value = [...data.permissions];
    name.value = user.userName;
    nickname.value = user.nickName;
    avatar.value = profile;
    userId.value = user.userId;
    identityLoaded.value = true;
  };

  const clearLocalSession = () => {
    sessionGeneration.value++;
    identityLoaded.value = false;
    token.value = '';
    roles.value = [];
    permissions.value = [];
    name.value = '';
    nickname.value = '';
    userId.value = '';
    avatar.value = '';
    removeToken();
    adminHttp.cancelPending();
    closePush();
    useNavigationStore().resetRoutes();
    useNoticeStore().clearNotice();
    useTagsViewStore().resetSession();
  };

  // Keep one bounded remote attempt; remote failure cannot skip local teardown.
  const logout = (): Promise<void> => {
    if (logoutAttempt) return logoutAttempt;
    const current = ++sessionGeneration.value;
    const previousToken = getToken();
    const wasRecovering = isRelogin.show;
    isRelogin.show = true;
    closePush();
    adminHttp.cancelPending();
    const attempt = (async () => {
      try {
        await identityAccessService.logout();
      } finally {
        if (current === sessionGeneration.value && (!getToken() || getToken() === previousToken)) clearLocalSession();
        isRelogin.show = wasRecovering;
      }
    })();
    logoutAttempt = attempt;
    void attempt.finally(() => { if (logoutAttempt === attempt) logoutAttempt = undefined; }).catch(() => undefined);
    return attempt;
  };

  const setAvatar = (value: string) => {
    avatar.value = value;
  };

  return {
    userId,
    token,
    nickname,
    avatar,
    roles,
    permissions,
    identityLoaded,
    sessionGeneration,
    clearLocalSession,
    login,
    getInfo,
    logout,
    setAvatar
  };
});
