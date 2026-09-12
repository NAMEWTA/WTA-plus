import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import type { WorkflowWebRuntime } from './runtime';
import { requireWorkflowWebRuntime } from './runtime';

export { createLiveWorkflowDictRefs } from './runtime';
export type { WorkflowAttachment, WorkflowDictOption, WorkflowDictSource, WorkflowWebRuntime } from './runtime';
export { default as WorkflowUserSelect } from './components/UserSelect.vue';
export { default as WorkflowProcessActionDialog } from './components/ProcessActionDialog.vue';
export { default as WorkflowApprovalButton } from './components/ApprovalButton.vue';
export { default as WorkflowApprovalRecord } from './components/ApprovalRecord.vue';
export { default as WorkflowFlowChart } from './components/FlowChart.vue';
export { default as WorkflowFlowChartImg } from './components/FlowChartImg.vue';
export { default as WorkflowMessageType } from './components/MessageType.vue';
export { default as WorkflowUserNameDisplay } from './components/UserNameDisplay.vue';
export {
  createFlowInvalidPayload,
  calculateLeaveDays,
  createCancelProcessPayload,
  createInstanceVariablePayload,
  createUrgePayload,
  isCancellableLeaveStatus,
  isEditableLeaveStatus
} from './runtime-actions';
export {
  mergeUserSelection,
  normalizeUserIds,
  prepareUserSelection,
  serializeCandidateUserIds
} from './user-selection';
export {
  createBackPayload,
  createCompletePayload,
  createTaskOperationPayload,
  enabledProcessButtons
} from './process-actions';

async function runtimeView(
  name: string,
  runtime: WorkflowWebRuntime,
  load: () => Promise<{ default: Component }>,
  props: Record<string, unknown> = {}
): Promise<Component> {
  const page = (await load()).default;
  return defineComponent({ name, setup: () => () => h(page, { runtime, ...props }) });
}

export function createWorkflowWebDomain(runtimeInput: WorkflowWebRuntime | undefined): WebDomainManifest<Component> {
  const runtime = requireWorkflowWebRuntime(runtimeInput);
  return Object.freeze({
    id: 'web-domain-workflow',
    domainId: 'workflow',
    messages: Object.freeze([
      Object.freeze({
        namespace: 'workflow',
        messages: Object.freeze({
          categoryTitle: '流程分类',
          definitionTitle: '流程定义',
          spelTitle: '流程表达式'
        })
      })
    ]),
    permissions: Object.freeze([
      Object.freeze({
        id: 'workflow-category',
        permissions: Object.freeze([
          'workflow:category:list',
          'workflow:category:query',
          'workflow:category:add',
          'workflow:category:edit',
          'workflow:category:remove'
        ])
      }),
      Object.freeze({
        id: 'workflow-definition',
        permissions: Object.freeze([
          'workflow:definition:list',
          'workflow:definition:add',
          'workflow:definition:edit',
          'workflow:definition:remove',
          'workflow:definition:import',
          'workflow:definition:export',
          'workflow:definition:active',
          'workflow:definition:copy',
          'workflow:definition:query',
          'workflow:definition:publish'
        ])
      }),
      Object.freeze({
        id: 'workflow-spel',
        permissions: Object.freeze([
          'workflow:spel:list',
          'workflow:spel:query',
          'workflow:spel:add',
          'workflow:spel:edit',
          'workflow:spel:remove'
        ])
      }),
      Object.freeze({
        id: 'workflow-task-runtime',
        permissions: Object.freeze(['workflow:task:list', 'workflow:task:edit'])
      }),
      Object.freeze({
        id: 'workflow-instance-runtime',
        permissions: Object.freeze([
          'workflow:instance:list',
          'workflow:instance:currentList',
          'workflow:instance:cancel',
          'workflow:instance:remove',
          'workflow:instance:invalid',
          'workflow:instance:active',
          'workflow:instance:query',
          'workflow:instance:variable',
          'workflow:instance:variableQuery'
        ])
      }),
      Object.freeze({
        id: 'workflow-leave-runtime',
        permissions: Object.freeze([
          'workflow:leave:list',
          'workflow:leave:query',
          'workflow:leave:add',
          'workflow:leave:edit',
          'workflow:leave:remove',
          'workflow:leave:export'
        ])
      })
    ]),
    registrations: Object.freeze([
      Object.freeze({
        id: 'workflow-category',
        componentKey: 'workflow/category/index',
        componentName: 'Category',
        load: () => runtimeView('Category', runtime, () => import('./category/CategoryPage.vue'))
      }),
      Object.freeze({
        id: 'workflow-definition',
        componentKey: 'workflow/processDefinition/index',
        componentName: 'processDefinition',
        load: () => runtimeView('processDefinition', runtime, () => import('./definition/DefinitionPage.vue'))
      }),
      Object.freeze({
        id: 'workflow-definition-design',
        componentKey: 'workflow/processDefinition/design',
        componentName: 'WarmFlow',
        load: () => runtimeView('WarmFlow', runtime, () => import('./definition/DesignPage.vue'))
      }),
      Object.freeze({
        id: 'workflow-spel',
        componentKey: 'workflow/spel/index',
        componentName: 'Spel',
        load: () => runtimeView('Spel', runtime, () => import('./spel/SpelPage.vue'))
      }),
      Object.freeze({
        id: 'workflow-task-waiting',
        componentKey: 'workflow/task/taskWaiting',
        componentName: 'taskWaiting',
        load: () => runtimeView('taskWaiting', runtime, () => import('./task/TaskListPage.vue'), { mode: 'waiting' })
      }),
      Object.freeze({
        id: 'workflow-task-finished',
        componentKey: 'workflow/task/taskFinish',
        componentName: 'taskFinish',
        load: () => runtimeView('taskFinish', runtime, () => import('./task/TaskListPage.vue'), { mode: 'finished' })
      }),
      Object.freeze({
        id: 'workflow-task-copy',
        componentKey: 'workflow/task/taskCopyList',
        componentName: 'taskCopyList',
        load: () => runtimeView('taskCopyList', runtime, () => import('./task/TaskListPage.vue'), { mode: 'copy' })
      }),
      Object.freeze({
        id: 'workflow-my-document',
        componentKey: 'workflow/task/myDocument',
        componentName: 'myDocument',
        load: () => runtimeView('myDocument', runtime, () => import('./instance/MyDocumentPage.vue'))
      }),
      Object.freeze({
        id: 'workflow-all-task-waiting',
        componentKey: 'workflow/task/allTaskWaiting',
        componentName: 'allTaskWaiting',
        load: () =>
          runtimeView('allTaskWaiting', runtime, () => import('./task/TaskListPage.vue'), { mode: 'all-waiting' })
      }),
      Object.freeze({
        id: 'workflow-process-instance',
        componentKey: 'workflow/processInstance/index',
        componentName: 'processInstance',
        load: () => runtimeView('processInstance', runtime, () => import('./instance/InstancePage.vue'))
      }),
      Object.freeze({
        id: 'workflow-leave',
        componentKey: 'workflow/leave/index',
        componentName: 'leave',
        load: () => runtimeView('leave', runtime, () => import('./leave/LeaveListPage.vue'))
      }),
      Object.freeze({
        id: 'workflow-leave-edit',
        componentKey: 'workflow/leave/leaveEdit',
        componentName: 'leaveEdit',
        load: () => runtimeView('leaveEdit', runtime, () => import('./leave/LeaveEditPage.vue'))
      })
    ])
  });
}
