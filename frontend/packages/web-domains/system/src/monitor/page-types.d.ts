import type { FormRules } from 'element-plus';
declare global {
  interface PageData<T, D> {
    form: T;
    queryParams: D;
    rules: FormRules;
  }
  type ElFormInstance = import('element-plus').FormInstance;
  type ElTableInstance = import('element-plus').TableInstance;
}
export type MonitorPageTypeMarker = never;
