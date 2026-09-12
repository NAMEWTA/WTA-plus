import type { ThirdService } from '@namewta/domain-third';

export interface ThirdWebRuntime {
  service: ThirdService;
  confirm?: (message: string) => Promise<void>;
  success?: (message: string) => void;
  error?: (message: string) => void;
}
