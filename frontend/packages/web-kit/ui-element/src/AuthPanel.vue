<template>
  <section class="ui-auth-panel" :aria-labelledby="titleId">
    <header class="ui-auth-panel__header">
      <div>
        <p v-if="eyebrow" class="ui-auth-panel__eyebrow">{{ eyebrow }}</p>
        <h1 :id="titleId">{{ title }}</h1>
        <p v-if="description" class="ui-auth-panel__description">{{ description }}</p>
      </div>
      <slot name="header-action" />
    </header>
    <slot />
    <footer v-if="$slots.footer" class="ui-auth-panel__footer"><slot name="footer" /></footer>
  </section>
</template>

<script setup lang="ts">
import { useId } from 'vue';

defineProps<{ title: string; description?: string; eyebrow?: string }>();
const titleId = useId();
</script>

<style scoped>
.ui-auth-panel {
  width: min(440px, 100%);
  min-width: 0;
  padding: clamp(16px, 5vw, 32px);
  border: 1px solid var(--app-surface-border);
  border-radius: var(--app-radius-lg);
  background: var(--app-surface-bg);
  box-shadow: var(--app-shadow-md);
  color: var(--app-text-title);
}
.ui-auth-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 28px;
}
.ui-auth-panel__header > div {
  min-width: 0;
}
.ui-auth-panel__eyebrow {
  margin: 0 0 10px;
  color: var(--app-text-accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
}
h1 {
  margin: 0;
  color: var(--app-text-title);
  font-size: clamp(24px, 5vw, 30px);
  font-weight: 700;
  line-height: 1.3;
  overflow-wrap: anywhere;
}
.ui-auth-panel__description {
  margin: 12px 0 0;
  color: var(--app-text-muted);
  line-height: 1.7;
}
.ui-auth-panel__footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--app-surface-border);
  color: var(--app-text-muted);
  font-size: 13px;
}
.ui-auth-panel :deep(.el-form-item__label) {
  font-weight: 600;
}
.ui-auth-panel :deep(.el-input__wrapper) {
  min-height: 42px;
}
.ui-auth-panel :deep(.el-button) {
  max-width: 100%;
  height: auto;
  min-height: 36px;
  white-space: normal;
  line-height: 1.5;
}
.ui-auth-panel :deep(.el-button--primary:not(.is-circle)) {
  min-height: 42px;
}
</style>
