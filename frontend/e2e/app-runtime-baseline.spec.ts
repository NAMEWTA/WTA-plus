import { expect, test, type Page, type Route } from '@playwright/test';

const productionClientId = 'e5cd7e4891bf95d1d19206ce24a7b32e';

type ClientContextMode = 'valid' | 'request-failure' | 'missing-fields';
type AuthCodeMode = 'disabled' | 'fail-after-first';

type BaselineApiState = {
  authCodeMode: AuthCodeMode;
  authCodeRequests: number;
  clientContextMode: ClientContextMode;
  clientContextRequests: number;
  getInfoRequests: number;
  getRoutersRequests: number;
  loginClientHeader: string;
  loginEncryptKey: string;
  loginPostData: string;
  loginRequests: number;
  logoutRequests: number;
  messageBoxCode?: number;
  inboxMessages?: Record<string, unknown>[];
  inboxRequests: number;
  pushTicketRequests: number;
  menuComponent: string;
  networkOrder: string[];
  permissions: string[];
  registerRequests: number;
  unknownRequests: string[];
};

const createApiState = (overrides: Partial<BaselineApiState> = {}): BaselineApiState => ({
  authCodeMode: 'disabled',
  authCodeRequests: 0,
  clientContextMode: 'valid',
  clientContextRequests: 0,
  getInfoRequests: 0,
  getRoutersRequests: 0,
  loginClientHeader: '',
  loginEncryptKey: '',
  loginPostData: '',
  loginRequests: 0,
  logoutRequests: 0,
  menuComponent: 'demo/demo/index',
  inboxRequests: 0,
  pushTicketRequests: 0,
  networkOrder: [],
  permissions: ['demo:demo:list'],
  registerRequests: 0,
  unknownRequests: [],
  ...overrides
});

const fulfillJson = (route: Route, body: unknown) =>
  route.fulfill({
    contentType: 'application/json',
    body: JSON.stringify(body)
  });

const installBaselineApi = async (page: Page, state: BaselineApiState) => {
  await page.route('**/prod-api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    if (path === '/auth/client/context') {
      state.clientContextRequests += 1;
      if (state.clientContextMode === 'request-failure') {
        return route.abort('failed');
      }
      const data =
        state.clientContextMode === 'missing-fields'
          ? { clientEnabled: true }
          : { clientEnabled: true, registerEnabled: true };
      return fulfillJson(route, { code: 200, data });
    }
    if (path === '/auth/code') {
      state.authCodeRequests += 1;
      if (state.authCodeMode === 'fail-after-first' && state.authCodeRequests > 1) {
        return route.abort('failed');
      }
      if (state.authCodeMode === 'fail-after-first') {
        return fulfillJson(route, {
          code: 200,
          data: {
            captchaEnabled: true,
            uuid: 'captcha-proof',
            img: 'R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=='
          }
        });
      }
      return fulfillJson(route, { code: 200, data: { captchaEnabled: false } });
    }
    if (path === '/auth/login') {
      state.loginRequests += 1;
      state.networkOrder.push('login');
      state.loginClientHeader = request.headers()['clientid'] ?? '';
      state.loginEncryptKey = request.headers()['encrypt-key'] ?? '';
      state.loginPostData = request.postData() ?? '';
      return fulfillJson(route, { code: 200, data: { access_token: 'baseline-token' } });
    }
    if (path === '/auth/register') {
      state.registerRequests += 1;
      return fulfillJson(route, { code: 200, data: null });
    }
    if (path === '/auth/logout') {
      state.logoutRequests += 1;
      return fulfillJson(route, { code: 200, data: null });
    }
    if (path === '/system/user/getInfo') {
      state.getInfoRequests += 1;
      state.networkOrder.push('getInfo');
      return fulfillJson(route, {
        code: 200,
        data: {
          user: { userId: 1, userName: 'baseline-user', nickName: 'Baseline User', avatarUrl: '' },
          roles: ['baseline-role'],
          permissions: state.permissions
        }
      });
    }
    if (path === '/system/menu/getRouters') {
      state.getRoutersRequests += 1;
      state.networkOrder.push('getRouters');
      return fulfillJson(route, {
        code: 200,
        data: [
          {
            path: '/baseline',
            name: 'BaselineMenu',
            component: 'Layout',
            redirect: 'noRedirect',
            alwaysShow: true,
            meta: { title: '迁移基线', icon: 'dashboard', noCache: false },
            children: [
              {
                path: 'route',
                name: 'BaselineRoute',
                component: state.menuComponent,
                meta: {
                  activeMenu: null,
                  link: null,
                  title: '迁移基线路由',
                  icon: 'dashboard',
                  noCache: false
                }
              }
            ]
          }
        ]
      });
    }
    if (path === '/resource/message/close') {
      return fulfillJson(route, { code: 200, data: null });
    }
    if (path === '/resource/message/ticket') {
      state.pushTicketRequests += 1;
      return fulfillJson(route, { code: 200, data: 'test-push-ticket' });
    }
    if (path === '/resource/message') {
      return route.fulfill({ contentType: 'text/event-stream', body: '' });
    }
    if (path === '/notify/inbox') {
      state.inboxRequests += 1;
      return fulfillJson(route, {
        code: state.messageBoxCode ?? 200,
        data: state.inboxMessages ?? []
      });
    }
    if (path === '/demo/demo/list') {
      return fulfillJson(route, { code: 200, rows: [], total: 0 });
    }
    state.unknownRequests.push(`${request.method()} ${path}`);
    return fulfillJson(route, { code: 200, data: null, rows: [], total: 0 });
  });
};

const expectAuthenticationRequestsBlocked = async (page: Page, state: BaselineApiState) => {
  await expect(page.getByText('客户端认证配置不可用，无法登录', { exact: true })).toBeVisible();
  await expect(page.locator('.submit-button')).toBeDisabled();
  expect(state.clientContextRequests).toBe(1);
  expect(state.authCodeRequests).toBe(0);
  expect(state.loginRequests).toBe(0);
  expect(state.registerRequests).toBe(0);
  expect(state.unknownRequests).toEqual([]);
};

test('client context request failure keeps authentication fail-closed', async ({ page }) => {
  const state = createApiState({ clientContextMode: 'request-failure' });
  await installBaselineApi(page, state);

  await page.goto('/login');

  await expectAuthenticationRequestsBlocked(page, state);
});

test('client context missing a required field keeps authentication fail-closed', async ({ page }) => {
  const state = createApiState({ clientContextMode: 'missing-fields' });
  await installBaselineApi(page, state);

  await page.goto('/login');

  await expectAuthenticationRequestsBlocked(page, state);
});

test('login restores the selected manifest route without re-filtering the server menu', async ({ page }) => {
  const state = createApiState();
  await installBaselineApi(page, state);

  await page.goto('/login?redirect=%2Fbaseline%2Froute');
  await expect(page.locator('.submit-button')).toBeEnabled();
  await page.locator('.submit-button').click();

  await expect(page).toHaveURL(/\/baseline\/route$/);
  await expect(page.getByRole('heading', { name: '测试单列表' })).toBeVisible();
  await expect(page.getByLabel('面包屑').getByText('迁移基线路由', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '新增' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '修改' })).toHaveCount(0);

  expect(state.networkOrder).toEqual(['login', 'getInfo', 'getRouters']);
  expect(state.loginRequests).toBe(1);
  expect(state.getInfoRequests).toBe(1);
  expect(state.getRoutersRequests).toBe(1);
  expect(state.logoutRequests).toBe(0);
  expect(state.loginClientHeader).toBe(productionClientId);
  expect(state.loginEncryptKey).toBe('');
  expect(state.loginPostData).not.toBe('');
  expect(JSON.parse(state.loginPostData)).toMatchObject({ clientId: productionClientId, grantType: 'password' });
  expect(state.unknownRequests).toEqual([]);
});

test('captcha refresh failure is handled and disables authentication', async ({ page }) => {
  const state = createApiState({ authCodeMode: 'fail-after-first' });
  const pageErrors: Error[] = [];
  page.on('pageerror', error => pageErrors.push(error));
  await installBaselineApi(page, state);

  await page.goto('/login');
  await expect(page.locator('.submit-button')).toBeEnabled();
  await page.locator('.login-code-img').click();

  await expect(page.getByText('客户端认证配置不可用，无法登录', { exact: true })).toBeVisible();
  await expect(page.locator('.submit-button')).toBeDisabled();
  expect(state.authCodeRequests).toBe(2);
  expect(pageErrors).toEqual([]);
});

test('unknown server component fails closed with stable manifest diagnostics', async ({ page }) => {
  const state = createApiState({ menuComponent: 'unselected/report/index' });
  await installBaselineApi(page, state);

  await page.goto('/login?redirect=%2Fbaseline%2Froute');
  await expect(page.locator('.submit-button')).toBeEnabled();
  await page.locator('.submit-button').click();

  await expect(page).toHaveURL(/\/baseline\/route$/);
  await expect(page.getByTestId('manifest-route-diagnostic')).toContainText(
    '页面组件不可用 [missing-component-key] app=admin-web domain=unselected key=unselected/report/index'
  );
  await expect(page.getByRole('heading', { name: '测试单列表' })).toHaveCount(0);
  expect(state.networkOrder).toEqual(['login', 'getInfo', 'getRouters']);
  expect(state.getInfoRequests).toBe(1);
  expect(state.getRoutersRequests).toBe(1);
  expect(state.unknownRequests).toEqual([]);
});

test('an authenticated 401 logs out and preserves the protected route as redirect', async ({ page }) => {
  const state = createApiState({ messageBoxCode: 401 });
  await installBaselineApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'baseline-token'));

  await page.goto('/baseline/route');
  await expect(page.getByRole('dialog', { name: '系统提示' })).toBeVisible();
  await page.getByRole('button', { name: '重新登录' }).click();
  await expect(page).toHaveURL(/\/login\?redirect=/);

  const redirect = new URL(page.url()).searchParams.get('redirect');
  expect(redirect).toBe(encodeURIComponent('/baseline/route'));
  expect(state.logoutRequests).toBe(1);
  expect(state.unknownRequests).toEqual([]);
});


test('persisted notice is visible in the message box with production realtime disabled', async ({ page }) => {
  const state = createApiState({ inboxMessages: [{
    messageId: '9000000000000000101', category: 'notice', title: '持久公告可阅读',
    message: '当前用户的持久站内消息', createTime: '2026-09-22 10:00:00', readTime: null
  }] });
  await installBaselineApi(page, state);
  await page.goto('/login?redirect=%2Fbaseline%2Froute');
  await expect(page.locator('.submit-button')).toBeEnabled();
  await page.locator('.submit-button').click();
  await expect(page).toHaveURL(/\/baseline\/route$/);
  await expect.poll(() => state.inboxRequests).toBeGreaterThan(0);
  await page.locator('.message-trigger').click();
  await page.getByRole('tab', { name: '通知 1', exact: true }).click();
  await expect(page.getByText('持久公告可阅读', { exact: true })).toBeVisible();
  expect(state.pushTicketRequests).toBe(0);
  const requestsBeforeReload = state.inboxRequests;
  await page.reload();
  await expect.poll(() => state.inboxRequests).toBeGreaterThan(requestsBeforeReload);
  await page.locator('.message-trigger').click();
  await page.getByRole('tab', { name: '通知 1', exact: true }).click();
  await expect(page.getByText('持久公告可阅读', { exact: true })).toBeVisible();
  expect(state.pushTicketRequests).toBe(0);
});
