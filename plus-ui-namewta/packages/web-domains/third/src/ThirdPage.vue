<template>
  <div class="p-2 app-container third-page">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar-shell">
          <h3>{{ title }}</h3>
          <el-button v-if="editable" v-hasPermi="[permission + ':add']" type="primary" plain icon="Plus" @click="openForm">新增</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent="load">
        <el-form-item label="供应商">
          <el-input v-model="providerCode" clearable placeholder="供应商编码" @keyup.enter="load" />
        </el-form-item>
        <el-form-item v-if="kind === 'endpoints'" label="供应商 ID">
          <el-input v-model="providerId" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="load">查询</el-button></el-form-item>
      </el-form>
      <el-table v-loading="loading" border :data="rows">
        <el-table-column v-for="column in columns" :key="column.prop" :label="column.label" :prop="column.prop" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="kind === 'providers' || kind === 'endpoints'" label="状态" width="110" fixed="right">
          <template #default="scope">
            <el-switch v-hasPermi="[permission + ':edit']" :model-value="scope.row.status === '0'" inline-prompt active-text="启用" inactive-text="停用" @change="toggleStatus(scope.row, Boolean($event))" />
          </template>
        </el-table-column>
        <el-table-column v-if="editable" label="操作" width="250" fixed="right">
          <template #default="scope">
            <el-button v-hasPermi="[permission + ':edit']" link type="primary" icon="Edit" @click="editRow(scope.row)">编辑</el-button>
            <el-button v-if="kind === 'providers' || kind === 'endpoints'" v-hasPermi="['third:credential:list']" link type="primary" icon="Key" @click="openCredentials(scope.row)">凭据</el-button>
            <el-button v-hasPermi="[permission + ':remove']" link type="danger" icon="Delete" @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="formTitle" width="680px" destroy-on-close>
      <el-form :model="form" label-width="120px">
        <template v-if="kind === 'providers'">
          <el-form-item label="供应商编码" prop="providerCode"><el-input v-model="form.providerCode" :disabled="Boolean(form.providerId)" /></el-form-item>
          <el-form-item label="供应商名称" prop="providerName"><el-input v-model="form.providerName" /></el-form-item>
          <el-form-item label="Base URL" prop="baseUrl"><el-input v-model="form.baseUrl" placeholder="https://api.example.com" /></el-form-item>
          <el-form-item label="连接超时(ms)"><el-input-number v-model="form.timeoutConnectMs" :min="100" :max="120000" /></el-form-item>
          <el-form-item label="读取超时(ms)"><el-input-number v-model="form.timeoutReadMs" :min="100" :max="300000" /></el-form-item>
          <el-form-item label="供应商限流(次/秒)"><el-input-number v-model="form.rateLimit" :min="0" /></el-form-item>
          <el-form-item label="供应商并发上限"><el-input-number v-model="form.concurrencyLimit" :min="0" /></el-form-item>
          <el-form-item label="共享请求头 JSON"><el-input v-model="form.sharedHeadersJson" type="textarea" :rows="3" placeholder="仅允许服务端白名单头" /></el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
        </template>
        <template v-else-if="kind === 'endpoints'">
          <el-form-item label="供应商 ID" prop="providerId"><el-input v-model="form.providerId" :disabled="Boolean(form.endpointId)" /></el-form-item>
          <el-form-item label="供应商编码" prop="providerCode"><el-input v-model="form.providerCode" :disabled="Boolean(form.endpointId)" /></el-form-item>
          <el-form-item label="接口编码" prop="endpointCode"><el-input v-model="form.endpointCode" :disabled="Boolean(form.endpointId)" /></el-form-item>
          <el-form-item label="接口名称" prop="endpointName"><el-input v-model="form.endpointName" /></el-form-item>
          <el-form-item label="HTTP 方法"><el-select v-model="form.httpMethod"><el-option v-for="method in methods" :key="method" :label="method" :value="method" /></el-select></el-form-item>
          <el-form-item label="相对路径"><el-input v-model="form.relativePath" placeholder="/company/{id}" /></el-form-item>
          <el-form-item label="请求模式"><el-select v-model="form.requestMode"><el-option v-for="mode in requestModes" :key="mode" :label="mode" :value="mode" /></el-select></el-form-item>
          <el-form-item label="响应模式"><el-select v-model="form.responseMode"><el-option v-for="mode in responseModes" :key="mode" :label="mode" :value="mode" /></el-select></el-form-item>
          <el-form-item label="幂等接口"><el-switch v-model="form.idempotent" /></el-form-item>
          <el-form-item label="重试次数"><el-input-number v-model="form.retryCount" :min="0" :max="3" /></el-form-item>
          <el-form-item label="接口限流(次/秒)"><el-input-number v-model="form.rateLimit" :min="0" /></el-form-item>
          <el-form-item label="接口并发上限"><el-input-number v-model="form.concurrencyLimit" :min="0" /></el-form-item>
          <el-form-item label="Query 白名单 JSON"><el-input v-model="form.querySchemaJson" type="textarea" :rows="2" placeholder='{"allowed":["id"]}' /></el-form-item>
          <el-form-item label="Header 白名单 JSON"><el-input v-model="form.headerSchemaJson" type="textarea" :rows="2" placeholder='{"allowed":["Accept"]}' /></el-form-item>
          <el-form-item label="Body 白名单 JSON"><el-input v-model="form.bodySchemaJson" type="textarea" :rows="2" placeholder='{"allowed":["name"]}' /></el-form-item>
          <el-form-item label="共享头覆盖 JSON"><el-input v-model="form.overrideJson" type="textarea" :rows="2" placeholder='{"headers":{"X-Version":"v2"}}' /></el-form-item>
          <el-form-item label="响应脱敏字段 JSON"><el-input v-model="form.sensitiveFieldsJson" type="textarea" :rows="2" placeholder='["phone"]' /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button v-if="editing" v-hasPermi="[permission + ':edit']" type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button v-else v-hasPermi="[permission + ':add']" type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="credentialDialogVisible" title="凭据管理" width="680px" destroy-on-close>
      <el-table :data="credentialRows" border>
        <el-table-column prop="credentialType" label="类型" />
        <el-table-column prop="scopeType" label="作用范围" />
        <el-table-column prop="kekVersion" label="版本" />
        <el-table-column prop="expiresAt" label="过期时间" />
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button v-hasPermi="['third:credential:add']" link type="primary" @click="replaceCredential(scope.row)">替换</el-button>
            <el-button v-hasPermi="['third:credential:remove']" link type="danger" @click="removeCredential(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-hasPermi="['third:credential:add']">
        <el-divider />
        <el-form :model="credentialForm" label-width="120px">
          <el-form-item label="Provider code"><el-input v-model="credentialForm.providerCode" disabled /></el-form-item>
          <el-form-item label="Endpoint code"><el-input v-model="credentialForm.endpointCode" disabled /></el-form-item>
          <el-form-item label="凭据类型"><el-input v-model="credentialForm.credentialType" /></el-form-item>
          <el-form-item label="凭据 JSON"><el-input v-model="credentialForm.secretJson" type="textarea" :rows="4" show-password placeholder="保存时加密，不会回显" /></el-form-item>
          <el-form-item label="启用"><el-switch v-model="credentialForm.enabled" /></el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button v-hasPermi="['third:credential:add']" type="primary" :loading="credentialSaving" @click="saveCredential">保存</el-button>
        <el-button @click="credentialDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { CredentialSummary, Endpoint, Invocation, Provider, Statistic } from '@namewta/domain-third';
import { computed, onMounted, reactive, ref } from 'vue';
import type { ThirdWebRuntime } from './runtime';

const props = defineProps<{ runtime: ThirdWebRuntime; kind: 'providers' | 'endpoints' | 'invocations' | 'statistics' }>();
const loading = ref(false);
const saving = ref(false);
const rows = ref<Array<Provider | Endpoint | CredentialSummary | Invocation | Statistic>>([]);
const providerCode = ref('');
const providerId = ref('');
const dialogVisible = ref(false);
const credentialDialogVisible = ref(false);
const credentialSaving = ref(false);
const credentialRows = ref<CredentialSummary[]>([]);
const editing = ref(false);
const methods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
const requestModes = ['JSON', 'QUERY', 'FORM'];
const responseModes = ['JSON', 'TEXT', 'BYTES'];
const form = reactive<Record<string, any>>({});

const config = computed(() => ({
  providers: { title: '三方供应商', permission: 'third:provider', editable: true, columns: [{ label: '编码', prop: 'providerCode' }, { label: '名称', prop: 'providerName' }, { label: 'Base URL', prop: 'baseUrl' }] },
  endpoints: { title: '三方接口', permission: 'third:endpoint', editable: true, columns: [{ label: '供应商', prop: 'providerCode' }, { label: '接口编码', prop: 'endpointCode' }, { label: '方法', prop: 'httpMethod' }, { label: '路径', prop: 'relativePath' }] },
  invocations: { title: '调用明细', permission: 'third:invocation', editable: false, columns: [{ label: '请求 ID', prop: 'requestId' }, { label: '接口', prop: 'endpointCode' }, { label: '状态', prop: 'logicalStatus' }, { label: '失败分类', prop: 'failureCategory' }, { label: '耗时', prop: 'durationMs' }] },
  statistics: { title: '调用统计', permission: 'third:statistics', editable: false, columns: [{ label: '接口', prop: 'endpointCode' }, { label: '日期', prop: 'statDate' }, { label: '调用次数', prop: 'attemptCount' }, { label: '成功', prop: 'successCount' }, { label: '失败', prop: 'failureCount' }, { label: '限流拒绝', prop: 'rejectedCount' }] }
}[props.kind]));
const title = computed(() => config.value.title);
const formTitle = computed(() => `${editing.value ? '编辑' : '新增'}${title.value}`);
const permission = computed(() => config.value.permission);
const editable = computed(() => config.value.editable);
const columns = computed(() => config.value.columns);

function resetForm() {
  Object.assign(form, {
    providerId: '', providerCode: providerCode.value, providerName: '', baseUrl: '', status: '0',
    timeoutConnectMs: 3000, timeoutReadMs: 10000, rateLimit: 0, concurrencyLimit: 0, sharedHeadersJson: '', remark: '',
    endpointId: '', endpointCode: '', endpointName: '', httpMethod: 'GET', relativePath: '/', requestMode: 'JSON', responseMode: 'JSON',
    pathSchemaJson: '', querySchemaJson: '', headerSchemaJson: '', bodySchemaJson: '', responseSchemaJson: '', overrideJson: '',
    idempotent: false, retryCount: 0, sensitiveFieldsJson: '', adapterCode: '', credentialId: '', credentialType: 'API_KEY', secretJson: '', enabled: true
  });
}

async function load() {
  loading.value = true;
  try {
    const service = props.runtime.service;
    const response = props.kind === 'providers' ? await service.listProviders(providerCode.value)
        : props.kind === 'endpoints' ? await service.listEndpoints(providerId.value || undefined, providerCode.value || undefined)
          : props.kind === 'invocations' ? await service.listInvocations(providerCode.value)
            : await service.listStatistics(providerCode.value);
    rows.value = response.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openForm() { editing.value = false; resetForm(); dialogVisible.value = true; }

async function editRow(row: unknown) {
  editing.value = true;
  const value = props.kind === 'providers'
    ? await props.runtime.service.getProvider((row as Provider).providerId)
    : await props.runtime.service.getEndpoint((row as Endpoint).endpointId);
  Object.assign(form, value.data ?? row);
  dialogVisible.value = true;
}

async function save() {
  saving.value = true;
  try {
    const service = props.runtime.service;
    if (props.kind === 'providers') await service.saveProvider(form as any);
    else if (props.kind === 'endpoints') await service.saveEndpoint(form as any);
    dialogVisible.value = false;
    props.runtime.success?.('保存成功');
    await load();
  } catch (error) {
    props.runtime.error?.(String(error));
  } finally {
    saving.value = false;
  }
}

async function toggleStatus(row: unknown, enabled: boolean) {
  try {
    const status = enabled ? '0' : '1';
    if (props.kind === 'providers') await props.runtime.service.changeProviderStatus((row as Provider).providerId, status);
    else await props.runtime.service.changeEndpointStatus((row as Endpoint).endpointId, status);
    await load();
  } catch (error) {
    props.runtime.error?.(String(error));
    await load();
  }
}

async function remove(row: unknown) {
  await props.runtime.confirm?.('确认删除当前配置？');
  if (props.kind === 'providers') await props.runtime.service.deleteProvider((row as Provider).providerId);
  else await props.runtime.service.deleteEndpoint((row as Endpoint).endpointId);
  await load();
}

const credentialForm = reactive<{ credentialId?: string | number; providerCode: string; endpointCode?: string; credentialType: string; secretJson: string; enabled: boolean }>({
  providerCode: '', endpointCode: '', credentialType: 'API_KEY', secretJson: '', enabled: true
});

async function openCredentials(row: unknown) {
  const configRow = row as Provider | Endpoint;
  credentialForm.providerCode = configRow.providerCode;
  credentialForm.endpointCode = props.kind === 'endpoints' ? (configRow as Endpoint).endpointCode : '';
  credentialForm.credentialId = undefined;
  credentialForm.credentialType = 'API_KEY';
  credentialForm.secretJson = '';
  credentialForm.enabled = true;
  credentialDialogVisible.value = true;
  try {
    await refreshCredentials();
  } catch (error) {
    props.runtime.error?.(String(error));
  }
}

async function refreshCredentials() {
  const response = await props.runtime.service.listCredentials(credentialForm.providerCode, credentialForm.endpointCode || undefined);
  credentialRows.value = response.data ?? [];
}

function replaceCredential(row: unknown) {
  const credential = row as CredentialSummary;
  credentialForm.credentialId = credential.credentialId;
  credentialForm.credentialType = credential.credentialType;
  credentialForm.secretJson = '';
  credentialForm.enabled = credential.enabled === '0';
}

async function saveCredential() {
  credentialSaving.value = true;
  try {
    await props.runtime.service.saveCredential(credentialForm);
    credentialForm.secretJson = '';
    props.runtime.success?.('保存成功');
    await refreshCredentials();
  } catch (error) {
    props.runtime.error?.(String(error));
  } finally {
    credentialSaving.value = false;
  }
}

async function removeCredential(row: unknown) {
  await props.runtime.confirm?.('确认删除当前凭据？');
  await props.runtime.service.deleteCredential((row as CredentialSummary).credentialId);
  await refreshCredentials();
}

onMounted(load);
</script>
