import { chromium, expect } from '@playwright/test';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

/** 仅操作任务自有真实环境：UI 创建接入，协议/管理响应均不 mock，凭据不写入证据。 */
const fixturePath = process.env.OIDC_MANAGEMENT_FIXTURE;
if (!fixturePath?.startsWith('/tmp/')) throw new Error('Owned fixture file required');
const fixture = JSON.parse(await readFile(fixturePath, 'utf8'));
const ownedOrigin = value => {
  const url = new URL(value);
  if (!['127.0.0.1', 'localhost', 'admin.localhost', 'home.localhost', 'sso.localhost'].includes(url.hostname)) {
    throw new Error('Owned loopback endpoint required');
  }
  return url.href.replace(/\/$/, '');
};
const adminOrigin = ownedOrigin(fixture.adminOrigin);
ownedOrigin(fixture.provider.issuer);
for (const registration of fixture.registrations) {
  ownedOrigin(registration.appPublicUrl);
  ownedOrigin(registration.apiPublicBase);
}
if (!fixture.evidenceDir?.startsWith('/tmp/')) throw new Error('Owned evidence directory required');
await mkdir(fixture.evidenceDir, { recursive: true, mode: 0o700 });
const paths = {
  providers: '/system/externalAuthProvider',
  registrations: '/system/externalAuthRegistration',
  clients: '/system/client',
  ...fixture.paths
};
const browser = await chromium.launch({ channel: process.env.OIDC_MANAGEMENT_BROWSER || 'chrome' });
const context = await browser.newContext({ viewport: { width: 1440, height: 1050 }, ignoreHTTPSErrors: true });
await context.grantPermissions(['clipboard-read', 'clipboard-write']);
const page = await context.newPage();
page.setDefaultTimeout(20000);
const checks = [];
const registrations = [];
let step = 'local-admin-login';
const pass = message => {
  checks.push(message);
  console.log(`PASS ${message}`);
};
const screenshot = async name => {
  await expect(page.locator('.el-message')).toHaveCount(0, { timeout: 6000 });
  return page.screenshot({ path: join(fixture.evidenceDir, `${name}.png`), fullPage: true, animations: 'disabled' });
};
const exact = value => new RegExp(`^${value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`);
const item = (dialog, label) =>
  dialog
    .locator('.el-form-item')
    .filter({ has: page.locator('.el-form-item__label').filter({ hasText: exact(label) }) });
const input = (dialog, label) => item(dialog, label).getByRole('textbox');
async function choose(dialog, label, text) {
  await item(dialog, label).locator('.el-select__wrapper').click();
  await page.getByRole('option', { name: text, exact: true }).click();
}
async function setEnabled(dialog) {
  const control = item(dialog, '启用').getByRole('switch');
  if ((await control.getAttribute('aria-checked')) !== 'true') await item(dialog, '启用').locator('.el-switch').click();
}
async function copy(dialog, label, expected) {
  await dialog.getByRole('button', { name: `复制 ${label}`, exact: true }).click();
  await expect.poll(() => page.evaluate(() => navigator.clipboard.readText())).toBe(expected);
}
async function closeInfo() {
  const dialog = page.getByRole('dialog', { name: '业务 App 接入信息', exact: true });
  await dialog.getByRole('button', { name: '关闭', exact: true }).last().click();
  await expect(dialog).toBeHidden();
}
async function createProvider() {
  step = 'create-provider';
  await page.goto(adminOrigin + paths.providers);
  await expect(page.getByRole('heading', { name: '外部身份源', exact: true })).toBeVisible();
  if (fixture.reuseProvider) {
    await page
      .getByRole('row')
      .filter({ hasText: fixture.provider.providerKey })
      .getByRole('button', { name: '编辑', exact: true })
      .click();
  } else {
    await page.getByRole('button', { name: '新增身份源', exact: true }).click();
  }
  const dialog = page.getByRole('dialog', { name: fixture.reuseProvider ? '编辑身份源' : '新增身份源', exact: true });
  if (!fixture.reuseProvider) {
    await input(dialog, '身份源标识').fill(fixture.provider.providerKey);
    await input(dialog, '显示名称').fill(fixture.provider.name);
    await input(dialog, 'Issuer').fill(fixture.provider.issuer);
  } else {
    await expect(input(dialog, 'Issuer')).toHaveValue(fixture.provider.issuer);
  }
  await setEnabled(dialog);
  await dialog.getByRole('button', { name: '检查 OIDC 连接', exact: true }).click();
  await expect(
    dialog.getByText('已读取元数据；客户端密钥、账号准入和完整登录仍需实际验证。', { exact: true })
  ).toBeVisible();
  await expect(dialog.getByText('检查时间', { exact: true })).toBeVisible();
  await screenshot('provider-discovery');
  await dialog.getByRole('button', { name: '保存', exact: true }).click();
  await expect(dialog).toBeHidden();
  await expect(page.getByRole('row').filter({ hasText: fixture.provider.providerKey })).toBeVisible();
  pass(
    `Admin ${fixture.reuseProvider ? 'edits' : 'creates'} an external OIDC identity source and reads fresh Discovery through the UI`
  );
}
async function createRegistration(config, index) {
  step = `create-registration-${index}`;
  await page.goto(adminOrigin + paths.registrations);
  await expect(page.getByRole('heading', { name: '业务 App 登录接入', exact: true })).toBeVisible();
  const reuse = fixture.reuseRegistrations?.includes(config.externalClientId);
  const ownedRow = () =>
    page
      .getByRole('row')
      .filter({ hasText: fixture.provider.name })
      .filter({ hasText: config.businessClientId })
      .filter({ hasText: config.externalClientId });
  if (reuse) {
    await ownedRow().getByRole('button', { name: '编辑', exact: true }).click();
  } else {
    await page.getByRole('button', { name: '新增接入', exact: true }).click();
  }
  const dialog = page.getByRole('dialog', { name: reuse ? '编辑接入' : '新增接入', exact: true });
  if (!reuse) {
    const provider = dialog.getByRole('combobox', { name: '身份源', exact: true });
    await provider.fill(fixture.provider.name);
    await page.getByRole('option').filter({ hasText: fixture.provider.providerKey }).click();
    const client = dialog.getByRole('combobox', { name: '业务客户端', exact: true });
    await client.fill(config.clientKey);
    await page.getByRole('option').filter({ hasText: config.businessClientId }).click();
    await input(dialog, '身份提供方 Client ID').fill(config.externalClientId);
    await item(dialog, '客户端密钥').locator('input').fill(config.clientSecret);
  }
  await choose(
    dialog,
    '客户端认证方式',
    config.authenticationMethod === 'client_secret_post' ? '表单 Client Secret' : 'HTTP Basic'
  );
  await input(dialog, 'App 公共访问地址').fill(config.appPublicUrl);
  await dialog.getByRole('button', { name: '生成回调地址', exact: true }).click();
  const appBase = config.appPublicUrl.replace(/\/+$/, '');
  const callback = `${appBase}/social-callback`;
  const logout = `${appBase}/logout/callback`;
  await expect(input(dialog, '登录回调地址')).toHaveValue(callback);
  await expect(input(dialog, '退出回调地址')).toHaveValue(logout);
  await input(dialog, '业务 API 公共地址').fill(config.apiPublicBase);
  await dialog
    .locator('.el-radio')
    .filter({ hasText: exact(config.firstLoginPolicy === 'AUTO_REGISTER' ? '自动创建账号' : '仅已绑定账号') })
    .click();
  await setEnabled(dialog);
  await dialog.getByRole('button', { name: '保存并查看接入信息', exact: true }).click();
  const info = page.getByRole('dialog', { name: '业务 App 接入信息', exact: true });
  await expect(info.getByText('登录入口已启用', { exact: true })).toBeVisible();
  const registrationId = await input(info, '接入记录 ID').inputValue();
  const backchannel = `${config.apiPublicBase.replace(/\/+$/, '')}/auth/social/backchannel/${registrationId}`;
  await expect(input(info, '后台退出通知地址')).toHaveValue(backchannel);
  await expect(input(info, '授权范围')).toHaveValue('openid profile');
  await copy(info, '登录回调地址', callback);
  await copy(info, '后台退出通知地址', backchannel);
  await expect(info.getByRole('link', { name: '打开目标 App 验证登录', exact: true })).toHaveAttribute(
    'href',
    `${appBase}/login`
  );
  await info.locator('.el-dialog__body').evaluate(element => {
    element.scrollTop = 0;
  });
  await screenshot(`connection-${index}`);
  await input(info, '后台退出通知地址').scrollIntoViewIfNeeded();
  await screenshot(`connection-${index}-callbacks`);
  await page.setViewportSize({ width: 390, height: 844 });
  await info.locator('.el-dialog__body').evaluate(element => {
    element.scrollTop = 0;
  });
  await screenshot(`connection-${index}-mobile`);
  await input(info, '登录回调地址').scrollIntoViewIfNeeded();
  await screenshot(`connection-${index}-mobile-callbacks`);
  await page.setViewportSize({ width: 1440, height: 1050 });
  registrations.push({ businessClientId: config.businessClientId, registrationId, backchannelLogoutUri: backchannel });
  await closeInfo();
  pass(
    `App ${index} registration produces editable prefixed callbacks, a preserved ID and copyable backchannel information`
  );

  step = `retain-secret-${index}`;
  const row = ownedRow();
  await row.getByRole('button', { name: '编辑', exact: true }).click();
  const edit = page.getByRole('dialog', { name: '编辑接入', exact: true });
  await expect(item(edit, '更换客户端密钥（留空保留）').locator('input')).toHaveValue('');
  const response = page.waitForResponse(
    value => value.url().endsWith('/system/auth/registration/edit') && value.request().method() === 'POST'
  );
  await edit.getByRole('button', { name: '保存并查看接入信息', exact: true }).click();
  const result = await response;
  const body = result.request().postDataJSON();
  if (Object.hasOwn(body, 'clientSecret')) throw new Error('Blank secret edit must omit the secret field');
  await expect(
    page
      .getByRole('dialog', { name: '业务 App 接入信息', exact: true })
      .getByText('密钥已配置（不回显）', { exact: true })
  ).toBeVisible();
  await closeInfo();
  pass(`App ${index} editing without a replacement preserves the stored secret`);

  step = `client-counts-${index}`;
  await page.goto(adminOrigin + paths.clients);
  const clientRow = page.getByRole('row').filter({ hasText: config.businessClientId });
  await expect(clientRow.getByText(/启用\s+[1-9]\d*\s*\/\s*配置\s+[1-9]\d*/)).toBeVisible();
  await screenshot(`client-counts-${index}`);
  await clientRow.getByRole('button', { name: '管理接入', exact: true }).click();
  await expect(page.getByRole('heading', { name: '业务 App 登录接入', exact: true })).toBeVisible();
  await expect.poll(() => new URL(page.url()).searchParams.get('businessClientId')).toBe(config.businessClientId);
  await expect(ownedRow()).toBeVisible();
  pass(`App ${index} client list shows external-provider counts and opens its filtered registration page`);
}
try {
  await page.goto(`${adminOrigin}/login`);
  await page.getByRole('textbox', { name: '用户名', exact: true }).fill(fixture.adminLocal.username);
  await page.getByLabel('密码', { exact: true }).fill(fixture.adminLocal.password);
  await page.getByRole('button', { name: /^登\s*录$/ }).click();
  await expect(page.locator('.avatar-container .el-dropdown')).toBeVisible();
  pass('Local Admin login remains available');
  await createProvider();
  for (const [index, registration] of fixture.registrations.entries())
    await createRegistration(registration, index + 1);
  await writeFile(
    join(fixture.evidenceDir, 'management-result.json'),
    JSON.stringify(
      {
        status: 'passed',
        checks,
        provider: { providerKey: fixture.provider.providerKey, name: fixture.provider.name },
        registrations
      },
      null,
      2
    ),
    { mode: 0o600 }
  );
} catch (failure) {
  let reason = failure instanceof Error ? failure.message : 'Unexpected browser failure';
  for (const secret of [fixture.adminLocal.password, ...fixture.registrations.map(item => item.clientSecret)]) {
    if (secret) reason = reason.split(secret).join('[redacted]');
  }
  reason = reason.slice(0, 3000);
  await screenshot('management-failure').catch(() => undefined);
  await writeFile(
    join(fixture.evidenceDir, 'management-result.json'),
    JSON.stringify({ status: 'failed', step, reason, checks, registrations }, null, 2),
    { mode: 0o600 }
  );
  console.error(`FAIL management UI at ${step}: ${reason}`);
  process.exitCode = 1;
} finally {
  await browser.close();
}
