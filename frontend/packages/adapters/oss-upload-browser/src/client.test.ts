import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createOssUploadClient } from './client';
import type { OssUploadGateway, OssResumeStore } from './types';

const file = () => new File([new Uint8Array([1, 2, 3, 4, 5, 6])], 'archive.bin', { type: 'application/octet-stream' });
const signedPart = (partNumber: number, suffix = 'a') => ({
  partNumber,
  method: 'PUT',
  url: `https://oss.test/part-${partNumber}-${suffix}`,
  requiredHeaders: {},
  expiresAt: '2099-01-01T00:00:00Z'
});

function createFixture() {
  const gateway: OssUploadGateway = {
    abortUpload: vi.fn(),
    completeUpload: vi.fn(),
    delete: vi.fn(),
    downloadUrl: vi.fn(),
    initUpload: vi.fn(),
    listByIds: vi.fn(),
    resumeUpload: vi.fn(),
    signParts: vi.fn()
  };
  const records = new Map<string, { fingerprint: string; uploadToken: string; expiresAt: string; fileName: string; fileSize: number; contentType: string }>();
  const resumeStore: OssResumeStore = {
    get: vi.fn(key => Promise.resolve(records.get(key))),
    put: vi.fn(record => {
      records.set(record.fingerprint, record);
      return Promise.resolve();
    }),
    remove: vi.fn(key => {
      records.delete(key);
      return Promise.resolve();
    })
  };
  const transfer = vi.fn(async () => 'etag');
  const client = createOssUploadClient({
    clientId: 'admin-web',
    fingerprint: vi.fn(async () => 'fingerprint'),
    gateway,
    getToken: () => 'token',
    resumeStore,
    transfer
  });
  return { client, gateway, records, resumeStore, transfer };
}

describe('OSS browser upload adapter', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('resolves private previews independently and keeps references when an address is unavailable', async () => {
    const fixture = createFixture();
    vi.mocked(fixture.gateway.listByIds).mockResolvedValue({ data: [
      { ossId: '1', originalName: 'public.txt', url: 'https://oss.test/public' },
      { ossId: '2', originalName: 'private.txt', url: '' },
      { ossId: '3', originalName: 'unavailable.txt', url: '' }
    ] });
    vi.mocked(fixture.gateway.downloadUrl).mockResolvedValueOnce({ data: { url: 'https://oss.test/private' } })
      .mockRejectedValueOnce(new Error('address unavailable'));
    await expect(fixture.client.resolve(['1', '2', '3'])).resolves.toEqual([
      { id: '1', name: 'public.txt', url: 'https://oss.test/public' },
      { id: '2', name: 'private.txt', url: 'https://oss.test/private' },
      { id: '3', name: 'unavailable.txt', url: '' }
    ]);
    expect(vi.mocked(fixture.gateway.downloadUrl).mock.calls).toEqual([['2'], ['3']]);
    expect(fixture.gateway.initUpload).not.toHaveBeenCalled();
    expect(fixture.gateway.delete).not.toHaveBeenCalled();
  });

  it('keeps a completed upload when preview resolution fails without creating an unowned Blob URL', async () => {
    const fixture = createFixture();
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:unowned');
    vi.mocked(fixture.gateway.initUpload).mockResolvedValue({
      data: { uploadToken: 'preview-failure', mode: 'SINGLE', expiresAt: '2099-01-01T00:00:00Z', presignedRequest: signedPart(1) }
    });
    vi.mocked(fixture.gateway.completeUpload).mockResolvedValue({ data: '9004' });
    vi.mocked(fixture.gateway.downloadUrl).mockRejectedValue(new Error('owned URL service unavailable'));

    await expect(fixture.client.upload(file(), { signal: new AbortController().signal })).resolves.toEqual({
      id: '9004', name: 'archive.bin', url: ''
    });
    expect(createObjectURL).not.toHaveBeenCalled();
    expect(fixture.gateway.completeUpload).toHaveBeenCalledOnce();
    expect(fixture.transfer).toHaveBeenCalledOnce();
    expect(fixture.gateway.abortUpload).not.toHaveBeenCalled();
    expect(fixture.gateway.delete).not.toHaveBeenCalled();
    expect(fixture.records.size).toBe(0);
  });

  it('uploads multipart files and maps the result to the shared client contract', async () => {
    const fixture = createFixture();
    vi.mocked(fixture.gateway.initUpload).mockResolvedValue({
      data: { uploadToken: 'upload-1', mode: 'MULTIPART', expiresAt: '2099-01-01T00:00:00Z', partSize: 3, partCount: 2 }
    });
    vi.mocked(fixture.gateway.signParts)
      .mockResolvedValueOnce({ data: { parts: [signedPart(1), signedPart(2)] } })
      .mockResolvedValueOnce({ data: { parts: [signedPart(1, 'retry')] } });
    vi.mocked(fixture.transfer)
      .mockRejectedValueOnce(new Error('expired signature'))
      .mockResolvedValueOnce('etag-2')
      .mockResolvedValueOnce('etag-1');
    vi.mocked(fixture.gateway.completeUpload).mockResolvedValue({ data: '9001' });
    vi.mocked(fixture.gateway.downloadUrl).mockResolvedValue({ data: { url: 'https://oss.test/download' } });

    await expect(fixture.client.upload(file(), { signal: new AbortController().signal })).resolves.toEqual({
      id: '9001',
      name: 'archive.bin',
      url: 'https://oss.test/download'
    });
    expect(fixture.gateway.signParts).toHaveBeenNthCalledWith(2, 'upload-1', [1]);
    expect(fixture.gateway.completeUpload).toHaveBeenCalledWith('upload-1', [
      { partNumber: 1, eTag: 'etag-1' },
      { partNumber: 2, eTag: 'etag-2' }
    ]);
  });

  it('reconciles a completed resume session without sending file data', async () => {
    const fixture = createFixture();
    const record = {
      fingerprint: 'resume-key',
      uploadToken: 'upload-2',
      expiresAt: '2099-01-01T00:00:00Z',
      fileName: 'archive.bin',
      fileSize: 6,
      contentType: 'application/octet-stream'
    };
    vi.mocked(fixture.resumeStore.get).mockResolvedValue(record);
    vi.mocked(fixture.gateway.resumeUpload).mockResolvedValue({
      data: {
        uploadToken: 'upload-2',
        mode: 'SINGLE',
        state: 'COMPLETED',
        completedOssId: '9002',
        fileName: 'archive.bin',
        fileSize: 6,
        contentType: 'application/octet-stream',
        partSize: 0,
        partCount: 0,
        expiresAt: '2099-01-01T00:00:00Z',
        uploadedParts: []
      }
    });
    vi.mocked(fixture.gateway.downloadUrl).mockResolvedValue({ data: { url: 'https://oss.test/download-2' } });

    await expect(fixture.client.upload(file(), { signal: new AbortController().signal })).resolves.toMatchObject({ id: '9002' });
    expect(fixture.transfer).not.toHaveBeenCalled();
    expect(fixture.gateway.completeUpload).not.toHaveBeenCalled();
  });

  it('rejects a completion response without an identifier', async () => {
    const fixture = createFixture();
    vi.mocked(fixture.gateway.initUpload).mockResolvedValue({
      data: { uploadToken: 'upload-invalid', mode: 'SINGLE', expiresAt: '2099-01-01T00:00:00Z', presignedRequest: signedPart(1) }
    });
    vi.mocked(fixture.gateway.completeUpload).mockResolvedValue({ data: null });

    await expect(fixture.client.upload(file(), { signal: new AbortController().signal })).rejects.toThrow('完成 OSS 上传失败');
    expect(fixture.gateway.downloadUrl).not.toHaveBeenCalled();
  });

  it('restarts from initialization when a stored session is stale', async () => {
    const fixture = createFixture();
    vi.mocked(fixture.resumeStore.get).mockResolvedValue({
      fingerprint: 'resume-key',
      uploadToken: 'expired-upload',
      expiresAt: '2099-01-01T00:00:00Z',
      fileName: 'archive.bin',
      fileSize: 6,
      contentType: 'application/octet-stream'
    });
    vi.mocked(fixture.gateway.resumeUpload).mockRejectedValue(new Error('上传会话不存在'));
    vi.mocked(fixture.gateway.initUpload).mockResolvedValue({
      data: { uploadToken: 'fresh-upload', mode: 'SINGLE', expiresAt: '2099-01-01T00:00:00Z', presignedRequest: signedPart(1) }
    });
    vi.mocked(fixture.gateway.completeUpload).mockResolvedValue({ data: '9003' });
    vi.mocked(fixture.gateway.downloadUrl).mockResolvedValue({ data: { url: 'https://oss.test/download-3' } });

    await expect(fixture.client.upload(file(), { signal: new AbortController().signal })).resolves.toMatchObject({ id: '9003' });
    expect(fixture.gateway.initUpload).toHaveBeenCalledOnce();
  });
});
