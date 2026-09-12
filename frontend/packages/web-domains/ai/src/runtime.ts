import type { AiService } from '@namewta/domain-ai';

export interface AiWebRuntime {
  baseUrl(): string | undefined;
  probeFrame(input: { signal: AbortSignal; url: string }): Promise<void>;
  service: AiService;
  trustedCredential(): string | null;
}

export function requireAiWebRuntime(runtime: AiWebRuntime | undefined): AiWebRuntime {
  if (!runtime) throw new Error('AiWebRuntime is required');
  if (typeof runtime.baseUrl !== 'function') {
    throw new Error('AiWebRuntime.baseUrl is required');
  }
  if (typeof runtime.probeFrame !== 'function') {
    throw new Error('AiWebRuntime.probeFrame is required');
  }
  if (!runtime.service || typeof runtime.service.registerCurrentSnailUser !== 'function') {
    throw new Error('AiWebRuntime.service is required');
  }
  if (typeof runtime.trustedCredential !== 'function') {
    throw new Error('AiWebRuntime.trustedCredential is required');
  }
  return runtime;
}
