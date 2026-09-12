import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ossUploadClient } from '@/application/services';
import { getDirectOssUploadErrorMessage, uploadDirectToOss } from './useDirectOssUpload';

vi.mock('@/application/services', () => ({
  ossUploadClient: {
    upload: vi.fn()
  }
}));

const upload = vi.mocked(ossUploadClient.upload);

describe('direct OSS upload compatibility facade', () => {
  beforeEach(() => vi.clearAllMocks());

  it('maps the shared upload result to the legacy ossId shape', async () => {
    upload.mockResolvedValue({ id: '9001', name: 'archive.bin', url: 'https://oss.test/download' });
    await expect(uploadDirectToOss(new File(['archive'], 'archive.bin'), { signal: new AbortController().signal })).resolves.toEqual({
      ossId: '9001',
      fileName: 'archive.bin',
      url: 'https://oss.test/download'
    });
  });

  it('keeps handled error suppression for legacy callers', () => {
    expect(getDirectOssUploadErrorMessage(Object.assign(new Error('服务端已提示'), { isHandled: true }), '上传文件失败')).toBeUndefined();
    expect(getDirectOssUploadErrorMessage('unknown failure', '上传文件失败')).toBe('上传文件失败');
  });
});
