import { ref, type Ref } from 'vue';
type FormRef = { resetFields?(): void };
export function useLoading(initial = false) {
  const loading = ref(initial);
  const withLoading = async <T>(task: () => Promise<T>) => {
    loading.value = true;
    try {
      return await task();
    } finally {
      loading.value = false;
    }
  };
  return { loading, withLoading };
}
export function useSearchToggle(initial = true) {
  return { showSearch: ref(initial) };
}
export function useDateRangeQuery() {
  const dateRange = ref<[string, string]>(['', '']);
  const applyDateRange = <T extends object>(query: T) => {
    const target = query as T & { params?: Record<string, unknown> };
    target.params = { ...target.params, beginTime: dateRange.value[0], endTime: dateRange.value[1] };
    return query;
  };
  const resetDateRange = () => {
    dateRange.value = ['', ''];
  };
  return { dateRange, applyDateRange, resetDateRange };
}
export function useSearchReset<T extends object>(options: {
  queryFormRef: Ref<FormRef | undefined>;
  queryParams?: Ref<T>;
  pageNumKey?: keyof T;
  pageSizeKey?: keyof T;
  initialPageNum?: number;
  initialPageSize?: number;
  resetExtras?: () => void;
  afterReset?: () => void;
}) {
  return {
    resetQuery: () => {
      options.queryFormRef.value?.resetFields?.();
      if (options.queryParams && options.pageNumKey)
        (options.queryParams.value as Record<keyof T, unknown>)[options.pageNumKey] = options.initialPageNum ?? 1;
      if (options.queryParams && options.pageSizeKey && options.initialPageSize !== undefined)
        (options.queryParams.value as Record<keyof T, unknown>)[options.pageSizeKey] = options.initialPageSize;
      options.resetExtras?.();
      options.afterReset?.();
    }
  };
}
export function useTableSelection<T, I extends string | number = string | number>(id: (row: T) => I) {
  const ids = ref<I[]>([]) as Ref<I[]>;
  const selectedRows = ref<T[]>([]) as Ref<T[]>;
  const single = ref(true);
  const multiple = ref(true);
  return {
    ids,
    selectedRows,
    single,
    multiple,
    handleSelectionChange(rows: T[]) {
      selectedRows.value = rows;
      ids.value = rows.map(id);
      single.value = rows.length !== 1;
      multiple.value = rows.length === 0;
    }
  };
}
export function useTableSortQuery<T extends object>(options: {
  queryParams: Ref<T>;
  tableRef?: Ref<{ sort?(prop: string, order: string): void } | undefined>;
  defaultSort: { prop: string; order: 'ascending' | 'descending' };
  onSortChange?: () => void;
}) {
  const defaultSort = options.defaultSort;
  const query = options.queryParams as Ref<Record<string, unknown>>;
  const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
    query.value.orderByColumn = prop;
    query.value.isAsc = order;
    options.onSortChange?.();
  };
  const resetSort = () => {
    query.value.orderByColumn = defaultSort.prop;
    query.value.isAsc = defaultSort.order;
    if (options.tableRef?.value?.sort) {
      options.tableRef.value.sort(defaultSort.prop, defaultSort.order);
      return;
    }
    options.onSortChange?.();
  };
  return { defaultSort, handleSortChange, resetSort };
}
