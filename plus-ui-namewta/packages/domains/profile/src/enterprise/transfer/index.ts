export { createEnterpriseTransferService, type EnterpriseTransferService } from './service';
export type {
  EnterpriseTransferConfirmCommand,
  EnterpriseTransferResult,
  EnterpriseTransferSendCommand
} from './types';

export const profileEnterpriseTransferResource = Object.freeze({
  controller: 'EnterpriseTransferController',
  basePath: '/profile/enterprise/transfer'
});
