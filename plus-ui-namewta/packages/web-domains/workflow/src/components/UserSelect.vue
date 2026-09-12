<template>
  <el-dialog v-model="visible" title="选择用户" width="920px" destroy-on-close @closed="resetDialog">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-input v-model="departmentFilter" clearable placeholder="筛选部门" />
        <el-tree
          ref="departmentTreeRef"
          :data="departments"
          node-key="id"
          :props="{ label: 'label', children: 'children' }"
          :filter-node-method="filterDepartment"
          @node-click="selectDepartment"
        />
      </el-col>
      <el-col :span="18">
        <el-form :inline="true" :model="query" @submit.prevent="search">
          <el-form-item label="用户名称"><el-input v-model="query.userName" clearable /></el-form-item>
          <el-form-item label="手机号码"><el-input v-model="query.phoneNumber" clearable /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable style="width: 110px">
              <el-option label="正常" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
          <el-form-item><el-button @click="resetSearch">重置</el-button></el-form-item>
        </el-form>
        <el-alert v-if="failure" :title="failure" type="error" show-icon :closable="false" />
        <el-table ref="tableRef" v-loading="loading" :data="rows" row-key="userId" @selection-change="select">
          <el-table-column type="selection" width="48" :selectable="() => multiple || selected.length === 0" />
          <el-table-column prop="userName" label="用户名称" />
          <el-table-column prop="nickName" label="用户昵称" />
          <el-table-column prop="deptName" label="部门" />
          <el-table-column prop="status" label="状态" />
        </el-table>
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="load"
        />
      </el-col>
    </el-row>
    <div v-if="selected.length" class="selected-users">
      <el-tag v-for="user in selected" :key="String(user.userId)" closable @close="remove(user.userId)">
        {{ user.nickName }}
      </el-tag>
    </div>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :disabled="selected.length === 0" @click="confirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { DepartmentSummary, UserSummary, WorkflowDefinitionService } from '@namewta/domain-workflow';
import { nextTick, reactive, ref, watch } from 'vue';
import { mergeUserSelection, prepareUserSelection } from '../user-selection';

const props = withDefaults(
  defineProps<{
    data?: string | number | readonly (string | number)[];
    modelValue?: UserSummary | UserSummary[];
    multiple?: boolean;
    service: WorkflowDefinitionService;
    userIds?: string | number | readonly (string | number)[];
  }>(),
  { data: undefined, modelValue: undefined, multiple: true, userIds: undefined }
);
const emit = defineEmits<{ confirm: [users: UserSummary[]]; 'update:modelValue': [users: UserSummary[]] }>();
const visible = ref(false);
const loading = ref(false);
const failure = ref('');
const rows = ref<UserSummary[]>([]);
const selected = ref<UserSummary[]>([]);
const total = ref(0);
const departments = ref<DepartmentSummary[]>([]);
const departmentFilter = ref('');
const syncingSelection = ref(false);
const tableRef = ref<{ clearSelection(): void; toggleRowSelection(row: UserSummary, selected: boolean): void }>();
const departmentTreeRef = ref<{ filter(value: string): void; setCurrentKey(key?: string): void }>();
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  phoneNumber: '',
  status: '',
  deptId: undefined as string | number | undefined,
  userIds: undefined as string | undefined
});

watch(departmentFilter, value => departmentTreeRef.value?.filter(value));

async function load() {
  loading.value = true;
  failure.value = '';
  try {
    const response = await props.service.users.list({ ...query });
    rows.value = response.data.rows;
    total.value = response.data.total;
    await nextTick();
    syncingSelection.value = true;
    tableRef.value?.clearSelection();
    const selectedIds = new Set(selected.value.map(user => String(user.userId)));
    for (const row of rows.value)
      if (selectedIds.has(String(row.userId))) tableRef.value?.toggleRowSelection(row, true);
    await nextTick();
    syncingSelection.value = false;
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : '用户查询失败';
  } finally {
    loading.value = false;
  }
}
function search() {
  query.pageNum = 1;
  void load();
}
function resetSearch() {
  Object.assign(query, { pageNum: 1, userName: '', phoneNumber: '', status: '', deptId: undefined });
  departmentTreeRef.value?.setCurrentKey();
  void load();
}
function filterDepartment(value: string, data: DepartmentSummary) {
  return !value || data.label.includes(value);
}
function selectDepartment(data: DepartmentSummary) {
  query.deptId = data.id;
  search();
}
function select(users: UserSummary[]) {
  if (syncingSelection.value) return;
  selected.value = mergeUserSelection(selected.value, rows.value, users, props.multiple);
}
function remove(userId: string | number) {
  selected.value = selected.value.filter(user => String(user.userId) !== String(userId));
  const row = rows.value.find(user => String(user.userId) === String(userId));
  if (row) tableRef.value?.toggleRowSelection(row, false);
}
function confirm() {
  const result = [...selected.value];
  emit('update:modelValue', result);
  emit('confirm', result);
  close();
}
function close() {
  visible.value = false;
}
function resetDialog() {
  Object.assign(query, { pageNum: 1, userName: '', phoneNumber: '', status: '', deptId: undefined });
  departmentFilter.value = '';
  rows.value = [];
  total.value = 0;
  failure.value = '';
}
async function open() {
  visible.value = true;
  failure.value = '';
  try {
    const prepared = await prepareUserSelection(props.service, props);
    selected.value = prepared.selected;
    query.userIds = prepared.listUserIds;
    const tree = await props.service.users.departmentTree();
    departments.value = tree.data;
    await load();
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : '用户选择初始化失败';
  }
}

defineExpose({ open, close });
</script>

<style scoped>
.selected-users {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
</style>
