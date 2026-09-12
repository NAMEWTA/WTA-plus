import type { OssResumeRecord, OssResumeStore } from './types';

const DATABASE = 'wta-oss-upload';
const STORE = 'sessions';

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (!globalThis.indexedDB) {
      reject(new Error('IndexedDB unavailable'));
      return;
    }
    const request = globalThis.indexedDB.open(DATABASE, 1);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(STORE)) {
        request.result.createObjectStore(STORE, { keyPath: 'fingerprint' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function transact<T>(mode: IDBTransactionMode, operation: (store: IDBObjectStore) => IDBRequest<T>): Promise<T> {
  const database = await openDatabase();
  try {
    return await new Promise<T>((resolve, reject) => {
      const request = operation(database.transaction(STORE, mode).objectStore(STORE));
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  } finally {
    database.close();
  }
}

export function createIndexedDbResumeStore(): OssResumeStore {
  return {
    get: fingerprint => transact<OssResumeRecord | undefined>('readonly', store => store.get(fingerprint)),
    put: record => transact<IDBValidKey>('readwrite', store => store.put(record)),
    remove: fingerprint => transact<undefined>('readwrite', store => store.delete(fingerprint))
  };
}
