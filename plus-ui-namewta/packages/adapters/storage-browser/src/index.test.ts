import { describe, expect, it } from 'vitest';
import { createBrowserSessionStore, createBrowserTokenStorage } from './index';

describe('browser token storage', () => {
  it('retains compatibility in a runtime without localStorage', () => {
    const tokens = createBrowserTokenStorage({ key: 'Admin-Token' });
    expect(tokens.get()).toBeNull();
    tokens.set('server-render-token');
    expect(tokens.get()).toBe('server-render-token');
    tokens.remove();
    expect(tokens.get()).toBeNull();
  });

  it('preserves the configured key and isolates namespaces', () => {
    const values = new Map<string, string>();
    const storage = {
      getItem: (key: string) => values.get(key) ?? null,
      removeItem: (key: string) => void values.delete(key),
      setItem: (key: string, value: string) => void values.set(key, value)
    };
    const admin = createBrowserTokenStorage({ key: 'Admin-Token', storage });
    const client = createBrowserTokenStorage({ key: 'Client-Token', storage });
    admin.set('admin-token');
    client.set('client-token');
    expect(admin.get()).toBe('admin-token');
    admin.remove();
    expect(admin.get()).toBeNull();
    expect(client.get()).toBe('client-token');
  });

  it('treats a successful null read as an authoritative external removal', () => {
    const values = new Map<string, string>();
    const storage = {
      getItem: (key: string) => values.get(key) ?? null,
      removeItem: (key: string) => void values.delete(key),
      setItem: (key: string, value: string) => void values.set(key, value)
    };
    const tokens = createBrowserTokenStorage({ key: 'Admin-Token', storage });
    tokens.set('access-token');
    values.delete('Admin-Token');

    expect(tokens.get()).toBeNull();
    expect(tokens.get()).toBeNull();
  });

  it('exposes the platform session contract through the same namespace', () => {
    const values = new Map<string, string>();
    const storage = {
      getItem: (key: string) => values.get(key) ?? null,
      removeItem: (key: string) => void values.delete(key),
      setItem: (key: string, value: string) => void values.set(key, value)
    };
    const session = createBrowserSessionStore({ key: 'Admin-Token', storage });
    session.setToken('access-token');
    expect(session.getToken()).toBe('access-token');
    session.clear();
    expect(session.getToken()).toBeNull();
  });

  it('falls back when browser storage throws security or quota errors', () => {
    const storage = {
      getItem: () => {
        throw new DOMException('blocked', 'SecurityError');
      },
      removeItem: () => {
        throw new DOMException('blocked', 'SecurityError');
      },
      setItem: () => {
        throw new DOMException('full', 'QuotaExceededError');
      }
    };
    const tokens = createBrowserTokenStorage({ key: 'Admin-Token', storage });

    expect(tokens.get()).toBeNull();
    expect(() => tokens.set('fallback-token')).not.toThrow();
    expect(tokens.get()).toBe('fallback-token');
    expect(() => tokens.remove()).not.toThrow();
    expect(tokens.get()).toBeNull();
  });
});
