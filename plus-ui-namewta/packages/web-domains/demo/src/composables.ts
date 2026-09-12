import type { FormInstance, TableInstance } from 'element-plus';
import { reactive, ref, type Ref } from 'vue';

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

export function useSearchToggle(initialValue = true) {
  return { showSearch: ref(initialValue) };
}

export function useSearchReset<T extends object>(options: {
  afterReset?: () => void;
  pageNumKey?: keyof T;
  queryFormRef: Ref<FormInstance | undefined>;
  queryParams?: Ref<T>;
}) {
  const resetQuery = () => {
    options.queryFormRef.value?.resetFields();
    if (options.queryParams?.value && options.pageNumKey)
      options.queryParams.value[options.pageNumKey] = 1 as T[keyof T];
    options.afterReset?.();
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
  const openDialog = (title: string) => {
    resetForm();
    dialog.title = title;
    dialog.visible = true;
  };
  const showDialog = (title: string) => {
    dialog.title = title;
    dialog.visible = true;
  };
  const closeDialog = () => {
    dialog.visible = false;
  };
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

export function useTreeTableExpand<T extends { children?: T[] }>(options: {
  data: Ref<T[]>;
  tableRef: Ref<TableInstance | undefined>;
}) {
  const isExpandAll = ref(true);
  const toggleRows = (rows: T[], expanded: boolean) => {
    for (const row of rows) {
      options.tableRef.value?.toggleRowExpansion(row, expanded);
      if (row.children?.length) toggleRows(row.children, expanded);
    }
  };
  const handleToggleExpandAll = () => {
    isExpandAll.value = !isExpandAll.value;
    toggleRows(options.data.value, isExpandAll.value);
  };
  return { isExpandAll, handleToggleExpandAll };
}

export function buildTree<T extends object>(
  data: T[],
  idKey = 'id',
  parentIdKey = 'parentId',
  childrenKey = 'children'
): T[] {
  const nodes = new Map<unknown, T>();
  const result: T[] = [];
  for (const item of data) {
    const record = item as Record<string, unknown>;
    record[childrenKey] = [];
    nodes.set(record[idKey], item);
  }
  for (const item of data) {
    const record = item as Record<string, unknown>;
    const parent = nodes.get(record[parentIdKey]);
    if (!parent) result.push(item);
    else ((parent as Record<string, unknown>)[childrenKey] as T[]).push(item);
  }
  return result;
}
