import { readFileSync } from 'node:fs';
import { expect, test, type Page, type Response } from '@playwright/test';

interface Manifest {
  schema_version: 1;
  run_id: string;
  v1: { noticeId: string; messageId: string; title: string; content: string; path: string };
  legacy: { messageId: string; title: string; content: string; noticeId: string };
  bControl: { messageId: string; title: string; content: string };
  absentId: string;
}

function required(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error('Required isolated T40 variable is missing: ' + name);
  return value;
}

function manifest(): Manifest {
  const value: unknown = JSON.parse(readFileSync(required('T40_SEED_MANIFEST'), 'utf8'));
  if (!value || typeof value !== 'object') throw new Error('T40 manifest is invalid');
  const parsed = value as Partial<Manifest>;
  if (parsed.schema_version !== 1 || parsed.run_id !== required('T40_RUN_ID') ||
      !parsed.v1?.messageId || !parsed.legacy?.messageId || !parsed.bControl?.messageId ||
      parsed.v1.path !== `/notify/inbox?messageId=${parsed.v1.messageId}` || !parsed.absentId) {
    throw new Error('T40 manifest does not match real publication proof');
  }
  return parsed as Manifest;
}

async function login(page: Page, origin: string, username: string, password: string) {
  if (new URL(page.url()).pathname !== '/login') await page.goto(new URL('/login', origin).toString());
  const form = page.locator('.login-form');
  await expect(form.locator('input').first()).toBeEnabled();
  await form.locator('input').first().fill(username);
  await form.locator('input[type="password"]').fill(password);
  await form.locator('.submit-button').click();
  await expect(page.locator('.message-trigger')).toBeVisible({ timeout: 30_000 });
}

async function samePageQuery(page: Page, messageId?: string) {
  await page.evaluate(id => {
    const url = new URL(location.href);
    if (id) url.searchParams.set('messageId', id);
    else url.searchParams.delete('messageId');
    history.pushState(history.state, '', url);
    dispatchEvent(new PopStateEvent('popstate', { state: history.state }));
  }, messageId);
}

function detail(page: Page) {
  return page.getByRole('dialog', { name: '通知详情' });
}

async function renderedAfter(page: Page, response: Response) {
  await response.finished();
  await page.evaluate(() => new Promise<void>(resolve =>
    requestAnimationFrame(() => requestAnimationFrame(() => resolve()))));
}

test('T-40 real published V1 survives retract in A personal off-page inbox', async ({ browser }) => {
  test.setTimeout(120_000);
  const owned = manifest();
  const origin = required('T40_ADMIN_ORIGIN');
  const context = await browser.newContext();
  try {
    const page = await context.newPage();
    const managementGets: string[] = [];
    page.on('request', request => {
      const path = new URL(request.url()).pathname;
      if (request.method() === 'GET' && /^\/(?:dev-api\/)?notify\/notice\/\d+$/.test(path)) managementGets.push('notice-get');
    });
    await login(page, origin, required('T40_A_USERNAME'), required('T40_A_PASSWORD'));
    await page.goto(new URL(owned.v1.path, origin).toString());
    const v1Detail = detail(page);
    await expect(v1Detail).toBeVisible({ timeout: 30_000 });
    await expect(v1Detail.locator('.el-descriptions__content').last()).toHaveText(owned.v1.content);
    await expect(v1Detail.getByText(owned.v1.title, { exact: true })).toBeVisible();
    await v1Detail.getByRole('button', { name: '关闭', exact: true }).click();
    await expect(v1Detail).toBeHidden();
    await expect(page.locator('.el-table__body-wrapper tbody tr')).toHaveCount(20);
    await expect(page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: owned.v1.title })).toHaveCount(0);

    // 同页先悬挂真实 V1 详情，再切到另一条本人消息；只延迟真实后端字节。
    await samePageQuery(page);
    let release = () => {};
    const held = new Promise<void>(resolve => { release = resolve; });
    let forwarded = (_completed: boolean) => {};
    const finished = new Promise<boolean>(resolve => { forwarded = resolve; });
    let fetched = 0;
    await page.route(`**/notify/inbox/${owned.v1.messageId}`, async route => {
      try {
        const response = await route.fetch();
        ++fetched;
        await held;
        await route.fulfill({ response });
        forwarded(true);
      } catch (error) {
        forwarded(false);
        throw error;
      }
    }, { times: 1 });
    try {
      await samePageQuery(page, owned.v1.messageId);
      await expect.poll(() => fetched).toBe(1);
      await samePageQuery(page, owned.legacy.messageId);
      await expect(detail(page).locator('.el-descriptions__content').last()).toHaveText(owned.legacy.content);
      const staleResponse = page.waitForResponse(response => response.request().method() === 'GET' &&
        new URL(response.url()).pathname.endsWith('/notify/inbox/' + owned.v1.messageId));
      release();
      expect(await finished).toBe(true);
      await renderedAfter(page, await staleResponse);
      await expect(detail(page).locator('.el-descriptions__content').last()).toHaveText(owned.legacy.content);
    } finally {
      release();
      await page.unroute(`**/notify/inbox/${owned.v1.messageId}`);
    }
    await detail(page).getByRole('button', { name: '关闭', exact: true }).click();
    await expect(detail(page)).toBeHidden();
    await samePageQuery(page);

    await page.locator('.message-trigger').click();
    const popover = page.locator('.el-popover').filter({ hasText: '消息盒子' });
    const legacyRow = popover.locator('.content-box-item').filter({ hasText: owned.legacy.title });
    await expect(legacyRow).toBeVisible();
    await legacyRow.click();
    await expect(detail(page).locator('.el-descriptions__content').last()).toHaveText(owned.legacy.content);
    const topDialog = await detail(page).elementHandle();
    expect(topDialog).not.toBeNull();
    const legacyResponse = page.waitForResponse(response => response.request().method() === 'GET' &&
      new URL(response.url()).pathname.endsWith('/notify/inbox/' + owned.legacy.messageId));
    await detail(page).getByRole('button', { name: '查看业务' }).click();
    await topDialog!.waitForElementState('hidden');
    await expect(page).toHaveURL(new RegExp(`/notify/inbox\\?messageId=${owned.legacy.messageId}$`));
    expect((await legacyResponse).status()).toBe(200);
    await expect(detail(page).locator('.el-descriptions__content').last()).toHaveText(owned.legacy.content);
    expect(managementGets).toEqual([]);

    // SPA 注销期间保留旧页面实例；迟到的旧详情不能进入下一个身份。
    await samePageQuery(page);
    await expect(detail(page)).toBeHidden();
    let unblock = () => {};
    const pending = new Promise<void>(resolve => { unblock = resolve; });
    let sessionForwarded = (_completed: boolean) => {};
    const sessionFinished = new Promise<boolean>(resolve => { sessionForwarded = resolve; });
    let oldFetched = 0;
    await page.route(`**/notify/inbox/${owned.v1.messageId}`, async route => {
      try {
        const response = await route.fetch();
        ++oldFetched;
        await pending;
        await route.fulfill({ response });
        sessionForwarded(true);
      } catch (error) {
        sessionForwarded(false);
        throw error;
      }
    }, { times: 1 });
    try {
      await samePageQuery(page, owned.v1.messageId);
      await expect.poll(() => oldFetched).toBe(1);
      await page.locator('.avatar-wrapper').click();
      await page.getByText('退出登录', { exact: true }).click();
      await page.getByRole('button', { name: '确定', exact: true }).click();
      await expect(page).toHaveURL(/\/login(?:\?|$)/);
      await login(page, origin, required('T40_B_USERNAME'), required('T40_B_PASSWORD'));
      const staleSessionResponse = page.waitForResponse(response => response.request().method() === 'GET' &&
        new URL(response.url()).pathname.endsWith('/notify/inbox/' + owned.v1.messageId));
      unblock();
      expect(await sessionFinished).toBe(true);
      await renderedAfter(page, await staleSessionResponse);
      await expect(page.getByText(owned.v1.content, { exact: true })).toHaveCount(0);
    } finally {
      unblock();
      await page.unroute(`**/notify/inbox/${owned.v1.messageId}`);
    }
  } finally {
    await context.close();
  }
});

test('T-40 real B foreign and absent inbox details fail with the same owner response', async ({ browser }) => {
  test.setTimeout(90_000);
  const owned = manifest();
  const origin = required('T40_ADMIN_ORIGIN');
  const context = await browser.newContext();
  try {
    const page = await context.newPage();
    const managementGets: string[] = [];
    const detailGets: string[] = [];
    page.on('request', request => {
      const path = new URL(request.url()).pathname;
      if (request.method() !== 'GET') return;
      if (/^\/(?:dev-api\/)?notify\/notice\/\d+$/.test(path)) managementGets.push('notice-get');
      if (/^\/(?:dev-api\/)?notify\/inbox\/\d+$/.test(path)) detailGets.push(path);
    });
    await login(page, origin, required('T40_B_USERNAME'), required('T40_B_PASSWORD'));
    await page.goto(new URL(`/notify/inbox?messageId=${owned.bControl.messageId}`, origin).toString());
    await expect(detail(page).locator('.el-descriptions__content').last()).toHaveText(owned.bControl.content);
    await detail(page).getByRole('button', { name: '关闭', exact: true }).click();
    await expect(detail(page)).toBeHidden();

    const failures: Array<{ status: number; code: unknown; msg: unknown }> = [];
    for (const id of [owned.v1.messageId, owned.absentId]) {
      const responsePromise = page.waitForResponse(response =>
        response.request().method() === 'GET' &&
        new URL(response.url()).pathname.endsWith('/notify/inbox/' + id));
      await samePageQuery(page, id);
      const response = await responsePromise;
      const body: unknown = await response.json();
      const result = body && typeof body === 'object' ? body as { code?: unknown; msg?: unknown } : {};
      failures.push({ status: response.status(), code: result.code, msg: result.msg });
      await expect(detail(page).getByRole('alert')).toContainText('消息不存在');
      await expect(page.getByText(owned.v1.content, { exact: true })).toHaveCount(0);
    }
    expect(failures[0]).toEqual(failures[1]);
    const beforeMalformed = detailGets.length;
    await samePageQuery(page, 'not-an-id');
    await expect(detail(page).getByRole('alert')).toContainText('消息编号无效');
    expect(detailGets).toHaveLength(beforeMalformed);
    expect(managementGets).toEqual([]);
  } finally {
    await context.close();
  }
});
