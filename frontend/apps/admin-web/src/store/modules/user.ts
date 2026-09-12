import type { PasswordLoginInput } from '@namewta/domain-admin';
import type { UserVO } from '@namewta/domain-system';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { adminHttp } from '@/application/http';
import { identityAccessService } from '@/application/services';
import { getToken, removeToken } from '@/application/session';
import defAva from '@/assets/images/profile.jpg';
import { closePush } from '@/utils/push';

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

  /**
   * 登录
   * @param userInfo
   * @returns
   */
  const login = async (userInfo: AdminLoginInput): Promise<void> => {
    const session = await identityAccessService.login(userInfo);
    token.value = session.accessToken;
  };

  // 获取用户信息
  const getInfo = async (): Promise<void> => {
    const data = await identityAccessService.getInfo();
    const user = data.user as UserVO;
    const profile = user.avatarUrl == '' || user.avatarUrl == null ? defAva : user.avatarUrl;

    roles.value = data.roles.length ? [...data.roles] : ['ROLE_DEFAULT'];
    permissions.value = [...data.permissions];
    name.value = user.userName;
    nickname.value = user.nickName;
    avatar.value = profile;
    userId.value = user.userId;
  };

  // 注销
  const logout = async (): Promise<void> => {
    closePush();
    if (
      import.meta.env.VITE_APP_MESSAGE_ENABLED === 'true' &&
      import.meta.env.VITE_APP_MESSAGE_TRANSPORT.toLowerCase() === 'sse'
    ) {
      void adminHttp
        .request({ url: import.meta.env.VITE_APP_MESSAGE_PATH + '/close', method: 'get' })
        .catch(() => undefined);
    }
    await identityAccessService.logout();
    token.value = '';
    roles.value = [];
    permissions.value = [];
    removeToken();
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
    login,
    getInfo,
    logout,
    setAvatar
  };
});
