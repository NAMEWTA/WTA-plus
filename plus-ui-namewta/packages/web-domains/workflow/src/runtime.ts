import type { WorkflowDefinitionService } from '@namewta/domain-workflow';
import { computed, shallowRef, type Component, type Ref } from 'vue';

export interface WorkflowDictOption {
  label: string;
  value: string;
}

export interface WorkflowAttachment {
  originalName: string;
  ossId: string | number;
}

export interface WorkflowWebRuntime {
  chartUrl(instanceId: string | number): Promise<string> | string;
  closeCurrentPage(): Promise<void> | void;
  confirm(message: string): Promise<void>;
  closeDesigner(activeName: string | undefined): Promise<void> | void;
  designUrl(definitionId: string, disabled: boolean): Promise<string> | string;
  dicts(...types: string[]): Record<string, Readonly<Ref<readonly WorkflowDictOption[]>>>;
  download(url: string, params: unknown, fileName: string): Promise<void> | void;
  downloadAttachment(ossId: string | number): Promise<void> | void;
  error(message: string): void;
  fileUpload: Component;
  resolveAttachments(ids: string | number): Promise<readonly WorkflowAttachment[]>;
  service: WorkflowDefinitionService;
  success(message: string): void;
  treePanel: Component;
}

export type WorkflowDictSource = Record<string, WorkflowDictOption[]>;

export function createLiveWorkflowDictRefs(
  types: readonly string[],
  load: () => Promise<WorkflowDictSource> | WorkflowDictSource
): Record<string, Readonly<Ref<readonly WorkflowDictOption[]>>> {
  const source = shallowRef<WorkflowDictSource>();
  void Promise.resolve(load()).then(loaded => {
    source.value = loaded;
  });
  return Object.fromEntries(types.map(type => [type, computed(() => source.value?.[type] ?? [])]));
}

export function requireWorkflowWebRuntime(runtime: WorkflowWebRuntime | undefined): WorkflowWebRuntime {
  if (!runtime) throw new Error('WorkflowWebRuntime is required');
  return runtime;
}
