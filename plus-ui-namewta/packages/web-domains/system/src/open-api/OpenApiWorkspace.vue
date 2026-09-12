<template>
  <div class="openapi-workspace">
    <header class="workspace-heading">
      <div>
        <h3>OpenAPI 凭据</h3>
        <p>{{ state.scopeLabel }}</p>
      </div>
      <el-button
        circle
        icon="Refresh"
        aria-label="刷新 OpenAPI 数据"
        :loading="state.loading"
        @click="controller.load"
      />
    </header>

    <el-alert
      v-if="state.error"
      :title="errorCopy[state.error].title"
      :description="errorCopy[state.error].description"
      :type="state.error === 'forbidden' ? 'warning' : 'error'"
      show-icon
      :closable="false"
      class="workspace-alert"
    >
      <template #default>
        <el-button v-if="state.error !== 'forbidden'" size="small" @click="controller.load">重试</el-button>
      </template>
    </el-alert>

    <div v-if="state.loading" v-loading="true" class="workspace-loading" aria-label="正在加载 OpenAPI 数据" />

    <template v-else>
      <section class="workspace-section" aria-labelledby="credential-heading">
        <div class="section-heading">
          <div>
            <h4 id="credential-heading">调用身份</h4>
            <p>AppKey 可重复查看，AppSecret 仅在创建或重置后展示一次。</p>
          </div>
          <el-tag v-if="state.credential" :type="credentialTag.type" effect="plain">{{ credentialTag.label }}</el-tag>
        </div>

        <el-empty v-if="!state.credential" description="尚未创建 OpenAPI 凭据" :image-size="72">
          <el-button v-if="controller.can('create')" type="primary" icon="Plus" @click="createVisible = true">
            创建凭据
          </el-button>
        </el-empty>

        <template v-else>
          <dl class="credential-grid">
            <div>
              <dt>应用名称</dt>
              <dd>{{ state.credential.appName }}</dd>
            </div>
            <div>
              <dt>AppKey</dt>
              <dd class="mono-value">{{ state.credential.appKey }}</dd>
            </div>
            <div>
              <dt>到期时间</dt>
              <dd>{{ formatDate(state.credential.expiresAt) }}</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ formatDate(state.credential.updateTime) }}</dd>
            </div>
            <div class="credential-remark">
              <dt>备注</dt>
              <dd>{{ state.credential.remark || '-' }}</dd>
            </div>
          </dl>
          <div class="credential-actions">
            <el-button v-if="controller.can('edit')" icon="RefreshRight" @click="confirmReset">重置密钥</el-button>
            <el-button
              v-if="controller.can('edit') && credentialState !== 'expired'"
              :type="credentialState === 'enabled' ? 'warning' : 'success'"
              :icon="credentialState === 'enabled' ? 'VideoPause' : 'VideoPlay'"
              @click="controller.setEnabled(credentialState !== 'enabled')"
            >
              {{ credentialState === 'enabled' ? '停用' : '启用' }}
            </el-button>
            <el-button v-if="controller.can('delete')" type="danger" plain icon="Delete" @click="confirmDelete">
              删除
            </el-button>
          </div>
        </template>
      </section>

      <section class="workspace-section" aria-labelledby="catalog-heading">
        <div class="section-heading">
          <div>
            <h4 id="catalog-heading">可调用接口</h4>
            <p>目录按当前身份的实时权限生成，不依赖凭据是否已创建。</p>
          </div>
          <span class="catalog-count">{{ state.catalog.length }} 项</span>
        </div>
        <el-empty v-if="state.catalog.length === 0" description="当前没有可调用接口" :image-size="72" />
        <el-table v-else :data="[...state.catalog]" row-key="interfaceId" class="catalog-table">
          <el-table-column label="方法" width="92">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.method }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路径" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="mono-value">{{ row.path }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="summary" label="说明" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="76" align="center">
            <template #default="{ row }">
              <el-tooltip content="查看调用合同" placement="top">
                <el-button circle text icon="View" aria-label="查看调用合同" @click="showInterface(row)" />
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <el-dialog v-model="createVisible" title="创建 OpenAPI 凭据" width="520px" append-to-body destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="createForm.appName" maxlength="100" placeholder="例如：订单同步服务" />
        </el-form-item>
        <el-form-item label="到期时间" prop="expiresAt">
          <el-date-picker
            v-model="createForm.expiresAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="不设置则长期有效"
            class="full-control"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="state.submitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog
      :model-value="Boolean(state.issued)"
      title="一次性 AppSecret"
      width="560px"
      append-to-body
      :close-on-click-modal="false"
      @update:model-value="closeIssuedSecret"
      @closed="controller.dismissIssuedSecret"
    >
      <el-alert
        title="关闭后无法再次查看，请立即保存到安全的密钥管理系统。"
        type="warning"
        show-icon
        :closable="false"
      />
      <div v-if="state.issued" class="issued-fields">
        <label>AppKey</label>
        <el-input :model-value="state.issued.appKey" readonly />
        <label>AppSecret</label>
        <div class="secret-row">
          <el-input :model-value="state.issued.appSecret" readonly aria-label="一次性 AppSecret" />
          <el-tooltip content="复制 AppSecret" placement="top">
            <el-button icon="DocumentCopy" aria-label="复制 AppSecret" @click="controller.copyIssuedSecret" />
          </el-tooltip>
        </div>
        <el-alert v-if="state.copyError" :title="state.copyError" type="error" show-icon :closable="false" />
      </div>
      <template #footer>
        <el-button type="primary" @click="controller.dismissIssuedSecret">关闭</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="接口调用合同" size="min(720px, 92vw)" append-to-body>
      <template v-if="selectedInterface">
        <dl class="interface-meta">
          <div>
            <dt>接口</dt>
            <dd>{{ selectedInterface.summary }}</dd>
          </div>
          <div>
            <dt>方法与路径</dt>
            <dd class="mono-value">{{ selectedInterface.method }} {{ selectedInterface.path }}</dd>
          </div>
          <div>
            <dt>响应模型</dt>
            <dd class="mono-value">{{ selectedInterface.responseSchema }}</dd>
          </div>
        </dl>
        <el-tabs>
          <el-tab-pane label="cURL">
            <pre class="code-block">{{ selectedInterface.curlExample }}</pre>
          </el-tab-pane>
          <el-tab-pane label="Java">
            <pre class="code-block">{{ selectedInterface.javaExample }}</pre>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus';
import { resolveOpenApiCredentialState, type OpenApiCatalogItem } from '@namewta/domain-system';
import { computed, reactive, ref, watch } from 'vue';
import {
  createOpenApiWorkspaceController,
  createOpenApiWorkspaceState,
  type OpenApiWorkspaceRuntime,
  type OpenApiWorkspaceScope
} from './workflow';

const props = defineProps<{ runtime: OpenApiWorkspaceRuntime; scope: OpenApiWorkspaceScope }>();
const state = reactive(createOpenApiWorkspaceState());
const createVisible = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = reactive({ appName: '', expiresAt: null as null | string, remark: '' });
const selectedInterface = ref<OpenApiCatalogItem>();
const controller = computed(() => createOpenApiWorkspaceController(props.runtime, state, props.scope));
const detailVisible = computed({
  get: () => Boolean(selectedInterface.value),
  set: value => {
    if (!value) selectedInterface.value = undefined;
  }
});
const credentialState = computed(() =>
  state.credential ? resolveOpenApiCredentialState(state.credential) : 'disabled'
);
const credentialTag = computed(
  () =>
    ({
      enabled: { label: '已启用', type: 'success' as const },
      disabled: { label: '已停用', type: 'warning' as const },
      expired: { label: '已过期', type: 'danger' as const }
    })[credentialState.value]
);
const errorCopy = {
  disabled: { title: 'OpenAPI 当前未启用', description: '服务端功能关闭或入口尚未装配。' },
  forbidden: { title: '无权访问', description: '当前账号没有此 OpenAPI scope 的权限。' },
  conflict: { title: '凭据状态已变化', description: '请刷新后再执行操作。' },
  unavailable: { title: 'OpenAPI 暂不可用', description: '请求未完成，请稍后重试。' }
} as const;
const createRules: FormRules = {
  appName: [{ required: true, message: '应用名称不能为空', trigger: 'blur' }]
};

watch(
  () => props.scope,
  () => {
    Object.assign(state, createOpenApiWorkspaceState());
    void controller.value.load();
  },
  { immediate: true }
);

async function submitCreate(): Promise<void> {
  if (!(await createFormRef.value?.validate().catch(() => false))) return;
  await controller.value.create({
    appName: createForm.appName,
    expiresAt: createForm.expiresAt,
    remark: createForm.remark || null
  });
  if (state.issued) {
    createVisible.value = false;
    Object.assign(createForm, { appName: '', expiresAt: null, remark: '' });
  }
}

async function confirmReset(): Promise<void> {
  await props.runtime.confirm('重置后旧 AppSecret 立即失效，是否继续？');
  await controller.value.reset();
}

async function confirmDelete(): Promise<void> {
  await props.runtime.confirm('删除后该凭据无法恢复，是否继续？');
  await controller.value.deleteCredential();
}

function formatDate(value: null | string): string {
  return value ? value.replace('T', ' ') : '长期有效';
}

function showInterface(value: unknown): void {
  selectedInterface.value = value as OpenApiCatalogItem;
}

function closeIssuedSecret(visible: boolean): void {
  if (!visible) controller.value.dismissIssuedSecret();
}
</script>

<style scoped>
.openapi-workspace {
  display: grid;
  gap: 18px;
  min-width: 0;
}
.workspace-heading,
.section-heading,
.credential-actions,
.secret-row {
  align-items: center;
  display: flex;
}
.workspace-heading,
.section-heading {
  justify-content: space-between;
  gap: 16px;
}
.workspace-heading h3,
.section-heading h4 {
  margin: 0;
}
.workspace-heading p,
.section-heading p {
  color: var(--el-text-color-secondary);
  margin: 5px 0 0;
}
.workspace-section {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 18px;
  min-width: 0;
}
.workspace-alert,
.workspace-loading {
  margin-bottom: 2px;
}
.workspace-loading {
  min-height: 180px;
}
.credential-grid,
.interface-meta {
  display: grid;
  gap: 16px 24px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 20px 0;
}
.credential-grid > div,
.interface-meta > div {
  min-width: 0;
}
.credential-grid dt,
.interface-meta dt,
.issued-fields label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-bottom: 6px;
}
.credential-grid dd,
.interface-meta dd {
  margin: 0;
  overflow-wrap: anywhere;
}
.credential-remark {
  grid-column: 1 / -1;
}
.credential-actions {
  flex-wrap: wrap;
  gap: 8px;
}
.catalog-count {
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}
.catalog-table {
  margin-top: 16px;
  width: 100%;
}
.mono-value,
.code-block {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
.issued-fields {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}
.secret-row {
  gap: 8px;
}
.secret-row .el-input {
  min-width: 0;
}
.full-control {
  width: 100%;
}
.code-block {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  margin: 0;
  overflow: auto;
  padding: 14px;
  white-space: pre-wrap;
}
@media (max-width: 640px) {
  .credential-grid,
  .interface-meta {
    grid-template-columns: 1fr;
  }
  .credential-remark {
    grid-column: auto;
  }
}
</style>
