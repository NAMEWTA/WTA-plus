<template>
  <div
    class="app-wrapper ui-app-shell"
    :class="{ hideSidebar: !sidebarOpened, openSidebar: sidebarOpened, withoutAnimation, mobile }"
  >
    <button
      v-if="mobile && sidebarVisible && sidebarOpened"
      class="drawer-bg"
      type="button"
      aria-label="关闭导航菜单"
      @click="$emit('close-sidebar')"
    />
    <slot v-if="sidebarVisible" name="sidebar" />
    <div class="main-container" :class="{ hasTagsView: showTabs, sidebarHide: !sidebarVisible }">
      <div class="layout-header" :class="{ 'fixed-header': fixedHeader }">
        <slot name="navbar" />
        <slot v-if="showTabs" name="tabs" />
      </div>
      <slot :content-class="{ 'with-fixed-header': fixedHeader, 'with-tags-view': showTabs }" />
      <slot name="overlays" />
    </div>
  </div>
</template>
<script setup lang="ts">
// 布局状态由 App 持有；保留 Admin 页签全屏和表格测量依赖的 DOM 合同。
withDefaults(
  defineProps<{
    sidebarVisible?: boolean;
    sidebarOpened?: boolean;
    mobile?: boolean;
    withoutAnimation?: boolean;
    fixedHeader?: boolean;
    showTabs?: boolean;
  }>(),
  {
    sidebarVisible: true,
    sidebarOpened: true,
    mobile: false,
    withoutAnimation: false,
    fixedHeader: true,
    showTabs: false
  }
);
defineEmits<{ 'close-sidebar': [] }>();
</script>
