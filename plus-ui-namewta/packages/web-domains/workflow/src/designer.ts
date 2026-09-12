import type { WorkflowWebRuntime } from './runtime';

export interface DesignerQuery {
  activeName?: unknown;
  definitionId?: unknown;
  disabled?: unknown;
}

export function createDesignerController(runtime: WorkflowWebRuntime, query: DesignerQuery) {
  const activeName = typeof query.activeName === 'string' ? query.activeName : undefined;
  return Object.freeze({
    url: async () => runtime.designUrl(String(query.definitionId ?? ''), String(query.disabled) === 'true'),
    onMessage: (data: unknown) => {
      if (data && typeof data === 'object' && (data as { method?: unknown }).method === 'close') {
        return runtime.closeDesigner(activeName);
      }
    }
  });
}
