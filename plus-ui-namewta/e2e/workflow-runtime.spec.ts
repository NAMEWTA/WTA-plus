import { expect, test, type Page, type Route } from '@playwright/test';

type RuntimeState = {
  backBodies?: unknown[];
  cancelBodies?: unknown[];
  completeBodies: unknown[];
  failBack?: boolean;
  failComplete: boolean;
  failUsers: boolean;
  invalidBodies: unknown[];
  leaveBodies?: unknown[];
  network: string[];
  operationBodies: { operation: string; body: unknown }[];
  permissions: string[];
  terminationBodies: unknown[];
  workflowStartBodies?: unknown[];
  unknown: string[];
  urgeBodies: unknown[];
  userListUrls?: string[];
  variableBodies?: unknown[];
};

const json = (route: Route, body: unknown) =>
  route.fulfill({ contentType: 'application/json', body: JSON.stringify(body) });

const menus = [
  {
    path: '/workflow',
    name: 'Workflow',
    component: 'Layout',
    meta: { title: '工作流' },
    children: [
      {
        path: 'task/taskWaiting',
        name: 'taskWaiting',
        component: 'workflow/task/taskWaiting',
        meta: { title: '我的待办' }
      },
      {
        path: 'processInstance/index',
        name: 'processInstance',
        component: 'workflow/processInstance/index',
        meta: { title: '流程实例' }
      },
      {
        path: 'leave/index',
        name: 'leave',
        component: 'workflow/leave/index',
        meta: { title: '请假管理' }
      },
      {
        path: 'task/allTaskWaiting',
        name: 'allTaskWaiting',
        component: 'workflow/task/allTaskWaiting',
        meta: { title: '待办任务' }
      },
      {
        path: 'task/myDocument',
        name: 'myDocument',
        component: 'workflow/task/myDocument',
        meta: { title: '我的单据' }
      },
      {
        path: 'leaveEdit/index',
        name: 'leaveEdit',
        component: 'workflow/leave/leaveEdit',
        hidden: true,
        meta: { title: '请假申请' }
      }
    ]
  }
];

async function installRuntimeApi(page: Page, state: RuntimeState) {
  await page.route('**/workflow-runtime-upload', route => {
    if (!['GET', 'PUT'].includes(route.request().method())) return route.abort('failed');
    return route.fulfill({
      status: 200,
      headers: { ETag: 'workflow-etag', 'access-control-expose-headers': 'ETag' }
    });
  });
  await page.route('**/prod-api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/prod-api', '');
    const method = request.method();
    state.network.push(`${method} ${path}`);
    if (path === '/auth/client/context' && method === 'GET')
      return json(route, { code: 200, data: { clientEnabled: true, registerEnabled: true } });
    if (path === '/auth/code' && method === 'GET') return json(route, { code: 200, data: { captchaEnabled: false } });
    if (path === '/auth/login' && method === 'POST')
      return json(route, { code: 200, data: { access_token: 'workflow-runtime-token' } });
    if (path === '/system/user/getInfo' && method === 'GET')
      return json(route, {
        code: 200,
        data: {
          user: { userId: '1', userName: 'operator', nickName: '流程操作员', avatarUrl: '' },
          roles: ['operator'],
          permissions: state.permissions
        }
      });
    if (path === '/system/menu/getRouters' && method === 'GET') return json(route, { code: 200, data: menus });
    if (path === '/workflow/task/pageByTaskWait' && method === 'GET')
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              id: 'task-1',
              instanceId: 'instance-1',
              businessId: 'leave-1',
              businessCode: 'LEAVE-1',
              businessTitle: '研发请假',
              flowCode: 'leave1',
              flowName: '请假审批',
              flowStatus: 'waiting',
              nodeCode: 'approve',
              nodeName: '部门审批',
              nodeType: 1,
              nodeRatio: 1,
              formCustom: 'N',
              formPath: '/workflow/leaveEdit/index'
            }
          ],
          total: 1
        }
      });
    if (path === '/workflow/task/pageByAllTaskWait' && method === 'GET')
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              id: 'task-2',
              instanceId: 'instance-2',
              businessId: 'leave-2',
              businessCode: 'LEAVE-2',
              businessTitle: '受限任务',
              flowCode: 'leave1',
              flowName: '请假审批',
              flowStatus: 'waiting',
              nodeCode: 'approve',
              nodeName: '部门审批',
              nodeType: 1,
              nodeRatio: 1,
              formCustom: 'N',
              formPath: '/workflow/leaveEdit/index'
            }
          ],
          total: 1
        }
      });
    if (path === '/workflow/task/pageByAllTaskFinish' && method === 'GET')
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              id: 'task-finished',
              instanceId: 'instance-finished',
              businessId: 'leave-finished',
              businessCode: 'LEAVE-FINISHED',
              businessTitle: '已完成任务',
              flowCode: 'leave1',
              flowName: '请假审批',
              flowStatus: 'finish',
              nodeCode: 'approve',
              nodeName: '部门审批',
              nodeType: 1,
              nodeRatio: 1,
              formCustom: 'N',
              formPath: '/workflow/leaveEdit/index'
            }
          ],
          total: 1
        }
      });
    if (path.startsWith('/workflow/task/getTask/') && method === 'GET') {
      const id = decodeURIComponent(path.slice(path.lastIndexOf('/') + 1));
      return json(route, {
        code: 200,
        data: {
          id,
          instanceId: `instance-${id}`,
          businessId: id === 'task-1' ? 'leave-1' : 'leave-2',
          businessCode: 'LEAVE-1',
          businessTitle: '请假审批',
          flowCode: 'leave1',
          flowName: '请假审批',
          flowStatus: 'waiting',
          nodeCode: 'approve',
          nodeName: '部门审批',
          nodeType: 1,
          nodeRatio: 1,
          formCustom: 'N',
          formPath: '/workflow/leaveEdit/index',
          copyList: [],
          buttonList:
            id === 'task-2'
              ? []
              : [
                  { code: 'pop', show: true },
                  { code: 'copy', show: true },
                  { code: 'file', show: true },
                  { code: 'trust', show: true },
                  { code: 'transfer', show: true },
                  { code: 'addSign', show: true },
                  { code: 'subSign', show: true },
                  { code: 'termination', show: true },
                  { code: 'back', show: true }
                ]
        }
      });
    }
    if (path === '/workflow/task/getNextNodeList' && method === 'POST')
      return json(route, {
        code: 200,
        data: [{ nodeCode: 'review', nodeName: '部门复核', permissionFlag: '7' }]
      });
    if (path.startsWith('/workflow/task/currentTaskAllUser/') && method === 'GET')
      return json(route, {
        code: 200,
        data: [{ userId: '7', nickName: '流程负责人', email: 'private@example.test', phoneNumber: '13800000000' }]
      });
    if (path === '/workflow/task/terminationTask' && method === 'POST') {
      state.terminationBodies.push(request.postDataJSON());
      return json(route, { code: 200 });
    }
    if (path.startsWith('/workflow/task/taskOperation/') && method === 'POST') {
      state.operationBodies.push({ operation: path.slice(path.lastIndexOf('/') + 1), body: request.postDataJSON() });
      return json(route, { code: 200 });
    }
    if (path.startsWith('/workflow/task/getBackTaskNode/') && method === 'GET')
      return json(route, { code: 200, data: [{ nodeCode: 'start', nodeName: '申请人' }] });
    if (path === '/workflow/task/backProcess' && method === 'POST') {
      if (state.failBack) return json(route, { code: 500, msg: '退回任务失败' });
      state.backBodies?.push(request.postDataJSON());
      return json(route, { code: 200 });
    }
    if (path === '/workflow/category/categoryTree' && method === 'GET')
      return json(route, { code: 200, data: [{ id: '0', parentId: '0', label: '全部流程', weight: 0, children: [] }] });
    if (path === '/workflow/instance/pageByRunning' && method === 'GET')
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              id: 'instance-1',
              businessId: 'leave-1',
              businessCode: 'LEAVE-1',
              businessTitle: '待作废流程',
              definitionId: 'definition-1',
              flowCode: 'leave1',
              flowName: '请假审批',
              flowStatus: 'waiting',
              flowStatusName: '审批中',
              flowTaskList: [],
              activityStatus: 1,
              version: '1'
            }
          ],
          total: 1
        }
      });
    if (path === '/workflow/instance/invalid' && method === 'POST') {
      state.invalidBodies.push(request.postDataJSON());
      return json(route, { code: 200 });
    }
    if (path === '/workflow/instance/instanceVariable/instance-1' && method === 'GET')
      return json(route, { code: 200, data: { variable: { amount: '100' } } });
    if (path === '/workflow/instance/updateVariable' && method === 'PUT') {
      state.variableBodies?.push(request.postDataJSON());
      return json(route, { code: 200 });
    }
    if (path === '/workflow/instance/pageByCurrent' && method === 'GET')
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              id: 'instance-1',
              businessId: 'leave-1',
              businessCode: 'LEAVE-1',
              businessTitle: '我的请假单',
              definitionId: 'definition-1',
              flowCode: 'leave1',
              flowName: '请假审批',
              flowStatus: 'waiting',
              flowStatusName: '审批中',
              flowTaskList: [],
              activityStatus: 1,
              version: '1'
            }
          ],
          total: 1
        }
      });
    if (path === '/workflow/instance/cancelProcessApply' && method === 'PUT') {
      state.cancelBodies?.push(request.postDataJSON());
      return json(route, { code: 200 });
    }
    if (path === '/workflow/task/urgeTask' && method === 'POST') {
      state.urgeBodies.push(request.postDataJSON());
      return json(route, { code: 200 });
    }
    if (path === '/workflow/leave/list' && method === 'GET')
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              id: 'draft-1',
              leaveType: '2',
              startDate: '2026-08-26',
              endDate: '2026-08-26',
              leaveDays: 1,
              remark: '草稿',
              status: 'draft'
            },
            {
              id: 'waiting-1',
              leaveType: '4',
              startDate: '2026-08-27',
              endDate: '2026-08-28',
              leaveDays: 2,
              remark: '审批中',
              status: 'waiting'
            }
          ],
          total: 2
        }
      });
    if (path === '/workflow/leave' && method === 'POST') {
      const body = request.postDataJSON() as Record<string, unknown>;
      state.leaveBodies?.push(body);
      return json(route, {
        code: 200,
        data: { id: 'leave-created', applyCode: 'LEAVE-CREATED', status: 'draft', ...body }
      });
    }
    if (path === '/workflow/task/startWorkFlow' && method === 'POST') {
      state.workflowStartBodies?.push(request.postDataJSON());
      return json(route, { code: 200, data: { taskId: 'task-1' } });
    }
    const leaveMatch = path.match(/^\/workflow\/leave\/(leave-\d+)$/);
    if (leaveMatch && method === 'GET')
      return json(route, {
        code: 200,
        data: {
          id: leaveMatch[1],
          applyCode: leaveMatch[1].toUpperCase(),
          leaveType: '1',
          startDate: '2026-08-26',
          endDate: '2026-08-27',
          leaveDays: 2,
          remark: '原始请假原因',
          status: 'waiting'
        }
      });
    if (/^\/workflow\/instance\/flowHisTaskList\/leave-\d+$/.test(path) && method === 'GET')
      return json(route, {
        code: 200,
        data: { instanceId: 'instance-1', list: [{ id: 'history-1', nodeName: '提交申请', createTime: '2026-08-26' }] }
      });
    if (path === '/system/user/list' && method === 'GET') {
      state.userListUrls?.push(request.url());
      if (state.failUsers) return json(route, { code: 500, msg: '用户查询失败' });
      const candidateLimited = new URL(request.url()).searchParams.get('userIds') === '7';
      return json(route, {
        code: 200,
        data: {
          rows: [
            {
              userId: '7',
              userName: 'owner',
              nickName: '流程负责人',
              deptName: '研发部',
              status: '0',
              email: 'private@example.test',
              phoneNumber: '13800000000'
            },
            ...(candidateLimited
              ? []
              : [{ userId: '8', userName: 'forbidden', nickName: '越权候选人', deptName: '研发部', status: '0' }])
          ],
          total: candidateLimited ? 1 : 2
        }
      });
    }
    if (path === '/system/user/deptTree' && method === 'GET')
      return json(route, { code: 200, data: [{ id: '10', label: '研发部', children: [] }] });
    if (path.startsWith('/system/user/optionselect') && method === 'GET')
      return json(route, {
        code: 200,
        data: [
          {
            userId: '7',
            userName: 'owner',
            nickName: '流程负责人',
            deptName: '研发部',
            status: '0',
            email: 'private@example.test'
          }
        ]
      });
    if (path === '/workflow/task/completeTask' && method === 'POST') {
      if (state.failComplete) return json(route, { code: 500, msg: '任务办理失败' });
      state.completeBodies.push(request.postDataJSON());
      return json(route, { code: 200, data: null });
    }
    if (path === '/resource/oss/uploads' && method === 'POST') {
      const input = request.postDataJSON() as { fileName: string; fileSize: number };
      return json(route, {
        code: 200,
        data: {
          uploadToken: 'workflow-upload',
          mode: 'SINGLE',
          expiresAt: '2099-01-01T00:00:00Z',
          fileName: input.fileName,
          fileSize: input.fileSize,
          presignedRequest: {
            method: 'PUT',
            url: 'http://127.0.0.1:4173/workflow-runtime-upload',
            requiredHeaders: {},
            expiresAt: '2099-01-01T00:00:00Z'
          }
        }
      });
    }
    if (path === '/resource/oss/uploads/workflow-upload/complete' && method === 'POST')
      return json(route, { code: 200, data: 'oss-workflow' });
    if (path === '/resource/oss/listByIds/oss-workflow' && method === 'GET')
      return json(route, {
        code: 200,
        data: [{ ossId: 'oss-workflow', originalName: 'approval.txt', fileName: 'approval.txt', url: '' }]
      });
    if (path === '/resource/oss/oss-workflow/download-url' && method === 'GET')
      return json(route, {
        code: 200,
        data: {
          url: 'http://127.0.0.1:4173/workflow-runtime-upload',
          fileName: 'approval.txt',
          expiresAt: '2099-01-01'
        }
      });
    if (path === '/notify/inbox' && method === 'GET')
      return json(route, { code: 200, data: [] });
    if (path === '/resource/message' && method === 'GET')
      return route.fulfill({ contentType: 'text/event-stream', body: '' });
    state.unknown.push(`${method} ${path}`);
    return route.abort('failed');
  });
}

async function loginToTask(page: Page, state: RuntimeState) {
  await installRuntimeApi(page, state);
  await page.goto('/login?redirect=%2Fworkflow%2Ftask%2FtaskWaiting');
  await page.locator('.submit-button').click();
  await expect(page).toHaveURL(/\/workflow\/task\/taskWaiting$/);
  await expect(page.getByRole('cell', { name: '研发请假' })).toBeVisible();
  await page.getByRole('button', { name: '办理', exact: true }).click();
  await expect(page).toHaveURL(/\/workflow\/leaveEdit\/index\?.*taskId=task-1/);
  await expect(page).toHaveURL(/type=approval/);
  await expect(page.locator('.el-form-item').filter({ hasText: '请假原因' }).getByRole('textbox')).toHaveValue(
    '原始请假原因'
  );
  await expect(page.getByRole('button', { name: '保存', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '提交审批', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '后端发起', exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: '办理任务', exact: true }).click();
  await expect(page.getByRole('dialog', { name: '流程办理' })).toBeVisible();
}

test('admin selected workflow completes a task through the public user seam', async ({ page }) => {
  const state: RuntimeState = {
    completeBodies: [],
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:task:list', 'workflow:leave:query', 'workflow:task:edit'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: [],
    userListUrls: []
  };
  await loginToTask(page, state);
  const processDialog = page.getByRole('dialog', { name: '流程办理' });
  await processDialog.getByLabel('审批意见').fill('同意办理');
  await processDialog.locator('input[type="file"]').setInputFiles({
    name: 'approval.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('workflow attachment')
  });
  await expect(processDialog.getByRole('link', { name: 'approval.txt' }).first()).toBeVisible();
  await processDialog.getByRole('button', { name: '选择', exact: true }).click();
  const selector = page.getByRole('dialog', { name: '选择用户' });
  await expect(selector.getByRole('cell', { name: '流程负责人' })).toBeVisible();
  await expect(selector.getByText('private@example.test', { exact: true })).toHaveCount(0);
  await expect(selector.getByText('13800000000', { exact: true })).toHaveCount(0);
  await expect(selector.getByText('越权候选人', { exact: true })).toHaveCount(0);
  await selector.locator('label.el-checkbox').first().click();
  await selector.getByRole('button', { name: '确定', exact: true }).click();
  await processDialog.getByRole('button', { name: '提交', exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect(page).toHaveURL(/\/workflow\/task\/taskWaiting$/);
  await expect
    .poll(() => state.completeBodies)
    .toEqual([
      {
        taskId: 'task-1',
        message: '同意办理',
        messageType: ['1'],
        variables: { leaveDays: 2, userList: ['1', '3', '4'] },
        assigneeMap: { review: '7' },
        flowCopyList: [],
        fileId: 'oss-workflow'
      }
    ]);
  expect(state.network).toContain('GET /system/user/list');
  expect(state.userListUrls?.some(url => new URL(url).searchParams.get('userIds') === '7')).toBe(true);
  expect(state.userListUrls?.some(url => url.includes('userIds%5B0%5D'))).toBe(false);
  expect(state.unknown).toEqual([]);
});

test('user and task failures remain visible without clearing approval input', async ({ page }) => {
  const state: RuntimeState = {
    completeBodies: [],
    failComplete: true,
    failUsers: true,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:task:list', 'workflow:leave:query', 'workflow:task:edit'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: []
  };
  await loginToTask(page, state);
  const processDialog = page.getByRole('dialog', { name: '流程办理' });
  await processDialog.getByLabel('审批意见').fill('保留这段审批意见');
  await processDialog.getByRole('button', { name: '选择', exact: true }).click();
  const selector = page.getByRole('dialog', { name: '选择用户' });
  await expect(selector.getByText('用户查询失败', { exact: true })).toBeVisible();
  state.failUsers = false;
  await selector.getByRole('button', { name: '搜索', exact: true }).click();
  await selector.locator('label.el-checkbox').first().click();
  await selector.getByRole('button', { name: '确定', exact: true }).click();
  await processDialog.getByRole('button', { name: '提交', exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect(processDialog.getByText('任务办理失败', { exact: true })).toBeVisible();
  await expect(processDialog.getByLabel('审批意见')).toHaveValue('保留这段审批意见');
  expect(state.completeBodies).toEqual([]);
  expect(state.unknown).toEqual([]);
});

test('participant actions execute delegate transfer add-sign and reduce-sign contracts', async ({ page }) => {
  const state: RuntimeState = {
    completeBodies: [],
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:task:list', 'workflow:leave:query'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: []
  };
  await loginToTask(page, state);

  const chooseFirstUser = async () => {
    const selector = page.getByRole('dialog', { name: '选择用户' });
    await expect(selector.getByRole('cell', { name: '流程负责人' })).toBeVisible();
    await selector.getByRole('row').filter({ hasText: '流程负责人' }).locator('label.el-checkbox').click();
    await selector.getByRole('button', { name: '确定', exact: true }).click();
    await expect(selector).toBeHidden();
    const confirmation = page.getByRole('dialog', { name: '系统提示' });
    await expect(confirmation).toBeVisible();
    await confirmation.getByRole('button', { name: '确定', exact: true }).click();
    await expect(confirmation).toBeHidden();
  };
  const reopen = async () => {
    await expect(page).toHaveURL(/\/workflow\/task\/taskWaiting$/);
    await page.getByRole('button', { name: '办理', exact: true }).click();
    await expect(page).toHaveURL(/\/workflow\/leaveEdit\/index\?.*taskId=task-1/);
    await page.getByRole('button', { name: '办理任务', exact: true }).click();
    await expect(page.getByRole('dialog', { name: '流程办理' })).toBeVisible();
  };

  let processDialog = page.getByRole('dialog', { name: '流程办理' });
  await processDialog.getByRole('button', { name: '委托', exact: true }).click();
  await chooseFirstUser();
  await reopen();
  processDialog = page.getByRole('dialog', { name: '流程办理' });
  await processDialog.getByRole('button', { name: '转办', exact: true }).click();
  await chooseFirstUser();
  await reopen();
  processDialog = page.getByRole('dialog', { name: '流程办理' });
  await processDialog.getByRole('button', { name: '加签', exact: true }).click();
  await chooseFirstUser();
  await reopen();
  processDialog = page.getByRole('dialog', { name: '流程办理' });
  await processDialog.getByRole('button', { name: '减签', exact: true }).click();
  const reduction = page.getByRole('dialog', { name: '选择减签人员' });
  await reduction.getByRole('button', { name: '减签', exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();

  await expect
    .poll(() => state.operationBodies.map(item => item.operation))
    .toEqual(['delegateTask', 'transferTask', 'addSignature', 'reductionSignature']);
  expect(state.operationBodies).toEqual([
    { operation: 'delegateTask', body: { taskId: 'task-1', userId: '7', message: '', messageType: ['1'] } },
    { operation: 'transferTask', body: { taskId: 'task-1', userId: '7', message: '', messageType: ['1'] } },
    { operation: 'addSignature', body: { taskId: 'task-1', userIds: ['7'], message: '', messageType: ['1'] } },
    {
      operation: 'reductionSignature',
      body: { taskId: 'task-1', userIds: ['7'], message: '', messageType: ['1'] }
    }
  ]);
  expect(state.unknown).toEqual([]);
});

test('task edit controls fail closed when the permission contribution is absent', async ({ page }) => {
  const state: RuntimeState = {
    completeBodies: [],
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:task:list'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: []
  };
  await installRuntimeApi(page, state);
  await page.goto('/login?redirect=%2Fworkflow%2Ftask%2FallTaskWaiting');
  await page.locator('.submit-button').click();
  await expect(page).toHaveURL(/\/workflow\/task\/allTaskWaiting$/);
  await expect(page.getByRole('cell', { name: '受限任务' })).toBeVisible();
  await expect(page.getByRole('button', { name: '催办', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '修改办理人', exact: true })).toHaveCount(0);
  expect(state.unknown).toEqual([]);
});

test('all-task runtime separates finished work and sends exact urge payload', async ({ page }) => {
  const state: RuntimeState = {
    completeBodies: [],
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:task:list', 'workflow:task:edit'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: []
  };
  await installRuntimeApi(page, state);
  await page.goto('/login?redirect=%2Fworkflow%2Ftask%2FallTaskWaiting');
  await page.locator('.submit-button').click();
  await expect(page.getByRole('cell', { name: '受限任务' })).toBeVisible();
  await page.getByRole('button', { name: '催办', exact: true }).click();
  await page.getByRole('dialog', { name: '任务催办' }).locator('label.el-checkbox').filter({ hasText: '邮件' }).click();
  await page.getByPlaceholder('请输入催办消息').fill('请在今天完成');
  await page.getByRole('dialog', { name: '任务催办' }).getByRole('button', { name: '确定', exact: true }).click();
  await expect
    .poll(() => state.urgeBodies)
    .toEqual([{ taskIdList: ['task-2'], message: '请在今天完成', messageType: ['1', '2'] }]);
  await page.getByRole('row').filter({ hasText: '受限任务' }).locator('label.el-checkbox').click();
  await page.getByRole('button', { name: '流程干预', exact: true }).click();
  const processDialog = page.getByRole('dialog', { name: '流程办理' });
  await expect(processDialog.getByRole('button', { name: '转办', exact: true })).toBeVisible();
  await expect(processDialog.getByRole('button', { name: '加签', exact: true })).toBeVisible();
  await expect(processDialog.getByRole('button', { name: '减签', exact: true })).toBeVisible();
  await processDialog.getByLabel('审批意见').fill('任务已不再需要');
  await processDialog.getByRole('button', { name: '终止', exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect.poll(() => state.terminationBodies).toEqual([{ taskId: 'task-2', comment: '任务已不再需要' }]);
  await page.getByRole('tab', { name: '已办任务' }).click();
  await expect(page.getByRole('cell', { name: '已完成任务' })).toBeVisible();
  await expect(page.getByRole('button', { name: '催办', exact: true })).toHaveCount(0);
  await page.getByRole('tab', { name: '待办任务' }).click();
  await page.getByRole('button', { name: '查看', exact: true }).click();
  await expect(page).toHaveURL(/type=view/);
  expect(state.network).toContain('GET /workflow/task/pageByAllTaskFinish');
  expect(state.unknown).toEqual([]);
});

test('instance invalidation requires a reason and posts exact FlowInvalidBo', async ({ page }) => {
  const state: RuntimeState = {
    completeBodies: [],
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:instance:list', 'workflow:instance:invalid'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: []
  };
  await installRuntimeApi(page, state);
  await page.goto('/login?redirect=%2Fworkflow%2FprocessInstance%2Findex');
  await page.locator('.submit-button').click();
  await expect(page.getByRole('cell', { name: '待作废流程' })).toBeVisible();
  await page.getByRole('button', { name: '作废', exact: true }).click();
  await page.getByPlaceholder('请输入作废原因').fill('申请内容失效');
  await page.getByRole('dialog', { name: '作废流程' }).getByRole('button', { name: '确认作废' }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect.poll(() => state.invalidBodies).toEqual([{ id: 'instance-1', comment: '申请内容失效' }]);
  expect(state.unknown).toEqual([]);
});

test('task back uploads a real attachment and preserves the exact payload', async ({ page }) => {
  const state: RuntimeState = {
    backBodies: [],
    completeBodies: [],
    failBack: true,
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:task:list', 'workflow:leave:query'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: []
  };
  await loginToTask(page, state);
  const processDialog = page.getByRole('dialog', { name: '流程办理' });
  await processDialog.getByRole('button', { name: '退回', exact: true }).click();
  const backDialog = page.getByRole('dialog', { name: '退回任务' });
  await backDialog.getByPlaceholder('请输入退回意见').fill('首次退回失败');
  await backDialog.locator('input[type="file"]').setInputFiles({
    name: 'approval.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('back attachment')
  });
  await expect(backDialog.getByRole('link', { name: 'approval.txt' }).first()).toBeVisible();
  await backDialog.getByRole('button', { name: '确认退回', exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect(backDialog.getByText('退回任务失败', { exact: true })).toBeVisible();
  await expect(backDialog.getByPlaceholder('请输入退回意见')).toHaveValue('首次退回失败');
  await expect(backDialog.getByRole('link', { name: 'approval.txt' }).first()).toBeVisible();
  expect(state.backBodies).toEqual([]);

  await backDialog.getByPlaceholder('请输入退回意见').fill('请补充附件');
  state.failBack = false;
  await backDialog.getByRole('button', { name: '确认退回', exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect
    .poll(() => state.backBodies)
    .toEqual([
      {
        taskId: 'task-1',
        nodeCode: 'start',
        message: '请补充附件',
        messageType: ['1'],
        variables: { leaveDays: 2, userList: ['1', '3', '4'] },
        fileId: 'oss-workflow'
      }
    ]);
  expect(state.unknown).toEqual([]);
});

test('instance variables can be updated under the exact permission and payload', async ({ page }) => {
  const state: RuntimeState = {
    completeBodies: [],
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:instance:list', 'workflow:instance:variableQuery', 'workflow:instance:variable'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: [],
    variableBodies: []
  };
  await installRuntimeApi(page, state);
  await page.goto('/login?redirect=%2Fworkflow%2FprocessInstance%2Findex');
  await page.locator('.submit-button').click();
  await page.getByRole('button', { name: '变量', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: '流程变量 - 请假审批' });
  await expect(dialog.getByText('amount', { exact: false })).toBeVisible();
  await dialog.getByPlaceholder('请输入变量 KEY').fill('amount');
  await dialog.getByPlaceholder('请输入变量值').fill('200');
  await dialog.getByRole('button', { name: '更新变量', exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect.poll(() => state.variableBodies).toEqual([{ instanceId: 'instance-1', key: 'amount', value: '200' }]);
  expect(state.unknown).toEqual([]);
});

test('my document cancellation keeps the backend cancellation reason', async ({ page }) => {
  const state: RuntimeState = {
    cancelBodies: [],
    completeBodies: [],
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:instance:currentList', 'workflow:instance:cancel'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: []
  };
  await installRuntimeApi(page, state);
  await page.goto('/login?redirect=%2Fworkflow%2Ftask%2FmyDocument');
  await page.locator('.submit-button').click();
  await expect(page.getByRole('cell', { name: '我的请假单' })).toBeVisible();
  await page.getByRole('button', { name: '撤销', exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect.poll(() => state.cancelBodies).toEqual([{ businessId: 'leave-1', message: '申请人撤销流程！' }]);
  expect(state.unknown).toEqual([]);
});

test('leave controls follow exact state and permission gates', async ({ page }) => {
  const state: RuntimeState = {
    completeBodies: [],
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:leave:list'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: []
  };
  await installRuntimeApi(page, state);
  await page.goto('/login?redirect=%2Fworkflow%2Fleave%2Findex');
  await page.locator('.submit-button').click();
  const draft = page.getByRole('row').filter({ hasText: '草稿' });
  const waiting = page.getByRole('row').filter({ hasText: '审批中' });
  await expect(draft.getByRole('button', { name: '修改', exact: true })).toHaveCount(0);
  await expect(draft.getByRole('button', { name: '删除', exact: true })).toHaveCount(0);
  await expect(waiting.getByRole('button', { name: '撤销', exact: true })).toHaveCount(0);
  await expect(waiting.getByRole('button', { name: '修改', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '导出', exact: true })).toHaveCount(0);
  expect(state.unknown).toEqual([]);
});

test('leave add calculates days, starts workflow and closes after task completion', async ({ page }) => {
  const state: RuntimeState = {
    completeBodies: [],
    failComplete: false,
    failUsers: false,
    invalidBodies: [],
    leaveBodies: [],
    network: [],
    operationBodies: [],
    permissions: ['workflow:leave:add'],
    terminationBodies: [],
    unknown: [],
    urgeBodies: [],
    workflowStartBodies: []
  };
  await installRuntimeApi(page, state);
  await page.goto('/login?redirect=%2Fworkflow%2FleaveEdit%2Findex%3Ftype%3Dadd');
  await page.locator('.submit-button').click();
  await expect(page).toHaveURL(/\/workflow\/leaveEdit\/index\?type=add$/);

  const leaveType = page.locator('.el-form-item').filter({ hasText: '请假类型' });
  await leaveType.locator('.el-select__wrapper').click();
  await page.getByRole('option', { name: '事假', exact: true }).click();
  const leaveTime = page.locator('.el-form-item').filter({ hasText: '请假时间' }).locator('.el-range-input');
  await leaveTime.first().fill('2026-09-01 09:00:00');
  await leaveTime.first().press('Tab');
  await leaveTime.nth(1).fill('2026-09-02 18:00:00');
  await leaveTime.nth(1).press('Enter');
  await page.locator('.el-form-item').filter({ hasText: '请假原因' }).getByRole('textbox').fill('前端架构评审');
  await expect(page.locator('.el-form-item').filter({ hasText: '请假天数' }).getByRole('textbox')).toHaveValue('2');

  await page.getByRole('button', { name: '提交审批', exact: true }).click();
  const processDialog = page.getByRole('dialog', { name: '流程办理' });
  await expect(processDialog).toBeVisible();
  await processDialog.getByLabel('审批意见').fill('同意请假');
  await processDialog.getByRole('button', { name: '选择', exact: true }).click();
  const selector = page.getByRole('dialog', { name: '选择用户' });
  await selector.getByRole('row').filter({ hasText: '流程负责人' }).locator('label.el-checkbox').click();
  await selector.getByRole('button', { name: '确定', exact: true }).click();
  await processDialog.getByRole('button', { name: '提交', exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();

  await expect(page).not.toHaveURL(/\/workflow\/leaveEdit\/index/);
  expect(state.leaveBodies).toEqual([
    {
      leaveType: '1',
      startDate: '2026-09-01 09:00:00',
      endDate: '2026-09-02 18:00:00',
      leaveDays: 2,
      remark: '前端架构评审'
    }
  ]);
  expect(state.workflowStartBodies).toEqual([
    {
      businessId: 'leave-created',
      flowCode: 'leave1',
      variables: { leaveDays: 2, userList: ['1', '3', '4'] },
      bizExt: { businessTitle: '请假申请', businessCode: 'LEAVE-CREATED' }
    }
  ]);
  expect(state.completeBodies).toHaveLength(1);
  expect(state.unknown).toEqual([]);
});
