export type { FlowInvalidPayload, InstanceQuery, WorkflowHistory, WorkflowInstance } from './types';

export const workflowInstanceResource = Object.freeze({
  controller: 'FlwInstanceController',
  basePath: '/workflow/instance'
});
