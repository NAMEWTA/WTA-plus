import type { ApiErrorInfo } from '@namewta/platform-contracts';

export interface BaseEntity {
  createBy?: string;
  createDept?: string | number;
  createTime?: string;
  params?: Record<string, unknown>;
  remark?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
  orderByColumn?: string;
  isAsc?: string;
  params?: Record<string, unknown>;
}

export type Identifier = string | number;
export type IdentifierList = Identifier | readonly Identifier[];

export interface ApiResponse<T = unknown> {
  code?: number;
  data: T;
  msg?: string;
  error?: ApiErrorInfo;
}

export interface PageResult<T = unknown> {
  rows: T[];
  total: number;
}

export interface UserRoleAssignment {
  clientId: Identifier;
  roleIds: string;
  userId: string;
}

export interface RoleUserAssignment {
  roleId: Identifier;
  userIds: string;
}

export interface RoleUserCancellation {
  roleId: Identifier;
  userId: Identifier;
}
