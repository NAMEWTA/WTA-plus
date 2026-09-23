import { expect, test, type Page, type Route } from '@playwright/test';

test.use({ viewport: { width: 1440, height: 900 } });

type State = {
  categories: Record<string, unknown>[];
  publishedDefinitions: Record<string, unknown>[];
  unpublishedDefinitions: Record<string, unknown>[];
  definitionRequests: string[];
  exports: { method: string; path: string }[];
  failCategoryAdd: boolean;
  imports: { body: string; contentType: string; method: string; path: string }[];
  spels: Record<string, unknown>[];
  mutations: { method: string; path: string; body: unknown }[];
  unknown: string[];
};
const createState = (overrides: Partial<State> = {}): State => ({
  categories: [{ categoryId: 'c1', parentId: 0, categoryName: '审批', orderNum: 1, createTime: '', children: [] }],
  publishedDefinitions: [
    { id: 'd0', flowName: '既有已发布流程', flowCode: 'published', version: '1', isPublish: 1, activityStatus: 1 }
  ],
  unpublishedDefinitions: [
    { id: 'd1', flowName: '请假审批', flowCode: 'leave', version: '1', isPublish: 0, activityStatus: 1 }
  ],
  definitionRequests: [],
  exports: [],
  failCategoryAdd: false,
  imports: [],
  spels: [
    { id: 's1', componentName: 'owner', methodName: 'resolve', methodParams: '', viewSpel: '#owner', status: '0' }
  ],
  mutations: [],
  unknown: [],
  ...overrides
});
const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });
const menus = [
  {
    path: '/workflow',
    name: 'Workflow',
    component: 'Layout',
    meta: { title: '工作流' },
    children: [
      { path: 'category', name: 'Category', component: 'workflow/category/index', meta: { title: '流程分类' } },
      {
        path: 'processDefinition',
        name: 'processDefinition',
        component: 'workflow/processDefinition/index',
        meta: { title: '流程定义' }
      },
      {
        path: 'design/index',
        name: 'WarmFlow',
        component: 'workflow/processDefinition/design',
        hidden: true,
        meta: { title: '流程设计' }
      },
      { path: 'spel', name: 'Spel', component: 'workflow/spel/index', meta: { title: '流程表达式' } }
    ]
  }
];

async function installApi(page: Page, state: State, permissions: string[]) {
  await page.route('**/prod-api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    const method = request.method();
    if (path.startsWith('/warm-flow-ui/')) return route.fulfill({ contentType: 'text/html; charset=utf-8', body: '<!doctype html><html><body><button onclick="window.parent.postMessage({method: \'close\'}, \'*\')">关闭设计器</button></body></html>' });
    if (path === '/auth/client/context')
      return json(route, { code: 200, data: { clientEnabled: true, registerEnabled: true } });
    if (path === '/auth/code') return json(route, { code: 200, data: { captchaEnabled: false } });
    if (path === '/auth/login') return json(route, { code: 200, data: { access_token: 'workflow-token' } });
    if (path === '/system/user/getInfo')
      return json(route, {
        code: 200,
        data: {
          user: { userId: 1, userName: 'workflow-user', nickName: 'Workflow User', avatarUrl: '' },
          roles: ['operator'],
          permissions
        }
      });
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: menus });
    if (path === '/system/dict/data/type/sys_show_hide')
      return json(route, {
        code: 200,
        data: [
          { dictLabel: '显示', dictValue: '0', listClass: 'primary', cssClass: '' },
          { dictLabel: '隐藏', dictValue: '1', listClass: 'info', cssClass: '' }
        ]
      });
    if (path === '/system/dict/data/type/sys_normal_disable')
      return json(route, {
        code: 200,
        data: [
          { dictLabel: '正常', dictValue: '0', listClass: 'success', cssClass: '' },
          { dictLabel: '停用', dictValue: '1', listClass: 'danger', cssClass: '' }
        ]
      });
    if (path === '/workflow/category/list') return json(route, { code: 200, data: state.categories });
    if (path === '/workflow/category/categoryTree')
      return json(route, {
        code: 200,
        data: state.categories.map(item => ({
          id: item.categoryId,
          parentId: item.parentId,
          label: item.categoryName,
          weight: item.orderNum,
          children: []
        }))
      });
    if (path === '/workflow/definition/list') {
      state.definitionRequests.push(method + ' ' + path);
      return json(route, {
        code: 200,
        data: { rows: state.publishedDefinitions, total: state.publishedDefinitions.length }
      });
    }
    if (path === '/workflow/definition/unPublishList') {
      state.definitionRequests.push(method + ' ' + path);
      return json(route, {
        code: 200,
        data: { rows: state.unpublishedDefinitions, total: state.unpublishedDefinitions.length }
      });
    }
    if (path === '/workflow/spel/list')
      return json(route, { code: 200, data: { rows: state.spels, total: state.spels.length } });
    if (path === '/workflow/category' && method === 'POST') {
      const body = request.postDataJSON();
      if (state.failCategoryAdd) return json(route, { code: 500, msg: '分类保存失败' });
      state.mutations.push({ method, path, body });
      state.categories.push({ ...body, categoryId: 'c2', createTime: '', children: [] });
      return json(route, { code: 200, data: null });
    }
    if (path === '/workflow/definition/publish/d1' && method === 'POST') {
      state.mutations.push({ method, path, body: null });
      state.definitionRequests.push(method + ' ' + path);
      const published = state.unpublishedDefinitions.find(item => item.id === 'd1');
      state.unpublishedDefinitions = state.unpublishedDefinitions.filter(item => item.id !== 'd1');
      if (published) state.publishedDefinitions.push({ ...published, isPublish: 1 });
      return json(route, { code: 200, data: null });
    }
    if (path === '/workflow/definition/importDef' && method === 'POST') {
      state.imports.push({
        method,
        path,
        contentType: request.headers()['content-type'] ?? '',
        body: request.postData() ?? ''
      });
      state.unpublishedDefinitions.push({
        id: 'd2',
        flowName: '导入流程',
        flowCode: 'imported',
        version: '1',
        isPublish: 0,
        activityStatus: 1
      });
      return json(route, { code: 200, data: null });
    }
    if (path === '/workflow/definition/exportDef/d0' && method === 'POST') {
      state.exports.push({ method, path });
      return route.fulfill({ contentType: 'application/octet-stream', body: '{"flowCode":"published"}' });
    }
    if (path === '/workflow/spel' && method === 'POST') {
      const body = request.postDataJSON();
      state.mutations.push({ method, path, body });
      state.spels.push({ ...body, id: 's2' });
      return json(route, { code: 200, data: null });
    }
    if (path === '/notify/inbox')
      return json(route, { code: 200, data: { rows: [], total: 0, unreadTotal: 0 } });
    if (path === '/resource/message/ticket') return json(route, { code: 200, data: 'owned-push-ticket' });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    state.unknown.push(method + ' ' + path);
    return json(route, { code: 200, data: null });
  });
}

async function expectDefinitionGrid(page: Page, panelSpan: 1 | 4, contentSpan: 20 | 23) {
  const panel = page.locator('.workflow-process-definition-page .tree-panel-col');
  const content = page.locator('.workflow-process-definition-page .tree-content-col');
  const grid = page.locator('.workflow-process-definition-page .content-grid');
  await expect(panel).toHaveClass(new RegExp(`el-col-lg-${panelSpan}`));
  await expect(content).toHaveClass(new RegExp(`el-col-lg-${contentSpan}`));
  let previous: number[] | undefined;
  let stableSamples = 0;
  await expect
    .poll(
      async () => {
        const boxes = await Promise.all([panel.boundingBox(), content.boundingBox(), grid.boundingBox()]);
        if (!boxes.every(Boolean)) return false;
        const [panelBox, contentBox, gridBox] = boxes as [
          NonNullable<(typeof boxes)[0]>,
          NonNullable<(typeof boxes)[0]>,
          NonNullable<(typeof boxes)[0]>
        ];
        const terminalRange =
          Math.abs(panelBox.y - contentBox.y) < 2 &&
          panelBox.x + panelBox.width <= contentBox.x + 1 &&
          contentBox.x + contentBox.width <= gridBox.x + gridBox.width + 1 &&
          (panelSpan === 4
            ? panelBox.width / gridBox.width > 0.14 && panelBox.width / gridBox.width < 0.2
            : panelBox.width >= 54 && panelBox.width <= 58 && contentBox.width / gridBox.width > 0.9);
        const signature = [
          panelBox.x,
          panelBox.y,
          panelBox.width,
          contentBox.x,
          contentBox.y,
          contentBox.width,
          gridBox.x,
          gridBox.width
        ];
        const stable = previous?.every((value, index) => Math.abs(value - signature[index]) < 0.5) ?? false;
        stableSamples = terminalRange ? (stable ? stableSamples + 1 : 1) : 0;
        previous = signature;
        return stableSamples >= 3;
      },
      { message: `definition grid did not settle at ${panelSpan}+${contentSpan}` }
    )
    .toBe(true);
}

test('selected workflow manifest completes category, definition, designer and SpEL mutations', async ({ page }) => {
  const state = createState();
  await installApi(page, state, ['*:*:*']);
  await page.goto('/login?redirect=%2Fworkflow%2Fcategory');
  await page.locator('.submit-button').click();
  await expect(page.getByRole('heading', { name: '流程分类' })).toBeVisible();

  const categoryRow = page.getByRole('row').filter({ hasText: '审批' });
  await categoryRow.locator('button').nth(1).click();
  const categoryDialog = page.getByRole('dialog', { name: '添加流程分类' });
  await categoryDialog.getByPlaceholder('请输入分类名称').fill('财务审批');
  await categoryDialog.getByRole('button', { name: '确 定' }).click();
  await expect(page.getByText('财务审批', { exact: true })).toBeVisible();

  await page.locator('.sidebar-container').getByText('流程定义', { exact: true }).click();
  await expect(page.getByText('既有已发布流程', { exact: true })).toBeVisible();
  await expectDefinitionGrid(page, 4, 20);
  await page.locator('.workflow-process-definition-page .tree-panel-header').click();
  await expect(page.locator('.workflow-process-definition-page .tree-panel-col')).toHaveClass(/is-collapsed/);
  await expectDefinitionGrid(page, 1, 23);
  await page.locator('.workflow-process-definition-page .tree-panel-header').click();
  await expectDefinitionGrid(page, 4, 20);
  const publishedRow = page.getByRole('row').filter({ hasText: '既有已发布流程' });
  const publishedSelection = publishedRow.locator('label.el-checkbox');
  await publishedSelection.click();
  await expect(publishedSelection).toHaveClass(/is-checked/);
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: '导出' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe('published.json');
  await page.getByRole('tab', { name: '未发布' }).click();
  await expect(page.getByText('请假审批', { exact: true })).toBeVisible();
  await page.getByRole('row').filter({ hasText: '请假审批' }).getByRole('button', { name: '流程设计' }).click();
  await expect(page).toHaveURL(/\/workflow\/design\/index.*definitionId=d1.*disabled=false/);
  const iframe = page.locator('iframe[title="流程设计"]');
  await expect(iframe).toHaveAttribute('src', /id=d1&onlyDesignShow=false/);
  const iframeUrl = new URL((await iframe.getAttribute('src'))!, page.url());
  // The frame is an explicit HTML fixture; this only verifies the credential-free URL contract.
  expect(iframeUrl.searchParams.get('Authorization')).toBeNull();
  expect(iframeUrl.searchParams.get('clientid')).toBe('e5cd7e4891bf95d1d19206ce24a7b32e');
  await page.frameLocator('iframe[title="流程设计"]').getByRole('button', { name: '关闭设计器' }).click();
  await expect(page).toHaveURL(/\/workflow\/processDefinition(?:\?.*)?$/);
  await page.getByRole('button', { name: '发布流程' }).click();
  await page.getByRole('button', { name: '确定' }).click();
  await expect(
    page.getByRole('row').filter({ hasText: '请假审批' }).getByText('已发布', { exact: true })
  ).toBeVisible();

  await page.getByRole('button', { name: '部署流程文件' }).click();
  const uploadDialog = page.getByRole('dialog', { name: '部署流程文件' });
  await uploadDialog.locator('.el-select__wrapper').click();
  await page.locator('.el-select-dropdown:visible').getByText('审批', { exact: true }).click();
  await uploadDialog.locator('input[type="file"]').setInputFiles({
    name: 'import-flow.json',
    mimeType: 'application/json',
    buffer: Buffer.from('{"flowCode":"imported","flowName":"导入流程"}')
  });
  await expect(page.getByRole('row').filter({ hasText: '导入流程' })).toBeVisible();

  await page.locator('.sidebar-container').getByText('流程表达式', { exact: true }).click();
  await page.getByRole('button', { name: '新增' }).first().click();
  const spelDialog = page.getByRole('dialog', { name: '添加流程spel表达式定义' });
  await spelDialog.getByPlaceholder('请输入组件名称').fill('spelRuleComponent');
  await spelDialog.getByPlaceholder('请输入方法名称').fill('resolveOwner');
  await spelDialog.getByPlaceholder('请输入方法参数').fill('deptId');
  await expect(spelDialog.getByText('#{@spelRuleComponent.resolveOwner(#deptId)}', { exact: true })).toBeVisible();
  await spelDialog.getByRole('button', { name: '确 定' }).click();
  await expect(page.getByText('#{@spelRuleComponent.resolveOwner(#deptId)}', { exact: true })).toBeVisible();

  const expectedSpelMutation = {
    method: 'POST',
    path: '/workflow/spel',
    body: {
      componentName: 'spelRuleComponent',
      methodName: 'resolveOwner',
      methodParams: 'deptId',
      viewSpel: '#{@spelRuleComponent.resolveOwner(#deptId)}',
      status: '0'
    }
  };
  await expect
    .poll(() => state.mutations.find(item => item.method === 'POST' && item.path === '/workflow/spel'))
    .toEqual(expectedSpelMutation);

  expect(state.mutations).toEqual([
    { method: 'POST', path: '/workflow/category', body: { categoryName: '财务审批', parentId: 'c1', orderNum: 0 } },
    { method: 'POST', path: '/workflow/definition/publish/d1', body: null },
    expectedSpelMutation
  ]);
  const unpublishedIndex = state.definitionRequests.indexOf('GET /workflow/definition/unPublishList');
  const publishIndex = state.definitionRequests.indexOf('POST /workflow/definition/publish/d1');
  const refreshedPublishedIndex = state.definitionRequests.indexOf('GET /workflow/definition/list', publishIndex);
  expect(unpublishedIndex).toBeGreaterThan(-1);
  expect(publishIndex).toBeGreaterThan(unpublishedIndex);
  expect(refreshedPublishedIndex).toBeGreaterThan(publishIndex);
  expect(state.exports).toEqual([{ method: 'POST', path: '/workflow/definition/exportDef/d0' }]);
  expect(state.imports).toHaveLength(1);
  expect(state.imports[0]).toMatchObject({ method: 'POST', path: '/workflow/definition/importDef' });
  expect(state.imports[0].contentType).toContain('multipart/form-data; boundary=');
  expect(state.imports[0].body).toContain('name="category"');
  expect(state.imports[0].body).toContain('c1');
  expect(state.imports[0].body).toContain('filename="import-flow.json"');
  expect(state.imports[0].body).toContain('"flowCode":"imported"');
  expect(state.unknown).toEqual([]);
});

test('workflow business failure stays visible without false success or loading lock', async ({ page }) => {
  const state = createState({ failCategoryAdd: true });
  await installApi(page, state, ['*:*:*']);
  await page.goto('/login?redirect=%2Fworkflow%2Fcategory');
  await page.locator('.submit-button').click();
  await expect(page.getByRole('heading', { name: '流程分类' })).toBeVisible();

  await page.getByRole('row').filter({ hasText: '审批' }).locator('button').nth(1).click();
  const dialog = page.getByRole('dialog', { name: '添加流程分类' });
  await dialog.getByPlaceholder('请输入分类名称').fill('失败分类');
  await dialog.getByRole('button', { name: '确 定' }).click();
  await expect(page.getByText('分类保存失败', { exact: true })).toBeVisible();
  await expect(page.getByText('操作成功', { exact: true })).toHaveCount(0);
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole('button', { name: '确 定' })).toBeEnabled();
  await expect(page.getByRole('table').getByText('失败分类', { exact: true })).toHaveCount(0);
  expect(state.categories.some(item => item.categoryName === '失败分类')).toBe(false);
  expect(state.mutations).toEqual([]);
  expect(state.unknown).toEqual([]);
});

test('workflow permissions hide mutations without filtering selected server menus', async ({ page }) => {
  const state = createState();
  await installApi(page, state, ['workflow:category:list', 'workflow:category:query']);
  await page.goto('/login?redirect=%2Fworkflow%2Fcategory');
  await page.locator('.submit-button').click();
  await expect(page.getByRole('heading', { name: '流程分类' })).toBeVisible();
  await expect(page.getByRole('button', { name: '新增' })).toHaveCount(0);
  expect(state.mutations).toEqual([]);
  expect(state.unknown).toEqual([]);
});


async function openOwnedDesigner(page: Page) {
  const state = createState();
  await installApi(page, state, ['*:*:*']);
  await page.goto('/login?redirect=%2Fworkflow%2FprocessDefinition');
  await page.locator('.submit-button').click();
  await expect(page.getByText('既有已发布流程', { exact: true })).toBeVisible();
  await page.getByRole('tab', { name: '未发布' }).click();
  await page.getByRole('row').filter({ hasText: '请假审批' }).getByRole('button', { name: '流程设计' }).click();
  await expect(page.frameLocator('iframe[title="流程设计"]').getByRole('button', { name: '关闭设计器' })).toBeVisible();
}

async function expectDesignerStillOpen(page: Page) {
  // Observe the asynchronous navigation boundary, including the lazy host import.
  await expect(page.waitForURL(/\/workflow\/processDefinition(?:\?.*)?$/, { timeout: 400 })).rejects.toThrow(/Timeout/);
  await expect(page.locator('iframe[title="流程设计"]')).toBeVisible();
}

test('designer rejects foreign origin, other windows and unknown payloads', async ({ page }) => {
  await openOwnedDesigner(page);
  const iframe = page.locator('iframe[title="流程设计"]');
  const originalUrl = (await iframe.getAttribute('src'))!;

  await page.evaluate(() => window.postMessage({ method: 'close' }, window.location.origin));
  await expectDesignerStillOpen(page);
  await page.evaluate(() => window.dispatchEvent(new MessageEvent('message', { data: { method: 'close' } })));
  await expectDesignerStillOpen(page);

  // An actual same-origin sibling window has the right origin but the wrong source.
  await page.evaluate(() => {
    const sibling = document.createElement('iframe'); sibling.id = 'foreign-designer';
    sibling.style.cssText = 'position:fixed;right:0;top:0;width:320px;height:80px;z-index:2147483647';
    sibling.src = '/prod-api/warm-flow-ui/other.html'; document.body.append(sibling);
  });
  await page.frameLocator('#foreign-designer').getByRole('button', { name: '关闭设计器' }).click();
  await expectDesignerStillOpen(page);
  await page.locator('#foreign-designer').evaluate(element => element.remove());

  // Navigate the owned frame itself: right WindowProxy, wrong origin.
  await page.route('http://127.0.0.1:4174/untrusted-designer', route => route.fulfill({
    contentType: 'text/html; charset=utf-8', body: '<button onclick="parent.postMessage({method: \'close\'}, \'*\')">伪造关闭</button>'
  }));
  await iframe.evaluate(element => { (element as HTMLIFrameElement).src = 'http://127.0.0.1:4174/untrusted-designer'; });
  await page.frameLocator('iframe[title="流程设计"]').getByRole('button', { name: '伪造关闭' }).click();
  await expectDesignerStillOpen(page);
  await iframe.evaluate((element, url) => { (element as HTMLIFrameElement).src = url; }, originalUrl);
  await expect(page.frameLocator('iframe[title="流程设计"]').getByRole('button', { name: '关闭设计器' })).toBeVisible();
  const ownedFrame = page.frames().find(frame => frame.url().includes('/warm-flow-ui/index.html'))!;
  for (const payload of [null, 'close', { method: 'save' }, { method: 'publish' }, { method: 1 }, [{ method: 'close' }]]) {
    await ownedFrame.evaluate(data => window.parent.postMessage(data, '*'), payload);
    await expectDesignerStillOpen(page);
  }
  await page.frameLocator('iframe[title="流程设计"]').getByRole('button', { name: '关闭设计器' }).click();
  await expect(page).toHaveURL(/\/workflow\/processDefinition(?:\?.*)?$/);
});

test('designer refresh replaces its window and releases message listeners after closing', async ({ page }) => {
  await page.addInitScript(() => {
    const listeners = new Set<EventListenerOrEventListenerObject>();
    const add = window.addEventListener; const remove = window.removeEventListener;
    window.addEventListener = function (type: string, listener: EventListenerOrEventListenerObject, options?: boolean | AddEventListenerOptions) {
      if (type === 'message' && listener) listeners.add(listener);
      add.call(this, type, listener, options);
    };
    window.removeEventListener = function (type: string, listener: EventListenerOrEventListenerObject, options?: boolean | EventListenerOptions) {
      if (type === 'message' && listener) listeners.delete(listener);
      remove.call(this, type, listener, options);
    };
    Object.defineProperty(window, '__ownedMessageListenerCount', { get: () => listeners.size });
  });
  await openOwnedDesigner(page);
  const count = () => page.evaluate(() => Reflect.get(window, '__ownedMessageListenerCount') as number);
  const activeCount = await count();
  expect(activeCount).toBeGreaterThan(0);
  const oldWindow = await page.locator('iframe[title="流程设计"]').evaluateHandle(element => (element as HTMLIFrameElement).contentWindow);
  await page.getByTitle('刷新页面', { exact: true }).click();
  await expect.poll(() => page.locator('iframe[title="流程设计"]').evaluate((element, previous) => (element as HTMLIFrameElement).contentWindow !== previous, oldWindow)).toBe(true);
  await expect(page.frameLocator('iframe[title="流程设计"]').getByRole('button', { name: '关闭设计器' })).toBeVisible();
  await expect.poll(count).toBe(activeCount);
  await page.evaluate(previous => window.dispatchEvent(new MessageEvent('message', {
    source: previous, origin: window.location.origin, data: { method: 'close' }
  })), oldWindow);
  await expectDesignerStillOpen(page);
  await oldWindow.dispose();
  await page.frameLocator('iframe[title="流程设计"]').getByRole('button', { name: '关闭设计器' }).click();
  await expect(page).toHaveURL(/\/workflow\/processDefinition(?:\?.*)?$/);
  await expect.poll(count).toBe(activeCount - 1);
  await page.locator('.sidebar-container').getByText('流程表达式', { exact: true }).click();
  await expect(page).toHaveURL(/\/workflow\/spel$/);
  await page.evaluate(() => window.postMessage({ method: 'close' }, window.location.origin));
  await expect(page.waitForURL(/\/workflow\/processDefinition(?:\?.*)?$/, { timeout: 400 })).rejects.toThrow(/Timeout/);
});
