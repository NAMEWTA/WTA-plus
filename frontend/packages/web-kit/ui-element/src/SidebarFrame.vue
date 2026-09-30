<template>
  <div class="sidebar-shell ui-sidebar" :class="`theme-${theme}`">
    <slot name="brand" />
    <slot />
  </div>
</template>
<script setup lang="ts">
withDefaults(defineProps<{ theme?: 'light' | 'dark' }>(), { theme: 'dark' });
</script>
