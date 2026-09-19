<template>
  <div class="warm-flow-designer-page">
    <iframe v-if="iframeUrl" :key="generation" ref="designerFrame" :src="iframeUrl" frameborder="0" class="warm-flow-designer-page__iframe" title="流程设计"></iframe>
  </div>
</template>

<script setup name="WarmFlow" lang="ts">
import { onActivated, onBeforeUnmount, onDeactivated, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import type { WorkflowWebRuntime } from '../runtime';
import { createDesignerController, isDesignerCloseMessage } from '../designer';

const { runtime } = defineProps<{ runtime: WorkflowWebRuntime }>();
const route = useRoute();
const iframeUrl = ref('');
const designerFrame = ref<HTMLIFrameElement>();
const generation = ref(0);
const controller = createDesignerController(runtime, route.query);
let active = false;
let disposed = false;
let closing = false;
const onDesignerMessage = async (event: MessageEvent<unknown>) => {
  if (!active || closing || !isDesignerCloseMessage(event, designerFrame.value?.contentWindow, iframeUrl.value, window.location.href)) return;
  closing = true;
  try {
    await controller.close();
  } catch (error: unknown) {
    if (active) runtime.error(error instanceof Error ? error.message : '设计器关闭失败');
  } finally {
    closing = false;
  }
};
const open = async (definitionId: unknown, disabled: unknown) => {
  if (disposed) return;
  const owner = ++generation.value;
  iframeUrl.value = '';
  try {
    const url = await runtime.designUrl(String(definitionId ?? ''), String(disabled) === 'true');
    if (!disposed && owner === generation.value) iframeUrl.value = url;
  } catch (error: unknown) {
    if (!disposed && owner === generation.value) runtime.error(error instanceof Error ? error.message : '设计器加载失败');
  }
};
const listen = () => {
  active = true;
  window.addEventListener('message', onDesignerMessage);
};
const stopListening = () => {
  active = false;
  window.removeEventListener('message', onDesignerMessage);
};
onMounted(() => {
  listen();
  void open(route.query.definitionId, route.query.disabled);
});
onActivated(listen);
onDeactivated(stopListening);
onBeforeUnmount(() => {
  disposed = true;
  generation.value++;
  stopListening();
});
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
