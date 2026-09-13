<template>
  <main class="sso-callback">正在完成 SSO 登录…</main>
</template>
<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { homeSso } from '@/application/sso';
import { session } from '@/application/session';

const router = useRouter();

onMounted(async () => {
  try {
    const result = await homeSso.handleCallback(window.location.search);
    session.setToken(result.accessToken);
    await router.replace('/profile');
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
