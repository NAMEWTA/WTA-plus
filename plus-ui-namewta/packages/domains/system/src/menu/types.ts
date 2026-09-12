import type { BaseEntity } from '../types';

export type MenuType = 'C' | 'F' | 'M';

/**
 * 菜单树形结构类型
 */
export interface MenuTreeOption {
  id: string | number;
  label: string;
  parentId: string | number;
  weight: number;
  menuType?: MenuType | string;
  visible?: string;
  status?: string;
  disabled?: boolean;
  children?: MenuTreeOption[];
}

export interface RoleMenuTree {
  menus: MenuTreeOption[];
  checkedKeys: Array<string | number>;
}

/**
 * 角色菜单分配中的按钮节点类型
 */
export interface RoleMenuButtonOption {
  menuId: string | number;
  menuName: string;
  parentId: string | number;
  perms?: string;
  status?: string;
  disabled?: boolean;
}

/**
 * 菜单查询参数类型
 */
export interface MenuQuery {
  keywords?: string;
  menuName?: string;
  status?: string;
  /**
   * 归属客户端主键，列表/新增/查询必填
   */
  clientId?: string | number;
}

/**
 * 菜单视图对象类型
 */
export interface MenuVO extends BaseEntity {
  parentName: string;
  parentId: string | number;
  children: MenuVO[];
  menuId: string | number;
  menuName: string;
  orderNum: number;
  path: string;
  component: string;
  queryParam: string;
  isFrame: string;
  isCache: string;
  menuType: MenuType;
  visible: string;
  status: string;
  /** 图标协议：本地 SVG 名称、tabler:name，或显式外部 Iconify 名称 */
  icon: string;
  activeMenu: string;
  ext: string;
  remark: string;
  /**
   * 归属客户端主键（sys_client.id）
   */
  clientId?: string | number;
}

export interface MenuForm {
  parentName?: string;
  parentId?: string | number;
  children?: MenuForm[];
  menuId?: string | number;
  menuName: string;
  orderNum: number;
  path: string;
  component?: string;
  queryParam?: string;
  isFrame?: string;
  isCache?: string;
  menuType?: MenuType;
  visible?: string;
  status?: string;
  /** 图标协议：本地 SVG 名称、tabler:name，或显式外部 Iconify 名称 */
  icon?: string;
  activeMenu?: string;
  ext?: string;
  remark?: string;
  query?: string;
  perms?: string;
  /**
   * 归属客户端主键，创建后只读
   */
  clientId?: string | number;
}
