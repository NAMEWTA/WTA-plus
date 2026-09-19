import { expect, test, type Page, type Route } from '@playwright/test';

const policy = { minimumLength: 8, maximumLength: 30, requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'], allowedSpecialCharacters: '@$!%*?&' };
const image = 'R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==';
function origin(app: 'home' | 'admin' = 'home') {
  const value = process.env[`REGISTRATION_${app.toUpperCase()}_ORIGIN`];
  if (!value || new URL(value).protocol !== 'https:' || new URL(value).hostname !== '127.0.0.1') throw new Error('Owned HTTPS registration fixture required');
  return value;
}
function fixture() {
  return { enabled: true, contextFailure: false, codeFailure: false, contextRequests: 0, holdCode: false, holdRegistration: false, failFirst: false, failFirstNetwork: false, codeRequests: 0,
    held: [] as Route[], pendingRegistration: undefined as Route | undefined, writes: [] as Array<{ uuid: unknown; code: unknown; username: unknown }> };
}
type State = ReturnType<typeof fixture>;
const json = (route: Route, value: unknown) => route.fulfill({ contentType: 'application/json', body: JSON.stringify(value) });
const code = (route: Route, uuid: string) => json(route, { code: 200, data: { captchaEnabled: true, img: image, uuid } });
async function install(page: Page, state: State) {
  await page.route('**/prod-api/**', route => {
    const path = new URL(route.request().url()).pathname.replace('/prod-api', '');
    if (path === '/auth/client/context') {
      state.contextRequests++;
      return state.contextFailure ? route.abort('failed') : json(route, { code: 200, data: { clientEnabled: true, registerEnabled: state.enabled, passwordPolicy: policy } });
    }
    if (path === '/auth/code') {
      const sequence = ++state.codeRequests;
      if (state.codeFailure) return route.abort('internetdisconnected');
      if (state.holdCode) { state.held.push(route); return; }
      return code(route, `challenge-${sequence}`);
    }
    if (path === '/auth/register') {
      const body = route.request().postDataJSON() as Record<string, unknown>;
      state.writes.push({ uuid: body.uuid, code: body.code, username: body.username });
      if (state.holdRegistration) { state.pendingRegistration = route; return; }
      if (state.failFirstNetwork && state.writes.length === 1) return route.abort('internetdisconnected');
      return json(route, { code: state.failFirst && state.writes.length === 1 ? 500 : 200, msg: '验证码已过期' });
    }
    return json(route, { code: 200, data: [] });
  });
}
async function fill(page: Page, value: string) {
  const form = page.locator('.register-form');
  await form.getByLabel('用户名', { exact: true }).fill('owned-register');
  await form.getByLabel('密码', { exact: true }).fill('OwnedPass!9');
  await form.getByLabel('确认密码', { exact: true }).fill('OwnedPass!9');
  await form.getByLabel('验证码', { exact: true }).fill(value);
}

test('T-13 disabled registration hides public entries and denies direct page submission', async ({ page }) => {
  const state = fixture(); state.enabled = false; await install(page, state);
  await page.goto(origin());
  await expect(page.locator('a[href="/register"]')).toHaveCount(0);
  await page.goto(origin() + '/register');
  await expect(page.locator('.register-form').getByRole('alert')).toContainText('未开放注册');
  await expect(page.getByRole('button', { name: '注册', exact: true })).toBeDisabled();
  expect(state.writes).toEqual([]);
});

test('T-13 failed context hides entries and direct registration can retry without reload', async ({ page }) => {
  const state = fixture(); state.contextFailure = true; await install(page, state);
  await page.goto(origin());
  await expect(page.locator('a[href="/register"]')).toHaveCount(0);
  await page.goto(origin() + '/register');
  await expect(page.getByRole('button', { name: '注册', exact: true })).toBeDisabled();
  state.contextFailure = false;
  await page.getByRole('button', { name: '重新检查注册入口', exact: true }).click();
  await expect(page.getByRole('button', { name: '注册', exact: true })).toBeEnabled();
});

for (const app of ['home', 'admin'] as const) {
  test(`T-13 ${app} registration remains disabled until the captcha response is applied`, async ({ page }) => {
    const state = fixture(); state.holdCode = true; await install(page, state);
    await page.goto(origin(app) + '/register');
    await expect.poll(() => state.held.length).toBe(1);
    const submit = page.locator('.register-form').getByRole('button', { name: app === 'admin' ? '注 册' : '注册', exact: true });
    await expect(submit).toBeDisabled();
    await code(state.held[0]!, 'ready');
    await expect(submit).toBeEnabled();
    expect(state.writes).toEqual([]);
  });
}

test('T-13 Admin login remains disabled until the captcha response is applied', async ({ page }) => {
  const state = fixture(); state.holdCode = true; await install(page, state);
  await page.goto(origin('admin') + '/login');
  await expect.poll(() => state.held.length).toBe(1);
  await expect(page.locator('.submit-button')).toBeDisabled();
  await code(state.held[0]!, 'ready');
  await expect(page.locator('.submit-button')).toBeEnabled();
});

for (const failure of ['rejected', 'network'] as const) {
  test(`T-13 keyboard refresh keeps the latest uuid and ${failure} registration can recover`, async ({ page }) => {
    const state = fixture(); state.failFirst = failure === 'rejected'; state.failFirstNetwork = failure === 'network'; await install(page, state);
    await page.goto(origin() + '/register');
    await expect(page.getByRole('button', { name: '注册', exact: true })).toBeEnabled();
    state.holdCode = true;
    const refresh = page.getByRole('button', { name: '刷新验证码', exact: true });
    await refresh.focus(); await page.keyboard.press('Enter');
    await expect.poll(() => state.held.length).toBe(1);
    await page.keyboard.press('Space');
    await expect.poll(() => state.held.length).toBe(2);
    await code(state.held[1]!, 'newer');
    await expect(page.getByRole('button', { name: '注册', exact: true })).toBeEnabled();
    await fill(page, 'fresh-code');
    await code(state.held[0]!, 'older');
    state.holdCode = false;
    await page.locator('.register-form').getByLabel('验证码', { exact: true }).press('Enter');
    await expect.poll(() => state.writes.length).toBe(1);
    expect(state.writes[0]).toEqual({ username: 'owned-register', uuid: 'newer', code: 'fresh-code' });
    await expect(page.locator('.register-form').getByRole('alert')).toBeVisible();
    if (failure === 'rejected') await expect(page.locator('.register-form').getByRole('alert')).toContainText('验证码已过期');
    await expect(page.locator('.register-form').getByLabel('密码', { exact: true })).toHaveValue('');
    await expect(page.locator('.register-form').getByLabel('确认密码', { exact: true })).toHaveValue('');
    await expect(page.locator('.register-form').getByLabel('验证码', { exact: true })).toHaveValue('');
    await expect(page.locator('.register-form').getByLabel('用户名', { exact: true })).toHaveValue('owned-register');
    await expect(page.getByRole('button', { name: '注册', exact: true })).toBeEnabled();
    await fill(page, 'retry-code');
    await page.locator('.register-form').getByLabel('验证码', { exact: true }).press('Enter');
    await expect(page).toHaveURL(origin() + '/login');
    expect(state.writes).toHaveLength(2);
    expect(state.writes[1]?.uuid).not.toBe('newer');
    expect(state.writes[1]?.uuid).not.toBe('older');
  });
}

test('T-13 captcha network failure stays disabled and keyboard refresh recovers', async ({ page }) => {
  const state = fixture(); await install(page, state);
  await page.goto(origin() + '/register');
  const submit = page.getByRole('button', { name: '注册', exact: true });
  await expect(submit).toBeEnabled();
  state.codeFailure = true;
  await page.getByRole('button', { name: '刷新验证码', exact: true }).click();
  await expect(page.locator('.register-form').getByRole('alert')).toBeVisible();
  await expect(submit).toBeDisabled();
  state.codeFailure = false;
  await page.getByRole('button', { name: '刷新验证码', exact: true }).focus();
  await page.keyboard.press('Enter');
  await expect(submit).toBeEnabled();
  expect(state.contextRequests).toBe(1);
  await fill(page, 'recovered'); await submit.click();
  await expect(page).toHaveURL(origin() + '/login');
  expect(state.writes).toHaveLength(1);
});

test('T-13 registration rejection rechecks a Client that has closed registration', async ({ page }) => {
  const state = fixture(); state.failFirst = true; await install(page, state);
  await page.goto(origin() + '/register');
  await expect(page.getByRole('button', { name: '注册', exact: true })).toBeEnabled();
  await fill(page, 'consumed');
  state.enabled = false;
  await page.getByRole('button', { name: '注册', exact: true }).click();
  await expect(page.locator('.register-form').getByRole('alert')).toContainText('未开放注册');
  await expect(page.getByRole('button', { name: '注册', exact: true })).toBeDisabled();
  await expect(page.locator('a[href="/register"]')).toHaveCount(0);
  expect(state.writes).toHaveLength(1);
});

test('T-13 repeated submit sends once and cancelling ignores its late completion', async ({ page }) => {
  const state = fixture(); state.holdRegistration = true; await install(page, state);
  await page.goto(origin() + '/register');
  await expect(page.getByRole('button', { name: '注册', exact: true })).toBeEnabled();
  await fill(page, 'held');
  await page.getByRole('button', { name: '注册', exact: true }).click();
  await expect.poll(() => Boolean(state.pendingRegistration)).toBe(true);
  await page.locator('.register-form').evaluate(form => {
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  });
  await page.getByRole('button', { name: '取消注册，返回登录', exact: true }).click();
  await expect(page).toHaveURL(origin() + '/login');
  await page.locator('.brand').click();
  await expect(page).toHaveURL(origin() + '/');
  const response = page.waitForResponse(value => value.url().endsWith('/auth/register'));
  await json(state.pendingRegistration!, { code: 200 });
  await (await response).finished();
  await expect(page).toHaveURL(origin() + '/');
  expect(state.writes).toHaveLength(1);
});
