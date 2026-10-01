<template>
  <main class="ui-auth-page">
    <StatusPanel
      :title="valid ? '已退出登录' : '退出回调未通过校验'"
      :message="
        valid
          ? '当前应用与统一登录会话已退出。其他应用仅在收到有效的退出通知后退出。'
          : '请返回登录页；此链接已过期或已使用。'
      "
      :error="!valid"
    >
      <el-button @click="runtime.returnToLogin()">返回登录页</el-button>
    </StatusPanel>
  </main>
</template>
<script setup lang="ts">
import StatusPanel from '@namewta/web-kit-ui-element/status-panel';
import { onBeforeUnmount, onMounted, ref } from 'vue';
import type { SocialWebRuntime } from '../socialRuntime';
const { runtime } = defineProps<{ runtime: SocialWebRuntime }>();
const valid = ref(false);
onMounted(() => {
  const search = window.location.search;
  window.history.replaceState(window.history.state, '', window.location.pathname);
  valid.value = runtime.logoutCallback(search);
});
onBeforeUnmount(() => runtime.dispose());
</script>
