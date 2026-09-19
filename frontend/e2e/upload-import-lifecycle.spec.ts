import { expect, test, type Page, type Route } from '@playwright/test';

const required = (key: string) => { const value = process.env[key]; if (!value) throw new Error(`Owned fixture required: ${key}`); return value; };
const origin = () => required('UPLOAD_TEST_ORIGIN');
const json = (route: Route, body: unknown) => route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });
const textBytes = Buffer.from('owned upload bytes');
const imageBytes = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9ioAAAAASUVORK5CYII=', 'base64');
const menus = [
  { path: '/owned-users', name: 'OwnedUsers', component: 'system/user/index', meta: { title: '用户管理' } },
  { path: '/owned-oss', name: 'OwnedOss', component: 'system/oss/index', meta: { title: '文件管理' } },
  { path: '/owned-definitions', name: 'OwnedDefinitions', component: 'workflow/processDefinition/index', meta: { title: '流程定义' } }
];
function fixture() {
  return { importMode: 'ok' as 'ok' | 'business' | 'network' | '401' | 'http401' | 'hold',
    imports: [] as Array<{ multipart: boolean; bytes: boolean; currentToken: boolean; updateSupport: string | null }>,
    heldImport: undefined as Route | undefined, previewFailure: true, image: false, initCount: 0, completeCount: 0,
    holdComplete: false, heldComplete: undefined as Route | undefined, deletes: 0, unknown: [] as string[] };
}
type State = ReturnType<typeof fixture>;
async function install(page: Page, state: State) {
  await page.addInitScript(() => {
    localStorage.setItem('Admin-Token', 'owned-import-old');
    const active = new Set<string>();
    const create = URL.createObjectURL.bind(URL); const revoke = URL.revokeObjectURL.bind(URL);
    URL.createObjectURL = value => { const url = create(value); active.add(url); return url; };
    URL.revokeObjectURL = value => { active.delete(value); revoke(value); };
    Object.defineProperty(window, '__ownedBlobCount', { get: () => active.size });
  });
  await page.route('**/prod-api/**', route => {
    const request = route.request(); const url = new URL(request.url()); const path = url.pathname.replace('/prod-api', '');
    if (path === '/system/user/getInfo') return json(route, { code: 200, data: { user: { userId: 7, userName: 'owned', nickName: 'Owned', avatarUrl: '' }, roles: [], permissions: ['system:user:list', 'system:user:import', 'system:oss:list', 'system:oss:upload', 'workflow:definition:list', 'workflow:definition:import'] } });
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: menus });
    if (path === '/auth/client/context') return json(route, { code: 200, data: { clientEnabled: true, registerEnabled: false, passwordPolicy: { minimumLength: 8, maximumLength: 30, requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT'], allowedSpecialCharacters: '!' } } });
    if (path === '/auth/code') return json(route, { code: 200, data: { captchaEnabled: false } });
    if (path === '/auth/logout') return json(route, { code: 200 });
    if (path === '/resource/message/ticket') return json(route, { code: 200, data: 'owned-stream' });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    if (path === '/resource/message/close' || path === '/notify/inbox' || path.startsWith('/system/dict/data/type/')) return json(route, { code: 200, data: [] });
    if (['/system/user/deptTree', '/system/userType/options'].includes(path)) return json(route, { code: 200, data: [] });
    if (path.startsWith('/system/config/configKey/')) return json(route, { code: 200, data: 'false' });
    if (['/system/user/list', '/system/client/list', '/resource/oss/list', '/workflow/definition/list', '/workflow/definition/unPublishList'].includes(path)) return json(route, { code: 200, data: { rows: [], total: 0 } });
    if (path === '/workflow/category/categoryTree') return json(route, { code: 200, data: [{ id: '9', parentId: '0', label: '审批', children: [] }] });
    if (path === '/system/user/importData' || path === '/workflow/definition/importDef') {
      const body = request.postDataBuffer()?.toString('utf8') ?? '';
      state.imports.push({ multipart: Boolean(request.headers()['content-type']?.includes('multipart/form-data; boundary=')), bytes: body.includes('owned-import-bytes'), currentToken: request.headers()['authorization'] === 'Bearer owned-import-new', updateSupport: url.searchParams.get('updateSupport') });
      if (state.importMode === 'hold') { state.heldImport = route; return; }
      if (state.importMode === 'network') return route.abort('internetdisconnected');
      if (state.importMode === 'http401') return route.fulfill({ status: 401, contentType: 'application/json', body: '{"msg":"owned session expired"}' });
      if (state.importMode !== 'ok') return json(route, { code: state.importMode === '401' ? 401 : 500, msg: 'owned import rejected' });
      return json(route, { code: 200, msg: '导入完成', data: null });
    }
    const kind = state.image ? 'IMAGE' : 'FILE';
    const fileName = state.image ? 'owned.png' : 'owned.txt';
    if (path === '/resource/oss/uploads') {
      state.initCount++;
      return json(route, { code: 200, data: { uploadToken: 'owned-upload', mode: 'SINGLE', expiresAt: '2099-01-01T00:00:00Z', presignedRequest: { method: 'PUT', url: required('UPLOAD_TEST_PUT_' + kind), requiredHeaders: {}, expiresAt: '2099-01-01T00:00:00Z' } } });
    }
    if (path === '/resource/oss/uploads/owned-upload/complete') {
      state.completeCount++;
      if (state.holdComplete) { state.heldComplete = route; return; }
      return json(route, { code: 200, data: 'owned-oss-id' });
    }
    if (path.endsWith('/download-url')) {
      if (state.previewFailure) return json(route, { code: 500, msg: 'owned preview unavailable' });
      return json(route, { code: 200, data: { accessType: 'PRIVATE', url: required('UPLOAD_TEST_GET_' + kind), expiresAt: '2099-01-01T00:00:00Z', fileName } });
    }
    if (path.startsWith('/resource/oss/listByIds/')) return json(route, { code: 200, data: [{ ossId: 'owned-oss-id', originalName: fileName, url: '' }] });
    if (path === '/resource/oss/uploads/owned-upload' && request.method() === 'POST') return json(route, { code: 200 });
    if (request.method() === 'POST' && path.startsWith('/resource/oss/')) { state.deletes++; return json(route, { code: 500, msg: 'owned reference protected' }); }
    state.unknown.push(request.method() + ' ' + path);
    return json(route, { code: 200, data: [] });
  });
}
async function openImport(page: Page) {
  await page.goto(origin() + '/owned-users');
  await page.getByRole('button', { name: '更多', exact: true }).hover();
  await page.getByText('导入数据', { exact: true }).click();
  return page.getByRole('dialog', { name: '用户导入' });
}
const workbook = { name: 'users.xlsx', mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', buffer: Buffer.from('owned-import-bytes') };

for (const failure of ['network', 'business', '401', 'http401'] as const) {
  test(`T-18 user import ${failure} resets and retries through current authentication`, async ({ page }) => {
    const state = fixture(); state.importMode = failure; await install(page, state);
    const dialog = await openImport(page);
    await page.evaluate(() => localStorage.setItem('Admin-Token', 'owned-import-new'));
    await dialog.locator('input[type="file"]').setInputFiles(workbook);
    await dialog.getByRole('button', { name: '确 定' }).click();
    if (failure === '401' || failure === 'http401') {
      await expect(page.locator('.el-message-box')).toBeVisible();
      await page.locator('.el-message-box').getByRole('button', { name: '取消' }).click();
    }
    await expect(dialog.getByRole('alert')).toBeVisible();
    await expect(dialog.getByRole('button', { name: '确 定' })).toBeEnabled();
    expect(state.imports[0]).toEqual({ multipart: true, bytes: true, currentToken: true, updateSupport: 'false' });
    state.importMode = 'ok';
    await dialog.locator('.el-checkbox').click();
    await dialog.locator('input[type="file"]').setInputFiles(workbook);
    await dialog.getByRole('button', { name: '确 定' }).click();
    await expect(dialog).toBeHidden();
    await expect(page.getByText('导入完成', { exact: true })).toBeVisible();
    expect(state.imports).toHaveLength(2); expect(state.imports[1]?.updateSupport).toBe('true');
    expect(state.unknown).toEqual([]);
  });
}

test('T-18 user import cancellation stops the request and a fresh dialog can retry', async ({ page }) => {
  const state = fixture(); state.importMode = 'hold'; await install(page, state);
  const dialog = await openImport(page);
  await dialog.locator('input[type="file"]').setInputFiles(workbook);
  await dialog.getByRole('button', { name: '确 定' }).click();
  await expect.poll(() => Boolean(state.heldImport)).toBe(true);
  await expect(dialog.getByRole('button', { name: '确 定' })).toBeDisabled();
  const cancelled = page.waitForEvent('requestfailed', request => request.url().includes('/system/user/importData'));
  await dialog.getByRole('button', { name: '取 消' }).click();
  await cancelled; await expect(dialog).toBeHidden();
  state.importMode = 'ok';
  await page.getByRole('button', { name: '更多', exact: true }).hover();
  await page.getByText('导入数据', { exact: true }).click();
  await dialog.locator('input[type="file"]').setInputFiles(workbook);
  await dialog.getByRole('button', { name: '确 定' }).click();
  await expect(dialog).toBeHidden(); expect(state.imports).toHaveLength(2);
});

for (const image of [false, true]) {
  test(`T-18 real MinIO ${image ? 'image' : 'file'} survives preview failure and retries without retransmission`, async ({ page }) => {
    const state = fixture(); state.image = image; state.holdComplete = true; await install(page, state);
    await page.goto(origin() + '/owned-oss');
    const title = image ? '上传图片' : '上传文件'; await page.getByRole('button', { name: title }).click();
    const dialog = page.getByRole('dialog', { name: title });
    await dialog.locator('input[type="file"]').setInputFiles({ name: image ? 'owned.png' : 'owned.txt', mimeType: image ? 'image/png' : 'text/plain', buffer: image ? imageBytes : textBytes });
    await expect.poll(() => Boolean(state.heldComplete)).toBe(true);
    await expect(dialog.getByRole('button', { name: '确 定' })).toBeDisabled();
    await json(state.heldComplete!, { code: 200, data: 'owned-oss-id' });
    await expect(dialog.getByText(/预览暂不可用/).first()).toBeVisible();
    await expect(dialog.getByRole('button', { name: '确 定' })).toBeEnabled();
    await expect.poll(() => page.evaluate(() => Reflect.get(window, '__ownedBlobCount'))).toBe(0);
    state.previewFailure = false;
    await dialog.getByRole('button', { name: '重试预览' }).click();
    await expect(dialog.getByText(/预览暂不可用/)).toHaveCount(0);
    if (image) {
      const preview = dialog.locator('.el-upload-list__item-thumbnail');
      await expect(preview).toHaveAttribute('src', required('UPLOAD_TEST_GET_IMAGE'));
      await expect.poll(() => preview.evaluate(node => node instanceof HTMLImageElement && node.complete && node.naturalWidth > 0)).toBe(true);
    } else await expect(dialog.getByRole('link', { name: 'owned.txt' })).toHaveAttribute('href', required('UPLOAD_TEST_GET_FILE'));
    expect(state.initCount).toBe(1); expect(state.completeCount).toBe(1); expect(state.deletes).toBe(0);
    await dialog.getByRole('button', { name: '确 定' }).click(); await expect(dialog).toBeHidden();
    await expect.poll(() => page.evaluate(() => Reflect.get(window, '__ownedBlobCount'))).toBe(0);
    expect(state.unknown).toEqual([]);
  });
}

test('T-18 workflow import completes error and success callbacks with actual multipart bytes', async ({ page }) => {
  const state = fixture(); state.importMode = 'business'; await install(page, state);
  await page.goto(origin() + '/owned-definitions');
  await page.getByRole('button', { name: '部署流程文件' }).click();
  const dialog = page.getByRole('dialog', { name: '部署流程文件' });
  await dialog.locator('.el-select__wrapper').click();
  await page.locator('.el-select-dropdown:visible').getByText('审批', { exact: true }).click();
  const flow = { name: 'owned.json', mimeType: 'application/json', buffer: Buffer.from('{"name":"owned-import-bytes"}') };
  await dialog.locator('input[type="file"]').setInputFiles(flow);
  await expect(dialog.getByRole('alert')).toHaveText('owned import rejected');
  await expect(dialog.locator('.el-upload-list__item')).toHaveCount(0);
  state.importMode = 'ok';
  await dialog.locator('input[type="file"]').setInputFiles(flow);
  await expect(dialog).toBeHidden(); expect(state.imports).toHaveLength(2);
  expect(state.imports.every(row => row.multipart && row.bytes)).toBe(true);
});

test('T-18 pending image dialog releases its local preview and ignores late completion', async ({ page }) => {
  const state = fixture(); state.image = true; state.holdComplete = true; await install(page, state);
  await page.goto(origin() + '/owned-oss');
  await page.getByRole('button', { name: '上传图片' }).click();
  const dialog = page.getByRole('dialog', { name: '上传图片' });
  await dialog.locator('input[type="file"]').setInputFiles({ name: 'owned.png', mimeType: 'image/png', buffer: imageBytes });
  await expect.poll(() => Boolean(state.heldComplete)).toBe(true);
  await expect.poll(() => page.evaluate(() => Reflect.get(window, '__ownedBlobCount'))).toBe(1);
  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
  await expect.poll(() => page.evaluate(() => Reflect.get(window, '__ownedBlobCount'))).toBe(0);
  await json(state.heldComplete!, { code: 200, data: 'owned-oss-id' });
  await page.getByRole('button', { name: '上传图片' }).click();
  await expect(dialog.locator('.el-upload-list__item')).toHaveCount(0);
  await expect(dialog.getByRole('button', { name: '确 定' })).toBeEnabled();
  expect(state.deletes).toBe(0);
});

test('T-18 workflow import cancellation aborts transport and permits another attempt', async ({ page }) => {
  const state = fixture(); state.importMode = 'hold'; await install(page, state);
  await page.goto(origin() + '/owned-definitions');
  await page.getByRole('button', { name: '部署流程文件' }).click();
  const dialog = page.getByRole('dialog', { name: '部署流程文件' });
  await dialog.locator('.el-select__wrapper').click();
  await page.locator('.el-select-dropdown:visible').getByText('审批', { exact: true }).click();
  const flow = { name: 'owned.json', mimeType: 'application/json', buffer: Buffer.from('{"name":"owned-import-bytes"}') };
  await dialog.locator('input[type="file"]').setInputFiles(flow);
  await expect.poll(() => Boolean(state.heldImport)).toBe(true);
  const cancelled = page.waitForEvent('requestfailed', request => request.url().includes('/workflow/definition/importDef'));
  await dialog.getByRole('button', { name: '关闭此对话框' }).click();
  await cancelled; await expect(dialog).toBeHidden();
  state.importMode = 'ok';
  await page.getByRole('button', { name: '部署流程文件' }).click();
  await dialog.locator('.el-select__wrapper').click();
  await page.locator('.el-select-dropdown:visible').getByText('审批', { exact: true }).click();
  await dialog.locator('input[type="file"]').setInputFiles(flow);
  await expect(dialog).toBeHidden(); expect(state.imports).toHaveLength(2);
});
