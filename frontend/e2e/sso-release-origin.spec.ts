import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

interface AppFixture { kind: 'admin' | 'home'; clientId: string; env: string; tokenKey: string; target: string }

const apps: AppFixture[] = [
  { kind: 'admin', clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e', env: 'SSO_TEST_ADMIN_ORIGIN', tokenKey: 'Admin-Token', target: '/index?source=release#restored' },
  { kind: 'home', clientId: '428a8310cd442757ae699df5d894f051', env: 'SSO_TEST_HOME_ORIGIN', tokenKey: 'Home-Token', target: '/profile?source=release#restored' }
];

function ownedUrl(name: string): string {
  const value = process.env[name];
  if (!value || !['localhost', '127.0.0.1'].includes(new URL(value).hostname)) throw new Error(`Owned fixture required: ${name}`);
  return value;
}

async function systemFixtures(page: Page, app: AppFixture) {
  // System身份/菜单显式模拟；Client公开配置与SSO请求经过真实Nginx和Controller。
  await page.route('**/prod-api/**', async route => {
    const path = new URL(route.request().url()).pathname.split('/prod-api')[1];
    if (path.startsWith('/sso/') || path === '/auth/client/context') return route.continue();
    let data: unknown = [];
    if (path === '/auth/code') data = { captchaEnabled: false };
    if (path === '/system/user/getInfo') data = { user: { userId: '7', userName: 'sso-test-user', nickName: 'Owned SSO user', avatarUrl: '' }, roles: ['owned-role'], permissions: [] };
    if (path === '/system/menu/getRouters') data = app.kind === 'home'
      ? [{ path: '/profile', name: 'ProfileCenter', component: 'profile/center/index', meta: { title: '档案中心' } }]
      : [];
    await route.fulfill({ json: { code: 200, data } });
  });
}

async function login(page: Page, app: AppFixture) {
  const response = await page.request.get(ownedUrl(app.env) + '/prod-api/auth/client/context', {
    headers: { clientid: app.clientId }, timeout: 5000
  });
  expect(response.status()).toBe(200);
  expect(await response.json()).toMatchObject({ code: 200, data: {
    clientEnabled: true, ssoEnabled: true, ssoAuthorizeUrl: ownedUrl('SSO_TEST_AUTHORIZE_URL'),
    passwordPolicy: { minimumLength: 8, maximumLength: 30 }
  } });
  await page.goto(`${ownedUrl(app.env)}/login?${new URLSearchParams({ redirect: app.target })}`);
  await page.getByTestId('sso-first-provider').click();
  const authorize = new URL(ownedUrl('SSO_TEST_AUTHORIZE_URL'));
  await expect.poll(() => {
    const current = new URL(page.url());
    return current.origin === authorize.origin && current.pathname === authorize.pathname;
  }).toBe(true);
  await page.reload();
  await page.getByLabel('用户名').fill('sso-test-user');
  await page.getByLabel('密码').fill('owned-sso-test-only');
  await page.getByRole('button', { name: '登录并继续' }).click();
  await success(page, app);
}

async function success(page: Page, app: AppFixture) {
  // 不在失败报告打印过渡URL或凭据。
  await expect.poll(() => page.url() === ownedUrl(app.env) + app.target).toBe(true);
  expect(await page.evaluate(key => Boolean(localStorage.getItem(key)), app.tokenKey)).toBe(true);
  expect(await page.evaluate(key => sessionStorage.getItem(key), `namewta-${app.kind}-sso-pending`)).toBeNull();
  await expect(page.getByText('登录未完成', { exact: true })).toHaveCount(0);
}

for (const app of apps) {
  test.describe(`T-08 ${app.kind} through release Nginx`, () => {
    test.beforeEach(async ({ page }) => { await systemFixtures(page, app); });

    test('HTTPS login, callback and page refresh preserve the App session', async ({ page }) => {
      await login(page, app);
      await page.reload();
      await success(page, app);
    });

    test('expired SSO session can sign in again after business session removal', async ({ page, context }) => {
      await login(page, app);
      const cookie = (await context.cookies(ownedUrl('SSO_TEST_HTTPS_ORIGIN'))).find(row => row.name === 'Sso-Token');
      expect(Boolean(cookie)).toBe(true);
      const expired = await page.request.post(`${ownedUrl('SSO_TEST_CONTROL_ORIGIN')}/__test/expire-session`, {
        headers: { Cookie: `Sso-Token=${cookie!.value}` }
      });
      expect(expired.ok()).toBe(true);
      await page.evaluate(key => localStorage.removeItem(key), app.tokenKey);
      await login(page, app);
      await success(page, app);
    });
  });
}

test('T-08 three HTTPS health routes and host-only SSO cookie boundaries', async ({ page, context }) => {
  const sso = ownedUrl('SSO_TEST_HTTPS_ORIGIN');
  for (const url of [sso + '/healthz', ...apps.map(app => ownedUrl(app.env) + '/healthz')]) {
    const response = await page.request.get(url);
    expect(response.status()).toBe(200);
    expect(await response.text()).toBe('ok\n');
  }
  const loginResponse = await page.request.post(sso + '/sso/login', {
    headers: { Origin: sso }, data: { username: 'sso-test-user', password: 'owned-sso-test-only' }
  });
  expect(loginResponse.status()).toBe(200);
  expect((await loginResponse.json()).code).toBe(200);
  const cookie = (await context.cookies(sso)).find(row => row.name === 'Sso-Token');
  expect(Boolean(cookie?.secure && cookie.httpOnly && cookie.path === '/' && !cookie.domain.startsWith('.'))).toBe(true);
  for (const app of apps) {
    expect((await context.cookies(ownedUrl(app.env))).some(row => row.name === 'Sso-Token')).toBe(false);
    const forbidden = await page.request.get(sso + '/sso/session', { headers: { Origin: new URL(ownedUrl(app.env)).origin } });
    expect(forbidden.status()).toBe(403);
    const hiddenControl = await page.request.post(ownedUrl(app.env) + '/prod-api/__test/expire-session');
    expect(hiddenControl.status()).toBe(404);
  }
  await page.goto(ownedUrl('SSO_TEST_AUTHORIZE_URL'));
  await expect(page.getByRole('heading', { name: 'WTA SSO', exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.cookie.includes('Sso-Token'))).toBe(false);
  expect(await page.evaluate(() => Object.keys(localStorage).some(key => /token/i.test(key)))).toBe(false);
  const session = await page.request.get(sso + '/sso/session', { headers: { Origin: sso } });
  expect(session.status()).toBe(200);
  expect((await session.json()).code).toBe(200);
  expect((await page.request.get(sso + '/')).status()).toBe(404);
});
