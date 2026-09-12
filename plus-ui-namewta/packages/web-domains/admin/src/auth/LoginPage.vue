<template>
  <section class="identity-login" aria-labelledby="identity-login-title">
    <div class="identity-login__intro">
      <p class="identity-login__eyebrow">CLIENT WORKSPACE</p>
      <h1 id="identity-login-title">{{ runtime.title ?? '客户服务入口' }}</h1>
      <p>{{ runtime.description ?? '登录客户工作台，查看服务资料并处理示例业务。' }}</p>
    </div>

    <el-form class="identity-login__form" label-position="top" @submit.prevent="submit">
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
  </section>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import type { IdentityAccessWebRuntime } from '../runtime';
import { createIdentityLoginState } from '../loginState';
import { requireIdentityAccessWebRuntime } from '../runtime';

const props = defineProps<{ runtime: IdentityAccessWebRuntime }>();
const runtime = requireIdentityAccessWebRuntime(props.runtime);
const state = createIdentityLoginState(runtime);
const { captchaImage, errorMessage, form, prepare, preparing, ready, submit, submitting, verification } = state;

onMounted(prepare);
onUnmounted(state.dispose);
</script>

<style scoped>
.identity-login {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 390px);
  gap: 48px;
  align-items: center;
  min-height: calc(100vh - 164px);
}

.identity-login__intro h1 {
  margin: 10px 0 14px;
  color: var(--client-text);
  font-size: 46px;
  line-height: 1.12;
}

.identity-login__intro p:last-child {
  max-width: 560px;
  color: var(--client-text-muted);
  line-height: 1.8;
}

.identity-login__eyebrow {
  margin: 0;
  color: var(--client-accent-strong);
  font-size: 12px;
  font-weight: 700;
}

.identity-login__form {
  padding: 30px;
  border: 1px solid var(--client-line);
  border-radius: var(--client-radius);
  background: var(--client-surface);
  box-shadow: var(--client-shadow);
}

.identity-login__submit {
  width: 100%;
  min-height: 42px;
}

.identity-login__captcha {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px auto;
  gap: 8px;
  align-items: center;
}

.identity-login__captcha img {
  width: 112px;
  height: 38px;
  border: 1px solid var(--client-line);
  object-fit: contain;
}

.identity-login__error {
  color: #b42318;
  font-size: 14px;
}

.identity-login__status {
  margin: 16px 0 0;
  color: var(--client-text-muted);
  font-size: 13px;
  text-align: center;
}

@media (max-width: 760px) {
  .identity-login {
    grid-template-columns: 1fr;
    gap: 28px;
    min-height: auto;
  }

  .identity-login__intro h1 {
    font-size: 36px;
  }

  .identity-login__captcha {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .identity-login__captcha img {
    grid-column: 1 / -1;
  }
}
</style>
