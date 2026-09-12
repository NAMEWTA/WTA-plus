<template>
  <div class="warm-flow-designer-page">
    <iframe :src="iframeUrl" frameborder="0" class="warm-flow-designer-page__iframe" title="流程设计"></iframe>
  </div>
</template>

<script setup name="WarmFlow" lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import type { WorkflowWebRuntime } from '../runtime';
import { createDesignerController } from '../designer';

const { runtime } = defineProps<{ runtime: WorkflowWebRuntime }>();
const route = useRoute();
const iframeUrl = ref('');
const controller = createDesignerController(runtime, route.query);
const onDesignerMessage = (event: MessageEvent) => {
  void controller.onMessage(event.data);
};
const open = async (definitionId: unknown, disabled: unknown) => {
  iframeUrl.value = await runtime.designUrl(String(definitionId ?? ''), String(disabled) === 'true');
};

onMounted(async () => {
  window.addEventListener('message', onDesignerMessage);
  iframeUrl.value = await controller.url();
});
onBeforeUnmount(() => window.removeEventListener('message', onDesignerMessage));
defineExpose({ open });
</script>

<style scoped>
.warm-flow-designer-page {
  width: 100%;
  height: calc(100vh - 123px);
  overflow: hidden;
}
.warm-flow-designer-page__iframe {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
