import { describe, expect, it } from 'vitest';
import { projectClientAuthContextTransport, type ClientAuthContextTransport } from './transport';

describe('identity OpenAPI transport boundary', () => {
  it('keeps domain booleans explicit when optional transport fields are absent', () => {
    const transport: ClientAuthContextTransport = { clientEnabled: true };
    expect(projectClientAuthContextTransport(transport)).toEqual({ clientEnabled: true, registerEnabled: false });
  });

  it('strictly projects the public password policy and drops internal fields', () => {
    const transport = {
      clientEnabled: true,
      registerEnabled: true,
      passwordPolicy: {
        minimumLength: 8,
        maximumLength: 30,
        requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'],
        allowedSpecialCharacters: '@$!%*?&',
        fixedValue: 'must-not-cross-the-boundary',
        mode: 'FIXED'
      }
    } as unknown as ClientAuthContextTransport;

    expect(projectClientAuthContextTransport(transport)).toEqual({
      clientEnabled: true,
      registerEnabled: true,
      passwordPolicy: {
        minimumLength: 8,
        maximumLength: 30,
        requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'],
        allowedSpecialCharacters: '@$!%*?&'
      }
    });
  });
});
