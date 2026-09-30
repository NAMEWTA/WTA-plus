<template>
  <SidebarFrame
    :class="{ 'has-logo': showLogo }"
    :theme="sideTheme === 'theme-dark' ? 'dark' : 'light'"
    :style="menuStyle"
  >
    <template #brand><logo v-if="showLogo" :collapse="isCollapse" /></template>
    <el-scrollbar :class="sideTheme" wrap-class="scrollbar-wrapper">
      <transition :enter-active-class="animateConfig.menuSearchAnimate.enter" mode="out-in">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :unique-opened="true"
          :collapse-transition="false"
          :popper-offset="12"
          mode="vertical"
        >
          <sidebar-item v-for="(r, index) in sidebarRouters" :key="r.path + index" :item="r" :base-path="r.path" />
        </el-menu>
      </transition>
    </el-scrollbar>
  </SidebarFrame>
</template>

<script setup lang="ts">
import SidebarFrame from '@namewta/web-kit-ui-element/sidebar-frame';
import { RouteRecordRaw } from 'vue-router';
import animateConfig from '@/animate';
import { useAppStore } from '@/store/modules/app';
import { useNavigationStore } from '@/store/modules/navigation';
import { useSettingsStore } from '@/store/modules/settings';
import Logo from './Logo.vue';
import SidebarItem from './SidebarItem.vue';

const route = useRoute();
const appStore = useAppStore();
const settingsStore = useSettingsStore();
const navigationStore = useNavigationStore();
const sidebarRouters = computed<RouteRecordRaw[]>(() => navigationStore.getSidebarRoutes());
const showLogo = computed(() => settingsStore.sidebarLogo);
const sideTheme = computed(() => settingsStore.sideTheme);
const theme = computed(() => settingsStore.theme);
const isCollapse = computed(() => !appStore.sidebar.opened);

const activeMenu = computed(() => {
  const { meta, path } = route;
  // if set path, the sidebar will highlight the path you set
  if (meta.activeMenu) {
    return meta.activeMenu;
  }
  return path;
});

const bgColor = computed(() => (sideTheme.value === 'theme-dark' ? '#111827' : '#ffffff'));
const textColor = computed(() => (sideTheme.value === 'theme-dark' ? '#e5edf8' : '#1f2937'));
const menuStyle = computed(() => ({
  backgroundColor: bgColor.value,
  '--el-menu-bg-color': bgColor.value,
  '--el-menu-text-color': textColor.value,
  '--el-menu-active-color': theme.value
}));
</script>
