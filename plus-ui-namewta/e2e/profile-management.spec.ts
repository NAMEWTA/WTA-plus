import { expect, test, type Page, type Route } from '@playwright/test';

type State = {
  completedTasks: unknown[];
  unknownRequests: string[];
};

const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });

const menus = [
  {
    path: '/profile',
    name: 'Profile',
    component: 'Layout',
    meta: { title: '档案管理' },
    children: [
      {
        path: 'materialTag',
        name: 'ProfileMaterialTag',
        component: 'profile/materialTag/index',
        meta: { title: '材料标签' }
      },
      { path: 'person', name: 'PersonProfile', component: 'profile/person/index', meta: { title: '个人档案' } },
      {
        path: 'person/detail',
        name: 'PersonProfileDetail',
        component: 'profile/person/detail',
        hidden: true,
        meta: { title: '个人详情' }
      },
      {
        path: 'person/review',
        name: 'PersonProfileReview',
        component: 'profile/person/review',
        hidden: true,
        meta: { title: '个人审核' }
      },
      {
        path: 'enterprise',
        name: 'EnterpriseProfile',
        component: 'profile/enterprise/index',
        meta: { title: '企业档案' }
      },
      {
        path: 'enterprise/detail',
        name: 'EnterpriseProfileDetail',
        component: 'profile/enterprise/detail',
        hidden: true,
        meta: { title: '企业详情' }
      },
      {
        path: 'enterprise/review',
        name: 'EnterpriseProfileReview',
        component: 'profile/enterprise/review',
        hidden: true,
        meta: { title: '企业审核' }
      }
    ]
  }
];

async function installApi(page: Page, state: State) {
  await page.route('**/prod-api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    if (path === '/auth/client/context')
      return json(route, { code: 200, data: { clientEnabled: true, registerEnabled: true } });
    if (path === '/auth/code') return json(route, { code: 200, data: { captchaEnabled: false } });
    if (path === '/auth/login') return json(route, { code: 200, data: { access_token: 'profile-e2e-token' } });
    if (path === '/system/user/getInfo')
      return json(route, {
        code: 200,
        data: {
          user: { userId: 1, userName: 'profile-admin', nickName: 'Profile Admin', avatarUrl: '' },
          roles: ['profile-admin'],
          permissions: [
            'profile:material-tag:query',
            'profile:material-tag:manage',
            'profile:person:query',
            'profile:person:review',
            'profile:person:material',
            'profile:person:manage',
            'profile:person:override',
            'profile:enterprise:query',
            'profile:enterprise:review',
            'profile:enterprise:material',
            'profile:enterprise:manage',
            'profile:enterprise:override'
          ]
        }
      });
    if (path === '/system/menu/getRouters') return json(route, { code: 200, data: menus });
    if (path === '/profile/material-tags/tree')
      return json(route, {
        code: 200,
        data: [
          {
            materialNodeId: 11,
            parentId: 0,
            nodeType: 'CATEGORY',
            nodeDepth: 1,
            scope: 'PERSON',
            materialTagCode: null,
            nodeName: '个人材料',
            systemRequired: false,
            enabled: true,
            orderNum: 1,
            version: 0,
            children: [
              {
                materialNodeId: 12,
                parentId: 11,
                nodeType: 'TAG',
                nodeDepth: 2,
                scope: 'PERSON',
                materialTagCode: 'PERSON_ID_CARD_PORTRAIT',
                nodeName: '居民身份证人像面',
                systemRequired: true,
                enabled: true,
                orderNum: 1,
                version: 0,
                children: []
              }
            ]
          }
        ]
      });
    if (path === '/profile/person/archive')
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              profileId: 101,
              previousProfileId: null,
              fullName: '张三',
              documentTypeCode: 'CN_RESIDENT_ID',
              documentNumber: '110101199001011234',
              gender: 'MALE',
              birthDate: '1990-01-01',
              status: 'ACTIVE',
              bindingUserId: 7,
              bindingStatus: 'ACTIVE',
              createTime: '2026-09-01T00:00:00Z'
            }
          ],
          total: 1
        }
      });
    if (path === '/profile/enterprise/archive')
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              profileId: 201,
              previousProfileId: null,
              enterpriseName: '示例科技有限公司',
              unifiedCreditCode: '91110000123456789X',
              enterpriseType: 'COMPANY',
              legalRepresentativeName: '张三',
              status: 'ACTIVE',
              bindingUserId: 7,
              bindingStatus: 'ACTIVE',
              createTime: '2026-09-01T00:00:00Z'
            }
          ],
          total: 1
        }
      });
    if (path === '/profile/person/archive/application/301/review-context')
      return json(route, {
        code: 200,
        data: {
          applicationId: 301,
          applicantUserId: 7,
          status: 'WAITING',
          submissionSeq: 1,
          decisionVersion: 0,
          version: 1,
          submissionId: 302,
          fieldSnapshotJson: '{"fullName":"张三","documentNumber":"110101199001011234"}',
          submittedTime: '2026-09-01T00:00:00Z',
          materials: []
        }
      });
    if (path === '/workflow/task/completeTask' && request.method() === 'POST') {
      state.completedTasks.push(request.postDataJSON());
      return json(route, { code: 200, data: null });
    }
    if (path === '/notify/inbox')
      return json(route, { code: 200, data: [] });
    if (path === '/resource/message/close') return json(route, { code: 200, data: null });
    if (path === '/resource/message') return route.fulfill({ contentType: 'text/event-stream', body: '' });
    state.unknownRequests.push(`${request.method()} ${path}`);
    return json(route, { code: 200, data: null });
  });
}

test('profile manifests resolve all management surfaces and submit a workflow rejection', async ({ page }) => {
  const state: State = { completedTasks: [], unknownRequests: [] };
  await installApi(page, state);

  await page.goto('/login?redirect=%2Fprofile%2FmaterialTag');
  await expect(page.locator('.submit-button')).toBeEnabled();
  await page.locator('.submit-button').click();

  await expect(page.getByRole('heading', { name: '材料标签' })).toBeVisible();
  await expect(page.getByText('居民身份证人像面', { exact: true })).toBeVisible();

  await page.locator('.sidebar-container').getByText('个人档案', { exact: true }).click();
  await expect(page.getByRole('heading', { name: '个人档案' })).toBeVisible();
  await expect(page.getByText('110101199001011234', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '新建档案' })).toBeVisible();

  await page.locator('.sidebar-container').getByText('企业档案', { exact: true }).click();
  await expect(page.getByRole('heading', { name: '企业档案' })).toBeVisible();
  await expect(page.getByText('示例科技有限公司', { exact: true })).toBeVisible();

  await page.goto('/profile/person/review?id=301&taskId=task-901');
  await expect(page.getByRole('heading', { name: '个人认证审核' })).toBeVisible();
  await expect(page.getByText('不可变申请快照', { exact: true })).toBeVisible();
  await page.getByText('驳回', { exact: true }).click();
  await page.getByRole('textbox', { name: '审核意见' }).fill('材料信息不一致');
  await page.getByRole('button', { name: '提交流程审核' }).click();
  await page.getByRole('button', { name: '确定' }).click();

  await expect
    .poll(() => state.completedTasks)
    .toEqual([
      {
        taskId: 'task-901',
        message: '材料信息不一致',
        variables: { profileDecision: 'REJECT' }
      }
    ]);
  expect(state.unknownRequests).toEqual([]);
});
