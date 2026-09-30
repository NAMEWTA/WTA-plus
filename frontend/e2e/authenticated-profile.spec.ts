import { expect, test, type Page } from '@playwright/test';
import { join } from 'node:path';

async function screenshot(page: Page, name: string) {
  const directory = process.env.A11Y_EVIDENCE_DIR;
  if (directory)
    await page.screenshot({ path: join(directory, `${name}.png`), fullPage: true, animations: 'disabled' });
}

const origin = (app: 'home' | 'admin') => {
  const value = process.env[`A11Y_${app.toUpperCase()}_ORIGIN`];
  if (!value || new URL(value).hostname !== '127.0.0.1') throw new Error('Owned UI fixture required');
  return value;
};
const person = {
  fullName: '张三',
  documentTypeCode: 'CN_RESIDENT_ID',
  documentNumber: '110101199001011237',
  gender: 'MALE',
  birthDate: '1990-01-01',
  validFrom: '2020-01-01',
  validUntil: '2039-01-01'
};
const enterprise = {
  enterpriseName: '示例企业',
  unifiedCreditCode: '91310000MA1K123454',
  enterpriseType: '有限责任公司',
  legalRepresentativeName: '张三',
  legalDocumentTypeCode: 'CN_RESIDENT_ID',
  legalDocumentNumber: person.documentNumber,
  establishedDate: '2020-01-01',
  businessTermFrom: '',
  businessTermUntil: '',
  registeredAddress: '上海市测试路',
  businessScope: '软件服务',
  contactName: '张三',
  contactPhone: '13800138000',
  email: '',
  registeredCapital: 100,
  industryCode: '',
  website: '',
  handlerIsLegalRepresentative: true
};
const homeMenus = [
  {
    path: '/profile',
    name: 'ProfileCenter',
    component: 'profile/center/index',
    meta: { title: '个人中心', icon: 'tabler:id' },
    children: [
      {
        path: 'person',
        name: 'PersonVerification',
        component: 'profile/person/application',
        hidden: true,
        meta: { title: '实名认证', activeMenu: '/profile' }
      },
      {
        path: 'enterprise',
        name: 'EnterpriseVerification',
        component: 'profile/enterprise/application',
        hidden: true,
        meta: { title: '企业认证', activeMenu: '/profile' }
      }
    ]
  },
  {
    path: '/taskWaiting',
    name: 'HomeWaiting',
    component: 'workflow/task/taskWaiting',
    meta: { title: '我的待办', icon: 'tabler:inbox' }
  },
  {
    path: '/taskFinish',
    name: 'HomeFinished',
    component: 'workflow/task/taskFinish',
    meta: { title: '我的已办', icon: 'tabler:check' }
  },
  {
    path: '/person-review',
    name: 'PersonReview',
    component: 'profile/person/review',
    hidden: true,
    meta: { title: '实名认证审核', activeMenu: '/taskWaiting' }
  }
];
async function install(page: Page, app: 'home' | 'admin' = 'home') {
  const state = { enterpriseStatus: 'BACK', requests: [] as string[], decisions: [] as unknown[] };
  await page.addInitScript(
    key => localStorage.setItem(key, 'owned-ui-token'),
    app === 'home' ? 'Home-Token' : 'Admin-Token'
  );
  await page.route('**/prod-api/**', async route => {
    const path = new URL(route.request().url()).pathname.replace('/prod-api', '');
    state.requests.push(path);
    const send = (data: unknown) => route.fulfill({ json: { code: 200, data } });
    if (path === '/system/user/getInfo')
      return send({
        user: { userId: '8', userName: 'owned', nickName: '测试用户' },
        roles: [],
        permissions:
          app === 'admin'
            ? ['*:*:*']
            : [
                'profile:person:apply',
                'profile:enterprise:apply',
                'profile:enterprise:material',
                'profile:person:material',
                'profile:person:task-review',
                'workflow:task:participate',
                'system:oss:upload'
              ]
      });
    if (path === '/system/menu/getRouters')
      return send(
        app === 'home'
          ? homeMenus
          : [
              {
                path: '/shell',
                name: 'Shell',
                component: 'Layout',
                meta: { title: '档案', icon: 'tabler:user' },
                children: [
                  {
                    path: 'check',
                    name: 'ShellCheck',
                    component: 'profile/person/index',
                    meta: { title: '个人档案', icon: 'tabler:user' }
                  }
                ]
              }
            ]
      );
    if (path === '/profile/person/application/summary')
      return send({
        status: 'VERIFIED',
        returnReason: null,
        currentApplication: null,
        certifiedProfile: { profileId: '9', verifiedAt: '2026-09-30T10:00:00Z', identity: person }
      });
    const enterpriseApplication = () => ({
      ...enterprise,
      enterpriseApplicationId: '10',
      version: 2,
      snapshotVersion: 1,
      status: state.enterpriseStatus,
      providerCode: 'MANUAL',
      submittedTime: null,
      finishedTime: null
    });
    if (path === '/profile/enterprise/application/summary')
      return send({
        status: state.enterpriseStatus,
        returnReason: state.enterpriseStatus === 'BACK' ? '请核对联系电话' : null,
        currentApplication: enterpriseApplication(),
        certifiedProfile: null
      });
    if (path === '/profile/enterprise/application' && route.request().method() === 'POST')
      return send(enterpriseApplication());
    if (path === '/profile/enterprise/application/submit') {
      state.enterpriseStatus = 'WAITING';
      return send(enterpriseApplication());
    }
    if (path.startsWith('/profile/material-tags') || path.startsWith('/profile/enterprise/materials')) return send([]);
    if (path === '/workflow/task/pageByTaskWait' || path === '/workflow/task/pageByTaskFinish')
      return send({
        rows: [
          {
            id: path.endsWith('Finish') ? 'history-1' : 'task-1',
            taskId: path.endsWith('Finish') ? 'original-task-1' : undefined,
            businessId: '13',
            businessTitle: '实名认证',
            flowName: '实名认证审批',
            nodeName: '资料初审',
            formPath: '/profile/person/review'
          }
        ],
        total: 1
      });
    if (path.endsWith('/decision')) {
      state.decisions.push(route.request().postDataJSON());
      return send(null);
    }
    if (path.startsWith('/profile/person/review/tasks/'))
      return send({
        applicantUserId: '8',
        applicationId: '13',
        decisionVersion: 1,
        fieldSnapshotJson: JSON.stringify(person),
        materials: [],
        status: 'WAITING',
        submissionId: '14',
        submissionSeq: 3,
        submittedTime: '2026-09-30T10:00:00Z',
        version: 4
      });
    if (path === '/profile/person/archive') return send({ rows: [], total: 0 });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    return send([]);
  });
  return state;
}

test('个人中心两个认证入口，认证通过后刷新仍显示只读资料', async ({ page }) => {
  await install(page);
  await page.goto(origin('home') + '/profile');
  await expect(page.getByRole('heading', { name: '个人中心', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '企业认证', exact: true })).toBeEnabled();
  await expect(page.getByText('已认证', { exact: true })).toBeVisible();
  await expect(page.getByText('已退回', { exact: true })).toBeVisible();
  await screenshot(page, 'home-profile-center');
  await page.getByRole('button', { name: '实名认证', exact: true }).click();
  await expect(page.getByText('张三', { exact: true })).toBeVisible();
  await expect(page.getByText('已认证', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '保存草稿' })).toHaveCount(0);
  await page.reload();
  await expect(page.getByText('张三', { exact: true })).toBeVisible();
  await expect(page.locator('.sidebar-container .is-active')).toContainText('个人中心');
  await screenshot(page, 'home-certified-person');
});

test('退回原因可见，补充后重新提交并锁定表单', async ({ page }) => {
  const state = await install(page);
  await page.goto(origin('home') + '/profile/enterprise');
  await expect(page.getByText('退回原因：请核对联系电话')).toBeVisible();
  await screenshot(page, 'home-returned-enterprise');
  await expect(page.getByRole('button', { name: '提交认证' })).toBeEnabled();
  await page.getByRole('button', { name: '提交认证' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'OK', exact: true }).click();
  await expect(page.getByText('审核中', { exact: true })).toBeVisible();
  expect(state.enterpriseStatus).toBe('WAITING');
  await expect(page.getByRole('button', { name: '提交认证' })).toHaveCount(0);
  await page.reload();
  await expect(page.getByText('审核中', { exact: true })).toBeVisible();
});

test('Home 待办按菜单映射审核页，已办使用原始 taskId 且只读', async ({ page }) => {
  const state = await install(page);
  await page.goto(origin('home') + '/taskWaiting');
  await page.getByRole('button', { name: '办理', exact: true }).click();
  await expect(page).toHaveURL(/\/person-review\?/);
  await expect(page.getByText('张三', { exact: true })).toBeVisible();
  await expect(page.locator('pre')).toHaveCount(0);
  await screenshot(page, 'home-task-review');
  await page.locator('.decision-form textarea').fill('资料已核验');
  await page.getByRole('button', { name: '提交流程审核' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'OK', exact: true }).click();
  await expect(page).toHaveURL(/\/taskWaiting$/);
  expect(state.decisions).toEqual([{ decision: 'APPROVE', reason: '资料已核验', snapshotVersion: 3 }]);
  await page.goto(origin('home') + '/taskFinish');
  await page.getByRole('button', { name: '查看', exact: true }).click();
  await expect(page.getByText('当前记录为只读。')).toBeVisible();
  expect(state.requests).toContain('/profile/person/review/tasks/original-task-1');
  expect(state.requests).not.toContain('/profile/person/review/tasks/history-1');
  await expect(page.getByRole('button', { name: '提交流程审核' })).toHaveCount(0);
});

test('Admin 与 Home 使用相同壳层尺寸，Home 移动侧栏可关闭', async ({ browser }) => {
  const metrics: unknown[] = [];
  for (const app of ['admin', 'home'] as const) {
    const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, ignoreHTTPSErrors: true });
    await install(page, app);
    await page.goto(origin(app) + (app === 'admin' ? '/shell/check' : '/profile'));
    await expect(page.locator('.sidebar-container')).toBeVisible();
    await expect(page.locator('.ui-topbar')).toBeVisible();
    await screenshot(page, `${app}-desktop-shell`);
    metrics.push(
      await page.evaluate(() => {
        const sidebar = document.querySelector('.sidebar-container')!.getBoundingClientRect();
        const navbar = document.querySelector('.ui-topbar')!.getBoundingClientRect();
        return {
          sidebarX: sidebar.x,
          sidebarWidth: sidebar.width,
          navbarX: navbar.x,
          navbarHeight: navbar.height,
          sidebarBackground: getComputedStyle(document.querySelector('.sidebar-container')!).backgroundColor
        };
      })
    );
    {
      await page.setViewportSize({ width: 390, height: 844 });
      await expect(page.locator('.ui-app-shell')).toHaveClass(/mobile/);
      if (app === 'home') await page.getByRole('button', { name: '展开或收起导航菜单' }).click();
      else await page.locator('.hamburger-shell').click();
      await expect(page.getByRole('button', { name: '关闭导航菜单' })).toBeVisible();
      await page.getByRole('button', { name: '关闭导航菜单' }).click({ position: { x: 350, y: 400 } });
      await expect(page.getByRole('button', { name: '关闭导航菜单' })).toHaveCount(0);
      await expect(page.locator('.sidebar-container')).not.toBeInViewport();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
      await screenshot(page, `${app}-mobile-shell`);
    }
    await page.close();
  }
  expect(metrics[0]).toEqual(metrics[1]);
});
