<template>
  <div class="iframe-wrapper">
    <iframe :src="iframeUrl" title="流程图" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { WorkflowWebRuntime } from '../runtime';

const props = defineProps<{ insId?: string | number | null; runtime: WorkflowWebRuntime }>();
const iframeUrl = ref('');
onMounted(async () => {
  if (props.insId !== undefined && props.insId !== null) iframeUrl.value = await props.runtime.chartUrl(props.insId);
});
</script>

<style scoped>
.iframe-wrapper {
  height: 68vh;
  overflow: hidden;
}
.iframe-wrapper iframe {
  width: 100%;
  height: 100%;
  border: 0;
}
</style>
