import { onScopeDispose, ref, shallowRef, watch } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { authConfigError } from './errors';

/** 两种远程选择器共用替换请求和会话取消，避免快速输入时写回旧列表。 */
export function useAuthLookup<T>(
  runtime: SystemWebRuntime,
  load: (keyword: string, signal: AbortSignal) => Promise<T[]>
) {
  const rows = shallowRef<T[]>([]);
  const loading = ref(false);
  const error = ref('');
  let controller: AbortController | undefined;
  let version = 0;
  let active = true;
  const identity = runtime.sessionSnapshot().generation;
  async function search(keyword = '') {
    if (!active || runtime.sessionSnapshot().generation !== identity) return;
    const current = ++version;
    controller?.abort();
    controller = new AbortController();
    loading.value = true;
    error.value = '';
    try {
      const result = await load(keyword.trim(), controller.signal);
      if (active && current === version && runtime.sessionSnapshot().generation === identity) rows.value = result;
    } catch (failure) {
      if (active && current === version) error.value = authConfigError(failure, '选项加载失败，请重试');
    } finally {
      if (active && current === version) loading.value = false;
    }
  }
  function dispose() {
    active = false;
    version++;
    controller?.abort();
    rows.value = [];
    loading.value = false;
    error.value = '';
  }
  watch(() => runtime.sessionSnapshot().generation, dispose, { flush: 'sync' });
  onScopeDispose(dispose);
  return { rows, loading, error, search };
}
