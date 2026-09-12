import type { FlowCopy, TaskOperationPayload, UserSummary, WorkflowTask } from '@namewta/domain-workflow';

export type ProcessButtonCode =
  | 'addSign'
  | 'back'
  | 'copy'
  | 'file'
  | 'pop'
  | 'subSign'
  | 'termination'
  | 'transfer'
  | 'trust';

export const enabledProcessButtons = (
  task: Pick<WorkflowTask, 'buttonList'> & Partial<Pick<WorkflowTask, 'flowStatus' | 'nodeRatio'>>,
  mode: 'intervention' | 'participant' = 'participant'
): ReadonlySet<string> => {
  if (mode === 'participant') return new Set(task.buttonList?.filter(button => button.show).map(button => button.code));
  if (task.flowStatus !== 'waiting') return new Set();
  return new Set(['transfer', 'termination', ...(Number(task.nodeRatio ?? 0) > 0 ? ['addSign', 'subSign'] : [])]);
};

export const createTaskOperationPayload = (
  taskId: string | number,
  users: readonly UserSummary[],
  message: string,
  messageType: readonly string[],
  multiple: boolean
): TaskOperationPayload => ({
  taskId,
  ...(multiple ? { userIds: users.map(user => user.userId) } : { userId: users[0]?.userId }),
  message: message.trim(),
  messageType: [...messageType]
});

export const createCompletePayload = (input: {
  assigneeMap: Readonly<Record<string, string>>;
  copyUsers: readonly FlowCopy[];
  fileId?: string;
  message: string;
  messageType: readonly string[];
  taskId: string | number;
  variables: Readonly<Record<string, unknown>>;
}) => ({
  taskId: input.taskId,
  message: input.message.trim(),
  messageType: [...input.messageType],
  variables: { ...input.variables },
  assigneeMap: { ...input.assigneeMap },
  flowCopyList: input.copyUsers.map(({ userId, nickName }) => ({ userId, nickName })),
  ...(input.fileId ? { fileId: input.fileId } : {})
});

export const createBackPayload = (input: {
  fileId?: string;
  message: string;
  messageType: readonly string[];
  nodeCode: string;
  taskId: string | number;
  variables: Readonly<Record<string, unknown>>;
}) => ({
  taskId: input.taskId,
  nodeCode: input.nodeCode,
  message: input.message.trim(),
  messageType: [...input.messageType],
  variables: { ...input.variables },
  ...(input.fileId ? { fileId: input.fileId } : {})
});
