import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { UploadClient, UploadResult } from '@namewta/platform-contracts';
import type { UploadRequestOptions } from 'element-plus';
import { createUploadRequest } from './upload-request';

function options(): UploadRequestOptions {
  return {
    action: '#', method: 'post', headers: {}, data: {}, filename: 'file', withCredentials: false,
    file: Object.assign(new File(['owned'], 'owned.txt'), { uid: 1 }),
    onProgress: vi.fn(), onSuccess: vi.fn(), onError: vi.fn()
  };
}
function fixture() {
  let complete!: (value: UploadResult) => void;
  let reject!: (error: unknown) => void;
  const pending = new Promise<UploadResult>((resolve, fail) => { complete = resolve; reject = fail; });
  const client: UploadClient = { upload: vi.fn(() => pending), resolve: vi.fn(), remove: vi.fn() };
  const deltas: number[] = [];
  const owner = new AbortController();
  const request = createUploadRequest(client, 'document', delta => deltas.push(delta), owner.signal);
  return { client, deltas, owner, request, complete, reject };
}

describe('upload request ownership', () => {
  beforeEach(() => vi.stubGlobal('XMLHttpRequest', class { abort = vi.fn(); }));
  afterEach(() => vi.unstubAllGlobals());

  it('reports upload success independently of an unavailable preview', async () => {
    const f = fixture(); const callbacks = options(); f.request(callbacks);
    f.complete({ id: 'owned-id', name: 'owned.txt', url: '' });
    await vi.waitFor(() => expect(callbacks.onSuccess).toHaveBeenCalledOnce());
    expect(f.deltas).toEqual([1, -1]); expect(callbacks.onError).not.toHaveBeenCalled();
  });

  it('cancels immediately and ignores completion from a transport that ignores abort', async () => {
    const f = fixture(); const callbacks = options(); const handle = f.request(callbacks) as XMLHttpRequest;
    handle.abort(); handle.abort();
    expect(f.deltas).toEqual([1, -1]);
    f.complete({ id: 'late', name: 'owned.txt', url: '' }); await Promise.resolve();
    expect(callbacks.onSuccess).not.toHaveBeenCalled(); expect(callbacks.onError).not.toHaveBeenCalled();
    expect(vi.mocked(f.client.upload).mock.calls[0]?.[1].signal.aborted).toBe(true);
  });

  it('scope disposal settles pending once and ignores late progress or failure', async () => {
    const f = fixture(); const callbacks = options(); f.request(callbacks);
    f.owner.abort();
    vi.mocked(f.client.upload).mock.calls[0]?.[1].onProgress?.(90);
    f.reject(new Error('late network')); await Promise.resolve();
    expect(f.deltas).toEqual([1, -1]); expect(callbacks.onProgress).not.toHaveBeenCalled();
    expect(callbacks.onError).not.toHaveBeenCalled();
  });

  it('settles a synchronous client failure with exactly one error callback', async () => {
    const f = fixture(); const callbacks = options();
    vi.mocked(f.client.upload).mockImplementation(() => { throw new Error('owned immediate failure'); });
    f.request(callbacks);
    expect(f.deltas).toEqual([1, -1]); expect(callbacks.onError).toHaveBeenCalledOnce();
    expect(callbacks.onSuccess).not.toHaveBeenCalled();
  });
});
