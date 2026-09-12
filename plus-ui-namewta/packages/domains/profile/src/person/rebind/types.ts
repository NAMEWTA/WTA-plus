import type { StatusProbe } from '../../types';

export interface PersonMatchResult extends StatusProbe {
  maskedPhone: string | null;
}

export interface PersonConfirmationResult extends PersonMatchResult {
  version: number;
}

export interface PersonSubmissionResult extends StatusProbe {
  snapshotVersion: number;
  version: number;
}
