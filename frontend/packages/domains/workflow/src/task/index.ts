export type { TaskOperationPayload, TaskQuery, TerminateTaskPayload, UrgeTaskPayload, WorkflowTask } from './types';

export const workflowTaskResource = Object.freeze({ controller: 'FlwTaskController', basePath: '/workflow/task' });
