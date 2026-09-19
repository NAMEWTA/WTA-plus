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
    };

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


describe('malformed auth context transport', () => {
  it.each([null, undefined, [], 'context', { clientEnabled: 'true' }, { registerEnabled: null }])('rejects invalid optional flags %j', value => {
    expect(() => projectClientAuthContextTransport(value)).toThrow('客户端认证配置不可用');
  });
  it('keeps absent policy optional but rejects an invalid policy', () => {
    expect(projectClientAuthContextTransport({})).toEqual({ clientEnabled: false, registerEnabled: false });
    expect(() => projectClientAuthContextTransport({ passwordPolicy: null })).toThrow();
  });
});
