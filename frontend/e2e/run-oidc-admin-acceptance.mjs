/** 真实 Admin 管理验收：只使用自有服务和自有应用，不输出凭据或一次性密钥。 */
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { request as httpRequest } from 'node:http';
import { join } from 'node:path';
import { chromium, expect } from '@playwright/test';

const directory = process.env.OIDC_ACCEPTANCE_DIR;
if (!directory?.startsWith('/tmp/namewta-oidc-real-')) throw new Error('Owned acceptance directory required');
const credentials = JSON.parse(await readFile(join(directory, 'browser-credentials.json'), 'utf8'));
const origin = 'https://admin.localhost:19441';
const evidence = [];
const pass = label => { evidence.push(label); console.log(`PASS ${label}`); };
const requireValue = (value, label) => { if (!value) throw new Error(label); };
let token;
let ownedId;
let browser;
let step = 'Admin login';

async function api(path, body) {
  return new Promise((resolve, reject) => {
    const request = httpRequest({ hostname: '127.0.0.1', port: 18888, path, method: body ? 'POST' : 'GET',
      headers: { clientid: credentials.clientId, 'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}) } }, response => {
      const chunks = [];
      response.on('data', value => chunks.push(value));
      response.on('end', () => {
        try {
          const result = JSON.parse(Buffer.concat(chunks).toString());
          requireValue(response.statusCode === 200 && (result.code === 200 || (result.code === undefined && Array.isArray(result.rows))), 'Owned admin API request');
          resolve(result);
        } catch { reject(new Error('Owned admin API request failed')); }
      });
    });
    request.setTimeout(15000, () => request.destroy(new Error('Owned admin API timeout')));
    request.on('error', reject);
    request.end(body ? JSON.stringify(body) : undefined);
  });
}

try {
  token = (await api('/auth/login', { ...credentials, grantType: 'password' })).data.access_token;
  requireValue(token, 'Admin session');
  const fields = (await api('/oidc/admin/fields')).data;
  const sensitive = fields.filter(field => field.sensitive);
  requireValue(sensitive.length === 2, 'Two explicitly authorized full identity fields');
  browser = await chromium.launch({ channel: 'chrome', args: ['--host-resolver-rules=MAP *.localhost 127.0.0.1'] });
  const context = await browser.newContext({ ignoreHTTPSErrors: true, viewport: { width: 1440, height: 1000 } });
  context.setDefaultTimeout(15000);
  await context.addInitScript(value => localStorage.setItem('Admin-Token', value), token);
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', () => errors.push('pageerror'));
  page.on('console', message => { if (message.type() === 'error') errors.push('console.error'); });
  await page.goto(`${origin}/system/oidcApp`);
  await expect(page.getByRole('heading', { name: '单点登录', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '创建应用', exact: true })).toBeEnabled();
  pass('Real permission/menu route opens application management');

  step = 'Create and ordinary disclosure selection';
  await page.getByRole('button', { name: '创建应用', exact: true }).click();
  const create = page.getByRole('dialog', { name: '创建应用', exact: true });
  const name = `UI acceptance ${Date.now()}`;
  await create.getByLabel('应用名称', { exact: true }).fill(name);
  await create.getByLabel('登录回调地址', { exact: true }).fill('https://rp1.localhost:19444/callback');
  await create.getByRole('button', { name: '可获取的用户信息' }).click();
  for (const button of await create.getByRole('button', { name: '选择常用字段', exact: true }).all()) await button.click();
  for (const label of ['账户资料', '个人认证资料', '企业认证资料']) await expect(create.locator('legend').filter({ hasText: label })).toBeVisible();
  for (const field of sensitive) await expect(create.getByRole('checkbox', { name: new RegExp(`^${field.label}\\s*（单独授权）$`) })).not.toBeChecked();
  for (const field of fields.filter(field => !field.sensitive)) await expect(create.getByRole('checkbox', { name: field.label, exact: true })).toBeChecked();
  const artifacts = join(directory, 'admin-ui-evidence');
  await mkdir(artifacts, { recursive: true, mode: 0o700 });
  for (const dark of [false, true]) {
    await page.evaluate(value => document.documentElement.classList.toggle('dark', value), dark);
    for (const width of [320, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      await create.evaluate(node => Promise.allSettled(node.getAnimations({ subtree: true }).map(animation => animation.finished)));
      requireValue(await create.evaluate(node => node.scrollWidth <= node.clientWidth + 1), 'Responsive application dialog has no horizontal overflow');
      const contrasts = await create.evaluate(node => {
        const rgb = value => (value.match(/[\d.]+/g) ?? []).map(Number).slice(0, 3);
        const luminance = value => rgb(value).map(channel => channel / 255).map(channel => channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4)
          .reduce((sum, channel, index) => sum + channel * [0.2126, 0.7152, 0.0722][index], 0);
        return [...node.querySelectorAll('.el-checkbox.is-checked .el-checkbox__label, .el-button--primary.is-link, .form-hint, .field-hint')].map(label => {
          let parent = label;
          let backgroundColor = 'rgb(255, 255, 255)';
          while (parent) {
            const color = getComputedStyle(parent).backgroundColor;
            if (!color.startsWith('rgba') || color.endsWith(', 1)')) { backgroundColor = color; break; }
            parent = parent.parentElement;
          }
          const background = luminance(backgroundColor);
          const foreground = luminance(getComputedStyle(label).color);
          return { selector: label.className, color: getComputedStyle(label).color, background: backgroundColor, ratio: (Math.max(foreground, background) + 0.05) / (Math.min(foreground, background) + 0.05) };
        });
      });
      requireValue(contrasts.length > 0 && contrasts.every(value => value.ratio >= 4.5), `Checked labels and field links meet rendered text contrast: ${JSON.stringify(contrasts.filter(value => value.ratio < 4.5).slice(0, 4))}`);
      await create.screenshot({ path: join(artifacts, `application-fields-${dark ? 'dark' : 'light'}-${width}.png`) });
    }
  }
  await page.evaluate(() => document.documentElement.classList.remove('dark'));
  const created = page.waitForResponse(response => response.url().endsWith('/oidc/admin/applications/create') && response.request().method() === 'POST');
  await create.getByRole('button', { name: '创建并获取配置', exact: true }).click();
  const delivery = (await (await created).json()).data;
  requireValue(delivery?.application?.applicationId && delivery.clientSecret, 'Created application delivery');
  ownedId = delivery.application.applicationId;
  requireValue(sensitive.every(field => !delivery.application.allowedFields.includes(field.key)), 'Bulk disclosure excludes both sensitive fields');
  pass('Creation with name/callback, ordinary bulk selection excludes both sensitive fields, responsive light/dark dialog');

  step = 'One-time creation secret';
  const configuration = page.getByRole('dialog', { name: '应用接入配置', exact: true });
  await expect(configuration.getByLabel('Client Secret（仅本次显示）', { exact: true })).toBeVisible();
  requireValue(await configuration.getByLabel('Client Secret（仅本次显示）', { exact: true }).inputValue() === delivery.clientSecret, 'One-time secret shown after create');
  await configuration.getByRole('button', { name: '完成', exact: true }).click();
  await expect(configuration).toBeHidden();
  const secretAbsent = value => !document.documentElement.innerHTML.includes(value) && ![...document.querySelectorAll('input')].some(input => input.value.includes(value));
  requireValue(await page.evaluate(secretAbsent, delivery.clientSecret), 'Closed secret removed from DOM and inputs');
  const row = () => page.getByRole('row').filter({ has: page.getByText(name, { exact: true }) });
  await row().getByRole('button', { name: '接入配置', exact: true }).click();
  await expect(configuration).toBeVisible();
  await expect(configuration.getByLabel('Client Secret（仅本次显示）', { exact: true })).toHaveCount(0);
  requireValue(!JSON.stringify(await api(`/oidc/admin/applications/${ownedId}`)).includes(delivery.clientSecret), 'Read endpoint does not redeliver secret');
  await configuration.screenshot({ path: join(artifacts, 'configuration-after-close.png') });
  await configuration.getByRole('button', { name: '完成', exact: true }).click();
  pass('Creation secret disappears after close and is absent when configuration reopens');

  step = 'Edit and explicit sensitive authorization';
  await row().getByRole('button', { name: '编辑', exact: true }).click();
  const edit = page.getByRole('dialog', { name: '编辑应用', exact: true });
  await expect(edit).toBeVisible();
  await edit.getByLabel('登录回调地址', { exact: true }).fill('https://rp1.localhost:19444/callback\nhttps://rp2.localhost:19445/callback');
  const disclosure = edit.getByRole('button', { name: '可获取的用户信息' });
  const explicitField = edit.getByRole('checkbox', { name: new RegExp(`^${sensitive[0].label}\\s*（单独授权）$`) });
  await edit.evaluate(node => Promise.allSettled(node.getAnimations({ subtree: true }).map(animation => animation.finished)));
  if (await disclosure.getAttribute('aria-expanded') !== 'true') await disclosure.click();
  // Element 的原生 input 为零尺寸，由关联 label 提供实际点击区域。
  await edit.locator('.el-checkbox').filter({ hasText: sensitive[0].label }).click();
  await expect(explicitField).toBeChecked();
  await edit.getByRole('button', { name: '保存', exact: true }).click();
  await expect(edit).toBeHidden();
  const edited = (await api(`/oidc/admin/applications/${ownedId}`)).data;
  requireValue(edited.redirectUris.length === 2 && edited.allowedFields.includes(sensitive[0].key) && !edited.allowedFields.includes(sensitive[1].key), 'Explicit field and callback changes persisted');
  pass('Edit persists callback list and individually selected sensitive field');

  step = 'Disable and enable';
  const confirm = async () => page.getByRole('dialog', { name: '系统提示', exact: true }).getByRole('button', { name: '确定', exact: true }).click();
  for (const enabled of [false, true]) {
    await row().getByRole('button', { name: enabled ? '启用' : '停用', exact: true }).click();
    await confirm();
    await expect(row().getByRole('button', { name: enabled ? '停用' : '启用', exact: true })).toBeVisible();
    requireValue((await api(`/oidc/admin/applications/${ownedId}`)).data.enabled === enabled, 'Status change persisted');
  }
  pass('Disable and re-enable persist and update row actions');

  step = 'Rotate secret';
  await row().getByRole('button', { name: '重置密钥', exact: true }).click();
  const rotatedResponse = page.waitForResponse(response => response.url().endsWith(`/${ownedId}/rotate-secret`) && response.request().method() === 'POST');
  await confirm();
  const rotated = (await (await rotatedResponse).json()).data;
  requireValue(rotated?.clientSecret && rotated.clientSecret !== delivery.clientSecret, 'Rotation creates new secret');
  await expect(configuration.getByLabel('Client Secret（仅本次显示）', { exact: true })).toBeVisible();
  await configuration.getByRole('button', { name: '完成', exact: true }).click();
  await expect(configuration).toBeHidden();
  requireValue(await page.evaluate(secretAbsent, rotated.clientSecret), 'Rotated secret removed after closing');
  await row().getByRole('button', { name: '接入配置', exact: true }).click();
  await expect(configuration.getByLabel('Client Secret（仅本次显示）', { exact: true })).toHaveCount(0);
  await configuration.getByRole('button', { name: '完成', exact: true }).click();
  pass('Rotation delivers a new secret once and clears it on close');

  step = 'Delete owned application';
  await row().getByRole('button', { name: '删除', exact: true }).click();
  const deletedId = ownedId;
  const deletedResponse = page.waitForResponse(response => response.url().endsWith(`/${deletedId}/delete`) && response.request().method() === 'POST');
  await confirm();
  requireValue((await (await deletedResponse).json()).code === 200, 'Owned deletion response');
  ownedId = undefined;
  await expect(row()).toHaveCount(0);
  const listing = await api(`/oidc/admin/applications?pageNum=1&pageSize=20&name=${encodeURIComponent(name)}`);
  requireValue(!listing.rows.some(application => application.applicationId === deletedId), 'Owned application deleted');
  pass('Deletion removes the owned application from UI and persisted list');
  requireValue(errors.length === 0, `Browser console/page errors: ${errors.length}`);
  pass('No browser console errors or uncaught page errors');
  await writeFile(join(artifacts, 'verification.json'), JSON.stringify({ passed: evidence, browserErrors: errors.length }, null, 2));
} catch (error) {
  console.error(`FAIL ${step}: ${error instanceof Error ? error.message : 'Unknown failure'}`);
  process.exitCode = 1;
} finally {
  if (ownedId) {
    try {
      const application = (await api(`/oidc/admin/applications/${ownedId}`)).data;
      await api(`/oidc/admin/applications/${ownedId}/delete`, { version: application.version });
    } catch { console.error('Owned application cleanup failed'); process.exitCode = 1; }
  }
  await browser?.close();
}
