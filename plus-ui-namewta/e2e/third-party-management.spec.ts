import { expect, test, type Page, type Route } from '@playwright/test';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4173';
const adminClientId = 'e5cd7e4891bf95d1d19206ce24a7b32e';

type ThirdPage = 'provider' | 'endpoint' | 'invocation' | 'statistics';
type RequestRecord = { clientId: string; method: string; path: string };
type State = { requests: RequestRecord[]; unknown: string[] };

const pageMenus: Record<ThirdPage, { component: string; permission: string; title: string }> = {
  provider: { component: 'third/provider/index', permission: 'third:provider:list', title: '供应商管理' },
  endpoint: { component: 'third/endpoint/index', permission: 'third:endpoint:list', title: '接口管理' },
  invocation: { component: 'third/invocation/index', permission: 'third:invocation:list', title: '调用明细' },
  statistics: { component: 'third/statistics/index', permission: 'third:statistics:list', title: '调用统计' }
};

const menus = (pages: ThirdPage[]) => [
  {
    path: '/system',
    name: 'ThirdSystemProof',
    component: 'Layout',
    meta: { title: '系统管理' },
    children: [
      {
        path: 'third',
        name: 'ThirdManagementProof',
        component: 'ParentView',
        meta: { title: '三方接口管理' },
        children: pages.map(page => ({
          path: page,
          name: `Third${page[0]!.toUpperCase()}${page.slice(1)}Proof`,
          component: pageMenus[page].component,
          meta: { title: pageMenus[page].title, permissions: [pageMenus[page].permission] }
        }))
      }
    ]
  }
];

const json = (route: Route, body: unknown, status = 200) =>
  route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });

async function installApi(page: Page, state: State, permissions: string[], pages: ThirdPage[]) {
  await page.route('**/prod-api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    const method = request.method();
    if (path === '/auth/client/context') {
      return json(route, {
        code: 200,
        data: {
          clientEnabled: true,
          registerEnabled: false,
          passwordPolicy: {
            minimumLength: 8,
            maximumLength: 20,
            requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'],
            allowedSpecialCharacters: '!@#'
          }
        }
      });
    }
    if (path === '/system/user/getInfo') {
      return json(route, {
        code: 200,
        data: {
          user: { userId: 31, userName: 'third-admin', nickName: 'Third Admin', avatarUrl: '' },
          roles: ['operator'],
          permissions
        }
      });
    }
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: menus(pages) });
    if (path === '/notify/inbox') {
      return json(route, { code: 200, data: [] });
    }
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });

    if (path.startsWith('/third/')) {
      state.requests.push({ clientId: request.headers()['clientid'] ?? '', method, path });
      if (path === '/third/provider/list' && method === 'GET') {
        return json(route, {
          code: 200,
          data: [
            {
              providerId: '1',
              providerCode: 'qichacha',
              providerName: '企查查',
              baseUrl: 'https://api.qichacha.example',
              status: '0',
              timeoutConnectMs: 3000,
              timeoutReadMs: 10000,
              rateLimit: 5,
              concurrencyLimit: 2,
              sharedHeadersJson: '{"X-Version":"v1"}',
              version: 1
            }
          ]
        });
      }
      if (path === '/third/provider/1' && method === 'GET') {
        return json(route, {
          code: 200,
          data: {
            providerId: '1',
            providerCode: 'qichacha',
            providerName: '企查查',
            baseUrl: 'https://api.qichacha.example',
            status: '0',
            timeoutConnectMs: 3000,
            timeoutReadMs: 10000,
            rateLimit: 5,
            concurrencyLimit: 2,
            sharedHeadersJson: '{"X-Version":"v1"}',
            version: 1
          }
        });
      }
      if (path === '/third/provider/1/status' && method === 'POST') return json(route, { code: 200 });
      if (path === '/third/endpoint/list' && method === 'GET') {
        return json(route, {
          code: 200,
          data: [
            {
              endpointId: '11',
              providerId: '1',
              providerCode: 'qichacha',
              endpointCode: 'company-basic',
              endpointName: '企业基本信息',
              httpMethod: 'GET',
              relativePath: '/company/{id}',
              requestMode: 'QUERY',
              responseMode: 'JSON',
              status: '0',
              idempotent: true,
              rateLimit: 3,
              concurrencyLimit: 2,
              retryCount: 1
            }
          ]
        });
      }
      if (path === '/third/credential/list' && method === 'GET') {
        return json(route, {
          code: 200,
          data: [
            {
              credentialId: '21',
              providerCode: 'qichacha',
              scopeType: 'PROVIDER',
              credentialType: 'API_KEY',
              kekVersion: 'v1',
              version: 2,
              enabled: '0'
            }
          ]
        });
      }
      if (path === '/third/invocation/list' && method === 'GET') {
        return json(route, {
          code: 200,
          data: [
            {
              invocationId: '31',
              requestId: 'request-third-001',
              providerCode: 'qichacha',
              endpointCode: 'company-basic',
              attemptCount: 2,
              logicalStatus: 'SUCCESS',
              failureCategory: 'NONE',
              httpStatus: 200,
              durationMs: 42,
              sanitizedRequestJson: '{"token":"***"}',
              sanitizedResponseJson: '{"result":"ok"}',
              createTime: '2026-09-05 03:00:00'
            }
          ]
        });
      }
      if (path === '/third/statistics/list' && method === 'GET') {
        return json(route, {
          code: 200,
          data: [
            {
              providerCode: 'qichacha',
              endpointCode: 'company-basic',
              statDate: '2026-09-05',
              attemptCount: 3,
              successCount: 2,
              failureCount: 1,
              timeoutCount: 0,
              rejectedCount: 1,
              quotaValue: 1000
            }
          ]
        });
      }
    }

    state.unknown.push(`${method} ${path}`);
    return json(route, { code: 200, data: null });
  });
}

test('admin projects all third pages and keeps outbound management requests Client scoped', async ({ page }) => {
  const state: State = { requests: [], unknown: [] };
  const permissions = [
    'third:provider:list',
    'third:provider:query',
    'third:provider:add',
    'third:provider:edit',
    'third:provider:remove',
    'third:endpoint:list',
    'third:endpoint:query',
    'third:endpoint:add',
    'third:endpoint:edit',
    'third:endpoint:remove',
    'third:credential:list',
    'third:credential:add',
    'third:credential:remove',
    'third:invocation:list',
    'third:statistics:list'
  ];
  await installApi(page, state, permissions, ['provider', 'endpoint', 'invocation', 'statistics']);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'third-management-proof'));

  await page.goto(`${adminUrl}/system/third/provider`);
  await expect(page.getByRole('heading', { name: '三方供应商' })).toBeVisible();
  const providerRow = page.getByRole('row').filter({ hasText: 'qichacha' });
  await expect(providerRow).toContainText('企查查');
  await expect(page.getByRole('button', { name: '新增' })).toBeVisible();
  await providerRow.getByRole('button', { name: '编辑' }).click();
  const editDialog = page.getByRole('dialog', { name: '编辑三方供应商' });
  await expect(editDialog.getByLabel('供应商编码')).toBeDisabled();
  await editDialog.getByRole('button', { name: '取消' }).click();

  await providerRow.getByRole('button', { name: '凭据' }).click();
  const credentialDialog = page.getByRole('dialog', { name: '凭据管理' });
  await expect(credentialDialog.getByText('API_KEY', { exact: true })).toBeVisible();
  await expect(credentialDialog.getByLabel('凭据 JSON')).toHaveValue('');
  await expect(credentialDialog.getByRole('button', { name: '保存' })).toBeVisible();
  await credentialDialog.getByRole('button', { name: '取消' }).click();

  await providerRow.locator('.el-switch').click();
  await expect.poll(() => state.requests.filter(item => item.path === '/third/provider/1/status').length).toBe(1);

  await page.goto(`${adminUrl}/system/third/endpoint`);
  await expect(page.getByRole('heading', { name: '三方接口' })).toBeVisible();
  await expect(page.getByRole('row').filter({ hasText: 'company-basic' })).toContainText('/company/{id}');

  await page.goto(`${adminUrl}/system/third/invocation`);
  await expect(page.getByRole('heading', { name: '调用明细' })).toBeVisible();
  await expect(page.getByPlaceholder('供应商编码')).toBeVisible();
  await expect(page.getByText('request-third-001', { exact: true })).toBeVisible();

  await page.goto(`${adminUrl}/system/third/statistics`);
  await expect(page.getByRole('heading', { name: '调用统计' })).toBeVisible();
  await expect(page.getByPlaceholder('供应商编码')).toBeVisible();
  await expect(page.getByRole('row').filter({ hasText: '2026-09-05' })).toContainText('company-basic');

  const thirdRequests = state.requests.filter(item => item.path.startsWith('/third/'));
  expect(thirdRequests.every(item => item.clientId === adminClientId)).toBe(true);
  expect(thirdRequests.map(item => `${item.method} ${item.path}`)).toEqual(
    expect.arrayContaining([
      'GET /third/provider/list',
      'GET /third/provider/1',
      'GET /third/credential/list',
      'POST /third/provider/1/status',
      'GET /third/endpoint/list',
      'GET /third/invocation/list',
      'GET /third/statistics/list'
    ])
  );
  await expect(page.getByText(/raw-secret|request-secret|response-secret/)).toHaveCount(0);
  expect(state.unknown).toEqual([]);
});

test('list-only permission hides every mutation and direct ungranted route fails closed', async ({ page }) => {
  const state: State = { requests: [], unknown: [] };
  await installApi(page, state, ['third:provider:list', 'third:credential:list'], ['provider']);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'third-list-only-proof'));

  await page.goto(`${adminUrl}/system/third/provider`);
  await expect(page.getByRole('heading', { name: '三方供应商' })).toBeVisible();
  const providerRow = page.getByRole('row').filter({ hasText: 'qichacha' });
  await expect(page.getByRole('button', { name: '新增' })).toHaveCount(0);
  await expect(providerRow.getByRole('button', { name: '编辑' })).toHaveCount(0);
  await expect(providerRow.getByRole('button', { name: '删除' })).toHaveCount(0);
  await expect(providerRow.getByRole('switch')).toHaveCount(0);

  await providerRow.getByRole('button', { name: '凭据' }).click();
  const credentialDialog = page.getByRole('dialog', { name: '凭据管理' });
  await expect(credentialDialog.getByText('API_KEY', { exact: true })).toBeVisible();
  await expect(credentialDialog.getByLabel('凭据 JSON')).toHaveCount(0);
  await expect(credentialDialog.getByRole('button', { name: '保存' })).toHaveCount(0);
  await expect(credentialDialog.getByRole('button', { name: '替换' })).toHaveCount(0);
  await expect(credentialDialog.getByRole('button', { name: '删除' })).toHaveCount(0);
  await credentialDialog.getByRole('button', { name: '取消' }).click();

  const endpointRequestsBefore = state.requests.filter(item => item.path === '/third/endpoint/list').length;
  await page.goto(`${adminUrl}/system/third/endpoint`);
  await expect(page.getByRole('heading', { name: '三方接口' })).toHaveCount(0);
  expect(state.requests.filter(item => item.path === '/third/endpoint/list')).toHaveLength(endpointRequestsBefore);
  expect(state.requests.filter(item => item.method !== 'GET')).toEqual([]);
  expect(state.unknown).toEqual([]);
});
