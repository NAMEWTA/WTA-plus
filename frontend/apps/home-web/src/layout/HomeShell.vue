<template>
  <AuthenticatedShell v-if="token && isUserCenter" />
  <div v-else class="home-shell">
    <header class="home-header">
      <router-link to="/" class="brand">
        <span class="brand-mark">N</span>
        <span>NAMEWTA</span>
      </router-link>
      <nav>
        <router-link v-if="token && primaryMenu" :to="primaryMenu.path">
          {{ primaryMenu.meta?.title ?? '用户中心' }}
        </router-link>
        <template v-else-if="!token">
          <router-link to="/login">登录</router-link>
          <router-link v-if="registration.enabled" class="register-link" to="/register">注册</router-link>
        </template>
        <button v-if="token" type="button" @click="logout">退出</button>
      </nav>
    </header>
    <router-view />
    <footer>NAMEWTA · 可信身份与档案服务</footer>
  </div>
</template>
<script setup lang="ts">
import { computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useNavigationStore } from '@/store/navigation';
import { useRegistrationAvailabilityStore } from '@/store/registrationAvailability';
import { useUserStore } from '@/store/user';
import AuthenticatedShell from './AuthenticatedShell.vue';
const router = useRouter();
const user = useUserStore();
const token = computed(() => Boolean(user.token));
const route = useRoute();
const navigation = useNavigationStore();
const primaryMenu = computed(() => navigation.routes.find(item => !item.hidden));
const isUserCenter = computed(
  () =>
    route.path !== '/' &&
    route.path !== '/login' &&
    route.path !== '/register' &&
    !['/sso/callback', '/social-callback', '/logout/callback'].includes(route.path)
);
const registration = useRegistrationAvailabilityStore();
watch(
  [() => route.path, token],
  ([path, authenticated]) => {
    if (authenticated) registration.reset();
    else if (path !== '/register') void registration.load();
  },
  { immediate: true }
);
async function logout() {
  try {
    await user.logout();
  } catch {
    /* Remote failure is reported by the HTTP boundary. */
  } finally {
    await router.replace('/');
  }
}
</script>
<style scoped>
.home-shell {
  min-height: 100vh;
  color: var(--app-text-title);
  background: var(--app-shell-bg);
}
.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1180px;
  margin: 0 auto;
  padding: 18px 28px;
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--app-text-title);
  font-weight: 800;
  text-decoration: none;
  letter-spacing: 0.08em;
}
.brand-mark {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: var(--app-radius-lg);
  color: #fff;
  background: var(--app-action-primary);
}
nav {
  display: flex;
  align-items: center;
  gap: 18px;
}
nav a,
nav button {
  color: var(--app-text-muted);
  font: inherit;
  text-decoration: none;
  background: transparent;
  border: 0;
  cursor: pointer;
}
nav a:hover,
nav button:hover {
  color: var(--app-text-accent);
}
.register-link {
  padding: 8px 16px;
  border-radius: var(--app-radius-md);
  color: #fff;
  background: var(--app-action-primary);
}
footer {
  max-width: 1180px;
  margin: 0 auto;
  padding: 36px 28px;
  color: var(--app-text-muted);
  font-size: 13px;
}
.home-body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  max-width: 1180px;
  min-height: calc(100vh - 140px);
  margin: 0 auto;
  border-top: 1px solid var(--app-surface-border);
}
.home-sidebar {
  padding: 28px 14px;
  border-right: 1px solid var(--app-surface-border);
}
.home-sidebar a {
  display: block;
  padding: 12px 14px;
  border-radius: var(--app-radius-md);
  color: var(--app-text-muted);
  text-decoration: none;
}
.home-sidebar a.router-link-active {
  color: var(--app-text-accent);
  background: var(--app-accent-soft);
  font-weight: 700;
}
.home-content {
  min-width: 0;
}
@media (max-width: 720px) {
  .home-body {
    grid-template-columns: 1fr;
  }
  .home-sidebar {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    padding: 14px 18px;
    border-right: 0;
    border-bottom: 1px solid var(--app-surface-border);
  }
  .home-sidebar a {
    white-space: nowrap;
  }
}
@media (max-width: 420px) {
  .home-header {
    flex-wrap: wrap;
    justify-content: center;
    gap: 14px;
    padding: 18px 12px;
  }
  .brand {
    max-width: 100%;
    flex-wrap: wrap;
    justify-content: center;
  }
  nav {
    flex-wrap: wrap;
    justify-content: center;
  }
  footer {
    padding: 28px 12px;
    overflow-wrap: anywhere;
  }
}
</style>
