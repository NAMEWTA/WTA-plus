import { expect, test, type Page, type Route } from '@playwright/test';

type ApiState = {
  demoListRequests: number;
  treeListRequests: number;
  unknownRequests: string[];
};

type MenuMode = 'diagnostic' | 'valid';

const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });

const demoChildren = (mode: MenuMode) =>
  mode === 'diagnostic'
    ? [
        {
          path: 'manifest-diagnostic',
          name: 'DemoManifestDiagnostic',
          component: 'demo/missing/index',
          meta: { title: '缺失组件诊断', icon: 'warning', noCache: true }
        }
      ]
    : [
        {
          path: 'demo',
          name: 'Demo',
          component: 'demo/demo/index',
          meta: { title: '测试单', icon: 'dashboard', noCache: false }
        },
        {
          path: 'tree',
          name: 'Tree',
          component: 'demo/tree/index',
          meta: { title: '测试树', icon: 'tree-table', noCache: false }
        }
      ];

async function installApi(page: Page, state: ApiState, mode: MenuMode) {
  await page.route('**/prod-api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    if (path === '/auth/client/context')
      return json(route, { code: 200, data: { clientEnabled: true, registerEnabled: true } });
    if (path === '/auth/code') return json(route, { code: 200, data: { captchaEnabled: false } });
    if (path === '/auth/login') return json(route, { code: 200, data: { access_token: 'demo-manifest-token' } });
    if (path === '/system/user/getInfo')
      return json(route, {
        code: 200,
        data: {
          user: { userId: 1, userName: 'demo-user', nickName: 'Demo User', avatarUrl: '' },
          roles: ['demo-role'],
          permissions: ['demo:demo:list', 'demo:tree:list']
        }
      });
    if (path === '/system/menu/getRouters')
      return json(route, {
        code: 200,
        data: [
          {
            path: '/demo',
            name: 'DemoMenu',
            component: 'Layout',
            redirect: 'noRedirect',
            alwaysShow: true,
            meta: { title: '演示管理', icon: 'dashboard', noCache: false },
            children: demoChildren(mode)
          }
        ]
      });
    if (path === '/demo/demo/list') {
      state.demoListRequests += 1;
      return json(route, {
        code: 200,
        data: {
          rows: [{ id: 1, deptId: 10, userId: 20, orderNum: 1, testKey: 'manifest-key', value: 'pilot' }],
          total: 1
        }
      });
    }
    if (path === '/demo/tree/list') {
      state.treeListRequests += 1;
      return json(route, {
        code: 200,
        data: [{ id: 1, parentId: 0, deptId: 10, userId: 20, treeName: 'manifest-root', children: [] }]
      });
    }
    if (path === '/notify/inbox')
      return json(route, { code: 200, data: [] });
    if (path === '/resource/message/close') return json(route, { code: 200, data: null });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    state.unknownRequests.push(`${request.method()} ${path}`);
    return json(route, { code: 200, data: null });
  });
}

const createState = (): ApiState => ({ demoListRequests: 0, treeListRequests: 0, unknownRequests: [] });

test('selected demo registry routes survive a keep-alive menu round trip', async ({ page }) => {
  const state = createState();
  await installApi(page, state, 'valid');

  await page.goto('/login?redirect=%2Fdemo%2Fdemo');
  await expect(page.locator('.submit-button')).toBeEnabled();
  await page.locator('.submit-button').click();

  await expect(page).toHaveURL(/\/demo\/demo$/);
  await expect(page.getByRole('heading', { name: '测试单列表' })).toBeVisible();
  await expect(page.getByText('manifest-key', { exact: true })).toBeVisible();
  await expect.poll(() => state.demoListRequests).toBe(1);

  await page.locator('.sidebar-container').getByText('测试树', { exact: true }).click();
  await expect(page).toHaveURL(/\/demo\/tree$/);
  await expect(page.getByRole('heading', { name: '测试树列表' })).toBeVisible();
  await expect(page.getByText('manifest-root', { exact: true })).toBeVisible();
  await expect.poll(() => state.treeListRequests).toBe(1);

  await page.locator('.sidebar-container').getByText('测试单', { exact: true }).click();
  await expect(page).toHaveURL(/\/demo\/demo$/);
  await expect(page.getByText('manifest-key', { exact: true })).toBeVisible();
  expect(state.demoListRequests).toBe(1);
  expect(state.treeListRequests).toBe(1);
  expect(state.unknownRequests).toEqual([]);
});

test('shared manifest diagnostic renders a stable missing-key error', async ({ page }) => {
  const state = createState();
  await installApi(page, state, 'diagnostic');

  await page.goto('/login?redirect=%2Fdemo%2Fmanifest-diagnostic');
  await expect(page.locator('.submit-button')).toBeEnabled();
  await page.locator('.submit-button').click();

  await expect(page).toHaveURL(/\/demo\/manifest-diagnostic$/);
  await expect(page.getByRole('heading', { name: '页面加载失败' })).toBeVisible();
  await expect(page.getByTestId('manifest-route-diagnostic')).toContainText(
    '页面组件不可用 [missing-component-key] app=admin-web domain=demo key=demo/missing/index'
  );
  expect(state.demoListRequests).toBe(0);
  expect(state.treeListRequests).toBe(0);
  expect(state.unknownRequests).toEqual([]);
});
