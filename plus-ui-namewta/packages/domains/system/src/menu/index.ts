export * from './public';
export type {
  MenuForm,
  MenuQuery,
  MenuTreeOption,
  MenuType,
  MenuVO,
  RoleMenuButtonOption,
  RoleMenuTree
} from './types';

export const systemMenuResource = Object.freeze({ controller: 'SysMenuController', basePath: '/system/menu' });
