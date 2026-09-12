import { describe, expect, it } from 'vitest';
import { requireClientContext } from './index';

describe('requireClientContext', () => {
  it('fails closed instead of selecting a fallback client', () => {
    expect(() => requireClientContext({ clientId: '   ' })).toThrow('ClientContext.clientId is required');
  });
});
