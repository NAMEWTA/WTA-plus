import type { FormInstance, TableInstance } from 'element-plus';
import { reactive, ref, type Ref } from 'vue';

export function useLoading(initialValue = false) {
  const loading = ref(initialValue);
  const setLoading = (value: boolean) => (loading.value = value);
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
  const closeDialog = () => (dialog.visible = false);
  return { dialog, openDialog, closeDialog };
}
export function useFormDialog<T extends object>(options: {
  form: Ref<T>;
  formRef?: Ref<FormInstance | undefined>;
  initialFormData: T;
  initialTitle?: string;
}) {
  const { dialog, openDialog, closeDialog } = useDialogState(options.initialTitle);
  const resetForm = () => {
    options.form.value = { ...options.initialFormData };
    options.formRef?.value?.resetFields();
    options.formRef?.value?.clearValidate();
  };
  return {
    dialog,
    resetForm,
    openDialog: (title?: string) => {
      resetForm();
      openDialog(title);
    },
    showDialog: openDialog,
    closeDialog
  };
}
export function useSearchToggle(initialValue = true) {
  return { showSearch: ref(initialValue) };
}
export function useSearchReset<T extends Record<string, unknown>>(options: {
  queryFormRef: Ref<FormInstance | undefined>;
  queryParams?: Ref<T>;
  pageNumKey?: keyof T;
  pageSizeKey?: keyof T;
  initialPageSize?: number;
  resetExtras?: () => void;
  afterReset?: () => void;
}) {
  return {
    resetQuery: () => {
      options.queryFormRef.value?.resetFields();
      if (options.queryParams?.value && options.pageNumKey)
        options.queryParams.value[options.pageNumKey] = 1 as T[keyof T];
      if (options.queryParams?.value && options.pageSizeKey && options.initialPageSize !== undefined)
        options.queryParams.value[options.pageSizeKey] = options.initialPageSize as T[keyof T];
      options.resetExtras?.();
      options.afterReset?.();
    }
  };
}
export function useTableSelection<T, ID extends string | number = string | number>(getRowId: (row: T) => ID) {
  const ids = ref<ID[]>([]) as Ref<ID[]>;
  const selectedRows = ref<T[]>([]) as Ref<T[]>;
  const single = ref(true);
  const multiple = ref(true);
  const handleSelectionChange = (selection: T[]) => {
    selectedRows.value = selection;
    ids.value = selection.map(getRowId);
    single.value = selection.length !== 1;
    multiple.value = selection.length === 0;
  };
  return { ids, selectedRows, single, multiple, handleSelectionChange };
}
export function useTreeCollapsed(initialValue = false) {
  return { treeCollapsed: ref(initialValue) };
}
export function useTreeTableExpand<T extends object>(options: {
  tableRef: Ref<TableInstance | undefined>;
  data: Ref<T[]>;
  initialExpandAll?: boolean;
}) {
  const isExpandAll = ref(options.initialExpandAll ?? true);
  const toggle = (rows: T[], expanded: boolean) =>
    rows.forEach(row => {
      options.tableRef.value?.toggleRowExpansion(row, expanded);
      const children = (row as { children?: unknown }).children;
      if (Array.isArray(children)) toggle(children as T[], expanded);
    });
  return {
    isExpandAll,
    handleToggleExpandAll: () => {
      isExpandAll.value = !isExpandAll.value;
      toggle(options.data.value, isExpandAll.value);
    }
  };
}
