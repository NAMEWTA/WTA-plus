import { expect, test } from '@playwright/test';

const policy = {
  minimumLength: 8,
  maximumLength: 30,
  requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'],
  allowedSpecialCharacters: '@$!%*?&'
};

for (const app of ['admin', 'home'] as const) {
  for (const missingPhone of ['', '   ']) {
    test(`${app} registration rejects ${missingPhone ? 'blank' : 'missing'} phone and transmits the valid value`, async ({
      page
    }) => {
      const writes: Record<string, unknown>[] = [];
      await page.route('**/prod-api/**', async route => {
        const path = new URL(route.request().url()).pathname;
        if (path.endsWith('/auth/client/context')) {
          await route.fulfill({
            json: { code: 200, data: { clientEnabled: true, registerEnabled: true, passwordPolicy: policy } }
          });
        } else if (path.endsWith('/auth/code')) {
          await route.fulfill({ json: { code: 200, data: { captchaEnabled: false } } });
        } else if (path.endsWith('/auth/register')) {
          writes.push(route.request().postDataJSON() as Record<string, unknown>);
          await route.fulfill({ json: { code: 200 } });
        } else {
          await route.fulfill({ json: { code: 200, data: [] } });
        }
      });
      await page.goto(`http://127.0.0.1:${app === 'admin' ? 4173 : 4174}/register`);
      const form = page.locator('.register-form');
      const phone = form.getByRole('textbox', { name: '手机号码', exact: true });
      await expect(phone).toBeVisible();
      await form.locator('input').first().fill('phone-user');
      await form.locator('input[type="password"]').nth(0).fill('OwnedPass!9');
      await form.locator('input[type="password"]').nth(1).fill('OwnedPass!9');
      const submit = form.getByRole('button', { name: app === 'admin' ? '注 册' : '注册', exact: true });
      await phone.fill(missingPhone);
      await submit.click();
      await expect(
        form.getByText(app === 'admin' ? '手机号码不能为空' : '请输入有效的手机号码', { exact: true })
      ).toBeVisible();
      expect(writes).toHaveLength(0);
      await phone.fill('13800138000');
      await submit.click();
      await expect.poll(() => writes.length).toBe(1);
      expect(writes[0]?.phoneNumber).toBe('13800138000');
    });
  }
}

for (const entry of ['add', 'edit', 'profile'] as const) {
  for (const missingPhone of ['', '   ']) {
    test(`admin ${entry} rejects ${missingPhone ? 'blank' : 'missing'} phone before saving`, async ({ page }) => {
      const writes: Record<string, unknown>[] = [];
      const user = {
        userId: 42,
        userName: 'legacy-user',
        nickName: 'Legacy User',
        phoneNumber: '',
        email: 'legacy@example.test',
        status: '0',
        userTypeIds: [9]
      };
      await page.route('**/prod-api/**', async route => {
        const path = new URL(route.request().url()).pathname.replace('/prod-api', '');
        let data: unknown = [];
        if (path === '/auth/client/context')
          data = { clientEnabled: true, registerEnabled: true, passwordPolicy: policy };
        else if (path === '/system/user/getInfo') data = { user, roles: ['admin'], permissions: ['*:*:*'] };
        else if (path === '/system/menu/getRouters')
          data = [
            {
              path: '/system',
              name: 'System',
              component: 'Layout',
              children: [{ path: 'user', name: 'User', component: 'system/user/index', meta: { title: '用户管理' } }]
            }
          ];
        else if (path === '/system/user/list') data = { rows: [user], total: 1 };
        else if (path === '/system/user/profile' && route.request().method() === 'GET')
          data = { user, roleGroup: 'admin', postGroup: '' };
        else if (path === '/system/user/42' || path === '/system/user/')
          data = { user, roles: [], posts: [], roleIds: [], postIds: [], password: 'OwnedPass!9' };
        else if (path.includes('password') && path.includes('policy')) data = policy;
        else if (path.includes('userType'))
          data = [{ userTypeId: 9, userTypeName: '测试登录域', userTypeCode: 'owned', status: '0' }];
        else if (
          route.request().method() === 'POST' &&
          ['/system/user', '/system/user/update', '/system/user/profile'].includes(path)
        ) {
          writes.push(route.request().postDataJSON() as Record<string, unknown>);
          data = null;
        } else if (path === '/resource/message') {
          await route.fulfill({ contentType: 'text/event-stream', body: '' });
          return;
        } else if (path === '/resource/message/ticket') data = 'owned-phone-ticket';
        await route.fulfill({ json: { code: 200, data } });
      });
      await page.addInitScript(() => localStorage.setItem('Admin-Token', 'owned-phone-token'));
      await page.goto(`http://127.0.0.1:4173${entry === 'profile' ? '/user/profile' : '/system/user'}`);
      if (entry !== 'profile') {
        if (entry === 'edit') {
          await page.locator('.el-table__body-wrapper .el-checkbox').first().click();
        }
        await page
          .getByRole('button', { name: entry === 'add' ? '新增' : '修改', exact: true })
          .first()
          .click();
      }
      const form = entry === 'profile' ? page.locator('.profile-form') : page.getByRole('dialog');
      const phone = form.getByRole('textbox', { name: /手机号码/ });
      await expect(phone).toBeVisible();
      if (entry === 'add') {
        await form.getByPlaceholder('请输入用户昵称').fill('New Phone User');
        await form.getByPlaceholder('请输入用户名称').fill('new-phone-user');
        await form.getByText('请选择登录域', { exact: true }).click();
        await page.getByRole('option', { name: '测试登录域' }).click();
        await phone.click();
      }
      const submit = form.getByRole('button', { name: entry === 'profile' ? '保存' : '确 定', exact: true });
      await phone.fill(missingPhone);
      await submit.click();
      await expect(form.getByText('手机号码不能为空', { exact: true })).toBeVisible();
      expect(writes).toHaveLength(0);
      await phone.fill('13800138004');
      await submit.click();
      await expect.poll(() => writes.length).toBe(1);
      expect(writes[0]?.phoneNumber).toBe('13800138004');
    });
  }
}
