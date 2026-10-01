import { describe, expect, it } from 'vitest';
import { createSocialTransactionStore, type SocialTransaction } from './social-transaction';
function fixture() {
  const data = new Map<string, string>();
  const storage = {
    getItem: (key: string) => data.get(key) ?? null,
    setItem: (key: string, value: string) => {
      data.set(key, value);
    },
    removeItem: (key: string) => {
      data.delete(key);
    }
  };
  const store = createSocialTransactionStore(storage, 'home:client', () => 1000);
  const transaction: SocialTransaction = {
    state: 'state-A',
    transactionKey: 'private-key',
    providerKey: 'sso',
    clientId: 'home',
    purpose: 'LOGIN',
    returnPath: '/profile',
    expiresAt: 2000,
    owner: ''
  };
  return { store, storage, transaction };
}
describe('external login transactions', () => {
  it('consumes each state once and keeps independent attempts isolated', () => {
    const { store, transaction } = fixture();
    store.save(transaction);
    store.save({ ...transaction, state: 'state-B' });
    expect(store.consume('?code=once&state=state-A', 'home').transaction.transactionKey).toBe('private-key');
    expect(() => store.consume('?code=once&state=state-A', 'home')).toThrow();
    expect(store.consume('?code=other&state=state-B', 'home').code).toBe('other');
  });
  it.each(['?code=x&state=state-A&state=state-A', '?code=x&code=y&state=state-A', '?error=denied&state=state-A'])(
    'rejects malformed/cancelled callback %s',
    search => {
      const { store, transaction } = fixture();
      store.save(transaction);
      expect(() => store.consume(search, 'home')).toThrow();
      expect(() => store.consume('?code=x&state=state-A', 'home')).toThrow();
    }
  );
  it('rejects expired and mismatched clients and unsafe return destinations', () => {
    const { store, transaction } = fixture();
    store.save(transaction);
    expect(() => store.consume('?code=x&state=state-A', 'admin')).toThrow();
    store.save({ ...transaction, expiresAt: 999 });
    expect(() => store.consume('?code=x&state=state-A', 'home')).toThrow();
    expect(() => store.save({ ...transaction, returnPath: '//outside.example' })).toThrow();
  });
  it('validates logout callback state once without allowing login replay', () => {
    const { store } = fixture();
    store.saveLogout('logout-A');
    expect(store.consumeLogout('?state=logout-A')).toBe(true);
    expect(store.consumeLogout('?state=logout-A')).toBe(false);
    store.saveLogout('logout-B');
    expect(store.consumeLogout('?state=other')).toBe(false);
  });
});
