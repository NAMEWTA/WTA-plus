import { describe, expect, it } from 'vitest';
import {
  parsePasswordPolicy,
  requirePasswordPolicy,
  validatePassword,
  type ClientAuthContext,
  type PasswordPolicyTransport
} from './password-policy';

const transport = (overrides: Partial<PasswordPolicyTransport> = {}): PasswordPolicyTransport => ({
  minimumLength: 8,
  maximumLength: 30,
  requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'],
  allowedSpecialCharacters: '@$!%*?&',
  ...overrides
});

describe('public password policy', () => {
  it('projects and freezes only the public contract fields', () => {
    const policy = parsePasswordPolicy({
      ...transport(),
      mode: 'FIXED',
      fixedValue: 'must-not-cross-the-boundary',
      generator: { length: 24 }
    });

    expect(policy).toEqual(transport());
    expect(Object.isFrozen(policy)).toBe(true);
    expect(Object.isFrozen(policy.requiredCharacterClasses)).toBe(true);
    expect(policy).not.toHaveProperty('mode');
    expect(policy).not.toHaveProperty('fixedValue');
    expect(policy).not.toHaveProperty('generator');
  });

  it.each([
    {},
    transport({ minimumLength: 0 }),
    transport({ minimumLength: 31 }),
    transport({ maximumLength: 7 }),
    transport({ requiredCharacterClasses: ['UPPERCASE', 'UNKNOWN'] as never[] }),
    transport({ requiredCharacterClasses: ['UPPERCASE', 'UPPERCASE'] }),
    transport({ requiredCharacterClasses: ['SPECIAL'], allowedSpecialCharacters: '' }),
    transport({ allowedSpecialCharacters: 'A!' })
  ])('fails closed for malformed public policy: %j', value => {
    expect(() => parsePasswordPolicy(value)).toThrow('密码策略配置不可用');
  });

  it('returns violations in the backend wire order without echoing the submitted password', () => {
    const policy = parsePasswordPolicy(transport());
    const submitted = '汉字';
    const violations = validatePassword(policy, submitted);

    expect(violations.map(item => item.reason)).toEqual([
      'PASSWORD_TOO_SHORT',
      'PASSWORD_MISSING_UPPERCASE',
      'PASSWORD_MISSING_LOWERCASE',
      'PASSWORD_MISSING_DIGIT',
      'PASSWORD_MISSING_SPECIAL',
      'PASSWORD_CONTAINS_DISALLOWED_CHARACTER'
    ]);
    expect(JSON.stringify(violations)).not.toContain(submitted);
  });

  it('accepts only ASCII letters, digits and the server-published special characters', () => {
    const policy = parsePasswordPolicy(transport());

    expect(validatePassword(policy, 'ValidT05!9')).toEqual([]);
    expect(validatePassword(policy, 'ValidT05#9').map(item => item.reason)).toEqual([
      'PASSWORD_MISSING_SPECIAL',
      'PASSWORD_CONTAINS_DISALLOWED_CHARACTER'
    ]);
  });

  it('requires consumers to fail closed when an enabled context has no policy', () => {
    expect(() => requirePasswordPolicy({ clientEnabled: true, registerEnabled: true } as ClientAuthContext)).toThrow(
      '密码策略配置不可用'
    );
  });
});
