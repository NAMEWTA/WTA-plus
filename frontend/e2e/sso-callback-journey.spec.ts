import { createHash } from 'node:crypto';
import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

interface AppFixture { kind: 'admin' | 'home'; clientId: string; env: string; tokenKey: string; target: string }

const apps: AppFixture[] = [
  { kind: 'admin', clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e', env: 'SSO_TEST_ADMIN_ORIGIN', tokenKey: 'Admin-Token', target: '/index?source=sso#restored' },
  { kind: 'home', clientId: '428a8310cd442757ae699df5d894f051', env: 'SSO_TEST_HOME_ORIGIN', tokenKey: 'Home-Token', target: '/profile?source=sso#restored' }
];

function isolatedOrigin(name: string): string {
  const value = process.env[name];
  if (!value || !['localhost', '127.0.0.1'].includes(new URL(value).hostname)) throw new Error(`Owned fixture required: ${name}`);
  return value;
}

const fingerprint = (value: string) => createHash('sha256').update(value).digest('hex');

async function systemFixtures(page: Page, app: AppFixture) {
  // System身份/菜单为显式fixture；SSO HTTP、Redis和MySQL始终走自建真实后端。
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname.slice('/api'.length);
    if (path.startsWith('/sso/')) return route.continue();
    let data: unknown = [];
    if (path === '/auth/client/context') data = { clientEnabled: true, registerEnabled: false, ssoEnabled: true,
      authMode: 'both', ssoAuthorizeUrl: `${isolatedOrigin('SSO_TEST_HTTPS_ORIGIN')}/authorize` };
    if (path === '/auth/code') data = { captchaEnabled: false };
    if (path === '/system/user/getInfo') data = { user: { userId: '7', userName: 'sso-test-user', nickName: 'Owned SSO user', avatarUrl: '' }, roles: ['owned-role'], permissions: [] };
    if (path === '/system/menu/getRouters') data = app.kind === 'home'
      ? [{ path: '/profile', name: 'ProfileCenter', component: 'profile/center/index', meta: { title: '档案中心' } }]
      : [];
    await route.fulfill({ json: { code: 200, data } });
  });
}

async function start(page: Page, app: AppFixture) {
  const base = isolatedOrigin(app.env);
  await page.goto(`${base}/login?${new URLSearchParams({ redirect: app.target })}`);
  await page.getByTestId('sso-first-provider').click();
  await page.waitForURL(url => url.hostname === 'localhost' && url.pathname === '/authorize');
  const authorize = new URL(page.url());
  expect(authorize.searchParams.get('redirect_uri')).toBe(`${base}/sso/callback`);
  expect(authorize.searchParams.get('client_id')).toBe(app.clientId);
}

async function password(page: Page) {
  await page.getByLabel('用户名').fill('sso-test-user');
  await page.getByLabel('密码').fill('owned-sso-test-only');
  await page.getByRole('button', { name: '登录并继续' }).click();
}

async function success(page: Page, app: AppFixture, target = app.target) {
  const expected = new URL(isolatedOrigin(app.env) + target);
  // 只报告是否到达目标，失败日志不打印过渡URL中的code/state。
  await expect.poll(() => page.url() === expected.toString()).toBe(true);
  expect(await page.evaluate(key => Boolean(localStorage.getItem(key)), app.tokenKey)).toBe(true);
  expect(await page.evaluate(key => sessionStorage.getItem(key), `namewta-${app.kind}-sso-pending`)).toBeNull();
  await expect(page.getByText('登录未完成', { exact: true })).toHaveCount(0);
  if (app.kind === 'home') await expect(page.getByRole('button', { name: '退出', exact: true })).toBeVisible();
}

async function failure(page: Page, app: AppFixture, message: RegExp) {
  await expect(page.getByRole('alert')).toContainText(message);
  expect(new URL(page.url()).search).toBe('');
  expect(await page.evaluate(key => sessionStorage.getItem(key), `namewta-${app.kind}-sso-pending`)).toBeNull();
  expect(await page.evaluate(key => localStorage.getItem(key), app.tokenKey)).toBeNull();
  await expect(page.locator('body')).not.toContainText('owned-business-test-token');
}

function trackAuthorize(page: Page) {
  const requests: string[] = [];
  page.on('request', request => {
    const url = new URL(request.url());
    if (url.pathname === '/authorize') requests.push(fingerprint(`${url.searchParams.get('state')}:${url.searchParams.get('code_challenge')}`));
  });
  return requests;
}

for (const app of apps) {
  test.describe(`T-07 ${app.kind} callback journey`, () => {
    test.beforeEach(async ({ page }) => { await systemFixtures(page, app); });

    test('registered context callback returns to the requested App route', async ({ page }) => {
      await start(page, app);
      await password(page);
      await success(page, app);
    });

    test('opaque state including spaces, plus, ampersand, percent and Unicode survives the browser round trip', async ({ page }) => {
      const redirectUri = `${isolatedOrigin(app.env)}/sso/callback`;
      const verifier = 'dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk';
      const state = '  +&%中文/尾部  ';
      await page.addInitScript(({ redirectUri, pending, key }) => {
        if (location.href.startsWith(redirectUri)) sessionStorage.setItem(key, JSON.stringify(pending));
      }, { redirectUri, pending: { clientId: app.clientId, redirectUri, verifier, state, returnTo: app.target }, key: `namewta-${app.kind}-sso-pending` });
      const query = new URLSearchParams({ response_type: 'code', client_id: app.clientId, redirect_uri: redirectUri,
        state, code_challenge: createHash('sha256').update(verifier).digest('base64url'), code_challenge_method: 'S256' });
      await page.goto(`${isolatedOrigin('SSO_TEST_HTTPS_ORIGIN')}/authorize?${query}`);
      await password(page);
      await success(page, app);
    });

    test('expired real authorization code offers a fresh authorization without reusing credentials', async ({ page }) => {
      const authorizations = trackAuthorize(page);
      let count = 0;
      await page.route('**/api/sso/oauth2/token', async route => {
        if (++count === 1) expect((await page.request.post(`${isolatedOrigin('SSO_TEST_HTTPS_ORIGIN')}/__test/advance-clock`)).ok()).toBe(true);
        await route.continue();
      });
      await start(page, app);
      await password(page);
      await failure(page, app, /授权已过期/);
      expect(count).toBe(1);
      await page.getByRole('button', { name: '重新授权', exact: true }).click();
      await success(page, app);
      expect(count).toBe(2);
      expect(authorizations.length === 2 && authorizations[0] !== authorizations[1]).toBe(true);
    });

    test('network failure and refresh never replay the old exchange', async ({ page }) => {
      const authorizations = trackAuthorize(page);
      let count = 0;
      await page.route('**/api/sso/oauth2/token', async route => {
        if (++count === 1) return route.abort('failed');
        await route.continue();
      });
      await start(page, app);
      await password(page);
      await failure(page, app, /无法确认登录结果/);
      await page.reload();
      await failure(page, app, /登录校验已失效/);
      expect(count).toBe(1);
      await page.getByRole('button', { name: '重新授权', exact: true }).click();
      await success(page, app, app.kind === 'admin' ? '/index' : '/profile');
      expect(count).toBe(2);
      expect(authorizations.length === 2 && authorizations[0] !== authorizations[1]).toBe(true);
    });

    test('wrong verifier is rejected by the real backend and fresh authorization recovers', async ({ page }) => {
      const authorizations = trackAuthorize(page);
      await page.addInitScript(({ key, callback }) => {
        if (location.pathname !== callback || sessionStorage.getItem('owned-verifier-tampered')) return;
        const raw = sessionStorage.getItem(key);
        const pending: unknown = raw ? JSON.parse(raw) : null;
        if (pending && typeof pending === 'object' && 'verifier' in pending) {
          pending.verifier = 'a'.repeat(43);
          sessionStorage.setItem(key, JSON.stringify(pending));
          sessionStorage.setItem('owned-verifier-tampered', 'true');
        }
      }, { key: `namewta-${app.kind}-sso-pending`, callback: `/${app.kind}/sso/callback` });
      await start(page, app);
      await password(page);
      await failure(page, app, /未能完成登录/);
      await page.getByRole('button', { name: '重新授权', exact: true }).click();
      await success(page, app);
      expect(authorizations.length === 2 && authorizations[0] !== authorizations[1]).toBe(true);
    });

    test('wrong state fails before exchange and can restart from the callback page', async ({ page }) => {
      const redirectUri = `${isolatedOrigin(app.env)}/sso/callback`;
      await page.addInitScript(({ redirectUri, key, clientId, target }) => {
        if (!location.href.startsWith(redirectUri) || sessionStorage.getItem('owned-state-seeded')) return;
        sessionStorage.setItem(key, JSON.stringify({ clientId, redirectUri, returnTo: target, state: 'owned-correct-state', verifier: 'b'.repeat(43) }));
        sessionStorage.setItem('owned-state-seeded', 'true');
      }, { redirectUri, key: `namewta-${app.kind}-sso-pending`, clientId: app.clientId, target: app.target });
      let exchanges = 0;
      page.on('request', request => { if (new URL(request.url()).pathname === '/api/sso/oauth2/token') exchanges += 1; });
      await page.goto(`${redirectUri}?code=owned-not-issued-code&state=wrong-state`);
      await failure(page, app, /登录校验已失效/);
      expect(exchanges).toBe(0);
      await page.getByRole('button', { name: '重新授权', exact: true }).click();
      await page.waitForURL(url => url.pathname === '/authorize' && url.hostname === 'localhost');
      await password(page);
      await success(page, app);
      expect(exchanges).toBe(1);
    });
  });
}
