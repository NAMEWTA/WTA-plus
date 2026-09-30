<template>
  <main class="app-container oidc-applications">
    <el-card shadow="never">
      <template #header>
        <div class="page-heading">
          <div>
            <h2>单点登录</h2>
            <p>为第三方应用配置统一登录，以及可获取的账户资料。</p>
          </div>
          <el-button v-if="can('add')" type="primary" :disabled="!loaded" @click="openCreate">创建应用</el-button>
        </div>
      </template>
      <el-alert
        v-if="provider && (!provider.ready || !provider.enabled)"
        title="统一登录服务尚未就绪，应用可以先完成配置，服务启用后即可接入。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form inline class="query-form" @submit.prevent="search">
        <el-form-item label="应用名称">
          <el-input v-model="name" clearable maxlength="128" placeholder="搜索应用" />
        </el-form-item>
        <el-form-item><el-button native-type="submit" :loading="loading">查询</el-button></el-form-item>
      </el-form>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-button v-if="error" class="retry-button" @click="initialize">重试加载</el-button>
      <el-table v-loading="loading" :data="applications" row-key="applicationId" border>
        <el-table-column label="应用名称" prop="name" min-width="160" />
        <el-table-column label="Client ID" prop="clientId" min-width="210" show-overflow-tooltip />
        <!-- @vue-generic {OidcApplication} -->
        <el-table-column label="登录回调地址" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.redirectUris.join('；') }}</template>
        </el-table-column>
        <!-- @vue-generic {OidcApplication} -->
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <!-- @vue-generic {OidcApplication} -->
        <el-table-column label="操作" min-width="290">
          <template #default="{ row }">
            <el-button v-if="can('query')" link type="primary" :disabled="busy" @click="showConfiguration(row)">
              接入配置
            </el-button>
            <el-button v-if="can('edit')" link type="primary" :disabled="busy" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('edit')" link :disabled="busy" @click="changeStatus(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
            <el-button v-if="can('rotate')" link type="warning" :disabled="busy" @click="rotate(row)">
              重置密钥
            </el-button>
            <el-button v-if="can('remove')" link type="danger" :disabled="busy" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > pageSize"
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        class="pagination"
        @current-change="changePage"
      />
    </el-card>
    <OidcApplicationForm
      :visible="formVisible"
      :application="editing"
      :fields="fields"
      :busy="busy"
      :error="formError"
      @close="closeForm"
      @save="save"
    />
    <OidcConfigurationDialog
      :visible="configurationVisible"
      :application="configurationApp"
      :secret="secret"
      :provider="provider"
      :fields="fields"
      :runtime="runtime"
      @close="closeConfiguration"
    />
  </main>
</template>
<script setup lang="ts">
import type { OidcApplication, OidcApplicationInput, OidcField, OidcProvider } from '@namewta/domain-oidc';
import {
  ElAlert,
  ElButton,
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElPagination,
  ElTable,
  ElTableColumn,
  ElTag,
  vLoading
} from 'element-plus';
import { onBeforeUnmount, onMounted, ref } from 'vue';
import type { OidcWebRuntime } from './runtime';
import OidcApplicationForm from './OidcApplicationForm.vue';
import OidcConfigurationDialog from './OidcConfigurationDialog.vue';

const { runtime } = defineProps<{ runtime: OidcWebRuntime }>();
const applications = ref<OidcApplication[]>([]);
const fields = ref<OidcField[]>([]);
const provider = ref<OidcProvider>();
const name = ref('');
const pageNum = ref(1);
const pageSize = 20;
const total = ref(0);
const loading = ref(false);
const loaded = ref(false);
const busy = ref(false);
const error = ref('');
const formError = ref('');
const formVisible = ref(false);
const editing = ref<OidcApplication>();
const configurationVisible = ref(false);
const configurationApp = ref<OidcApplication>();
const secret = ref<string>();
let live = true;
let listRequest: AbortController | undefined;
const lifetime = new AbortController();
const can = (action: string) => runtime.hasPermission(`oidc:application:${action}`);

async function loadList() {
  listRequest?.abort();
  const request = new AbortController();
  listRequest = request;
  loading.value = true;
  error.value = '';
  try {
    const page = await runtime.service.list(
      { pageNum: pageNum.value, pageSize, name: name.value.trim() || undefined },
      request.signal
    );
    if (!live || request.signal.aborted) return;
    applications.value = page.rows;
    total.value = page.total;
  } catch {
    if (live && !request.signal.aborted) error.value = '应用列表加载失败，请重试。';
  } finally {
    if (live && !request.signal.aborted) loading.value = false;
  }
}
async function initialize() {
  error.value = '';
  try {
    const [catalog, status] = await Promise.all([
      runtime.service.fields(lifetime.signal),
      runtime.service.provider(lifetime.signal)
    ]);
    if (!live) return;
    fields.value = catalog;
    provider.value = status;
    loaded.value = true;
    await loadList();
  } catch {
    if (live) error.value = '接入配置加载失败，请检查权限或重试。';
  }
}
function search() {
  pageNum.value = 1;
  void loadList();
}
function changePage(page: number) {
  pageNum.value = page;
  void loadList();
}
function openCreate() {
  editing.value = undefined;
  formError.value = '';
  formVisible.value = true;
}
function closeForm() {
  formVisible.value = false;
  editing.value = undefined;
  formError.value = '';
}
function closeConfiguration() {
  secret.value = undefined;
  configurationApp.value = undefined;
  configurationVisible.value = false;
}

async function run(action: () => Promise<void>, message = '操作未完成，请刷新后重试。') {
  if (busy.value) return;
  busy.value = true;
  error.value = '';
  try {
    await action();
  } catch {
    if (live) error.value = message;
  } finally {
    if (live) busy.value = false;
  }
}
async function openEdit(row: OidcApplication) {
  await run(async () => {
    const app = await runtime.service.get(row.applicationId, lifetime.signal);
    if (!live) return;
    editing.value = app;
    formError.value = '';
    formVisible.value = true;
  });
}
async function showConfiguration(row: OidcApplication) {
  await run(async () => {
    const app = await runtime.service.get(row.applicationId, lifetime.signal);
    if (!live) return;
    secret.value = undefined;
    configurationApp.value = app;
    configurationVisible.value = true;
  });
}
async function save(input: OidcApplicationInput) {
  if (busy.value) return;
  busy.value = true;
  formError.value = '';
  try {
    if (editing.value) {
      await runtime.service.update(
        { ...input, applicationId: editing.value.applicationId, version: editing.value.version },
        lifetime.signal
      );
      if (!live) return;
      runtime.success('应用配置已保存');
    } else {
      const delivery = await runtime.service.create(input, lifetime.signal);
      if (!live) return;
      configurationApp.value = delivery.application;
      secret.value = delivery.clientSecret;
      configurationVisible.value = true;
    }
    closeForm();
    await loadList();
  } catch {
    if (live) formError.value = '保存失败，请检查回调地址与字段设置；配置发生变化时请关闭后重新编辑。';
  } finally {
    if (live) busy.value = false;
  }
}
async function confirm(message: string) {
  try {
    await runtime.confirm(message);
    return live;
  } catch {
    return false;
  }
}
async function changeStatus(row: OidcApplication) {
  if (!(await confirm(row.enabled ? `停用“${row.name}”后将停止登录并撤销已有授权。` : `启用“${row.name}”？`))) return;
  await run(async () => {
    await runtime.service.status(row.applicationId, row.version, !row.enabled, lifetime.signal);
    if (live) await loadList();
  });
}
async function rotate(row: OidcApplication) {
  if (!(await confirm(`重置“${row.name}”的密钥？旧密钥及已有授权会失效，请同步更新第三方配置。`))) return;
  await run(async () => {
    const delivery = await runtime.service.rotateSecret(row.applicationId, row.version, lifetime.signal);
    if (!live) return;
    configurationApp.value = delivery.application;
    secret.value = delivery.clientSecret;
    configurationVisible.value = true;
    await loadList();
  });
}
async function remove(row: OidcApplication) {
  if (!(await confirm(`删除“${row.name}”并撤销该应用的授权？`))) return;
  await run(async () => {
    await runtime.service.remove(row.applicationId, row.version, lifetime.signal);
    if (live) await loadList();
  });
}
onMounted(initialize);
onBeforeUnmount(() => {
  live = false;
  lifetime.abort();
  listRequest?.abort();
  closeConfiguration();
});
</script>
<style scoped>
.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
h2 {
  margin: 0;
  font-size: 20px;
}
.page-heading p {
  color: var(--el-text-color-secondary);
  margin: 8px 0 0;
}
.query-form {
  margin-top: 20px;
}
.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
.retry-button {
  margin: 12px 0;
}
</style>
