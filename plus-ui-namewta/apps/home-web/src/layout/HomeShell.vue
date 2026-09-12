<template>
  <div class="home-shell">
    <header class="home-header">
      <router-link to="/" class="brand"><span class="brand-mark">N</span><span>NAMEWTA</span></router-link>
      <nav><router-link v-if="token && primaryMenu" :to="primaryMenu.path">{{ primaryMenu.meta?.title ?? '用户中心' }}</router-link><template v-else-if="!token"><router-link to="/login">登录</router-link><router-link class="register-link" to="/register">注册</router-link></template><button v-if="token" type="button" @click="logout">退出</button></nav>
    </header>
    <div v-if="token && isUserCenter" class="home-body">
      <aside class="home-sidebar" aria-label="用户中心菜单">
        <router-link v-for="item in navigation.routes.filter(route => !route.hidden)" :key="String(item.name ?? item.path)" :to="item.path">
          {{ item.meta?.title ?? item.name ?? item.path }}
        </router-link>
      </aside>
      <section class="home-content"><router-view /></section>
    </div>
    <router-view v-else />
    <footer>NAMEWTA · 可信身份与档案服务</footer>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store/user';
import { useNavigationStore } from '@/store/navigation';
const router = useRouter(); const user = useUserStore(); const token = computed(() => Boolean(user.token));
const route = useRoute(); const navigation = useNavigationStore(); const primaryMenu = computed(() => navigation.routes.find(item => !item.hidden)); const isUserCenter = computed(() => route.path !== '/' && route.path !== '/login' && route.path !== '/register');
async function logout() { await user.logout(); await router.replace('/'); }
</script>
<style scoped>
.home-shell { min-height: 100vh; color: #172033; background: #f6f8f9; } .home-header { display: flex; align-items: center; justify-content: space-between; max-width: 1180px; margin: 0 auto; padding: 18px 28px; } .brand { display: inline-flex; align-items: center; gap: 10px; color: #172033; font-weight: 800; text-decoration: none; letter-spacing: .08em; } .brand-mark { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 8px; color: #fff; background: #0f766e; } nav { display: flex; align-items: center; gap: 18px; } nav a, nav button { color: #475569; font: inherit; text-decoration: none; background: transparent; border: 0; cursor: pointer; } nav a:hover, nav button:hover { color: #0f766e; } .register-link { padding: 8px 16px; border-radius: 6px; color: #fff; background: #0f766e; } footer { max-width: 1180px; margin: 0 auto; padding: 36px 28px; color: #94a3b8; font-size: 13px; }
.home-body { display: grid; grid-template-columns: 220px minmax(0, 1fr); max-width: 1180px; min-height: calc(100vh - 140px); margin: 0 auto; border-top: 1px solid #dbe4ea; }.home-sidebar { padding: 28px 14px; border-right: 1px solid #dbe4ea; }.home-sidebar a { display: block; padding: 12px 14px; border-radius: 6px; color: #475569; text-decoration: none; }.home-sidebar a.router-link-active { color: #0f766e; background: #e7f3f1; font-weight: 700; }.home-content { min-width: 0; } @media (max-width: 720px) { .home-body { grid-template-columns: 1fr; } .home-sidebar { display: flex; gap: 8px; overflow-x: auto; padding: 14px 18px; border-right: 0; border-bottom: 1px solid #dbe4ea; } .home-sidebar a { white-space: nowrap; } }
</style>
