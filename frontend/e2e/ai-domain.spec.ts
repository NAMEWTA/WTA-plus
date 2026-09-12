import { expect, test, type Page, type Route } from '@playwright/test';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4173';
const adminClientId = 'e5cd7e4891bf95d1d19206ce24a7b32e';

type AiProofState = {
  abortedProbes: number;
  authenticatedRegistration: boolean;
  clientIds: string[];
  frameRequests: number;
  hangingProbes: number;
  probes: number;
  registrations: number;
  timedOutProbes: number;
  unknownRequests: string[];
};

const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });

async function installAdminApi(page: Page, state: AiProofState) {
  let releaseHangingProbe: (() => void) | undefined;
  let hangingProbeUrl = '';
  page.on('requestfailed', request => {
    if (request.resourceType() !== 'fetch' || request.url() !== hangingProbeUrl || !releaseHangingProbe) return;
    state.timedOutProbes += 1;
    const release = releaseHangingProbe;
    releaseHangingProbe = undefined;
    hangingProbeUrl = '';
    release();
  });

  await page.route('**/prod-api/**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/prod-api', '');
    const method = request.method();

    if (path === '/system/user/getInfo' && method === 'GET') {
      return json(route, {
        code: 200,
        data: {
          user: { userId: 12, userName: 'ai-reader', nickName: 'AI Reader', avatarUrl: '' },
          roles: ['operator'],
          permissions: []
        }
      });
    }
    if (path === '/system/menu/getRouters' && method === 'GET') {
      return json(route, {
        code: 200,
        data: [
          {
            path: '/ai-chat-proof',
            name: 'AiChatProof',
            component: 'ai/chat/index',
            meta: { title: 'AI 会话' }
          }
        ]
      });
    }
    if (path === '/notify/inbox' && method === 'GET') {
      return json(route, { code: 200, data: [] });
    }
    if (path === '/resource/message/close') return json(route, { code: 200, data: null });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    if (path === '/snail-ai/user/register' && method === 'POST') {
      state.registrations += 1;
      state.authenticatedRegistration ||= Boolean(request.headers().authorization);
      state.clientIds.push(request.headers().clientid ?? '');
      return json(route, { code: 200, data: { openId: 'browser-proof-user' } });
    }
    if (path === '/snail-chat/' && method === 'GET') {
      if (request.resourceType() === 'fetch') {
        state.probes += 1;
        if (state.abortedProbes > 0) {
          state.abortedProbes -= 1;
          return route.abort('failed');
        }
        if (state.hangingProbes > 0) {
          state.hangingProbes -= 1;
          hangingProbeUrl = request.url();
          await new Promise<void>(resolve => {
            releaseHangingProbe = resolve;
          });
          try {
            await route.abort('failed');
          } catch {
            // The browser-side AbortController already owns this terminal request state.
          }
          return;
        }
        return route.fulfill({
          contentType: 'text/html',
          body: '<!doctype html><title>Snail AI probe</title>'
        });
      }
      state.frameRequests += 1;
      return route.fulfill({
        contentType: 'text/html',
        body: '<!doctype html><title>Snail AI proof</title><main>Snail AI ready</main>'
      });
    }

    state.unknownRequests.push(`${method} ${path}`);
    return route.abort('failed');
  });
}

const createState = (): AiProofState => ({
  abortedProbes: 0,
  authenticatedRegistration: false,
  clientIds: [],
  frameRequests: 0,
  hangingProbes: 0,
  probes: 0,
  registrations: 0,
  timedOutProbes: 0,
  unknownRequests: []
});

test('admin selects the permission-neutral AI manifest and loads the embedded chat', async ({ page }) => {
  const state = createState();
  await installAdminApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'ai-browser-proof'));

  await page.goto(`${adminUrl}/ai-chat-proof`);
  const frame = page.locator('iframe[title="Snail AI"]');
  await expect(frame).toBeVisible();
  await expect(frame.contentFrame().getByText('Snail AI ready', { exact: true })).toBeVisible();
  await expect(page.locator('.ai-chat-page .el-loading-mask')).toHaveCount(0);
  await expect.poll(() => state.probes).toBe(1);
  await expect.poll(() => state.frameRequests).toBe(1);

  const source = await frame.getAttribute('src');
  const frameUrl = new URL(source!, page.url());
  expect(frameUrl.pathname).toBe('/prod-api/snail-chat/');
  expect(frameUrl.searchParams.get('openId')).toBe('browser-proof-user');
  expect(frameUrl.searchParams.has('trustedCredential')).toBe(true);
  expect(state).toMatchObject({
    authenticatedRegistration: true,
    clientIds: [adminClientId],
    registrations: 1,
    unknownRequests: []
  });
});

test('failed and hanging probes keep the sensitive URL private and remain retryable', async ({ page }) => {
  const state = createState();
  state.abortedProbes = 1;
  state.hangingProbes = 1;
  await installAdminApi(page, state);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'ai-browser-retry-proof'));

  await page.goto(`${adminUrl}/ai-chat-proof`);
  const frame = page.locator('iframe[title="Snail AI"]');
  await expect(page.getByText('AI 聊天连接中断，请重新加载', { exact: true })).toBeVisible();
  await expect(frame).toHaveCount(0);
  await expect(page.locator('.ai-chat-page .el-loading-mask')).toHaveCount(0);
  await expect.poll(() => state.registrations).toBe(1);
  await expect.poll(() => state.probes).toBe(1);
  expect(await page.locator('html').innerHTML()).not.toContain('ai-browser-retry-proof');

  await page.getByRole('button', { name: '重新加载' }).click();
  await expect(page.getByText('AI 聊天连接中断，请重新加载', { exact: true })).toHaveCount(0);
  await expect(page.locator('.ai-chat-page .el-loading-mask')).toBeVisible();
  await expect.poll(() => state.probes).toBe(2);
  await expect(page.getByText('AI 聊天连接中断，请重新加载', { exact: true })).toBeVisible({ timeout: 20000 });
  await expect(page.locator('iframe[title="Snail AI"]')).toHaveCount(0);
  await expect(page.locator('.ai-chat-page .el-loading-mask')).toHaveCount(0);
  await expect.poll(() => state.timedOutProbes).toBe(1);
  expect(await page.locator('html').innerHTML()).not.toContain('ai-browser-retry-proof');

  await page.getByRole('button', { name: '重新加载' }).click();
  const retryFrame = page.locator('iframe[title="Snail AI"]');
  await expect(retryFrame).toBeVisible();
  await expect(retryFrame.contentFrame().getByText('Snail AI ready', { exact: true })).toBeVisible();
  await expect(page.locator('.ai-chat-page .el-loading-mask')).toHaveCount(0);
  await expect.poll(() => state.registrations).toBe(3);
  await expect.poll(() => state.probes).toBe(3);
  await expect.poll(() => state.frameRequests).toBe(1);
  const retrySource = await retryFrame.getAttribute('src');
  expect(new URL(retrySource!, page.url()).searchParams.has('trustedCredential')).toBe(true);
  expect(state.unknownRequests).toEqual([]);
});
