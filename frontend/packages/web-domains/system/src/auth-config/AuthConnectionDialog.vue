<template>
  <el-dialog
    :model-value="Boolean(registrationId)"
    title="业务 App 接入信息"
    width="min(760px, 94vw)"
    destroy-on-close
    @close="emit('close')"
  >
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-button v-if="error" :loading="loading" @click="load">重新加载</el-button>
    <div v-loading="loading">
      <template v-if="info">
        <p>{{ info.providerName }} → {{ info.businessClientId }}。将以下回调登记到身份提供方的应用设置。</p>
        <el-space wrap>
          <el-tag :type="info.enabled ? 'success' : 'info'">
            {{ info.enabled ? '登录入口已启用' : '登录入口未启用' }}
          </el-tag>
          <el-tag :type="info.secretConfigured ? 'success' : 'warning'">
            {{ info.secretConfigured ? '密钥已配置（不回显）' : '密钥未配置' }}
          </el-tag>
        </el-space>
        <el-form label-position="top">
          <el-form-item v-for="item in fields" :key="item.label" :label="item.label">
            <el-input :model-value="item.value" readonly placeholder="尚未配置">
              <template #append>
                <el-button :disabled="!item.value" :aria-label="`复制 ${item.label}`" @click="copy(item.value)">
                  复制
                </el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-form>
        <el-alert
          v-if="info.protocol === 'OIDC' && !info.backchannelLogoutUri"
          title="尚未填写业务 API 公共地址。编辑接入并填写含 API 前缀的外部可达地址，即可生成后台退出通知地址。"
          type="info"
          :closable="false"
        />
        <p>
          业务 Client ID 用于本地 App 请求；身份提供方 Client ID 用于 OIDC
          协议，二者不能互换。入口启用不代表实际登录已通过验证。
        </p>
        <p v-if="info.protocol === 'OIDC'">
          如需跨 App 退出，请在身份提供方登记后台退出通知地址，并启用
          backchannel_logout_session_required=true，使退出通知携带 sid。
        </p>
        <p v-if="loginUrl">
          <a :href="loginUrl" target="_blank" rel="noopener noreferrer">打开目标 App 验证登录</a>
          （新窗口）
        </p>
        <p v-else>尚未填写 App 公共访问地址；填写后可从这里打开实际登录页。</p>
        <OidcMetadataPanel
          v-if="info.protocol === 'OIDC'"
          :runtime="runtime"
          :issuer="info.issuer"
          :authentication-method="info.authenticationMethod"
        />
      </template>
    </div>
    <template #footer><el-button @click="emit('close')">关闭</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { normalizeAuthPublicUrl, type AuthConnectionInfo } from '@namewta/domain-system';
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { authConfigError } from './errors';
import OidcMetadataPanel from './OidcMetadataPanel.vue';
const { runtime, registrationId } = defineProps<{ runtime: SystemWebRuntime; registrationId?: string }>();
const emit = defineEmits<{ close: [] }>();
const info = shallowRef<AuthConnectionInfo>();
const error = ref('');
const loading = ref(false);
let controller: AbortController | undefined;
let version = 0;
const identity = runtime.sessionSnapshot().generation;
const loginUrl = computed(() => {
  if (!info.value?.appPublicUrl) return '';
  try {
    return `${normalizeAuthPublicUrl(info.value.appPublicUrl)}/login`;
  } catch {
    return '';
  }
});
const fields = computed(() =>
  info.value
    ? [
        { label: '接入记录 ID', value: info.value.registrationId },
        { label: '业务 Client ID', value: info.value.businessClientId },
        { label: '身份提供方 Client ID', value: info.value.externalClientId },
        ...(info.value.protocol === 'OIDC'
          ? [
              { label: 'Issuer', value: info.value.issuer },
              { label: 'Discovery', value: info.value.discoveryUrl },
              { label: '客户端认证方式', value: info.value.authenticationMethod }
            ]
          : []),
        { label: '登录回调地址', value: info.value.redirectUri },
        { label: '退出回调地址', value: info.value.postLogoutRedirectUri },
        { label: '授权范围', value: info.value.scopes.join(' ') },
        ...(info.value.protocol === 'OIDC'
          ? [{ label: '后台退出通知地址', value: info.value.backchannelLogoutUri }]
          : [])
      ]
    : []
);
function invalidate() {
  version++;
  controller?.abort();
  info.value = undefined;
  loading.value = false;
  error.value = '';
}
async function load() {
  invalidate();
  if (!registrationId || runtime.sessionSnapshot().generation !== identity) return;
  const current = version;
  controller = new AbortController();
  loading.value = true;
  try {
    const result = await runtime.service.authConfig.connectionInfo(registrationId, controller.signal);
    if (current === version) info.value = result;
  } catch (failure) {
    if (current === version) error.value = authConfigError(failure, '接入信息读取失败');
  } finally {
    if (current === version) loading.value = false;
  }
}
async function copy(value: string) {
  try {
    await runtime.copyText(value);
    runtime.success('已复制');
  } catch (failure) {
    error.value = authConfigError(failure, '复制失败，请手动选择内容');
  }
}
watch(() => registrationId, load, { immediate: true });
watch(
  () => runtime.sessionSnapshot().generation,
  () => {
    invalidate();
    emit('close');
  }
);
onBeforeUnmount(invalidate);
</script>
