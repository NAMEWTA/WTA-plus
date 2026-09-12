import { describe, expect, it } from 'vitest';
import {
  projectResetPasswordCandidateTransport,
  projectSystemUserTransport,
  projectTemporaryPasswordTransport,
  type ResetPasswordCandidateTransport,
  type SystemUserTransport,
  type TemporaryPasswordTransport
} from './transport';

describe('system OpenAPI transport boundary', () => {
  it('projects transport into the public user summary contract', () => {
    const transport: SystemUserTransport = { userId: 3, userName: 'demo', nickName: 'Demo' };
    expect(projectSystemUserTransport(transport)).toEqual({
      userId: 3,
      userName: 'demo',
      nickName: 'Demo',
      deptName: undefined,
      status: undefined
    });
  });

  it('narrows credential responses into domain-owned values', () => {
    expect(projectResetPasswordCandidateTransport({ password: 'Candidate9!' })).toEqual({
      password: 'Candidate9!'
    });
    expect(projectTemporaryPasswordTransport({ password: 'Temporary9!', expiresInSeconds: 60 })).toEqual({
      password: 'Temporary9!',
      expiresInSeconds: 60
    });
  });

  it.each([
    [projectResetPasswordCandidateTransport, {}],
    [projectResetPasswordCandidateTransport, { password: '' }],
    [projectTemporaryPasswordTransport, { password: 'Temporary9!' }],
    [projectTemporaryPasswordTransport, { password: 'Temporary9!', expiresInSeconds: 0 }],
    [projectTemporaryPasswordTransport, { password: '', expiresInSeconds: 60 }]
  ])('fails closed for malformed credential transport', (project, value) => {
    expect(() => project(value as never)).toThrow('用户凭据响应不可用');
  });

  it('keeps generated response types at the transport edge', () => {
    const candidate: ResetPasswordCandidateTransport = { password: 'Candidate9!' };
    const temporary: TemporaryPasswordTransport = { password: 'Temporary9!', expiresInSeconds: 60 };
    expect(candidate.password).toBeTruthy();
    expect(temporary.expiresInSeconds).toBe(60);
  });
});
