import { readFile } from 'node:fs/promises';
import { expect, test, type Page, type Response } from '@playwright/test';

const password = 'OwnedPass!9';
const apps = [
  { kind: 'admin', env: 'TRANSPORT_ADMIN_ORIGIN', clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e', tokenKey: 'Admin-Token', target: '/transport-users' },
  { kind: 'home', env: 'TRANSPORT_HOME_ORIGIN', clientId: '428a8310cd442757ae699df5d894f051', tokenKey: 'Home-Token', target: '/profile' }
] as const;
type App = (typeof apps)[number];

test.beforeEach(async ({ page }) => {
  // Observe the real XHR before Home's full navigation discards the DevTools response body.
  // Keep only the parsed status code; never persist a response token or request password.
  await page.addInitScript(() => {
    const send = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function (body) {
      this.addEventListener('readystatechange', () => {
        if (this.readyState !== 4 || !/\/auth\/(login|register)$/.test(this.responseURL)) return;
        try {
          const value = typeof this.response === 'string' ? JSON.parse(this.response) : this.response;
          sessionStorage.setItem(`transport-observed:${new URL(this.responseURL).pathname}`, String(value.code));
        } catch {
          sessionStorage.setItem(`transport-observed:${new URL(this.responseURL).pathname}`, 'invalid-json');
        }
      });
      return send.call(this, body);
    };
  });
});

async function observedCode(page: Page, path: string, expected: string): Promise<void> {
  await expect.poll(() => page.evaluate(key => sessionStorage.getItem(key), `transport-observed:/prod-api/auth/${path}`)).toBe(expected);
}

function origin(app: App): string {
  const value = process.env[app.env];
  if (!value || new URL(value).protocol !== 'https:' || new URL(value).hostname !== '127.0.0.1') {
    throw new Error(`Owned HTTPS fixture required: ${app.env}`);
  }
  return value;
}

async function assertJson(response: Response, app: App, expectedPassword: string): Promise<void> {
  expect(response.status()).toBe(200);
  const request = response.request();
  expect(new URL(request.url()).protocol).toBe('https:');
  expect(request.headers()['encrypt-key']).toBeUndefined();
  expect(request.headers()['isencrypt']).toBeUndefined();
  expect(request.headers().clientid).toBe(app.clientId);
  expect(request.headers()['content-type']).toContain('application/json');
  // Assertions report booleans instead of printing credentials in failure evidence.
  const body = request.postDataJSON() as Record<string, unknown>;
  expect(body.password === expectedPassword && body.clientId === app.clientId).toBe(true);
  expect(response.headers()['encrypt-key']).toBeUndefined();
  expect(response.headers()['content-type']).toContain('application/json');
}

async function submitLogin(page: Page, app: App, username: string, secret = password): Promise<Response> {
  await page.goto(`${origin(app)}/login?redirect=${encodeURIComponent(app.target)}`);
  const form = page.locator(app.kind === 'admin' ? '.login-form' : '.identity-login__form');
  const usernameInput = app.kind === 'admin' ? form.locator('input').first() : form.locator('input[name="username"]');
  await expect(usernameInput).toBeEnabled();
  await usernameInput.fill(username);
  await form.locator('input[type="password"]').fill(secret);
  const completed = page.waitForResponse(response => response.url().endsWith('/auth/login'));
  await form.locator(app.kind === 'admin' ? '.submit-button' : '.identity-login__submit').click();
  const response = await completed;
  await assertJson(response, app, secret);
  return response;
}

for (const app of apps) {
  test(`T-11 ${app.kind} registers, logs in and reloads using ordinary HTTPS JSON`, async ({ page }) => {
    // No Playwright route mocks: every API request reaches the owned Spring MVC server.
    const prepared = page.waitForResponse(response => response.url().endsWith('/auth/code'));
    await page.goto(`${origin(app)}/register`);
    await (await prepared).finished();
    const form = page.locator('.register-form');
    await expect(form.locator('input').first()).toBeVisible();
    await form.locator('input').nth(0).fill(`registered-${app.kind}`);
    await form.getByLabel('手机号码', { exact: true }).fill(app.kind === 'admin' ? '13800138001' : '13800138002');
    await form.locator('input[type="password"]').nth(0).fill(password);
    await form.locator('input[type="password"]').nth(1).fill(password);
    const registered = page.waitForResponse(response => response.url().endsWith('/auth/register'));
    await form.getByRole('button', { name: app.kind === 'admin' ? '注 册' : '注册', exact: true }).click();
    const response = await registered;
    await assertJson(response, app, password);
    if (app.kind === 'admin') {
      expect((await response.json()).code).toBe(200);
      await page.getByRole('button', { name: '确定', exact: true }).click();
    }
    await expect(page).toHaveURL(`${origin(app)}/login`);
    await observedCode(page, 'register', '200');
    await submitLogin(page, app, `registered-${app.kind}`);
    await expect(page).toHaveURL(origin(app) + app.target);
    await observedCode(page, 'login', '200');
    expect(await page.evaluate(key => Boolean(localStorage.getItem(key)), app.tokenKey)).toBe(true);
    await page.reload();
    await expect(page).toHaveURL(origin(app) + app.target);
    await expect(page.getByRole('heading', { name: app.kind === 'admin' ? '用户列表' : '档案中心', exact: true })).toBeVisible();
  });

  test(`T-11 ${app.kind} preserves ordinary JSON login failures without creating a session`, async ({ page }) => {
    const response = await submitLogin(page, app, `owned-${app.kind}`, 'WrongPass!8');
    expect((await response.json()).code).not.toBe(200);
    await expect(page.getByText('账号或密码错误', { exact: true }).first()).toBeVisible();
    expect(await page.evaluate(key => localStorage.getItem(key), app.tokenKey)).toBeNull();
    expect(new URL(page.url()).pathname).toBe('/login');
  });
}

test('T-11 Admin download preserves binary bytes and displays a JSON download failure', async ({ page }) => {
  const app = apps[0];
  await submitLogin(page, app, 'owned-admin');
  await expect(page.getByRole('heading', { name: '用户列表', exact: true })).toBeVisible();
  await page.getByRole('button', { name: '更多', exact: true }).hover();
  const downloaded = page.waitForEvent('download');
  await page.getByRole('menuitem', { name: '导出数据' }).click();
  const download = await downloaded;
  expect(download.suggestedFilename()).toMatch(/^user_\d+\.xlsx$/);
  expect([...await readFile((await download.path())!)]).toEqual([0, 1, 2, 127, 128, 255, 10]);
  await expect(page.locator('.el-loading-mask')).toHaveCount(0);
  await page.getByRole('button', { name: '更多', exact: true }).hover();
  await page.getByRole('menuitem', { name: '下载模板' }).click();
  await expect(page.getByText('下载测试失败', { exact: true })).toBeVisible();
  await expect(page.locator('.el-loading-mask')).toHaveCount(0);
});

test('T-11 real Auth MVC rejects unknown Client and malformed registration without writing users', async ({ request }) => {
  const base = origin(apps[0]) + '/prod-api';
  const unknown = await request.post(base + '/auth/login', { data: { clientId: 'unknown', grantType: 'password', username: 'owned-admin', password } });
  expect((await unknown.json()).code).not.toBe(200);
  const invalid = await request.post(base + '/auth/register', { data: { clientId: apps[0].clientId, username: '', password } });
  expect((await invalid.json()).code).not.toBe(200);
  const weak = await request.post(base + '/auth/register', { data: { clientId: apps[0].clientId, username: 'weak-policy', password: 'weak', phoneNumber: '13800138003' } });
  const result = await weak.json();
  expect(result.code).not.toBe(200);
  expect(result.data.violations.length).toBeGreaterThan(0);
});
