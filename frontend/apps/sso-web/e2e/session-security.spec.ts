import { createHash } from 'node:crypto';
import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

const verifier = 'dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk';
const challenge = createHash('sha256').update(verifier).digest('base64url');
const clientId = 'sso-security-business-client';

function origin(name: string): string {
  const value = process.env[name];
  if (!value || !['localhost', '127.0.0.1'].includes(new URL(value).hostname)) {
    throw new Error(`Missing isolated test origin: ${name}`);
  }
  return value;
}

function record(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) throw new Error('Invalid response object');
  return value as Record<string, unknown>;
}

function query(base: string): URLSearchParams {
  return new URLSearchParams({
    response_type: 'code', client_id: clientId, redirect_uri: `${base}/callback`,
    state: 'owned-test-state', code_challenge: challenge, code_challenge_method: 'S256'
  });
}

async function json(page: Page, path: string, payload?: Record<string, unknown>): Promise<Record<string, unknown>> {
  const value: unknown = await page.evaluate(async ({ path, payload }) => {
    const response = await fetch(path, payload === undefined ? { credentials: 'include' } : {
      method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
    });
    const value: unknown = await response.json();
    return value;
  }, { path, payload });
  return record(value);
}

async function login(page: Page, base: string): Promise<string> {
  await page.goto(`${base}/authorize?${query(base)}`);
  await page.getByLabel('用户名').fill('sso-test-user');
  await page.getByLabel('密码').fill('owned-sso-test-only');
  await page.getByRole('button', { name: '登录并继续' }).click();
  await page.waitForURL(url => url.pathname === '/callback');
  const code = new URL(page.url()).searchParams.get('code');
  if (!code) throw new Error('Missing callback code');
  return code;
}

test('HTTPS login preserves host-only HttpOnly cookies, PKCE, expiry, one-time use and CORS boundaries', async ({ page, context }) => {
  test.setTimeout(60_000);
  const base = origin('SSO_TEST_HTTPS_ORIGIN');
  await context.addCookies([{ name: 'Sso-Token', value: '1af730bb2e87131a1af730bb2e87131b', url: base, httpOnly: true, secure: true, sameSite: 'Lax' }]);
  const code = await login(page, base);
  expect(code).toMatch(/^[A-Za-z0-9_-]{43}$/);
  const cookie = (await context.cookies()).find(cookie => cookie.name === 'Sso-Token');
  expect(cookie).toMatchObject({ domain: 'localhost', path: '/', secure: true, httpOnly: true, sameSite: 'Lax' });
  expect(cookie?.value).toMatch(/^[A-Za-z0-9_-]{43}$/);
  expect(await page.evaluate(() => document.cookie)).not.toContain('Sso-Token');
  expect((await json(page, '/sso/session')).code).toBe(200);

  const exchange = { grant_type: 'authorization_code', code, redirect_uri: `${base}/callback`, client_id: clientId, code_verifier: verifier };
  expect((await json(page, '/sso/oauth2/token', { ...exchange, code_verifier: `${verifier}wrong` })).code).not.toBe(200);
  expect((await json(page, '/sso/oauth2/token', { ...exchange, client_id: 'another-client' })).code).not.toBe(200);
  expect((await json(page, '/sso/oauth2/token', { ...exchange, redirect_uri: `${base}/other-callback` })).code).not.toBe(200);
  const results = await Promise.all([json(page, '/sso/oauth2/token', exchange), json(page, '/sso/oauth2/token', exchange)]);
  const success = results.filter(result => result.code === 200);
  expect(success).toHaveLength(1);
  expect(record(success[0]?.data).client_id).toBe(clientId);
  expect((await json(page, '/sso/oauth2/token', exchange)).code).not.toBe(200);

  const authorized = await json(page, `/sso/oauth2/authorize?${query(base)}`);
  const redirect = record(authorized.data).redirectUri;
  if (typeof redirect !== 'string') throw new Error('Missing fresh authorization redirect');
  const expired = new URL(redirect).searchParams.get('code');
  expect(authorized.code).toBe(200);
  expect(expired).toMatch(/^[A-Za-z0-9_-]{43}$/);
  expect(expired).not.toBe(code);
  expect((await json(page, '/__test/advance-clock', {})).code).toBe(200);
  expect(await json(page, '/sso/oauth2/token', { ...exchange, code: expired })).toMatchObject({ code: 500, msg: '授权码已过期' });

  const rejected = await context.request.post(`${base}/sso/login`, {
    headers: { Origin: 'https://unknown.example.test' }, data: { username: 'sso-test-user', password: 'owned-sso-test-only' }
  });
  expect(rejected.status()).toBe(403);
  expect(rejected.headers()['access-control-allow-origin']).toBeUndefined();
  const foreignPage = await context.newPage();
  await foreignPage.goto(`${base.replace('localhost', '127.0.0.1')}/callback`);
  const blocked = await foreignPage.evaluate(async (target) => {
    try {
      await fetch(`${target}/sso/login`, { method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'sso-test-user', password: 'owned-sso-test-only' }) });
      return false;
    } catch { return true; }
  }, base);
  expect(blocked).toBe(true);
  await foreignPage.close();

  await json(page, '/__test/expire-session', {});
  expect((await json(page, '/sso/session')).code).not.toBe(200);
  await login(page, base);
  expect((await json(page, '/sso/session')).code).toBe(200);
  expect((await json(page, '/sso/logout', {})).code).toBe(200);
  expect((await context.cookies()).some(cookie => cookie.name === 'Sso-Token')).toBe(false);
  expect((await json(page, '/sso/session')).code).not.toBe(200);
});

test('explicit local/dev HTTP mode can login and clears the same cookie', async ({ page, context }) => {
  const base = origin('SSO_TEST_HTTP_ORIGIN');
  await login(page, base);
  expect((await context.cookies()).find(cookie => cookie.name === 'Sso-Token'))
    .toMatchObject({ domain: 'localhost', path: '/', secure: false, httpOnly: true, sameSite: 'Lax' });
  expect((await json(page, '/sso/session')).code).toBe(200);
  expect((await json(page, '/sso/logout', {})).code).toBe(200);
  expect((await context.cookies()).some(cookie => cookie.name === 'Sso-Token')).toBe(false);
});
