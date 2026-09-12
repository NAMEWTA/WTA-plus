<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive } from 'vue';
import { createAiChatSession, type AiChatSnapshot } from '../chatSession';
import type { AiWebRuntime } from '../runtime';

defineOptions({ name: 'AiChatPage' });

const { runtime } = defineProps<{ runtime: AiWebRuntime }>();
const state = reactive<AiChatSnapshot>({ error: '', frameUrl: '', loading: false });
const session = createAiChatSession(runtime, snapshot => Object.assign(state, snapshot));

const frameSource = (event: Event) => (event.currentTarget as HTMLIFrameElement | null)?.getAttribute('src') ?? undefined;
const handleFrameLoad = (event: Event) => session.frameLoaded(frameSource(event));
const retry = () => void session.load();

onMounted(retry);
onBeforeUnmount(() => session.dispose());
</script>

<template>
  <div v-loading="state.loading" class="ai-chat-page">
    <iframe
      v-if="state.frameUrl"
      class="chat-frame"
      :src="state.frameUrl"
      title="Snail AI"
      allow="clipboard-read; clipboard-write"
      @load="handleFrameLoad"
    />
    <el-empty v-else class="chat-empty" :description="state.error || '正在加载 Snail AI'">
      <el-button v-if="state.error" type="primary" @click="retry">重新加载</el-button>
    </el-empty>
  </div>
</template>

<style scoped lang="scss">
.ai-chat-page {
  height: calc(100vh - 123px);
  min-height: 0;
  overflow: hidden;
  background: var(--el-bg-color-page);
  border-radius: var(--app-radius-base);
}

.chat-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
  background: var(--app-surface-bg);
}

.chat-empty {
  height: 100%;
  background: var(--app-surface-bg);
}

@media (max-width: 768px) {
  .ai-chat-page {
    height: calc(100vh - 88px);
  }
}
</style>
