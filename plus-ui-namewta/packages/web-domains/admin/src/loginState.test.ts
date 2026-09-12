import type { IdentityAccessService } from '@namewta/domain-admin';
import { describe, expect, it, vi } from 'vitest';
import { createIdentityLoginState } from './loginState';

const deferred = <T>() => {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(done => {
    resolve = done;
  });
  return { promise, resolve };
};

describe('identity login presentation state', () => {
  it('keeps login disabled while a captcha refresh is pending and replaces the challenge on success', async () => {
    const refreshed = deferred<Awaited<ReturnType<IdentityAccessService['prepareLogin']>>>();
    const prepareLogin = vi
      .fn<IdentityAccessService['prepareLogin']>()
      .mockResolvedValueOnce({
        context: { clientEnabled: true, registerEnabled: false },
        verification: { captchaEnabled: true, img: 'first-image', uuid: 'first-uuid' }
      })
      .mockReturnValueOnce(refreshed.promise);
    const state = createIdentityLoginState({
      service: { prepareLogin } as unknown as IdentityAccessService,
      onAuthenticated: vi.fn()
    });

    await state.prepare();
    expect(state.ready.value).toBe(true);
    expect(state.captchaImage.value).toBe('data:image/gif;base64,first-image');
    state.form.code = 'old-code';

    const refresh = state.prepare();
    expect(state.ready.value).toBe(false);
    expect(state.preparing.value).toBe(true);
    expect(state.form.code).toBe('');

    refreshed.resolve({
      context: { clientEnabled: true, registerEnabled: false },
      verification: { captchaEnabled: true, img: 'second-image', uuid: 'second-uuid' }
    });
    await refresh;

    expect(prepareLogin).toHaveBeenCalledTimes(2);
    expect(state.ready.value).toBe(true);
    expect(state.preparing.value).toBe(false);
    expect(state.verification.value?.uuid).toBe('second-uuid');
    expect(state.captchaImage.value).toBe('data:image/gif;base64,second-image');
  });

  it('stays disabled and exposes a safe terminal error when preparation fails', async () => {
    const state = createIdentityLoginState({
      service: {
        prepareLogin: vi.fn().mockRejectedValue(new Error('登录验证码配置不可用'))
      } as unknown as IdentityAccessService,
      onAuthenticated: vi.fn()
    });

    await state.prepare();

    expect(state.ready.value).toBe(false);
    expect(state.preparing.value).toBe(false);
    expect(state.errorMessage.value).toBe('登录验证码配置不可用');
  });

  it('clears a previously usable challenge when refresh fails', async () => {
    const prepareLogin = vi
      .fn<IdentityAccessService['prepareLogin']>()
      .mockResolvedValueOnce({
        context: { clientEnabled: true, registerEnabled: false },
        verification: { captchaEnabled: true, img: 'first-image', uuid: 'first-uuid' }
      })
      .mockRejectedValueOnce(new Error('登录验证码配置不可用'));
    const state = createIdentityLoginState({
      service: { prepareLogin } as unknown as IdentityAccessService,
      onAuthenticated: vi.fn()
    });

    await state.prepare();
    await state.prepare();

    expect(state.ready.value).toBe(false);
    expect(state.verification.value).toBeUndefined();
    expect(state.captchaImage.value).toBe('');
    expect(state.errorMessage.value).toBe('登录验证码配置不可用');
  });
});
