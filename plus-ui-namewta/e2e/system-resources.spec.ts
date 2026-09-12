import { expect, test, type Page, type Route } from '@playwright/test';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:4173';
const adminClientId = 'e5cd7e4891bf95d1d19206ce24a7b32e';

const menus = [
  {
    path: '/system',
    name: 'SystemResourceProof',
    component: 'Layout',
    meta: { title: '系统管理' },
    children: [
      { path: 'dict', name: 'SystemDictProof', component: 'system/dict/index', meta: { title: '字典管理' } },
      { path: 'config', name: 'SystemConfigProof', component: 'system/config/index', meta: { title: '参数设置' } },
      { path: 'oss', name: 'SystemOssProof', component: 'system/oss/index', meta: { title: '文件管理' } },
      {
        path: 'oss-config/index',
        name: 'SystemOssConfigProof',
        component: 'system/oss/config',
        meta: { title: 'OSS配置' }
      }
    ]
  }
];

type State = {
  configFailure: boolean;
  socialFailure: boolean;
  uploaded: boolean;
  uploadTransfers: string[];
  requests: Array<{ clientId: string; method: string; path: string }>;
  unknown: string[];
};

const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });

async function installApi(page: Page, state: State, permissions: string[]) {
  await page.route('https://uploads.example.test/system-resource-proof.txt', async route => {
    state.uploadTransfers.push(`${route.request().method()} ${new URL(route.request().url()).pathname}`);
    await route.fulfill({ status: 200, headers: { ETag: 'proof-etag' }, body: '' });
  });
  await page.route('https://files.example.test/system-resource-proof.txt', route =>
    route.fulfill({
      status: 200,
      headers: { 'Content-Disposition': 'attachment; filename="system-resource-proof.txt"' },
      body: 'system resource proof'
    })
  );
  await page.route('**/prod-api/**', route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    const method = request.method();
    if (path === '/auth/client/context') {
      return json(route, {
        code: 200,
        data: {
          clientEnabled: true,
          registerEnabled: true,
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
          user: { userId: 11, userName: 'resource-admin', nickName: 'Resource Admin', avatarUrl: '' },
          roles: ['operator'],
          permissions
        }
      });
    }
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: menus });
    if (path === '/notify/inbox') {
      state.requests.push({ clientId: request.headers()['clientid'] ?? '', method, path });
      return json(route, {
        code: 200,
        data: [{ messageId: 1, title: '系统资源消息', message: '已完成', category: 'system' }]
      });
    }
    if (path === '/resource/message/ticket') return json(route, { code: 200, data: 'test-push-ticket' });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    if (path.startsWith('/system/dict/data/type/')) return json(route, { code: 200, data: [] });
    if (path === '/system/dict/data/list' && method === 'GET') {
      state.requests.push({ clientId: request.headers()['clientid'] ?? '', method, path });
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              dictCode: '1',
              dictLabel: '启用',
              dictValue: '0',
              cssClass: '',
              listClass: 'primary',
              dictSort: 1,
              remark: ''
            }
          ],
          total: 1
        }
      });
    }
    if (path === '/system/user/profile') {
      return json(route, {
        code: 200,
        data: {
          user: { userId: 11, userName: 'resource-admin', nickName: 'Resource Admin' },
          roleGroup: 'operator',
          postGroup: '运维'
        }
      });
    }
    if (path === '/monitor/online') return json(route, { code: 200, data: { rows: [], total: 0 } });
    if (path === '/system/social/list') {
      state.requests.push({ clientId: request.headers()['clientid'] ?? '', method, path });
      return json(route, {
        code: 200,
        data: [{ id: 42, source: 'github', avatar: '', userName: 'resource-admin', createTime: '2026-08-26' }]
      });
    }
    if (path === '/auth/binding/github' || path === '/auth/unlock/42') {
      state.requests.push({ clientId: request.headers()['clientid'] ?? '', method, path });
      if (state.socialFailure) return json(route, { code: 500, msg: '当前 Client 社交账号服务不可用' });
      return json(route, { code: 200, data: path.startsWith('/auth/binding/') ? '/social-callback' : null });
    }
    if (path === '/resource/oss/uploads' && method === 'POST') {
      state.requests.push({ clientId: request.headers()['clientid'] ?? '', method, path });
      return json(route, {
        code: 200,
        data: {
          uploadToken: 'system-resource-upload',
          mode: 'SINGLE',
          expiresAt: '2099-01-01T00:00:00Z',
          presignedRequest: {
            method: 'PUT',
            url: 'https://uploads.example.test/system-resource-proof.txt',
            requiredHeaders: { 'Content-Type': 'text/plain' },
            expiresAt: '2099-01-01T00:00:00Z'
          }
        }
      });
    }
    if (path === '/resource/oss/uploads/system-resource-upload/complete' && method === 'POST') {
      state.requests.push({ clientId: request.headers()['clientid'] ?? '', method, path });
      state.uploaded = true;
      return json(route, { code: 200, data: '8' });
    }
    if (path === '/resource/oss/listByIds/8' && method === 'GET') {
      state.requests.push({ clientId: request.headers()['clientid'] ?? '', method, path });
      return json(route, {
        code: 200,
        data: [
          {
            ossId: 8,
            fileName: 'system-resource-proof.txt',
            originalName: 'system-resource-proof.txt',
            fileSuffix: '.txt',
            url: 'https://untrusted.example.test/stored-public-url',
            createByName: 'resource-admin',
            service: 'proof',
            isTemp: 'N',
            deleteState: 'ACTIVE',
            referenceCount: 0,
            references: []
          }
        ]
      });
    }

    const resourcePaths = new Set([
      '/system/dict/type/list',
      '/system/config/list',
      '/resource/oss/list',
      '/resource/oss/7/download-url',
      '/resource/oss/8/download-url',
      '/resource/oss/config/list',
      '/system/config/configKey/sys.oss.previewListResource'
    ]);
    if (resourcePaths.has(path)) {
      state.requests.push({ clientId: request.headers()['clientid'] ?? '', method, path });
      if (path === '/system/config/list') {
        if (state.configFailure) return json(route, { code: 500, msg: '当前 Client 参数查询失败' });
        return json(route, {
          code: 200,
          data: {
            rows: [{ configId: 1, configName: '站点名称', configKey: 'site.name', configValue: 'NAMEWTA' }],
            total: 1
          }
        });
      }
      if (path === '/system/dict/type/list') {
        return json(route, {
          code: 200,
          data: { rows: [{ dictId: 1, dictName: '资源状态', dictType: 'resource_status' }], total: 1 }
        });
      }
      if (path === '/resource/oss/config/list') return json(route, { code: 200, data: { rows: [], total: 0 } });
      if (path === '/system/config/configKey/sys.oss.previewListResource')
        return json(route, { code: 200, data: 'true' });
      if (path === '/resource/oss/list') {
        const uploadedRow = state.uploaded
          ? [
              {
                ossId: 8,
                fileName: 'system-resource-proof.txt',
                originalName: 'system-resource-proof.txt',
                fileSuffix: '.txt',
                url: 'https://untrusted.example.test/stored-public-url',
                service: 'proof'
              }
            ]
          : [];
        return json(route, {
          code: 200,
          data: {
            rows: [
              {
                ossId: 7,
                fileName: 'proof.txt',
                originalName: 'proof.txt',
                fileSuffix: '.txt',
                url: 'https://untrusted.example.test/stored-private-url',
                service: 'proof'
              },
              ...uploadedRow
            ],
            total: 1 + uploadedRow.length
          }
        });
      }
      if (path === '/resource/oss/7/download-url') {
        return json(route, {
          code: 200,
          data: {
            url: 'https://files.example.test/system-resource-proof.txt',
            fileName: 'system-resource-proof.txt',
            accessType: 'PRIVATE',
            expiresAt: '2099-01-01T00:00:00Z'
          }
        });
      }
      if (path === '/resource/oss/8/download-url') {
        return json(route, {
          code: 200,
          data: {
            url: 'https://files.example.test/system-resource-proof.txt',
            fileName: 'system-resource-proof.txt',
            accessType: 'PUBLIC',
            expiresAt: null
          }
        });
      }
    }

    state.unknown.push(`${method} ${path}`);
    return json(route, { code: 200, data: null });
  });
}

test('admin selects resource manifests and keeps message/config/dict/OSS requests Client scoped', async ({ page }) => {
  const state: State = {
    configFailure: false,
    socialFailure: false,
    uploaded: false,
    uploadTransfers: [],
    requests: [],
    unknown: []
  };
  await installApi(page, state, [
    'system:dict:list',
    'system:config:list',
    'system:oss:list',
    'system:oss:upload',
    'system:oss:download'
  ]);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'system-resource-proof'));

  await page.goto(`${adminUrl}/system/dict`);
  await expect(page.getByRole('heading', { name: '字典管理' })).toBeVisible();
  await expect(page.getByText('资源状态', { exact: true })).toBeVisible();
  await page.locator('.message-trigger').click();
  await expect(page.getByText('系统资源消息', { exact: true })).toBeVisible();

  await page.goto(`${adminUrl}/system/config`);
  await expect(page.getByRole('heading', { name: '参数列表' })).toBeVisible();
  await expect(page.getByText('site.name', { exact: true })).toBeVisible();

  await page.goto(`${adminUrl}/system/oss`);
  await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible();
  await expect(page.getByText('proof.txt', { exact: true }).first()).toBeVisible();
  await expect(page.getByText(/untrusted\.example\.test/)).toHaveCount(0);
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: '下载' }).first().click();
  expect((await download).suggestedFilename()).toBe('system-resource-proof.txt');

  await page.getByRole('button', { name: '上传文件' }).click();
  const uploadDialog = page.getByRole('dialog', { name: '上传文件' });
  await uploadDialog.locator('input[type="file"]').setInputFiles({
    name: 'system-resource-proof.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('system resource proof')
  });
  await expect(
    uploadDialog.getByRole('link', { name: 'system-resource-proof.txt', exact: true }).first()
  ).toBeVisible();
  const ossListRequestsBeforeConfirm = state.requests.filter(item => item.path === '/resource/oss/list').length;
  await uploadDialog.getByRole('button', { name: '确 定' }).click();
  await expect(uploadDialog).toBeHidden();
  await expect
    .poll(() => state.requests.filter(item => item.path === '/resource/oss/list').length)
    .toBe(ossListRequestsBeforeConfirm + 1);
  await expect(page.getByRole('row').filter({ hasText: 'system-resource-proof.txt' })).toBeVisible();
  expect(state.uploadTransfers).toEqual(['PUT /system-resource-proof.txt']);

  expect(state.requests.every(item => item.clientId === adminClientId)).toBe(true);
  expect(state.requests.map(item => `${item.method} ${item.path}`)).toEqual(
    expect.arrayContaining([
      'GET /notify/inbox',
      'GET /system/dict/type/list',
      'GET /system/dict/data/list',
      'GET /system/config/list',
      'GET /resource/oss/list',
      'GET /resource/oss/7/download-url',
      'POST /resource/oss/uploads',
      'POST /resource/oss/uploads/system-resource-upload/complete',
      'GET /resource/oss/listByIds/8',
      'GET /resource/oss/8/download-url'
    ])
  );
  expect(state.unknown).toEqual([]);
});

test('resource permission denial hides mutations and a Client query failure never invents fallback data', async ({
  page
}) => {
  const state: State = {
    configFailure: true,
    socialFailure: false,
    uploaded: false,
    uploadTransfers: [],
    requests: [],
    unknown: []
  };
  await installApi(page, state, ['system:config:list']);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'system-resource-denied'));

  await page.goto(`${adminUrl}/system/config`);
  await expect(page.getByText('当前 Client 参数查询失败', { exact: true })).toBeVisible();
  await expect(page.getByText('site.name', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '新增' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '导出' })).toHaveCount(0);
  expect(state.requests.filter(item => item.path === '/system/config/list')).toHaveLength(1);
  expect(state.unknown).toEqual([]);
});

test('social list is rendered and binding/unlock failures remain Client scoped and visible', async ({ page }) => {
  const state: State = {
    configFailure: false,
    socialFailure: true,
    uploaded: false,
    uploadTransfers: [],
    requests: [],
    unknown: []
  };
  await installApi(page, state, []);
  await page.addInitScript(() => localStorage.setItem('Admin-Token', 'system-social-proof'));

  await page.goto(`${adminUrl}/user/profile`);
  await page.getByRole('tab', { name: '第三方应用' }).click();
  await expect(page.getByText('github', { exact: true })).toBeVisible();
  await page.getByTitle('使用 GitHub 账号授权登录').click();
  const socialServiceFailure = page.getByRole('alert').filter({ hasText: '当前 Client 社交账号服务不可用' });
  await expect(socialServiceFailure).toHaveCount(1);
  await expect(socialServiceFailure).toBeVisible();
  await expect(socialServiceFailure).toHaveCount(0);
  await page.locator('.profile-auth-table tbody tr').filter({ hasText: 'github' }).getByRole('button').click();
  const unlockDialog = page.getByRole('dialog').filter({ hasText: '解除"github"的账号绑定' });
  await expect(unlockDialog).toBeVisible();
  await unlockDialog.getByRole('button', { name: '确定' }).click();
  await expect(unlockDialog).toBeHidden();
  await expect(socialServiceFailure).toHaveCount(1);
  await expect(socialServiceFailure).toBeVisible();

  expect(
    state.requests
      .filter(item => ['/system/social/list', '/auth/binding/github', '/auth/unlock/42'].includes(item.path))
      .map(item => `${item.clientId} ${item.method} ${item.path}`)
  ).toEqual([
    `${adminClientId} GET /system/social/list`,
    `${adminClientId} GET /auth/binding/github`,
    `${adminClientId} DELETE /auth/unlock/42`
  ]);
  expect(state.unknown).toEqual([]);
});
