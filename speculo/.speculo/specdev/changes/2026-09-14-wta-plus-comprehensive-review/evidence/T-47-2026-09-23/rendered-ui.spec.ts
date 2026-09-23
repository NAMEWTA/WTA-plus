import { expect, test, type Page, type Route } from '/srv/WTA-plus/frontend/node_modules/@playwright/test/index.mjs';

const adminUrl = process.env.ADMIN_WEB_URL ?? 'http://127.0.0.1:58503';
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

test('T47 rendered error retry and pending preview leave list usable', async ({page}) => {
  const state: State = {configFailure:false,socialFailure:false,uploaded:false,uploadTransfers:[],requests:[],unknown:[]};
  await installApi(page,state,['system:oss:list','system:oss:download']);
  let calls=0; let previewStarted=false; let release!:()=>void;
  const gate=new Promise<void>(resolve=>{release=resolve});
  await page.route('**/prod-api/resource/oss/list*',route=>{
    calls++;
    if(calls===1)return json(route,{code:500,msg:'T47 controlled list failure'});
    return json(route,{code:200,data:{rows:[{ossId:7,fileName:'T47-pending-preview.png',originalName:'T47-pending-preview.png',fileSuffix:'.png',url:'https://untrusted.example.test/image',service:'synthetic',deleteState:'ACTIVE',accessPolicy:'PRIVATE',createByName:'Fixture'}],total:1}});
  });
  await page.route('**/prod-api/resource/oss/7/download-url',async route=>{
    previewStarted=true; await gate;
    await json(route,{code:500,msg:'Synthetic preview unavailable'});
  });
  try {
    await page.addInitScript(()=>localStorage.setItem('Admin-Token','t47-rendered-fixture'));
    await page.goto(`${adminUrl}/system/oss`);
    const alert=page.locator('.system-oss-page .el-alert');
    await expect(alert).toBeVisible(); await expect(alert).toContainText('T47 controlled list failure');
    await page.screenshot({path:'/tmp/wta-t47/oss-error.png',fullPage:true});
    await page.getByRole('button',{name:'搜索',exact:true}).click();
    await expect(alert).toHaveCount(0);
    await expect(page.getByRole('row').filter({hasText:'T47-pending-preview.png'})).toBeVisible();
    await expect.poll(()=>previewStarted).toBe(true);
    await expect(page.locator('.system-oss-page .el-loading-mask')).toBeHidden();
    await expect(page.getByText(/untrusted\.example\.test/)).toHaveCount(0);
    await page.screenshot({path:'/tmp/wta-t47/oss-pending-preview.png',fullPage:true});
    expect(calls).toBe(2);
  } finally { release(); }
});
