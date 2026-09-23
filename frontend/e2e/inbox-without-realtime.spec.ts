import { expect, test, type Page, type Route } from '@playwright/test';

const reply = (route: Route, data: unknown, code = 200) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify({ code, data }) });

interface InboxFixture {
  identity: 'A' | 'B';
  inboxCalls: number;
  heldInbox?: Route;
  heldRead?: Route;
  holdFirst: boolean;
  holdFirstRead: boolean;
  failFirst: boolean;
  empty: boolean;
  read: boolean;
  readCalls: number;
  pushRequests: string[];
}

const fixture = (changes: Partial<InboxFixture> = {}): InboxFixture => ({
  identity: 'A', inboxCalls: 0, holdFirst: false, holdFirstRead: false, failFirst: false,
  empty: false, read: false, readCalls: 0, pushRequests: [], ...changes
});

const message = (state: InboxFixture) => ({
  messageId: state.identity === 'A' ? '101' : '202',
  category: 'notice', title: `${state.identity} 的公告`,
  message: `${state.identity} 的正文`, content: `${state.identity} 的正文`,
  readTime: state.read ? '2026-09-23 10:00:00' : null
});
const inboxPage = (state: InboxFixture) => ({
  rows: state.empty ? [] : [{ ...message(state), content: undefined }],
  total: state.empty ? 0 : 1,
  unreadTotal: state.empty || state.read ? 0 : 1
});

async function install(page: Page, state: InboxFixture) {
  await page.route('**/prod-api/**', route => {
    const path = new URL(route.request().url()).pathname.replace('/prod-api', '');
    if (path === '/auth/client/context') return reply(route, { clientEnabled: true, registerEnabled: true, passwordPolicy: { minimumLength: 8, maximumLength: 30, requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'], allowedSpecialCharacters: '@$!%*?&' } });
    if (path === '/auth/code') return reply(route, { captchaEnabled: false });
    if (path === '/auth/login') return reply(route, { access_token: `owned-${state.identity}` });
    if (path === '/auth/logout') return reply(route, null);
    if (path === '/system/user/getInfo') return reply(route, { user: { userId: state.identity, userName: state.identity, nickName: state.identity, avatarUrl: '' }, roles: ['admin'], permissions: ['notify:inbox:read'] });
    if (path === '/system/menu/getRouters') return reply(route, []);
    if (path === '/notify/inbox') {
      state.inboxCalls++;
      if (state.holdFirst && state.inboxCalls === 1) { state.heldInbox = route; return; }
      if (state.failFirst && state.inboxCalls <= 2) return route.abort('failed');
      return reply(route, inboxPage(state));
    }
    if (/^\/notify\/inbox\/\d+$/.test(path)) return reply(route, message(state));
    if (/^\/notify\/inbox\/\d+\/read$/.test(path)) {
      state.readCalls++;
      if (state.holdFirstRead && state.readCalls === 1) { state.heldRead = route; return; }
      state.read = true;
      return reply(route, null);
    }
    if (path === '/notify/inbox/read-all') { state.read = true; return reply(route, null); }
    if (path.startsWith('/resource/message')) state.pushRequests.push(path);
    return reply(route, null);
  });
}

async function login(page: Page) {
  // 再次登录沿用 logout 后的 SPA 文档，让旧请求的回调仍可竞争。
  if (new URL(page.url()).pathname !== '/login') await page.goto('/login');
  const form = page.locator('.login-form');
  await expect(form.locator('input').first()).toBeEnabled();
  await form.locator('input').first().fill('owned-user');
  await form.locator('input[type="password"]').fill('OwnedPass!9');
  await form.locator('.submit-button').click();
  await expect(page.locator('.message-trigger')).toBeVisible();
}

test('T-34 message box distinguishes loading, failure, retry, notice category and read state without realtime', async ({ page }) => {
  const state = fixture({ holdFirst: true, failFirst: true });
  await install(page, state);
  await login(page);
  await expect.poll(() => Boolean(state.heldInbox)).toBe(true);
  await page.locator('.message-trigger').click();
  await expect(page.getByRole('status').filter({ hasText: '正在加载消息…' })).toBeVisible();
  await expect(page.getByText('暂无消息')).toHaveCount(0);
  // Popover 的 show 在进入动画结束后刷新；先等它与仍在途的查询合并，再注入失败。
  await expect(page.locator('.el-popover').filter({ hasText: '消息盒子' })).not.toHaveClass(/el-zoom-in-top-enter/);
  expect(state.inboxCalls).toBe(1);
  await state.heldInbox!.abort('failed');
  await expect(page.getByRole('alert').filter({ hasText: '消息加载失败，请重试' })).toBeVisible();
  await expect(page.getByText('暂无消息')).toHaveCount(0);
  await page.getByRole('button', { name: '重试' }).click();
  const row = page.locator('.content-box-item').filter({ hasText: 'A 的公告' });
  await expect(row).toBeVisible();
  await expect(row.getByText('A 的正文', { exact: true })).toHaveCount(1);
  await row.click();
  await expect(page.getByRole('dialog', { name: '通知详情' }).getByText('A 的正文', { exact: true })).toHaveCount(1);
  await expect(row.getByText('已读', { exact: true })).toBeVisible();
  expect(state.inboxCalls).toBeGreaterThanOrEqual(3);
  expect(state.pushRequests).toEqual([]);
});

test('T-34 old inbox failure cannot leak into the next login', async ({ page }) => {
  const state = fixture({ holdFirst: true });
  await install(page, state);
  await login(page);
  await expect.poll(() => Boolean(state.heldInbox)).toBe(true);
  await page.locator('.avatar-wrapper').click();
  await page.getByRole('menuitem', { name: /退出/ }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect(page).toHaveURL(/\/login/);
  state.identity = 'B';
  await login(page);
  await page.locator('.message-trigger').click();
  await expect(page.locator('.content-box-item').filter({ hasText: 'B 的公告' })).toBeVisible();
  await state.heldInbox!.abort('failed').catch(() => undefined);
  await expect(page.getByRole('alert').filter({ hasText: '消息加载失败，请重试' })).toHaveCount(0);
  await expect(page.getByText('A 的公告')).toHaveCount(0);
  expect(state.pushRequests).toEqual([]);
});

test('T-34 old read failure and detail are gone after unmount and next login', async ({ page }) => {
  const state = fixture({ holdFirstRead: true });
  await install(page, state);
  await login(page);
  await page.locator('.message-trigger').click();
  const oldRow = page.locator('.content-box-item').filter({ hasText: 'A 的公告' });
  await expect(oldRow).toBeVisible();
  await oldRow.click();
  const detail = page.getByRole('dialog', { name: '通知详情' });
  await expect(detail).toBeVisible();
  await expect.poll(() => Boolean(state.heldRead)).toBe(true);
  await detail.getByRole('button', { name: '关闭', exact: true }).click();
  await page.locator('.avatar-wrapper').click();
  await page.getByRole('menuitem', { name: /退出/ }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect(page).toHaveURL(/\/login/);
  state.identity = 'B';
  await login(page);
  await page.locator('.message-trigger').click();
  await expect(page.locator('.content-box-item').filter({ hasText: 'B 的公告' })).toBeVisible();
  await state.heldRead!.abort('failed').catch(() => undefined);
  await expect(page.getByRole('dialog', { name: '通知详情' })).toHaveCount(0);
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.getByText('A 的正文')).toHaveCount(0);
});

test('T-34 successful empty inbox is distinct from loading and failure', async ({ page }) => {
  const state = fixture({ empty: true });
  await install(page, state);
  await login(page);
  await page.locator('.message-trigger').click();
  await expect(page.getByText('暂无消息', { exact: true })).toBeVisible();
  await expect(page.getByRole('status')).toHaveCount(0);
  await expect(page.getByRole('alert').filter({ hasText: '消息加载失败，请重试' })).toHaveCount(0);
  expect(state.pushRequests).toEqual([]);
});

test('T-34 opening during the initial request refreshes a notice arriving after its snapshot', async ({ page }) => {
  const state = fixture({ holdFirst: true });
  await install(page, state);
  await login(page);
  await expect.poll(() => Boolean(state.heldInbox)).toBe(true);
  await page.locator('.message-trigger').click();
  await expect(page.getByRole('status').filter({ hasText: '正在加载消息…' })).toBeVisible();
  await expect(page.locator('.el-popover').filter({ hasText: '消息盒子' })).not.toHaveClass(/el-zoom-in-top-enter/);
  // 初始查询已获取空快照；打开时已存在的公告只能由后续查询得到。
  await reply(state.heldInbox!, { rows: [], total: 0, unreadTotal: 0 });
  await expect(page.locator('.content-box-item').filter({ hasText: 'A 的公告' })).toBeVisible();
  expect(state.inboxCalls).toBe(2);
  expect(state.pushRequests).toEqual([]);
});
