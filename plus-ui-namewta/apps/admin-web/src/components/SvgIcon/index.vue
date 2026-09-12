<template>
  <Icon
    v-if="resolvedIcon.kind === 'iconify'"
    :icon="resolvedIcon.value"
    :class="svgClass"
    :style="iconStyle"
    aria-hidden="true"
  />
  <svg v-else :class="svgClass" aria-hidden="true" :style="iconStyle">
    <use :xlink:href="resolvedIcon.value" :fill="color" />
  </svg>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue';
import { propTypes } from '@/utils/propTypes';
import { resolveIcon } from './iconRegistry';

const props = defineProps({
  iconClass: propTypes.string.isRequired,
  className: propTypes.string.def(''),
  color: propTypes.string.def(''),
  size: propTypes.string.def('')
});
const resolvedIcon = computed(() => resolveIcon(props.iconClass));
const svgClass = computed(() => {
  if (props.className) {
    return `svg-icon ${props.className}`;
  }
  return 'svg-icon';
});
const iconStyle = computed(() => ({
  color: props.color || undefined,
  fontSize: props.size || undefined
}));
</script>

<style lang="scss" scoped>
.sub-el-icon,
.nav-icon {
  display: inline-block;
  font-size: 15px;
  margin-right: 12px;
  position: relative;
}

.svg-icon {
  width: 1em;
  height: 1em;
  position: relative;
  fill: currentColor;
  vertical-align: -2px;
  flex-shrink: 0;
}
</style>
