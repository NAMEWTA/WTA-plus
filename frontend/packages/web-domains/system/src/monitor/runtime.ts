import type { NavigationIntent, MonitorService } from '@namewta/domain-system/monitor';
import { computed, shallowRef, type Ref } from 'vue';

export interface MonitorDictOption {
  label: string;
  value: string;
  elTagType?: string;
  elTagClass?: string;
}
export interface MonitorWebRuntime {
  service: MonitorService;
  confirm(message: string): Promise<void>;
  success(message: string): void;
  error(message: string): void;
  loading(message: string): unknown;
  closeLoading(handle: unknown): void;
  download(url: string, params: unknown, fileName: string): Promise<void> | void;
  openDownload(intent: NavigationIntent): Promise<void> | void;
  dicts(...types: string[]): Record<string, Readonly<Ref<MonitorDictOption[]>>>;
  hasPermission(permission: string): boolean;
}

export function createLiveMonitorDictRefs(
  types: readonly string[],
  load: () => Promise<Record<string, MonitorDictOption[]>>
) {
  const source = shallowRef<Record<string, MonitorDictOption[]>>({});
  void load().then(value => {
    source.value = value;
  });
  return Object.fromEntries(types.map(type => [type, computed(() => source.value[type] ?? [])]));
}
