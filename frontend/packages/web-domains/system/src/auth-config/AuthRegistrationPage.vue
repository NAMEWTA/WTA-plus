<template>
  <section class="app-container">
    <LegacyAuthImportDialog
      :visible="importVisible"
      :runtime="runtime"
      @close="importVisible = false"
      @imported="load"
    />
    <AuthConnectionDialog :registration-id="connectionId" :runtime="runtime" @close="connectionId = undefined" />
    <el-card>
      <template #header>
        <h2>业务 App 登录接入</h2>
        <p>选择已有身份源，为每个业务 App 配置独立凭据和回调。本地登录继续保留。</p>
      </template>
      <el-space wrap>
        <el-button v-if="runtime.hasPermission('system:authRegistration:add')" type="primary" @click="create">
          新增接入
        </el-button>
        <el-button v-if="runtime.hasPermission('system:authRegistration:edit')" :loading="busy" @click="refresh">
          刷新缓存
        </el-button>
        <el-button @click="load">刷新列表</el-button>
        <el-button
          v-if="
            runtime.hasPermission('system:authProvider:add') && runtime.hasPermission('system:authRegistration:add')
          "
          @click="importVisible = true"
        >
          导入旧配置
        </el-button>
      </el-space>
      <el-form inline @submit.prevent="search">
        <el-form-item label="业务 App">
          <AuthClientSelect v-model="queryClientId" :runtime="runtime" allow-unavailable />
        </el-form-item>
        <el-form-item label="身份源"><AuthProviderSelect v-model="queryProviderId" :runtime="runtime" /></el-form-item>
        <el-button native-type="submit">查询</el-button>
      </el-form>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-table v-loading="loading" :data="rows">
        <el-table-column label="身份源">
          <template #default="{ row }">
            {{ row.providerName || row.providerId }}
          </template>
        </el-table-column>
        <el-table-column label="业务客户端" prop="businessClientId" min-width="170" show-overflow-tooltip />
        <el-table-column label="外部 Client ID" prop="externalClientId" min-width="170" show-overflow-tooltip />
        <el-table-column label="登录回调地址" prop="redirectUri" min-width="230" show-overflow-tooltip />
        <el-table-column label="首次登录" width="145">
          <template #default="{ row }">
            {{ row.firstLoginPolicy === 'AUTO_REGISTER' ? '自动创建账号' : '仅已绑定账号' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230">
          <template #default="{ row }">
            <el-button link type="primary" @click="connectionId = row.id">接入信息</el-button>
            <el-button
              v-if="runtime.hasPermission('system:authRegistration:edit')"
              link
              type="primary"
              @click="editById(row.id)"
            >
              编辑
            </el-button>
            <el-button
              v-if="runtime.hasPermission('system:authRegistration:remove')"
              link
              type="danger"
              @click="removeById(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="20"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </el-card>
    <el-dialog
      v-model="visible"
      :title="draft.id ? '编辑接入' : '新增接入'"
      width="min(720px, 94vw)"
      destroy-on-close
      @closed="close"
    >
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-form ref="form" :model="draft" label-position="top" @submit.prevent="submit">
        <el-form-item label="身份源" prop="providerId" :rules="required">
          <AuthProviderSelect
            v-model="draft.providerId"
            :runtime="runtime"
            :disabled="Boolean(draft.id)"
            @selected="selectedProvider = $event"
          />
        </el-form-item>
        <el-form-item label="业务客户端 ID" prop="businessClientId" :rules="required">
          <AuthClientSelect
            v-model="draft.businessClientId"
            :runtime="runtime"
            :disabled="Boolean(draft.id)"
            @selected="selectedClient = $event"
          />
        </el-form-item>
        <el-form-item label="身份提供方 Client ID" prop="externalClientId" :rules="required">
          <el-input :disabled="Boolean(draft.id)" v-model="draft.externalClientId" autocomplete="off" />
          <small>从身份提供方的应用设置复制，与上方业务 Client ID 不同。</small>
        </el-form-item>
        <el-form-item
          :label="draft.secretConfigured ? '更换客户端密钥（留空保留）' : '客户端密钥'"
          prop="clientSecret"
          :rules="draft.secretConfigured ? [] : required"
        >
          <el-input v-model="draft.clientSecret" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item v-if="isOidc" label="客户端认证方式">
          <el-select v-model="authenticationMethod">
            <el-option :label="`继承身份源（${inheritedMethod}）`" value="inherit" />
            <el-option label="HTTP Basic" value="client_secret_basic" />
            <el-option label="表单 Client Secret" value="client_secret_post" />
          </el-select>
        </el-form-item>
        <el-form-item label="App 公共访问地址">
          <el-input v-model="appPublicUrl" placeholder="https://apps.example.com/home" />
          <el-button :disabled="!appPublicUrl.trim()" @click="generateCallbacks">生成回调地址</el-button>
          <small>包含前端部署路径。点击生成后仍可编辑下方地址。</small>
        </el-form-item>
        <el-form-item label="登录回调地址" prop="redirectUri" :rules="urlRules">
          <el-input v-model="draft.redirectUri" placeholder="https://home.example.com/social-callback" />
        </el-form-item>
        <el-form-item label="退出回调地址" prop="postLogoutRedirectUri">
          <el-input v-model="draft.postLogoutRedirectUri" placeholder="https://home.example.com/logout/callback" />
        </el-form-item>
        <el-form-item v-if="isOidc" label="业务 API 公共地址">
          <el-input v-model="apiPublicBase" placeholder="https://apps.example.com/home/prod-api" />
          <small>填写身份提供方后端可访问的地址，包含 API 代理前缀；保存后可复制后台退出通知地址。</small>
        </el-form-item>
        <el-form-item label="授权范围">
          <el-select v-model="draft.scopes" multiple filterable allow-create default-first-option>
            <el-option v-for="scope in ['openid', 'profile', 'email', 'phone']" :key="scope" :value="scope" />
          </el-select>
        </el-form-item>
        <el-form-item label="首次登录策略">
          <el-radio-group v-model="draft.firstLoginPolicy">
            <el-radio value="BIND_ONLY">仅已绑定账号</el-radio>
            <el-radio value="AUTO_REGISTER">自动创建账号</el-radio>
          </el-radio-group>
          <p v-if="draft.firstLoginPolicy === 'AUTO_REGISTER'">
            缺少手机号时先补填；手机号或邮箱冲突时引导绑定原账号。
          </p>
          <p v-if="draft.firstLoginPolicy === 'AUTO_REGISTER' && selectedClient && !selectedClient.registerEnabled">
            当前业务客户端关闭公开注册，自动建号不会生效；请先检查客户端注册设置。
          </p>
          <small>Admin 使用仅已绑定账号；Home 可按注册开关自动创建账号。</small>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="draft.enabled" /></el-form-item>
        <OidcMetadataPanel
          v-if="isOidc && selectedProvider"
          :runtime="runtime"
          :issuer="selectedProvider.issuer"
          :authentication-method="authenticationMethod === 'inherit' ? inheritedMethod : authenticationMethod"
        />
        <el-collapse>
          <el-collapse-item title="高级扩展参数">
            <el-input v-model="optionsText" type="textarea" :rows="4" aria-label="高级扩展参数JSON" />
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button :disabled="busy" @click="close">取消</el-button>
        <el-button type="primary" :loading="busy" :disabled="!selectedProvider || !selectedClient" @click="submit">
          保存并查看接入信息
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>
<script setup lang="ts">
import type { FormInstance, FormItemRule } from 'element-plus';
import {
  authCallbackUrls,
  normalizeAuthPublicUrl,
  parseAuthOptions,
  oidcAuthenticationMethod,
  type AuthClientOption,
  type AuthProviderOption,
  type AuthRegistrationConfig
} from '@namewta/domain-system';
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import type { SystemWebRuntime } from '../runtime';
import AuthClientSelect from './AuthClientSelect.vue';
import AuthConnectionDialog from './AuthConnectionDialog.vue';
import AuthProviderSelect from './AuthProviderSelect.vue';
import { authConfigError } from './errors';
import LegacyAuthImportDialog from './LegacyAuthImportDialog.vue';
import OidcMetadataPanel from './OidcMetadataPanel.vue';
import { useAuthConfig } from './useAuthConfig';
const importVisible = ref(false);
const { runtime } = defineProps<{ runtime: SystemWebRuntime }>();
const route = useRoute();
const queryClientId = ref(typeof route.query.businessClientId === 'string' ? route.query.businessClientId : '');
const queryProviderId = ref('');
const connectionId = ref<string>();
const selectedProvider = ref<AuthProviderOption>();
const selectedClient = ref<AuthClientOption>();
const appPublicUrl = ref('');
const apiPublicBase = ref('');
const authenticationMethod = ref('inherit');
const isOidc = computed(() => selectedProvider.value?.protocol === 'OIDC');
const inheritedMethod = computed(() => {
  try {
    return oidcAuthenticationMethod(selectedProvider.value?.options ?? {}, {});
  } catch {
    return '身份源配置无效';
  }
});
const empty = (): AuthRegistrationConfig => ({
  providerId: '',
  businessClientId: queryClientId.value,
  externalClientId: '',
  clientSecret: '',
  redirectUri: '',
  postLogoutRedirectUri: '',
  scopes: ['openid', 'profile'],
  firstLoginPolicy: 'BIND_ONLY',
  enabled: false,
  version: 0,
  options: {}
});
const {
  rows,
  total,
  pageNum,
  loading,
  busy,
  error,
  visible,
  draft,
  load,
  search,
  create,
  editById,
  close,
  save,
  removeById,
  refresh
} = useAuthConfig(runtime, runtime.service.authConfig.registrations, 'system:authRegistration', empty, () => ({
  businessClientId: queryClientId.value || undefined,
  providerId: queryProviderId.value || undefined
}));
const form = ref<FormInstance>();
const optionsText = ref('{}');
const required: FormItemRule[] = [{ required: true, message: '请填写此项' }];
const urlRules: FormItemRule[] = [{ required: true, type: 'url', message: '请输入完整回调 URL' }];
watch(visible, value => {
  selectedProvider.value = undefined;
  selectedClient.value = undefined;
  const {
    authenticationMethod: method,
    appPublicUrl: appUrl,
    apiPublicBase: apiUrl,
    ...advanced
  } = value ? draft.value.options : {};
  optionsText.value = JSON.stringify(advanced, null, 2);
  authenticationMethod.value = method || 'inherit';
  appPublicUrl.value = appUrl || '';
  apiPublicBase.value = apiUrl || '';
});
watch(
  () => route.query.businessClientId,
  value => {
    queryClientId.value = typeof value === 'string' ? value : '';
    void search();
  }
);
function generateCallbacks() {
  try {
    Object.assign(draft.value, authCallbackUrls(appPublicUrl.value));
    error.value = '';
  } catch (failure) {
    error.value = authConfigError(failure, 'App 公共访问地址无效');
  }
}
async function submit() {
  if (!(await form.value?.validate().catch(() => false))) return;
  try {
    const options = parseAuthOptions(optionsText.value);
    if (isOidc.value && !draft.value.scopes.includes('openid')) throw new Error('OIDC 授权范围必须包含 openid');
    // 专用表单项优先，避免高级 JSON 与可见字段产生两个配置来源。
    delete options.authenticationMethod;
    delete options.appPublicUrl;
    delete options.apiPublicBase;
    if (isOidc.value && authenticationMethod.value !== 'inherit')
      options.authenticationMethod = authenticationMethod.value;
    if (isOidc.value) oidcAuthenticationMethod(selectedProvider.value?.options ?? {}, options);
    if (appPublicUrl.value.trim()) options.appPublicUrl = normalizeAuthPublicUrl(appPublicUrl.value);
    if (apiPublicBase.value.trim()) options.apiPublicBase = normalizeAuthPublicUrl(apiPublicBase.value);
    draft.value.options = options;
  } catch (failure) {
    error.value = authConfigError(failure, '接入配置无效');
    return;
  }
  const id = await save();
  if (id) connectionId.value = id;
}
</script>
