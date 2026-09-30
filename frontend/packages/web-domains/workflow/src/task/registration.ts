import type { WorkflowDefinitionService, WorkflowTask } from '@namewta/domain-workflow';
import type { WebDomainManifest } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
/** 用户端只组合本人待办/已办，不要求设计器、目录和上传等管理端端口。 */
export interface WorkflowTaskWebRuntime {
  service: WorkflowDefinitionService;
  openWorkflowForm?(task: WorkflowTask, actionable: boolean): Promise<void> | void;
  error(message: string): void;
  success(message: string): void;
}
export function createWorkflowTaskWebDomain(runtime: WorkflowTaskWebRuntime): WebDomainManifest<Component> {
  if (!runtime?.service || typeof runtime.error !== 'function' || typeof runtime.success !== 'function')
    throw new Error('WorkflowTaskWebRuntime is required');
  return Object.freeze({
    id: 'web-domain-workflow-tasks',
    domainId: 'workflow',
    messages: Object.freeze([]),
    permissions: Object.freeze([{ id: 'workflow-tasks', permissions: Object.freeze(['workflow:task:participate']) }]),
    registrations: Object.freeze(
      (
        [
          ['waiting', 'workflow/task/taskWaiting', 'taskWaiting'],
          ['finished', 'workflow/task/taskFinish', 'taskFinish']
        ] as const
      ).map(([mode, componentKey, componentName]) => ({
        id: `workflow-task-${mode}`,
        componentKey,
        componentName,
        load: () =>
          import('./TaskListPage.vue').then(module =>
            defineComponent({
              name: componentName,
              setup: () => () => h(module.default, { runtime, mode, participantOnly: true })
            })
          )
      }))
    )
  });
}
