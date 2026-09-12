<template><div ref="container" class="rich-text-viewer" :class="`rich-text-viewer--${profile}`" /></template>
<script setup lang="ts">
import DOMPurify from 'dompurify';
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { extractOssIds, resolveRichTextMedia } from './content';
import type { RichTextAssetsPort } from './types';

const props = withDefaults(defineProps<{ html?: string; assets: RichTextAssetsPort; resourceId?: string; profile?: 'article' | 'compact'; showMedia?: boolean }>(), { html: '', profile: 'article', showMedia: true });
const container = ref<HTMLElement>();
let controller: AbortController | undefined;
let generation = 0;

async function render() {
  const current = ++generation;
  controller?.abort();
  controller = new AbortController();
  const ids = props.showMedia ? extractOssIds(props.html ?? '') : [];
  let resolved = props.html ?? '';
  if (ids.length) {
    try { resolved = resolveRichTextMedia(resolved, await props.assets.resolve(ids, { signal: controller.signal, richTextId: props.resourceId })); } catch { /* unavailable placeholders remain */ }
  }
  if (current !== generation || !container.value) return;
  container.value.innerHTML = DOMPurify.sanitize(resolved, { USE_PROFILES: { html: true } });
  if (!props.showMedia) container.value.querySelectorAll('img,audio,video,[data-oss-id]').forEach(element => element.remove());
}
watch(() => [props.html, props.profile, props.showMedia], () => void nextTick(render), { immediate: true });
onBeforeUnmount(() => { generation += 1; controller?.abort(); });
</script>
<style scoped>
.rich-text-viewer { line-height: 1.7; overflow-wrap: anywhere; }
.rich-text-viewer :deep(img), .rich-text-viewer :deep(video) { max-width: 100%; height: auto; }
.rich-text-viewer :deep(audio) { max-width: 100%; }
.rich-text-viewer--compact { font-size: 13px; }
.rich-text-viewer :deep([data-richtext-unavailable="true"]) { color: var(--el-text-color-placeholder, #a8abb2); }
</style>
