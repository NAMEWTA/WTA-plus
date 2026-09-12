<template>
  <el-dialog v-model="visible" title="流程办理" :width="width" :close-on-click-modal="false" @closed="handleClosed">
    <el-alert v-if="failure" :title="failure" type="error" show-icon :closable="false" />
    <div v-loading="loading">
      <el-descriptions v-if="task" :column="2" border>
        <el-descriptions-item label="流程名称">{{ task.flowName }}</el-descriptions-item>
        <el-descriptions-item label="任务节点">{{ task.nodeName }}</el-descriptions-item>
        <el-descriptions-item label="节点编码">{{ task.nodeCode }}</el-descriptions-item>
        <el-descriptions-item label="流程实例 ID">{{ task.instanceId }}</el-descriptions-item>
        <el-descriptions-item label="业务 ID">{{ task.businessId }}</el-descriptions-item>
        <el-descriptions-item label="版本号">{{ task.version }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ task.createTime }}</el-descriptions-item>
      </el-descriptions>
      <el-form v-if="task" label-width="110px">
        <el-form-item label="消息提醒">
          <el-checkbox-group v-model="messageType">
            <el-checkbox value="1" disabled>站内信</el-checkbox>
            <el-checkbox value="2">邮件</el-checkbox>
            <el-checkbox value="3">短信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="enabled.has('file')" label="附件">
          <FileUpload
            v-model="fileId"
            :file-type="['png', 'jpg', 'jpeg', 'doc', 'docx', 'xlsx', 'xls', 'ppt', 'txt', 'pdf']"
            :file-size="20"
          />
        </el-form-item>
        <el-form-item v-if="enabled.has('copy')" label="抄送人">
          <el-tag v-for="user in copyUsers" :key="String(user.userId)" closable @close="removeCopy(user.userId)">
            {{ user.nickName }}
          </el-tag>
          <el-button @click="openSelector('copy', true)">选择抄送人</el-button>
        </el-form-item>
        <el-form-item
          v-for="node in enabled.has('pop') ? nextNodes : []"
          :key="node.nodeCode"
          :label="String(node.nodeName ?? '下一节点')"
        >
          <el-input :model-value="assigneeNames[node.nodeCode]" readonly>
            <template #append>
              <el-button :disabled="!node.permissionFlag" @click="selectNode(node)">选择</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="审批意见"><el-input v-model="message" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        v-if="allowComplete && task?.flowStatus === 'waiting'"
        type="primary"
        :loading="submitting"
        @click="complete"
      >
        提交
      </el-button>
      <el-button v-if="can('trust')" :loading="submitting" @click="openSelector('delegateTask', false)">委托</el-button>
      <el-button v-if="canAction('transfer')" :loading="submitting" @click="openSelector('transferTask', false)">
        转办
      </el-button>
      <el-button v-if="canRatio('addSign')" :loading="submitting" @click="openSelector('addSignature', true)">
        加签
      </el-button>
      <el-button v-if="canRatio('subSign')" :loading="submitting" @click="openReduction">减签</el-button>
      <el-button v-if="can('back')" type="danger" :loading="submitting" @click="openBack">退回</el-button>
      <el-button v-if="canAction('termination')" type="danger" :loading="submitting" @click="terminate">终止</el-button>
    </template>
  </el-dialog>

  <UserSelect
    ref="selector"
    :service="runtime.service"
    :multiple="selectorMultiple"
    :data="selectorData"
    :user-ids="selectorUserIds"
    @confirm="handleUsers"
  />

  <el-dialog v-model="reductionVisible" title="选择减签人员" width="560px" append-to-body>
    <el-table :data="currentUsers" row-key="userId">
      <el-table-column prop="nickName" label="办理人" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="danger" @click="reduce(scope.row)">减签</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog v-model="backVisible" title="退回任务" width="560px" append-to-body>
    <el-alert v-if="backFailure" :title="backFailure" type="error" show-icon :closable="false" />
    <el-select v-model="backNodeCode" placeholder="请选择退回节点">
      <el-option v-for="node in backNodes" :key="node.nodeCode" :label="node.nodeName" :value="node.nodeCode" />
    </el-select>
    <el-input v-model="backMessage" type="textarea" :rows="3" placeholder="请输入退回意见" />
    <el-form-item label="附件">
      <FileUpload
        v-model="backFileId"
        :file-type="['png', 'jpg', 'jpeg', 'doc', 'docx', 'xlsx', 'xls', 'ppt', 'txt', 'pdf']"
        :file-size="20"
      />
    </el-form-item>
    <template #footer>
      <el-button type="primary" :loading="submitting" @click="back">确认退回</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { UserSummary, WorkflowTask } from '@namewta/domain-workflow';
import { computed, nextTick, reactive, ref } from 'vue';
import type { WorkflowWebRuntime } from '../runtime';
import {
  createBackPayload,
  createCompletePayload,
  createTaskOperationPayload,
  enabledProcessButtons
} from '../process-actions';
import UserSelect from './UserSelect.vue';

type NodeOption = {
  nodeCode: string;
  nodeName?: string;
  permissionFlag?: string | number | readonly (string | number)[];
};
type SelectorAction = 'addSignature' | 'copy' | 'delegateTask' | 'node' | 'transferTask';
type TaskOperation = 'addSignature' | 'delegateTask' | 'reductionSignature' | 'transferTask';

const props = withDefaults(
  defineProps<{
    allowComplete?: boolean;
    mode?: 'intervention' | 'participant';
    runtime: WorkflowWebRuntime;
    taskVariables?: Readonly<Record<string, unknown>>;
    width?: string;
  }>(),
  { allowComplete: true, mode: 'participant', taskVariables: () => ({}), width: '720px' }
);
const emit = defineEmits<{ cancelled: []; completed: [] }>();
const visible = ref(false);
const loading = ref(false);
const submitting = ref(false);
const failure = ref('');
const actionCompleted = ref(false);
const task = ref<WorkflowTask>();
const message = ref('');
const messageType = ref(['1']);
const fileId = ref('');
const FileUpload = props.runtime.fileUpload;
const nextNodes = ref<NodeOption[]>([]);
const copyUsers = ref<UserSummary[]>([]);
const assigneeMap = reactive<Record<string, string>>({});
const assigneeNames = reactive<Record<string, string>>({});
const enabled = computed(() => (task.value ? enabledProcessButtons(task.value, props.mode) : new Set<string>()));
const selector = ref<InstanceType<typeof UserSelect>>();
const selectorAction = ref<SelectorAction>('copy');
const selectorMultiple = ref(true);
const selectorData = ref<(string | number)[]>([]);
const selectorUserIds = ref<string | number | readonly (string | number)[]>();
const selectedNode = ref<NodeOption>();
const reductionVisible = ref(false);
const currentUsers = ref<UserSummary[]>([]);
const backVisible = ref(false);
const backNodes = ref<NodeOption[]>([]);
const backNodeCode = ref('');
const backMessage = ref('');
const backFileId = ref('');
const backFailure = ref('');

function can(code: string) {
  return task.value?.flowStatus === 'waiting' && enabled.value.has(code);
}
function canAction(code: string) {
  return task.value?.flowStatus === 'waiting' && enabled.value.has(code);
}
function canRatio(code: string) {
  return canAction(code) && Number(task.value?.nodeRatio ?? 0) > 0;
}
async function open(taskId: string | number) {
  visible.value = true;
  loading.value = true;
  failure.value = '';
  actionCompleted.value = false;
  message.value = '';
  messageType.value = ['1'];
  fileId.value = '';
  Object.keys(assigneeMap).forEach(key => delete assigneeMap[key]);
  Object.keys(assigneeNames).forEach(key => delete assigneeNames[key]);
  try {
    const [taskResponse, nodeResponse] = await Promise.all([
      props.runtime.service.getTask(taskId),
      props.runtime.service.getNextNodes({ taskId, variables: props.taskVariables })
    ]);
    task.value = taskResponse.data;
    nextNodes.value = (nodeResponse.data ?? []) as NodeOption[];
    copyUsers.value = [...(task.value?.copyList ?? [])];
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : '任务信息加载失败';
  } finally {
    loading.value = false;
  }
}
async function openSelector(action: SelectorAction, multiple: boolean) {
  selectorAction.value = action;
  selectorMultiple.value = multiple;
  selectorData.value = action === 'copy' ? copyUsers.value.map(user => user.userId) : [];
  selectorUserIds.value = undefined;
  await nextTick();
  await selector.value?.open();
}
async function selectNode(node: NodeOption) {
  selectedNode.value = node;
  selectorAction.value = 'node';
  selectorMultiple.value = true;
  selectorData.value = [];
  selectorUserIds.value = node.permissionFlag;
  await nextTick();
  await selector.value?.open();
}
async function handleUsers(users: UserSummary[]) {
  if (!task.value || !users.length) return;
  if (selectorAction.value === 'copy') {
    copyUsers.value = users;
    return;
  }
  if (selectorAction.value === 'node' && selectedNode.value) {
    assigneeMap[selectedNode.value.nodeCode] = users.map(user => user.userId).join(',');
    assigneeNames[selectedNode.value.nodeCode] = users.map(user => user.nickName).join(',');
    return;
  }
  if (selectorAction.value === 'node') return;
  await runOperation(selectorAction.value, users, selectorMultiple.value);
}
function removeCopy(userId: string | number) {
  copyUsers.value = copyUsers.value.filter(user => String(user.userId) !== String(userId));
}
async function execute(action: () => Promise<unknown>, success = '操作成功') {
  submitting.value = true;
  failure.value = '';
  try {
    await props.runtime.confirm('是否确认提交？');
    await action();
    props.runtime.success(success);
    actionCompleted.value = true;
    visible.value = false;
    emit('completed');
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : '流程操作失败';
  } finally {
    submitting.value = false;
  }
}
function handleClosed() {
  if (!actionCompleted.value) emit('cancelled');
}
async function complete() {
  if (!task.value) return;
  if (enabled.value.has('pop') && nextNodes.value.some(node => !assigneeMap[node.nodeCode])) {
    failure.value = '请选择审批人';
    return;
  }
  await execute(() =>
    props.runtime.service.completeTask(
      createCompletePayload({
        taskId: task.value!.id,
        message: message.value,
        messageType: messageType.value,
        variables: props.taskVariables,
        assigneeMap,
        copyUsers: copyUsers.value,
        fileId: fileId.value || undefined
      })
    )
  );
}
async function runOperation(operation: TaskOperation, users: UserSummary[], multiple: boolean) {
  if (!task.value) return;
  await execute(() =>
    props.runtime.service.operateTask(
      createTaskOperationPayload(task.value!.id, users, message.value, messageType.value, multiple),
      operation
    )
  );
}
async function openReduction() {
  if (!task.value) return;
  const response = await props.runtime.service.currentTaskUsers(task.value.id);
  currentUsers.value = response.data ?? [];
  reductionVisible.value = true;
}
async function reduce(input: unknown) {
  const user = input as UserSummary;
  reductionVisible.value = false;
  await runOperation('reductionSignature', [user], true);
}
async function terminate() {
  if (!task.value) return;
  await execute(
    () => props.runtime.service.terminateTask({ taskId: task.value!.id, comment: message.value.trim() }),
    '任务已终止'
  );
}
async function openBack() {
  if (!task.value) return;
  const response = await props.runtime.service.getBackTaskNodes(task.value.id, task.value.nodeCode);
  backNodes.value = (response.data ?? []) as NodeOption[];
  backNodeCode.value = backNodes.value[0]?.nodeCode ?? '';
  backMessage.value = '';
  backFileId.value = '';
  backFailure.value = '';
  backVisible.value = true;
}
async function back() {
  if (submitting.value || !task.value || !backNodeCode.value) return;
  const currentTask = task.value;
  submitting.value = true;
  backFailure.value = '';
  try {
    await props.runtime.confirm('是否确认提交？');
    await props.runtime.service.backProcess(
      createBackPayload({
        taskId: currentTask.id,
        nodeCode: backNodeCode.value,
        message: backMessage.value.trim(),
        messageType: [...messageType.value],
        variables: { ...props.taskVariables },
        fileId: backFileId.value || undefined
      })
    );
    props.runtime.success('操作成功');
    actionCompleted.value = true;
    backVisible.value = false;
    visible.value = false;
    emit('completed');
  } catch (error: unknown) {
    backFailure.value = error instanceof Error ? error.message : '退回失败';
  } finally {
    submitting.value = false;
  }
}

defineExpose({ open, close: () => (visible.value = false) });
</script>
