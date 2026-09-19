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

function userRecord(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('用户响应不可用');
  return value as Record<string, unknown>;
}

function optionalUserText(value: unknown): string | undefined {
  if (value === null || value === undefined) return undefined;
  if (typeof value !== 'string') throw new Error('用户响应不可用');
  return value;
}

export const projectUserSummary = (value: unknown): UserSummary => {
  const source = userRecord(value);
  const userId = source.userId;
  if (!(typeof userId === 'string' && userId.trim()) && !(typeof userId === 'number' && Number.isFinite(userId))) {
    throw new Error('用户响应不可用');
  }
  return {
    userId,
    userName: optionalUserText(source.userName),
    nickName: optionalUserText(source.nickName) ?? '',
    phoneNumber: optionalUserText(source.phoneNumber),
    deptName: optionalUserText(source.deptName),
    status: optionalUserText(source.status)
  };
};

function userResponse(value: unknown): UserQueryResponse<unknown> {
  const response = userRecord(value);
  const code = response.code;
  if (code !== undefined && (typeof code !== 'number' || !Number.isFinite(code))) throw new Error('用户响应不可用');
  return { code: typeof code === 'number' ? code : undefined, msg: optionalUserText(response.msg), data: response.data };
}

export function createUserQueryPort(http: HttpClient): UserQueryPort {
  return Object.freeze<UserQueryPort>({
    list: async query => {
      const response = userResponse(await http.request<unknown>({
        url: '/system/user/list',
        method: 'get',
        params: query
      }));
      const data = userRecord(response.data);
      if (!Array.isArray(data.rows) || typeof data.total !== 'number' || !Number.isInteger(data.total) || data.total < 0) {
        throw new Error('用户响应不可用');
      }
      return { ...response, data: { total: data.total, rows: data.rows.map(projectUserSummary) } };
    },
    options: async userIds => {
      const response = userResponse(await http.request<unknown>({
        url: '/system/user/optionselect?userIds=' + identifiers(userIds),
        method: 'get'
      }));
      if (!Array.isArray(response.data)) throw new Error('用户响应不可用');
      return { ...response, data: response.data.map(projectUserSummary) };
    },
    departmentTree: () =>
      http.request<UserQueryResponse<DepartmentSummary[]>>({ url: '/system/user/deptTree', method: 'get' })
  });
}
