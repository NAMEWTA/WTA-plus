import { createWorkflowDefinitionService, type ApiResponse, type WorkflowTask } from '@namewta/domain-workflow';
import { effectScope, reactive } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { WorkflowWebRuntime } from '../runtime';
import { useProcessActionDialog } from './useProcessActionDialog';

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (error: Error) => void;
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
const task = (id: string | number): WorkflowTask => ({
  id, instanceId: `instance-${id}`, businessId: `business-${id}`, businessCode: 'LEAVE', businessTitle: '请假',
  flowCode: 'leave', flowName: `审批-${id}`, flowStatus: 'waiting', formCustom: 'N', formPath: '/leave',
  nodeCode: 'approval', nodeName: '审批', nodeRatio: 1, nodeType: 1,
  buttonList: ['back', 'subSign', 'termination', 'trust', 'transfer', 'addSign'].map(code => ({ code, show: true }))
});
const scopes: ReturnType<typeof effectScope>[] = [];
afterEach(() => scopes.splice(0).forEach(scope => scope.stop()));
function fixture() {
  const base = createWorkflowDefinitionService({ request: async () => { throw new Error('Unexpected request'); } });
  const service = { ...base,
    getTask: vi.fn(async (id: string | number): Promise<ApiResponse<WorkflowTask>> => ({ data: task(id) })),
    getNextNodes: vi.fn(async (): Promise<ApiResponse<Record<string, unknown>[]>> => ({ data: [] })),
    getBackTaskNodes: vi.fn(async (): Promise<ApiResponse<Record<string, unknown>[]>> => ({ data: [{ nodeCode: 'start' }] })),
    currentTaskUsers: vi.fn(async () => ({ data: [{ userId: '7', nickName: '审批人' }] })),
    completeTask: vi.fn(async () => ({})), backProcess: vi.fn(async () => ({})),
    terminateTask: vi.fn(async () => ({})), operateTask: vi.fn(async () => ({}))
  };
  const runtime: WorkflowWebRuntime = {
    service, confirm: vi.fn(async () => undefined), success: vi.fn(), error: vi.fn(), chartUrl: () => '',
    closeCurrentPage: vi.fn(), closeDesigner: vi.fn(), designUrl: () => '', dicts: () => ({}),
    download: vi.fn(), downloadAttachment: vi.fn(), resolveAttachments: async () => [], fileUpload: {}, treePanel: {}
  };
  const props = reactive({ allowComplete: true, mode: 'participant' as const, runtime, taskVariables: { reason: { label: 'original' } } });
  const emit = vi.fn(); const scope = effectScope(); scopes.push(scope);
  const state = scope.run(() => useProcessActionDialog(props, emit))!;
  return { state, props, runtime, service, emit, scope };
}

describe('process dialog task ownership', () => {
  it('clears A immediately and never submits A when B fails to load', async () => {
    const { state, service } = fixture(); await state.open('A');
    state.backVisible.value = true; state.reductionVisible.value = true;
    const loadingB = deferred<ApiResponse<WorkflowTask>>(); service.getTask.mockReturnValueOnce(loadingB.promise);
    const opening = state.open('B');
    expect(state.task.value).toBeUndefined();
    expect(state.backVisible.value || state.reductionVisible.value).toBe(false);
    loadingB.reject(new Error('B unavailable')); await opening;
    await state.complete(); await state.terminate();
    expect(service.completeTask).not.toHaveBeenCalled(); expect(service.terminateTask).not.toHaveBeenCalled();
    expect(state.failure.value).toBe('B unavailable'); expect(state.loading.value).toBe(false);
  });

  it('ignores obsolete load results, errors and finalizers while B is loading', async () => {
    const { state, service } = fixture(); const a = deferred<ApiResponse<WorkflowTask>>(); const b = deferred<ApiResponse<WorkflowTask>>();
    service.getTask.mockReturnValueOnce(a.promise).mockReturnValueOnce(b.promise);
    const first = state.open('A'); const second = state.open('B');
    a.resolve({ data: task('A') }); await first;
    expect(state.task.value).toBeUndefined(); expect(state.loading.value).toBe(true);
    b.resolve({ data: task('B') }); await second;
    expect(state.task.value?.id).toBe('B'); expect(state.loading.value).toBe(false);
  });

  it.each(['complete', 'terminate', 'back'] as const)('invalidates %s confirmation when another task opens', async action => {
    const { state, service, runtime } = fixture(); await state.open('A');
    if (action === 'back') await state.openBack();
    const confirmation = deferred<void>(); vi.mocked(runtime.confirm).mockReturnValueOnce(confirmation.promise);
    const pending = state[action](); await state.open('B'); confirmation.resolve(); await pending;
    expect(service.completeTask).not.toHaveBeenCalled(); expect(service.terminateTask).not.toHaveBeenCalled();
    expect(service.backProcess).not.toHaveBeenCalled(); expect(state.visible.value).toBe(true);
    expect(state.task.value?.id).toBe('B');
  });

  it.each(['close', 'unmount'] as const)('invalidates pending confirmations on %s', async action => {
    const { state, service, scope, runtime } = fixture(); await state.open('A');
    const confirmation = deferred<void>(); vi.mocked(runtime.confirm).mockReturnValueOnce(confirmation.promise);
    const pending = state.complete(); if (action === 'close') state.close(); else scope.stop();
    confirmation.resolve(); await pending; expect(service.completeTask).not.toHaveBeenCalled();
  });

  it('blocks double submission before confirmation resolves', async () => {
    const { state, service, runtime } = fixture(); await state.open('A');
    const confirmation = deferred<void>(); vi.mocked(runtime.confirm).mockReturnValue(confirmation.promise);
    const first = state.complete(); const second = state.complete();
    expect(runtime.confirm).toHaveBeenCalledTimes(1); confirmation.resolve(); await Promise.all([first, second]);
    expect(service.completeTask).toHaveBeenCalledTimes(1);
  });

  it('captures the task and complete payload before confirmation, including nested variables', async () => {
    const { state, service, runtime, props } = fixture(); await state.open('A');
    state.message.value = 'original'; state.messageType.value = ['1'];
    const confirmation = deferred<void>(); vi.mocked(runtime.confirm).mockReturnValueOnce(confirmation.promise);
    const pending = state.complete(); state.message.value = 'changed'; state.messageType.value.push('2');
    props.taskVariables.reason.label = 'changed'; confirmation.resolve(); await pending;
    expect(service.completeTask).toHaveBeenCalledWith(expect.objectContaining({ taskId: 'A', message: 'original', messageType: ['1'], variables: { reason: { label: 'original' } } }));
  });

  it('does not close B or emit completion when A finishes after switching', async () => {
    const { state, service, emit, runtime } = fixture(); await state.open('A');
    const response = deferred<object>(); const called = deferred<void>();
    service.completeTask.mockImplementationOnce(() => { called.resolve(); return response.promise; });
    const pending = state.complete(); await called.promise; await state.open('B'); response.resolve({}); await pending;
    expect(state.visible.value).toBe(true); expect(state.task.value?.id).toBe('B');
    expect(emit).not.toHaveBeenCalledWith('completed'); expect(runtime.success).not.toHaveBeenCalled();
  });

  it.each(['openBack', 'openReduction'] as const)('does not attach old %s results to a new task', async action => {
    const { state, service } = fixture(); await state.open('A');
    const gate = deferred<void>();
    if (action === 'openBack') service.getBackTaskNodes.mockImplementationOnce(async () => { await gate.promise; return { data: [{ nodeCode: 'old' }] }; });
    else service.currentTaskUsers.mockImplementationOnce(async () => { await gate.promise; return { data: [{ userId: 'old', nickName: 'old' }] }; });
    const pending = state[action](); await state.open('B'); gate.resolve(); await pending;
    expect(state.backVisible.value || state.reductionVisible.value).toBe(false);
  });

  it('keeps current failure recoverable and retries once', async () => {
    const { state, service, emit } = fixture(); await state.open('A'); service.completeTask.mockRejectedValueOnce(new Error('unavailable'));
    await state.complete(); expect(state.failure.value).toBe('unavailable'); expect(state.submitting.value).toBe(false);
    await state.complete(); expect(service.completeTask).toHaveBeenCalledTimes(2); expect(emit).toHaveBeenCalledWith('completed');
  });
  it('keeps B usable when an obsolete load rejects after B succeeds', async () => {
    const { state, service } = fixture(); const first = deferred<ApiResponse<WorkflowTask>>();
    service.getTask.mockReturnValueOnce(first.promise); const opening = state.open('A'); await state.open('B');
    first.reject(new Error('old failure')); await opening;
    expect(state.failure.value).toBe(''); expect(state.task.value?.id).toBe('B'); expect(state.loading.value).toBe(false);
  });

  it('rejects an empty or mismatched task response and permits a clean retry', async () => {
    const { state, service } = fixture();
    for (const response of [{}, { data: task('wrong') }]) {
      service.getTask.mockResolvedValueOnce(response); await state.open('A'); await state.complete();
      expect(state.task.value).toBeUndefined(); expect(state.failure.value).toBeTruthy();
    }
    expect(service.completeTask).not.toHaveBeenCalled();
    await state.open('A'); expect(state.task.value?.id).toBe('A'); expect(state.failure.value).toBe('');
  });

  it.each(['openBack', 'openReduction'] as const)('reports a recoverable current %s failure', async action => {
    const { state, service } = fixture(); await state.open('A');
    if (action === 'openBack') service.getBackTaskNodes.mockRejectedValueOnce(new Error('read failed'));
    else service.currentTaskUsers.mockRejectedValueOnce(new Error('read failed'));
    await state[action](); expect(state.failure.value).toBe('read failed');
    await state[action](); expect(state.failure.value).toBe('');
    expect(action === 'openBack' ? state.backVisible.value : state.reductionVisible.value).toBe(true);
  });

  it.each(['delegateTask', 'transferTask', 'addSignature'] as const)('invalidates %s selection confirmation across task changes', async action => {
    const { state, service, runtime } = fixture(); await state.open('A'); await state.openSelector(action, true);
    const confirmation = deferred<void>(); vi.mocked(runtime.confirm).mockReturnValueOnce(confirmation.promise);
    const pending = state.handleUsers([{ userId: '7', nickName: '审批人' }]); await state.open('B'); confirmation.resolve(); await pending;
    expect(service.operateTask).not.toHaveBeenCalled();
  });

  it('ignores a stale selector event and cancels its pending nextTick open', async () => {
    const { state, service } = fixture(); await state.open('A'); const selector = { open: vi.fn(async () => undefined), close: vi.fn() };
    state.selector.value = selector; const opening = state.openSelector('delegateTask', false); const switching = state.open('B');
    await opening; await switching; await state.handleUsers([{ userId: '7', nickName: '审批人' }]);
    expect(selector.open).not.toHaveBeenCalled(); expect(selector.close).toHaveBeenCalled(); expect(service.operateTask).not.toHaveBeenCalled();
  });

  it('captures the complete back payload before confirmation', async () => {
    const { state, service, runtime, props } = fixture(); await state.open('A'); await state.openBack();
    state.backMessage.value = 'original'; state.backFileId.value = 'attachment-A';
    const confirmation = deferred<void>(); vi.mocked(runtime.confirm).mockReturnValueOnce(confirmation.promise);
    const pending = state.back(); state.backNodeCode.value = 'changed'; state.backMessage.value = 'changed';
    state.backFileId.value = 'attachment-B'; props.taskVariables.reason.label = 'changed'; confirmation.resolve(); await pending;
    expect(service.backProcess).toHaveBeenCalledWith(expect.objectContaining({ taskId: 'A', nodeCode: 'start', message: 'original', fileId: 'attachment-A', variables: { reason: { label: 'original' } } }));
  });

  it('does not clear the newer submission guard when an older request fails', async () => {
    const { state, service, runtime } = fixture(); await state.open('A');
    const request = deferred<object>(); const started = deferred<void>();
    service.completeTask.mockImplementationOnce(() => { started.resolve(); return request.promise; });
    const old = state.complete(); await started.promise; await state.open('B');
    const confirmation = deferred<void>(); vi.mocked(runtime.confirm).mockReturnValueOnce(confirmation.promise);
    const current = state.complete(); request.reject(new Error('old failed')); await old;
    expect(state.submitting.value).toBe(true); expect(state.failure.value).toBe('');
    confirmation.resolve(); await current; expect(state.visible.value).toBe(false);
  });

});
