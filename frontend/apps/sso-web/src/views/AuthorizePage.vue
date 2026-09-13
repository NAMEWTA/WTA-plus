<template>
  <main class="sso-shell">
    <section class="sso-card">
      <p class="eyebrow">FIRST-PARTY SSO</p>
      <h1>WTA SSO</h1>
      <p class="hint">使用本仓账号密码完成统一认人。业务应用不会共用这张会话。</p>
      <p v-if="status" class="status">{{ status }}</p>
      <form v-if="needPassword" class="sso-form" @submit.prevent="submit">
        <label>
          用户名
          <input v-model="username" name="username" autocomplete="username" />
        </label>
        <label>
          密码
          <input v-model="password" name="password" type="password" autocomplete="current-password" />
        </label>
        <button type="submit" :disabled="busy">登录并继续</button>
      </form>
    </section>
  </main>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchSession, loginWithPassword, parseAuthorizeQuery, requestAuthorize } from '../ssoApi';

const route = useRoute();
const username = ref('WTA');
const password = ref('');
const needPassword = ref(false);
const busy = ref(false);
const status = ref('正在检查 SSO 会话…');

const continueAuthorize = async () => {
  const query = parseAuthorizeQuery(route.fullPath.includes('?') ? route.fullPath.slice(route.fullPath.indexOf('?')) : '');
  const result = await requestAuthorize(query);
  if (result.loginRequired) {
    needPassword.value = true;
    status.value = '请输入本仓账号密码';
    return;
  }
  if (result.redirectUri) {
    window.location.assign(result.redirectUri);
    return;
  }
  status.value = '授权未返回回调地址';
};

const submit = async () => {
  busy.value = true;
  try {
    await loginWithPassword(username.value, password.value);
    needPassword.value = false;
    status.value = '登录成功，正在授权…';
    await continueAuthorize();
  } catch (error) {
    status.value = error instanceof Error ? error.message : '登录失败';
  } finally {
    busy.value = false;
  }
};

onMounted(async () => {
  try {
    const signedIn = await fetchSession();
    needPassword.value = !signedIn;
    if (signedIn) {
      status.value = '已有 SSO 会话，正在授权…';
      await continueAuthorize();
    } else {
      status.value = '请输入本仓账号密码';
    }
  } catch {
    needPassword.value = true;
    status.value = '请输入本仓账号密码';
  }
});
</script>
<style scoped>
.sso-shell {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0f172a;
  color: #e2e8f0;
  font-family: sans-serif;
}
.sso-card {
  width: min(420px, 92vw);
  padding: 32px;
  border-radius: 12px;
  background: #111827;
}
.eyebrow {
  letter-spacing: 0.12em;
  font-size: 12px;
  color: #38bdf8;
}
h1 {
  margin: 8px 0 12px;
}
.hint,
.status {
  color: #94a3b8;
  line-height: 1.5;
}
.sso-form {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}
label {
  display: grid;
  gap: 6px;
  font-size: 14px;
}
input,
button {
  min-height: 40px;
  border-radius: 8px;
  border: 1px solid #334155;
  background: #0b1220;
  color: inherit;
  padding: 0 12px;
}
button {
  background: #0284c7;
  border: 0;
  cursor: pointer;
}
</style>
