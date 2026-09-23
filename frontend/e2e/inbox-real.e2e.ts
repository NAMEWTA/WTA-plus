import { expect, test } from '@playwright/test';

const required = (name: string) => {
  const value = process.env[name];
  if (!value) throw new Error(`${name} is required for the isolated T-34 browser test`);
  return value;
};

test('T-34 real Admin inbox without realtime', async ({ page }, testInfo) => {
  test.setTimeout(90_000);
  const origin = required('T34_ADMIN_ORIGIN');
  const username = required('T34_USERNAME');
  const password = required('T34_PASSWORD');
  const title = required('T34_NOTICE_TITLE');
  const body = required('T34_NOTICE_BODY');
  const inboxBodies: Array<{ code?: number; data?: Array<{ title?: string; category?: string }> }> = [];
  const pushRequests: string[] = [];

  page.on('request', request => {
    const path = new URL(request.url()).pathname;
    if (/\/resource\/message(?:\/ticket)?$/.test(path)) pushRequests.push(path);
  });
  page.on('response', response => {
    if (response.request().method() !== 'GET' || !new URL(response.url()).pathname.endsWith('/notify/inbox')) return;
    void response.json().then(body => inboxBodies.push(body)).catch(() => undefined);
  });

  await page.goto(new URL('/login', origin).toString());
  const form = page.locator('.login-form');
  await expect(form.locator('input').first()).toBeEnabled();
  await form.locator('input').first().fill(username);
  await form.locator('input[type="password"]').fill(password);
  await form.locator('.submit-button').click();
  await expect(page.locator('.message-trigger')).toBeVisible({ timeout: 30_000 });
  await page.locator('.message-trigger').click();
  await expect.poll(() => inboxBodies.some(result => result.code === 200 && result.data?.some(item => item.title === title && item.category === 'notice'))).toBe(true);
  const row = page.locator('.content-box-item').filter({ hasText: title });
  await expect(row).toBeVisible();
  await expect(row.getByText(body, { exact: true })).toHaveCount(1);
  await row.click();
  const detail = page.getByRole('dialog', { name: '通知详情' });
  await expect(detail).toBeVisible();
  await expect(detail.getByText(body, { exact: true })).toHaveCount(1);
  await testInfo.attach('t34-inbox-detail', { body: await page.screenshot({ animations: 'disabled' }), contentType: 'image/png' });
  await expect(page.getByRole('alert').filter({ hasText: '消息加载失败，请重试' })).toHaveCount(0);
  expect(pushRequests).toEqual([]);
});
