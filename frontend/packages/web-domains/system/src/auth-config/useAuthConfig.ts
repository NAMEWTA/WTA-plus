import type { AuthConfigQuery } from '@namewta/domain-system';
import { onBeforeUnmount, onMounted, ref, shallowRef, watch, type Ref } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { authConfigError } from './errors';

interface Resource<T> {
  list(query: AuthConfigQuery, signal?: AbortSignal): Promise<{ rows: T[]; total: number }>;
  get(id: string, signal?: AbortSignal): Promise<T>;
  save(value: T, signal?: AbortSignal): Promise<string>;
  remove(value: T, signal?: AbortSignal): Promise<void>;
  refresh(signal?: AbortSignal): Promise<void>;
}
/** 两个配置页共用查询/弹窗生命周期，密钥草稿由关闭与身份变化一并清除。 */
export function useAuthConfig<T extends { id?: string; version: number }>(
  runtime: SystemWebRuntime,
  service: Resource<T>,
  permission: string,
  empty: () => T,
  query: () => Partial<AuthConfigQuery> = () => ({})
) {
  const rows = shallowRef<T[]>([]);
  const total = ref(0);
  const pageNum = ref(1);
  const loading = ref(false);
  const busy = ref(false);
  const error = ref('');
  const visible = ref(false);
  const draft = ref(empty()) as Ref<T>;
  const lifetime = new AbortController();
  let active = true;
  let requestVersion = 0;
  let queryAbort: AbortController | undefined;
  const identity = runtime.sessionSnapshot().generation;
  const current = () => active && runtime.sessionSnapshot().generation === identity;
  async function load() {
    const version = ++requestVersion;
    queryAbort?.abort();
    queryAbort = new AbortController();
    if (!runtime.hasPermission(`${permission}:list`)) {
      error.value = '无权读取此配置';
      return;
    }
    loading.value = true;
    error.value = '';
    try {
      const result = await service.list({ ...query(), pageNum: pageNum.value, pageSize: 20 }, queryAbort.signal);
      if (current() && version === requestVersion) {
        rows.value = result.rows;
        total.value = result.total;
      }
    } catch (failure) {
      if (current() && version === requestVersion) error.value = authConfigError(failure, '配置加载失败，请重试');
    } finally {
      if (current() && version === requestVersion) loading.value = false;
    }
  }
  function close() {
    visible.value = false;
    draft.value = empty();
  }
  function create() {
    if (!runtime.hasPermission(`${permission}:add`)) return;
    draft.value = empty();
    visible.value = true;
    error.value = '';
  }
  async function edit(row: T) {
    if (!row.id || busy.value || !runtime.hasPermission(`${permission}:edit`)) return;
    busy.value = true;
    try {
      const value = await service.get(row.id, lifetime.signal);
      if (current()) {
        draft.value = value;
        visible.value = true;
      }
    } catch (failure) {
      if (current()) error.value = authConfigError(failure, '配置读取失败，请重试');
    } finally {
      if (current()) busy.value = false;
    }
  }
  async function save() {
    if (busy.value || !runtime.hasPermission(`${permission}:${draft.value.id ? 'edit' : 'add'}`)) return;
    busy.value = true;
    error.value = '';
    try {
      const id = await service.save(draft.value, lifetime.signal);
      if (current()) {
        close();
        runtime.success('配置已保存');
        await load();
        if (current()) return id;
      }
    } catch (failure) {
      if (current()) error.value = authConfigError(failure, '保存失败，请检查填写内容；版本冲突时请关闭后重新编辑。');
    } finally {
      if (current()) busy.value = false;
    }
  }
  async function remove(row: T) {
    if (busy.value || !runtime.hasPermission(`${permission}:remove`)) return;
    try {
      await runtime.confirm('确认删除此登录配置？已配置的登录入口将不可用。');
    } catch {
      return;
    }
    if (!current()) return;
    busy.value = true;
    try {
      await service.remove(row, lifetime.signal);
      if (current()) await load();
    } catch (failure) {
      if (current()) error.value = authConfigError(failure, '删除失败，配置可能已被引用或变更，请刷新后重试');
    } finally {
      if (current()) busy.value = false;
    }
  }
  async function refresh() {
    if (busy.value || !runtime.hasPermission(`${permission}:edit`)) return;
    busy.value = true;
    try {
      await service.refresh(lifetime.signal);
      if (current()) {
        runtime.success('缓存已刷新');
        await load();
      }
    } catch (failure) {
      if (current()) error.value = authConfigError(failure, '缓存刷新失败，请稍后重试');
    } finally {
      if (current()) busy.value = false;
    }
  }
  watch(
    () => runtime.sessionSnapshot().generation,
    () => {
      close();
      rows.value = [];
      queryAbort?.abort();
      lifetime.abort();
    }
  );
  onMounted(load);
  onBeforeUnmount(() => {
    active = false;
    close();
    queryAbort?.abort();
    lifetime.abort();
  });
  const editById = (id: unknown) => {
    const row = rows.value.find(item => item.id === id);
    if (row) return edit(row);
  };
  const search = () => {
    pageNum.value = 1;
    return load();
  };
  const removeById = (id: unknown) => {
    const row = rows.value.find(item => item.id === id);
    if (row) return remove(row);
  };
  return {
    editById,
    removeById,
    rows,
    total,
    pageNum,
    loading,
    busy,
    error,
    visible,
    draft,
    load,
    search,
    create,
    edit,
    close,
    save,
    remove,
    refresh
  };
}
