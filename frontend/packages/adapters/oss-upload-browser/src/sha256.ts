import { sha256Bytes } from '@namewta/platform-auth';
export { sha256Bytes } from '@namewta/platform-auth';

function toHex(bytes: ArrayBuffer | Uint8Array) {
  return Array.from(bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes), value =>
    value.toString(16).padStart(2, '0')
  ).join('');
}

function asBytes(data: ArrayBuffer | Uint8Array | string) {
  if (typeof data === 'string') return new TextEncoder().encode(data);
  if (data instanceof Uint8Array) return new Uint8Array(data);
  return new Uint8Array(data);
}

export async function sha256Hex(data: ArrayBuffer | Uint8Array | string) {
  const bytes = asBytes(data);
  const subtle = globalThis.crypto?.subtle;
  if (subtle) return toHex(await subtle.digest('SHA-256', bytes as BufferSource));
  return toHex(sha256Bytes(bytes));
}
