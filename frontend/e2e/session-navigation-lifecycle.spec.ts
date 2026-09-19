import { expect, test, type Page, type Route } from '@playwright/test';

const apps = [
  { kind: 'admin', env: 'LIFECYCLE_ADMIN_ORIGIN', clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e', tokenKey: 'Admin-Token', user: 'user', navigation: 'navigation', target: '/index' },
  { kind: 'home', env: 'LIFECYCLE_HOME_ORIGIN', clientId: '428a8310cd442757ae699df5d894f051', tokenKey: 'Home-Token', user: 'home-user', navigation: 'home-navigation', target: '/profile' }
] as const;
type App = (typeof apps)[number];
type Fault = 'none' | 'offline' | 'timeout' | 'unauthorized' | 'http-unauthorized';
const passwordPolicy = { minimumLength: 8, maximumLength: 30, requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'], allowedSpecialCharacters: '@$!%*?&' };

function origin(app: App): string {
  const value = process.env[app.env];
  if (!value || new URL(value).protocol !== 'https:' || new URL(value).hostname !== '127.0.0.1') throw new Error(`Owned HTTPS fixture required: ${app.env}`);
  return value;
}

function fixture() {
  return { realStream: false, identity: 'First', roles: [] as string[], fault: 'none' as Fault, unauthorized: false, hold: '', held: [] as Route[], requests: [] as string[], clients: [] as string[], logout: 0 };
}
type State = ReturnType<typeof fixture>;
const json = (route: Route, body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });

async function install(page: Page, state: State) {
  // Only the HTTP failure/identity boundary is a fixture. Router, Pinia, Axios and UI are built product code.
  await page.route('**/prod-api/**', async route => {
    const path = new URL(route.request().url()).pathname.replace('/prod-api', '');
    state.requests.push(path);
    if (path !== '/resource/message') state.clients.push(route.request().headers().clientid ?? '');
    if (path === state.hold) { state.held.push(route); return; }
    if (path === '/auth/client/context') return json(route, { code: 200, data: { clientEnabled: true, registerEnabled: true, passwordPolicy } });
    if (path === '/auth/code') return json(route, { code: 200, data: { captchaEnabled: false } });
    if (path === '/auth/login') return json(route, { code: 200, data: { access_token: `owned-${state.identity}` } });
    if (path === '/system/user/getInfo') return json(route, state.unauthorized ? { code: 401, msg: '会话已过期' } : {
      code: 200, data: { user: { userId: state.identity, userName: state.identity, nickName: state.identity, avatarUrl: '' }, roles: state.roles, permissions: [`owned:${state.identity}:read`] }
    });
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: [{ path: '/owned-menu', name: `Owned${state.identity}`, component: 'profile/owned-diagnostic', meta: { title: state.identity } }] });
    if (path === '/auth/logout') {
      state.logout++;
      if (state.fault === 'timeout') { state.held.push(route); return; }
      if (state.fault === 'offline') return route.abort('internetdisconnected');
      if (state.fault === 'http-unauthorized') return json(route, { code: 401 }, 401);
      return json(route, { code: state.fault === 'unauthorized' ? 401 : 200 });
    }
    if (path === '/resource/message/ticket') return json(route, { code: 200, data: 'owned-push-ticket' });
    if (path === '/resource/message') return state.realStream ? route.continue() : route.fulfill({ contentType: 'text/event-stream', body: '' });
    return json(route, { code: 200, data: [], rows: [], total: 0 });
  });
}

interface Store {
  token: string; roles: string[]; permissions: string[]; nickname: string; userId: string;
  identityLoaded: boolean; navigationLoaded: boolean; routes: unknown[];
  getInfo(): Promise<void>; logout(): Promise<void>; resetRoutes(): void;
}
interface Runtime {
  $pinia: object;
  $router: { getRoutes(): Array<{ name: unknown; path: string }>; push(path: string): Promise<unknown> };
}

// Inspect existing Vue/Pinia runtime objects; no production test hook or replacement store is installed.
async function probe(input: { app: App; action?: string; path?: string }) {
  const element = document.querySelector('#app');
  if (!element) return undefined;
  // Reflection reads framework-owned diagnostic properties without adding an App API.
  const vueApp = Reflect.get(element, '__vue_app__') as { config: { globalProperties: Runtime } } | undefined;
  if (!vueApp) return undefined;
  const runtime = vueApp.config.globalProperties;
  const stores = Reflect.get(runtime.$pinia, '_s') as Map<string, Store>;
  const user = stores.get(input.app.user);
  const navigation = stores.get(input.app.navigation);
  if (!user || !navigation) return undefined;
  if (input.action === 'navigate') await runtime.$router.push(input.path!);
  if (input.action === 'unauthorized') {
    void user.getInfo().catch(() => undefined);
    void user.getInfo().catch(() => undefined);
    void user.getInfo().catch(() => undefined);
  }
  if (input.action === 'menus') { navigation.resetRoutes(); void runtime.$router.push('/owned-menu?pending=1'); }
  if (input.action === 'logout') await user.logout().catch(() => undefined);
  return {
    hasToken: Boolean(user.token || localStorage.getItem(input.app.tokenKey)), nickname: user.nickname,
    roles: [...user.roles], permissions: [...user.permissions], identityLoaded: user.identityLoaded,
    navigationLoaded: navigation.navigationLoaded, projected: navigation.routes.length,
    owned: runtime.$router.getRoutes().map(route => route.name).filter(name => String(name).startsWith('Owned')),
    hasLogin: runtime.$router.getRoutes().some(route => route.path === '/login')
  };
}

async function login(page: Page, app: App) {
  await page.goto(`${origin(app)}/login`);
  const form = page.locator(app.kind === 'admin' ? '.login-form' : '.identity-login__form');
  const username = app.kind === 'admin' ? form.locator('input').first() : form.locator('input[name="username"]');
  await expect(username).toBeEnabled();
  await username.fill('owned-user');
  await form.locator('input[type="password"]').fill('OwnedPass!9');
  await form.locator(app.kind === 'admin' ? '.submit-button' : '.identity-login__submit').click();
  await page.waitForURL(origin(app) + app.target, { waitUntil: 'load' });
  await expect.poll(async () => (await page.evaluate(probe, { app }))?.navigationLoaded).toBe(true);
}

async function logoutUI(page: Page, app: App) {
  if (app.kind === 'home') await page.getByRole('button', { name: '退出', exact: true }).click();
  else {
    await page.locator('.avatar-wrapper').click();
    await page.getByRole('menuitem', { name: /退出/ }).click();
    await page.getByRole('button', { name: '确定', exact: true }).click();
  }
}

async function assertCleared(page: Page, app: App) {
  await expect.poll(() => page.evaluate(probe, { app }), { timeout: 15_000 }).toEqual({ hasToken: false, nickname: '', roles: [], permissions: [], identityLoaded: false, navigationLoaded: false, projected: 0, owned: [], hasLogin: true });
}

for (const app of apps) {
  test(`T-12 ${app.kind} empty roles recover once across repeated SPA navigation`, async ({ page }) => {
    const state = fixture(); await install(page, state); await login(page, app);
    await page.evaluate(probe, { app, action: 'navigate', path: '/owned-menu?a=1' });
    await page.evaluate(probe, { app, action: 'navigate', path: '/owned-menu?a=2' });
    expect(state.requests.filter(path => path === '/system/user/getInfo')).toHaveLength(1);
    expect(state.requests.filter(path => path === '/system/menu/getRouters')).toHaveLength(1);
    expect(await page.evaluate(probe, { app })).toMatchObject({ roles: [], identityLoaded: true, owned: ['OwnedFirst'] });
    expect(state.clients.every(client => client === app.clientId)).toBe(true);
  });

  for (const fault of ['offline', 'timeout', 'unauthorized', 'http-unauthorized'] as const) {
    test(`T-12 ${app.kind} ${fault} logout clears local state and routes and completes UI navigation`, async ({ page }) => {
      const state = fixture(); await install(page, state); await login(page, app);
      state.fault = fault;
      await logoutUI(page, app);
      await assertCleared(page, app);
      await expect(page).toHaveURL(app.kind === 'admin' ? /\/login\?redirect=/ : origin(app) + '/');
      expect(state.logout).toBe(1);
      await expect(page.getByRole('dialog')).toHaveCount(0);
      // Same document and router instance: a later identity cannot retain the old menu.
      state.fault = 'none'; state.identity = 'Second'; state.roles = ['second'];
      await page.evaluate(probe, { app, action: 'navigate', path: '/login' });
      // Home's existing login callback reloads; route reclamation was asserted before that reload.
      const form = page.locator(app.kind === 'admin' ? '.login-form' : '.identity-login__form');
      await expect(form.locator('input').first()).toBeEnabled();
      await form.locator('input').first().fill('second-user');
      await form.locator('input[type="password"]').fill('OwnedPass!9');
      await form.locator(app.kind === 'admin' ? '.submit-button' : '.identity-login__submit').click();
      await page.waitForURL(origin(app) + app.target, { waitUntil: 'load' });
      await expect.poll(async () => (await page.evaluate(probe, { app }))?.owned).toEqual(['OwnedSecond']);
      expect(await page.evaluate(probe, { app })).toMatchObject({ nickname: 'Second', roles: ['second'], permissions: ['owned:Second:read'] });
    });
  }

  test(`T-12 ${app.kind} concurrent 401 cancellation and retry has one terminating recovery`, async ({ page }) => {
    const state = fixture(); await install(page, state); await login(page, app);
    state.unauthorized = true;
    await page.evaluate(probe, { app, action: 'unauthorized' });
    const dialog = page.getByRole('dialog', { name: '系统提示' });
    await expect(dialog).toHaveCount(1);
    await dialog.getByRole('button', { name: app.kind === 'admin' ? '取消' : 'Cancel', exact: true }).click();
    expect(state.logout).toBe(0);
    await expect(dialog).toHaveCount(0);
    await page.evaluate(probe, { app, action: 'unauthorized' });
    await expect(dialog).toHaveCount(1);
    state.fault = 'unauthorized';
    await dialog.getByRole('button', { name: app.kind === 'admin' ? '重新登录' : 'OK', exact: true }).click();
    await assertCleared(page, app);
    await expect(page).toHaveURL(/\/login\?redirect=/);
    await expect(dialog).toHaveCount(0);
    expect(state.logout).toBe(1);
  });

  test(`T-12 ${app.kind} initial identity 401 terminates without a competing modal`, async ({ page }) => {
    const state = fixture(); state.unauthorized = true; await install(page, state);
    await page.addInitScript(key => localStorage.setItem(key, 'owned-expired'), app.tokenKey);
    await page.goto(origin(app) + app.target);
    await assertCleared(page, app);
    await expect(page.getByRole('dialog')).toHaveCount(0);
    expect(state.logout).toBe(1);
    expect(state.requests.filter(path => path === '/system/menu/getRouters')).toHaveLength(0);
  });

  for (const pending of ['identity', 'menus'] as const) {
    test(`T-12 ${app.kind} logout cancels pending ${pending} and prevents stale recovery`, async ({ page }) => {
      const state = fixture(); await install(page, state); await login(page, app);
      state.hold = pending === 'identity' ? '/system/user/getInfo' : '/system/menu/getRouters';
      await page.evaluate(probe, { app, action: pending === 'identity' ? 'unauthorized' : 'menus' });
      await expect.poll(() => state.held.length).toBeGreaterThan(0);
      const cancelled: string[] = [];
      page.on('requestfailed', request => { if (new URL(request.url()).pathname.endsWith(state.hold)) cancelled.push('cancelled'); });
      await page.evaluate(probe, { app, action: 'logout' });
      await assertCleared(page, app);
      await expect.poll(() => cancelled.length).toBeGreaterThan(0);
      for (const held of state.held) await json(held, { code: 401, msg: 'late response' }).catch(() => undefined);
      await assertCleared(page, app);
      await expect(page.getByRole('dialog')).toHaveCount(0);
    });
  }
}

test('T-12 separate Client origins retain only their own identity and navigation', async ({ context }) => {
  const admin = await context.newPage(); const home = await context.newPage();
  const first = fixture(); const second = fixture(); second.identity = 'Home'; second.roles = ['home'];
  await install(admin, first); await login(admin, apps[0]);
  await install(home, second); await login(home, apps[1]);
  await logoutUI(admin, apps[0]); await assertCleared(admin, apps[0]);
  expect(await home.evaluate(probe, { app: apps[1] })).toMatchObject({ hasToken: true, nickname: 'Home', roles: ['home'], owned: ['OwnedHome'] });
  expect(first.clients.every(client => client === apps[0].clientId)).toBe(true);
  expect(second.clients.every(client => client === apps[1].clientId)).toBe(true);
});

test('T-12 Admin logout closes a real open HTTPS EventSource despite remote failure', async ({ page }) => {
  await page.addInitScript(() => {
    const Original = window.EventSource;
    const sources: EventSource[] = [];
    Reflect.set(window, 'ownedSseSources', sources);
    window.EventSource = class extends Original {
      constructor(url: string | URL, options?: EventSourceInit) {
        super(url, options);
        sources.push(this);
      }
    };
  });
  const state = fixture(); state.realStream = true;
  await install(page, state); await login(page, apps[0]);
  const connectionStates = () => page.evaluate(() => (Reflect.get(window, 'ownedSseSources') as EventSource[]).map(source => source.readyState));
  await expect.poll(connectionStates).toEqual([1]);
  state.fault = 'offline';
  await logoutUI(page, apps[0]);
  await assertCleared(page, apps[0]);
  await expect.poll(connectionStates).toEqual([2]);
});
