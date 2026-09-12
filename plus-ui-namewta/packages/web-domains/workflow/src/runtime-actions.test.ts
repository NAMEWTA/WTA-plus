import { describe, expect, it } from 'vitest';
import {
  calculateLeaveDays,
  createCancelProcessPayload,
  createFlowInvalidPayload,
  createInstanceVariablePayload,
  createUrgePayload,
  isCancellableLeaveStatus,
  isEditableLeaveStatus
} from './index';

describe('workflow runtime action contracts', () => {
  it('builds the exact FlowInvalidBo and urge payloads', () => {
    expect(createFlowInvalidPayload('instance/1', '  reason  ')).toEqual({ id: 'instance/1', comment: 'reason' });
    expect(createUrgePayload(['task/1', 'task,2'], '  please act  ')).toEqual({
      taskIdList: ['task/1', 'task,2'],
      message: 'please act',
      messageType: ['1']
    });
  });

  it('keeps leave mutations fail-closed by workflow state', () => {
    expect(['draft', 'cancel', 'back'].every(isEditableLeaveStatus)).toBe(true);
    expect(['waiting', 'finish', undefined].some(isEditableLeaveStatus)).toBe(false);
    expect(isCancellableLeaveStatus('waiting')).toBe(true);
    expect(isCancellableLeaveStatus('draft')).toBe(false);
  });

  it('preserves timestamp leave, cancellation, and instance-variable payload contracts', () => {
    expect(calculateLeaveDays(['2026-08-26 00:00:00', '2026-08-27 23:59:59'])).toBe(2);
    expect(calculateLeaveDays([])).toBeUndefined();
    expect(createCancelProcessPayload('leave-1')).toEqual({
      businessId: 'leave-1',
      message: '申请人撤销流程！'
    });
    expect(createInstanceVariablePayload('instance-1', ' amount ', ' 200 ')).toEqual({
      instanceId: 'instance-1',
      key: 'amount',
      value: '200'
    });
  });
});
