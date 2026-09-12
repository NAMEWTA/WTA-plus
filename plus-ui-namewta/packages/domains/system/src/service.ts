import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type { ClientForm, ClientQuery, ClientVO } from './client/types';
import type { DeptForm, DeptQuery, DeptTreeVO, DeptVO } from './dept/types';
import type { MenuForm, MenuQuery, MenuTreeOption, MenuVO, RoleMenuTree } from './menu/types';
import type { PostForm, PostQuery, PostVO } from './post/types';
import type { RoleDeptTree, RoleForm, RoleQuery, RoleVO } from './role/types';
import type { UserTypeForm, UserTypeQuery, UserTypeVO } from './user-type/types';
import type {
  ResetPasswordCandidate,
  TemporaryPassword,
  UserForm,
  UserInfoVO,
  UserProfileForm,
  UserProfileInfoVO,
  UserQuery,
  UserVO
} from './user/types';
import { createSystemResourceService, type SystemResourceService } from './resource-service';
import { projectResetPasswordCandidateTransport, projectTemporaryPasswordTransport } from './transport';
import type {
  ApiResponse,
  Identifier,
  IdentifierList,
  PageResult,
  RoleUserAssignment,
  RoleUserCancellation,
  UserRoleAssignment
} from './types';
import { createUserQueryPort, type UserQueryPort } from './user/public';

const segment = (value: IdentifierList) =>
  (Array.isArray(value) ? value : [value]).map(item => encodeURIComponent(String(item))).join(',');

export interface SystemService {
  readonly identity: {
    loadInfo(): Promise<unknown>;
    loadMenus(): Promise<unknown>;
  };
  readonly resources: SystemResourceService;
  readonly publicUsers: UserQueryPort;
  readonly users: {
    list(query: UserQuery): Promise<ApiResponse<PageResult<UserVO>>>;
    options(userIds: readonly Identifier[]): Promise<ApiResponse<UserVO[]>>;
    add(data: UserForm): Promise<ApiResponse>;
    authRoles(userId: Identifier, clientId: Identifier): Promise<ApiResponse<{ roles: RoleVO[]; user: UserVO }>>;
    changeStatus(userId: Identifier, status: string): Promise<ApiResponse>;
    delete(ids: IdentifierList): Promise<ApiResponse>;
    departmentTree(): Promise<ApiResponse<DeptTreeVO[]>>;
    get(userId?: Identifier, clientId?: Identifier): Promise<ApiResponse<UserInfoVO>>;
    listByDepartment(deptId: Identifier): Promise<ApiResponse<UserVO[]>>;
    profile(): Promise<ApiResponse<UserProfileInfoVO>>;
    passwordResetCandidate(userId: Identifier): Promise<ApiResponse<ResetPasswordCandidate>>;
    issueTemporaryPassword(userId: Identifier): Promise<ApiResponse<TemporaryPassword>>;
    resetPassword(userId: Identifier, password: string): Promise<ApiResponse>;
    unlock(userId: Identifier): Promise<ApiResponse>;
    update(data: UserForm): Promise<ApiResponse>;
    updateAuthRoles(data: UserRoleAssignment): Promise<ApiResponse>;
    updatePassword(oldPassword: string, newPassword: string): Promise<ApiResponse>;
    updateProfile(data: UserProfileForm): Promise<ApiResponse>;
  };
  readonly clients: CrudService<ClientQuery, ClientForm, ClientVO, [query?: ClientQuery]> & {
    changeStatus(clientId: string, status: string): Promise<ApiResponse>;
    options(): Promise<ClientVO[]>;
  };
  readonly userTypes: CrudService<UserTypeQuery, UserTypeForm, UserTypeVO, [query?: UserTypeQuery]> & {
    options(): Promise<ApiResponse<UserTypeVO[]>>;
  };
  readonly roles: CrudService<RoleQuery, RoleForm, RoleVO, [query: RoleQuery]> & {
    allocatedUsers(query: UserQuery): Promise<ApiResponse<PageResult<UserVO>>>;
    cancelUser(data: RoleUserCancellation): Promise<ApiResponse>;
    cancelUsers(data: RoleUserAssignment): Promise<ApiResponse>;
    changeStatus(roleId: Identifier, status: string): Promise<ApiResponse>;
    departmentTree(roleId: Identifier): Promise<ApiResponse<RoleDeptTree>>;
    options(roleIds: readonly Identifier[]): Promise<ApiResponse<RoleVO[]>>;
    selectUsers(data: RoleUserAssignment): Promise<ApiResponse>;
    unallocatedUsers(query: UserQuery): Promise<ApiResponse<PageResult<UserVO>>>;
    updatePermission(data: RoleForm): Promise<ApiResponse>;
  };
  readonly menus: {
    add(data: MenuForm): Promise<ApiResponse>;
    cascadeDelete(ids: readonly Identifier[]): Promise<ApiResponse>;
    delete(id: Identifier): Promise<ApiResponse>;
    get(id: Identifier): Promise<ApiResponse<MenuVO>>;
    list(query?: MenuQuery): Promise<ApiResponse<MenuVO[]>>;
    roleTree(roleId: Identifier): Promise<ApiResponse<RoleMenuTree>>;
    tree(clientId?: Identifier): Promise<ApiResponse<MenuTreeOption[]>>;
    update(data: MenuForm): Promise<ApiResponse>;
  };
  readonly departments: {
    add(data: DeptForm): Promise<ApiResponse>;
    delete(id: Identifier): Promise<ApiResponse>;
    excludeChildren(id: Identifier): Promise<ApiResponse<DeptVO[]>>;
    get(id: Identifier): Promise<ApiResponse<DeptVO>>;
    list(query?: DeptQuery): Promise<ApiResponse<DeptVO[]>>;
    options(ids: readonly Identifier[]): Promise<ApiResponse<DeptVO[]>>;
    update(data: DeptForm): Promise<ApiResponse>;
  };
  readonly posts: CrudService<PostQuery, PostForm, PostVO, [query: PostQuery]> & {
    departmentTree(): Promise<ApiResponse<DeptTreeVO[]>>;
    options(deptId?: Identifier, postIds?: readonly Identifier[]): Promise<ApiResponse<PostVO[]>>;
  };
}

interface CrudService<Query, Form, View, ListArgs extends [query?: Query] | [query: Query]> {
  add(data: Form): Promise<ApiResponse>;
  changeStatus?(id: Identifier, status: string): Promise<ApiResponse>;
  delete(ids: IdentifierList): Promise<ApiResponse>;
  get(id: Identifier): Promise<ApiResponse<View>>;
  list(...args: ListArgs): Promise<ApiResponse<PageResult<View>>>;
  update(data: Form): Promise<ApiResponse>;
}

export function createSystemService(http: HttpClient): SystemService {
  const request = <T = unknown>(config: HttpRequest) => http.request<ApiResponse<T>>(config);
  const publicUsers = createUserQueryPort(http);
  const clients = Object.freeze({
    list: (params?: ClientQuery) =>
      request<PageResult<ClientVO>>({ url: '/system/client/list', method: 'get', params }),
    get: (id: Identifier) => request<ClientVO>({ url: '/system/client/' + segment(id), method: 'get' }),
    add: (data: ClientForm) => request({ url: '/system/client', method: 'post', data }),
    update: (data: ClientForm) => request({ url: '/system/client', method: 'put', data }),
    delete: (ids: IdentifierList) => request({ url: '/system/client/' + segment(ids), method: 'delete' }),
    changeStatus: (clientId: string, status: string) =>
      request({ url: '/system/client/changeStatus', method: 'put', data: { clientId, status } }),
    options: async () => (await clients.list({ pageNum: 1, pageSize: 1000 })).data?.rows ?? []
  });
  const userTypes = Object.freeze({
    list: (params?: UserTypeQuery) =>
      request<PageResult<UserTypeVO>>({ url: '/system/userType/list', method: 'get', params }),
    get: (id: Identifier) => request<UserTypeVO>({ url: '/system/userType/' + segment(id), method: 'get' }),
    options: () => request<UserTypeVO[]>({ url: '/system/userType/options', method: 'get' }),
    add: (data: UserTypeForm) => request({ url: '/system/userType', method: 'post', data }),
    update: (data: UserTypeForm) => request({ url: '/system/userType', method: 'put', data }),
    delete: (ids: IdentifierList) => request({ url: '/system/userType/' + segment(ids), method: 'delete' })
  });
  const roles = Object.freeze({
    list: (params: RoleQuery) => request<PageResult<RoleVO>>({ url: '/system/role/list', method: 'get', params }),
    options: (ids: readonly Identifier[]) =>
      request<RoleVO[]>({ url: '/system/role/optionselect?roleIds=' + segment(ids), method: 'get' }),
    get: (id: Identifier) => request<RoleVO>({ url: '/system/role/' + segment(id), method: 'get' }),
    add: (data: RoleForm) => request({ url: '/system/role', method: 'post', data }),
    update: (data: RoleForm) => request({ url: '/system/role', method: 'put', data }),
    updatePermission: (data: RoleForm) => request({ url: '/system/role/permission', method: 'put', data }),
    changeStatus: (roleId: Identifier, status: string) =>
      request({ url: '/system/role/changeStatus', method: 'put', data: { roleId, status } }),
    delete: (ids: IdentifierList) => request({ url: '/system/role/' + segment(ids), method: 'delete' }),
    allocatedUsers: (params: UserQuery) =>
      request<PageResult<UserVO>>({ url: '/system/role/authUser/allocatedList', method: 'get', params }),
    unallocatedUsers: (params: UserQuery) =>
      request<PageResult<UserVO>>({ url: '/system/role/authUser/unallocatedList', method: 'get', params }),
    cancelUser: (data: RoleUserCancellation) => request({ url: '/system/role/authUser/cancel', method: 'put', data }),
    cancelUsers: (params: RoleUserAssignment) =>
      request({ url: '/system/role/authUser/cancelAll', method: 'put', params }),
    selectUsers: (params: RoleUserAssignment) =>
      request({ url: '/system/role/authUser/selectAll', method: 'put', params }),
    departmentTree: (id: Identifier) =>
      request<RoleDeptTree>({ url: '/system/role/deptTree/' + segment(id), method: 'get' })
  });
  return Object.freeze({
    identity: Object.freeze({
      loadInfo: async () => (await request<unknown>({ url: '/system/user/getInfo', method: 'get' })).data,
      loadMenus: async () => (await request<unknown>({ url: '/system/menu/getRouters', method: 'get' })).data
    }),
    resources: createSystemResourceService(http),
    clients,
    publicUsers,
    userTypes,
    roles,
    users: Object.freeze({
      list: (params: UserQuery) => request<PageResult<UserVO>>({ url: '/system/user/list', method: 'get', params }),
      options: (ids: readonly Identifier[]) =>
        request<UserVO[]>({ url: '/system/user/optionselect?userIds=' + segment(ids), method: 'get' }),
      get: (id?: Identifier, clientId?: Identifier) =>
        request<UserInfoVO>({
          url: '/system/user/' + (id === undefined ? '' : segment(id)),
          method: 'get',
          params: clientId === undefined ? undefined : { clientId }
        }),
      add: (data: UserForm) => request({ url: '/system/user', method: 'post', data }),
      update: (data: UserForm) => request({ url: '/system/user', method: 'put', data }),
      delete: (ids: IdentifierList) => request({ url: '/system/user/' + segment(ids), method: 'delete' }),
      resetPassword: (userId: Identifier, password: string) =>
        request({
          url: '/system/user/resetPwd',
          method: 'put',
          headers: { isEncrypt: true, repeatSubmit: false },
          data: { userId, password }
        }),
      passwordResetCandidate: async (userId: Identifier) => {
        const response = await request({
          url: '/system/user/resetPwd/candidate',
          method: 'post',
          headers: { 'Cache-Control': 'no-store', repeatSubmit: false },
          data: { userId }
        });
        return { ...response, data: projectResetPasswordCandidateTransport(response.data) };
      },
      issueTemporaryPassword: async (userId: Identifier) => {
        const response = await request({
          url: '/system/user/temporaryPassword',
          method: 'post',
          headers: { 'Cache-Control': 'no-store', repeatSubmit: false },
          data: { userId }
        });
        return { ...response, data: projectTemporaryPasswordTransport(response.data) };
      },
      changeStatus: (userId: Identifier, status: string) =>
        request({ url: '/system/user/changeStatus', method: 'put', data: { userId, status } }),
      unlock: (id: Identifier) => request({ url: '/system/user/unlock/' + segment(id), method: 'get' }),
      profile: () => request<UserProfileInfoVO>({ url: '/system/user/profile', method: 'get' }),
      updateProfile: (data: UserProfileForm) => request({ url: '/system/user/profile', method: 'put', data }),
      updatePassword: (oldPassword: string, newPassword: string) =>
        request({
          url: '/system/user/profile/updatePwd',
          method: 'put',
          headers: { isEncrypt: true, repeatSubmit: false },
          data: { oldPassword, newPassword }
        }),
      authRoles: (id: Identifier, clientId: Identifier) =>
        request<{ roles: RoleVO[]; user: UserVO }>({
          url: '/system/user/authRole/' + segment(id),
          method: 'get',
          params: { clientId }
        }),
      updateAuthRoles: (params: UserRoleAssignment) => request({ url: '/system/user/authRole', method: 'put', params }),
      listByDepartment: (id: Identifier) =>
        request<UserVO[]>({ url: '/system/user/list/dept/' + segment(id), method: 'get' }),
      departmentTree: () => request<DeptTreeVO[]>({ url: '/system/user/deptTree', method: 'get' })
    }),
    menus: Object.freeze({
      list: (params?: MenuQuery) => request<MenuVO[]>({ url: '/system/menu/list', method: 'get', params }),
      get: (id: Identifier) => request<MenuVO>({ url: '/system/menu/' + segment(id), method: 'get' }),
      tree: (clientId?: Identifier) =>
        request<MenuTreeOption[]>({ url: '/system/menu/treeselect', method: 'get', params: { clientId } }),
      roleTree: (id: Identifier) =>
        request<RoleMenuTree>({ url: '/system/menu/roleMenuTreeselect/' + segment(id), method: 'get' }),
      add: (data: MenuForm) => request({ url: '/system/menu', method: 'post', data }),
      update: (data: MenuForm) => request({ url: '/system/menu', method: 'put', data }),
      delete: (id: Identifier) => request({ url: '/system/menu/' + segment(id), method: 'delete' }),
      cascadeDelete: (ids: readonly Identifier[]) =>
        request({ url: '/system/menu/cascade/' + segment(ids), method: 'delete' })
    }),
    departments: Object.freeze({
      list: (params?: DeptQuery) => request<DeptVO[]>({ url: '/system/dept/list', method: 'get', params }),
      options: (ids: readonly Identifier[]) =>
        request<DeptVO[]>({ url: '/system/dept/optionselect?deptIds=' + segment(ids), method: 'get' }),
      excludeChildren: (id: Identifier) =>
        request<DeptVO[]>({ url: '/system/dept/list/exclude/' + segment(id), method: 'get' }),
      get: (id: Identifier) => request<DeptVO>({ url: '/system/dept/' + segment(id), method: 'get' }),
      add: (data: DeptForm) => request({ url: '/system/dept', method: 'post', data }),
      update: (data: DeptForm) => request({ url: '/system/dept', method: 'put', data }),
      delete: (id: Identifier) => request({ url: '/system/dept/' + segment(id), method: 'delete' })
    }),
    posts: Object.freeze({
      list: (params: PostQuery) => request<PageResult<PostVO>>({ url: '/system/post/list', method: 'get', params }),
      get: (id: Identifier) => request<PostVO>({ url: '/system/post/' + segment(id), method: 'get' }),
      options: (deptId?: Identifier, postIds?: readonly Identifier[]) =>
        request<PostVO[]>({ url: '/system/post/optionselect', method: 'get', params: { deptId, postIds } }),
      add: (data: PostForm) => request({ url: '/system/post', method: 'post', data }),
      update: (data: PostForm) => request({ url: '/system/post', method: 'put', data }),
      delete: (ids: IdentifierList) => request({ url: '/system/post/' + segment(ids), method: 'delete' }),
      departmentTree: () => request<DeptTreeVO[]>({ url: '/system/post/deptTree', method: 'get' })
    })
  });
}
