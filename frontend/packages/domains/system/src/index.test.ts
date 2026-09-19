import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, expectTypeOf, it, vi } from 'vitest';
import {
  createSystemService,
  systemDomainModule,
  systemSsoAppResource,
  type ApiResponse,
  type ClientForm,
  type ClientQuery,
  type ClientVO,
  type DeptQuery,
  type DeptVO,
  type MenuQuery,
  type MenuVO,
  type PageResult,
  type PostQuery,
  type PostVO,
  type RoleForm,
  type RoleQuery,
  type RoleVO,
  type ResetPasswordCandidate,
  type TemporaryPassword,
  type UserQuery,
  type UserTypeQuery,
  type UserTypeVO,
  type UserVO
} from './index';

describe('system transport contracts', () => {
  it('preserves every migrated endpoint and method for all seven slices', async () => {
    const requests: HttpRequest[] = [];
    const http: HttpClient = {
      request: vi.fn(async request => {
        requests.push(request);
        return { code: 200, data: request.url.endsWith('/list') ? { rows: [], total: 0 } : {} } as never;
      })
    };
    const service = createSystemService(http);

    const input = { marker: 'preserved' } as never;
    const query = { pageNum: 1, pageSize: 10 } as never;
    const cases: Array<[HttpRequest, () => Promise<unknown>]> = [
      [{ url: '/system/client/list', method: 'get', params: query }, () => service.clients.list(query)],
      [{ url: '/system/client/client%2F1', method: 'get' }, () => service.clients.get('client/1')],
      [{ url: '/system/client', method: 'post', data: input }, () => service.clients.add(input)],
      [{ url: '/system/client/update', method: 'post', data: input }, () => service.clients.update(input)],
      [
        { url: '/system/client/client%2F1,client%2F2', method: 'post' },
        () => service.clients.delete(['client/1', 'client/2'])
      ],
      [
        { url: '/system/client/changeStatus', method: 'post', data: { clientId: 'client-a', status: '1' } },
        () => service.clients.changeStatus('client-a', '1')
      ],
      [
        { url: '/system/client/sso/bind', method: 'post', data: { id: '1', ssoAuthMode: 'both' } },
        () => service.clients.bindSsoAccess('1', 'both')
      ],
      [{ url: '/system/ssoApp/list', method: 'get', params: query }, () => service.ssoApps.list(query)],
      [{ url: '/system/ssoApp/app%2F1', method: 'get' }, () => service.ssoApps.get('app/1')],
      [{ url: '/system/ssoApp', method: 'post', data: input }, () => service.ssoApps.add(input)],
      [{ url: '/system/ssoApp/update', method: 'post', data: input }, () => service.ssoApps.update(input)],
      [
        { url: '/system/ssoApp/rotateSecret', method: 'post', data: { id: 'app/1' } },
        () => service.ssoApps.rotateSecret('app/1')
      ],

      [{ url: '/system/user/list', method: 'get', params: query }, () => service.users.list(query)],
      [
        { url: '/system/user/optionselect?userIds=user%2F1,user%2F2', method: 'get' },
        () => service.users.options(['user/1', 'user/2'])
      ],
      [
        { url: '/system/user/user%2F1', method: 'get', params: { clientId: 'client/1' } },
        () => service.users.get('user/1', 'client/1')
      ],
      [{ url: '/system/user/', method: 'get', params: undefined }, () => service.users.get()],
      [{ url: '/system/user', method: 'post', data: input }, () => service.users.add(input)],
      [{ url: '/system/user/update', method: 'post', data: input }, () => service.users.update(input)],
      [{ url: '/system/user/user%2F1,user%2F2', method: 'post' }, () => service.users.delete(['user/1', 'user/2'])],
      [
        {
          url: '/system/user/resetPwd',
          method: 'post',
          headers: { repeatSubmit: false },
          data: { userId: 'user/1', password: 'secret' }
        },
        () => service.users.resetPassword('user/1', 'secret')
      ],
      [
        { url: '/system/user/changeStatus', method: 'post', data: { userId: 'user/1', status: '0' } },
        () => service.users.changeStatus('user/1', '0')
      ],
      [{ url: '/system/user/unlock/user%2F1', method: 'post' }, () => service.users.unlock('user/1')],
      [{ url: '/system/user/profile', method: 'get' }, () => service.users.profile()],
      [{ url: '/system/user/profile', method: 'post', data: input }, () => service.users.updateProfile(input)],
      [
        {
          url: '/system/user/profile/updatePwd',
          method: 'post',
          headers: { repeatSubmit: false },
          data: { oldPassword: 'old', newPassword: 'new' }
        },
        () => service.users.updatePassword('old', 'new')
      ],
      [
        { url: '/system/user/authRole/user%2F1', method: 'get', params: { clientId: 'client/1' } },
        () => service.users.authRoles('user/1', 'client/1')
      ],
      [{ url: '/system/user/authRole', method: 'post', params: input }, () => service.users.updateAuthRoles(input)],
      [{ url: '/system/user/list/dept/dept%2F1', method: 'get' }, () => service.users.listByDepartment('dept/1')],
      [{ url: '/system/user/deptTree', method: 'get' }, () => service.users.departmentTree()],

      [{ url: '/system/userType/list', method: 'get', params: query }, () => service.userTypes.list(query)],
      [{ url: '/system/userType/type%2F1', method: 'get' }, () => service.userTypes.get('type/1')],
      [{ url: '/system/userType/options', method: 'get' }, () => service.userTypes.options()],
      [{ url: '/system/userType', method: 'post', data: input }, () => service.userTypes.add(input)],
      [{ url: '/system/userType/update', method: 'post', data: input }, () => service.userTypes.update(input)],
      [
        { url: '/system/userType/type%2F1,type%2F2', method: 'post' },
        () => service.userTypes.delete(['type/1', 'type/2'])
      ],

      [{ url: '/system/role/list', method: 'get', params: query }, () => service.roles.list(query)],
      [
        { url: '/system/role/optionselect?roleIds=role%2F1,role%2F2', method: 'get' },
        () => service.roles.options(['role/1', 'role/2'])
      ],
      [{ url: '/system/role/role%2F1', method: 'get' }, () => service.roles.get('role/1')],
      [{ url: '/system/role', method: 'post', data: input }, () => service.roles.add(input)],
      [{ url: '/system/role/update', method: 'post', data: input }, () => service.roles.update(input)],
      [{ url: '/system/role/permission', method: 'post', data: input }, () => service.roles.updatePermission(input)],
      [
        { url: '/system/role/changeStatus', method: 'post', data: { roleId: 'role/1', status: '0' } },
        () => service.roles.changeStatus('role/1', '0')
      ],
      [{ url: '/system/role/role%2F1,role%2F2', method: 'post' }, () => service.roles.delete(['role/1', 'role/2'])],
      [
        { url: '/system/role/authUser/allocatedList', method: 'get', params: query },
        () => service.roles.allocatedUsers(query)
      ],
      [
        { url: '/system/role/authUser/unallocatedList', method: 'get', params: query },
        () => service.roles.unallocatedUsers(query)
      ],
      [{ url: '/system/role/authUser/cancel', method: 'post', data: input }, () => service.roles.cancelUser(input)],
      [
        { url: '/system/role/authUser/cancelAll', method: 'post', params: input },
        () => service.roles.cancelUsers(input)
      ],
      [
        { url: '/system/role/authUser/selectAll', method: 'post', params: input },
        () => service.roles.selectUsers(input)
      ],
      [{ url: '/system/role/deptTree/role%2F1', method: 'get' }, () => service.roles.departmentTree('role/1')],

      [{ url: '/system/menu/list', method: 'get', params: query }, () => service.menus.list(query)],
      [{ url: '/system/menu/menu%2F1', method: 'get' }, () => service.menus.get('menu/1')],
      [
        { url: '/system/menu/treeselect', method: 'get', params: { clientId: 'client/1' } },
        () => service.menus.tree('client/1')
      ],
      [{ url: '/system/menu/roleMenuTreeselect/role%2F1', method: 'get' }, () => service.menus.roleTree('role/1')],
      [{ url: '/system/menu', method: 'post', data: input }, () => service.menus.add(input)],
      [{ url: '/system/menu/update', method: 'post', data: input }, () => service.menus.update(input)],
      [{ url: '/system/menu/menu%2F1', method: 'post' }, () => service.menus.delete('menu/1')],
      [
        { url: '/system/menu/cascade/menu%2F1,menu%2F2', method: 'post' },
        () => service.menus.cascadeDelete(['menu/1', 'menu/2'])
      ],

      [{ url: '/system/dept/list', method: 'get', params: query }, () => service.departments.list(query)],
      [
        { url: '/system/dept/optionselect?deptIds=dept%2F1,dept%2F2', method: 'get' },
        () => service.departments.options(['dept/1', 'dept/2'])
      ],
      [
        { url: '/system/dept/list/exclude/dept%2F1', method: 'get' },
        () => service.departments.excludeChildren('dept/1')
      ],
      [{ url: '/system/dept/dept%2F1', method: 'get' }, () => service.departments.get('dept/1')],
      [{ url: '/system/dept', method: 'post', data: input }, () => service.departments.add(input)],
      [{ url: '/system/dept/update', method: 'post', data: input }, () => service.departments.update(input)],
      [{ url: '/system/dept/dept%2F1', method: 'post' }, () => service.departments.delete('dept/1')],

      [{ url: '/system/post/list', method: 'get', params: query }, () => service.posts.list(query)],
      [{ url: '/system/post/post%2F1', method: 'get' }, () => service.posts.get('post/1')],
      [
        { url: '/system/post/optionselect', method: 'get', params: { deptId: 'dept/1', postIds: ['post/1'] } },
        () => service.posts.options('dept/1', ['post/1'])
      ],
      [{ url: '/system/post', method: 'post', data: input }, () => service.posts.add(input)],
      [{ url: '/system/post/update', method: 'post', data: input }, () => service.posts.update(input)],
      [{ url: '/system/post/post%2F1,post%2F2', method: 'post' }, () => service.posts.delete(['post/1', 'post/2'])],
      [{ url: '/system/post/deptTree', method: 'get' }, () => service.posts.departmentTree()]
    ];

    for (const [, invoke] of cases) await invoke();
    expect(requests).toEqual(cases.map(([expected]) => expected));
    expect(cases).toHaveLength(71);
  });

  it('keeps table responses at response.data.rows and response.data.total', async () => {
    const page = { rows: [{ id: 'client-a' }], total: 1 };
    const http: HttpClient = {
      request: async <T>(_request: HttpRequest): Promise<T> => ({ code: 200, data: page }) as T
    };
    const service = createSystemService(http);
    await expect(service.clients.list({ pageNum: 1 })).resolves.toMatchObject({ data: page });
    await expect(service.clients.options()).resolves.toEqual(page.rows);
  });

  it('does not rewrite a failed user query with fallback data', async () => {
    const error = new Error('client scoped query failed');
    const service = createSystemService({ request: vi.fn(async () => Promise.reject(error)) });
    await expect(service.users.list({ pageNum: 1, pageSize: 10 })).rejects.toBe(error);
  });

  it('uses no-store POST requests and narrows credential responses', async () => {
    const requests: HttpRequest[] = [];
    const http: HttpClient = {
      request: vi.fn(async request => {
        requests.push(request);
        return {
          code: 200,
          data: request.url.endsWith('/candidate')
            ? { password: 'Candidate9!', ignored: 'transport-only' }
            : { password: 'Temporary9!', expiresInSeconds: 60, ignored: 'transport-only' }
        } as never;
      })
    };
    const users = createSystemService(http).users;

    await expect(users.passwordResetCandidate(42)).resolves.toEqual({
      code: 200,
      data: { password: 'Candidate9!' }
    });
    await expect(users.issueTemporaryPassword(42)).resolves.toEqual({
      code: 200,
      data: { password: 'Temporary9!', expiresInSeconds: 60 }
    });
    expect(requests).toEqual([
      {
        url: '/system/user/resetPwd/candidate',
        method: 'post',
        headers: { 'Cache-Control': 'no-store', repeatSubmit: false },
        data: { userId: 42 }
      },
      {
        url: '/system/user/temporaryPassword',
        method: 'post',
        headers: { 'Cache-Control': 'no-store', repeatSubmit: false },
        data: { userId: 42 }
      }
    ]);
  });

  it('retains concrete DTO and VO contracts for every governance slice', () => {
    const service = createSystemService({ request: vi.fn() });
    expectTypeOf(service.clients.list).toEqualTypeOf<
      (query?: ClientQuery) => Promise<ApiResponse<PageResult<ClientVO>>>
    >();
    expectTypeOf(service.clients.add).toEqualTypeOf<(data: ClientForm) => Promise<ApiResponse>>();
    expectTypeOf(service.users.list).toEqualTypeOf<(query: UserQuery) => Promise<ApiResponse<PageResult<UserVO>>>>();
    expectTypeOf(service.users.passwordResetCandidate).toEqualTypeOf<
      (userId: string | number) => Promise<ApiResponse<ResetPasswordCandidate>>
    >();
    expectTypeOf(service.users.issueTemporaryPassword).toEqualTypeOf<
      (userId: string | number) => Promise<ApiResponse<TemporaryPassword>>
    >();
    expectTypeOf(service.userTypes.list).toEqualTypeOf<
      (query?: UserTypeQuery) => Promise<ApiResponse<PageResult<UserTypeVO>>>
    >();
    expectTypeOf(service.roles.list).toEqualTypeOf<(query: RoleQuery) => Promise<ApiResponse<PageResult<RoleVO>>>>();
    expectTypeOf(service.roles.updatePermission).toEqualTypeOf<(data: RoleForm) => Promise<ApiResponse>>();
    expectTypeOf(service.menus.list).toEqualTypeOf<(query?: MenuQuery) => Promise<ApiResponse<MenuVO[]>>>();
    expectTypeOf(service.departments.list).toEqualTypeOf<(query?: DeptQuery) => Promise<ApiResponse<DeptVO[]>>>();
    expectTypeOf(service.posts.list).toEqualTypeOf<(query: PostQuery) => Promise<ApiResponse<PageResult<PostVO>>>>();
  });

  it('publishes the independent SSO 管理 resource on /system/ssoApp', () => {
    expect(systemSsoAppResource).toEqual({
      controller: 'SysSsoAppController',
      basePath: '/system/ssoApp'
    });
  });

  it('publishes the exact system capability identity', () => {
    expect(systemDomainModule).toEqual({
      id: 'system',
      backendModules: ['wta-system'],
      capabilities: [
        'client',
        'user',
        'user-type',
        'role',
        'menu',
        'department',
        'post',
        'dict',
        'config',
        'oss',
        'oss-config',
        'social',
        'monitor-cache',
        'monitor-login-info',
        'monitor-online',
        'monitor-operlog'
      ]
    });
  });
});
