import { createIdentityAccessService } from '@namewta/domain-admin';
import { describe, expect, it, vi } from 'vitest';
import { createSocialWebRuntime } from './socialRuntime';

function fixture() {
  const data = new Map<string, string>();
  let owner = '';
  let generation = 0;
  const request = vi.fn();
  const service = createIdentityAccessService({
    client: { clientId: 'home' },
    http: { request },
    identity: { loadInfo: vi.fn(), loadMenus: vi.fn() },
    session: {
      getToken: () => owner,
      setToken: value => {
        owner = value;
      },
      clear: () => {
        owner = '';
      }
    }
  });
  const acceptToken = vi.fn();
  const navigate = vi.fn().mockResolvedValue(undefined);
  const navigateExternal = vi.fn();
  const clearSession = vi.fn();
  const runtime = createSocialWebRuntime({
    service,
    namespace: 'home',
    storage: {
      getItem: key => data.get(key) ?? null,
      setItem: (key, value) => {
        data.set(key, value);
      },
      removeItem: key => {
        data.delete(key);
      }
    },
    defaultReturnPath: '/profile',
    bindingReturnPath: '/account/bindings',
    owner: () => owner,
    snapshot: () => String(generation) + owner,
    acceptToken,
    navigate,
    navigateExternal,
    clearSession
  });
  const authorize = async () => {
    request.mockResolvedValueOnce({
      data: { authorizationUrl: 'https://id.example/authorize', state: 's', transactionKey: 't', expiresIn: 300 }
    });
    await runtime.start('corp');
  };
  return {
    runtime,
    request,
    authorize,
    acceptToken,
    navigate,
    navigateExternal,
    clearSession,
    replaceSession: () => {
      generation++;
      owner = 'new-session';
    }
  };
}
describe('shared external login lifecycle', () => {
  it('keeps business authentication pending until required profile information is completed', async () => {
    const f = fixture();
    await f.authorize();
    f.request.mockResolvedValueOnce({
      data: { nextAction: 'COMPLETE_PROFILE', registrationTicket: 'ticket', requiredFields: ['phoneNumber'] }
    });
    expect(await f.runtime.callback('?state=s&code=c')).toMatchObject({ nextAction: 'COMPLETE_PROFILE' });
    expect(f.acceptToken).not.toHaveBeenCalled();
    expect(f.navigate).not.toHaveBeenCalled();
    f.request.mockResolvedValueOnce({
      data: { nextAction: 'LOGIN_COMPLETE', access_token: 'business', client_id: 'home' }
    });
    await f.runtime.register('13800138000');
    expect(f.acceptToken).toHaveBeenCalledWith('business');
    expect(f.navigate).toHaveBeenCalledWith('/profile');
  });
  it('guides conflicts to local login and then the account binding page', async () => {
    const f = fixture();
    await f.authorize();
    f.request.mockResolvedValueOnce({ data: { nextAction: 'BIND_REQUIRED', message: '请绑定原账号' } });
    await f.runtime.callback('?state=s&code=c');
    await f.runtime.returnToLogin();
    expect(f.acceptToken).not.toHaveBeenCalled();
    expect(f.navigate).toHaveBeenCalledWith('/login?redirect=%2Faccount%2Fbindings');
  });
  it.each(['session', 'unmount'])('ignores a late exchange after %s change', async reason => {
    const f = fixture();
    await f.authorize();
    let resolve!: (value: unknown) => void;
    f.request.mockReturnValueOnce(
      new Promise(done => {
        resolve = done;
      })
    );
    const pending = f.runtime.callback('?state=s&code=c');
    await vi.waitFor(() => expect(f.request).toHaveBeenCalledTimes(2));
    if (reason === 'session') f.replaceSession();
    else f.runtime.dispose();
    resolve({ data: { access_token: 'old-token', client_id: 'home' } });
    await expect(pending).rejects.toThrow();
    expect(f.acceptToken).not.toHaveBeenCalled();
    expect(f.navigate).not.toHaveBeenCalled();
  });
  it('prevents a pending login from replacing a session created in another tab', async () => {
    const f = fixture();
    await f.authorize();
    f.replaceSession();
    await expect(f.runtime.callback('?state=s&code=c')).rejects.toThrow();
    expect(f.request).toHaveBeenCalledTimes(1);
  });
});
