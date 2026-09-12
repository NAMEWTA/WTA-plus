import type { OpenApiService, SystemService } from '@namewta/domain-system';
import type { RouteLocationRaw } from 'vue-router';
import { computed, shallowRef, type Component, type Ref } from 'vue';

export interface SystemDictOption {
  label: string;
  value: string;
}

export type SystemPasswordCharacterClass = 'UPPERCASE' | 'LOWERCASE' | 'DIGIT' | 'SPECIAL';

export type SystemPasswordViolationReason =
  | 'PASSWORD_TOO_SHORT'
  | 'PASSWORD_TOO_LONG'
  | 'PASSWORD_MISSING_UPPERCASE'
  | 'PASSWORD_MISSING_LOWERCASE'
  | 'PASSWORD_MISSING_DIGIT'
  | 'PASSWORD_MISSING_SPECIAL'
  | 'PASSWORD_CONTAINS_DISALLOWED_CHARACTER';

export interface SystemPasswordPolicy {
  allowedSpecialCharacters: string;
  maximumLength: number;
  minimumLength: number;
  requiredCharacterClasses: readonly SystemPasswordCharacterClass[];
}

export interface SystemPasswordViolation {
  reason: SystemPasswordViolationReason;
}

export interface SystemWebRuntime {
  service: SystemService;
  openApi: OpenApiService;
  treePanel: Component;
  editor: Component;
  imagePreview: Component;
  confirm(message: string): Promise<void>;
  success(message: string): void;
  error(message: string): void;
  warning(message: string): void;
  download(url: string, params: unknown, fileName: string): Promise<void> | void;
  downloadOss(ossId: string | number): Promise<void> | void;
  dictCache: { clean(): void; remove(type: string): void };
  sanitizeHtml(value?: string): string;
  replaceOssContentUrls(value: string, urls: Record<string, string>): Promise<string> | string;
  dicts(...types: string[]): Record<string, Readonly<Ref<SystemDictOption[]>>>;
  closeCurrentPage(): Promise<void> | void;
  closeAndOpenPage(location: RouteLocationRaw): Promise<void> | void;
  config(key: string): Promise<string | undefined>;
  hasPermission(permission: string): boolean;
  currentUserId(): string | number | undefined;
  passwordPolicy: {
    load(): Promise<SystemPasswordPolicy>;
    validate(policy: SystemPasswordPolicy, password: string): readonly SystemPasswordViolation[];
  };
  copyText(value: string): Promise<void>;
  uploadHeaders(): Readonly<Record<string, string>>;
}

export function createLiveSystemDictRefs(
  types: readonly string[],
  load: () => Promise<Record<string, SystemDictOption[]>>
): Record<string, Readonly<Ref<SystemDictOption[]>>> {
  const source = shallowRef<Record<string, SystemDictOption[]>>({});
  void load().then(value => {
    source.value = value;
  });
  return Object.fromEntries(types.map(type => [type, computed(() => source.value[type] ?? [])]));
}
