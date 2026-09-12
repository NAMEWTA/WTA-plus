import { expect, test, type Page, type Route } from '@playwright/test';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4173';

const menus = [
  {
    path: '/system',
    name: 'SystemOssConfigRoot',
    component: 'Layout',
    meta: { title: '系统管理' },
    children: [
      {
        path: 'oss-config/index',
        name: 'SystemOssConfigAccessPolicy',
        component: 'system/oss/config',
        meta: { title: 'OSS配置' }
      }
    ]
  }
];

type State = {
  requests: Array<{ method: string; path: string }>;
  unknown: string[];
  privateStatus: 'Y' | 'N';
};

const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });

const config = (accessPolicy: '0' | '2', status: 'Y' | 'N') => ({
  ossConfigId: accessPolicy === '0' ? 1 : 2,
  configKey: accessPolicy === '0' ? 'private-main' : 'public-portal',
  accessKey: 'access-key',
  secretKey: 'secret-key',
  bucketName: accessPolicy === '0' ? 'private-bucket' : 'public-bucket',
  prefix: '',
  endpoint: '127.0.0.1:39000',
  domainUrl: accessPolicy === '0' ? '' : 'cdn.example.test',
  isHttps: 'Y',
  region: 'us-east-1',
  status,
  ext1: '',
  remark: '',
  accessPolicy
});

async function installApi(page: Page, state: State) {
  await page.route('**/prod-api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    const method = request.method();
    if (path === '/auth/client/context') {
      return json(route, {
        code: 200,
        data: {
          clientEnabled: true,
          registerEnabled: false,
          passwordPolicy: {
            minimumLength: 8,
            maximumLength: 20,
            requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT'],
            allowedSpecialCharacters: '!@#'
          }
        }
      });
    }
    if (path === '/system/user/getInfo') {
      return json(route, {
        code: 200,
        data: {
          user: { userId: 11, userName: 'oss-admin', nickName: 'OSS Admin', avatarUrl: '' },
          roles: ['operator'],
          permissions: [
            'system:ossConfig:list',
            'system:ossConfig:add',
            'system:ossConfig:edit',
            'system:ossConfig:remove'
          ]
        }
      });
    }
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: menus });
    if (path === '/notify/inbox') {
      return json(route, { code: 200, data: [] });
    }
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    if (path === '/system/dict/data/type/sys_yes_no') {
      return json(route, {
        code: 200,
        data: [
          { dictLabel: '是', dictValue: 'Y', listClass: 'success' },
          { dictLabel: '否', dictValue: 'N', listClass: 'info' }
        ]
      });
    }
    if (path === '/resource/oss/config/list' && method === 'GET') {
      state.requests.push({ method, path });
      return json(route, {
        code: 200,
        data: { rows: [config('0', state.privateStatus), config('2', 'N')], total: 2 }
      });
    }
    if (path === '/resource/oss/config/1' && method === 'GET') {
      state.requests.push({ method, path });
      return json(route, { code: 200, data: config('0', state.privateStatus) });
    }
    if (path === '/resource/oss/config/changeStatus' && method === 'POST') {
      state.requests.push({ method, path });
      state.privateStatus = 'N';
      return json(route, { code: 200, data: null });
    }
    if (path === '/resource/oss/config/edit' && method === 'POST') {
      state.requests.push({ method, path });
      return json(route, { code: 200, data: null });
    }
    if (path === '/resource/oss/config/remove/2' && method === 'POST') {
      state.requests.push({ method, path });
      return json(route, { code: 200, data: null });
    }

    state.unknown.push(`${method} ${path}`);
    return json(route, { code: 200, data: null });
  });
}

test('OSS config exposes two policies, enforces public constraints and uses POST commands', async ({ page }) => {
  const state: State = { requests: [], unknown: [], privateStatus: 'Y' };
  await installApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'oss-config-access-policy'));

  await page.goto(`${adminUrl}/system/oss-config/index`);
  await expect(page.getByRole('heading', { name: 'OSS 配置' })).toBeVisible();
  await expect(page.getByText('PRIVATE', { exact: true })).toBeVisible();
  await expect(page.getByText('PUBLIC_READ', { exact: true })).toBeVisible();
  await expect(page.getByText('custom', { exact: true })).toHaveCount(0);

  const publicRow = page.getByRole('row').filter({ hasText: 'public-portal' });
  await expect(publicRow.getByRole('switch')).toBeDisabled();

  await page.getByRole('button', { name: '新增' }).click();
  const addDialog = page.getByRole('dialog', { name: '添加对象存储配置' });
  await addDialog.getByText('PUBLIC_READ', { exact: true }).click();
  await expect(addDialog.getByText('PUBLIC_READ 在生产环境必须配置可公开访问的 domainUrl')).toBeVisible();
  await expect(addDialog.getByRole('switch', { name: '是否默认' })).toBeDisabled();
  await addDialog.getByRole('button', { name: '确 定' }).click();
  await expect(addDialog.getByText('PUBLIC_READ 在生产环境必须配置 domainUrl')).toBeVisible();
  await addDialog.getByRole('button', { name: '取 消' }).click();

  const privateRow = page.getByRole('row').filter({ hasText: 'private-main' });
  await privateRow.locator('.el-switch').click();
  await page.getByRole('dialog').getByRole('button', { name: '确定' }).click();
  await expect.poll(() => state.requests.filter(item => item.path.endsWith('/changeStatus')).length).toBe(1);

  await privateRow.getByRole('button', { name: '修改' }).click();
  const editDialog = page.getByRole('dialog', { name: '修改对象存储配置' });
  await editDialog.getByRole('button', { name: '确 定' }).click();
  await expect.poll(() => state.requests.filter(item => item.path.endsWith('/edit')).length).toBe(1);

  await publicRow.getByRole('button', { name: '删除' }).click();
  await page.getByRole('dialog').getByRole('button', { name: '确定' }).click();
  await expect.poll(() => state.requests.filter(item => item.path.endsWith('/remove/2')).length).toBe(1);

  expect(state.requests).toEqual(
    expect.arrayContaining([
      { method: 'POST', path: '/resource/oss/config/changeStatus' },
      { method: 'POST', path: '/resource/oss/config/edit' },
      { method: 'POST', path: '/resource/oss/config/remove/2' }
    ])
  );
  expect(state.unknown).toEqual([]);
});
