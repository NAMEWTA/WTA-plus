<template>
  <main class="sso-shell ui-auth-page">
    <AuthPanel class="sso-card" title="统一登录" eyebrow="NAMEWTA">
      <p class="hint">
        使用您的 NAMEWTA 账号，{{ applicationName ? `继续访问 ${applicationName}。` : '安全登录并继续访问应用。' }}
      </p>
      <p v-if="status" class="status" role="status" aria-live="polite">{{ status }}</p>
      <el-button v-if="canRetry && !needPassword" native-type="button" :disabled="busy" @click="checkAndAuthorize">
        重试授权
      </el-button>
      <el-form v-if="needPassword" class="sso-form" label-position="top" :aria-busy="busy" @submit.prevent="submit">
        <el-form-item label="用户名" for="sso-username">
          <el-input id="sso-username" v-model="username" name="username" autocomplete="username" :disabled="busy" />
        </el-form-item>
        <el-form-item label="密码" for="sso-password">
          <el-input
            id="sso-password"
            v-model="password"
            name="password"
            type="password"
            autocomplete="current-password"
            show-password
            :disabled="busy"
          />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="busy" class="sso-submit">登录并继续</el-button>
      </el-form>
    </AuthPanel>
  </main>
</template>
<script setup lang="ts">
import AuthPanel from '@namewta/web-kit-ui-element/auth-panel';
import { ElButton, ElForm, ElFormItem, ElInput } from 'element-plus';
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import {
  fetchOidcContext,
  fetchSession,
  loginWithPassword,
  oidcResumeUrl,
  parseAuthorizeQuery,
  parseOidcRequest,
  requestAuthorize
} from '../ssoApi';

const route = useRoute();
const username = ref('');
const applicationName = ref('');
const password = ref('');
const needPassword = ref(false);
const busy = ref(false);
const canRetry = ref(false);
const status = ref('正在检查 SSO 会话…');
const lifetime = new AbortController();
let live = true;
let oidcRequest: string | undefined;
const search = () => (route.fullPath.includes('?') ? route.fullPath.slice(route.fullPath.indexOf('?')) : '');

const continueAuthorize = async () => {
  if (!live) return;
  if (oidcRequest) {
    window.location.assign(oidcResumeUrl(oidcRequest));
    return;
  }
  const query = parseAuthorizeQuery(search());
  const result = await requestAuthorize(query, lifetime.signal);
  if (!live) return;
  if (result.loginRequired) {
    needPassword.value = true;
    status.value = '请输入您的账号和密码';
    return;
  }
  if (result.redirectUri) {
    window.location.assign(result.redirectUri);
    return;
  }
  status.value = '授权未返回回调地址';
};

const submit = async () => {
  if (busy.value) return;
  busy.value = true;
  canRetry.value = false;
  try {
    await loginWithPassword(username.value, password.value, lifetime.signal);
    if (!live) return;
    needPassword.value = false;
    status.value = '登录成功，正在授权…';
    await continueAuthorize();
  } catch {
    if (!live) return;
    status.value = needPassword.value
      ? '登录失败，请检查用户名和密码后重试。'
      : '授权未完成，请重试或从原应用重新登录。';
    canRetry.value = true;
  } finally {
    password.value = '';
    busy.value = false;
  }
};

async function checkAndAuthorize() {
  if (busy.value) return;
  busy.value = true;
  canRetry.value = false;
  try {
    oidcRequest = parseOidcRequest(search());
    if (oidcRequest) {
      const context = await fetchOidcContext(oidcRequest, lifetime.signal);
      if (!live) return;
      applicationName.value = context.applicationName;
      if (context.forceLogin) {
        needPassword.value = true;
        status.value = '请登录以继续访问应用';
        return;
      }
    }
    const signedIn = await fetchSession(lifetime.signal);
    if (!live) return;
    needPassword.value = !signedIn;
    if (signedIn) {
      status.value = '已有 SSO 会话，正在授权…';
      await continueAuthorize();
    } else {
      status.value = '请输入您的账号和密码';
    }
  } catch {
    if (!live) return;
    needPassword.value = false;
    canRetry.value = true;
    status.value = '无法完成授权，请检查网络后重试，或从原应用重新登录。';
  } finally {
    busy.value = false;
  }
}

onMounted(checkAndAuthorize);
onBeforeUnmount(() => {
  live = false;
  lifetime.abort();
  password.value = '';
});
</script>
<style scoped>
.hint,
.status {
  color: var(--app-text-muted);
  line-height: 1.7;
}
.hint {
  margin: -12px 0 20px;
}
.status {
  margin: 0 0 20px;
}
.sso-form {
  margin-top: 24px;
}
.sso-submit {
  width: 100%;
}
</style>
