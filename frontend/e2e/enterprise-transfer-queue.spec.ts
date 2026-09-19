import { expect, test, type Page } from '@playwright/test';
import { resolve } from 'node:path';

const origin = process.env.TRANSFER_TEST_ORIGIN;
if (!origin || new URL(origin).hostname !== '127.0.0.1') throw new Error('Owned Home transfer fixture required');

function state() {
  return { permission: true, sends: 0, confirms: 0, expires: 300, status: 'QUEUED', requests: [] as unknown[], hold: undefined as Promise<void> | undefined };
}
async function install(page: Page, data: ReturnType<typeof state>) {
  await page.addInitScript(() => localStorage.setItem('Home-Token', 'owned-transfer-session'));
  await page.route('**/prod-api/**', async route => {
    const path = new URL(route.request().url()).pathname.slice('/prod-api'.length);
    const reply = (body: unknown) => route.fulfill({ json: { code: 200, data: body } });
    if (path === '/auth/client/context') return reply({ clientEnabled: true, registerEnabled: false, authMode: 'password' });
    if (path === '/system/user/getInfo') return reply({ user: { userId: '7', userName: 'owned', nickName: 'Owned' }, roles: [], permissions: data.permission ? ['profile:enterprise:apply'] : [] });
    if (path === '/system/menu/getRouters') return reply([
      { path: '/profile', name: 'ProfileCenter', component: 'profile/center/index', meta: { title: '档案中心' } },
      { path: '/profile/person', name: 'PersonVerification', component: 'profile/person/application', meta: { title: '个人认证' } }
    ]);
    if (path === '/profile/enterprise/transfer/send') {
      data.sends++; data.requests.push(route.request().postDataJSON());
      await data.hold;
      if (route.request().failure()) return;
      return reply({ status: 'QUEUED', challengeId: `owned-${data.sends}`, expiresInSeconds: data.expires });
    }
    if (path === '/profile/enterprise/transfer/confirm') {
      data.confirms++; data.requests.push(route.request().postDataJSON());
      return reply({ status: data.status, challengeId: data.status === 'QUEUED' ? `owned-${data.sends}` : null, expiresInSeconds: data.status === 'QUEUED' ? data.expires : null });
    }
    return reply(null);
  });
  await page.goto(`${origin}/profile`);
  await expect(page.getByRole('heading', { name: '档案中心', exact: true })).toBeVisible();
}
async function send(page: Page) {
  await page.getByLabel('接收者姓名', { exact: true }).fill('接收者');
  await page.getByLabel('证件后四位', { exact: true }).fill('3001');
  await page.getByLabel('接收者手机号', { exact: true }).fill('13800138000');
  await page.getByRole('button', { name: '发送转移验证码', exact: true }).click();
}

test('queued confirmation stays pending until the server reports transfer success', async ({ page }) => {
  const data = state(); await install(page, data); await send(page);
  await expect(page.getByRole('status').filter({ hasText: '验证码已排队' })).toBeVisible();
  if (process.env.TRANSFER_EVIDENCE_DIR) await page.screenshot({ path: resolve(process.env.TRANSFER_EVIDENCE_DIR, 'T-31-transfer-desktop.png'), fullPage: true });
  await page.getByLabel('转移验证码', { exact: true }).fill('123456');
  await page.getByRole('button', { name: '确认转移', exact: true }).click();
  await expect.poll(() => data.confirms).toBe(1);
  await expect(page.getByText('企业负责人已转移。', { exact: true })).toHaveCount(0);
  await expect(page.getByLabel('转移验证码', { exact: true })).toBeVisible();
  // 不触发浏览器防重复提交：等待新一次用户操作窗口，而不是重放第一次请求。
  await page.clock.install(); await page.clock.fastForward(1500);
  data.status = 'TRANSFERRED';
  await page.getByRole('button', { name: '确认转移', exact: true }).click();
  await expect(page.getByText('企业负责人已转移。', { exact: true })).toBeVisible();
  expect(data.sends).toBe(1); expect(data.confirms).toBe(2);
  expect(data.requests[1]).toEqual({ challengeId: 'owned-1', code: '123456' });
});

test('failed delivery can be resent and local expiry never enables confirmation', async ({ page }) => {
  const data = state(); data.status = 'FAILED'; await page.clock.install(); await install(page, data); await send(page);
  await page.getByLabel('转移验证码', { exact: true }).fill('123456');
  await page.getByRole('button', { name: '确认转移', exact: true }).click();
  await expect(page.getByText('验证码投递失败，请重新发码。', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: '重新发码', exact: true }).click();
  data.expires = 1; await page.clock.fastForward(1500); await send(page);
  await expect(page.getByLabel('转移验证码', { exact: true })).toBeVisible();
  await page.clock.fastForward(2000);
  await expect(page.getByRole('button', { name: '确认转移', exact: true })).toBeDisabled();
  await expect(page.getByText('验证码已过期，请重新发码。', { exact: true })).toBeVisible();
  expect(data.sends).toBe(2); expect(data.confirms).toBe(1);
});

test('repeated submission while pending sends only one command', async ({ page }) => {
  const data = state(); let release = () => {}; data.hold = new Promise<void>(resolve => { release = resolve; });
  try {
    await install(page, data); await send(page);
    await expect.poll(() => data.sends).toBe(1);
    await expect(page.getByRole('button', { name: '发送转移验证码', exact: true })).toBeDisabled();
    await page.locator('.enterprise-transfer form').first().evaluate(form => {
      form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    });
    release();
    await expect(page.getByLabel('转移验证码', { exact: true })).toBeVisible();
    expect(data.sends).toBe(1);
    await page.setViewportSize({ width: 320, height: 900 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    if (process.env.TRANSFER_EVIDENCE_DIR) await page.screenshot({ path: resolve(process.env.TRANSFER_EVIDENCE_DIR, 'T-31-transfer-mobile.png'), fullPage: true });
  } finally { release(); }
});

test('a session without enterprise apply permission has no transfer controls', async ({ page }) => {
  const data = state(); data.permission = false; await install(page, data);
  await expect(page.getByRole('heading', { name: '转移企业负责人', exact: true })).toHaveCount(0);
  expect(data.sends).toBe(0);
});

test('leaving the page aborts its pending send and does not restore a stale challenge', async ({ page }) => {
  const data = state(); let release = () => {}; data.hold = new Promise<void>(resolve => { release = resolve; });
  try {
    await install(page, data); await send(page);
    await expect.poll(() => data.sends).toBe(1);
    const cancelled = page.waitForEvent('requestfailed', request => request.url().endsWith('/profile/enterprise/transfer/send'));
    await page.locator('a[href="/profile/person"]').first().click();
    await expect(page.getByRole('heading', { name: '个人认证', exact: true })).toBeVisible();
    await cancelled; release();
    await page.locator('a[href="/profile"]').first().click();
    await expect(page.getByRole('heading', { name: '转移企业负责人', exact: true })).toBeVisible();
    await expect(page.getByLabel('转移验证码', { exact: true })).toHaveCount(0);
    await expect(page.getByRole('button', { name: '发送转移验证码', exact: true })).toBeEnabled();
  } finally { release(); }
});
