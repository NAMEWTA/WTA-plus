import { expect, test, type Page, type Route } from '@playwright/test';

const task = (id: string) => ({ id, instanceId: `instance-${id}`, businessId: `business-${id}`, businessCode: 'LEAVE',
  businessTitle: '请假', flowCode: 'leave', flowName: `审批-${id}`, flowStatus: 'waiting', formCustom: 'N', formPath: '/leave',
  nodeCode: 'approval', nodeName: '审批', nodeRatio: 1, nodeType: 1, copyList: [],
  buttonList: ['back', 'subSign', 'termination', 'trust', 'transfer', 'addSign'].map(code => ({ code, show: true })) });
const reply = (route: Route, data: unknown, code = 200) => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ code, data, msg: code === 200 ? '' : '任务 B 加载失败' }) });
async function fixture(page: Page) {
  const writes: { path: string; body: unknown }[] = [];
  const held = new Map<string, Route>(); const hold = new Set<string>(); const fail = new Set<string>();
  const errors: string[] = []; page.on('pageerror', error => errors.push(error.message));
  await page.route('**/workflow/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (!path.startsWith('/workflow/')) return route.continue();
    const data = route.request().postDataJSON();
    if (hold.has(path)) { held.set(path, route); return; }
    if (fail.has(path)) return reply(route, undefined, 500);
    if (path.startsWith('/workflow/task/getTask/')) return reply(route, task(path.split('/').at(-1)!));
    if (path === '/workflow/task/getNextNodeList') return reply(route, []);
    if (path.startsWith('/workflow/task/getBackTaskNode/')) return reply(route, [{ nodeCode: 'start', nodeName: '申请人' }]);
    if (path.startsWith('/workflow/task/currentTaskAllUser/')) return reply(route, [{ userId: '7', nickName: '审批人' }]);
    writes.push({ path, body: data }); return reply(route, null);
  });
  await page.goto('/packages/web-domains/workflow/src/components/fixtures/task-integrity/index.html');
  const dialog = page.getByRole('dialog', { name: '流程办理', exact: true });
  const open = async (id: string) => { await page.getByRole('button', { name: `打开 ${id}`, exact: true }).click(); };
  const ready = async (id: string) => { await expect(dialog.getByText(`审批-${id}`, { exact: true })).toBeVisible(); };
  const release = async (path: string, data: unknown) => { await expect.poll(() => held.has(path)).toBe(true); await reply(held.get(path)!, data); held.delete(path); };
  return { writes, held, hold, fail, errors, dialog, open, ready, release };
}

test('B loading failure removes A and recovers through a fresh B load', async ({ page }) => {
  const f = await fixture(page); await f.open('A'); await f.ready('A');
  f.fail.add('/workflow/task/getTask/B'); await f.open('B');
  await expect(f.dialog.getByText('任务 B 加载失败')).toBeVisible();
  await expect(f.dialog.getByText('审批-A', { exact: true })).toHaveCount(0);
  await expect(f.dialog.getByRole('button', { name: '提交', exact: true })).toHaveCount(0);
  expect(f.writes).toEqual([]); f.fail.clear(); await f.open('B'); await f.ready('B');
  await f.dialog.getByLabel('审批意见').fill('仅办理 B');
  await f.dialog.getByRole('button', { name: '提交', exact: true }).click();
  await page.locator('.el-message-box').getByRole('button', { name: '确定', exact: true }).click();
  await expect.poll(() => f.writes.length).toBe(1);
  expect(f.writes[0].body).toMatchObject({ taskId: 'B', message: '仅办理 B' }); expect(f.errors).toEqual([]);
});

test('out-of-order A cannot replace B or enable stale actions', async ({ page }) => {
  const f = await fixture(page); f.hold.add('/workflow/task/getTask/A');
  await f.open('A'); await expect.poll(() => f.held.has('/workflow/task/getTask/A')).toBe(true);
  await f.open('B'); await f.ready('B'); await f.release('/workflow/task/getTask/A', task('A'));
  await expect(f.dialog.getByText('审批-B', { exact: true })).toBeVisible();
  await expect(f.dialog.getByText('审批-A', { exact: true })).toHaveCount(0); expect(f.writes).toEqual([]); expect(f.errors).toEqual([]);
});

for (const action of ['提交', '终止', '退回']) {
  test(`${action} confirmation cannot mutate either task after an A/B switch`, async ({ page }) => {
    const f = await fixture(page); await f.open('A'); await f.ready('A');
    await f.dialog.getByRole('button', { name: action, exact: true }).click();
    if (action === '退回') await page.getByRole('dialog', { name: '退回任务' }).getByRole('button', { name: '确认退回' }).click();
    const confirm = page.locator('.el-message-box'); await expect(confirm).toBeVisible();
    await f.open('B'); await f.ready('B'); await confirm.getByRole('button', { name: '确定', exact: true }).click();
    await expect(confirm).toBeHidden(); await expect(f.dialog).toBeVisible();
    expect(f.writes).toEqual([]); expect(f.errors).toEqual([]);
  });
}
for (const action of ['关闭任务', '卸载任务']) {
  test(`${action} invalidates the outstanding confirmation`, async ({ page }) => {
    const f = await fixture(page); await f.open('A'); await f.ready('A');
    await f.dialog.getByRole('button', { name: '提交', exact: true }).click();
    const confirm = page.locator('.el-message-box'); await expect(confirm).toBeVisible();
    await page.getByRole('button', { name: action, exact: true }).click();
    await confirm.getByRole('button', { name: '确定', exact: true }).click();
    await expect(f.dialog).toBeHidden(); expect(f.writes).toEqual([]); expect(f.errors).toEqual([]);
  });
}

test('two synchronous submit clicks create one confirmation and one request', async ({ page }) => {
  const f = await fixture(page); await f.open('A'); await f.ready('A');
  await f.dialog.getByRole('button', { name: '提交', exact: true }).evaluate((element: HTMLButtonElement) => { element.click(); element.click(); });
  await expect(page.locator('.el-message-box')).toHaveCount(1);
  await page.locator('.el-message-box').getByRole('button', { name: '确定', exact: true }).click();
  await expect(f.dialog).toBeHidden(); expect(f.writes).toHaveLength(1); expect(f.errors).toEqual([]);
});

test('an old successful request cannot close the new task dialog', async ({ page }) => {
  const f = await fixture(page); await f.open('A'); await f.ready('A'); f.hold.add('/workflow/task/completeTask');
  await f.dialog.getByRole('button', { name: '提交', exact: true }).click();
  await page.locator('.el-message-box').getByRole('button', { name: '确定', exact: true }).click();
  await expect.poll(() => f.held.has('/workflow/task/completeTask')).toBe(true);
  expect(f.held.get('/workflow/task/completeTask')!.request().postDataJSON()).toMatchObject({ taskId: 'A' });
  await f.open('B'); await f.ready('B'); await f.release('/workflow/task/completeTask', null);
  await expect(f.dialog.getByText('审批-B', { exact: true })).toBeVisible();
  await expect(page.getByLabel('完成次数')).toHaveText('0'); expect(f.errors).toEqual([]);
});

test('old back-node response cannot open a secondary dialog for B', async ({ page }) => {
  const f = await fixture(page); await f.open('A'); await f.ready('A'); f.hold.add('/workflow/task/getBackTaskNode/A/approval');
  await f.dialog.getByRole('button', { name: '退回', exact: true }).click();
  await expect.poll(() => f.held.has('/workflow/task/getBackTaskNode/A/approval')).toBe(true);
  await f.open('B'); await f.ready('B'); await f.release('/workflow/task/getBackTaskNode/A/approval', [{ nodeCode: 'start', nodeName: '旧申请人' }]);
  await expect(page.getByRole('dialog', { name: '退回任务' })).toBeHidden(); expect(f.writes).toEqual([]); expect(f.errors).toEqual([]);
});
