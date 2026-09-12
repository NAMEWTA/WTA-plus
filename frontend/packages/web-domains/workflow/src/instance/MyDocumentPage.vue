<template>
  <div class="workflow-document-page app-container">
    <el-row :gutter="20">
      <TreePanel
        v-model:collapsed="treeCollapsed"
        title="流程分类"
        placeholder="请输入流程分类名"
        :data="categories"
        :expanded-span="4"
        filter-field="label"
        @node-click="filterCategory"
      />
      <el-col :lg="treeCollapsed ? 23 : 20" :xs="24">
        <el-card shadow="never">
          <el-form :inline="true" :model="query">
            <el-form-item label="流程名称"><el-input v-model="query.flowName" clearable /></el-form-item>
            <el-form-item label="流程编码"><el-input v-model="query.flowCode" clearable /></el-form-item>
            <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
            <el-form-item><el-button @click="reset">重置</el-button></el-form-item>
          </el-form>
          <el-alert v-if="failure" :title="failure" type="error" show-icon :closable="false" />
          <el-table v-loading="loading" :data="rows" row-key="id">
            <el-table-column prop="businessCode" label="业务编号" />
            <el-table-column prop="businessTitle" label="业务标题" />
            <el-table-column prop="flowName" label="流程名称" />
            <el-table-column prop="flowStatusName" label="状态" />
            <el-table-column label="操作">
              <template #default="scope">
                <el-button link type="primary" @click="open(scope.row, 'view')">查看</el-button>
                <el-button
                  v-if="isEditable(scope.row)"
                  v-hasPermi="['workflow:instance:currentList']"
                  link
                  type="primary"
                  @click="open(scope.row, 'update')"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="isEditable(scope.row)"
                  v-hasPermi="['workflow:instance:remove']"
                  link
                  type="danger"
                  @click="remove(scope.row)"
                >
                  删除
                </el-button>
                <el-button
                  v-if="scope.row.flowStatus === 'waiting'"
                  v-hasPermi="['workflow:instance:cancel']"
                  link
                  type="danger"
                  @click="cancel(scope.row)"
                >
                  撤销
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="query.pageNum" :total="total" @current-change="load" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import type { CategoryTreeVO, InstanceQuery, WorkflowInstance } from '@namewta/domain-workflow';
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { WorkflowWebRuntime } from '../runtime';
import { createCancelProcessPayload } from '../runtime-actions';

const props = defineProps<{ runtime: WorkflowWebRuntime }>();
const router = useRouter();
const query = reactive<InstanceQuery>({ pageNum: 1, pageSize: 10, flowName: '' });
const rows = ref<WorkflowInstance[]>([]);
const total = ref(0);
const loading = ref(false);
const failure = ref('');
const TreePanel = props.runtime.treePanel;
const treeCollapsed = ref(false);
const categories = ref<CategoryTreeVO[]>([]);
async function load() {
  loading.value = true;
  failure.value = '';
  try {
    const response = await props.runtime.service.pageCurrentInstances(query);
    rows.value = response.data?.rows ?? [];
    total.value = response.data?.total ?? 0;
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : '单据查询失败';
  } finally {
    loading.value = false;
  }
}
async function loadCategories() {
  try {
    const response = await props.runtime.service.categoryTree();
    categories.value = response.data ?? [];
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : '流程分类查询失败';
  }
}
function search() {
  query.pageNum = 1;
  void load();
}
function reset() {
  Object.assign(query, { pageNum: 1, flowName: '', flowCode: '', category: undefined });
  void load();
}
function filterCategory(category: CategoryTreeVO) {
  query.category = category.id === '0' ? '' : category.id;
  search();
}
function isEditable(input: unknown) {
  const row = input as WorkflowInstance;
  return row.flowStatus === 'draft' || row.flowStatus === 'cancel' || row.flowStatus === 'back';
}
function open(row: unknown, type: 'update' | 'view') {
  const instance = row as WorkflowInstance;
  const task = instance.flowTaskList?.[0];
  const formPath = instance.formPath ?? task?.formPath;
  if (formPath)
    void router.push({
      path: formPath,
      query: { id: instance.businessId, taskId: String(task?.id ?? instance.id), type }
    });
}
async function remove(input: unknown) {
  const row = input as WorkflowInstance;
  try {
    await props.runtime.confirm(`确认删除“${row.businessTitle}”吗？`);
    await props.runtime.service.deleteInstances(row.id);
    props.runtime.success('删除成功');
    await load();
  } catch (error: unknown) {
    if (error instanceof Error) props.runtime.error(error.message);
  }
}
async function cancel(row: unknown) {
  const instance = row as WorkflowInstance;
  try {
    await props.runtime.confirm(`确认撤销“${instance.businessTitle}”吗？`);
    await props.runtime.service.cancelProcess(createCancelProcessPayload(instance.businessId));
    props.runtime.success('撤销成功');
    await load();
  } catch (error: unknown) {
    if (error instanceof Error) props.runtime.error(error.message);
  }
}
void Promise.all([loadCategories(), load()]);
</script>
