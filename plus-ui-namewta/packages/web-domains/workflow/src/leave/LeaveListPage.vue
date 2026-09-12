<template>
  <div class="workflow-leave-page app-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="请假天数"><el-input-number v-model="query.startLeaveDays" :min="0" /></el-form-item>
        <el-form-item label="至"><el-input-number v-model="query.endLeaveDays" :min="0" /></el-form-item>
        <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
        <el-form-item><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
      <el-button v-hasPermi="['workflow:leave:add']" type="primary" @click="create">新增</el-button>
      <el-button v-hasPermi="['workflow:leave:export']" type="warning" @click="exportRows">导出</el-button>
      <el-alert v-if="failure" :title="failure" type="error" show-icon :closable="false" />
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="applyCode" label="申请编号" />
        <el-table-column label="请假类型">
          <template #default="scope">{{ leaveTypeLabel(scope.row.leaveType) }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" />
        <el-table-column prop="endDate" label="结束日期" />
        <el-table-column prop="leaveDays" label="天数" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" min-width="180">
          <template #default="scope">
            <el-button link type="primary" @click="view(scope.row)">查看</el-button>
            <el-button
              v-if="isEditableLeaveStatus(scope.row.status)"
              v-hasPermi="['workflow:leave:edit']"
              link
              type="primary"
              @click="edit(scope.row)"
            >
              修改
            </el-button>
            <el-button
              v-if="scope.row.status === 'waiting'"
              v-hasPermi="['workflow:instance:cancel']"
              link
              type="warning"
              @click="cancel(scope.row)"
            >
              撤销
            </el-button>
            <el-button
              v-if="isEditableLeaveStatus(scope.row.status)"
              v-hasPermi="['workflow:leave:remove']"
              link
              type="danger"
              @click="remove(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.pageNum" :total="total" @current-change="load" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { LeaveQuery, LeaveRecord } from '@namewta/domain-workflow';
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { WorkflowWebRuntime } from '../runtime';
import { isEditableLeaveStatus } from '../runtime-actions';

const props = defineProps<{ runtime: WorkflowWebRuntime }>();
const router = useRouter();
const query = reactive<LeaveQuery>({ pageNum: 1, pageSize: 10 });
const rows = ref<LeaveRecord[]>([]);
const total = ref(0);
const loading = ref(false);
const failure = ref('');
async function load() {
  loading.value = true;
  failure.value = '';
  try {
    const response = await props.runtime.service.listLeaves(query);
    rows.value = response.data?.rows ?? [];
    total.value = response.data?.total ?? 0;
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : '请假列表查询失败';
  } finally {
    loading.value = false;
  }
}
function create() {
  void router.push({ path: '/workflow/leaveEdit/index', query: { type: 'add' } });
}
function edit(input: unknown) {
  const row = input as LeaveRecord;
  void router.push({ path: '/workflow/leaveEdit/index', query: { id: String(row.id), type: 'update' } });
}
function view(input: unknown) {
  const row = input as LeaveRecord;
  void router.push({ path: '/workflow/leaveEdit/index', query: { id: String(row.id), type: 'view' } });
}
function search() {
  query.pageNum = 1;
  void load();
}
function reset() {
  Object.assign(query, { pageNum: 1, startLeaveDays: undefined, endLeaveDays: undefined });
  void load();
}
function exportRows() {
  return props.runtime.download('/workflow/leave/export', { ...query }, `leave_${Date.now()}.xlsx`);
}
function leaveTypeLabel(value: string) {
  return ({ '1': '事假', '2': '调休', '3': '病假', '4': '婚假' } as Record<string, string>)[value] ?? value;
}
async function cancel(input: unknown) {
  const row = input as LeaveRecord;
  try {
    await props.runtime.confirm('确认撤销该请假流程吗？');
    await props.runtime.service.cancelProcess({ businessId: row.id, message: '申请人撤销流程！' });
    props.runtime.success('撤销成功');
    await load();
  } catch (error: unknown) {
    if (error instanceof Error) props.runtime.error(error.message);
  }
}
async function remove(input: unknown) {
  const row = input as LeaveRecord;
  try {
    await props.runtime.confirm('确认删除该请假申请吗？');
    await props.runtime.service.deleteLeaves(row.id);
    props.runtime.success('删除成功');
    await load();
  } catch (error: unknown) {
    if (error instanceof Error) props.runtime.error(error.message);
  }
}
void load();
</script>
