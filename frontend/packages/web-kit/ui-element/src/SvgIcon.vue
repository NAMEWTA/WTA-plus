<template>
  <Icon
    v-if="resolved.kind === 'iconify'"
    :icon="resolved.value"
    :class="['svg-icon', className]"
    :style="iconStyle"
    aria-hidden="true"
  />
  <svg v-else :class="['svg-icon', className]" :style="iconStyle" aria-hidden="true">
    <use :href="resolved.value" />
  </svg>
</template>
<script setup lang="ts">
import { Icon } from '@iconify/vue';
import { computed } from 'vue';
import { createIconResolver } from './iconRegistry';
const props = withDefaults(
  defineProps<{
    iconClass?: string;
    className?: string;
    color?: string;
    size?: string;
    localNames?: readonly string[];
  }>(),
  { className: '', color: '', size: '' }
);
const resolver = computed(() =>
  createIconResolver(
    props.localNames,
    import.meta.env.DEV
      ? name => console.warn(`[SvgIcon] icon "${name}" was not found; using tabler:help-circle`)
      : undefined
  )
);
const resolved = computed(() => resolver.value(props.iconClass));
const iconStyle = computed(() => ({ color: props.color || undefined, fontSize: props.size || undefined }));
</script>
<style scoped>
.svg-icon {
  width: 1em;
  height: 1em;
  position: relative;
  fill: currentColor;
  vertical-align: -2px;
  flex-shrink: 0;
}
</style>
