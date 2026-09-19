import type { WorkflowWebRuntime } from './runtime';

export interface DesignerQuery {
  activeName?: unknown;
  definitionId?: unknown;
  disabled?: unknown;
}

/** Accept only the existing vendor close message from the currently owned frame. */
export function isDesignerCloseMessage(
  event: Pick<MessageEvent<unknown>, 'data' | 'origin' | 'source'>,
  frameWindow: Window | null | undefined,
  designUrl: string,
  baseUrl: string
): boolean {
  if (!frameWindow || event.source !== frameWindow || !designUrl) return false;
  const data = event.data;
  if (!data || typeof data !== 'object' || Array.isArray(data)
    || !Object.hasOwn(data, 'method') || (data as { method?: unknown }).method !== 'close') return false;
  try {
    const url = new URL(designUrl, baseUrl);
    return (url.protocol === 'https:' || url.protocol === 'http:') && event.origin === url.origin;
  } catch {
    return false;
  }
}

export function createDesignerController(runtime: WorkflowWebRuntime, query: DesignerQuery) {
  const activeName = typeof query.activeName === 'string' ? query.activeName : undefined;
  return Object.freeze({
    url: async () => runtime.designUrl(String(query.definitionId ?? ''), String(query.disabled) === 'true'),
    close: () => runtime.closeDesigner(activeName)
  });
}
