import type { OidcService } from '@namewta/domain-oidc';

export interface OidcWebRuntime {
  service: OidcService;
  hasPermission(permission: string): boolean;
  confirm(message: string): Promise<void>;
  copyText(value: string): Promise<void>;
  success(message: string): void;
}
