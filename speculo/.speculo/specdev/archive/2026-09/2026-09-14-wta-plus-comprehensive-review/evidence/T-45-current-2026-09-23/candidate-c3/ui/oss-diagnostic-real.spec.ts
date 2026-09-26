import { readFileSync } from 'node:fs';
import { expect, test, type Page, type Response } from '@playwright/test';

const TIMEOUT = 20_000;
const ADMIN_CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e';

interface Manifest {
  schema_version: 1;
  run_id: string;
  private_key: string;
  public_key: string;
  private_id: string;
  public_id: string;
}

function required(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error(`Missing owned T45 input: ${name}`);
  return value;
}

function manifest(): Manifest {
  const value: unknown = JSON.parse(readFileSync(required('T45_UI_MANIFEST'), 'utf8'));
  if (!value || typeof value !== 'object') throw new Error('Invalid owned T45 manifest');
  const item = value as Partial<Manifest>;
  if (item.schema_version !== 1 || item.run_id !== required('T45_RUN_ID') ||
      item.private_key !== 'minio' || item.public_key !== 'image' ||
      !/^\d+$/.test(item.private_id ?? '') || !/^\d+$/.test(item.public_id ?? '')) {
    throw new Error('Owned T45 manifest identity differs');
  }
  return item as Manifest;
}

async function login(page: Page, username: string, password: string) {
  await page.goto(new URL('/login', required('T45_ADMIN_ORIGIN')).toString());
  const form = page.locator('.login-form');
  await expect(form.locator('input').first()).toBeEnabled({ timeout: TIMEOUT });
  await form.locator('input').first().fill(username);
  await form.locator('input[type="password"]').fill(password);
  await form.locator('.submit-button').click();
  await expect(page.locator('.avatar-wrapper')).toBeVisible({ timeout: TIMEOUT });
}

async function diagnoseRow(page: Page, key: string, id: string, expectedObject: '允许 / 可读' | '拒绝') {
  const row = page.getByRole('row').filter({ has: page.getByRole('cell', { name: key, exact: true }) });
  await expect(row).toHaveCount(1);
  const responsePromise = page.waitForResponse(response => {
    const url = new URL(response.url());
    return response.request().method() === 'POST' &&
      url.pathname.endsWith(`/resource/oss/config/diagnose/${id}`);
  }, { timeout: TIMEOUT });
  await row.getByRole('button', { name: '只读诊断' }).click();
  const response: Response = await responsePromise;
  expect(response.status()).toBe(200);
  const body: unknown = await response.json();
  if (!body || typeof body !== 'object') throw new Error('Diagnostic HTTP shape differs');
  const result = body as { code?: number; data?: { facts?: unknown[] } };
  expect(result.code).toBe(200);
  expect(result.data?.facts).toHaveLength(6);
  const dialog = page.getByRole('dialog', { name: 'OSS 访问诊断' });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByText('单对象读取不证明全桶安全', { exact: false })).toBeVisible();
  for (const subject of ['诊断对象匿名 HEAD', '诊断对象匿名 GET']) {
    const factRow = dialog.getByRole('row').filter({ hasText: subject });
    await expect(factRow).toHaveCount(1);
    await expect(factRow.getByRole('cell', { name: expectedObject, exact: true })).toBeVisible();
  }
  for (const subject of ['策略对象读取声明', '策略危险写声明',
                          '桶 ACL 列举声明', '桶 ACL 写入或修改声明']) {
    const factRow = dialog.getByRole('row').filter({ hasText: subject });
    await expect(factRow).toHaveCount(1);
    await expect(factRow.getByRole('cell', { name: '未知', exact: true })).toBeVisible();
  }
  // Exact claims only: the correct caveat above contains the words “全桶安全”.
  await expect(dialog.getByText('全桶安全', { exact: true })).toHaveCount(0);
  await expect(dialog.getByText('匿名写已禁止', { exact: true })).toHaveCount(0);
  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
}

test('T-45 real Admin sees scoped public and private diagnostic facts', async ({ page }) => {
  test.setTimeout(120_000);
  const owned = manifest();
  await login(page, required('T45_CONTROL_USERNAME'), required('T45_CONTROL_PASSWORD'));
  await page.goto(new URL('/system/oss-config/index', required('T45_ADMIN_ORIGIN')).toString());
  await expect(page.getByRole('heading', { name: 'OSS 配置' })).toBeVisible({ timeout: TIMEOUT });
  await diagnoseRow(page, owned.public_key, owned.public_id, '允许 / 可读');
  await diagnoseRow(page, owned.private_key, owned.private_id, '拒绝');
});

test('T-45 real ordinary and anonymous identities cannot diagnose', async ({ page, request }) => {
  test.setTimeout(90_000);
  const owned = manifest();
  await login(page, required('T45_ORDINARY_USERNAME'), required('T45_ORDINARY_PASSWORD'));
  await page.goto(new URL('/system/oss-config/index', required('T45_ADMIN_ORIGIN')).toString());
  await expect(page.getByRole('button', { name: '只读诊断' })).toHaveCount(0);
  const ordinary = await page.evaluate(async ({ id, clientId }) => {
    const token = localStorage.getItem('Admin-Token');
    if (!token) throw new Error('Owned ordinary login has no token');
    const response = await fetch(`/prod-api/resource/oss/config/diagnose/${id}`, {
      method: 'POST', headers: { Authorization: `Bearer ${token}`, clientid: clientId }
    });
    const body: unknown = await response.json();
    return { status: response.status, code: (body as { code?: number }).code };
  }, { id: owned.private_id, clientId: ADMIN_CLIENT_ID });
  expect(ordinary).toEqual({ status: 200, code: 403 });
  const anonymous = await request.post(new URL(
    `/prod-api/resource/oss/config/diagnose/${owned.private_id}`,
    required('T45_ADMIN_ORIGIN')).toString(), { headers: { clientid: ADMIN_CLIENT_ID } });
  const anonymousBody: unknown = await anonymous.json();
  expect(anonymous.status()).toBe(200);
  expect((anonymousBody as { code?: number }).code).toBe(401);
});
