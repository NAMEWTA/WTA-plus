import type { FormInstance } from 'element-plus';
import { computed, onScopeDispose, reactive, ref, type Ref } from 'vue';

type TableRef<T> = { toggleRowExpansion(row: T, expanded: boolean): void };

export function useLoading() {
  const pendingCount = ref(0);
  const loading = computed(() => pendingCount.value > 0);
  const withLoading = async <T>(task: () => Promise<T>): Promise<T> => {
    pendingCount.value += 1;
    try { return await task(); } finally { pendingCount.value = Math.max(0, pendingCount.value - 1); }
  };
  return { loading, withLoading };
}

export function useLatestRequest() {
  let generation = 0;
  let disposed = false;
  const invalidate = () => { generation += 1; };
  const runLatest = async <T>(task: () => Promise<T>, commit: (value: T) => void): Promise<boolean> => {
    const activeGeneration = ++generation;
    const value = await task();
    if (disposed || activeGeneration !== generation) return false;
    commit(value);
    return true;
  };
  onScopeDispose(() => {
    disposed = true;
    invalidate();
  });
  return { invalidate, runLatest };
}

export function useSearchToggle(initialValue = true) {
  return { showSearch: ref(initialValue) };
}

export function useSearchReset<T extends object>(options: {
  queryFormRef: Ref<FormInstance | undefined>;
  queryParams?: Ref<T>;
  pageNumKey?: keyof T;
  pageSizeKey?: keyof T;
  initialPageSize?: number;
  resetExtras?: () => void;
  afterReset?: () => Promise<void> | void;
}) {
  const resetQuery = async () => {
    options.queryFormRef.value?.resetFields();
    if (options.queryParams?.value && options.pageNumKey) {
      (options.queryParams.value as Record<keyof T, unknown>)[options.pageNumKey] = 1;
    }
    if (options.queryParams?.value && options.pageSizeKey && options.initialPageSize !== undefined) {
      (options.queryParams.value as Record<keyof T, unknown>)[options.pageSizeKey] = options.initialPageSize;
    }
    options.resetExtras?.();
    await options.afterReset?.();
  };
  return { resetQuery };
}

export function useFormDialog<T extends object>(options: {
  form: Ref<T>;
  formRef?: Ref<FormInstance | undefined>;
  initialFormData: T;
}) {
  const dialog = reactive({ visible: false, title: '' });
  const resetForm = () => {
    options.form.value = { ...options.initialFormData };
    options.formRef?.value?.resetFields();
    options.formRef?.value?.clearValidate();
  };
  const showDialog = (title: string) => { dialog.title = title; dialog.visible = true; };
  const openDialog = (title: string) => { resetForm(); showDialog(title); };
  const closeDialog = () => { dialog.visible = false; };
  return { dialog, resetForm, openDialog, showDialog, closeDialog };
}

export function useTableSelection<T, ID extends string | number = string | number>(getRowId: (row: T) => ID) {
  const ids = ref<ID[]>([]) as Ref<ID[]>;
  const single = ref(true);
  const multiple = ref(true);
  const handleSelectionChange = (selection: T[]) => {
    ids.value = selection.map(getRowId);
    single.value = selection.length !== 1;
    multiple.value = selection.length === 0;
  };
  return { ids, single, multiple, handleSelectionChange };
}

export function useDateRangeQuery(propName: string) {
  const dateRange = ref<[string, string]>(['', '']);
  const applyDateRange = <T extends { params?: Readonly<Record<string, unknown>> }>(query: T): T => ({
    ...query,
    params: {
      ...query.params,
      [`begin${r'${propName}'}`]: dateRange.value[0],
      [`end${r'${propName}'}`]: dateRange.value[1]
    }
  });
  const resetDateRange = () => { dateRange.value = ['', '']; };
  return { dateRange, applyDateRange, resetDateRange };
}

export function useTreeTableExpand<T extends { children?: readonly T[] }>(options: {
  tableRef: Ref<TableRef<T> | undefined>;
  data: Ref<T[]>;
}) {
  const isExpandAll = ref(true);
  const toggleRows = (rows: readonly T[], expanded: boolean) => rows.forEach(row => {
    options.tableRef.value?.toggleRowExpansion(row, expanded);
    if (row.children?.length) toggleRows(row.children, expanded);
  });
  const handleToggleExpandAll = () => {
    isExpandAll.value = !isExpandAll.value;
    toggleRows(options.data.value, isExpandAll.value);
  };
  return { isExpandAll, handleToggleExpandAll };
}
