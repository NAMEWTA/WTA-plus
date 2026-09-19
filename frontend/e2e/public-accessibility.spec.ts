import { expect, test, type Locator, type Page, type Route } from '@playwright/test';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const policy = { minimumLength: 8, maximumLength: 30, requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'], allowedSpecialCharacters: '@$!%*?&' };
const image = 'R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==';
const json = (route: Route, value: unknown) => route.fulfill({ json: value });
const origin = (app: 'home' | 'sso') => {
  const value = process.env[`A11Y_${app.toUpperCase()}_ORIGIN`];
  if (!value || new URL(value).protocol !== 'https:' || new URL(value).hostname !== '127.0.0.1') throw new Error('Owned HTTPS accessibility fixture required');
  return value;
};
const authorization = '/authorize?' + new URLSearchParams({ client_id: 'owned-client', redirect_uri: 'https://127.0.0.1/a11y-authorized', state: 'owned-state', code_challenge: 'owned-challenge', code_challenge_method: 'S256' });
function fixture() {
  return { contextFailure: false, sessionFailure: false, loginCalls: 0, registerCalls: 0, authorizeCalls: 0 };
}
async function install(page: Page, state = fixture()) {
  await page.route('**/prod-api/**', route => {
    const path = new URL(route.request().url()).pathname.slice('/prod-api'.length);
    if (path === '/auth/client/context') return state.contextFailure ? route.abort('failed') : json(route, { code: 200, data: {
      clientEnabled: true, registerEnabled: true, passwordPolicy: policy, authMode: 'both', ssoEnabled: true, ssoAuthorizeUrl: origin('sso') + '/authorize'
    } });
    if (path === '/auth/code') return json(route, { code: 200, data: { captchaEnabled: true, img: image, uuid: 'owned-challenge' } });
    if (path === '/auth/login') return json(route, ++state.loginCalls === 1 ? { code: 500, msg: '登录失败，请重试' } : { code: 200, data: { access_token: 'owned-fixture-token' } });
    if (path === '/auth/register') return json(route, ++state.registerCalls === 1 ? { code: 500, msg: '注册失败，请重试' } : { code: 200 });
    if (path === '/system/user/getInfo') return json(route, { code: 200, data: { user: { userId: '7', userName: 'owned-user', nickName: 'Owned user' }, roles: [], permissions: [] } });
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: [{ name: 'ProfileCenter', path: '/profile', component: 'profile/center/index', meta: { title: '档案中心' } }] });
    return json(route, { code: 200, data: null });
  });
  await page.route('**/sso/session', route => state.sessionFailure ? route.abort('failed') : json(route, { code: 401, msg: '未登录' }));
  await page.route('**/sso/login', route => json(route, ++state.loginCalls === 1 ? { code: 500 } : { code: 200 }));
  await page.route('**/sso/oauth2/authorize?*', route => {
    state.authorizeCalls++;
    return json(route, { code: 200, data: { loginRequired: false, redirectUri: origin('sso') + '/a11y-authorized' } });
  });
  await page.route('**/a11y-authorized', route => route.fulfill({ contentType: 'text/html; charset=utf-8', body: '<p role="status">已到达授权目标</p>' }));
  return state;
}

const beforeFocus = new WeakMap<Locator, string>();
async function focusStyle(target: Locator) {
  return target.evaluate(node => {
    const style = getComputedStyle(node.closest('.el-input__wrapper') ?? node);
    return [style.outline, style.boxShadow, style.borderColor, style.backgroundColor].join('|');
  });
}

async function tabTo(page: Page, target: Locator) {
  await expect(target).toBeVisible();
  beforeFocus.set(target, await focusStyle(target));
  for (let index = 0; index < 40; index++) {
    if (await target.evaluate(node => node === document.activeElement)) return;
    await page.keyboard.press('Tab');
  }
  await expect(target).toBeFocused();
}
async function typeWithKeyboard(page: Page, target: Locator, text: string) {
  await tabTo(page, target); await page.keyboard.press('ControlOrMeta+A'); await page.keyboard.insertText(text);
}
async function visibleFocus(target: Locator) {
  expect(await target.evaluate(node => node.matches(':focus-visible'))).toBe(true);
  const previous = beforeFocus.get(target);
  expect(previous).toBeDefined();
  expect(await focusStyle(target)).not.toBe(previous);
}
async function contrast(target: Locator) {
  return target.evaluate(node => {
    const rgb = (value: string) => (value.match(/[\d.]+/g) ?? []).map(Number);
    const style = getComputedStyle(node); const foreground = rgb(style.color);
    let current: Element | null = node; let background = [255, 255, 255];
    while (current) {
      const color = rgb(getComputedStyle(current).backgroundColor);
      if (color.length === 3 || color[3] === 1) { background = color; break; }
      current = current.parentElement;
    }
    const luminance = (color: number[]) => color.slice(0, 3).map(channel => {
      const value = channel / 255; return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
    }).reduce((sum, value, index) => sum + value * [0.2126, 0.7152, 0.0722][index], 0);
    const front = luminance(foreground); const back = luminance(background);
    return { color: style.color, background, ratio: (Math.max(front, back) + 0.05) / (Math.min(front, back) + 0.05) };
  });
}

const pages = [
  { app: 'home', path: '/login', selector: '.identity-login', text: ['.identity-login__social-label', '.identity-login__status', '.identity-login__submit', 'footer'] },
  { app: 'home', path: '/register', selector: '.register-page', text: ['.register-intro p', '.status', '.submit', 'footer'] },
  { app: 'home', path: '/sso/callback', selector: '.sso-callback', text: ['.sso-callback p', 'footer'] },
  { app: 'sso', path: authorization, selector: '.sso-card', text: ['.hint', '.status', 'button[type=submit]'] }
] as const;

for (const entry of pages) for (const width of [320, 768, 1440]) for (const zoom of [1, 2]) {
  test.describe(`public viewport ${width} density ${zoom}`, () => {
    test.use({ viewport: { width: width / zoom, height: 900 / zoom }, deviceScaleFactor: zoom });
  test(`${entry.app} ${entry.path.split('?')[0]} ${width}px ${zoom * 100}% reflow and rendered contrast`, async ({ page }) => {
    await install(page); await page.goto(origin(entry.app) + entry.path);
    const scope = page.locator(entry.selector); await expect(scope).toBeVisible();
    if (entry.path === '/login' || entry.path === '/register') await expect(scope.getByLabel('用户名', { exact: true })).toBeEnabled();
    if (entry.app === 'sso') await expect(page.getByLabel('密码', { exact: true })).toBeVisible();
    if (entry.path === '/sso/callback') await expect(scope.getByRole('alert')).toBeVisible();
    if (entry.path === '/login' || entry.path === '/register') await expect(scope.locator('button[type=submit]')).toBeEnabled();
    // Sample settled rendered colors, after disabled-to-ready CSS transitions finish.
    await page.evaluate(async () => {
      for (let round = 0; round < 4; round++) {
        await new Promise<void>(resolve => requestAnimationFrame(() => resolve()));
        const animations = document.getAnimations().filter(animation => animation.playState === 'running' && Number.isFinite(animation.effect?.getTiming().iterations ?? 1));
        if (!animations.length) return;
        for (const animation of animations) await animation.finished.catch(() => undefined);
      }
      throw new Error('Page colors did not settle');
    });
    const dimensions = await page.evaluate(() => ({ viewport: innerWidth, scrollWidth: document.documentElement.scrollWidth, dpr: devicePixelRatio }));
    const contrasts = [];
    for (const selector of entry.text) contrasts.push({ selector, ...await contrast(page.locator(selector)) });
    const directory = process.env.A11Y_EVIDENCE_DIR;
    if (!directory) throw new Error('Owned artifact directory required');
    await mkdir(directory, { recursive: true });
    const name = `${entry.app}-${entry.path.split('?')[0].slice(1).replaceAll('/', '-')}-${width}-${zoom}x`;
    const screenshot = resolve(directory, name + '.png');
    await page.screenshot({ path: screenshot, fullPage: true, scale: 'device' });
    const png = await readFile(screenshot);
    expect(png.readUInt32BE(16)).toBe(width);
    await writeFile(resolve(directory, name + '.json'), JSON.stringify({ dimensions, contrasts, zoomMethod: 'Playwright context CSS viewport / zoom, DPR=zoom; device-pixel PNG, equivalent desktop reflow, not CSS zoom' }, null, 2));
    expect.soft(dimensions.viewport).toBe(width / zoom);
    expect.soft(dimensions.dpr).toBe(zoom);
    expect.soft(dimensions.scrollWidth).toBeLessThanOrEqual(dimensions.viewport + 1);
    for (const row of contrasts) expect.soft(row.ratio, `${row.selector} rendered text contrast`).toBeGreaterThanOrEqual(4.5);
    if (entry.path === '/login') expect(await scope.evaluate(node => getComputedStyle(node).getPropertyValue('--client-surface').trim())).not.toBe('');
  });
  });
}

test('Home login preparation failure has a keyboard retry with visible focus', async ({ page }) => {
  const state = fixture(); state.contextFailure = true; await install(page, state);
  await page.goto(origin('home') + '/login');
  await expect(page.locator('.identity-login__error')).toBeVisible();
  const retry = page.getByRole('button', { name: '重新检查登录入口', exact: true });
  await tabTo(page, retry); await visibleFocus(retry);
  state.contextFailure = false; await page.keyboard.press('Enter');
  await expect(page.getByLabel('用户名', { exact: true })).toBeEnabled();
  await expect(page.getByRole('navigation')).toBeVisible();
});

test('Home login can fail and complete using only keyboard input', async ({ page }) => {
  const state = await install(page); await page.goto(origin('home') + '/login');
  for (const attempt of [1, 2]) {
    const username = page.getByLabel('用户名', { exact: true }); await expect(username).toBeEnabled();
    await typeWithKeyboard(page, username, 'owned-user'); await visibleFocus(username);
    await typeWithKeyboard(page, page.getByLabel('密码', { exact: true }), 'OwnedPass!9');
    await typeWithKeyboard(page, page.getByLabel('验证码', { exact: true }), 'owned');
    const submit = page.getByRole('button', { name: '登录', exact: true }); await tabTo(page, submit); await visibleFocus(submit); await page.keyboard.press('Enter');
    await expect.poll(() => state.loginCalls).toBe(attempt);
    if (attempt === 1) await expect(page.locator('.identity-login__error')).toHaveAttribute('role', 'alert');
  }
  await expect(page).toHaveURL(origin('home') + '/profile');
});

test('Home registration retains labels and alert while keyboard retry completes', async ({ page }) => {
  const state = await install(page); await page.goto(origin('home') + '/register');
  for (const attempt of [1, 2]) {
    const username = page.getByLabel('用户名', { exact: true }); await expect(username).toBeEnabled();
    await typeWithKeyboard(page, username, 'owned-user'); await visibleFocus(username);
    await typeWithKeyboard(page, page.getByLabel('手机号码', { exact: true }), '13800138000');
    await typeWithKeyboard(page, page.getByLabel('密码', { exact: true }), 'OwnedPass!9');
    await typeWithKeyboard(page, page.getByLabel('确认密码', { exact: true }), 'OwnedPass!9');
    await typeWithKeyboard(page, page.getByLabel('验证码', { exact: true }), 'owned');
    const submit = page.getByRole('button', { name: '注册', exact: true }); await tabTo(page, submit); await visibleFocus(submit); await page.keyboard.press('Enter');
    await expect.poll(() => state.registerCalls).toBe(attempt);
    if (attempt === 1) await expect(page.locator('.register-form').getByRole('alert')).toContainText('注册失败');
  }
  await expect(page).toHaveURL(origin('home') + '/login');
});

test('SSO network retry and failed password status remain accessible to keyboard and AX tree', async ({ page }) => {
  const state = fixture(); state.sessionFailure = true; await install(page, state);
  await page.goto(origin('sso') + authorization);
  const status = page.getByRole('status'); await expect(status).toContainText('无法完成授权');
  await expect(status).toHaveAttribute('aria-live', 'polite');
  const retry = page.getByRole('button', { name: '重试授权', exact: true }); await tabTo(page, retry); await visibleFocus(retry);
  state.sessionFailure = false; await page.keyboard.press('Enter');
  for (const attempt of [1, 2]) {
    await expect(page.getByLabel('用户名', { exact: true })).toBeVisible();
    await typeWithKeyboard(page, page.getByLabel('用户名', { exact: true }), 'owned-user');
    await typeWithKeyboard(page, page.getByLabel('密码', { exact: true }), 'OwnedPass!9');
    const submit = page.getByRole('button', { name: '登录并继续' }); await tabTo(page, submit); await visibleFocus(submit); await page.keyboard.press('Enter');
    await expect.poll(() => state.loginCalls).toBe(attempt);
    if (attempt === 1) {
      await expect(status).toContainText('登录失败');
      const cdp = await page.context().newCDPSession(page); const tree = await cdp.send('Accessibility.getFullAXTree'); await cdp.detach();
      expect(tree.nodes.some(node => node.role?.value === 'status' && node.properties?.some(property => property.name === 'live' && property.value.value === 'polite'))).toBe(true);
    }
  }
  await expect(page.getByRole('status')).toHaveText('已到达授权目标'); expect(state.authorizeCalls).toBe(1);
});

test('Home callback preserves live errors and keyboard restart/back navigation', async ({ page }) => {
  await install(page); await page.goto(origin('home') + '/sso/callback');
  await expect(page.getByRole('alert')).toBeVisible();
  const back = page.getByRole('link', { name: '返回登录页' }); await tabTo(page, back); await visibleFocus(back); await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/login\?/);
  await page.goto(origin('home') + '/sso/callback');
  const retry = page.getByRole('button', { name: '重新授权', exact: true }); await tabTo(page, retry); await visibleFocus(retry); await page.keyboard.press('Enter');
  await expect(page).toHaveURL(new RegExp(origin('sso') + '/authorize\\?'));
});
