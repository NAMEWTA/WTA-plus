import { readFileSync } from 'node:fs';
import { expect, test, type Page, type Request, type Response } from '@playwright/test';

const stepTimeout = 15_000;

async function bounded<T>(promise: Promise<T>, label: string): Promise<T> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  try {
    return await Promise.race([promise, new Promise<never>((_, reject) => {
      timer = setTimeout(() => reject(new Error(`${label} exceeded 15 seconds`)), stepTimeout);
    })]);
  } finally {
    if (timer) clearTimeout(timer);
  }
}

async function cleanupPreserving(primary: unknown, cleanup: () => Promise<void>, label: string) {
  try {
    await bounded(cleanup(), label);
  } catch (error) {
    if (primary !== undefined) throw new AggregateError([primary, error], `${label} failed after primary error`);
    throw error;
  }
}

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
  await expect(page.locator('.message-trigger')).toBeVisible({ timeout: stepTimeout });
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

async function painted(page: Page) {
  await bounded(page.evaluate(() => new Promise<void>(resolve =>
    requestAnimationFrame(() => requestAnimationFrame(() => resolve())))), 'browser paint');
}

async function renderedAfter(page: Page, response: Response) {
  expect(await bounded(response.finished(), 'page response completion')).toBeNull();
  await painted(page);
}

test('T-40 real published V1 survives retract in A personal off-page inbox', async ({ browser }) => {
  test.setTimeout(120_000);
  const owned = manifest();
  const origin = required('T40_ADMIN_ORIGIN');
  const context = await browser.newContext();
  let testFailure: unknown;
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
    await expect(v1Detail).toBeVisible({ timeout: stepTimeout });
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
    let samePageRequest: Request | undefined;
    const samePagePattern = `**/notify/inbox/${owned.v1.messageId}`;
    await page.route(samePagePattern, async route => {
      samePageRequest = route.request();
      try {
        const response = await route.fetch();
        expect(response.status()).toBe(200);
        ++fetched;
        await held;
        await route.fulfill({ response });
        forwarded(true);
      } catch (error) {
        forwarded(false);
        throw error;
      }
    }, { times: 1 });
    let samePageFailure: unknown;
    try {
      await samePageQuery(page, owned.v1.messageId);
      await expect.poll(() => fetched, { timeout: stepTimeout }).toBe(1);
      expect(samePageRequest).toBeDefined();
      await samePageQuery(page, owned.legacy.messageId);
      await expect(detail(page).locator('.el-descriptions__content').last()).toHaveText(owned.legacy.content);
      const staleResponse = page.waitForResponse(response => response.request() === samePageRequest,
        { timeout: stepTimeout });
      release();
      const [routeCompleted, response] = await Promise.all([
        bounded(finished, 'same-page route completion'), staleResponse,
      ]);
      expect(routeCompleted).toBe(true);
      await renderedAfter(page, response);
      await expect(detail(page).locator('.el-descriptions__content').last()).toHaveText(owned.legacy.content);
    } catch (error) {
      samePageFailure = error;
      throw error;
    } finally {
      release();
      await cleanupPreserving(samePageFailure, () => page.unroute(samePagePattern), 'same-page unroute');
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
    type SessionRouteOutcome = 'fulfilled' | 'failed';
    let sessionForwarded = (_outcome: SessionRouteOutcome) => {};
    const sessionFinished = new Promise<SessionRouteOutcome>(resolve => { sessionForwarded = resolve; });
    let oldFetched = 0;
    let oldRequest: Request | undefined;
    let oldFailure: string | undefined;
    let oldResponses = 0;
    const oldResponse = (response: Response) => { if (response.request() === oldRequest) oldResponses++; };
    const oldRequestFailed = (request: Request) => {
      if (request === oldRequest) oldFailure = request.failure()?.errorText;
    };
    page.on('response', oldResponse);
    page.on('requestfailed', oldRequestFailed);
    const sessionPattern = `**/notify/inbox/${owned.v1.messageId}`;
    await page.route(sessionPattern, async route => {
      oldRequest = route.request();
      let outcome: SessionRouteOutcome = 'failed';
      try {
        const response = await route.fetch();
        expect(response.status()).toBe(200);
        ++oldFetched;
        await pending;
        await route.fulfill({ response });
        outcome = 'fulfilled';
      } finally {
        sessionForwarded(outcome);
      }
    }, { times: 1 });
    let sessionFailure: unknown;
    try {
      await samePageQuery(page, owned.v1.messageId);
      await expect.poll(() => oldFetched, { timeout: stepTimeout }).toBe(1);
      expect(oldRequest).toBeDefined();
      await page.locator('.avatar-wrapper').click();
      await page.getByText('退出登录', { exact: true }).click();
      await page.getByRole('button', { name: '确定', exact: true }).click();
      await expect(page).toHaveURL(/\/login(?:\?|$)/);
      await login(page, origin, required('T40_B_USERNAME'), required('T40_B_PASSWORD'));
      // B 的 redirect 可能请求同 URL，必须以 Request 身份区分，不能把 B 的 403 当成 A 响应。
      await expect(page).toHaveURL(/\/notify\/inbox(?:\?|$)/);
      await samePageQuery(page, owned.bControl.messageId);
      await expect(detail(page).locator('.el-descriptions__content').last()).toHaveText(owned.bControl.content);
      expect(oldResponses).toBe(0);
      unblock();
      const outcome = await bounded(sessionFinished, 'old A route completion');
      expect(outcome).toBe('fulfilled');
      await expect.poll(() => oldFailure, { timeout: stepTimeout }).toBe('net::ERR_ABORTED');
      await painted(page);
      expect(oldResponses).toBe(0);
      await expect(detail(page).locator('.el-descriptions__content').last()).toHaveText(owned.bControl.content);
      await expect(page.getByText(owned.v1.content, { exact: true })).toHaveCount(0);
    } catch (error) {
      sessionFailure = error;
      throw error;
    } finally {
      unblock();
      page.off('response', oldResponse);
      page.off('requestfailed', oldRequestFailed);
      await cleanupPreserving(sessionFailure, () => page.unroute(sessionPattern), 'session unroute');
    }
  } catch (error) {
    testFailure = error;
    throw error;
  } finally {
    await cleanupPreserving(testFailure, () => context.close(), 'A browser context close');
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
