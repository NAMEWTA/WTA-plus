import { chromium, expect } from '@playwright/test';
/** 真实三端浏览器验收。配置/凭据只从自有 /tmp fixture 读取，不 mock OIDC，不输出协议查询或 token。 */
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

const fixtureFile = process.env.EXTERNAL_AUTH_FIXTURE;
if (!fixtureFile?.startsWith('/tmp/')) throw new Error('Owned fixture file required');
const fixture = JSON.parse(await readFile(fixtureFile, 'utf8'));
for (const key of ['adminOrigin', 'homeOrigin', 'ssoOrigin']) {
  const url = new URL(fixture[key]);
  if (!['127.0.0.1', 'localhost', 'admin.localhost', 'home.localhost', 'sso.localhost'].includes(url.hostname))
    throw new Error('Owned loopback origin required');
}
if (!fixture.evidenceDir?.startsWith('/tmp/')) throw new Error('Owned evidence directory required');
await mkdir(fixture.evidenceDir, { recursive: true, mode: 0o700 });
const evidence = [];
let step = 'launch';
let activePage;
const browser = await chromium.launch({
  channel: process.env.EXTERNAL_AUTH_BROWSER || 'chrome',
  args: ['--host-resolver-rules=MAP *.localhost 127.0.0.1']
});
const contexts = [];
const pass = label => {
  evidence.push(label);
  console.log(`PASS ${label}`);
};
const assert = (condition, label) => {
  if (!condition) throw new Error(label);
};
const localPath = (origin, path) => `${origin.replace(/\/$/, '')}${path}`;
const adminTarget = fixture.adminTarget || '/index';
const homeTarget = fixture.homeTarget || '/profile';
// App URL 可以带部署前缀；仅比较 Origin 会把同域的其他 App 误判为回调完成。
const adminBase = fixture.adminOrigin;
const homeBase = fixture.homeOrigin;
function belongsToApp(url, appBase) {
  const base = new URL(appBase);
  const prefix = base.pathname.replace(/\/$/, '');
  return url.origin === base.origin && (!prefix || url.pathname === prefix || url.pathname.startsWith(`${prefix}/`));
}
async function context() {
  const value = await browser.newContext({ ignoreHTTPSErrors: true, viewport: { width: 1440, height: 1000 } });
  value.setDefaultTimeout(20000);
  contexts.push(value);
  return value;
}
async function screenshot(page, name) {
  await page.screenshot({
    path: join(fixture.evidenceDir, `${fixture.mode || 'shared'}-${name}.png`),
    fullPage: true,
    animations: 'disabled'
  });
}
async function target(page, origin, path) {
  await expect.poll(() => page.url() === localPath(origin, path), { timeout: 30000 }).toBe(true);
}
async function localLogin(page) {
  await page.goto(localPath(fixture.adminOrigin, '/login'));
  await page.getByRole('textbox', { name: '用户名', exact: true }).fill(fixture.adminLocal.username);
  await page.getByLabel('密码', { exact: true }).fill(fixture.adminLocal.password);
  await page.getByRole('button', { name: /^登\s*录$/ }).click();
  await target(page, fixture.adminOrigin, adminTarget);
}
async function finishCentral(page, credentials, appOrigin, firstBinding = false, requireSessionReuse = false) {
  await page.waitForURL(
    url =>
      belongsToApp(url, fixture.ssoOrigin) ||
      (!firstBinding && belongsToApp(url, appOrigin) && !url.pathname.endsWith('/login')),
    { timeout: 30000 }
  );
  if (belongsToApp(new URL(page.url()), appOrigin)) return;
  const login = page.locator('input[name="username"]');
  const result = await Promise.race([
    login.waitFor({ state: 'visible', timeout: 30000 }).then(() => 'password'),
    page
      .waitForURL(url => belongsToApp(url, appOrigin) && url.pathname.endsWith('/social-callback'), { timeout: 30000 })
      .then(() => 'callback')
  ]);
  if (result === 'password') {
    assert(!requireSessionReuse, 'Existing central session must not require another password');
    await login.fill(credentials.username);
    await page.locator('input[name="password"]').fill(credentials.password);
    await page.getByRole('button', { name: fixture.centralLoginButton || '登录并继续', exact: true }).click();
  }
}
async function socialLogin(page, app, credentials, requireSessionReuse = false) {
  const origin = app === 'admin' ? fixture.adminOrigin : fixture.homeOrigin;
  await page.goto(localPath(origin, '/login'));
  await page.getByRole('button', { name: fixture.providerName, exact: true }).click();
  await finishCentral(page, credentials, origin, false, requireSessionReuse);
}
async function accountCommand(page, command, admin = false) {
  await page.locator('.avatar-container .el-dropdown').click();
  await page.getByRole('menuitem', { name: command, exact: true }).click();
  if (admin || command === '退出全部应用') {
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: /^(确定|确认|确认退出)$/ }).click();
  }
}
async function noToken(page, key) {
  await expect.poll(() => page.evaluate(name => !localStorage.getItem(name), key)).toBe(true);
}

try {
  step = 'Admin local account binding';
  const shared = await context();
  const admin = await shared.newPage();
  activePage = admin;
  await localLogin(admin);
  await admin.goto(localPath(fixture.adminOrigin, '/user/profile?tab=thirdParty'));
  await expect(admin.getByRole('heading', { name: '账号绑定', exact: true })).toBeVisible();
  if (!fixture.sharedAlreadyBound) {
    await admin.getByRole('button', { name: fixture.providerName, exact: true }).click();
    await finishCentral(admin, fixture.sharedCentral, adminBase, true);
    await target(admin, fixture.adminOrigin, '/user/profile?tab=thirdParty');
  }
  await expect(admin.getByRole('button', { name: '解绑', exact: true }).first()).toBeVisible();
  await screenshot(admin, 'admin-bindings');
  pass('Admin preserves password login and binds an existing account through OIDC');

  step = 'Admin external login';
  await accountCommand(admin, '退出当前应用', true);
  await socialLogin(admin, 'admin', fixture.sharedCentral);
  await target(admin, fixture.adminOrigin, adminTarget);
  pass('Admin bound OIDC login restores business navigation');

  step = 'Home central-session reuse';
  const home = await shared.newPage();
  activePage = home;
  await socialLogin(home, 'home', fixture.sharedCentral, true);
  await target(home, fixture.homeOrigin, homeTarget);
  assert(await home.evaluate(() => Boolean(localStorage.getItem('Home-Token'))), 'Home business token');
  const adminToken = await admin.evaluate(() => localStorage.getItem('Admin-Token'));
  const crossClient = await home.request.get(localPath(fixture.homeOrigin, '/prod-api/system/user/getInfo'), {
    headers: {
      Authorization: `Bearer ${adminToken}`,
      clientid: fixture.homeClientId || '428a8310cd442757ae699df5d894f051'
    }
  });
  assert((await crossClient.json()).code !== 200, 'Admin business token must not authorize Home');
  await screenshot(home, 'home-oidc');
  await home.setViewportSize({ width: 390, height: 844 });
  await screenshot(home, 'home-oidc-mobile');
  await home.setViewportSize({ width: 1440, height: 1000 });
  pass(`Home login succeeds with ${fixture.mode || 'shared'} business backend`);

  step = 'Local logout preserves central session';
  await accountCommand(home, '退出当前应用');
  await noToken(home, 'Home-Token');
  await admin.reload();
  await expect(admin.locator('.avatar-container .el-dropdown')).toBeVisible();
  await socialLogin(home, 'home', fixture.sharedCentral, true);
  await target(home, fixture.homeOrigin, homeTarget);
  pass('Current-app logout permits subsequent central-session login');

  step = 'Cross-app global logout';
  await accountCommand(home, '退出全部应用');
  const centralConfirmation = home.getByRole('button', { name: fixture.centralLogoutButton || /确认退出|退出登录|继续退出|^退出$|^确认$/ });
  await Promise.race([
    home.waitForURL(url => belongsToApp(url, homeBase) && url.pathname.endsWith('/logout/callback'), {
      timeout: 30000
    }),
    centralConfirmation
      .first()
      .waitFor({ state: 'visible', timeout: 30000 })
      .then(() => centralConfirmation.first().click())
  ]);
  await expect(home.getByRole('heading', { name: '已退出登录', exact: true })).toBeVisible();
  await noToken(home, 'Home-Token');
  await screenshot(home, 'global-logout');
  await expect
    .poll(
      async () => {
        await admin.reload();
        return admin.getByRole('textbox', { name: '用户名', exact: true }).isVisible();
      },
      { timeout: 30000, intervals: [1000] }
    )
    .toBe(true);
  await noToken(admin, 'Admin-Token');
  await admin.getByRole('button', { name: fixture.providerName, exact: true }).click();
  await admin.waitForURL(url => belongsToApp(url, fixture.ssoOrigin), { timeout: 30000 });
  await expect(admin.locator('input[name="username"]')).toBeVisible();
  pass('RP initiated global logout ends both business sessions');

  step = 'Missing-phone first login';
  const phoneContext = await context();
  const phone = await phoneContext.newPage();
  activePage = phone;
  await socialLogin(phone, 'home', fixture.missingPhoneCentral);
  await expect(phone.getByRole('heading', { name: '补充手机号码', exact: true })).toBeVisible();
  await noToken(phone, 'Home-Token');
  assert(new URL(phone.url()).search === '', 'Callback clears one-time protocol parameters');
  await screenshot(phone, 'complete-phone');
  await phone.getByRole('textbox', { name: '手机号码', exact: true }).fill(fixture.missingPhoneCentral.phoneNumber);
  await phone.getByRole('button', { name: '保存并继续', exact: true }).click();
  await target(phone, fixture.homeOrigin, homeTarget);
  pass('Home defers business token until required phone information is supplied');

  step = 'Phone/email conflict';
  const conflictContext = await context();
  const conflict = await conflictContext.newPage();
  activePage = conflict;
  await socialLogin(conflict, 'home', fixture.conflictCentral);
  await expect(conflict.getByRole('heading', { name: '绑定已有账号', exact: true })).toBeVisible();
  await noToken(conflict, 'Home-Token');
  await screenshot(conflict, 'conflict-binding');
  await conflict.getByRole('button', { name: '登录已有账号并绑定', exact: true }).click();
  await expect(conflict.getByRole('textbox', { name: '用户名', exact: true })).toBeVisible();
  assert(
    new URL(conflict.url()).searchParams.get('redirect') === '/account/bindings',
    'Binding guidance preserves destination'
  );
  pass('Conflicting identity guides existing-account binding without automatic linking');

  step = 'Admin unbound identity';
  const unboundContext = await context();
  const unbound = await unboundContext.newPage();
  activePage = unbound;
  await socialLogin(unbound, 'admin', fixture.unboundCentral);
  await expect(unbound.getByRole('heading', { name: '绑定已有账号', exact: true })).toBeVisible();
  await noToken(unbound, 'Admin-Token');
  pass('Admin refuses to auto-provision an unbound identity');

  step = 'Invalid callback isolation';
  const invalidContext = await context();
  const invalid = await invalidContext.newPage();
  activePage = invalid;
  let exchanges = 0;
  invalid.on('request', request => {
    if (new URL(request.url()).pathname.endsWith('/auth/login') && request.method() === 'POST') exchanges++;
  });
  await invalid.goto(localPath(fixture.homeOrigin, '/social-callback?state=invalid&code=invalid'));
  await expect(invalid.getByRole('alert')).toBeVisible();
  await noToken(invalid, 'Home-Token');
  assert(exchanges === 0 && new URL(invalid.url()).search === '', 'Invalid callback does not exchange and clears URL');
  pass('Forged or expired browser transaction is rejected before code exchange');

  await writeFile(
    join(fixture.evidenceDir, `${fixture.mode || 'shared'}-result.json`),
    JSON.stringify({ status: 'passed', evidence }, null, 2),
    { mode: 0o600 }
  );
} catch (error) {
  if (activePage) await screenshot(activePage, 'failure').catch(() => undefined);
  await writeFile(
    join(fixture.evidenceDir, `${fixture.mode || 'shared'}-result.json`),
    JSON.stringify(
      {
        status: 'failed',
        step,
        evidence,
        error: error instanceof Error ? error.name : 'Error',
        detail: error instanceof Error ? error.message.split('\n')[0].replace(/https?:\/\/\S+/g, '<url>') : ''
      },
      null,
      2
    ),
    { mode: 0o600 }
  );
  console.error(`FAIL ${step}`);
  process.exitCode = 1;
} finally {
  for (const context of contexts) await context.close().catch(() => undefined);
  await browser.close();
}
