import { createHash } from 'node:crypto';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createOssFileFingerprint } from './fingerprint';
import { sha256Hex } from './sha256';

const vectors = ['', 'abc', 'hello OSS'];

function nodeHex(value: string | Uint8Array) {
  return createHash('sha256').update(value).digest('hex');
}

describe('sha256', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('matches the platform digest when subtle is available', async () => {
    for (const value of vectors) expect(await sha256Hex(value)).toBe(nodeHex(value));
    const bytes = new TextEncoder().encode('bytes');
    expect(await sha256Hex(bytes)).toBe(nodeHex(bytes));
  });

  it('matches the platform digest on an insecure origin without subtle', async () => {
    vi.stubGlobal('crypto', { getRandomValues: crypto.getRandomValues.bind(crypto) });
    for (const value of vectors) expect(await sha256Hex(value)).toBe(nodeHex(value));
    const file = new File(['abc'], 'a.txt', { lastModified: 1 });
    const withoutSubtle = await createOssFileFingerprint(file);
    vi.unstubAllGlobals();
    expect(withoutSubtle).toBe(await createOssFileFingerprint(file));
    expect(withoutSubtle.startsWith('v1:')).toBe(true);
  });
});
