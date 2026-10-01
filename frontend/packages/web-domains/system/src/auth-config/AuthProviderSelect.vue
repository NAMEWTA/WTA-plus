<template>
  <div class="auth-select">
    <el-select
      :model-value="modelValue"
      :disabled="disabled"
      filterable
      remote
      clearable
      :remote-method="search"
      :loading="loading"
      placeholder="搜索身份源名称"
      aria-label="身份源"
      @update:model-value="emit('update:modelValue', $event || '')"
    >
      <el-option v-for="item in rows" :key="item.id" :value="item.id" :label="`${item.name} · ${item.providerKey}`">
        {{ item.name }} · {{ item.providerKey }}{{ item.enabled ? '' : '（已停用）' }}
      </el-option>
      <template #footer><small>按名称搜索，最多显示 50 项。</small></template>
    </el-select>
    <p v-if="error" role="alert">
      {{ error }}
      <el-button link @click="search()">重试</el-button>
    </p>
    <small v-else-if="selected && !selected.enabled">此身份源已停用，接入启用后也不会显示登录入口。</small>
  </div>
</template>
<script setup lang="ts">
import type { AuthProviderOption } from '@namewta/domain-system';
import { computed, onMounted, watch } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { useAuthLookup } from './useAuthLookup';
const {
  runtime,
  modelValue,
  disabled = false
} = defineProps<{ runtime: SystemWebRuntime; modelValue: string; disabled?: boolean }>();
const emit = defineEmits<{ 'update:modelValue': [value: string]; selected: [value: AuthProviderOption | undefined] }>();
const { rows, loading, error, search } = useAuthLookup(runtime, async (keyword, signal) => {
  return runtime.service.authConfig.providerOptions(
    { keyword: keyword || undefined, selectedId: modelValue || undefined },
    signal
  );
});
const selected = computed(() => rows.value.find(item => item.id === modelValue));
watch(selected, value => emit('selected', value), { immediate: true });
watch(
  () => modelValue,
  () => {
    if (modelValue && !selected.value) void search();
  }
);
onMounted(() => search());
</script>
<style scoped>
.auth-select {
  width: 100%;
}
.auth-select small {
  display: block;
  color: var(--el-text-color-secondary);
}
</style>
