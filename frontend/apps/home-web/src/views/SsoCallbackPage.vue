<template>
  <main class="sso-callback">
    <h1>{{ failure ? '登录未完成' : '正在完成 SSO 登录…' }}</h1>
    <p v-if="failure" role="alert">{{ failure.message }}</p>
    <button v-if="failure" type="button" :disabled="busy" @click="restart">{{ busy ? '正在重新授权…' : '重新授权' }}</button>
    <RouterLink v-if="failure" :to="{ path: '/login', query: { redirect: returnTo } }">返回登录页</RouterLink>
  </main>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { SsoCallbackError } from '@namewta/platform-auth';
import { homeSso, homeSsoRedirectUri } from '@/application/sso';
import { identityAccessService } from '@/application/services';
import { session } from '@/application/session';
import { useUserStore } from '@/store/user';

const router = useRouter();
const userStore = useUserStore();
const failure = ref<SsoCallbackError>();
const busy = ref(false);
const returnTo = ref('/profile');

async function restart() {
  if (busy.value) return;
  busy.value = true;
  try {
    const context = await identityAccessService.getClientContext();
    if (!context.clientEnabled || !context.ssoEnabled || context.authMode === 'local' || !context.ssoAuthorizeUrl) throw new SsoCallbackError('exchange');
    await homeSso.startSsoLogin({ authorizeUrl: context.ssoAuthorizeUrl, clientId: import.meta.env.VITE_APP_CLIENT_ID,
      redirectUri: homeSsoRedirectUri(), returnTo: returnTo.value });
  } catch (error) {
    failure.value = error instanceof SsoCallbackError ? error : new SsoCallbackError('network');
  } finally { busy.value = false; }
}

onMounted(async () => {
  const search = window.location.search;
  // 先移除地址栏中的一次性凭据，失败/刷新不会重新提交旧code。
  window.history.replaceState(window.history.state, '', window.location.pathname + window.location.hash);
  try {
    const result = await homeSso.handleCallback(search);
    returnTo.value = result.returnTo;
    session.setToken(result.accessToken);
    userStore.token = result.accessToken;
    await router.replace(result.returnTo);
  } catch (error) {
    failure.value = error instanceof SsoCallbackError ? error : new SsoCallbackError('exchange');
    if (error instanceof SsoCallbackError) returnTo.value = error.returnTo;
  }
});
</script>
<style scoped>
.sso-callback {
  min-height: 40vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  text-align: center;
}
button { padding: 10px 20px; cursor: pointer; }
button:focus-visible, a:focus-visible { outline: 2px solid #0284c7; outline-offset: 4px; }
</style>
