<template>
  <section class="ui-status-panel" :aria-labelledby="titleId" :aria-busy="busy">
    <h1 :id="titleId">{{ title }}</h1>
    <p
      v-if="message"
      :role="error ? 'alert' : 'status'"
      aria-live="polite"
      :class="{ 'ui-status-panel__error': error }"
    >
      {{ message }}
    </p>
    <div v-if="$slots.default" class="ui-status-panel__actions"><slot /></div>
  </section>
</template>

<script setup lang="ts">
import { useId } from 'vue';
defineProps<{ title: string; message?: string; error?: boolean; busy?: boolean }>();
const titleId = useId();
</script>

<style scoped>
.ui-status-panel {
  width: min(520px, 100%);
  padding: clamp(20px, 5vw, 36px);
  border: 1px solid var(--app-surface-border);
  border-radius: var(--app-radius-lg);
  background: var(--app-surface-bg);
  box-shadow: var(--app-shadow-sm);
  text-align: center;
}
h1 {
  margin: 0;
  color: var(--app-text-title);
  font-size: 24px;
  line-height: 1.4;
}
p {
  margin: 16px 0 0;
  color: var(--app-text-muted);
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.ui-status-panel__error {
  color: var(--app-text-danger);
}
.ui-status-panel__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}
.ui-status-panel__actions :deep(a) {
  color: var(--app-text-accent);
}
</style>
