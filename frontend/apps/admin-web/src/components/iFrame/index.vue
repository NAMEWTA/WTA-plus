<template>
  <div v-loading="loading" :style="{ height }">
    <iframe
      :src="src"
      :title="title"
      frameborder="no"
      style="width: 100%; height: 100%"
      scrolling="auto"
      @load="loading = false"
    />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = withDefaults(defineProps<{ src: string; title?: string }>(), { title: '外部控制台' });

const height = ref(`${Math.max(document.documentElement.clientHeight - 94.5, 320)}px`);
const loading = ref(true);
const resize = () => {
  height.value = `${Math.max(document.documentElement.clientHeight - 94.5, 320)}px`;
};

watch(
  () => props.src,
  () => {
    loading.value = true;
  }
);
onMounted(() => {
  window.addEventListener('resize', resize);
});
onBeforeUnmount(() => window.removeEventListener('resize', resize));
</script>
