import { describe, expect, it, beforeEach } from 'vitest';
import { getToken, removeToken, session } from './session';

describe('home session namespace', () => {
  beforeEach(() => removeToken());

  it('stores and clears the Home-Token independently', () => {
    session.setToken('home-token');
    expect(getToken()).toBe('home-token');

    removeToken();
    expect(getToken()).toBeNull();
  });
});
