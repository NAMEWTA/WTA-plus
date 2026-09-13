<template>
  <div class="p-2 app-container system-sso-app-page">
    <el-card shadow="hover" class="table-panel">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <h3 data-testid="sso-admin-title">SSO 管理</h3>
            <p class="table-subtitle">创建应用并交付配置。客户端管理不承担创建主路径。</p>
          </div>
          <div class="toolbar-actions">
            <el-button
              v-hasPermi="['system:ssoApp:add']"
              type="primary"
              plain
              icon="Plus"
              data-testid="sso-admin-create"
              @click="handleAdd"
            >
              创建应用
            </el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="appList" border class="data-table">
        <el-table-column label="clientId" align="center" prop="clientId" min-width="180" />
        <el-table-column label="应用 key" align="center" prop="clientKey" />
        <el-table-column label="类型" align="center" prop="ssoClientKind" width="120" />
        <el-table-column label="精确回调" align="center" prop="ssoRedirectUris" min-width="220" show-overflow-tooltip />
        <el-table-column label="密钥" align="center" width="100">
          <template #default="scope">
            {{ scope.row.ssoSecretConfigured ? '已配置' : '未配置' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="160">
          <template #default="scope">
            <el-button
              v-hasPermi="['system:ssoApp:edit']"
              link
              type="primary"
              data-testid="sso-admin-deliver"
              @click="handleUpdate(scope.row)"
            >
              拿配置
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="应用 key" prop="clientKey">
          <el-input v-model="form.clientKey" :disabled="form.id != null" placeholder="例如 demo-app" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="登录密钥" prop="clientSecret">
          <el-input v-model="form.clientSecret" placeholder="用于生成 clientId，非 OAuth 密钥" />
        </el-form-item>
        <el-form-item label="应用种类" prop="ssoClientKind">
          <el-select v-model="form.ssoClientKind">
            <el-option label="自有（public）" value="public" />
            <el-option label="外部（confidential）" value="confidential" />
          </el-select>
        </el-form-item>
        <el-form-item label="精确回调" prop="ssoRedirectUris">
          <el-input
            v-model="form.ssoRedirectUris"
            type="textarea"
            :rows="3"
            placeholder="禁止 *，每行一个完整 http(s) 回调"
          />
        </el-form-item>
        <el-form-item label="登录域" prop="userTypeId">
          <el-select v-model="form.userTypeId" filterable>
            <el-option
              v-for="item in userTypeOptions"
              :key="item.userTypeId"
              :label="item.userTypeName"
              :value="item.userTypeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.id" label="clientId">
          <el-input :model-value="form.clientId" readonly />
        </el-form-item>
        <el-form-item v-if="form.id" label="密钥">
          <span>{{ secretConfigured ? '已配置' : '未配置' }}</span>
          <el-button type="warning" plain @click="handleRotate">轮换并再拿一次明文</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :loading="buttonLoading" type="primary" @click="submitForm">保存并交付</el-button>
        <el-button @click="closeDialog">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import type { ClientForm, ClientQuery, ClientVO, UserTypeVO } from '@namewta/domain-system';
import type { FormInstance as ElFormInstance } from 'element-plus';
import { ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref, toRefs } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { useFormDialog, useLoading } from '../composables';

const { runtime } = defineProps<{ runtime: SystemWebRuntime }>();
const ssoApps = runtime.service.ssoApps;
const modal = { msgSuccess: runtime.success };
const { loading, withLoading } = useLoading(true);
const { loading: buttonLoading, withLoading: withButtonLoading } = useLoading();
const appList = ref<ClientVO[]>([]);
const total = ref(0);
const formRef = ref<ElFormInstance>();
const userTypeOptions = ref<UserTypeVO[]>([]);
const secretConfigured = ref(false);
const initFormData: ClientForm = {
  clientKey: undefined,
  clientSecret: undefined,
  grantTypeList: ['password'],
  deviceType: 'pc',
  status: '0',
  ssoEnabled: true,
  ssoAuthMode: 'both',
  ssoClientKind: 'public',
  ssoRedirectUris: undefined,
  ssoPkceRequired: true,
  ssoAutoConsent: true,
  userTypeId: undefined
};
const data = reactive<PageData<ClientForm, ClientQuery>>({
  form: { ...initFormData },
  queryParams: { pageNum: 1, pageSize: 10 },
  rules: {
    clientKey: [{ required: true, message: '应用 key 不能为空', trigger: 'blur' }],
    clientSecret: [{ required: true, message: '登录密钥不能为空', trigger: 'blur' }],
    ssoRedirectUris: [{ required: true, message: '精确回调不能为空', trigger: 'blur' }],
    userTypeId: [{ required: true, message: '登录域不能为空', trigger: 'change' }]
  }
});
const { queryParams, form, rules } = toRefs(data);
const { dialog, resetForm, openDialog, showDialog, closeDialog } = useFormDialog({
  form,
  formRef,
  initialFormData: initFormData
});

const getList = async () => {
  await withLoading(async () => {
    const res = await ssoApps.list(queryParams.value);
    appList.value = res.data?.rows ?? [];
    total.value = res.data?.total ?? 0;
  });
};

const handleAdd = () => {
  secretConfigured.value = false;
  openDialog('创建 SSO 应用');
};

const handleUpdate = async (row: ClientVO) => {
  resetForm();
  const res = await ssoApps.get(row.id);
  Object.assign(form.value, res.data);
  secretConfigured.value = Boolean(res.data?.ssoSecretConfigured);
  showDialog('拿配置');
};

const revealOnce = async (clientId?: string, secret?: string) => {
  const lines = [`clientId: ${clientId ?? ''}`, secret ? `sso_secret（只此一次）: ${secret}` : '密钥：已配置，明文不再回显'];
  await ElMessageBox.alert(lines.join('\n'), '请立即保存配置', { confirmButtonText: '已保存' });
};

const handleRotate = async () => {
  if (!form.value.id) return;
  const res = await ssoApps.rotateSecret(form.value.id);
  secretConfigured.value = true;
  await revealOnce(res.data?.clientId ?? form.value.clientId, res.data?.ssoSecretOnce);
};

const submitForm = () => {
  formRef.value?.validate(async valid => {
    if (!valid) return;
    await withButtonLoading(async () => {
      const saved = form.value.id ? await ssoApps.update(form.value) : await ssoApps.add(form.value);
      await revealOnce(saved.data?.clientId, saved.data?.ssoSecretOnce);
    });
    modal.msgSuccess('已交付配置');
    closeDialog();
    await getList();
  });
};

onMounted(async () => {
  const types = await runtime.service.userTypes.options();
  userTypeOptions.value = types.data ?? [];
  await getList();
});
</script>
<style scoped>
.table-subtitle {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
