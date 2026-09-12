import type { StatusProbe } from '../../types';

export interface EnterpriseTransferResult extends StatusProbe {
  challengeId: string | null;
  expiresInSeconds: number | null;
}

export interface EnterpriseTransferSendCommand {
  documentLastFour: string;
  fullName: string;
  phone: string;
}

export interface EnterpriseTransferConfirmCommand {
  challengeId: string;
  code: string;
}
