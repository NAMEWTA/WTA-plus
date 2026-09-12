export type {
  ClientAuthContext,
  IdentityAccessManagementService,
  IdentityAccessServiceOptions,
  IdentityInfo,
  IdentitySession,
  PasswordLoginInput,
  RegistrationInput,
  SocialCallbackInput,
  SocialCallbackResult,
  ServerMenuMeta,
  ServerMenuNode
} from './types';
export { createClientSessionKey, createIdentityAccessService, IdentityAccessError } from '../index';

export const adminAuthResource = Object.freeze({ controller: 'AuthController', basePath: '/auth' });
