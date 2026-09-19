import { expect, test } from '@playwright/test';

for (const legacy of [true, false]) {
  test(`${legacy ? 'legacy' : 'current'} menus keep diagnostics and unrelated monitors without AI requests`, async ({
    page
  }) => {
    const retiredRequests: string[] = [];
    const recovery: string[] = [];
    page.on('request', request => {
      if (/\/snail-ai(?:\/|\?)|\/snail-chat(?:\/|\?)|:8900\//.test(request.url())) retiredRequests.push(request.url());
    });
    await page.route('**/prod-api/**', async route => {
      const path = new URL(route.request().url()).pathname.replace('/prod-api', '');
      let data: unknown = [];
      if (path === '/auth/client/context') data = { clientEnabled: true, registerEnabled: false };
      else if (path === '/system/user/getInfo') {
        recovery.push('identity');
        data = {
          user: { userId: 42, userName: 'owned-user', nickName: 'Owned User' },
          roles: ['admin'],
          permissions: ['*:*:*']
        };
      } else if (path === '/system/menu/getRouters') {
        recovery.push('menus');
        data = [
          ...(legacy
            ? [
                {
                  path: '/legacy-ai',
                  name: 'LegacyAi',
                  component: 'Layout',
                  meta: { title: '旧 AI 目录' },
                  children: [
                    { path: 'chat', name: 'OldChat', component: 'ai/chat/index', meta: { title: '旧 AI 聊天' } }
                  ]
                }
              ]
            : []),
          {
            path: '/monitor',
            name: 'Monitor',
            component: 'Layout',
            meta: { title: '监控测试' },
            children: [
              ...(legacy
                ? [
                    {
                      path: 'snailai',
                      name: 'OldConsole',
                      component: 'monitor/snailai/index',
                      meta: { title: '旧 AI 控制台' }
                    }
                  ]
                : []),
              {
                path: 'report',
                name: 'UnknownReport',
                component: 'monitor/report/index',
                meta: { title: '未知报表' },
                children: legacy ? [{ path: 'retired-child', component: 'ai/chat/index' }] : []
              },
              { path: 'snailjob', name: 'KeptJob', component: 'monitor/snailjob/index', meta: { title: '任务监控' } }
            ]
          }
        ];
      } else if (path === '/resource/message') {
        await route.fulfill({ contentType: 'text/event-stream', body: '' });
        return;
      } else if (path === '/resource/message/ticket') data = 'owned-retirement-ticket';
      await route.fulfill({ json: { code: 200, data } });
    });
    await page.addInitScript(() => localStorage.setItem('Admin-Token', 'owned-retirement-token'));
    await page.goto('/monitor/report');
    await expect(page.getByTestId('manifest-route-diagnostic')).toContainText('monitor/report/index');
    await expect(page.getByText('任务监控', { exact: true })).toBeVisible();
    await expect(page.getByText(/旧 AI/)).toHaveCount(0);
    await expect(page.locator('iframe')).toHaveCount(0);
    expect(recovery).toEqual(['identity', 'menus']);
    await page.reload();
    await expect(page.getByTestId('manifest-route-diagnostic')).toBeVisible();
    await expect(page.getByText(/旧 AI/)).toHaveCount(0);
    expect(recovery).toEqual(['identity', 'menus', 'identity', 'menus']);
    await page.goto('/legacy-ai/chat');
    await expect(page.getByText('404错误!', { exact: true })).toBeVisible();
    await expect(page.locator('iframe')).toHaveCount(0);
    expect(retiredRequests).toEqual([]);
  });
}
