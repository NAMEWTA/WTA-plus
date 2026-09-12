import type { FormRules } from 'element-plus';

declare global {
  interface FieldOption {
    key: number;
    label: string;
    visible: boolean;
    children?: FieldOption[];
  }
  interface ImportOption {
    headers: Record<string, string>;
    url: string;
    open: boolean;
    title: string;
    isUploading: boolean;
    updateSupport: number;
  }
  interface PageData<T, D> {
    form: T;
    queryParams: D;
    rules: FormRules;
  }
}

export type SystemPageTypeMarker = never;
