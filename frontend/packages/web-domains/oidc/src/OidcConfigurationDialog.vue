<template>
  <el-dialog
    :model-value="visible"
    title="应用接入配置"
    width="min(700px, 94vw)"
    append-to-body
    :close-on-click-modal="false"
    @update:model-value="
      value => {
        if (!value) emit('close');
      }
    "
  >
    <template v-if="application">
      <el-alert
        v-if="secret"
        title="请保存本次生成的 Client Secret。关闭后无法再次查看，可通过重置获得新密钥。"
        type="success"
        :closable="false"
        show-icon
      />
      <el-form label-position="top" class="configuration-values">
        <el-form-item v-for="item in configuration" :key="item.label" :label="item.label">
          <el-input
            :model-value="item.value"
            readonly
            :type="item.secret ? 'password' : 'text'"
            :show-password="item.secret"
            :autocomplete="item.secret ? 'new-password' : 'off'"
          >
            <template #append>
              <el-button :aria-label="`复制 ${item.label}`" @click="copy(item.value)">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <p class="configuration-hint">
        在第三方选择 OpenID Connect，填写 Discovery、Client ID 和 Client Secret。使用授权码登录；第三方按 iss 与 sub
        识别账户。
      </p>
      <p class="configuration-hint">已登记回调：{{ application.redirectUris.join('；') }}</p>
      <el-alert v-if="copyError" :title="copyError" type="error" :closable="false" />
    </template>
    <template #footer><el-button type="primary" @click="emit('close')">完成</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { applicationScopes, type OidcApplication, type OidcField, type OidcProvider } from '@namewta/domain-oidc';
import { ElAlert, ElButton, ElDialog, ElForm, ElFormItem, ElInput } from 'element-plus';
import { computed, ref, watch } from 'vue';
import type { OidcWebRuntime } from './runtime';

const props = defineProps<{
  visible: boolean;
  application?: OidcApplication;
  provider?: OidcProvider;
  fields: OidcField[];
  secret?: string;
  runtime: OidcWebRuntime;
}>();
const emit = defineEmits<{ close: [] }>();
const copyError = ref('');
watch(
  () => props.visible,
  () => {
    copyError.value = '';
  }
);
const configuration = computed(() => {
  const app = props.application;
  if (!app) return [];
  const values = [
    { label: 'Discovery', value: props.provider?.discoveryUrl ?? '', secret: false },
    { label: 'Issuer', value: props.provider?.issuer ?? '', secret: false },
    { label: 'Client ID', value: app.clientId, secret: false },
    { label: 'Scopes', value: applicationScopes(app, props.fields).join(' '), secret: false },
    { label: '客户端认证方式', value: app.clientAuthenticationMethod, secret: false },
    { label: '后端退出通知地址', value: app.backchannelLogoutUri || '未配置', secret: false }
  ];
  if (props.secret) values.splice(3, 0, { label: 'Client Secret（仅本次显示）', value: props.secret, secret: true });
  return values;
});
async function copy(value: string) {
  try {
    await props.runtime.copyText(value);
    copyError.value = '';
    props.runtime.success('已复制');
  } catch {
    copyError.value = '复制失败，请手动选择并保存内容。';
  }
}
</script>
<style scoped>
.configuration-values {
  margin-top: 20px;
}
.configuration-hint {
  color: var(--el-text-color-secondary);
  overflow-wrap: anywhere;
  line-height: 1.6;
}
</style>
