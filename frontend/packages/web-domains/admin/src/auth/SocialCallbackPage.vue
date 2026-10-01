<template>
  <main class="ui-auth-page ui-auth-page--embedded">
    <AuthPanel :title="title" description="完成第三方账号登录" eyebrow="NAMEWTA">
      <p v-if="busy" role="status">正在处理，请稍候…</p>
      <p v-if="message" role="alert">{{ message }}</p>
      <el-form v-if="action === 'COMPLETE_PROFILE'" label-position="top" @submit.prevent="register">
        <el-form-item label="手机号码" required>
          <el-input v-model="phone" autocomplete="tel" aria-label="手机号码" :disabled="busy" maxlength="11" />
        </el-form-item>
        <el-button native-type="submit" type="primary" :loading="busy">保存并继续</el-button>
      </el-form>
      <el-button v-if="!busy" @click="runtime.returnToLogin()">
        {{ action === 'BIND_REQUIRED' ? '登录已有账号并绑定' : '返回登录页' }}
      </el-button>
    </AuthPanel>
  </main>
</template>
<script setup lang="ts">
import AuthPanel from '@namewta/web-kit-ui-element/auth-panel';
import { computed, onMounted, onBeforeUnmount, ref } from 'vue';
import type { SocialWebRuntime } from '../socialRuntime';
const { runtime } = defineProps<{ runtime: SocialWebRuntime }>();
const busy = ref(true);
const action = ref('');
const message = ref('');
const phone = ref('');
let active = true;
const title = computed(() =>
  action.value === 'COMPLETE_PROFILE'
    ? '补充手机号码'
    : action.value === 'BIND_REQUIRED'
      ? '绑定已有账号'
      : '第三方登录'
);
function show(result: { nextAction: string; message?: string }) {
  action.value = result.nextAction;
  message.value = result.message ?? '';
}
async function register() {
  if (busy.value) return;
  if (!/^1[3-9]\d{9}$/.test(phone.value.trim())) {
    message.value = '请输入有效的手机号码';
    return;
  }
  busy.value = true;
  message.value = '';
  try {
    const result = await runtime.register(phone.value.trim());
    if (active) show(result);
  } catch (error) {
    if (active) message.value = error instanceof Error ? error.message : '保存失败，请重试';
  } finally {
    if (active) busy.value = false;
  }
}
onMounted(async () => {
  const search = window.location.search;
  window.history.replaceState(window.history.state, '', window.location.pathname);
  try {
    const result = await runtime.callback(search);
    if (active) show(result);
  } catch (error) {
    if (active) message.value = error instanceof Error ? error.message : '登录未完成，请重试';
  } finally {
    if (active) busy.value = false;
  }
});
onBeforeUnmount(() => {
  active = false;
  runtime.dispose();
});
</script>
