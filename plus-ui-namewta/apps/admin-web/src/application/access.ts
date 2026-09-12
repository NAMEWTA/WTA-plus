import { createAccessEvaluator } from '@namewta/platform-permission';
import { useUserStore } from '@/store/modules/user';

/** 基于当前 Admin 会话创建权限求值器。 */
export const createAdminAccessEvaluator = () => {
  const user = useUserStore();
  return createAccessEvaluator({ permissions: user.permissions, roles: user.roles });
};
