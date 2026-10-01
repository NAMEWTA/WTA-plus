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
      placeholder="搜索业务 App Key 或 Client ID"
      aria-label="业务客户端"
      @update:model-value="emit('update:modelValue', $event || '')"
    >
      <el-option
        v-for="item in rows"
        :key="item.clientId"
        :value="item.clientId"
        :label="`${item.clientKey} · ${item.clientId}`"
        :disabled="!allowUnavailable && (item.status !== '0' || !item.socialEnabled)"
      >
        {{ item.clientKey }} · {{ item.clientId }} {{ item.unavailableReason ? `（${item.unavailableReason}）` : '' }}
      </el-option>
    </el-select>
    <p v-if="error" role="alert">
      {{ error }}
      <el-button link @click="search()">重试</el-button>
    </p>
    <small v-else-if="selected">
      {{ selected.unavailableReason || (selected.registerEnabled ? '允许公开注册' : '关闭公开注册') }}
    </small>
  </div>
</template>
<script setup lang="ts">
import type { AuthClientOption } from '@namewta/domain-system';
import { computed, onMounted, watch } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { useAuthLookup } from './useAuthLookup';
const {
  runtime,
  modelValue,
  disabled = false,
  allowUnavailable = false
} = defineProps<{ runtime: SystemWebRuntime; modelValue: string; disabled?: boolean; allowUnavailable?: boolean }>();
const emit = defineEmits<{ 'update:modelValue': [value: string]; selected: [value: AuthClientOption | undefined] }>();
const { rows, loading, error, search } = useAuthLookup(runtime, async (keyword, signal) => {
  const result = await runtime.service.authConfig.clientOptions({ keyword }, signal);
  if (modelValue && !result.some(item => item.clientId === modelValue)) {
    result.unshift(...(await runtime.service.authConfig.clientOptions({ clientIds: [modelValue] }, signal)));
  }
  return result;
});
const selected = computed(() => rows.value.find(item => item.clientId === modelValue));
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
