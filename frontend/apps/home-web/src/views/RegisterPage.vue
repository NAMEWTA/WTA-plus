<template>
  <main class="register-page">
    <div class="register-intro">
      <span class="eyebrow">CREATE ACCOUNT</span>
      <h1>创建用户账号</h1>
      <p>注册后进入用户中心，完成个人或企业认证。</p>
    </div>
    <el-form class="register-form" label-position="top" :aria-busy="preparing || submitting" @submit.prevent="submit">
      <el-form-item label="用户名" for="register-username">
        <el-input id="register-username" v-model="form.username" name="username" autocomplete="username" :disabled="!ready || submitting" />
      </el-form-item>
      <el-form-item label="密码" for="register-password">
        <el-input id="register-password" v-model="form.password" name="password" type="password" show-password autocomplete="new-password" :disabled="!ready || submitting" />
      </el-form-item>
      <el-form-item label="确认密码" for="register-confirm">
        <el-input id="register-confirm" v-model="form.confirmPassword" name="confirmPassword" type="password" show-password autocomplete="new-password" :disabled="!ready || submitting" />
      </el-form-item>
      <el-form-item v-if="captchaEnabled" label="验证码" for="register-code">
        <el-input id="register-code" v-model="form.code" name="code" autocomplete="off" :disabled="!ready || submitting" />
        <div class="captcha">
          <img v-if="captchaImage" :src="captchaImage" alt="验证码图片" />
          <el-button native-type="button" aria-label="刷新验证码" :aria-busy="preparing" :disabled="submitting" @click="refreshCaptcha()">刷新验证码</el-button>
        </div>
      </el-form-item>
      <p v-if="errorMessage" class="error" role="alert" aria-live="polite">{{ errorMessage }}</p>
      <p class="status" aria-live="polite">{{ preparing ? '正在检查注册入口' : ready ? '注册入口已就绪' : '注册入口暂不可用' }}</p>
      <el-button v-if="!ready && !preparing && !submitting" native-type="button" @click="prepare()">重新检查注册入口</el-button>
      <el-button type="primary" native-type="submit" :loading="submitting" :disabled="!ready || preparing" class="submit">注册</el-button>
      <el-button native-type="button" class="back" @click="cancel">取消注册，返回登录</el-button>
    </el-form>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { IdentityAccessError, type LoginVerification } from '@namewta/domain-admin';
import { identityAccessService } from '@/application/services';
import { useRegistrationAvailabilityStore } from '@/store/registrationAvailability';

const router = useRouter();
const availability = useRegistrationAvailabilityStore();
const preparing = ref(false);
const submitting = ref(false);
const ready = ref(false);
const errorMessage = ref('');
const captchaEnabled = ref(false);
const verification = ref<LoginVerification>();
const form = reactive({ username: '', password: '', confirmPassword: '', code: '' });
const captchaImage = computed(() => verification.value?.img ? `data:image/gif;base64,${verification.value.img}` : '');
let generation = 0;
let active = true;
const current = (value: number) => active && value === generation;
const messageFor = (error: unknown) => error instanceof Error && error.message ? error.message : '注册请求失败，请稍后重试';
const clearSecrets = () => { form.password = ''; form.confirmPassword = ''; form.code = ''; };

async function prepare(preserveError = false) {
  const attempt = ++generation;
  ready.value = false;
  preparing.value = true;
  verification.value = undefined;
  form.code = '';
  if (!preserveError) errorMessage.value = '';
  availability.reset();
  try {
    const result = await identityAccessService.prepareLogin();
    if (!current(attempt)) return;
    availability.accept(result.context);
    if (!availability.enabled) { errorMessage.value = '当前客户端未开放注册'; return; }
    verification.value = result.verification;
    captchaEnabled.value = result.verification.captchaEnabled;
    ready.value = true;
  } catch (error) {
    if (current(attempt)) errorMessage.value = messageFor(error);
  } finally {
    if (current(attempt)) preparing.value = false;
  }
}

async function refreshCaptcha(preserveError = false) {
  if (submitting.value || !availability.enabled) return;
  const attempt = ++generation;
  ready.value = false;
  preparing.value = true;
  verification.value = undefined;
  form.code = '';
  if (!preserveError) errorMessage.value = '';
  try {
    const next = await identityAccessService.getVerification();
    if (!current(attempt)) return;
    verification.value = next;
    captchaEnabled.value = next.captchaEnabled;
    ready.value = true;
  } catch (error) {
    if (current(attempt)) errorMessage.value = messageFor(error);
  } finally {
    if (current(attempt)) preparing.value = false;
  }
}

async function submit() {
  if (!ready.value || preparing.value || submitting.value) return;
  const attempt = ++generation;
  submitting.value = true;
  errorMessage.value = '';
  let refresh = false;
  try {
    await identityAccessService.register({ ...form, ...(verification.value?.uuid ? { uuid: verification.value.uuid } : {}) });
    if (!current(attempt)) return;
    clearSecrets();
    await router.replace('/login');
  } catch (error) {
    if (!current(attempt)) return;
    errorMessage.value = messageFor(error);
    // 本地校验未发送请求；远端拒绝或网络结果不明时，验证码可能已被消费。
    if (!(error instanceof IdentityAccessError)) {
      clearSecrets();
      refresh = true;
    }
  } finally {
    if (current(attempt)) submitting.value = false;
  }
  if (refresh && current(attempt)) await prepare(true);
}

async function cancel() {
  generation++;
  ready.value = false;
  submitting.value = false;
  clearSecrets();
  await router.replace('/login');
}
onMounted(prepare);
onUnmounted(() => { active = false; generation++; clearSecrets(); });
</script>

<style scoped>
.register-page { display: grid; grid-template-columns: minmax(0, 1fr) 380px; gap: 64px; max-width: 920px; margin: 0 auto; padding: 86px 28px; align-items: center; }
.eyebrow { color: #0f766e; font-size: 12px; font-weight: 700; letter-spacing: .14em; }
.register-intro h1 { margin: 12px 0; font-size: 42px; }
.register-intro p { color: #475569; line-height: 1.7; }
.register-form { --el-border-color: #7b8794; min-width: 0; padding: clamp(12px, 4vw, 28px); border: 1px solid #dbe4ea; border-radius: 8px; background: #fff; }
.captcha { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-top: 8px; }
.captcha img { width: 120px; max-width: 100%; height: 38px; object-fit: contain; }
.register-form :deep(.el-button) { max-width: 100%; height: auto; min-height: 32px; white-space: normal; line-height: 1.5; }
.submit { width: 100%; margin: 12px 0 0; }
.back { display: block; margin: 16px auto 0; color: #0f766e; }
.error { color: #b42318; font-size: 13px; }
.status { color: #64748b; font-size: 13px; }
@media (max-width: 700px) { .register-page { grid-template-columns: minmax(0, 1fr); gap: 28px; padding: 44px 18px; } }
</style>
