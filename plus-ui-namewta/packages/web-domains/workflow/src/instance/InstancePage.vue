<template>
  <div class="workflow-instance-page app-container">
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
          <el-tabs v-model="tab" @tab-change="search">
            <el-tab-pane label="运行中" name="running" />
            <el-tab-pane label="已结束" name="finished" />
          </el-tabs>
          <el-form :inline="true" :model="query">
            <el-form-item label="流程名称"><el-input v-model="query.flowName" clearable /></el-form-item>
            <el-form-item label="流程编码"><el-input v-model="query.flowCode" clearable /></el-form-item>
            <el-form-item label="任务节点"><el-input v-model="query.nodeName" clearable /></el-form-item>
            <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
            <el-form-item><el-button @click="reset">重置</el-button></el-form-item>
            <el-form-item><el-button @click="userSelect?.open()">选择申请人</el-button></el-form-item>
          </el-form>
          <el-alert v-if="failure" :title="failure" type="error" show-icon :closable="false" />
          <el-table v-loading="loading" :data="rows" row-key="id" @selection-change="selection = $event">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="businessCode" label="业务编号" />
            <el-table-column prop="businessTitle" label="业务标题" />
            <el-table-column prop="flowName" label="流程名称" />
            <el-table-column prop="flowStatusName" label="流程状态" />
            <el-table-column v-if="tab === 'running'" v-hasPermi="['workflow:instance:active']" label="活动状态">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.activityStatus"
                  :active-value="1"
                  :inactive-value="0"
                  @change="toggleActive(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" min-width="170">
              <template #default="scope">
                <el-button
                  v-hasPermi="['workflow:instance:query']"
                  link
                  type="primary"
                  @click="openInstance(scope.row)"
                >
                  查看
                </el-button>
                <el-button
                  v-hasPermi="['workflow:instance:variableQuery']"
                  link
                  type="primary"
                  @click="showVariables(scope.row)"
                >
                  变量
                </el-button>
                <el-button v-hasPermi="['workflow:instance:query']" link type="primary" @click="showHistory(scope.row)">
                  历史
                </el-button>
                <el-button
                  v-if="tab === 'running'"
                  v-hasPermi="['workflow:instance:invalid']"
                  link
                  type="danger"
                  @click="openInvalid(scope.row)"
                >
                  作废
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @current-change="load"
          />
          <el-button
            v-hasPermi="['workflow:instance:remove']"
            :disabled="selection.length === 0"
            type="danger"
            @click="removeSelected"
          >
            删除
          </el-button>
        </el-card>
      </el-col>
    </el-row>
    <UserSelect ref="userSelect" :service="runtime.service" @confirm="filterUsers" />
    <el-dialog v-model="invalidVisible" title="作废流程" width="480px">
      <el-input v-model="invalidComment" type="textarea" :rows="4" placeholder="请输入作废原因" />
      <template #footer>
        <el-button @click="invalidVisible = false">取消</el-button>
        <el-button type="danger" :disabled="!invalidComment.trim()" @click="invalidate">确认作废</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="detailVisible" :title="detailTitle" width="680px">
      <pre class="instance-detail">{{ detail }}</pre>
      <el-form v-if="detailKind === 'variables'" :inline="true" label-width="88px">
        <el-form-item label="变量 KEY">
          <el-input v-model="variableKey" placeholder="请输入变量 KEY" />
        </el-form-item>
        <el-form-item label="变量值">
          <el-input v-model="variableValue" placeholder="请输入变量值" />
        </el-form-item>
        <el-form-item>
          <el-button
            v-hasPermi="['workflow:instance:variable']"
            type="primary"
            :loading="variableUpdating"
            :disabled="!variableKey.trim() || !variableValue.trim()"
            @click="updateVariable"
          >
            更新变量
          </el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { CategoryTreeVO, InstanceQuery, UserSummary, WorkflowInstance } from '@namewta/domain-workflow';
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { WorkflowWebRuntime } from '../runtime';
import UserSelect from '../components/UserSelect.vue';
import { createFlowInvalidPayload, createInstanceVariablePayload } from '../runtime-actions';

const props = defineProps<{ runtime: WorkflowWebRuntime }>();
const router = useRouter();
const tab = ref<'running' | 'finished'>('running');
const query = reactive<InstanceQuery>({ pageNum: 1, pageSize: 10, flowName: '' });
const rows = ref<WorkflowInstance[]>([]);
const selection = ref<WorkflowInstance[]>([]);
const total = ref(0);
const loading = ref(false);
const failure = ref('');
const userSelect = ref<InstanceType<typeof UserSelect>>();
const TreePanel = props.runtime.treePanel;
const categories = ref<CategoryTreeVO[]>([]);
const treeCollapsed = ref(false);
const invalidVisible = ref(false);
const invalidComment = ref('');
const invalidTarget = ref<WorkflowInstance>();
const detailVisible = ref(false);
const detailTitle = ref('');
const detail = ref<Record<string, unknown> | Record<string, unknown>[]>({});
const detailKind = ref<'history' | 'variables'>('history');
const variableTarget = ref<WorkflowInstance>();
const variableKey = ref('');
const variableValue = ref('');
const variableUpdating = ref(false);

async function load() {
  loading.value = true;
  failure.value = '';
  try {
    const response = await (tab.value === 'running'
      ? props.runtime.service.pageRunningInstances(query)
      : props.runtime.service.pageFinishedInstances(query));
    rows.value = response.data?.rows ?? [];
    total.value = response.data?.total ?? 0;
  } catch (error: unknown) {
    failure.value = error instanceof Error ? error.message : '流程实例查询失败';
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
  Object.assign(query, {
    pageNum: 1,
    flowName: '',
    flowCode: '',
    nodeName: '',
    createByIds: undefined,
    category: undefined
  });
  void load();
}
function filterUsers(users: UserSummary[]) {
  query.createByIds = users.map(user => user.userId);
  search();
}
function filterCategory(category: CategoryTreeVO) {
  query.category = category.id === '0' ? '' : category.id;
  search();
}
function openInstance(row: unknown) {
  const instance = row as WorkflowInstance;
  const task = instance.flowTaskList?.[0];
  const formPath = instance.formPath ?? task?.formPath;
  if (!formPath) return;
  void router.push({
    path: formPath,
    query: { id: instance.businessId, taskId: String(task?.id ?? instance.id), type: 'view' }
  });
}
function openInvalid(input: unknown) {
  invalidTarget.value = input as WorkflowInstance;
  invalidComment.value = '';
  invalidVisible.value = true;
}
async function invalidate() {
  const instance = invalidTarget.value;
  if (!instance) return;
  try {
    await props.runtime.confirm(`确认作废流程“${instance.businessTitle}”吗？`);
    await props.runtime.service.invalidateInstance(createFlowInvalidPayload(instance.id, invalidComment.value));
    props.runtime.success('流程已作废');
    invalidVisible.value = false;
    await load();
  } catch (error: unknown) {
    if (error instanceof Error) props.runtime.error(error.message);
  }
}
async function showVariables(input: unknown) {
  const row = input as WorkflowInstance;
  try {
    const response = await props.runtime.service.instanceVariables(row.id);
    variableTarget.value = row;
    variableKey.value = '';
    variableValue.value = '';
    detailKind.value = 'variables';
    detailTitle.value = `流程变量 - ${row.flowName}`;
    detail.value = response.data ?? {};
    detailVisible.value = true;
  } catch (error: unknown) {
    props.runtime.error(error instanceof Error ? error.message : '流程变量查询失败');
  }
}
async function showHistory(input: unknown) {
  const row = input as WorkflowInstance;
  try {
    const response = await props.runtime.service.flowHistory(row.businessId);
    detailKind.value = 'history';
    detailTitle.value = `流程历史 - ${row.flowName}`;
    detail.value = response.data?.list ?? [];
    detailVisible.value = true;
  } catch (error: unknown) {
    props.runtime.error(error instanceof Error ? error.message : '流程历史查询失败');
  }
}
async function updateVariable() {
  const row = variableTarget.value;
  if (!row || !variableKey.value.trim() || !variableValue.value.trim()) return;
  variableUpdating.value = true;
  try {
    await props.runtime.confirm('是否确认提交？');
    await props.runtime.service.updateInstanceVariables(
      createInstanceVariablePayload(row.id, variableKey.value, variableValue.value)
    );
    const response = await props.runtime.service.instanceVariables(row.id);
    detail.value = response.data ?? {};
    variableKey.value = '';
    variableValue.value = '';
    props.runtime.success('流程变量已更新');
  } catch (error: unknown) {
    if (error instanceof Error) props.runtime.error(error.message);
  } finally {
    variableUpdating.value = false;
  }
}
async function toggleActive(input: unknown) {
  const row = input as WorkflowInstance;
  const next = row.activityStatus === 1;
  try {
    await props.runtime.service.setInstanceActive(row.id, next);
    props.runtime.success(next ? '流程已激活' : '流程已挂起');
  } catch (error: unknown) {
    row.activityStatus = next ? 0 : 1;
    props.runtime.error(error instanceof Error ? error.message : '流程状态更新失败');
  }
}
async function removeSelected() {
  const ids = selection.value.map(item => item.id);
  if (!ids.length) return;
  try {
    await props.runtime.confirm('确认删除选中的流程实例吗？');
    if (tab.value === 'running') await props.runtime.service.deleteInstances(ids);
    else await props.runtime.service.deleteHistoricInstances(ids);
    props.runtime.success('删除成功');
    await load();
  } catch (error: unknown) {
    if (error instanceof Error) props.runtime.error(error.message);
  }
}
void Promise.all([loadCategories(), load()]);
</script>

<style scoped>
.workflow-instance-page .el-pagination {
  margin: 16px 0;
  justify-content: flex-end;
}
.instance-detail {
  max-height: 480px;
  overflow: auto;
  white-space: pre-wrap;
}
</style>
