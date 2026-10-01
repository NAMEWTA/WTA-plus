<template>
  <el-dialog
    :model-value="visible"
    :title="application ? '编辑应用' : '创建应用'"
    width="min(760px, 94vw)"
    append-to-body
    :close-on-click-modal="false"
    @update:model-value="close"
  >
    <el-form ref="formRef" :model="form" label-position="top" @submit.prevent="submit">
      <el-form-item
        label="应用名称"
        prop="name"
        :rules="[{ required: true, whitespace: true, message: '请输入应用名称', trigger: 'blur' }]"
      >
        <el-input v-model="form.name" maxlength="128" placeholder="例如：项目协作平台" />
      </el-form-item>
      <el-form-item
        label="登录回调地址"
        prop="redirectText"
        :rules="[{ required: true, whitespace: true, message: '请输入完整回调地址', trigger: 'blur' }]"
      >
        <el-input
          v-model="form.redirectText"
          type="textarea"
          :rows="3"
          placeholder="第三方系统提供的完整 HTTPS 回调地址，每行一个"
        />
        <p class="form-hint">回调地址必须精确匹配，不支持通配符。</p>
      </el-form-item>
      <el-collapse>
        <el-collapse-item title="可获取的用户信息" name="fields">
          <DisclosureFields v-model="form.allowedFields" :fields="fields" />
        </el-collapse-item>
        <el-collapse-item title="高级接入设置" name="advanced">
          <el-form-item label="退出完成地址">
            <el-input v-model="form.logoutText" type="textarea" :rows="2" placeholder="可选，每行一个完整地址" />
          </el-form-item>
          <el-form-item label="后端退出通知地址">
            <el-input v-model="form.backchannelLogoutUri" placeholder="第三方业务后端接收 logout_token 的完整地址" />
          </el-form-item>
          <el-checkbox v-model="form.backchannelLogoutSessionRequired">退出通知包含会话标识 sid</el-checkbox>
          <el-form-item label="客户端认证方式">
            <el-select v-model="form.clientAuthenticationMethod">
              <el-option label="HTTP Basic（推荐）" value="client_secret_basic" />
              <el-option label="表单 Client Secret" value="client_secret_post" />
            </el-select>
          </el-form-item>
          <el-checkbox v-model="form.pkceRequired">要求 PKCE S256</el-checkbox>
          <p class="form-hint">仅在确认第三方后端不支持 PKCE 时关闭；客户端仍必须使用密钥认证。</p>
        </el-collapse-item>
      </el-collapse>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="form-error" />
    </el-form>
    <template #footer>
      <el-button :disabled="busy" @click="close(false)">取消</el-button>
      <el-button type="primary" :loading="busy" @click="submit">
        {{ application ? '保存' : '创建并获取配置' }}
      </el-button>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { redirectLines, type OidcApplication, type OidcApplicationInput, type OidcField } from '@namewta/domain-oidc';
import {
  ElAlert,
  ElButton,
  ElCheckbox,
  ElCollapse,
  ElCollapseItem,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  type FormInstance
} from 'element-plus';
import { reactive, ref, watch } from 'vue';
import DisclosureFields from './DisclosureFields.vue';

const props = defineProps<{
  visible: boolean;
  application?: OidcApplication;
  fields: OidcField[];
  busy: boolean;
  error: string;
}>();
const emit = defineEmits<{ close: []; save: [value: OidcApplicationInput] }>();
const formRef = ref<FormInstance>();
const form = reactive({
  name: '',
  redirectText: '',
  logoutText: '',
  backchannelLogoutUri: '',
  backchannelLogoutSessionRequired: true,
  allowedFields: [] as string[],
  clientAuthenticationMethod: 'client_secret_basic' as OidcApplicationInput['clientAuthenticationMethod'],
  pkceRequired: true
});
watch(
  () => props.visible,
  visible => {
    if (!visible) return;
    const app = props.application;
    Object.assign(form, {
      name: app?.name ?? '',
      redirectText: app?.redirectUris.join('\n') ?? '',
      logoutText: app?.postLogoutRedirectUris.join('\n') ?? '',
      backchannelLogoutUri: app?.backchannelLogoutUri ?? '',
      backchannelLogoutSessionRequired: app?.backchannelLogoutSessionRequired ?? true,
      allowedFields: app
        ? [...app.allowedFields]
        : props.fields.filter(field => field.defaultEnabled).map(field => field.key),
      clientAuthenticationMethod: app?.clientAuthenticationMethod ?? 'client_secret_basic',
      pkceRequired: app?.pkceRequired ?? true
    });
    formRef.value?.clearValidate();
  }
);
function close(visible: boolean) {
  if (!visible && !props.busy) emit('close');
}
async function submit() {
  if (props.busy || !(await formRef.value?.validate().catch(() => false))) return;
  emit('save', {
    name: form.name.trim(),
    redirectUris: redirectLines(form.redirectText),
    postLogoutRedirectUris: redirectLines(form.logoutText),
    backchannelLogoutUri: form.backchannelLogoutUri.trim(),
    backchannelLogoutSessionRequired: form.backchannelLogoutSessionRequired,
    allowedFields: [...form.allowedFields],
    clientAuthenticationMethod: form.clientAuthenticationMethod,
    pkceRequired: form.pkceRequired
  });
}
</script>
<style scoped>
.form-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 6px 0 0;
  line-height: 1.5;
}
.form-error {
  margin-top: 16px;
}
</style>
