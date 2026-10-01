<template>
  <AppShell :sidebar-opened="opened" :mobile="mobile" @close-sidebar="opened = false">
    <template #sidebar>
      <SidebarFrame class="sidebar-container" theme="dark">
        <template #brand>
          <div class="sidebar-logo-container">
            <router-link class="sidebar-logo-link" to="/profile">
              <SvgIcon icon-class="tabler:hexagon-letter-n" size="28px" />
              <span v-if="opened || mobile" class="sidebar-title">NAMEWTA</span>
            </router-link>
          </div>
        </template>
        <el-scrollbar class="theme-dark" wrap-class="scrollbar-wrapper">
          <el-menu
            :default-active="activeMenu"
            :collapse="!opened && !mobile"
            :collapse-transition="false"
            :unique-opened="true"
            @select="navigate"
          >
            <HomeMenuItem v-for="item in navigation.routes" :key="item.path" :item="item" :base-path="item.path" />
          </el-menu>
        </el-scrollbar>
      </SidebarFrame>
    </template>
    <template #navbar>
      <TopbarFrame>
        <template #leading>
          <button
            type="button"
            class="hamburger-shell"
            :aria-expanded="opened"
            aria-label="展开或收起导航菜单"
            @click="opened = !opened"
          >
            <SvgIcon
              :icon-class="opened ? 'tabler:layout-sidebar-left-collapse' : 'tabler:layout-sidebar-left-expand'"
              size="20px"
            />
          </button>
        </template>
        <template #context>
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.path !== '/profile'">
              {{ route.meta.title || '用户服务' }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </template>
        <template #actions>
          <div class="avatar-container">
            <el-dropdown trigger="click" @command="accountAction">
              <button class="avatar-wrapper" type="button" aria-label="账户菜单">
                <span class="user-avatar avatar-placeholder"><SvgIcon icon-class="tabler:user" /></span>
                <span class="avatar-meta">
                  <span class="avatar-name">{{ user.nickname || '用户' }}</span>
                  <span class="avatar-role">个人中心</span>
                </span>
                <SvgIcon icon-class="tabler:chevron-down" />
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="bindings">账号绑定</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出当前应用</el-dropdown-item>
                  <el-dropdown-item v-if="user.globalLogoutAvailable" command="globalLogout">
                    退出全部应用
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </TopbarFrame>
    </template>
    <template #default="{ contentClass }">
      <section class="app-main" :class="contentClass"><router-view /></section>
    </template>
  </AppShell>
</template>
<script setup lang="ts">
import AppShell from '@namewta/web-kit-ui-element/app-shell';
import SvgIcon from '@namewta/web-kit-ui-element/icon';
import { SHELL_MOBILE_BREAKPOINT } from '@namewta/web-kit-ui-element/shell';
import SidebarFrame from '@namewta/web-kit-ui-element/sidebar-frame';
import TopbarFrame from '@namewta/web-kit-ui-element/topbar-frame';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createAppSocialRuntime } from '@/application/social';
import { useNavigationStore } from '@/store/navigation';
import { useUserStore } from '@/store/user';
import HomeMenuItem from './HomeMenuItem.vue';
const route = useRoute();
const router = useRouter();
const user = useUserStore();
const navigation = useNavigationStore();
const mobile = ref(false);
const opened = ref(true);
const activeMenu = computed(() =>
  String(route.meta.activeMenu || (route.path.startsWith('/profile/') ? '/profile' : route.path))
);
let media: MediaQueryList | undefined;
function resize() {
  mobile.value = media?.matches ?? false;
  if (mobile.value) opened.value = false;
}
onMounted(() => {
  media = window.matchMedia(`(max-width: ${SHELL_MOBILE_BREAKPOINT - 1}px)`);
  resize();
  media.addEventListener('change', resize);
});
onBeforeUnmount(() => media?.removeEventListener('change', resize));
async function navigate(path: string) {
  await router.push(path);
  if (mobile.value) opened.value = false;
}
async function accountAction(command: string) {
  if (command === 'profile') return navigate('/profile');
  if (command === 'bindings') return navigate('/account/bindings');
  if (command === 'globalLogout') {
    try {
      await ElMessageBox.confirm('将退出当前应用与统一登录会话，其他已接入应用会同步退出。', '退出全部应用', {
        confirmButtonText: '确认退出',
        cancelButtonText: '取消'
      });
    } catch {
      return;
    }
    try {
      await createAppSocialRuntime().globalLogout();
    } catch {
      ElMessage.error('统一退出未完成，请重试');
    }
    return;
  }
  if (command !== 'logout') return;
  try {
    await user.logout();
  } catch {
    /* 请求边界负责反馈，本地会话仍完成清理。 */
  } finally {
    await router.replace('/');
  }
}
</script>
