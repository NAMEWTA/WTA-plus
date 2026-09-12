import type { NotifyUserCandidate } from '@namewta/domain-notify';
import { computed, onScopeDispose, ref, watch, type Ref } from 'vue';
import type { NotifyUserDirectory } from '../runtime';

/** 候选结果与已选名单独立管理，查询翻页不会丢失用户已确认的选择。 */
export function useRecipientSelection(
  directory: NotifyUserDirectory,
  ids: Ref<Array<string | number>>,
  onError: (message: string) => void
) {
  const keyword = ref('');
  const page = ref(1);
  const pageSize = 20;
  const rows = ref<NotifyUserCandidate[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const searched = ref(false);
  const cache = ref(new Map<string, NotifyUserCandidate>());
  const selectedIds = computed(() => new Set(ids.value.map(String)));
  const selected = computed(() => [...selectedIds.value].map(id => cache.value.get(id) ?? { userId: id }));
  const allChecked = computed(
    () => rows.value.length > 0 && rows.value.every(row => selectedIds.value.has(String(row.userId)))
  );
  const someChecked = computed(
    () => rows.value.some(row => selectedIds.value.has(String(row.userId))) && !allChecked.value
  );
  let generation = 0;
  let disposed = false;
  let timer: ReturnType<typeof setTimeout> | undefined;

  function remember(users: NotifyUserCandidate[]) {
    users.forEach(user => cache.value.set(String(user.userId), user));
  }

  async function search(nextPage = 1) {
    clearTimeout(timer);
    const request = ++generation;
    const value = keyword.value.trim();
    page.value = nextPage;
    rows.value = [];
    total.value = 0;
    searched.value = !!value;
    loading.value = !!value;
    if (!value) return;
    try {
      const result = await directory.searchUsers(value, nextPage, pageSize);
      if (disposed || request !== generation) return;
      rows.value = result.data.rows.filter(row => row.status === '0');
      total.value = result.data.total;
      remember(rows.value);
    } catch (error) {
      if (!disposed && request === generation) onError(error instanceof Error ? error.message : '用户搜索失败，请重试');
    } finally {
      if (request === generation) loading.value = false;
    }
  }

  watch(
    keyword,
    () => {
      clearTimeout(timer);
      ++generation;
      rows.value = [];
      total.value = 0;
      loading.value = false;
      searched.value = false;
      if (keyword.value.trim()) timer = setTimeout(() => void search(), 300);
    },
    { flush: 'sync' }
  );

  function toggle(user: NotifyUserCandidate, checked: boolean) {
    const next = new Set(selectedIds.value);
    if (checked) {
      next.add(String(user.userId));
      remember([user]);
    } else next.delete(String(user.userId));
    ids.value = [...next];
  }

  function togglePage(checked: boolean) {
    const next = new Set(selectedIds.value);
    rows.value.forEach(user => (checked ? next.add(String(user.userId)) : next.delete(String(user.userId))));
    remember(rows.value);
    ids.value = [...next];
  }

  async function restore() {
    if (!ids.value.length) return;
    try {
      const result = await directory.usersByIds(ids.value);
      if (!disposed) remember(result.data);
    } catch (error) {
      if (!disposed) onError(error instanceof Error ? error.message : '已选用户信息加载失败');
    }
  }
  void restore();
  onScopeDispose(() => {
    disposed = true;
    ++generation;
    clearTimeout(timer);
  });

  return {
    keyword,
    page,
    pageSize,
    rows,
    total,
    loading,
    searched,
    selected,
    selectedIds,
    allChecked,
    someChecked,
    search,
    toggle,
    togglePage
  };
}
