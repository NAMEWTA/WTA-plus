import { expect, test, type Page } from '@playwright/test';

const origin = process.env.PROFILE_TEST_ORIGIN;
if (!origin) throw new Error('Owned Profile/MySQL/MinIO fixture is required');
const clientid = '428a8310cd442757ae699df5d894f051';
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9ioAAAAASUVORK5CYII=', 'base64');
const item = (page: Page, label: string) => page.locator('.el-form-item').filter({ has: page.locator('.el-form-item__label', { hasText: new RegExp(`^${label}$`) }) });
const tag = (page: Page, code: string) => page.locator(`[data-material-tag="${code}"]`);
async function session(page: Page, userId: number, denied = false) {
  const response = await page.request.post(`${origin}/prod-api/fixture/session?userId=${userId}&denied=${denied}`, { headers: { clientid } });
  const body = await response.json();
  expect(body.code).toBe(200);
  const token: string = body.data.token;
  await page.addInitScript(value => localStorage.setItem('Home-Token', value), token);
  return { Authorization: `Bearer ${token}`, clientid };
}
async function fill(page: Page, label: string, value: string) {
  const input = item(page, label).locator('input').first();
  await input.fill(value); await input.press('Tab');
}
function identityNumber(sequence: number) {
  const body = `11010119900101${String(sequence).padStart(3, '0')}`;
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
  const sum = [...body].reduce((total, digit, index) => total + Number(digit) * weights[index], 0);
  return body + '10X98765432'[sum % 11];
}
async function person(page: Page, sequence = 123) {
  await page.goto(`${origin}/profile/person`);
  await expect(page.getByRole('heading', { name: '个人认证', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '保存草稿', exact: true })).toBeEnabled();
  await fill(page, '姓名', '材料验证'); await fill(page, '证件号码', identityNumber(sequence));
  await item(page, '性别').locator('.el-select__wrapper').click(); await page.getByRole('option', { name: '男', exact: true }).click();
  await fill(page, '出生日期', '1990-01-01'); await fill(page, '有效期起', '2020-01-01'); await fill(page, '有效期止', '2039-01-01');
}
async function save(page: Page) {
  const saved = page.waitForResponse(response => /\/profile\/(person|enterprise)\/application$/.test(new URL(response.url()).pathname) && response.request().method() === 'POST');
  await page.getByRole('button', { name: '保存草稿', exact: true }).click();
  const body = await (await saved).json(); expect(body.code, body.msg).toBe(200);
  await expect(page.locator('.self-materials').getByText('请先保存草稿，再上传材料。')).toHaveCount(0);
  await expect(page.locator('.self-materials input[type=file]').first()).toBeEnabled();
  return body.data;
}
async function upload(page: Page, code: string, name: string) {
  // Native file input keeps its explicit label; uploaded bytes go directly to owned MinIO.
  const picker = tag(page, code).locator('input[type=file]').last();
  await expect(picker).toBeEnabled();
  await picker.setInputFiles({ name, mimeType: 'image/png', buffer: png });
  await expect(tag(page, code).getByText(name, { exact: true })).toBeVisible();
  await expect(tag(page, code).getByText('已登记', { exact: true })).toBeVisible();
}
async function submit(page: Page) {
  await expect(page.getByRole('button', { name: '提交认证', exact: true })).toBeEnabled();
  await page.getByRole('button', { name: '提交认证', exact: true }).click();
  await page.getByRole('dialog', { name: '确认', exact: true }).getByRole('button', { name: 'OK', exact: true }).click();
  await expect(page.getByText('审核中', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '提交认证', exact: true })).toHaveCount(0);
}
async function enterprise(page: Page, suffix: string, legal: boolean) {
  await page.goto(`${origin}/profile/enterprise`);
  await expect(page.getByRole('button', { name: '保存草稿', exact: true })).toBeEnabled();
  for (const [label, value] of [
    ['企业名称', `材料验证企业${suffix}`], ['统一信用代码', suffix === '5' ? '91310000MA1K123454' : '91310000MA1K123463'], ['企业类型', '有限责任公司'],
    ['成立日期', '2020-01-01'], ['注册地址', '上海市材料测试路'], ['法定代表人', '材料法人'], ['证件号码', '110101199001011237'],
    ['联系人', '材料联系人'], ['联系电话', '13800138000']
  ]) await fill(page, label, value);
  await item(page, '经营范围').locator('textarea').fill('软件服务');
  if (!legal) await item(page, '经办人是法定代表人').locator('.el-switch__core').click();
}

test('个人双面材料：缺项门禁、私有预览、刷新恢复和真实提交', async ({ page }) => {
  const headers = await session(page, 940001);
  await person(page);
  const draft = await save(page);
  await page.getByRole('button', { name: '提交认证', exact: true }).click();
  await expect(tag(page, 'PERSON_ID_CARD_PORTRAIT')).toBeFocused();
  const rejected = await page.request.post(`${origin}/prod-api/profile/person/application/submit`, { headers, data: { expectedVersion: draft.version } });
  expect((await rejected.json()).msg).toBe('MISSING_REQUIRED_MATERIAL:PERSON_ID_CARD_PORTRAIT');
  await upload(page, 'PERSON_ID_CARD_PORTRAIT', 'person-front.png');
  await page.reload();
  await expect(tag(page, 'PERSON_ID_CARD_PORTRAIT').getByText('person-front.png', { exact: true })).toBeVisible();
  await upload(page, 'PERSON_ID_CARD_EMBLEM', 'person-back.png');
  await tag(page, 'PERSON_ID_CARD_PORTRAIT').getByRole('button', { name: '预览', exact: true }).click();
  await expect(page.getByRole('img', { name: '认证材料预览' })).toBeVisible();
  await expect.poll(() => page.getByRole('img', { name: '认证材料预览' }).evaluate((image: HTMLImageElement) => image.complete && image.naturalWidth > 0)).toBe(true);
  await page.getByRole('dialog').getByRole('button', { name: 'Close this dialog' }).click();
  await submit(page);
  await page.reload();
  await expect(page.getByText('审核中', { exact: true })).toBeVisible();
  await expect(page.locator('.self-materials input[type=file]')).toHaveCount(0);
});

for (const legal of [true, false]) test(`企业真实提交，经办人${legal ? '是' : '不是'}法定代表人`, async ({ page }) => {
  await session(page, legal ? 940002 : 940003);
  await enterprise(page, legal ? '5' : '6', legal);
  await save(page);
  await upload(page, 'ENTERPRISE_BUSINESS_LICENSE', `business-${legal}.png`);
  await upload(page, 'ENTERPRISE_LEGAL_REPRESENTATIVE_DOCUMENT', `legal-${legal}.png`);
  if (!legal) {
    await page.getByRole('button', { name: '提交认证', exact: true }).click();
    await expect(tag(page, 'ENTERPRISE_AUTHORIZATION_LETTER')).toBeFocused();
    await upload(page, 'ENTERPRISE_AUTHORIZATION_LETTER', 'authorization.png');
  }
  await submit(page);
});

test('本人上传材料不能被另一账号读取、挂载或移除；缺权限直接请求被拒绝', async ({ page, browser }) => {
  const headers = await session(page, 940004);
  await person(page, 124); const draft = await save(page);
  await upload(page, 'PERSON_ID_CARD_PORTRAIT', 'owned-only.png');
  const path = `/profile/person/materials/WORKING/${draft.personApplicationId}`;
  const list = await (await page.request.get(`${origin}/prod-api${path}`, { headers })).json();
  const reference = list.data.find((value: { attached: boolean }) => value.attached);
  const other = await browser.newPage();
  try {
    const otherHeaders = await session(other, 940005);
    for (const url of [path, `${path}/${reference.materialRefId}/access-url`]) {
      const body = await (await other.request.get(`${origin}/prod-api${url}`, { headers: otherHeaders })).json();
      expect(body.msg).toBe('MATERIAL_ACCESS_DENIED');
    }
    const detached = await (await other.request.post(`${origin}/prod-api${path}/${reference.materialRefId}/detach`, { headers: otherHeaders })).json();
    expect(detached.msg).toBe('MATERIAL_ACCESS_DENIED');
    const attached = await (await other.request.post(`${origin}/prod-api${path}`, { headers: otherHeaders, data: { materialNodeId: reference.materialNodeId, ossId: reference.ossId } })).json();
    expect(attached.msg).toBe('MATERIAL_ACCESS_DENIED');
    await person(other, 125); const otherDraft = await save(other);
    const stolen = await (await other.request.post(materialPath(otherDraft.personApplicationId), { headers: otherHeaders, data: { materialNodeId: reference.materialNodeId, ossId: reference.ossId } })).json();
    expect(stolen.msg).toBe('MATERIAL_OSS_OWNER_MISMATCH');
    const denied = await session(other, 940006, true);
    const forbidden = await (await other.request.get(`${origin}/prod-api/profile/material-tags/requirements?profileType=PERSON&documentTypeCode=CN_RESIDENT_ID`, { headers: denied })).json();
    expect(forbidden.code).toBe(403);
  } finally { await other.close(); }
  await page.reload(); await expect(page.getByText('owned-only.png', { exact: true })).toBeVisible();
});

const front = 'PERSON_ID_CARD_PORTRAIT';
const back = 'PERSON_ID_CARD_EMBLEM';
const materialPath = (id: string) => `${origin}/prod-api/profile/person/materials/WORKING/${id}`;
const barrier = () => {
  let release!: () => void;
  const promise = new Promise<void>(resolve => { release = resolve; });
  return { promise, release };
};

test('直传网络失败与取消均不登记材料，草稿保留且可重新上传', async ({ page }) => {
  const headers = await session(page, 940007);
  await person(page, 127); const draft = await save(page);
  const storage = (url: URL) => url.pathname.startsWith('/t14-profile-');
  await page.route(storage, route => route.request().method() === 'PUT' ? route.abort('failed') : route.continue());
  await tag(page, front).locator('input[type=file]').last().setInputFiles({ name: 'network.png', mimeType: 'image/png', buffer: png });
  await expect(page.locator('.material-error')).toBeVisible();
  await expect(tag(page, front).getByText('已登记', { exact: true })).toHaveCount(0);
  await page.unroute(storage);
  const started = barrier(); const finish = barrier();
  await page.route(storage, async route => {
    if (route.request().method() !== 'PUT') { await route.continue(); return; }
    started.release(); await finish.promise; await route.abort('aborted');
  });
  await tag(page, front).locator('input[type=file]').last().setInputFiles({ name: 'cancel.png', mimeType: 'image/png', buffer: png });
  await started.promise;
  await expect(page.getByRole('button', { name: '提交认证', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: '取消上传', exact: true }).click();
  finish.release(); await page.unrouteAll({ behavior: 'wait' });
  await expect(page.locator('.material-error')).toHaveText('上传已取消');
  const empty = await (await page.request.get(materialPath(draft.personApplicationId), { headers })).json();
  expect(empty.data.filter((row: { attached: boolean }) => row.attached)).toHaveLength(0);
  await page.reload(); await expect(item(page, '姓名').locator('input')).toHaveValue('材料验证');
  await upload(page, front, 'recovered.png');
});

test('登记响应丢失先查询恢复，替换保留旧件至新件登记，移除仅解除引用', async ({ page }) => {
  const headers = await session(page, 940008);
  await person(page, 128); const draft = await save(page);
  const path = materialPath(draft.personApplicationId);
  let attachments = 0;
  await page.route(path, async route => {
    if (route.request().method() !== 'POST') { await route.continue(); return; }
    attachments++;
    const response = await route.fetch(); expect((await response.json()).code).toBe(200);
    await route.abort('failed');
  });
  await tag(page, front).locator('input[type=file]').last().setInputFiles({ name: 'lost-response.png', mimeType: 'image/png', buffer: png });
  await expect(page.getByRole('button', { name: '重试登记', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '提交认证', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: '重试登记', exact: true }).click();
  await expect(tag(page, front).getByText('lost-response.png', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '重试登记', exact: true })).toHaveCount(0);
  expect(attachments).toBe(1);
  await page.unroute(path);
  const before = await (await page.request.get(path, { headers })).json();
  expect(before.data).toHaveLength(1);
  const access = await (await page.request.get(`${path}/${before.data[0].materialRefId}/access-url`, { headers })).json();
  const previewPath = `${path}/${before.data[0].materialRefId}/access-url`;
  await page.route(previewPath, route => route.abort('failed'));
  await tag(page, front).getByRole('button', { name: '预览', exact: true }).click();
  await expect(page.locator('.material-error')).toBeVisible();
  await expect(tag(page, front).getByText('lost-response.png', { exact: true })).toBeVisible();
  await page.unroute(previewPath);
  await tag(page, front).getByRole('button', { name: '预览', exact: true }).click();
  await expect.poll(() => page.getByRole('img', { name: '认证材料预览' }).evaluate((image: HTMLImageElement) => image.complete && image.naturalWidth > 0)).toBe(true);
  await expect(page.locator('.material-error')).toHaveCount(0);
  await page.getByRole('dialog').getByRole('button', { name: 'Close this dialog' }).click();
  const held = barrier(); const resume = barrier();
  await page.route(path, async route => {
    if (route.request().method() === 'POST') { held.release(); await resume.promise; }
    await route.continue();
  });
  await tag(page, front).getByLabel('替换lost-response.png', { exact: true }).setInputFiles({ name: 'replacement.png', mimeType: 'image/png', buffer: png });
  await held.promise;
  await expect(tag(page, front).getByText('lost-response.png', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '提交认证', exact: true })).toBeDisabled();
  resume.release();
  await expect(tag(page, front).getByText('replacement.png', { exact: true })).toBeVisible();
  await expect(tag(page, front).getByText('lost-response.png', { exact: true })).toHaveCount(0);
  await tag(page, front).getByRole('button', { name: '移除', exact: true }).click();
  await expect(tag(page, front).getByText('已登记', { exact: true })).toHaveCount(0);
  const after = await (await page.request.get(path, { headers })).json();
  expect(after.data).toHaveLength(2);
  expect(after.data.every((row: { attached: boolean }) => !row.attached)).toBe(true);
  const originalBytes = await page.request.get(access.data.url);
  expect(originalBytes.ok()).toBe(true); expect(await originalBytes.body()).toEqual(png);
});

test('真实 Redis 票据过期拒绝 complete，刷新草稿并换新票据可恢复', async ({ page }) => {
  const headers = await session(page, 940009);
  await person(page, 129); const draft = await save(page);
  let expired = false;
  await page.route('**/resource/oss/uploads/*/complete', async route => {
    const token = new URL(route.request().url()).pathname.split('/').at(-2)!;
    const response = await page.request.post(`${origin}/prod-api/fixture/expire-ticket`, { headers, params: { token } });
    expect((await response.json()).code).toBe(200); expired = true;
    await route.continue();
  });
  await tag(page, front).locator('input[type=file]').last().setInputFiles({ name: 'expired-ticket.png', mimeType: 'image/png', buffer: png });
  await expect(page.locator('.material-error')).toHaveText('上传会话已过期'); expect(expired).toBe(true);
  const empty = await (await page.request.get(materialPath(draft.personApplicationId), { headers })).json();
  expect(empty.data).toHaveLength(0);
  await page.unrouteAll({ behavior: 'wait' }); await page.reload();
  await upload(page, front, 'expired-ticket.png');
});

test('提交末端故障回滚申请、不可变快照和 OSS 引用，刷新后可重试', async ({ page }) => {
  const headers = await session(page, 940010);
  await person(page, 130); await save(page);
  await upload(page, front, 'rollback-front.png'); await upload(page, back, 'rollback-back.png');
  const state = async () => (await (await page.request.get(`${origin}/prod-api/fixture/transaction-state`, { headers })).json()).data;
  expect(await state()).toEqual({ status: 'DRAFT', submissions: 0, snapshots: 0, ossReferences: 2 });
  const failed = page.waitForResponse(response => response.url().endsWith('/profile/person/application/submit'));
  await page.getByRole('button', { name: '提交认证', exact: true }).click();
  await page.getByRole('dialog', { name: '确认', exact: true }).getByRole('button', { name: 'OK', exact: true }).click();
  expect((await (await failed).json()).msg).toBe('OWNED_WORKFLOW_FAILURE');
  await expect(page.getByRole('button', { name: '提交认证', exact: true })).toBeEnabled();
  expect(await state()).toEqual({ status: 'DRAFT', submissions: 0, snapshots: 0, ossReferences: 2 });
  await page.reload(); await expect(page.getByText('rollback-front.png', { exact: true })).toBeVisible();
  await submit(page);
  expect(await state()).toEqual({ status: 'WAITING', submissions: 1, snapshots: 2, ossReferences: 4 });
});
