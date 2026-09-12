import { createAccessEvaluator } from '@namewta/platform-permission';
import { useUserStore } from '@/store/user';
export const hasPermission = (permission: string) => { const user = useUserStore(); return createAccessEvaluator({ permissions: user.permissions, roles: user.roles }).hasPermission(permission); };
