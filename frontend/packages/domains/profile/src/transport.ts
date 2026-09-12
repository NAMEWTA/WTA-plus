import type { ApiResponse, StatusProbe } from './types';
import { projectProfileResponse, requiredProfileString, strictProfileRecord } from './transport-support';

export { projectPersonMatch, projectPersonMatchResponse } from './person/rebind/transport';

export function projectStatusProbe(value: unknown): StatusProbe {
  const source = strictProfileRecord(value, ['status']);
  return Object.freeze({ status: requiredProfileString(source.status) });
}

export const projectStatusProbeResponse = (value: unknown): ApiResponse<StatusProbe> =>
  projectProfileResponse(value, projectStatusProbe);
