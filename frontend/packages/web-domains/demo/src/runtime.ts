import type { DemoService } from '@namewta/domain-demo';

export interface DemoWebRuntime {
  confirm(message: string): Promise<void>;
  download(url: string, params: unknown, fileName: string): Promise<void> | void;
  service: DemoService;
  success(message: string): void;
}

export function requireDemoWebRuntime(runtime: DemoWebRuntime | undefined): DemoWebRuntime {
  if (!runtime) throw new Error('DemoWebRuntime is required');
  return runtime;
}
