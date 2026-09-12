import type { UserVO } from '@namewta/domain-system';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { identityAccessService } from '@/application/services';
import { getToken, removeToken, session } from '@/application/session';

export const useUserStore = defineStore('home-user', () => {
  const token = ref(getToken());
  const roles = ref<string[]>([]);
  const permissions = ref<string[]>([]);
  const nickname = ref('');
  const userId = ref<string | number>('');
  const login = async (input: Parameters<typeof identityAccessService.login>[0]) => { const value = await identityAccessService.login(input); token.value = value.accessToken; };
  const getInfo = async () => { const info = await identityAccessService.getInfo(); const user = info.user as UserVO; roles.value = [...info.roles]; permissions.value = [...info.permissions]; nickname.value = user.nickName || user.userName; userId.value = user.userId; };
  const logout = async () => { try { await identityAccessService.logout(); } finally { token.value = ''; roles.value = []; permissions.value = []; removeToken(); } };
  return { token, roles, permissions, nickname, userId, login, getInfo, logout, session };
});
