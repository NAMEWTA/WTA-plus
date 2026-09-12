import { expect, test, type Page, type Route } from '@playwright/test';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4173';
const expectUnsafeConfiguredUrl = process.env.OPERATIONS_EXPECT_UNSAFE_URL === '1';
const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });
const menus = [
  { path: '/monitor-online', name: 'OnlineProof', component: 'monitor/online/index', meta: { title: '在线用户' } },
  { path: '/monitor-admin', name: 'MonitorAdminProof', component: 'monitor/admin/index', meta: { title: '服务监控' } },
  {
    path: '/monitor-snailjob',
    name: 'SnailJobProof',
    component: 'monitor/snailjob/index',
    meta: { title: '任务调度' }
  },
  { path: '/notify-monitor', name: 'NotifyProof', component: 'notify/monitor/index', meta: { title: '通知监控' } }
];

type ApiState = {
  attachmentMode: 'failure' | 'unsafe';
  attachmentRequests: number;
  unknown: string[];
};

async function installApi(page: Page, permissions: string[], state: ApiState) {
  await page.route('**/prod-api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    if (path === '/system/user/getInfo')
      return json(route, {
        code: 200,
        data: {
          user: { userId: 1, userName: 'operator', nickName: 'Operator', avatarUrl: '' },
          roles: ['operator'],
          permissions
        }
      });
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: menus });
    if (path === '/system/dict/data/type/sys_device_type') return json(route, { code: 200, data: [] });
    if (path === '/monitor/online/list' && request.method() === 'GET')
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              tokenId: 'session-1',
              userName: 'operations-user',
              deptName: '运维部',
              ipaddr: '127.0.0.1',
              loginLocation: '本机',
              browser: 'Chromium',
              os: 'Linux',
              loginTime: Date.now()
            }
          ],
          total: 1
        }
      });
    if (path === '/monitor/online/session-1' && request.method() === 'DELETE')
      return json(route, { code: 200, data: null });
    if (path === '/notify/monitor/deliveries' && request.method() === 'GET')
      return json(route, {
        code: 200,
        data: [{ deliveryId: 71, userId: 11, channel: 'MAIL', status: 'DISPATCH_ERROR', providerMessageId: null, errorCode: 'DISPATCH_ERROR' }]
      });
    if (path === '/notify/monitor/snapshot' && request.method() === 'GET')
      return json(route, {
        code: 200,
        data: { notificationId: '71', status: 'FAILED', createdAt: new Date().toISOString(), deliveries: [] }
      });
    if (path === '/notify/inbox')
      return json(route, { code: 200, data: [] });
    if (path === '/resource/message/close') return json(route, { code: 200, data: null });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    state.unknown.push(request.method() + ' ' + path);
    return route.abort('failed');
  });
}

const createState = (): ApiState => ({ attachmentMode: 'unsafe', attachmentRequests: 0, unknown: [] });

test('admin selects operations, queries a real monitor seam and enforces the configured URL boundary', async ({
  page
}) => {
  const state = createState();
  const embeddedRequests: string[] = [];
  page.on('request', request => {
    if (request.resourceType() === 'document' && request.frame() !== page.mainFrame()) {
      embeddedRequests.push(request.url());
    }
  });
  await installApi(page, ['monitor:online:list', 'monitor:online:forceLogout', 'monitor:admin:list'], state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'operations-proof'));
  await page.goto(`${adminUrl}/monitor-online`);
  await expect(page.getByText('operations-user', { exact: true })).toBeVisible();
  await page
    .locator('.monitor-online-page .el-table__body tr')
    .filter({ hasText: 'operations-user' })
    .locator('button')
    .click();
  await page.getByRole('dialog', { name: '系统提示' }).getByRole('button', { name: '确定' }).click();
  await expect(page.getByText('删除成功', { exact: true })).toBeVisible();
  await page.goto(`${adminUrl}/monitor-admin`);
  if (expectUnsafeConfiguredUrl) {
    await expect(page.getByRole('alert')).toContainText('运维入口地址不安全');
    await expect(page.locator('iframe')).toHaveCount(0);
    expect(embeddedRequests).toEqual([]);
  } else {
    await expect(page.locator('iframe')).toHaveAttribute('src', /(?:\/admin\/applications|\/applications)/);
    await expect.poll(() => embeddedRequests.length).toBe(1);
  }
  expect(state.unknown).toEqual([]);
});

test('permission denial renders no external frame', async ({ page }) => {
  const state = createState();
  await installApi(page, [], state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'operations-denied'));
  await page.goto(`${adminUrl}/monitor-snailjob`);
  await expect(page.getByRole('alert')).toContainText('无权访问运维入口');
  await expect(page.locator('iframe')).toHaveCount(0);
  expect(state.unknown).toEqual([]);
});

test('notification monitor uses the current notify contract and renders delivery status', async ({ page }) => {
  const state = createState();
  await installApi(page, ['notify:monitor:list', 'notify:monitor:query'], state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'operations-notify-proof'));
  await page.goto(`${adminUrl}/notify-monitor`);
  await expect(page.getByText('71', { exact: true })).toBeVisible();
  await expect(page.getByText('投递异常', { exact: true })).toBeVisible();
  expect(state.unknown).toEqual([]);
});
