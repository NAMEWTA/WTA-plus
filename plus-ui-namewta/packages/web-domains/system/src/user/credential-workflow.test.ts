import { describe, expect, it } from 'vitest';
import type { SystemPasswordPolicy } from '../runtime';
import {
  createCredentialState,
  describePasswordViolations,
  resetCredentialState,
  type CredentialState
} from './credential-workflow';

const policy: SystemPasswordPolicy = {
  minimumLength: 8,
  maximumLength: 20,
  requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'],
  allowedSpecialCharacters: '!@#'
};

describe('user credential workflow state', () => {
  it('clears every sensitive reference when a dialog closes', () => {
    const state: CredentialState = createCredentialState();
    Object.assign(state, {
      candidate: 'Candidate9!',
      confirmation: 'Candidate9!',
      temporaryPassword: 'Temporary9!',
      expiresInSeconds: 60,
      resetVisible: true,
      temporaryVisible: true
    });

    resetCredentialState(state);

    expect(state).toEqual(createCredentialState());
  });

  it('maps stable policy violations without exposing internal policy configuration', () => {
    expect(
      describePasswordViolations(policy, [
        { reason: 'PASSWORD_TOO_SHORT' },
        { reason: 'PASSWORD_MISSING_SPECIAL' },
        { reason: 'PASSWORD_CONTAINS_DISALLOWED_CHARACTER' }
      ])
    ).toEqual(['密码长度不能少于 8 位', '密码必须包含特殊字符', '密码包含不允许的字符，可用特殊字符：!@#']);
  });
});
