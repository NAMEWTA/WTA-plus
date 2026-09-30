<template>
  <AppShell
    :sidebar-visible="showSidebar"
    :sidebar-opened="sidebar.opened"
    :mobile="device === 'mobile'"
    :without-animation="sidebar.withoutAnimation"
    :fixed-header="fixedHeader"
    :show-tabs="needTagsView"
    :style="{ '--current-color': theme }"
    @close-sidebar="handleClickOutside"
  >
    <template #sidebar><side-bar class="sidebar-container" /></template>
    <template #navbar><navbar @set-layout="setLayout" /></template>
    <template #tabs><tags-view /></template>
    <template #default="{ contentClass }"><app-main :class="contentClass" /></template>
    <template #overlays><settings ref="settingRef" /></template>
  </AppShell>
</template>

<script setup lang="ts">
import AppShell from '@namewta/web-kit-ui-element/app-shell';
import { SHELL_MOBILE_BREAKPOINT } from '@namewta/web-kit-ui-element/shell';
import { NavTypeEnum } from '@/enums/NavTypeEnum';
import { useAppStore } from '@/store/modules/app';
import { useSettingsStore } from '@/store/modules/settings';
import { initMessageBox, initPush } from '@/utils/push';
import { AppMain, Navbar, Settings, TagsView } from './components';
import SideBar from './components/Sidebar/index.vue';

const settingsStore = useSettingsStore();
const theme = computed(() => settingsStore.theme);
const sidebar = computed(() => useAppStore().sidebar);
const device = computed(() => useAppStore().device);
const needTagsView = computed(() => settingsStore.tagsView);
const fixedHeader = computed(() => settingsStore.fixedHeader);
const layout = computed(() => settingsStore.navType);

// 根据布局模式判断是否显示侧边栏
const showSidebar = computed(() => {
  if (sidebar.value.hide) return false;
  return layout.value === NavTypeEnum.LEFT || layout.value === NavTypeEnum.MIX;
});

const { width } = useWindowSize();
const WIDTH = SHELL_MOBILE_BREAKPOINT;

watch(
  width,
  w => {
    if (w - 1 < WIDTH) {
      useAppStore().toggleDevice('mobile');
      useAppStore().closeSideBar({ withoutAnimation: true });
    } else {
      useAppStore().toggleDevice('desktop');
    }
  },
  { immediate: true }
);

const settingRef = ref<InstanceType<typeof Settings>>();

onMounted(async () => {
  try {
    await initMessageBox();
  } finally {
    initPush();
  }
});

const handleClickOutside = () => {
  useAppStore().closeSideBar({ withoutAnimation: false });
};

const setLayout = () => {
  settingRef.value?.openSetting();
};
</script>
