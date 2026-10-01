import { safeSsoReturnTo } from './index';

export interface SocialTransaction {
  clientId: string;
  providerKey: string;
  purpose: 'LOGIN' | 'BIND';
  returnPath: string;
  state: string;
  transactionKey: string;
  expiresAt: number;
  owner: string;
}
export interface SocialTransactionStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

/** 事务按 App/client/state 隔离，回调先消费再换票，禁止凭当前 token 猜测绑定意图。 */
export function createSocialTransactionStore(storage: SocialTransactionStorage, namespace: string, now = Date.now) {
  const key = (state: string) => `${namespace}:social:${encodeURIComponent(state)}`;
  return {
    save(transaction: SocialTransaction) {
      storage.setItem(
        key(transaction.state),
        JSON.stringify({ ...transaction, returnPath: safeSsoReturnTo(transaction.returnPath) })
      );
    },
    consume(search: string, clientId: string): { transaction: SocialTransaction; code: string } {
      const params = new URLSearchParams(search);
      const state = params.get('state') ?? '';
      const value = storage.getItem(key(state));
      storage.removeItem(key(state));
      if (
        !value ||
        !state ||
        params.getAll('state').length !== 1 ||
        params.getAll('code').length !== 1 ||
        params.has('error')
      )
        throw new Error('授权已过期或已取消，请重新登录。');
      const unknownValue: unknown = JSON.parse(value);
      if (!unknownValue || typeof unknownValue !== 'object' || Array.isArray(unknownValue))
        throw new Error('授权事务无效');
      const row = unknownValue as Record<string, unknown>;
      if (
        row.state !== state ||
        row.clientId !== clientId ||
        !['LOGIN', 'BIND'].includes(String(row.purpose)) ||
        typeof row.providerKey !== 'string' ||
        typeof row.transactionKey !== 'string' ||
        !row.transactionKey ||
        typeof row.returnPath !== 'string' ||
        typeof row.owner !== 'string' ||
        typeof row.expiresAt !== 'number' ||
        row.expiresAt <= now() ||
        !params.get('code')
      )
        throw new Error('授权事务无效或已过期，请重新登录。');
      return {
        transaction: {
          clientId,
          state,
          providerKey: row.providerKey,
          transactionKey: row.transactionKey,
          returnPath: safeSsoReturnTo(row.returnPath),
          owner: row.owner,
          expiresAt: row.expiresAt,
          purpose: row.purpose as 'LOGIN' | 'BIND'
        },
        code: params.get('code') ?? ''
      };
    },
    saveLogout(state: string) {
      storage.setItem(`${namespace}:logout`, JSON.stringify({ state, expiresAt: now() + 300000 }));
    },
    consumeLogout(search: string): boolean {
      const raw = storage.getItem(`${namespace}:logout`);
      storage.removeItem(`${namespace}:logout`);
      if (!raw) return false;
      try {
        const value: unknown = JSON.parse(raw);
        if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
        const row = value as Record<string, unknown>;
        const params = new URLSearchParams(search);
        return (
          typeof row.expiresAt === 'number' &&
          row.expiresAt > now() &&
          typeof row.state === 'string' &&
          params.getAll('state').length === 1 &&
          params.get('state') === row.state &&
          !params.has('error')
        );
      } catch {
        return false;
      }
    }
  };
}
