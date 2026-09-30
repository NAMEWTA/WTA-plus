<template>
  <SharedIcon
    :icon-class="iconClass"
    :class-name="className"
    :color="color"
    :size="size"
    :local-names="localIconNames"
  />
</template>
<script setup lang="ts">
import SharedIcon from '@namewta/web-kit-ui-element/icon';
import localIconNames from '@/components/IconSelect/requireIcons';
withDefaults(defineProps<{ iconClass: string; className?: string; color?: string; size?: string }>(), {
  className: '',
  color: '',
  size: ''
});
</script>
