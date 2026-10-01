<template>
  <section class="app-container">
    <el-card>
      <template #header>
        <h2>第三方身份源</h2>
        <p>配置外部登录服务，再为各业务客户端建立接入配置。</p>
      </template>
      <el-space wrap>
        <el-button v-if="runtime.hasPermission('system:authProvider:add')" type="primary" @click="create">
          新增身份源
        </el-button>
        <el-button v-if="runtime.hasPermission('system:authProvider:edit')" :loading="busy" @click="refresh">
          刷新缓存
        </el-button>
        <el-button @click="load">刷新列表</el-button>
      </el-space>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-table v-loading="loading" :data="rows">
        <el-table-column label="名称" prop="name" />
        <el-table-column label="标识" prop="providerKey" />
        <el-table-column label="协议" prop="protocol" width="110" />
        <el-table-column label="Issuer" prop="issuer" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button
              v-if="runtime.hasPermission('system:authProvider:edit')"
              link
              type="primary"
              @click="editById(row.id)"
            >
              编辑
            </el-button>
            <el-button
              v-if="runtime.hasPermission('system:authProvider:remove')"
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
      :title="draft.id ? '编辑身份源' : '新增身份源'"
      width="min(640px, 94vw)"
      destroy-on-close
      @closed="close"
    >
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-form ref="form" :model="draft" label-position="top" @submit.prevent="submit">
        <el-form-item
          label="身份源标识"
          prop="providerKey"
          :rules="[{ required: true, message: '请输入稳定的身份源标识' }]"
        >
          <el-input v-model="draft.providerKey" :disabled="Boolean(draft.id)" placeholder="例如 company-sso" />
        </el-form-item>
        <el-form-item label="显示名称" prop="name" :rules="[{ required: true, message: '请输入名称' }]">
          <el-input v-model="draft.name" />
        </el-form-item>
        <el-form-item label="协议">
          <el-select v-model="draft.protocol" filterable allow-create default-first-option>
            <el-option v-for="item in protocols" :key="item" :value="item" :label="item" />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="draft.protocol === 'OIDC'"
          label="Issuer"
          prop="issuer"
          :rules="[{ required: true, type: 'url', message: '请输入外部 OIDC 服务的完整 Issuer 地址' }]"
        >
          <el-input v-model="draft.issuer" placeholder="https://sso.example.com" />
        </el-form-item>
        <el-form-item label="图标"><el-input v-model="draft.icon" placeholder="tabler:key" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="draft.enabled" /></el-form-item>
        <el-collapse>
          <el-collapse-item title="高级扩展参数">
            <el-input v-model="optionsText" type="textarea" :rows="4" aria-label="高级扩展参数JSON" />
            <small>JSON 对象，仅用于协议要求的额外参数；客户端密钥请填写在接入配置中。</small>
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
import type { AuthProviderConfig } from '@namewta/domain-system';
import type { FormInstance } from 'element-plus';
import { ref, watch } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { useAuthConfig } from './useAuthConfig';
const { runtime } = defineProps<{ runtime: SystemWebRuntime }>();
const empty = (): AuthProviderConfig => ({
  providerKey: '',
  name: '',
  icon: 'tabler:key',
  protocol: 'OIDC',
  issuer: '',
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
} = useAuthConfig(runtime, runtime.service.authConfig.providers, 'system:authProvider', empty);
const form = ref<FormInstance>();
const optionsText = ref('{}');
const protocols = ['OIDC', 'GITHUB', 'GITEE', 'WECHAT_OPEN', 'MAXKEY', 'TOPIAM'];
watch(visible, value => {
  optionsText.value = value ? JSON.stringify(draft.value.options, null, 2) : '{}';
});
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
