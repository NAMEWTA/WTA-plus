import type { OpenApiSchema } from '@namewta/api-contracts';
import type { HttpClient } from '@namewta/platform-contracts';

export type SystemUserTransport = OpenApiSchema<'SysUserVo'>;

export interface UserSummary {
  phoneNumber?: string;
  deptName?: string;
  nickName: string;
  status?: string;
  userId: string | number;
  userName?: string;
}

export interface UserQuery {
  keyword?: string;
  createTime?: readonly string[];
  deptId?: string | number;
  pageNum?: number;
  pageSize?: number;
  phoneNumber?: string;
  status?: string;
  userIds?: string | number | readonly (string | number)[];
  userName?: string;
}

export interface DepartmentSummary {
  children?: readonly DepartmentSummary[];
  disabled?: boolean;
  id: string | number;
  label: string;
}

export interface UserPage {
  rows: UserSummary[];
  total: number;
}

export interface UserQueryResponse<T> {
  code?: number;
  data: T;
  msg?: string;
}

export interface UserQueryPort {
  departmentTree(): Promise<UserQueryResponse<DepartmentSummary[]>>;
  list(query: UserQuery): Promise<UserQueryResponse<UserPage>>;
  options(userIds: readonly (string | number)[]): Promise<UserQueryResponse<UserSummary[]>>;
}

const identifiers = (values: readonly (string | number)[]) =>
  values.map(value => encodeURIComponent(String(value))).join(',');

export const projectUserSummary = (source: SystemUserTransport): UserSummary => {
  return {
    userId: source.userId ?? '',
    userName: source.userName,
    nickName: source.nickName ?? '',
    phoneNumber: source.phoneNumber,
    deptName: source.deptName,
    status: source.status
  };
};

export function createUserQueryPort(http: HttpClient): UserQueryPort {
  return Object.freeze({
    list: async query => {
      const response = await http.request<UserQueryResponse<{ rows: SystemUserTransport[]; total: number }>>({
        url: '/system/user/list',
        method: 'get',
        params: query
      });
      return { ...response, data: { ...response.data, rows: response.data.rows.map(projectUserSummary) } };
    },
    options: async userIds => {
      const response = await http.request<UserQueryResponse<SystemUserTransport[]>>({
        url: '/system/user/optionselect?userIds=' + identifiers(userIds),
        method: 'get'
      });
      return { ...response, data: response.data.map(projectUserSummary) };
    },
    departmentTree: () =>
      http.request<UserQueryResponse<DepartmentSummary[]>>({ url: '/system/user/deptTree', method: 'get' })
  });
}
