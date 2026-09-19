import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { effectScope } from 'vue';
import type { UploadRequestOptions } from 'element-plus';
import { useUserImport } from './useUserImport';

function options(): UploadRequestOptions {
  return { action: '#', method: 'post', headers: {}, data: {}, filename: 'file', withCredentials: false,
    file: Object.assign(new File(['owned workbook'], 'users.xlsx'), { uid: 1 }),
    onProgress: vi.fn(), onSuccess: vi.fn(), onError: vi.fn() };
}

describe('user import dialog lifetime', () => {
  beforeEach(() => vi.stubGlobal('XMLHttpRequest', class { abort = vi.fn(); }));
  afterEach(() => vi.unstubAllGlobals());

  for (const message of ['owned network failure', 'owned business failure', '401']) {
    it(`resets after ${message} and can retry with current updateSupport`, async () => {
      const send = vi.fn().mockRejectedValueOnce(new Error(message)).mockResolvedValueOnce('导入成功');
      let updateSupport = false;
      const scope = effectScope(); const state = scope.run(() => useUserImport(send, () => updateSupport))!;
      const first = options(); state.request(first);
      await vi.waitFor(() => expect(state.busy.value).toBe(false));
      expect(first.onError).toHaveBeenCalledOnce(); expect(first.onSuccess).not.toHaveBeenCalled();
      expect(state.errorMessage.value).toBe(message);
      updateSupport = true; const second = options(); state.request(second);
      await vi.waitFor(() => expect(second.onSuccess).toHaveBeenCalledWith({ message: '导入成功' }));
      expect(send.mock.calls[1]?.[1]).toBe(true); expect(state.busy.value).toBe(false); scope.stop();
    });
  }

  it('sends once and cancelling an old request cannot settle a new import', async () => {
    let finish!: (value: string) => void;
    const send = vi.fn().mockImplementationOnce(() => new Promise<string>(resolve => { finish = resolve; })).mockResolvedValue('new result');
    const scope = effectScope(); const state = scope.run(() => useUserImport(send, () => false))!;
    const old = options(); state.request(old); state.request(options()); expect(send).toHaveBeenCalledOnce();
    state.cancel(); expect(state.busy.value).toBe(false); expect(send.mock.calls[0]?.[2].aborted).toBe(true);
    const next = options(); state.request(next); finish('old result');
    await vi.waitFor(() => expect(next.onSuccess).toHaveBeenCalledOnce());
    expect(old.onSuccess).not.toHaveBeenCalled(); expect(old.onError).not.toHaveBeenCalled(); scope.stop();
  });

  it('unmount aborts the request and suppresses late callbacks', async () => {
    let finish!: (value: string) => void;
    const send = vi.fn(() => new Promise<string>(resolve => { finish = resolve; }));
    const scope = effectScope(); const state = scope.run(() => useUserImport(send, () => false))!;
    const callbacks = options(); state.request(callbacks); scope.stop(); finish('late'); await Promise.resolve();
    expect(state.busy.value).toBe(false); expect(callbacks.onSuccess).not.toHaveBeenCalled();
  });
});
