<template>
  <section class="metadata-panel">
    <el-button :disabled="!issuer.trim() || !canInspect" :loading="loading" @click="inspect">检查 OIDC 连接</el-button>
    <p>从服务端重新读取 Discovery，核对端点和声明的能力；此检查不会登录或创建账号。</p>
    <p>当前接入使用授权码 code、PKCE S256 与 RS256 签名，客户端认证支持 HTTP Basic 或表单 Client Secret。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-if="metadata">
      <el-alert title="已读取元数据；客户端密钥、账号准入和完整登录仍需实际验证。" type="info" :closable="false" />
      <el-alert v-for="warning in warnings" :key="warning" :title="warning" type="warning" :closable="false" />
      <el-descriptions :column="1" border>
        <el-descriptions-item v-for="item in endpoints" :key="item.label" :label="item.label">
          <span class="endpoint">{{ item.value || '未声明' }}</span>
          <el-button v-if="item.value" link :aria-label="`复制 ${item.label}`" @click="copy(item.value)">
            复制
          </el-button>
        </el-descriptions-item>
        <el-descriptions-item label="客户端认证方式">
          {{ declared(metadata.authenticationMethods) }}
        </el-descriptions-item>
        <el-descriptions-item label="授权响应类型">{{ declared(metadata.responseTypes) }}</el-descriptions-item>
        <el-descriptions-item label="PKCE 方法">{{ declared(metadata.pkceMethods) }}</el-descriptions-item>
        <el-descriptions-item label="签名算法">{{ declared(metadata.signingAlgorithms) }}</el-descriptions-item>
        <el-descriptions-item label="授权范围">{{ declared(metadata.scopes) }}</el-descriptions-item>
        <el-descriptions-item label="后台退出">
          {{ metadata.backchannelLogoutSupported ? '已声明支持' : '未声明支持' }}；{{
            metadata.backchannelLogoutSessionSupported ? '已声明支持 sid' : '未声明支持 sid'
          }}
        </el-descriptions-item>
        <el-descriptions-item label="检查时间">{{ metadata.checkedAt }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </section>
</template>
<script setup lang="ts">
import type { OidcMetadataDiagnostic } from '@namewta/domain-system';
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { authConfigError } from './errors';
const {
  runtime,
  issuer,
  authenticationMethod = 'client_secret_basic'
} = defineProps<{ runtime: SystemWebRuntime; issuer: string; authenticationMethod?: string }>();
const metadata = shallowRef<OidcMetadataDiagnostic>();
const loading = ref(false);
const error = ref('');
const canInspect = computed(
  () => runtime.hasPermission('system:authProvider:list') || runtime.hasPermission('system:authRegistration:list')
);
let controller: AbortController | undefined;
let version = 0;
const identity = runtime.sessionSnapshot().generation;
const endpoints = computed(() =>
  metadata.value
    ? [
        { label: 'Issuer', value: metadata.value.issuer },
        { label: 'Discovery', value: metadata.value.discoveryUrl },
        { label: '授权端点', value: metadata.value.authorizationEndpoint },
        { label: 'Token 端点', value: metadata.value.tokenEndpoint },
        { label: 'JWKS', value: metadata.value.jwksUri },
        { label: 'UserInfo', value: metadata.value.userInfoEndpoint },
        { label: '退出端点', value: metadata.value.endSessionEndpoint }
      ]
    : []
);
const declared = (items: string[]) => (items.length ? items.join('、') : '未声明（未知）');
const warnings = computed(() => {
  const value = metadata.value;
  if (!value) return [];
  return [
    value.responseTypes.length && !value.responseTypes.includes('code')
      ? '身份提供方未声明支持授权码 code，本接入需要授权码登录。'
      : '',
    value.pkceMethods.length && !value.pkceMethods.includes('S256')
      ? '身份提供方未声明支持 PKCE S256，本接入固定使用 S256。'
      : '',
    value.authenticationMethods.length && !value.authenticationMethods.includes(authenticationMethod)
      ? `身份提供方未声明支持当前客户端认证方式 ${authenticationMethod}。`
      : '',
    value.signingAlgorithms.length && !value.signingAlgorithms.includes('RS256')
      ? '身份提供方未声明支持 RS256，当前接入仅支持该签名算法。'
      : ''
  ].filter(Boolean) as string[];
});
function invalidate() {
  version++;
  controller?.abort();
  metadata.value = undefined;
  loading.value = false;
  error.value = '';
}
watch(() => issuer, invalidate);
watch(() => runtime.sessionSnapshot().generation, invalidate);
async function inspect() {
  if (!canInspect.value || runtime.sessionSnapshot().generation !== identity) return;
  invalidate();
  const current = version;
  controller = new AbortController();
  loading.value = true;
  try {
    const result = await runtime.service.authConfig.oidcMetadata(issuer.trim(), controller.signal);
    if (current === version) metadata.value = result;
  } catch (failure) {
    if (current === version) error.value = authConfigError(failure, '无法读取 Discovery，请检查 Issuer 与后端网络');
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
onBeforeUnmount(invalidate);
</script>
<style scoped>
.metadata-panel {
  width: 100%;
  margin: 12px 0;
}
.metadata-panel p {
  color: var(--el-text-color-secondary);
}
.endpoint {
  overflow-wrap: anywhere;
}
.metadata-panel :deep(.el-alert) {
  margin-bottom: 12px;
}
</style>
