import type { SystemPasswordPolicy, SystemPasswordViolation } from '../runtime';

export interface CredentialState {
  candidate: string;
  confirmation: string;
  expiresInSeconds: number;
  resetVisible: boolean;
  temporaryPassword: string;
  temporaryVisible: boolean;
}

export function createCredentialState(): CredentialState {
  return {
    candidate: '',
    confirmation: '',
    expiresInSeconds: 0,
    resetVisible: false,
    temporaryPassword: '',
    temporaryVisible: false
  };
}

export function resetCredentialState(state: CredentialState): void {
  Object.assign(state, createCredentialState());
}

export function describePasswordViolations(
  policy: SystemPasswordPolicy,
  violations: readonly SystemPasswordViolation[]
): string[] {
  const messages: Record<SystemPasswordViolation['reason'], string> = {
    PASSWORD_TOO_SHORT: `密码长度不能少于 ${policy.minimumLength} 位`,
    PASSWORD_TOO_LONG: `密码长度不能超过 ${policy.maximumLength} 位`,
    PASSWORD_MISSING_UPPERCASE: '密码必须包含大写字母',
    PASSWORD_MISSING_LOWERCASE: '密码必须包含小写字母',
    PASSWORD_MISSING_DIGIT: '密码必须包含数字',
    PASSWORD_MISSING_SPECIAL: '密码必须包含特殊字符',
    PASSWORD_CONTAINS_DISALLOWED_CHARACTER: `密码包含不允许的字符，可用特殊字符：${policy.allowedSpecialCharacters}`
  };
  return violations.map(item => messages[item.reason]);
}
