import type { PostVO } from '../post/types';
import type { RoleVO } from '../role/types';
import type { BaseEntity, PageQuery } from '../types';

/**
 * 用户信息
 */
export interface UserInfo {
  user: UserVO;
  roles: string[];
  permissions: string[];
}

/**
 * 用户查询对象类型
 */
export interface UserQuery extends PageQuery {
  userName?: string;
  nickName?: string;
  phoneNumber?: string;
  status?: string;
  deptId?: string | number;
  roleId?: string | number;
  userIds?: string | number | (string | number)[] | undefined;
}

/**
 * 用户返回对象
 */
export interface UserVO extends BaseEntity {
  userId: string | number;
  tenantId: string;
  deptId: number;
  userName: string;
  nickName: string;
  email: string;
  phoneNumber: string;
  gender: string;
  avatar?: string | number;
  avatarUrl?: string;
  status: string;
  delFlag: string;
  loginIp: string;
  loginDate: string;
  remark: string;
  deptName: string;
  /** 详情接口可能返回嵌套部门 */
  dept?: { deptName?: string };
  roles: RoleVO[];
  roleIds?: Array<string | number>;
  postIds?: Array<string | number>;
  roleId?: string | number;
  admin: boolean;
  /**
   * 拥有的登录域 ID
   */
  userTypeIds?: Array<string | number>;
  /**
   * 拥有的登录域编码
   */
  userTypeCodes?: string[];
  /**
   * 拥有的登录域名称
   */
  userTypeNames?: string[];
}

/**
 * 用户表单类型
 */
export interface UserForm {
  id?: string;
  userId?: string | number;
  deptId?: string | number | null;
  userName: string;
  nickName?: string;
  password: string;
  phoneNumber?: string;
  email?: string;
  gender?: string;
  status: string;
  remark?: string;
  avatar?: string | number;
  postIds: Array<string | number> | null;
  roleIds: Array<string | number> | null;
  /**
   * 登录域 ID 列表
   */
  userTypeIds?: Array<string | number>;
}

/**
 * 个人资料表单类型
 */
export interface UserProfileForm {
  nickName?: string;
  phoneNumber?: string;
  email?: string;
  gender?: string;
  avatar?: string | number;
}

export interface UserInfoVO {
  password?: string;
  user?: UserVO;
  roles: RoleVO[];
  roleIds: Array<string | number>;
  posts: PostVO[];
  postIds: Array<string | number>;
}

export interface ResetPasswordCandidate {
  password: string;
}

export interface TemporaryPassword {
  expiresInSeconds: number;
  password: string;
}

export interface UserProfileInfoVO {
  user: UserVO;
  roleGroup: string;
  postGroup: string;
}

export interface ResetPwdForm {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}
