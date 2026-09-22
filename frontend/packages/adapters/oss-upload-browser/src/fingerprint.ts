import { sha256Hex } from './sha256';

const SAMPLE_SIZE = 1024 * 1024;

export async function createOssFileFingerprint(file: File): Promise<string> {
  const first = file.slice(0, Math.min(SAMPLE_SIZE, file.size));
  const last = file.slice(Math.max(0, file.size - SAMPLE_SIZE), file.size);
  const [firstDigest, lastDigest] = await Promise.all([
    sha256Hex(await first.arrayBuffer()),
    sha256Hex(await last.arrayBuffer())
  ]);
  const identity = `${file.name}\n${file.size}\n${file.lastModified}\n${firstDigest}\n${lastDigest}`;
  return `v1:${await sha256Hex(identity)}`;
}
