<template>
  <main class="register ui-auth-page">
    <AuthPanel title="创建账号" description="创建账号，使用统一身份访问工作台。" eyebrow="NAMEWTA">
      <template #header-action><lang-select /></template>
      <el-form
        ref="registerRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            aria-label="用户名"
            type="text"
            size="large"
            autocomplete="off"
            :placeholder="$t('register.username')"
          >
            <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="phoneNumber">
          <el-input
            v-model="registerForm.phoneNumber"
            aria-label="手机号码"
            type="tel"
            name="phoneNumber"
            autocomplete="tel"
            size="large"
            maxlength="11"
            placeholder="手机号码（必填）"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            aria-label="密码"
            type="password"
            size="large"
            autocomplete="off"
            :placeholder="$t('register.password')"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            aria-label="确认密码"
            type="password"
            size="large"
            autocomplete="off"
            :placeholder="$t('register.confirmPassword')"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="captchaEnabled" prop="code" class="captcha-row">
          <el-input
            v-model="registerForm.code"
            aria-label="验证码"
            size="large"
            autocomplete="off"
            :placeholder="$t('register.code')"
          >
            <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
          </el-input>
          <button type="button" class="register-code" aria-label="刷新验证码" @click="getCode">
            <img :src="codeUrl" class="register-code-img" alt="验证码图片" />
          </button>
        </el-form-item>

        <div class="form-meta">
          <span class="register-tip">注册后将返回登录页继续完成认证</span>
          <router-link class="link-type" :to="'/login'">{{ $t('register.switchLoginPage') }}</router-link>
        </div>

        <el-form-item class="submit-row">
          <el-button
            :loading="loading"
            :disabled="!registerEnabled"
            size="large"
            type="primary"
            class="submit-button"
            native-type="submit"
          >
            <span v-if="!loading">{{ $t('register.register') }}</span>
            <span v-else>{{ $t('register.registering') }}</span>
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>NAMEWTA · {{ currentYear }}</template>
    </AuthPanel>
  </main>
</template>
<script setup lang="ts">
import {
  requirePasswordPolicy,
  validatePassword,
  type PasswordPolicy,
  type PasswordViolationReason,
  type RegistrationInput
} from '@namewta/domain-admin';
import { isValidFormat } from '@namewta/platform-validation';
import AuthPanel from '@namewta/web-kit-ui-element/auth-panel';
import { to } from 'await-to-js';
import { useI18n } from 'vue-i18n';
import { identityAccessService } from '@/application/services';

const currentYear = new Date().getFullYear();
const router = useRouter();

const { t } = useI18n();
const passwordPolicy = ref<PasswordPolicy>();

const registerForm = ref<RegistrationInput>({
  username: '',
  phoneNumber: '',
  password: '',
  confirmPassword: '',
  code: '',
  uuid: ''
});

const equalToPassword = (rule: any, value: string, callback: any) => {
  if (registerForm.value.password !== value) {
    callback(new Error(t('register.rule.confirmPassword.equalToPassword')));
  } else {
    callback();
  }
};

const passwordViolationMessage = (reason: PasswordViolationReason) =>
  t(`passwordPolicy.${reason}`, {
    min: passwordPolicy.value?.minimumLength,
    max: passwordPolicy.value?.maximumLength,
    specials: passwordPolicy.value?.allowedSpecialCharacters
  });

const validatePasswordPolicy = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!passwordPolicy.value) {
    callback(new Error(t('passwordPolicy.unavailable')));
    return;
  }
  const violation = validatePassword(passwordPolicy.value, value).at(0);
  callback(violation ? new Error(passwordViolationMessage(violation.reason)) : undefined);
};

const registerRules: ElFormRules = {
  phoneNumber: [
    { required: true, whitespace: true, message: '手机号码不能为空', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) =>
        callback(isValidFormat(value, 'MAINLAND_MOBILE') ? undefined : new Error('请输入正确的手机号码')),
      trigger: ['blur', 'change']
    }
  ],
  username: [
    {
      required: true,
      trigger: 'blur',
      message: t('register.rule.username.required')
    },
    {
      min: 2,
      max: 20,
      message: t('register.rule.username.length', { min: 2, max: 20 }),
      trigger: 'blur'
    }
  ],
  password: [
    {
      required: true,
      trigger: 'blur',
      message: t('register.rule.password.required')
    },
    { validator: validatePasswordPolicy, trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    {
      required: true,
      trigger: 'blur',
      message: t('register.rule.confirmPassword.required')
    },
    { required: true, validator: equalToPassword, trigger: 'blur' }
  ],
  code: [
    {
      required: true,
      trigger: 'change',
      message: t('register.rule.code.required')
    }
  ]
};
const codeUrl = ref('');
const loading = ref(false);
const captchaEnabled = ref(true);
const authContextState = ref<'loading' | 'available' | 'unavailable'>('loading');
const captchaLoading = ref(true);
let captchaGeneration = 0;
let pageActive = true;
const registerEnabled = computed(
  () => authContextState.value === 'available' && !captchaLoading.value && !loading.value
);
const registerRef = ref<ElFormInstance>();

const handleRegister = () => {
  if (!registerEnabled.value) {
    return;
  }
  registerRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true;
      const [err] = await to(identityAccessService.register(registerForm.value));
      if (!pageActive) return;
      if (!err) {
        const username = registerForm.value.username;
        await ElMessageBox.alert(t('register.registerSuccess', { username }), '系统提示', {
          type: 'success'
        });
        if (pageActive) await router.push('/login');
      } else {
        loading.value = false;
        if (captchaEnabled.value) {
          getCode();
        }
      }
    }
  });
};

const getCode = async () => {
  if (!pageActive) return;
  const generation = ++captchaGeneration;
  captchaLoading.value = true;
  registerForm.value.code = '';
  registerForm.value.uuid = '';
  try {
    const verification = await identityAccessService.getVerification();
    if (!pageActive || generation !== captchaGeneration) return;
    captchaEnabled.value = verification.captchaEnabled;
    codeUrl.value = verification.captchaEnabled ? 'data:image/gif;base64,' + verification.img : '';
    registerForm.value.uuid = verification.uuid;
    authContextState.value = 'available';
  } catch {
    if (!pageActive || generation !== captchaGeneration) return;
    authContextState.value = 'unavailable';
    codeUrl.value = '';
    ElMessage.error('验证码获取失败，请刷新重试');
  } finally {
    if (pageActive && generation === captchaGeneration) captchaLoading.value = false;
  }
};

const loadClientAuthContext = async () => {
  authContextState.value = 'loading';
  try {
    const context = await identityAccessService.getClientContext();
    if (!pageActive) return;
    if (!context.clientEnabled || !context.registerEnabled) {
      authContextState.value = 'unavailable';
      ElMessage.warning('当前客户端未开放注册');
      await router.push('/login');
      return;
    }
    passwordPolicy.value = requirePasswordPolicy(context);
    authContextState.value = 'available';
  } catch {
    if (!pageActive) return;
    passwordPolicy.value = undefined;
    authContextState.value = 'unavailable';
    ElMessage.warning(t('passwordPolicy.unavailable'));
    await router.push('/login');
  }
};

onMounted(async () => {
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
.register-form {
  min-width: 0;
}
.register-form .input-icon {
  width: 16px;
  height: 16px;
}
.captcha-row :deep(.el-form-item__content) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px;
  gap: 12px;
}
.register-code {
  min-width: 0;
  height: 42px;
  padding: 0;
  border: 1px solid var(--app-input-border);
  border-radius: var(--app-radius-md);
  overflow: hidden;
  background: var(--app-surface-bg);
  cursor: pointer;
}
.register-code-img {
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
  .register-code {
    width: 112px;
  }
}
</style>
