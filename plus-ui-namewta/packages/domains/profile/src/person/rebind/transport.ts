import type { ApiResponse } from '../../types';
import type { PersonMatchResult } from './types';
import {
  projectProfileResponse,
  requiredProfileString,
  strictProfileRecord,
  unavailableProfileResponse
} from '../../transport-support';

export function projectPersonMatch(value: unknown): PersonMatchResult {
  const source = strictProfileRecord(value, ['maskedPhone', 'status']);
  if (source.maskedPhone !== null && typeof source.maskedPhone !== 'string') return unavailableProfileResponse();
  return Object.freeze({
    status: requiredProfileString(source.status),
    maskedPhone: source.maskedPhone as string | null
  });
}

export const projectPersonMatchResponse = (value: unknown): ApiResponse<PersonMatchResult> =>
  projectProfileResponse(value, projectPersonMatch);
