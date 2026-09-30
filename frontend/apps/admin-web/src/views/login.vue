<template>
  <main class="login ui-auth-page">
    <AuthPanel :title="title || '管理工作台'" description="登录管理工作台，继续处理您的业务。" eyebrow="NAMEWTA">
      <template #header-action><lang-select /></template>
      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form" @submit.prevent="handleLogin">
        <el-form-item v-if="authMode !== 'sso'" prop="username">
          <el-input
            v-model="loginForm.username"
            aria-label="用户名"
            type="text"
            size="large"
            autocomplete="off"
            :placeholder="$t('login.username')"
          >
            <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="authMode !== 'sso'" prop="password">
          <el-input
            v-model="loginForm.password"
            aria-label="密码"
            type="password"
            size="large"
            autocomplete="off"
            :placeholder="$t('login.password')"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="authMode !== 'sso' && captchaEnabled" prop="code" class="captcha-row">
          <el-input
            v-model="loginForm.code"
            aria-label="验证码"
            size="large"
            autocomplete="off"
            :placeholder="$t('login.code')"
          >
            <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
          </el-input>
          <button type="button" class="login-code" aria-label="刷新验证码" @click="getCode">
            <img :src="codeUrl" class="login-code-img" alt="验证码图片" />
          </button>
        </el-form-item>

        <div class="form-meta">
          <el-checkbox v-model="loginForm.rememberMe">{{ $t('login.rememberPassword') }}</el-checkbox>
          <router-link v-if="register" class="link-type" :to="'/register'">
            {{ $t('login.switchRegisterPage') }}
          </router-link>
        </div>

        <div class="social-panel">
          <span class="social-label">第三方登录</span>
          <div class="social-actions">
            <el-button
              v-if="ssoEnabled"
              circle
              data-testid="sso-first-provider"
              :disabled="!loginEnabled"
              title="WTA SSO"
              aria-label="WTA SSO"
              @click="doSsoLogin"
            >
              <svg-icon icon-class="wta" />
            </el-button>
            <el-button
              circle
              :disabled="!loginEnabled"
              :title="$t('login.social.wechat')"
              @click="doSocialLogin('wechat')"
            >
              <svg-icon icon-class="wechat" />
            </el-button>
            <el-button
              circle
              :disabled="!loginEnabled"
              :title="$t('login.social.maxkey')"
              @click="doSocialLogin('maxkey')"
            >
              <svg-icon icon-class="maxkey" />
            </el-button>
            <el-button
              circle
              :disabled="!loginEnabled"
              :title="$t('login.social.topiam')"
              @click="doSocialLogin('topiam')"
            >
              <svg-icon icon-class="topiam" />
            </el-button>
            <el-button
              circle
              :disabled="!loginEnabled"
              :title="$t('login.social.gitee')"
              @click="doSocialLogin('gitee')"
            >
              <svg-icon icon-class="gitee" />
            </el-button>
            <el-button
              circle
              :disabled="!loginEnabled"
              :title="$t('login.social.github')"
              @click="doSocialLogin('github')"
            >
              <svg-icon icon-class="github" />
            </el-button>
          </div>
        </div>

        <el-form-item v-if="authMode !== 'sso'" class="submit-row">
          <el-button
            :loading="loading || authContextState === 'loading'"
            :disabled="!loginEnabled"
            size="large"
            type="primary"
            class="submit-button"
            native-type="submit"
          >
            <span v-if="!loading">{{ $t('login.login') }}</span>
            <span v-else>{{ $t('login.logging') }}</span>
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>NAMEWTA · {{ currentYear }}</template>
    </AuthPanel>
  </main>
</template>
<script setup lang="ts">
import { identityAccessWebMessages } from '@namewta/web-domain-admin';
import AuthPanel from '@namewta/web-kit-ui-element/auth-panel';
import { to } from 'await-to-js';
import { useI18n } from 'vue-i18n';
import { identityAccessService } from '@/application/services';
import { adminSso, adminSsoRedirectUri } from '@/application/sso';
import { type AdminLoginInput, useUserStore } from '@/store/modules/user';

const title = import.meta.env.VITE_APP_TITLE;
const currentYear = new Date().getFullYear();
const userStore = useUserStore();
const router = useRouter();
const { t } = useI18n();

const loginForm = ref<AdminLoginInput>({
  username: 'WTA',
  password: 'admin123',
  rememberMe: false,
  code: '',
  uuid: ''
});

const loginRules: ElFormRules = {
  username: [
    {
      required: true,
      trigger: 'blur',
      message: t('login.rule.username.required')
    }
  ],
  password: [
    {
      required: true,
      trigger: 'blur',
      message: t('login.rule.password.required')
    }
  ],
  code: [
    {
      required: true,
      trigger: 'change',
      message: t('login.rule.code.required')
    }
  ]
};

const codeUrl = ref('');
const loading = ref(false);
const captchaEnabled = ref(true);
const register = ref(false);
const authContextState = ref<'loading' | 'available' | 'unavailable'>('loading');
const captchaLoading = ref(true);
let captchaGeneration = 0;
let pageActive = true;
const loginEnabled = computed(() => authContextState.value === 'available' && !captchaLoading.value && !loading.value);
const ssoEnabled = ref(false);
const ssoAuthorizeUrl = ref('');
const authMode = ref<'local' | 'sso' | 'both'>('both');
const redirect = ref('/');
const loginRef = ref<ElFormInstance>();

watch(
  () => router.currentRoute.value,
  (newRoute: any) => {
    redirect.value = newRoute.query && newRoute.query.redirect && decodeURIComponent(newRoute.query.redirect);
  },
  { immediate: true }
);

const handleLogin = () => {
  if (!loginEnabled.value) {
    return;
  }
  loginRef.value?.validate(async (valid: boolean, fields: any) => {
    if (valid) {
      loading.value = true;
      if (loginForm.value.rememberMe) {
        localStorage.setItem('username', String(loginForm.value.username));
        localStorage.setItem('rememberMe', String(loginForm.value.rememberMe));
      } else {
        localStorage.removeItem('username');
        localStorage.removeItem('rememberMe');
      }
      localStorage.removeItem('password');
      const [err] = await to(userStore.login(loginForm.value));
      if (!pageActive) return;
      if (!err) {
        const redirectUrl = redirect.value || '/';
        await router.push(redirectUrl);
        loading.value = false;
      } else {
        loading.value = false;
        if (captchaEnabled.value) {
          await getCode();
        }
      }
    } else {
      console.log('error submit!', fields);
    }
  });
};

const getCode = async () => {
  if (!pageActive) return;
  const generation = ++captchaGeneration;
  captchaLoading.value = true;
  loginForm.value.code = '';
  loginForm.value.uuid = '';
  try {
    const verification = await identityAccessService.getVerification();
    if (!pageActive || generation !== captchaGeneration) return;
    captchaEnabled.value = verification.captchaEnabled;
    codeUrl.value = verification.captchaEnabled ? 'data:image/gif;base64,' + verification.img : '';
    loginForm.value.uuid = verification.uuid;
    authContextState.value = 'available';
  } catch {
    if (!pageActive || generation !== captchaGeneration) return;
    authContextState.value = 'unavailable';
    codeUrl.value = '';
    register.value = false;
    captchaEnabled.value = false;
    ElMessage.error(identityAccessWebMessages.unavailable);
  } finally {
    if (pageActive && generation === captchaGeneration) captchaLoading.value = false;
  }
};

const getLoginData = () => {
  const username = localStorage.getItem('username');
  const rememberMe = localStorage.getItem('rememberMe');
  localStorage.removeItem('password');
  loginForm.value = {
    username: username === null ? String(loginForm.value.username) : username,
    password: username === null ? String(loginForm.value.password) : '',
    rememberMe: rememberMe === 'true'
  } as AdminLoginInput;
};

const doSsoLogin = async () => {
  if (!loginEnabled.value || !ssoAuthorizeUrl.value) return;
  await adminSso.startSsoLogin({
    authorizeUrl: ssoAuthorizeUrl.value,
    clientId: import.meta.env.VITE_APP_CLIENT_ID,
    redirectUri: adminSsoRedirectUri(),
    returnTo:
      typeof router.currentRoute.value.query.redirect === 'string' ? router.currentRoute.value.query.redirect : '/'
  });
};

const doSocialLogin = async (type: string) => {
  if (!loginEnabled.value) {
    return;
  }
  const res = await identityAccessService.social.bindingUrl(type);
  window.location.href = res.data;
};

const loadClientAuthContext = async () => {
  authContextState.value = 'loading';
  register.value = false;
  try {
    const context = await identityAccessService.getClientContext();
    if (!pageActive) return;
    if (!context.clientEnabled) {
      authContextState.value = 'unavailable';
      ElMessage.error('当前客户端已停用，无法登录');
      return;
    }
    authContextState.value = 'available';
    register.value = context.registerEnabled;
    ssoEnabled.value = context.ssoEnabled === true;
    ssoAuthorizeUrl.value = context.ssoAuthorizeUrl ?? '';
    authMode.value = context.authMode === 'sso' || context.authMode === 'local' ? context.authMode : 'both';
  } catch {
    if (!pageActive) return;
    authContextState.value = 'unavailable';
    register.value = false;
    ElMessage.error(identityAccessWebMessages.unavailable);
  }
};

onMounted(async () => {
  getLoginData();
  await loadClientAuthContext();
  if (pageActive && authContextState.value === 'available') {
    await getCode();
  }
});
onUnmounted(() => {
  pageActive = false;
  captchaGeneration++;
});
</script>
<style lang="scss" scoped>
.login-form {
  min-width: 0;
}
.login-form .input-icon {
  width: 16px;
  height: 16px;
}
.captcha-row :deep(.el-form-item__content) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px;
  gap: 12px;
}
.login-code {
  min-width: 0;
  height: 42px;
  padding: 0;
  border: 1px solid var(--app-input-border);
  border-radius: var(--app-radius-md);
  overflow: hidden;
  background: var(--app-surface-bg);
  cursor: pointer;
}
.login-code-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.form-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}
.link-type {
  color: var(--app-text-accent);
}
.register-tip {
  color: var(--app-text-muted);
  font-size: 13px;
  line-height: 1.6;
}
.social-panel {
  margin-bottom: 24px;
  padding: 16px;
  border: 1px solid var(--app-surface-border);
  border-radius: var(--app-radius-md);
  background: var(--app-elevated-soft-bg);
}
.social-label {
  display: block;
  margin-bottom: 12px;
  color: var(--app-text-muted);
  font-size: 13px;
}
.social-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.social-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.submit-row {
  margin-bottom: 0;
}
.submit-button {
  width: 100%;
}
@media (max-width: 420px) {
  .captcha-row :deep(.el-form-item__content) {
    grid-template-columns: minmax(0, 1fr);
  }
  .login-code {
    width: 112px;
  }
}
</style>
