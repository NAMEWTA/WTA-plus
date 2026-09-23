import { expect, test, type Page, type Request, type Route } from '@playwright/test';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4173';
const liveBaseUrl = process.env.NACOS_LIVE_BASE_URL?.replace(/\/$/, '');
const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });
const nacosMenu = {
  path: '/system-nacos',
  name: 'NacosConsoleProof',
  component: 'monitor/nacos/index',
  meta: { title: '配置中心' }
};

async function installAdminApi(page: Page, permissions: string[]) {
  const unknown: string[] = [];
  await page.route('**/prod-api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    if (path === '/system/user/getInfo')
      return json(route, {
        code: 200,
        data: {
          user: { userId: 1, userName: 'nacos-operator', nickName: 'Nacos Operator', avatarUrl: '' },
          roles: ['operator'],
          permissions
        }
      });
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: [nacosMenu] });
    if (path === '/notify/inbox')
      return json(route, { code: 200, data: { rows: [], total: 0, unreadTotal: 0 } });
    if (path === '/resource/message/close') return json(route, { code: 200, data: null });
    if (path === '/resource/message/ticket') return json(route, { code: 200, data: 'owned-push-ticket' });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    unknown.push(`${request.method()} ${path}`);
    return route.abort('failed');
  });
  return unknown;
}

test('authorized WTA user gets a same-origin Nacos frame without credential injection', async ({ page }) => {
  const unknown = await installAdminApi(page, ['system:nacos:console']);
  const frameRequests: Request[] = [];
  await page.route('**/nacos/**', route => {
    frameRequests.push(route.request());
    return route.fulfill({
      contentType: 'text/html',
      body: '<!doctype html><html><head><title>Nacos</title></head><body><main>Nacos Login</main><input name="username"></body></html>'
    });
  });
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'wta-session-proof'));

  await page.goto(`${adminUrl}${nacosMenu.path}`);
  const iframe = page.locator('iframe');
  await expect(iframe).toHaveAttribute('src', '/nacos/');
  await expect(page.frameLocator('iframe').getByText('Nacos Login')).toBeVisible();
  expect(frameRequests).toHaveLength(1);
  expect(frameRequests[0]?.headers().authorization).toBeUndefined();
  expect(frameRequests[0]?.headers().cookie).toBeUndefined();
  expect(unknown).toEqual([]);
});

test('WTA permission denial creates no Nacos frame or request', async ({ page }) => {
  const unknown = await installAdminApi(page, []);
  let nacosRequests = 0;
  await page.route('**/nacos/**', route => {
    nacosRequests += 1;
    return route.abort('blockedbyclient');
  });
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'wta-denied-proof'));

  await page.goto(`${adminUrl}${nacosMenu.path}`);
  await expect(page.getByRole('alert')).toContainText('无权访问运维入口');
  await expect(page.locator('iframe')).toHaveCount(0);
  expect(nacosRequests).toBe(0);
  expect(unknown).toEqual([]);
});

test('live proxy loads official login in a same-origin iframe and keeps API authentication', async ({
  page,
  request
}, testInfo) => {
  test.skip(!liveBaseUrl, 'Set NACOS_LIVE_BASE_URL to run the real Nginx/Nacos browser gate.');
  const failedStaticResources: string[] = [];
  const authenticationChallenges: number[] = [];
  const consoleErrors: string[] = [];
  page.on('console', message => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });
  page.on('response', response => {
    const url = response.url();
    if (!url.startsWith(`${liveBaseUrl}/nacos/`) || response.status() < 400) return;
    if (new URL(url).pathname.startsWith('/nacos/v1/')) authenticationChallenges.push(response.status());
    else failedStaticResources.push(`${response.status()} ${url}`);
  });

  await page.goto(`${liveBaseUrl}/nacos/`, { waitUntil: 'networkidle' });
  await expect(page).toHaveTitle(/Nacos/i);
  await expect(page.locator('input').first()).toBeVisible();

  await page.evaluate(() => {
    document.body.innerHTML = '<iframe id="nacos-console" src="/nacos/" style="width:100%;height:800px"></iframe>';
  });
  const officialFrame = page.frameLocator('#nacos-console');
  await expect(officialFrame.locator('input').first()).toBeVisible();
  await expect(officialFrame.locator('body')).toBeVisible();
  expect(failedStaticResources).toEqual([]);
  expect(authenticationChallenges.length).toBeGreaterThan(0);
  expect(authenticationChallenges.every(status => status === 403)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath('nacos-console-login.png'), fullPage: true });
  expect(consoleErrors).toHaveLength(authenticationChallenges.length);
  expect(consoleErrors.every(message => /status of 403/.test(message))).toBe(true);

  const anonymous = await request.get(
    `${liveBaseUrl}/nacos/v1/cs/configs?dataId=wta-namewta.yml&group=DEFAULT_GROUP&tenant=prod`
  );
  expect(anonymous.status()).toBe(403);
});
