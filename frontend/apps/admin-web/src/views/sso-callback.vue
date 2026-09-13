<template>
  <div class="sso-callback">正在完成 SSO 登录…</div>
</template>
<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { adminSso } from '@/application/sso';
import { session } from '@/application/session';
import { useUserStore } from '@/store/modules/user';

const router = useRouter();
const userStore = useUserStore();

onMounted(async () => {
  try {
    const result = await adminSso.handleCallback(window.location.search);
    session.setToken(result.accessToken);
    userStore.token = result.accessToken;
    await router.replace('/');
  } catch {
    await router.replace('/login');
  }
});
</script>
<style scoped>
.sso-callback {
  min-height: 40vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
