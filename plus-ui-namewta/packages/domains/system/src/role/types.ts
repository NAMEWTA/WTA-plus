import type { BaseEntity, PageQuery } from '../types';

/**
 * 菜单树形结构类型
 */
export interface DeptTreeOption {
  id: string;
  label: string;
  parentId: string;
  weight: number;
  children?: DeptTreeOption[];
}

export interface RoleDeptTree {
  checkedKeys: string[];
  depts: DeptTreeOption[];
}

export interface RoleVO extends BaseEntity {
  roleId: string | number;
  roleName: string;
  roleKey: string;
  roleSort: number;
  dataScope: string;
  menuCheckStrictly: boolean;
  deptCheckStrictly: boolean;
  status: string;
  delFlag: string;
  remark?: any;
  flag: boolean;
  menuIds?: Array<string | number>;
  deptIds?: Array<string | number>;
  admin: boolean;
  /**
   * 归属客户端主键（sys_client.id / LoginUser.clientPk）
   */
  clientId?: string | number;
  /**
   * 客户端名称/Key，列表展示用
   */
  clientKey?: string;
  /**
   * 是否为 Client 默认角色（JSON: clientDefault，不写入 sys_user_role）
   */
  clientDefault?: boolean;
}

export interface RoleQuery extends PageQuery {
  roleName?: string;
  roleKey?: string;
  status?: string;
  /**
   * 归属客户端主键，列表/新增/查询必填
   */
  clientId?: string | number;
}

export interface RoleForm {
  roleName: string;
  roleKey: string;
  roleSort: number;
  status: string;
  menuCheckStrictly: boolean;
  deptCheckStrictly: boolean;
  remark: string;
  dataScope?: string;
  roleId: string | undefined;
  menuIds: Array<string | number>;
  deptIds: Array<string | number>;
  /**
   * 归属客户端主键，创建后只读
   */
  clientId?: string | number;
}
