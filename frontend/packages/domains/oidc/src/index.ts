import type { DomainModule } from '@namewta/platform-app-runtime';

export const oidcDomainModule: DomainModule = Object.freeze({
  id: 'oidc',
  backendModules: ['wta-oidc'],
  capabilities: [
    'oidc-application-management',
    'identity-disclosure',
    'oidc-service-settings',
    'oidc-logout-deliveries'
  ]
});
export { createOidcService } from './service';
export type { OidcService } from './service';
export { applicationScopes, redirectLines, selectOrdinaryFields } from './types';
export type {
  OidcApplication,
  OidcApplicationInput,
  OidcApplicationPage,
  OidcApplicationQuery,
  OidcField,
  OidcProvider,
  OidcSecretDelivery
} from './types';

export * from './service-settings';
