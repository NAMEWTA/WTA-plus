import type { FlowInvalidPayload, UrgeTaskPayload } from '@namewta/domain-workflow';

export const calculateLeaveDays = (range: readonly string[]): number | undefined => {
  if (range.length !== 2) return undefined;
  const start = new Date(range[0]).getTime();
  const end = new Date(range[1]).getTime();
  if (!Number.isFinite(start) || !Number.isFinite(end) || end < start) return undefined;
  return Math.floor((end - start) / 86_400_000) + 1;
};

export const createCancelProcessPayload = (businessId: string | number) => ({
  businessId,
  message: '申请人撤销流程！'
});

export const createInstanceVariablePayload = (instanceId: string | number, key: string, value: string) => ({
  instanceId,
  key: key.trim(),
  value: value.trim()
});

export const createFlowInvalidPayload = (id: string | number, comment: string): FlowInvalidPayload => ({
  id,
  comment: comment.trim()
});

export const createUrgePayload = (
  taskIds: readonly (string | number)[],
  message: string,
  messageType: readonly string[] = ['1']
): UrgeTaskPayload => ({
  taskIdList: [...taskIds],
  message: message.trim(),
  messageType: [...messageType]
});

export const isEditableLeaveStatus = (status: string | undefined) =>
  status === 'draft' || status === 'cancel' || status === 'back';

export const isCancellableLeaveStatus = (status: string | undefined) => status === 'waiting';
