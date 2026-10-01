<template>
  <section class="identity-login ui-auth-page ui-auth-page--embedded">
    <AuthPanel
      :title="runtime.title ?? '用户登录'"
      :description="runtime.description ?? '使用您的账号，继续访问用户中心。'"
      eyebrow="NAMEWTA"
    >
      <el-form
        class="identity-login__form"
        label-position="top"
        :aria-busy="preparing || submitting"
        @submit.prevent="submit"
      >
        <div v-if="providers.length" class="identity-login__sso">
          <p class="identity-login__social-label">第三方登录</p>
          <div class="identity-login__social-row">
            <el-button
              v-for="provider in providers"
              :key="provider.providerKey"
              native-type="button"
              :disabled="!ready || submitting"
              @click="startSocial(provider.providerKey)"
            >
              <SvgIcon :icon-class="provider.icon || 'tabler:key'" />
              {{ provider.name }}
            </el-button>
          </div>
        </div>
        <el-form-item label="用户名">
          <el-input v-model="form.username" name="username" autocomplete="username" :disabled="!ready || submitting" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            name="password"
            type="password"
            autocomplete="current-password"
            show-password
            :disabled="!ready || submitting"
          />
        </el-form-item>
        <el-form-item v-if="verification?.captchaEnabled" label="验证码">
          <div class="identity-login__captcha">
            <el-input v-model="form.code" name="code" :disabled="!ready || submitting" />
            <img :src="captchaImage" alt="验证码图片" />
            <el-button
              native-type="button"
              aria-label="刷新验证码"
              :loading="preparing"
              :disabled="submitting"
              @click="prepare"
            >
              刷新
            </el-button>
          </div>
        </el-form-item>
        <p v-if="errorMessage" class="identity-login__error" role="alert">{{ errorMessage }}</p>
        <el-button v-if="!ready && !preparing" native-type="button" @click="prepare">重新检查登录入口</el-button>
        <el-button
          class="identity-login__submit"
          type="primary"
          native-type="submit"
          :loading="submitting"
          :disabled="!ready || preparing"
        >
          登录
        </el-button>
        <p class="identity-login__status" aria-live="polite">
          {{ ready ? '入口已就绪' : preparing ? '正在检查入口状态' : '入口暂不可用' }}
        </p>
      </el-form>
    </AuthPanel>
  </section>
</template>

<script setup lang="ts">
import AuthPanel from '@namewta/web-kit-ui-element/auth-panel';
import SvgIcon from '@namewta/web-kit-ui-element/icon';
import { onMounted, onUnmounted } from 'vue';
import type { IdentityAccessWebRuntime } from '../runtime';
import { createIdentityLoginState } from '../loginState';
import { requireIdentityAccessWebRuntime } from '../runtime';

const props = defineProps<{ runtime: IdentityAccessWebRuntime }>();
const runtime = requireIdentityAccessWebRuntime(props.runtime);
const state = createIdentityLoginState(runtime);
const { providers, captchaImage, errorMessage, form, prepare, preparing, ready, submit, submitting, verification } =
  state;

const startSocial = async (providerKey: string) => {
  if (submitting.value) return;
  submitting.value = true;
  try {
    await runtime.startSocialLogin?.(providerKey);
  } catch {
    errorMessage.value = '第三方登录入口暂不可用，请重试';
  } finally {
    submitting.value = false;
  }
};

onMounted(prepare);
onUnmounted(state.dispose);
</script>

<style scoped>
.identity-login__form {
  min-width: 0;
}
.identity-login__social-label {
  margin: 0 0 10px;
  color: var(--app-text-muted);
  font-size: 13px;
}
.identity-login__social-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}
.identity-login__sso-icon {
  width: 1em;
  height: 1em;
  fill: currentColor;
}
.identity-login__submit {
  width: 100%;
  margin-left: 0;
}
.identity-login__captcha {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px auto;
  gap: 8px;
  align-items: center;
}
.identity-login__captcha img {
  width: 112px;
  max-width: 100%;
  height: 38px;
  border: 1px solid var(--app-surface-border);
  object-fit: contain;
}
.identity-login__error {
  color: var(--app-text-danger);
  font-size: 14px;
}
.identity-login__status {
  margin: 16px 0 0;
  color: var(--app-text-muted);
  font-size: 13px;
  text-align: center;
}
@media (max-width: 760px) {
  .identity-login__captcha {
    grid-template-columns: minmax(0, 1fr) auto;
  }
  .identity-login__captcha img {
    grid-column: 1 / -1;
  }
}
</style>
