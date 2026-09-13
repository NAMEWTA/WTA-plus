import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4174';
const screenshotDir = resolve(process.cwd(), '../temp/team/lead/e2e');

function shot(name: string) {
  mkdirSync(screenshotDir, { recursive: true });
  return resolve(screenshotDir, name);
}

test.describe('Admin Client SSO 接入', () => {
  test('open Client list and SSO section by real clicks', async ({ page }) => {
    test.setTimeout(120_000);
    await page.goto(`${adminUrl}/login`);
    await page.locator('.login-form input').first().fill('WTA');
    await page.locator('.login-form input[type="password"]').fill('admin123');
    await page.locator('.login-form .submit-button').click();
    await page.waitForURL(url => !new URL(url).pathname.includes('/login'), { timeout: 30_000 });
    await expect.poll(async () => page.evaluate(() => window.localStorage.getItem('Admin-Token'))).toBeTruthy();

    await page.goto(`${adminUrl}/system/client`);
    await expect(page.getByText('客户端列表')).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('.el-table__body tr .el-table__cell').first()).toBeVisible({ timeout: 20_000 });
    await page.screenshot({ path: shot('sso-admin-config-list.png'), fullPage: true });

    await page.locator('.el-table__body tr').first().locator('.el-button').first().click();
    const dialog = page.locator('.el-dialog').filter({ hasText: 'SSO 接入' });
    await expect(dialog.getByText('SSO 接入')).toBeVisible({ timeout: 15_000 });
    await dialog.getByText('SSO 接入').scrollIntoViewIfNeeded();
    await expect(dialog.getByText('启用 SSO')).toBeVisible();
    await expect(dialog.getByText('精确回调')).toBeVisible();
    await expect(dialog.getByText('强制 PKCE')).toBeVisible();
    await dialog.screenshot({ path: shot('sso-admin-config-dialog.png') });

    await dialog.getByText('客户端类型').scrollIntoViewIfNeeded();
    await dialog.locator('.el-select').filter({ hasText: /public|confidential/ }).click();
    await page.getByRole('option', { name: /confidential/ }).click();
    await expect(dialog.getByText('已配置')).toBeVisible({ timeout: 10_000 });
    await dialog.getByText('已配置').scrollIntoViewIfNeeded();
    await dialog.screenshot({ path: shot('sso-admin-config-secret.png') });
  });
});
