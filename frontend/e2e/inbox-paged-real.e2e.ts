import { readFileSync } from 'node:fs';
import { expect, test, type Page } from '@playwright/test';

interface SeedManifest {
  schema_version: 1;
  run_id: string;
  a: { userId: string; total: number; unread: number };
  b: { userId: string; total: number; unread: number };
  aOldest: { messageId: string; title: string };
  shared: { messageId: string; title: string };
  bOnly: { messageId: string; title: string };
  pageNum: 26;
  pageSize: 20;
}

function required(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error('Required isolated T41 variable is missing: ' + name);
  return value;
}

function seed(): SeedManifest {
  const parsed: unknown = JSON.parse(readFileSync(required('T41_SEED_MANIFEST'), 'utf8'));
  if (!parsed || typeof parsed !== 'object') throw new Error('T41 seed manifest is invalid');
  const value = parsed as Partial<SeedManifest>;
  if (value.schema_version !== 1 || value.run_id !== required('T41_RUN_ID') ||
      value.pageNum !== 26 || value.pageSize !== 20 ||
      value.a?.total !== 501 || value.a?.unread !== 481 ||
      value.b?.total !== 2 || value.b?.unread !== 2 ||
      !value.aOldest?.messageId || !value.shared?.messageId || !value.bOnly?.messageId) {
    throw new Error('T41 seed manifest does not match the frozen browser contract');
  }
  return value as SeedManifest;
}

async function login(page: Page, origin: string, username: string, password: string) {
  await page.goto(new URL('/login', origin).toString());
  const form = page.locator('.login-form');
  await expect(form.locator('input').first()).toBeEnabled();
  await form.locator('input').first().fill(username);
  await form.locator('input[type="password"]').fill(password);
  await form.locator('.submit-button').click();
  await expect(page.locator('.message-trigger')).toBeVisible({ timeout: 30_000 });
}

async function inbox(page: Page, origin: string) {
  await page.goto(new URL('/notify/inbox', origin).toString());
  await expect(page.getByText('通知收件箱 · 未读', { exact: false })).toBeVisible({ timeout: 30_000 });
}

test('T-41 real Admin inbox page 26 and global read-all', async ({ browser }) => {
  test.setTimeout(90_000);
  const manifest = seed();
  const origin = required('T41_ADMIN_ORIGIN');
  const backendOrigin = required('T41_BACKEND_ORIGIN');
  const pushRequests: string[] = [];
  const contextA = await browser.newContext();
  const contextB = await browser.newContext();
  try {
    const a = await contextA.newPage();
    const b = await contextB.newPage();
    for (const page of [a, b]) {
      page.on('request', request => {
        const url = new URL(request.url());
        if ((url.origin === backendOrigin || url.origin === origin) &&
            (url.pathname.endsWith('/resource/message') || url.pathname.endsWith('/resource/message/ticket'))) {
          pushRequests.push('push-request');
        }
      });
    }

    await login(a, origin, required('T41_A_USERNAME'), required('T41_A_PASSWORD'));
    await a.locator('.message-trigger').click();
    const popover = a.locator('.el-popover').filter({ hasText: '消息盒子' });
    await expect(popover.getByText('未读 481', { exact: true })).toBeVisible();
    await expect(popover.locator('.content-box-item')).toHaveCount(10);
    const latestTitle = 'T41 ' + manifest.run_id + ' A 501';
    const topRow = popover.locator('.content-box-item').filter({ hasText: latestTitle });
    await expect(topRow).toBeVisible();
    await expect(topRow.locator('.content-box-msg')).toHaveText(latestTitle);
    const latestId = (BigInt(manifest.aOldest.messageId) + 1000n).toString();
    const topDetailResponse = a.waitForResponse(response =>
      response.request().method() === 'GET' &&
      new URL(response.url()).pathname.endsWith('/notify/inbox/' + latestId));
    await topRow.click();
    expect((await topDetailResponse).status()).toBe(200);
    const topDetail = a.getByRole('dialog', { name: '通知详情' });
    await expect(topDetail).toBeVisible();
    await expect(topDetail.locator('.el-descriptions__content').last()).toHaveText(latestTitle);
    await topDetail.getByRole('button', { name: '关闭', exact: true }).click();

    await inbox(a, origin);
    await expect(a.getByText('通知收件箱 · 未读 481')).toBeVisible();
    await expect(a.locator('.el-table__body-wrapper tbody tr')).toHaveCount(20);
    const lastResponse = a.waitForResponse(response => {
      const url = new URL(response.url());
      return response.request().method() === 'GET' && url.pathname.endsWith('/notify/inbox') &&
        url.searchParams.get('pageNum') === String(manifest.pageNum) &&
        url.searchParams.get('pageSize') === String(manifest.pageSize);
    });
    await a.locator('.el-pagination .el-pager li').filter({ hasText: /^26$/ }).click();
    expect((await lastResponse).status()).toBe(200);
    await expect(a.locator('.el-table__body-wrapper tbody tr')).toHaveCount(1);
    const oldest = a.locator('.el-table__body-wrapper tbody tr').filter({ hasText: manifest.aOldest.title });
    await expect(oldest).toBeVisible();
    await expect(a.getByText(manifest.bOnly.title)).toHaveCount(0);

    await login(b, origin, required('T41_B_USERNAME'), required('T41_B_PASSWORD'));
    await inbox(b, origin);
    await expect(b.getByText('通知收件箱 · 未读 2')).toBeVisible();
    await expect(b.locator('.el-table__body-wrapper tbody tr')).toHaveCount(2);
    await expect(b.getByText(manifest.shared.title)).toBeVisible();
    await expect(b.getByText(manifest.bOnly.title)).toBeVisible();
    await expect(b.getByText(manifest.aOldest.title)).toHaveCount(0);

    const oldestDetailResponse = a.waitForResponse(response =>
      response.request().method() === 'GET' &&
      new URL(response.url()).pathname.endsWith('/notify/inbox/' + manifest.aOldest.messageId));
    await oldest.getByRole('button', { name: '查看详情' }).click();
    expect((await oldestDetailResponse).status()).toBe(200);
    const fullDetail = a.getByRole('dialog', { name: '通知详情' });
    await expect(fullDetail.locator('.el-descriptions__content').last()).toHaveText(manifest.aOldest.title);
    await expect(a.getByText('通知收件箱 · 未读 480')).toBeVisible();
    await fullDetail.getByRole('button', { name: '关闭', exact: true }).click();
    await a.getByRole('button', { name: /全部已读/ }).click();
    await expect(a.getByText('通知收件箱 · 未读 0')).toBeVisible();
    await a.locator('.message-trigger').click();
    const refreshedPopover = a.locator('.el-popover').filter({ hasText: '消息盒子' });
    await expect(refreshedPopover.getByText('未读 0', { exact: true })).toBeVisible();
    await b.getByRole('button', { name: '刷新' }).click();
    await expect(b.getByText('通知收件箱 · 未读 2')).toBeVisible();
    expect(pushRequests).toEqual([]);
  } finally {
    await Promise.all([contextA.close(), contextB.close()]);
  }
});
