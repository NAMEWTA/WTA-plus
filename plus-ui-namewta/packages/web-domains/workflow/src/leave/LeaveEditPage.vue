<template>
  <div class="workflow-leave-edit-page app-container">
    <el-card shadow="never">
      <template #header>请假申请</template>
      <el-alert v-if="failure" :title="failure" type="error" show-icon :closable="false" />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="routeType === 'add'" label="流程定义">
          <el-select v-model="flowCode">
            <el-option v-for="item in flowCodes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" :disabled="readonly">
            <el-option label="事假" value="1" />
            <el-option label="调休" value="2" />
            <el-option label="病假" value="3" />
            <el-option label="婚假" value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假时间" required>
          <el-date-picker
            v-model="leaveRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :default-time="[new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]"
            :disabled="readonly"
            @change="syncLeaveRange"
          />
        </el-form-item>
        <el-form-item label="请假天数" prop="leaveDays">
          <el-input :model-value="form.leaveDays" disabled />
        </el-form-item>
        <el-form-item label="请假原因" prop="remark">
          <el-input v-model="form.remark" type="textarea" :disabled="readonly" />
        </el-form-item>
      </el-form>
      <div
        v-if="!readonly && (routeType === 'add' || routeType === 'update')"
        v-hasPermi="routeType === 'add' ? ['workflow:leave:add'] : ['workflow:leave:edit']"
        class="actions"
      >
        <el-button :loading="saving" @click="save">保存</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交审批</el-button>
        <el-button v-if="routeType === 'add'" type="success" :loading="saving" @click="submitDirect">
          后端发起
        </el-button>
      </div>
      <template v-if="effectiveTaskId && (routeType === 'approval' || startedTaskId)">
        <el-divider content-position="left">审批办理</el-divider>
        <el-button type="primary" @click="processActions?.open(effectiveTaskId)">办理任务</el-button>
        <ProcessActionDialog
          ref="processActions"
          :runtime="runtime"
          :task-variables="taskVariables"
          @completed="closeCompletedPage"
        />
      </template>
      <el-divider v-if="id" content-position="left">流程记录</el-divider>
      <el-timeline v-if="id">
        <el-timeline-item
          v-for="item in history"
          :key="String(item.id ?? item.nodeName)"
          :timestamp="String(item.createTime ?? '')"
        >
          {{ item.nodeName ?? item.message ?? '流程记录' }}
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { LeaveForm } from '@namewta/domain-workflow';
import { computed, nextTick, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import type { WorkflowWebRuntime } from '../runtime';
import ProcessActionDialog from '../components/ProcessActionDialog.vue';
import { calculateLeaveDays } from '../runtime-actions';

const props = defineProps<{ runtime: WorkflowWebRuntime }>();
const route = useRoute();
const id = computed(() => String(route.query.id ?? ''));
const taskId = computed(() => String(route.query.taskId ?? ''));
const startedTaskId = ref('');
const effectiveTaskId = computed(() => taskId.value || startedTaskId.value);
const routeType = computed(() => String(route.query.type ?? 'add'));
const readonly = computed(() => route.query.type === 'view' || route.query.type === 'approval');
const flowCode = ref('leave1');
const flowCodes = [
  { value: 'leave1', label: '请假申请-普通' },
  { value: 'leave2', label: '请假申请-条件' },
  { value: 'leave3', label: '请假申请-会签' },
  { value: 'leave4', label: '请假申请-票签' },
  { value: 'leave5', label: '请假申请-并行会签' },
  { value: 'leave6', label: '请假申请-排他并行会签' }
];
const formRef = ref<{ validate: () => Promise<boolean> }>();
const form = reactive<LeaveForm>({ leaveType: '', startDate: '', endDate: '', leaveDays: 1, remark: '' });
const leaveRange = ref<string[]>([]);
const rules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  remark: [{ required: true, message: '请输入请假原因', trigger: 'blur' }]
};
const saving = ref(false);
const failure = ref('');
const history = ref<Record<string, unknown>[]>([]);
const processActions = ref<InstanceType<typeof ProcessActionDialog>>();
const taskVariables = computed(() => ({ leaveDays: form.leaveDays, userList: ['1', '3', '4'] }));

async function load() {
  if (!id.value) return;
  try {
    const [leave, records] = await Promise.all([
      props.runtime.service.getLeave(id.value),
      props.runtime.service.flowHistory(id.value)
    ]);
    Object.assign(form, leave.data);
    leaveRange.value = form.startDate && form.endDate ? [form.startDate, form.endDate] : [];
    history.value = records.data?.list ?? [];
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : '请假详情加载失败';
  }
}
async function persist(action: 'draft' | 'start' | 'direct') {
  syncLeaveRange();
  if (leaveRange.value.length !== 2 || form.leaveDays === undefined) {
    failure.value = '请选择有效的请假时间';
    return;
  }
  await formRef.value?.validate();
  saving.value = true;
  failure.value = '';
  try {
    const response =
      action === 'direct'
        ? await props.runtime.service.submitLeave({ ...form })
        : form.id
          ? await props.runtime.service.updateLeave({ ...form })
          : await props.runtime.service.addLeave({ ...form });
    Object.assign(form, response.data);
    if (action === 'start') {
      if (!form.id) throw new Error('请假申请未返回业务 ID');
      const started = await props.runtime.service.startWorkflow({
        businessId: form.id,
        flowCode: flowCode.value,
        variables: { leaveDays: form.leaveDays, userList: ['1', '3', '4'] },
        bizExt: { businessTitle: '请假申请', businessCode: form.applyCode }
      });
      const startedTask = (started.data as { taskId?: string | number } | undefined)?.taskId;
      startedTaskId.value = startedTask === undefined ? '' : String(startedTask);
      if (startedTaskId.value) {
        await nextTick();
        await processActions.value?.open(startedTaskId.value);
      }
    }
    props.runtime.success(action === 'draft' ? '保存成功' : '提交成功');
    if (action !== 'start') await props.runtime.closeCurrentPage();
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : action === 'draft' ? '保存失败' : '提交失败';
  } finally {
    saving.value = false;
  }
}
function syncLeaveRange() {
  form.startDate = leaveRange.value[0] ?? '';
  form.endDate = leaveRange.value[1] ?? '';
  form.leaveDays = calculateLeaveDays(leaveRange.value);
}
async function closeCompletedPage() {
  await props.runtime.closeCurrentPage();
}
function save() {
  return persist('draft');
}
function submit() {
  return persist('start');
}
function submitDirect() {
  return persist('direct');
}
void load();
</script>

<style scoped>
.workflow-leave-edit-page .actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
