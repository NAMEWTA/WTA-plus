const SAMPLE_SIZE = 1024 * 1024;

function toHex(bytes: ArrayBuffer) {
  return Array.from(new Uint8Array(bytes), value => value.toString(16).padStart(2, '0')).join('');
}

async function digest(blob: Blob) {
  return toHex(await crypto.subtle.digest('SHA-256', await blob.arrayBuffer()));
}

export async function createOssFileFingerprint(file: File): Promise<string> {
  const first = file.slice(0, Math.min(SAMPLE_SIZE, file.size));
  const last = file.slice(Math.max(0, file.size - SAMPLE_SIZE), file.size);
  const [firstDigest, lastDigest] = await Promise.all([digest(first), digest(last)]);
  const identity = `${file.name}\n${file.size}\n${file.lastModified}\n${firstDigest}\n${lastDigest}`;
  return `v1:${toHex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(identity)))}`;
}
