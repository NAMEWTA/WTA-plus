import type { FormRules } from 'element-plus';

declare global {
  interface FieldOption {
    key: number;
    label: string;
    visible: boolean;
    children?: FieldOption[];
  }
  interface PageData<T, D> {
    form: T;
    queryParams: D;
    rules: FormRules;
  }
}

export type SystemPageTypeMarker = never;
