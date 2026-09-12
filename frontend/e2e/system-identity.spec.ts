import { expect, test, type Page, type Route } from '@playwright/test';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4173';
const adminClientId = 'e5cd7e4891bf95d1d19206ce24a7b32e';

const routes = [
  { path: '/system-user', name: 'SystemUserProof', component: 'system/user/index', meta: { title: '用户管理' } },
  { path: '/system-role', name: 'SystemRoleProof', component: 'system/role/index', meta: { title: '角色管理' } },
  { path: '/system-menu', name: 'SystemMenuProof', component: 'system/menu/index', meta: { title: '菜单管理' } }
];

type AdminState = {
  credentialRequests?: Array<{
    body?: Record<string, unknown>;
    method: string;
    path: string;
  }>;
  failUserList: boolean;
  failCredentialPath?: string;
  governanceRequests: Array<{
    body?: Record<string, unknown>;
    clientId: string;
    method: string;
    path: string;
    query: Record<string, string>;
  }>;
  unknownRequests: string[];
  permissions?: string[];
  temporaryIssueCount?: number;
};

const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });

async function installAdminApi(page: Page, state: AdminState) {
  await page.route('**/prod-api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    const method = request.method();
    if (path === '/system/user/getInfo') {
      return json(route, {
        code: 200,
        data: {
          user: { userId: 7, userName: 'governance-reader', nickName: 'Governance Reader', avatarUrl: '' },
          roles: ['operator'],
          permissions: state.permissions ?? [
            'system:user:list',
            'system:role:list',
            'system:role:edit',
            'system:menu:list'
          ]
        }
      });
    }
    if (path === '/auth/client/context') {
      return json(route, {
        code: 200,
        data: {
          clientEnabled: true,
          registerEnabled: true,
          passwordPolicy: {
            minimumLength: 8,
            maximumLength: 20,
            requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'],
            allowedSpecialCharacters: '!@#'
          }
        }
      });
    }
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: routes });
    if (path === '/notify/inbox') {
      return json(route, { code: 200, data: [] });
    }
    if (path === '/resource/message/close') return json(route, { code: 200, data: null });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    if (path.startsWith('/system/dict/data/type/')) return json(route, { code: 200, data: [] });

    if (path === '/system/user/' && method === 'GET') {
      state.credentialRequests ??= [];
      state.credentialRequests.push({ method, path });
      return json(route, {
        code: 200,
        data: { password: 'NewUserCandidate9!', postIds: [], posts: [], roleIds: [], roles: [] }
      });
    }

    if (
      path === '/system/user/resetPwd/candidate' ||
      path === '/system/user/resetPwd' ||
      path === '/system/user/temporaryPassword'
    ) {
      state.credentialRequests ??= [];
      state.credentialRequests.push({
        body: request.postData() ? (request.postDataJSON() as Record<string, unknown>) : undefined,
        method,
        path
      });
      if (state.failCredentialPath === path) {
        return json(route, { code: 500, msg: '凭据请求失败' });
      }
      if (path === '/system/user/resetPwd/candidate') {
        return json(route, { code: 200, data: { password: 'Candidate9!' } });
      }
      if (path === '/system/user/temporaryPassword') {
        state.temporaryIssueCount = (state.temporaryIssueCount ?? 0) + 1;
        return json(route, {
          code: 200,
          data: { password: `Temporary${state.temporaryIssueCount}!`, expiresInSeconds: 60 }
        });
      }
      return json(route, { code: 200, data: null });
    }

    const governancePaths = new Set([
      '/system/user/deptTree',
      '/system/user/list',
      '/system/userType/options',
      '/system/client/list',
      '/system/config/configKey/sys.user.initPassword',
      '/system/role/list',
      '/system/role/changeStatus',
      '/system/menu/list'
    ]);
    if (governancePaths.has(path)) {
      const url = new URL(request.url());
      state.governanceRequests.push({
        body: request.postData() ? (request.postDataJSON() as Record<string, unknown>) : undefined,
        clientId: request.headers()['clientid'] ?? '',
        method,
        path,
        query: Object.fromEntries(url.searchParams)
      });
      if (path === '/system/user/list') {
        if (state.failUserList) return json(route, { code: 500, msg: '跨 Client 用户查询被拒绝' });
        return json(route, {
          code: 200,
          data: {
            rows: [{ userId: 42, userName: 'scoped-user', nickName: 'Scoped User', status: '0' }],
            total: 1
          }
        });
      }
      if (path === '/system/client/list') {
        return json(route, {
          code: 200,
          data: { rows: [{ id: 1, clientId: 'proof-client', clientKey: 'proof-client', status: '0' }], total: 1 }
        });
      }
      if (path === '/system/role/list') {
        return json(route, {
          code: 200,
          data: {
            rows: [
              {
                roleId: 77,
                roleName: 'Client Scoped Operator',
                roleKey: 'scoped_operator',
                roleSort: 1,
                status: '0'
              }
            ],
            total: 1
          }
        });
      }
      if (path === '/system/role/changeStatus') return json(route, { code: 200, data: null });
      if (path === '/system/menu/list') {
        return json(route, {
          code: 200,
          data: [
            {
              menuId: 88,
              parentId: 0,
              menuName: 'Client Scoped Menu',
              menuType: 'C',
              orderNum: 1,
              status: '0',
              children: []
            }
          ]
        });
      }
      if (path === '/system/config/configKey/sys.user.initPassword') return json(route, { code: 200, data: '123456' });
      return json(route, { code: 200, data: [] });
    }

    state.unknownRequests.push(`${method} ${path}`);
    return json(route, { code: 200, data: null });
  });
}

test('admin runs Client-scoped governance queries and an authorized role mutation while unrelated controls fail closed', async ({
  page
}) => {
  const state: AdminState = { failUserList: false, governanceRequests: [], unknownRequests: [] };
  await installAdminApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'system-governance-proof'));

  await page.goto(`${adminUrl}/system-user`);
  await expect(page.getByRole('heading', { name: '用户列表' })).toBeVisible();
  await expect(page.getByText('scoped-user', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '新增' })).toHaveCount(0);

  await page.goto(`${adminUrl}/system-role`);
  await expect(page.getByRole('heading', { name: '角色列表' })).toBeVisible();
  await page.locator('.system-role-page .client-filter').click();
  await page.getByRole('option', { name: 'proof-client（proof-client）' }).click();
  await expect(page.getByText('Client Scoped Operator', { exact: true })).toBeVisible();
  await page.locator('.system-role-page .el-switch').click();
  await page.getByRole('dialog', { name: '系统提示' }).getByRole('button', { name: '确定' }).click();
  await expect
    .poll(() => state.governanceRequests.filter(item => item.path === '/system/role/changeStatus').length)
    .toBe(1);

  await page.goto(`${adminUrl}/system-menu`);
  await expect(page.getByRole('heading', { name: '菜单列表' })).toBeVisible();
  await page.locator('.system-menu-page .client-filter').click();
  await page.getByRole('option', { name: 'proof-client（proof-client）' }).click();
  await expect(page.getByText('Client Scoped Menu', { exact: true })).toBeVisible();

  expect(state.governanceRequests.every(item => item.clientId === adminClientId)).toBe(true);
  expect(state.governanceRequests).toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        method: 'GET',
        path: '/system/role/list',
        query: expect.objectContaining({ clientId: '1' })
      }),
      expect.objectContaining({
        method: 'GET',
        path: '/system/menu/list',
        query: expect.objectContaining({ clientId: '1' })
      }),
      expect.objectContaining({
        body: { roleId: 77, status: '1' },
        method: 'PUT',
        path: '/system/role/changeStatus'
      })
    ])
  );
  expect(state.unknownRequests).toEqual([]);
});

test('a rejected Client-scoped user query stays visible and does not invent fallback rows', async ({ page }) => {
  const state: AdminState = { failUserList: true, governanceRequests: [], unknownRequests: [] };
  await installAdminApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'system-governance-failure'));

  await page.goto(`${adminUrl}/system-user`);

  await expect(page.getByText('跨 Client 用户查询被拒绝', { exact: true })).toBeVisible();
  await expect(page.getByText('scoped-user', { exact: true })).toHaveCount(0);
  expect(state.governanceRequests.filter(item => item.path === '/system/user/list')).toHaveLength(1);
  expect(state.unknownRequests).toEqual([]);
});

test('credential permissions remain independent and fail closed in the user row', async ({ page }) => {
  const state: AdminState = {
    failUserList: false,
    governanceRequests: [],
    permissions: ['system:user:list', 'system:user:resetPwd'],
    unknownRequests: []
  };
  await installAdminApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'credential-permission-proof'));

  await page.goto(`${adminUrl}/system-user`);
  await expect(page.getByRole('button', { name: '重置密码' })).toHaveCount(1);
  await expect(page.getByRole('button', { name: '签发临时密码' })).toHaveCount(0);

  state.permissions = ['system:user:list', 'system:user:temporaryPassword'];
  await page.reload();
  await expect(page.getByRole('button', { name: '重置密码' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '签发临时密码' })).toHaveCount(1);

  state.permissions = ['system:user:list'];
  await page.reload();
  await expect(page.getByRole('button', { name: '重置密码' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '签发临时密码' })).toHaveCount(0);
});

test('new user form adopts the server candidate and rejects a weak edit without legacy config', async ({ page }) => {
  const state: AdminState = {
    credentialRequests: [],
    failUserList: false,
    governanceRequests: [],
    permissions: ['system:user:list', 'system:user:add'],
    unknownRequests: []
  };
  await installAdminApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'credential-add-proof'));

  await page.goto(`${adminUrl}/system-user`);
  await page.getByRole('button', { name: '新增' }).click();
  const dialog = page.getByRole('dialog', { name: '新增用户' });
  await expect(dialog).toBeVisible();
  const password = dialog.getByPlaceholder('请输入用户密码');
  await expect(password).toHaveValue('NewUserCandidate9!');
  await password.fill('weak');
  await password.blur();
  await expect(dialog.getByText(/密码长度不能少于 8 位/)).toBeVisible();
  expect(state.credentialRequests).toEqual([{ method: 'GET', path: '/system/user/' }]);
  expect(state.governanceRequests.some(item => item.path.includes('sys.user.initPassword'))).toBe(false);
  expect(state.unknownRequests).toEqual([]);
});

test('admin edits the server reset candidate while weak input sends no password write', async ({ page }, testInfo) => {
  const state: AdminState = {
    credentialRequests: [],
    failUserList: false,
    governanceRequests: [],
    permissions: ['system:user:list', 'system:user:resetPwd'],
    unknownRequests: []
  };
  await installAdminApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'credential-reset-proof'));

  await page.goto(`${adminUrl}/system-user`);
  await page.getByRole('button', { name: '重置密码' }).evaluate(button => {
    button.click();
    button.click();
  });
  const dialog = page.getByRole('dialog', { name: '重置永久密码' });
  await expect(dialog).toBeVisible();
  const passwordInputs = dialog.locator('input[type="password"]');
  await expect(passwordInputs).toHaveCount(2);
  await expect(passwordInputs.first()).toHaveValue('Candidate9!');
  expect(state.credentialRequests?.filter(item => item.path === '/system/user/resetPwd/candidate')).toHaveLength(1);

  await passwordInputs.first().fill('weak');
  await passwordInputs.nth(1).fill('weak');
  await dialog.getByRole('button', { name: '确定重置' }).click();
  await expect(dialog.getByText(/密码长度不能少于 8 位/)).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('password-reset-policy-error.png'), fullPage: true });
  expect(state.credentialRequests?.filter(item => item.path === '/system/user/resetPwd')).toHaveLength(0);

  await passwordInputs.first().fill('EditedCandidate9!');
  await passwordInputs.nth(1).fill('EditedCandidate9!');
  await expect(dialog.getByText(/密码长度不能少于 8 位/)).toBeHidden();
  await page.screenshot({ path: testInfo.outputPath('password-reset-candidate.png'), fullPage: true });
  await dialog.getByRole('button', { name: '确定重置' }).click();
  await expect(dialog).toBeHidden();
  await expect
    .poll(() => state.credentialRequests?.filter(item => item.path === '/system/user/resetPwd').length)
    .toBe(1);
  expect(state.credentialRequests).toEqual(
    expect.arrayContaining([
      expect.objectContaining({ method: 'POST', path: '/system/user/resetPwd/candidate' }),
      expect.objectContaining({ method: 'PUT', path: '/system/user/resetPwd' })
    ])
  );
  expect(state.unknownRequests).toEqual([]);
});

test('temporary password is copied once, removed on close and reissued as a new value', async ({ page }, testInfo) => {
  const state: AdminState = {
    credentialRequests: [],
    failUserList: false,
    governanceRequests: [],
    permissions: ['system:user:list', 'system:user:temporaryPassword'],
    unknownRequests: []
  };
  await installAdminApi(page, state);
  await page.addInitScript(() => {
    localStorage.setItem('Admin-Token', 'credential-temporary-proof');
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: async (value: string) => ((window as any).credentialCopyProof = value) }
    });
  });

  await page.goto(`${adminUrl}/system-user`);
  await page.getByRole('button', { name: '签发临时密码' }).click();
  const dialog = page.getByRole('dialog', { name: '一次性临时密码' });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByText(/60 秒后失效/)).toBeVisible();
  await expect(dialog.locator('.el-loading-mask')).toBeHidden();
  await dialog.evaluate(async element => {
    await Promise.all(
      element.getAnimations({ subtree: true }).map(animation => animation.finished.catch(() => undefined))
    );
  });
  const temporary = dialog.getByRole('textbox', { name: '一次性临时密码' });
  const firstValue = await temporary.inputValue();
  await page.screenshot({ path: testInfo.outputPath('temporary-password-once.png'), fullPage: true });
  await dialog.getByRole('button', { name: '复制临时密码' }).click();
  await expect(page.getByText('临时密码已复制', { exact: true })).toBeVisible();
  expect(await page.evaluate(() => (window as any).credentialCopyProof)).toBe(firstValue);

  await dialog.getByRole('button', { name: '关闭', exact: true }).click();
  await expect(dialog).toBeHidden();
  await expect(page.getByRole('textbox', { name: '一次性临时密码' })).toHaveCount(0);
  await expect(page.locator('body')).not.toContainText(firstValue);

  await page.getByRole('button', { name: '签发临时密码' }).click();
  await expect(dialog).toBeVisible();
  await expect(temporary).not.toHaveValue(firstValue);
  await dialog.getByRole('button', { name: '关闭', exact: true }).click();
  expect(state.credentialRequests?.filter(item => item.path === '/system/user/temporaryPassword')).toHaveLength(2);
  expect(state.unknownRequests).toEqual([]);
});

test('clipboard failure is visible without dismissing the one-time value', async ({ page }) => {
  const state: AdminState = {
    failUserList: false,
    governanceRequests: [],
    permissions: ['system:user:list', 'system:user:temporaryPassword'],
    unknownRequests: []
  };
  await installAdminApi(page, state);
  await page.addInitScript(() => {
    localStorage.setItem('Admin-Token', 'credential-copy-failure');
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: async () => Promise.reject(new Error('denied')) }
    });
  });

  await page.goto(`${adminUrl}/system-user`);
  await page.getByRole('button', { name: '签发临时密码' }).click();
  const dialog = page.getByRole('dialog', { name: '一次性临时密码' });
  await dialog.getByRole('button', { name: '复制临时密码' }).click();
  await expect(page.getByText('复制失败，请手动复制', { exact: true })).toBeVisible();
  await expect(dialog.getByRole('textbox', { name: '一次性临时密码' })).toBeVisible();
});

test('credential request failures close loading dialogs without exposing empty values', async ({ page }) => {
  const state: AdminState = {
    failCredentialPath: '/system/user/resetPwd/candidate',
    failUserList: false,
    governanceRequests: [],
    permissions: ['system:user:list', 'system:user:resetPwd', 'system:user:temporaryPassword'],
    unknownRequests: []
  };
  await installAdminApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'credential-network-failure'));

  await page.goto(`${adminUrl}/system-user`);
  await page.getByRole('button', { name: '重置密码' }).click();
  await expect(page.getByText('凭据请求失败', { exact: true })).toBeVisible();
  await expect(page.getByRole('dialog', { name: '重置永久密码' })).toBeHidden();

  state.failCredentialPath = '/system/user/temporaryPassword';
  await page.getByRole('button', { name: '签发临时密码' }).click();
  await expect(page.getByRole('dialog', { name: '一次性临时密码' })).toBeHidden();
  await expect(page.getByRole('textbox', { name: '一次性临时密码' })).toHaveCount(0);
});
