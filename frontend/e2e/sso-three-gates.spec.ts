import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { startFirstPartyAuthorization } from './first-party-sso-fixture.mjs';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4174';
const homeUrl = process.env.HOME_WEB_URL ?? 'http://127.0.0.1:4175';
const authorizeUrl = process.env.SSO_TEST_AUTHORIZE_URL ?? 'http://127.0.0.1:4176/authorize';
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

test.describe('First-party SSO compatibility gates', () => {
  test('AC-001 explicit protocol fixture, AC-002 reuse, AC-003 isolation', async ({ page, context, request }) => {
    test.setTimeout(120_000);
    await page.goto(`${adminUrl}/login`);
    await expect(page.getByRole('textbox', { name: '用户名', exact: true })).toBeVisible();
    await expect(page.getByTestId('sso-first-provider')).toHaveCount(0);
    await page.screenshot({ path: shot('sso-ac001-local-login-retained.png'), fullPage: true });

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

    await startFirstPartyAuthorization(page, {
      app: 'admin',
      appOrigin: adminUrl,
      clientId: adminClientId,
      authorizeUrl,
      returnTo: '/index'
    });
    await page.waitForURL(url => url.origin === new URL(authorizeUrl).origin, { timeout: 20_000 });
    const password = page.locator('input[name="password"]');
    if (await password.isVisible()) {
      await page.locator('input[name="username"]').fill('WTA');
      await password.fill('admin123');
      await page.locator('button[type="submit"]').click();
    }
    await page.waitForURL(
      url => {
        const parsed = new URL(url);
        return parsed.origin === new URL(adminUrl).origin && parsed.pathname !== '/login';
      },
      { timeout: 30_000 }
    );
    await expect
      .poll(async () => page.evaluate(() => window.localStorage.getItem('Admin-Token')), { timeout: 20_000 })
      .toBeTruthy();
    await page.waitForURL(
      url => {
        const path = new URL(url).pathname;
        return path !== '/login' && path !== '/sso/callback';
      },
      { timeout: 20_000 }
    );
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
    await startFirstPartyAuthorization(home, {
      app: 'home',
      appOrigin: homeUrl,
      clientId: homeClientId,
      authorizeUrl,
      returnTo: '/profile'
    });
    const sawSsoWeb = home
      .waitForURL(url => url.origin === new URL(authorizeUrl).origin, { timeout: 20_000 })
      .then(async () => {
        await expect(home.locator('input[name="password"]')).toHaveCount(0);
      })
      .catch(() => undefined);
    await home.waitForURL(
      url => {
        const parsed = new URL(url);
        return parsed.origin === new URL(homeUrl).origin;
      },
      { timeout: 30_000 }
    );
    await sawSsoWeb;
    await expect(home.locator('input[name="password"]')).toHaveCount(0);
    await home.waitForURL(
      url => {
        const parsed = new URL(url);
        return (
          parsed.origin === new URL(homeUrl).origin &&
          parsed.pathname !== '/login' &&
          parsed.pathname !== '/sso/callback' &&
          !parsed.pathname.endsWith('/sso/callback')
        );
      },
      { timeout: 30_000 }
    );
    await expect
      .poll(async () => home.evaluate(() => window.localStorage.getItem('Home-Token')), { timeout: 20_000 })
      .toBeTruthy();
    const homeHeader = home.locator('header');
    await expect(home.locator('.avatar-container .el-dropdown')).toBeVisible({ timeout: 20_000 });
    await expect(homeHeader.getByRole('link', { name: '登录', exact: true })).toHaveCount(0);
    await expect(home.getByText('没有访问权限')).toHaveCount(0);
    await expect(home).toHaveURL(/\/profile/);
    await home.screenshot({ path: shot('sso-ac002-reuse-no-password.png'), fullPage: true });
    const homeToken = String(await home.evaluate(() => window.localStorage.getItem('Home-Token')));
    const homeClient = homeClientFromApi || extraClientId(homeToken);
    expect(adminClient).not.toEqual(homeClient);
    expect(adminClient === 'sso' || homeClient === 'sso').toBeFalsy();

    const cross = await request.get(
      `${process.env.SSO_TEST_BACKEND_ORIGIN ?? 'http://127.0.0.1:38888'}/system/user/getInfo`,
      {
        headers: {
          Authorization: `Bearer ${adminToken}`,
          clientid: homeClientId
        }
      }
    );
    const crossBody = (await cross.json()) as { code?: number; msg?: string };
    await expect(home.locator('.avatar-container .el-dropdown')).toBeVisible();
    await expect(home.getByText('没有访问权限')).toHaveCount(0);
    await home.screenshot({ path: shot('sso-ac003-client-isolation.png'), fullPage: true });
    expect(crossBody.code).not.toBe(200);
    expect(adminClient).toBe(adminClientId);
    expect(homeClient).toBe(homeClientId);
  });
});
