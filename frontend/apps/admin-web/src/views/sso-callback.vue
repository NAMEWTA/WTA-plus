<template>
  <main class="sso-callback ui-auth-page ui-auth-page--embedded">
    <StatusPanel
      :title="failure ? '登录未完成' : '正在完成 SSO 登录…'"
      :message="failure?.message ?? '请稍候，即将进入应用。'"
      :error="Boolean(failure)"
      :busy="busy"
    >
      <el-button v-if="failure" type="primary" :disabled="busy" @click="restart">
        {{ busy ? '正在重新授权…' : '重新授权' }}
      </el-button>
      <RouterLink v-if="failure" :to="{ path: '/login', query: { redirect: returnTo } }">返回登录页</RouterLink>
    </StatusPanel>
  </main>
</template>
<script setup lang="ts">
import { SsoCallbackError } from '@namewta/platform-auth';
import StatusPanel from '@namewta/web-kit-ui-element/status-panel';
import { ElButton } from 'element-plus';
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { identityAccessService } from '@/application/services';
import { session } from '@/application/session';
import { adminSso, adminSsoRedirectUri } from '@/application/sso';
import { useUserStore } from '@/store/modules/user';

const router = useRouter();
const userStore = useUserStore();
const failure = ref<SsoCallbackError>();
const busy = ref(false);
let active = true;
onBeforeUnmount(() => {
  active = false;
});

function currentSession() {
  const generation = userStore.sessionGeneration;
  const token = session.getToken();
  return () => active && userStore.sessionGeneration === generation && session.getToken() === token;
}
const returnTo = ref('/');

async function restart() {
  if (busy.value) return;
  busy.value = true;
  const isCurrent = currentSession();
  try {
    const context = await identityAccessService.getClientContext();
    if (!isCurrent()) return;
    if (!context.clientEnabled || !context.ssoEnabled || context.authMode === 'local' || !context.ssoAuthorizeUrl)
      throw new SsoCallbackError('exchange');
    await adminSso.startSsoLogin({
      authorizeUrl: context.ssoAuthorizeUrl,
      clientId: import.meta.env.VITE_APP_CLIENT_ID,
      redirectUri: adminSsoRedirectUri(),
      returnTo: returnTo.value
    });
  } catch (error) {
    if (!isCurrent()) return;
    failure.value = error instanceof SsoCallbackError ? error : new SsoCallbackError('network');
  } finally {
    busy.value = false;
  }
}

onMounted(async () => {
  let isCurrent = currentSession();
  const search = window.location.search;
  // 先移除地址栏中的一次性凭据，失败/刷新不会重新提交旧code。
  window.history.replaceState(window.history.state, '', window.location.pathname + window.location.hash);
  try {
    const result = await adminSso.handleCallback(search);
    if (!isCurrent()) return;
    returnTo.value = result.returnTo;
    // 换票结果只属于发起时的页面与会话；新身份必须重新恢复菜单和权限。
    userStore.clearLocalSession();
    session.setToken(result.accessToken);
    userStore.token = result.accessToken;
    isCurrent = currentSession();
    await router.replace(result.returnTo);
  } catch (error) {
    if (!isCurrent()) return;
    failure.value = error instanceof SsoCallbackError ? error : new SsoCallbackError('exchange');
    if (error instanceof SsoCallbackError) returnTo.value = error.returnTo;
  }
});
</script>
