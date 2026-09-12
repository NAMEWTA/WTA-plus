import { reactive, ref, type Ref } from 'vue';

type FormRef = {
  resetFields?(): void;
  clearValidate?(): void;
};

type TableRef<T> = {
  toggleRowExpansion(row: T, expanded: boolean): void;
};

export function useLoading(initialValue = false) {
  const loading = ref(initialValue);
  const setLoading = (value: boolean) => {
    loading.value = value;
  };
  const withLoading = async <T>(task: () => Promise<T>) => {
    loading.value = true;
    try {
      return await task();
    } finally {
      loading.value = false;
    }
  };
  return { loading, setLoading, withLoading };
}

export function useDialogState(initialTitle = '') {
  const dialog = reactive({ visible: false, title: initialTitle });
  const openDialog = (title?: string) => {
    if (title !== undefined) dialog.title = title;
    dialog.visible = true;
  };
  const closeDialog = () => {
    dialog.visible = false;
  };
  const setTitle = (title: string) => {
    dialog.title = title;
  };
  return { dialog, openDialog, closeDialog, setTitle };
}

export function useFormDialog<T extends object>(options: {
  form: Ref<T>;
  formRef?: Ref<FormRef | undefined>;
  initialFormData: T;
  initialTitle?: string;
}) {
  const { dialog, openDialog: showDialog, closeDialog, setTitle } = useDialogState(options.initialTitle);
  const resetForm = () => {
    options.form.value = { ...options.initialFormData };
    options.formRef?.value?.resetFields?.();
    options.formRef?.value?.clearValidate?.();
  };
  const openDialog = (title?: string) => {
    resetForm();
    showDialog(title);
  };
  return { dialog, resetForm, openDialog, showDialog, closeDialog, setTitle };
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
  const resetQuery = () => {
    options.queryFormRef.value?.resetFields?.();
    if (options.queryParams?.value && options.pageNumKey) {
      (options.queryParams.value as Record<keyof T, unknown>)[options.pageNumKey] = options.initialPageNum ?? 1;
    }
    if (options.queryParams?.value && options.pageSizeKey && options.initialPageSize !== undefined) {
      (options.queryParams.value as Record<keyof T, unknown>)[options.pageSizeKey] = options.initialPageSize;
    }
    options.resetExtras?.();
    options.afterReset?.();
  };
  return { resetQuery };
}

export function useSearchToggle(initialValue = true) {
  return { showSearch: ref(initialValue) };
}

export function useDateRangeQuery(propName?: string) {
  const dateRange = ref<[string, string]>(['', '']);
  const applyDateRange = <T extends object>(query: T): T => {
    const record = query as T & { params?: Record<string, unknown> };
    const params =
      typeof record.params === 'object' && record.params !== null && !Array.isArray(record.params) ? record.params : {};
    const prefix = propName ?? 'Time';
    params[`begin${prefix}`] = dateRange.value[0];
    params[`end${prefix}`] = dateRange.value[1];
    record.params = params;
    return query;
  };
  const resetDateRange = () => {
    dateRange.value = ['', ''];
  };
  return { dateRange, applyDateRange, resetDateRange };
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

export function useTreeCollapsed(initialValue = false) {
  return { treeCollapsed: ref(initialValue) };
}

export function useTreeTableExpand<T extends object>(options: {
  tableRef: Ref<TableRef<T> | undefined>;
  data: Ref<T[]>;
  initialExpandAll?: boolean;
  getChildren?: (row: T) => T[] | undefined;
}) {
  const isExpandAll = ref(options.initialExpandAll ?? true);
  const getChildren = options.getChildren ?? (row => (row as { children?: T[] }).children);
  const toggleRows = (rows: T[], expanded: boolean) => {
    for (const row of rows) {
      options.tableRef.value?.toggleRowExpansion(row, expanded);
      const children = getChildren(row);
      if (children?.length) toggleRows(children, expanded);
    }
  };
  const handleToggleExpandAll = () => {
    isExpandAll.value = !isExpandAll.value;
    toggleRows(options.data.value, isExpandAll.value);
  };
  return { isExpandAll, handleToggleExpandAll };
}
