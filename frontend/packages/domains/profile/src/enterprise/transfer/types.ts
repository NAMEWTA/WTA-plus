import type { StatusProbe } from '../../types';

export interface EnterpriseTransferResult extends StatusProbe {
  status: 'QUEUED' | 'TRANSFERRED' | 'NOT_AVAILABLE' | 'EXPIRED' | 'FAILED' | 'UNBOUND';
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
