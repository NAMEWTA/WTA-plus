import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4174';
const homeUrl = process.env.HOME_WEB_URL ?? 'http://127.0.0.1:4175';
const screenshotDir = resolve(process.cwd(), '../temp/team/lead/e2e');
const adminClientId = 'e5cd7e4891bf95d1d19206ce24a7b32e';
const homeClientId = '428a8310cd442757ae699df5d894f051';

function shot(name: string) {
  mkdirSync(screenshotDir, { recursive: true });
  return resolve(screenshotDir, name);
}

function clientIdFromTokenResponse(payload: unknown): string {
  const body = payload as { data?: { client_id?: string; clientId?: string } };
  return String(body.data?.client_id ?? body.data?.clientId ?? '');
}

function extraClientId(token: string): string {
  const parts = token.split('.');
  if (parts.length < 2) {
    return '';
  }
  try {
    const json = Buffer.from(parts[1].replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8');
    const payload = JSON.parse(json) as Record<string, unknown>;
    const extra = (payload.extra ?? payload) as Record<string, unknown>;
    return String(extra.clientid ?? extra.clientId ?? payload.clientid ?? payload.clientId ?? '');
  } catch {
    return '';
  }
}

test.describe('SSO three hard gates', () => {
  test('AC-001 default provider, AC-002 reuse, AC-003 isolation', async ({ page, context, request }) => {
    test.setTimeout(120_000);
    await page.goto(`${adminUrl}/login`);
    const firstProvider = page.getByTestId('sso-first-provider');
    await expect(firstProvider).toBeVisible({ timeout: 20_000 });
    await page.screenshot({ path: shot('sso-ac001-default-provider-path.png'), fullPage: true });

    let adminClientFromApi = '';
    page.on('response', async response => {
      if (response.url().includes('/sso/oauth2/token') && response.ok()) {
        try {
          adminClientFromApi = clientIdFromTokenResponse(await response.json());
        } catch {
          /* ignore parse errors; token extras remain the fallback */
        }
      }
    });

    await firstProvider.click();
    await page.waitForURL(/127\.0\.0\.1:4176|\/authorize/, { timeout: 20_000 });
    const password = page.locator('input[name="password"]');
    if (await password.isVisible()) {
      await page.locator('input[name="username"]').fill('WTA');
      await password.fill('admin123');
      await page.locator('button[type="submit"]').click();
    }
    await page.waitForURL(url => {
      const parsed = new URL(url);
      return parsed.port === '4174' && parsed.pathname !== '/login';
    }, { timeout: 30_000 });
    await expect
      .poll(async () => page.evaluate(() => window.localStorage.getItem('Admin-Token')), { timeout: 20_000 })
      .toBeTruthy();
    await page.waitForURL(url => {
      const path = new URL(url).pathname;
      return path !== '/login' && path !== '/sso/callback';
    }, { timeout: 20_000 });
    await page.screenshot({ path: shot('sso-ac001-admin-logged-in.png'), fullPage: true });

    const adminToken = String(await page.evaluate(() => window.localStorage.getItem('Admin-Token')));
    const adminClient = adminClientFromApi || extraClientId(adminToken);

    const home = await context.newPage();
    let homeClientFromApi = '';
    home.on('response', async response => {
      if (response.url().includes('/sso/oauth2/token') && response.ok()) {
        try {
          homeClientFromApi = clientIdFromTokenResponse(await response.json());
        } catch {
          /* ignore parse errors; token extras remain the fallback */
        }
      }
    });
    await home.goto(`${homeUrl}/login`);
    const homeSso = home.getByTestId('sso-first-provider');
    await expect(homeSso).toBeVisible({ timeout: 20_000 });
    await homeSso.click();
    await home.waitForURL(/127\.0\.0\.1:4176/, { timeout: 20_000 });
    await expect(home.locator('input[name="password"]')).toHaveCount(0);
    await home.screenshot({ path: shot('sso-ac002-reuse-no-password.png'), fullPage: true });
    await home.waitForURL(/127\.0\.0\.1:4175/, { timeout: 30_000 });
    await expect
      .poll(async () => home.evaluate(() => window.localStorage.getItem('Home-Token')), { timeout: 20_000 })
      .toBeTruthy();
    const homeToken = String(await home.evaluate(() => window.localStorage.getItem('Home-Token')));
    const homeClient = homeClientFromApi || extraClientId(homeToken);
    expect(adminClient).not.toEqual(homeClient);
    expect(adminClient === 'sso' || homeClient === 'sso').toBeFalsy();

    const cross = await request.get('http://127.0.0.1:18080/system/user/getInfo', {
      headers: {
        Authorization: `Bearer ${adminToken}`,
        clientid: homeClientId
      }
    });
    const crossBody = (await cross.json()) as { code?: number; msg?: string };
    await home.screenshot({ path: shot('sso-ac003-client-isolation.png'), fullPage: true });
    expect(crossBody.code).not.toBe(200);
    expect(adminClient).toBe(adminClientId);
    expect(homeClient).toBe(homeClientId);
  });
});
