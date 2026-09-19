import type { UserSummary, WorkflowTask } from '@namewta/domain-workflow';
import { computed, nextTick, onScopeDispose, reactive, ref, toRaw, watch, type Ref } from 'vue';
import type { WorkflowWebRuntime } from '../runtime';
import { createBackPayload, createCompletePayload, createTaskOperationPayload, enabledProcessButtons } from '../process-actions';

type NodeOption = { nodeCode: string; nodeName?: string; permissionFlag?: string | number | readonly (string | number)[] };
type SelectorAction = 'addSignature' | 'copy' | 'delegateTask' | 'node' | 'transferTask';
type TaskOperation = 'addSignature' | 'delegateTask' | 'reductionSignature' | 'transferTask';
interface ProcessUserSelector { open(): Promise<void>; close(): void }
export interface ProcessActionOptions {
  allowComplete: boolean;
  mode: 'intervention' | 'participant';
  runtime: WorkflowWebRuntime;
  taskVariables: Readonly<Record<string, unknown>>;
}

/** Owns one task dialog, its dependent selections and asynchronous operations. */
export function useProcessActionDialog(props: ProcessActionOptions, emit: (event: 'cancelled' | 'completed') => void) {
  const visible = ref(false);
  const generation = ref(0);
  const loading = ref(false);
  const submitting = ref(false);
  const failure = ref('');
  const task = ref<WorkflowTask>();
  const message = ref('');
  const messageType = ref(['1']);
  const fileId = ref('');
  const nextNodes = ref<NodeOption[]>([]);
  const copyUsers = ref<UserSummary[]>([]);
  const assigneeMap = reactive<Record<string, string>>({});
  const assigneeNames = reactive<Record<string, string>>({});
  const enabled = computed(() => task.value ? enabledProcessButtons(task.value, props.mode) : new Set<string>());
  const selector = ref<ProcessUserSelector>();
  const selectorMultiple = ref(true);
  const selectorData = ref<(string | number)[]>([]);
  const selectorUserIds = ref<string | number | readonly (string | number)[]>();
  const reductionVisible = ref(false);
  const currentUsers = ref<UserSummary[]>([]);
  const backVisible = ref(false);
  const backNodes = ref<NodeOption[]>([]);
  const backNodeCode = ref('');
  const backMessage = ref('');
  const backFileId = ref('');
  const backFailure = ref('');
  let disposed = false;
  let actionCompleted = false;
  let selectorContext: { generation: number; action: SelectorAction; multiple: boolean; node?: NodeOption } | undefined;
  let backRequest = 0;
  let reductionRequest = 0;

  function isCurrent(owner: number) {
    return !disposed && visible.value && generation.value === owner;
  }
  function currentTask() {
    return isCurrent(generation.value) && !loading.value && !submitting.value && task.value?.flowStatus === 'waiting'
      ? task.value : undefined;
  }
  function can(code: string) {
    return isCurrent(generation.value) && !loading.value && task.value?.flowStatus === 'waiting' && enabled.value.has(code);
  }
  function canRatio(code: string) {
    return can(code) && Number(task.value?.nodeRatio ?? 0) > 0;
  }
  function resetTask() {
    task.value = undefined;
    nextNodes.value = [];
    copyUsers.value = [];
    message.value = '';
    messageType.value = ['1'];
    fileId.value = '';
    Object.keys(assigneeMap).forEach(key => delete assigneeMap[key]);
    Object.keys(assigneeNames).forEach(key => delete assigneeNames[key]);
    selector.value?.close();
    selectorContext = undefined;
    selectorData.value = [];
    selectorUserIds.value = undefined;
    backVisible.value = false;
    backNodes.value = [];
    backNodeCode.value = '';
    backMessage.value = '';
    backFileId.value = '';
    backFailure.value = '';
    reductionVisible.value = false;
    currentUsers.value = [];
    backRequest++;
    reductionRequest++;
    submitting.value = false;
    loading.value = false;
  }
  function invalidate() {
    generation.value++;
    resetTask();
  }
  function close() { visible.value = false; }
  // Invalidate immediately on v-model close, not after the dialog transition.
  watch(visible, shown => {
    if (shown) return;
    invalidate();
    if (!actionCompleted) emit('cancelled');
  }, { flush: 'sync' });
  onScopeDispose(() => { disposed = true; invalidate(); });

  async function open(taskId: string | number) {
    if (disposed) return;
    invalidate();
    const owner = generation.value;
    actionCompleted = false;
    failure.value = '';
    visible.value = true;
    loading.value = true;
    try {
      const [taskResponse, nodeResponse] = await Promise.all([
        props.runtime.service.getTask(taskId),
        props.runtime.service.getNextNodes({ taskId, variables: structuredClone(toRaw(props.taskVariables)) })
      ]);
      if (!isCurrent(owner)) return;
      if (!taskResponse.data || String(taskResponse.data.id) !== String(taskId)) throw new Error('任务信息不可用，请重新打开');
      task.value = taskResponse.data;
      nextNodes.value = (nodeResponse.data ?? []) as NodeOption[];
      copyUsers.value = [...(taskResponse.data.copyList ?? [])];
    } catch (error: unknown) {
      if (isCurrent(owner)) failure.value = error instanceof Error ? error.message : '任务信息加载失败';
    } finally {
      if (isCurrent(owner)) loading.value = false;
    }
  }
  async function openSelector(action: SelectorAction, multiple: boolean) {
    if (!currentTask()) return;
    const context = { generation: generation.value, action, multiple };
    selectorContext = context;
    selectorMultiple.value = multiple;
    selectorData.value = action === 'copy' ? copyUsers.value.map(user => user.userId) : [];
    selectorUserIds.value = undefined;
    await nextTick();
    if (isCurrent(context.generation) && selectorContext === context) await selector.value?.open();
  }
  async function selectNode(node: NodeOption) {
    if (!currentTask()) return;
    const context = { generation: generation.value, action: 'node' as const, multiple: true, node };
    selectorContext = context;
    selectorMultiple.value = true;
    selectorData.value = [];
    selectorUserIds.value = node.permissionFlag;
    await nextTick();
    if (isCurrent(context.generation) && selectorContext === context) await selector.value?.open();
  }
  async function handleUsers(users: UserSummary[]) {
    const context = selectorContext;
    if (!context || !isCurrent(context.generation) || !currentTask() || !users.length) return;
    selectorContext = undefined;
    if (context.action === 'copy') { copyUsers.value = users; return; }
    if (context.action === 'node') {
      if (context.node) {
        assigneeMap[context.node.nodeCode] = users.map(user => user.userId).join(',');
        assigneeNames[context.node.nodeCode] = users.map(user => user.nickName).join(',');
      }
      return;
    }
    await runOperation(context.action, users, context.multiple);
  }
  function removeCopy(userId: string | number) {
    if (currentTask()) copyUsers.value = copyUsers.value.filter(user => String(user.userId) !== String(userId));
  }
  async function execute(owner: number, action: () => Promise<unknown>, success = '操作成功', target: Ref<string> = failure) {
    if (!isCurrent(owner) || !currentTask()) return;
    // This synchronous guard covers repeated clicks while confirmation is pending.
    submitting.value = true;
    target.value = '';
    try {
      await props.runtime.confirm('是否确认提交？');
      if (!isCurrent(owner)) return;
      await action();
      if (!isCurrent(owner)) return;
      props.runtime.success(success);
      actionCompleted = true;
      close();
      emit('completed');
    } catch (error: unknown) {
      if (isCurrent(owner)) target.value = error instanceof Error ? error.message : '流程操作失败';
    } finally {
      if (isCurrent(owner)) submitting.value = false;
    }
  }
  async function complete() {
    const current = currentTask();
    if (!current || !props.allowComplete) return;
    if (enabled.value.has('pop') && nextNodes.value.some(node => !assigneeMap[node.nodeCode])) {
      failure.value = '请选择审批人'; return;
    }
    const payload = createCompletePayload({
      taskId: current.id, message: message.value, messageType: messageType.value,
      variables: structuredClone(toRaw(props.taskVariables)), assigneeMap, copyUsers: copyUsers.value,
      fileId: fileId.value || undefined
    });
    await execute(generation.value, () => props.runtime.service.completeTask(payload));
  }
  async function runOperation(operation: TaskOperation, users: UserSummary[], multiple: boolean) {
    const current = currentTask();
    if (!current) return;
    const payload = createTaskOperationPayload(current.id, users, message.value, messageType.value, multiple);
    await execute(generation.value, () => props.runtime.service.operateTask(payload, operation));
  }
  async function openReduction() {
    const current = currentTask();
    if (!current) return;
    const owner = generation.value; const request = ++reductionRequest;
    failure.value = '';
    try {
      const response = await props.runtime.service.currentTaskUsers(current.id);
      if (!isCurrent(owner) || request !== reductionRequest || submitting.value) return;
      currentUsers.value = response.data ?? [];
      reductionVisible.value = true;
    } catch (error: unknown) {
      if (isCurrent(owner) && request === reductionRequest) failure.value = error instanceof Error ? error.message : '办理人加载失败';
    }
  }
  async function reduce(input: unknown) {
    const user = input as UserSummary;
    if (!reductionVisible.value || !currentUsers.value.some(row => row.userId === user.userId)) return;
    reductionVisible.value = false;
    await runOperation('reductionSignature', [user], true);
  }
  async function terminate() {
    const current = currentTask();
    if (!current) return;
    const payload = { taskId: current.id, comment: message.value.trim() };
    await execute(generation.value, () => props.runtime.service.terminateTask(payload), '任务已终止');
  }
  async function openBack() {
    const current = currentTask();
    if (!current) return;
    const owner = generation.value; const request = ++backRequest;
    failure.value = '';
    try {
      const response = await props.runtime.service.getBackTaskNodes(current.id, current.nodeCode);
      if (!isCurrent(owner) || request !== backRequest || submitting.value) return;
      backNodes.value = (response.data ?? []) as NodeOption[];
      backNodeCode.value = backNodes.value[0]?.nodeCode ?? '';
      backMessage.value = ''; backFileId.value = ''; backFailure.value = '';
      backVisible.value = true;
    } catch (error: unknown) {
      if (isCurrent(owner) && request === backRequest) failure.value = error instanceof Error ? error.message : '退回节点加载失败';
    }
  }
  async function back() {
    const current = currentTask();
    if (!current || !backVisible.value || !backNodeCode.value) return;
    const payload = createBackPayload({
      taskId: current.id, nodeCode: backNodeCode.value, message: backMessage.value,
      messageType: messageType.value, variables: structuredClone(toRaw(props.taskVariables)), fileId: backFileId.value || undefined
    });
    await execute(generation.value, () => props.runtime.service.backProcess(payload), '操作成功', backFailure);
  }

  return { visible, generation, loading, submitting, failure, task, message, messageType, fileId, nextNodes, copyUsers,
    assigneeNames, enabled, selector, selectorMultiple, selectorData, selectorUserIds, reductionVisible, currentUsers,
    backVisible, backNodes, backNodeCode, backMessage, backFileId, backFailure, can, canAction: can, canRatio, open,
    openSelector, selectNode, handleUsers, removeCopy, complete, openReduction, reduce, terminate, openBack, back, close };
}
