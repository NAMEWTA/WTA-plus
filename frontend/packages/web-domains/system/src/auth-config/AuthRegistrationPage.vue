<template>
  <section class="app-container">
    <LegacyAuthImportDialog
      :visible="importVisible"
      :runtime="runtime"
      @close="importVisible = false"
      @imported="load"
    />
    <el-card>
      <template #header>
        <h2>第三方登录接入</h2>
        <p>为 Admin、Home 等业务客户端分别设置回调与首次登录策略。</p>
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
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-table v-loading="loading" :data="rows">
        <el-table-column label="身份源">
          <template #default="{ row }">
            {{ providers.find(item => item.id === row.providerId)?.name || row.providerId }}
          </template>
        </el-table-column>
        <el-table-column label="业务客户端" prop="businessClientId" min-width="170" show-overflow-tooltip />
        <el-table-column label="外部 Client ID" prop="externalClientId" min-width="170" show-overflow-tooltip />
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
        <el-table-column label="操作" width="145">
          <template #default="{ row }">
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
          <el-select
            :disabled="Boolean(draft.id)"
            v-model="draft.providerId"
            filterable
            allow-create
            default-first-option
          >
            <el-option v-for="provider in providers" :key="provider.id" :value="provider.id" :label="provider.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务客户端 ID" prop="businessClientId" :rules="required">
          <el-input
            :disabled="Boolean(draft.id)"
            v-model="draft.businessClientId"
            placeholder="系统客户端管理中的 clientId"
          />
        </el-form-item>
        <el-form-item label="外部 OIDC Client ID" prop="externalClientId" :rules="required">
          <el-input :disabled="Boolean(draft.id)" v-model="draft.externalClientId" autocomplete="off" />
        </el-form-item>
        <el-form-item
          :label="draft.secretConfigured ? '更换客户端密钥（留空保留）' : '客户端密钥'"
          prop="clientSecret"
          :rules="draft.secretConfigured ? [] : required"
        >
          <el-input v-model="draft.clientSecret" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="登录回调地址" prop="redirectUri" :rules="urlRules">
          <el-input v-model="draft.redirectUri" placeholder="https://home.example.com/social-callback" />
        </el-form-item>
        <el-form-item label="退出回调地址" prop="postLogoutRedirectUri">
          <el-input v-model="draft.postLogoutRedirectUri" placeholder="https://home.example.com/logout/callback" />
        </el-form-item>
        <el-form-item label="授权范围">
          <el-select v-model="draft.scopes" multiple filterable allow-create default-first-option>
            <el-option v-for="scope in ['openid', 'profile', 'email', 'phone']" :key="scope" :value="scope" />
          </el-select>
        </el-form-item>
        <el-form-item label="首次登录策略">
          <el-radio-group v-model="draft.firstLoginPolicy">
            <el-radio value="BIND_ONLY">仅已绑定账号（Admin）</el-radio>
            <el-radio value="AUTO_REGISTER">自动创建账号（Home）</el-radio>
          </el-radio-group>
          <p v-if="draft.firstLoginPolicy === 'AUTO_REGISTER'">
            缺少手机号时先补填；手机号或邮箱冲突时引导绑定原账号。
          </p>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="draft.enabled" /></el-form-item>
        <el-collapse>
          <el-collapse-item title="高级扩展参数">
            <el-input v-model="optionsText" type="textarea" :rows="4" aria-label="高级扩展参数JSON" />
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button :disabled="busy" @click="close">取消</el-button>
        <el-button type="primary" :loading="busy" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
<script setup lang="ts">
import type { AuthProviderConfig, AuthRegistrationConfig } from '@namewta/domain-system';
import type { FormInstance, FormItemRule } from 'element-plus';
import { ref, watch, onMounted, onBeforeUnmount } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import LegacyAuthImportDialog from './LegacyAuthImportDialog.vue';
import { useAuthConfig } from './useAuthConfig';
const importVisible = ref(false);
const { runtime } = defineProps<{ runtime: SystemWebRuntime }>();
const empty = (): AuthRegistrationConfig => ({
  providerId: '',
  businessClientId: '',
  externalClientId: '',
  clientSecret: '',
  redirectUri: '',
  postLogoutRedirectUri: '',
  scopes: ['openid', 'profile', 'phone'],
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
  create,
  editById,
  close,
  save,
  removeById,
  refresh
} = useAuthConfig(runtime, runtime.service.authConfig.registrations, 'system:authRegistration', empty);
const form = ref<FormInstance>();
const providers = ref<AuthProviderConfig[]>([]);
const optionsText = ref('{}');
const required: FormItemRule[] = [{ required: true, message: '请填写此项' }];
const urlRules: FormItemRule[] = [{ required: true, type: 'url', message: '请输入完整回调 URL' }];
const lifetime = new AbortController();
watch(visible, value => {
  optionsText.value = value ? JSON.stringify(draft.value.options, null, 2) : '{}';
});
onMounted(async () => {
  try {
    const result = await runtime.service.authConfig.providers.list({ pageNum: 1, pageSize: 100 }, lifetime.signal);
    if (!lifetime.signal.aborted) providers.value = result.rows;
  } catch {
    /* 可手动填写已知身份源 ID。 */
  }
});
onBeforeUnmount(() => lifetime.abort());
async function submit() {
  if (!(await form.value?.validate().catch(() => false))) return;
  try {
    const value: unknown = JSON.parse(optionsText.value);
    if (
      !value ||
      typeof value !== 'object' ||
      Array.isArray(value) ||
      Object.values(value).some(item => typeof item !== 'string')
    )
      throw new Error();
    draft.value.options = value as Record<string, string>;
  } catch {
    error.value = '高级扩展参数必须是字符串值的 JSON 对象';
    return;
  }
  await save();
}
</script>
