import { beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({
  axios: vi.fn(),
  close: vi.fn(),
  error: vi.fn(),
  extractErrorMessage: vi.fn(async () => ''),
  saveBlob: vi.fn()
}));

vi.mock('axios', () => ({ default: harness.axios }));
vi.mock('element-plus', () => ({
  ElLoading: { service: vi.fn(() => ({ close: harness.close })) },
  ElMessage: { error: harness.error }
}));
vi.mock('@/application/services', () => ({
  systemService: { resources: { oss: { downloadUrl: vi.fn() } } }
}));
vi.mock('@/application/http', () => ({
  extractErrorMessage: harness.extractErrorMessage,
  globalHeaders: () => ({ Authorization: 'redacted' })
}));
vi.mock('@/utils/save', () => ({ saveBlob: harness.saveBlob }));

import download, { isZipPayload } from './download';

const writeU16 = (view: DataView, offset: number, value: number) => view.setUint16(offset, value, true);
const writeU32 = (view: DataView, offset: number, value: number) => view.setUint32(offset, value, true);
const emptyZip = () => {
  const bytes = new Uint8Array(22);
  const view = new DataView(bytes.buffer);
  writeU32(view, 0, 0x06054b50);
  return bytes;
};
const singleEntryZip = () => {
  const bytes = new Uint8Array(98);
  const view = new DataView(bytes.buffer);
  writeU32(view, 0, 0x04034b50);
  writeU32(view, 30, 0x02014b50);
  writeU32(view, 72, 0);
  writeU32(view, 76, 0x06054b50);
  writeU16(view, 84, 1);
  writeU16(view, 86, 1);
  writeU32(view, 88, 46);
  writeU32(view, 92, 30);
  return bytes;
};
const truncatedEntryDataZip = () => {
  const bytes = singleEntryZip();
  const view = new DataView(bytes.buffer);
  writeU32(view, 18, 1);
  writeU32(view, 50, 1);
  return bytes;
};
const overlappingEntriesZip = () => {
  const bytes = new Uint8Array(144);
  const view = new DataView(bytes.buffer);
  writeU32(view, 0, 0x04034b50);
  writeU32(view, 30, 0x02014b50);
  writeU32(view, 72, 0);
  writeU32(view, 76, 0x02014b50);
  writeU32(view, 118, 0);
  writeU32(view, 122, 0x06054b50);
  writeU16(view, 130, 2);
  writeU16(view, 132, 2);
  writeU32(view, 134, 92);
  writeU32(view, 138, 30);
  return bytes;
};
const dataDescriptorZip = () => {
  const bytes = new Uint8Array(114);
  const view = new DataView(bytes.buffer);
  writeU32(view, 0, 0x04034b50);
  writeU16(view, 6, 0x0008);
  writeU32(view, 30, 0x08074b50);
  writeU32(view, 46, 0x02014b50);
  writeU16(view, 54, 0x0008);
  writeU32(view, 88, 0);
  writeU32(view, 92, 0x06054b50);
  writeU16(view, 100, 1);
  writeU16(view, 102, 1);
  writeU32(view, 104, 46);
  writeU32(view, 108, 46);
  return bytes;
};
const missingDataDescriptorZip = () => {
  const bytes = singleEntryZip();
  const view = new DataView(bytes.buffer);
  writeU16(view, 6, 0x0008);
  writeU16(view, 38, 0x0008);
  return bytes;
};
const signaturelessDataDescriptorZip = (crc = 0) => {
  const bytes = new Uint8Array(110);
  const view = new DataView(bytes.buffer);
  writeU32(view, 0, 0x04034b50);
  writeU16(view, 6, 0x0008);
  writeU32(view, 30, crc);
  writeU32(view, 42, 0x02014b50);
  writeU16(view, 50, 0x0008);
  writeU32(view, 58, crc);
  writeU32(view, 84, 0);
  writeU32(view, 88, 0x06054b50);
  writeU16(view, 96, 1);
  writeU16(view, 98, 1);
  writeU32(view, 100, 46);
  writeU32(view, 104, 42);
  return bytes;
};
const truncatedDataDescriptorZip = () => {
  const bytes = new Uint8Array(106);
  const view = new DataView(bytes.buffer);
  writeU32(view, 0, 0x04034b50);
  writeU16(view, 6, 0x0008);
  writeU32(view, 38, 0x02014b50);
  writeU16(view, 46, 0x0008);
  writeU32(view, 80, 0);
  writeU32(view, 84, 0x06054b50);
  writeU16(view, 92, 1);
  writeU16(view, 94, 1);
  writeU32(view, 96, 46);
  writeU32(view, 100, 38);
  return bytes;
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe('ZIP download safety', () => {
  it.each([
    [
      'json',
      new Blob([JSON.stringify({ code: 500, msg: '<b>生成失败</b>\u0000token' })], { type: 'application/json' })
    ],
    ['html', new Blob(['<html>gateway error</html>'], { type: 'text/html' })],
    ['plain text', new Blob(['gateway error'], { type: 'text/plain' })],
    ['truncated signature', new Blob([new Uint8Array([0x50, 0x4b, 0x03])], { type: 'application/zip' })],
    [
      'local header only',
      new Blob([new Uint8Array(30).fill(0).map((value, index) => [0x50, 0x4b, 0x03, 0x04][index] ?? value)])
    ],
    ['data descriptor only', new Blob([new Uint8Array([0x50, 0x4b, 0x07, 0x08, 0, 0, 0, 0])])],
    ['truncated EOCD', new Blob([emptyZip().slice(0, 21)])],
    ['truncated file data', new Blob([truncatedEntryDataZip()])],
    ['overlapping local entries', new Blob([overlappingEntriesZip()])],
    ['missing data descriptor', new Blob([missingDataDescriptorZip()])],
    ['truncated data descriptor', new Blob([truncatedDataDescriptorZip()])]
  ])('rejects %s without saving a corrupt file', async (_label, payload) => {
    harness.axios.mockResolvedValue({ data: payload });

    await download.zip('/downloads/archive.zip', 'archive.zip');

    expect(harness.saveBlob).not.toHaveBeenCalled();
    expect(harness.error).toHaveBeenCalledTimes(1);
    expect(harness.close).toHaveBeenCalledTimes(1);
    const message = String(harness.error.mock.calls[0][0]);
    expect(message).not.toContain('<');
    expect(message).not.toContain('>');
    expect([...message].every(character => character.charCodeAt(0) >= 32)).toBe(true);
  });

  it('saves a payload with a valid ZIP local-file signature', async () => {
    const payload = new Blob([singleEntryZip()]);
    harness.axios.mockResolvedValue({ data: payload });

    await download.zip('/downloads/archive.zip', 'archive.zip');

    expect(harness.saveBlob).toHaveBeenCalledWith(expect.any(Blob), 'archive.zip');
    expect(harness.error).not.toHaveBeenCalled();
    expect(harness.close).toHaveBeenCalledTimes(1);
  });

  it('sanitizes network errors and always closes loading', async () => {
    harness.axios.mockRejectedValue(new Error('transport-secret'));
    harness.extractErrorMessage.mockResolvedValue('<script>alert(1)</script> connection\u0000 failed');

    await download.zip('/downloads/archive.zip', 'archive.zip');

    expect(harness.saveBlob).not.toHaveBeenCalled();
    expect(harness.error).toHaveBeenCalledWith('alert(1) connection failed');
    expect(harness.close).toHaveBeenCalledTimes(1);
  });

  it('validates complete empty and non-empty ZIP structures and rejects arbitrary bytes', async () => {
    await expect(isZipPayload(emptyZip())).resolves.toBe(true);
    await expect(isZipPayload(singleEntryZip())).resolves.toBe(true);
    await expect(isZipPayload(dataDescriptorZip())).resolves.toBe(true);
    await expect(isZipPayload(signaturelessDataDescriptorZip())).resolves.toBe(true);
    await expect(isZipPayload(signaturelessDataDescriptorZip(0x08074b50))).resolves.toBe(true);
    await expect(isZipPayload(new Uint8Array([0x7b, 0x22, 0x78, 0x22]))).resolves.toBe(false);
    const brokenCentralDirectory = singleEntryZip();
    brokenCentralDirectory[30] = 0;
    await expect(isZipPayload(brokenCentralDirectory)).resolves.toBe(false);
    await expect(isZipPayload(truncatedEntryDataZip())).resolves.toBe(false);
    await expect(isZipPayload(overlappingEntriesZip())).resolves.toBe(false);
    await expect(isZipPayload(missingDataDescriptorZip())).resolves.toBe(false);
    await expect(isZipPayload(truncatedDataDescriptorZip())).resolves.toBe(false);
  });
});
