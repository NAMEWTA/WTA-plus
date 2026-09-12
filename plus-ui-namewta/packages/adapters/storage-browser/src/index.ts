import type { SessionStore, TokenStorage } from '@namewta/platform-contracts';

export interface BrowserStorage {
  getItem(key: string): string | null;
  removeItem(key: string): void;
  setItem(key: string, value: string): void;
}

export interface BrowserTokenStorageOptions {
  key: string;
  storage?: BrowserStorage;
}

export function createBrowserTokenStorage({ key, storage }: BrowserTokenStorageOptions): TokenStorage {
  if (!key) throw new Error('Token storage key is required');
  let fallbackValue: string | null = null;
  const target = () => {
    if (storage) return storage;
    try {
      return globalThis.localStorage;
    } catch {
      return undefined;
    }
  };
  return {
    get: () => {
      const storageTarget = target();
      if (!storageTarget) return fallbackValue;
      try {
        const value = storageTarget.getItem(key);
        fallbackValue = value;
        return value;
      } catch {
        return fallbackValue;
      }
    },
    remove: () => {
      fallbackValue = null;
      try {
        target()?.removeItem(key);
      } catch {
        // The in-memory namespace remains cleared when browser storage is unavailable.
      }
    },
    set: value => {
      fallbackValue = value;
      try {
        target()?.setItem(key, value);
      } catch {
        // The current runtime can continue with the isolated in-memory value.
      }
    }
  };
}

export function createBrowserSessionStore(options: BrowserTokenStorageOptions): SessionStore {
  const tokens = createBrowserTokenStorage(options);
  return {
    clear: tokens.remove,
    getToken: tokens.get,
    setToken: tokens.set
  };
}
