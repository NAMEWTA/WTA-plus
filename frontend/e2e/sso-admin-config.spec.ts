import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4174';
const screenshotDir = resolve(process.cwd(), '../temp/team/lead/e2e');

function shot(name: string) {
  mkdirSync(screenshotDir, { recursive: true });
  return resolve(screenshotDir, name);
}

async function loginAdmin(page: import('@playwright/test').Page) {
  await page.goto(`${adminUrl}/login`);
  await page.locator('.login-form input').first().fill('WTA');
  await page.locator('.login-form input[type="password"]').fill('admin123');
  await page.locator('.login-form .submit-button').click();
  await page.waitForURL(url => !new URL(url).pathname.includes('/login'), { timeout: 30_000 });
  await expect.poll(async () => page.evaluate(() => window.localStorage.getItem('Admin-Token'))).toBeTruthy();
}

test.describe('Round4 SSO Admin + Client bind', () => {
  test('SSO 管理 create/config and Client own-app 已接入', async ({ page }) => {
    test.setTimeout(180_000);
    await loginAdmin(page);

    await page.goto(`${adminUrl}/system/ssoApp`);
    await expect(page.getByTestId('sso-admin-title')).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('.el-table__row').first()).toBeVisible({ timeout: 20_000 });
    await page.screenshot({ path: shot('sso-r4-admin-list.png'), fullPage: true });

    await page.getByRole('button', { name: '创建应用' }).click();
    const dialog = page.locator('.el-dialog').filter({ hasText: '创建 SSO 应用' });
    await expect(dialog).toBeVisible();
    const key = `r4app${Date.now()}`;
    await dialog.getByPlaceholder('例如 demo-app').fill(key);
    await dialog.getByPlaceholder('用于生成 clientId，非 OAuth 密钥').fill(`${key}-secret`);
    await dialog.locator('textarea').fill('http://127.0.0.1:4174/sso/callback');
    await dialog.locator('.el-select').last().click();
    await page.getByRole('option').first().click();
    await page.screenshot({ path: shot('sso-r4-admin-create.png'), fullPage: true });
    await dialog.getByRole('button', { name: '保存并交付' }).click();
    const delivered = page.locator('.el-message-box').filter({ hasText: '请立即保存配置' });
    await expect(delivered).toBeVisible({ timeout: 15_000 });
    await expect(delivered.getByText(/clientId:/)).toBeVisible();
    await delivered.screenshot({ path: shot('sso-r4-admin-config.png') });
    await page.getByRole('button', { name: '已保存' }).click();

    await page.goto(`${adminUrl}/system/client`);
    await expect(page.getByText('客户端列表')).toBeVisible({ timeout: 20_000 });
    await expect(page.getByTestId('sso-access-success').first()).toBeVisible({ timeout: 20_000 });
    await page.screenshot({ path: shot('sso-r4-client-success.png'), fullPage: true });
    await expect(page.getByTestId('sso-access-success').first()).toHaveText('已接入');
  });
});
