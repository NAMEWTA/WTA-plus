import { describe, expect, it } from 'vitest';
import { createTransportError, isHandledError, isTransportError, normalizeTransportMessage } from './index';

describe('platform http errors', () => {
  it('keeps handled and network error semantics stable', () => {
    const cause = Object.assign(new Error('socket closed'), {
      code: 'ECONNRESET',
      config: { headers: { Authorization: 'Bearer secret-token' } }
    });
    const error = createTransportError({
      kind: 'network',
      message: 'failed',
      code: 'ECONNRESET',
      cause,
      handled: true
    });
    expect(isHandledError(error)).toBe(true);
    expect(isTransportError(error)).toBe(true);
    expect(error).toMatchObject({
      kind: 'network',
      code: 'ECONNRESET',
      cause: { code: 'ECONNRESET', message: 'socket closed', name: 'Error' },
      isHandled: true
    });
    expect(JSON.stringify(error.cause)).not.toContain('secret-token');
    expect(createTransportError({ kind: 'business', message: 'not presented' }).isHandled).toBe(false);
    expect(normalizeTransportMessage('Network Error')).toBe('后端接口连接异常');
    expect(normalizeTransportMessage('timeout of 50000ms exceeded')).toBe('系统接口请求超时');
  });
});


describe('handled error narrowing', () => {
  it.each([null, undefined, 1, 'failure', { isHandled: true }, { isHandled: 'false' }])('does not narrow foreign values into TransportError: %j', value => {
    expect(isHandledError(value)).toBe(false);
  });
});
