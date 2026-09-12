import { afterEach, describe, expect, it, vi } from 'vitest';
import type { AiWebRuntime } from './runtime';
import { createAiChatSession, type AiChatSnapshot } from './chatSession';

const runtime = (overrides: Partial<AiWebRuntime> = {}): AiWebRuntime => ({
  baseUrl: () => '/prod-api',
  probeFrame: vi.fn<AiWebRuntime['probeFrame']>(async () => undefined),
  service: {
    registerCurrentSnailUser: vi.fn(async () => ({ code: 200, data: { openId: 'open-user' } }))
  },
  trustedCredential: vi.fn(() => 'private-session-value'),
  ...overrides
});

afterEach(() => vi.useRealTimers());

describe('AI embedded chat lifecycle', () => {
  it('fails closed before registration when the host base URL is missing', async () => {
    const snapshots: AiChatSnapshot[] = [];
    const register = vi.fn();
    const session = createAiChatSession(
      runtime({ baseUrl: () => undefined, service: { registerCurrentSnailUser: register } }),
      snapshot => snapshots.push(snapshot)
    );

    await session.load();

    expect(register).not.toHaveBeenCalled();
    expect(snapshots.at(-1)).toEqual({
      error: 'AI 服务地址不存在，请联系管理员',
      frameUrl: '',
      loading: false
    });
  });

  it('fails closed with a stable message when the base URL provider throws', async () => {
    const register = vi.fn();
    const session = createAiChatSession(
      runtime({
        baseUrl: () => {
          throw new Error('sensitive host configuration');
        },
        service: { registerCurrentSnailUser: register }
      }),
      vi.fn()
    );

    await session.load();

    expect(register).not.toHaveBeenCalled();
    expect(session.snapshot()).toEqual({
      error: 'AI 服务地址不存在，请联系管理员',
      frameUrl: '',
      loading: false
    });
  });

  it.each([
    'https://attacker.example/prod-api',
    '//attacker.example/prod-api',
    '/prod-api?target=other',
    'javascript:alert(1)'
  ])('fails closed before registration for an untrusted base URL: %s', async baseUrl => {
    const snapshots: AiChatSnapshot[] = [];
    const register = vi.fn();
    const probeFrame = vi.fn();
    const session = createAiChatSession(
      runtime({ baseUrl: () => baseUrl, probeFrame, service: { registerCurrentSnailUser: register } }),
      snapshot => snapshots.push(snapshot)
    );

    await session.load();

    expect(register).not.toHaveBeenCalled();
    expect(probeFrame).not.toHaveBeenCalled();
    expect(snapshots.at(-1)).toEqual({
      error: 'AI 服务地址不存在，请联系管理员',
      frameUrl: '',
      loading: false
    });
  });

  it('fails closed before registration when the trusted credential is missing', async () => {
    const snapshots: AiChatSnapshot[] = [];
    const register = vi.fn();
    const session = createAiChatSession(
      runtime({ service: { registerCurrentSnailUser: register }, trustedCredential: () => null }),
      snapshot => snapshots.push(snapshot)
    );

    await session.load();

    expect(register).not.toHaveBeenCalled();
    expect(snapshots.at(-1)).toEqual({
      error: '登录凭证不存在，请重新登录后再试',
      frameUrl: '',
      loading: false
    });
  });

  it('maps registration failure and a missing identity to stable terminal states', async () => {
    const register = vi
      .fn()
      .mockRejectedValueOnce(new Error('registration transport with sensitive context'))
      .mockResolvedValueOnce({ code: 200, data: {} });
    const session = createAiChatSession(runtime({ service: { registerCurrentSnailUser: register } }), vi.fn());

    await session.load();
    expect(session.snapshot()).toEqual({
      error: '加载 AI 聊天失败，请稍后重试',
      frameUrl: '',
      loading: false
    });

    await session.load();
    expect(session.snapshot()).toEqual({
      error: '获取 AI 用户身份失败',
      frameUrl: '',
      loading: false
    });
  });

  it('publishes the credential URL only after a successful probe and keeps loading until the iframe loads', async () => {
    const snapshots: AiChatSnapshot[] = [];
    let finishProbe!: () => void;
    const probeFrame = vi.fn<AiWebRuntime['probeFrame']>(() => new Promise<void>(resolve => (finishProbe = resolve)));
    const session = createAiChatSession(runtime({ probeFrame }), snapshot => snapshots.push(snapshot));

    const loading = session.load();
    await vi.waitFor(() => expect(probeFrame).toHaveBeenCalledOnce());
    expect(session.snapshot()).toEqual({ error: '', frameUrl: '', loading: true });

    const probe = probeFrame.mock.calls[0][0];
    const probeUrl = new URL(probe.url, 'https://admin.example.test');
    expect(probe.signal).toBeInstanceOf(AbortSignal);
    expect(probeUrl.pathname).toBe('/prod-api/snail-chat/');
    expect(probeUrl.searchParams.get('openId')).toBe('open-user');
    expect(probeUrl.searchParams.has('trustedCredential')).toBe(true);
    finishProbe();
    await loading;

    const pending = snapshots.at(-1)!;
    expect(pending.loading).toBe(true);
    expect(pending.error).toBe('');
    const url = new URL(pending.frameUrl, 'https://admin.example.test');
    expect(url.pathname).toBe('/prod-api/snail-chat/');
    expect(url.searchParams.get('openId')).toBe('open-user');
    expect(url.searchParams.has('trustedCredential')).toBe(true);

    session.frameLoaded();
    expect(snapshots.at(-1)?.loading).toBe(false);

    session.dispose();
    expect(session.snapshot().frameUrl).toBe('');
  });

  it('keeps a failed probe URL private and succeeds on an immediate retry', async () => {
    const snapshots: AiChatSnapshot[] = [];
    const register = vi.fn(async () => ({ code: 200, data: { openId: 'retry-user' } }));
    const probeFrame = vi
      .fn<AiWebRuntime['probeFrame']>()
      .mockRejectedValueOnce(new Error('probe rejected with sensitive context'))
      .mockResolvedValueOnce(undefined);
    const session = createAiChatSession(
      runtime({ probeFrame, service: { registerCurrentSnailUser: register } }),
      snapshot => snapshots.push(snapshot)
    );

    await session.load();
    expect(snapshots.at(-1)).toEqual({
      error: 'AI 聊天连接中断，请重新加载',
      frameUrl: '',
      loading: false
    });

    await session.load();
    expect(snapshots.at(-1)?.frameUrl).not.toBe('');
    expect(register).toHaveBeenCalledTimes(2);
    expect(probeFrame).toHaveBeenCalledTimes(2);
    session.frameLoaded();
    expect(snapshots.at(-1)?.loading).toBe(false);
  });

  it('clears a credential-bearing URL when the iframe load times out', async () => {
    vi.useFakeTimers();
    const snapshots: AiChatSnapshot[] = [];
    const session = createAiChatSession(runtime(), snapshot => snapshots.push(snapshot));

    await session.load();
    expect(session.snapshot().frameUrl).not.toBe('');

    await vi.advanceTimersByTimeAsync(15000);

    expect(snapshots.at(-1)).toEqual({
      error: 'AI 聊天连接中断，请重新加载',
      frameUrl: '',
      loading: false
    });
  });

  it('aborts a hanging probe on timeout and allows an immediate successful retry', async () => {
    vi.useFakeTimers();
    let hangingSignal!: AbortSignal;
    const probeFrame = vi
      .fn<AiWebRuntime['probeFrame']>()
      .mockImplementationOnce(({ signal }) => {
        hangingSignal = signal;
        return new Promise<void>(() => undefined);
      })
      .mockResolvedValueOnce(undefined);
    const register = vi.fn(async () => ({ code: 200, data: { openId: 'retry-user' } }));
    const session = createAiChatSession(
      runtime({ probeFrame, service: { registerCurrentSnailUser: register } }),
      vi.fn()
    );

    const timedOutLoad = session.load();
    await vi.advanceTimersByTimeAsync(15000);
    await timedOutLoad;

    expect(hangingSignal.aborted).toBe(true);
    expect(session.snapshot()).toEqual({
      error: 'AI 聊天连接中断，请重新加载',
      frameUrl: '',
      loading: false
    });

    await session.load();
    expect(register).toHaveBeenCalledTimes(2);
    expect(probeFrame).toHaveBeenCalledTimes(2);
    expect(session.snapshot().frameUrl).not.toBe('');
    session.frameLoaded();
    expect(session.snapshot().loading).toBe(false);
  });

  it('does not let a stale registration overwrite a newer retry', async () => {
    let resolveFirst!: (value: { code: number; data: { openId: string } }) => void;
    const register = vi
      .fn()
      .mockImplementationOnce(() => new Promise(resolve => (resolveFirst = resolve)))
      .mockResolvedValueOnce({ code: 200, data: { openId: 'current-user' } });
    const session = createAiChatSession(runtime({ service: { registerCurrentSnailUser: register } }), vi.fn());

    const first = session.load();
    await session.load();
    resolveFirst({ code: 200, data: { openId: 'stale-user' } });
    await first;

    const url = new URL(session.snapshot().frameUrl, 'https://admin.example.test');
    expect(url.searchParams.get('openId')).toBe('current-user');
  });

  it('aborts an obsolete probe so it cannot publish a stale credential URL', async () => {
    let firstProbeSignal!: AbortSignal;
    let finishFirstProbe!: () => void;
    const probeFrame = vi
      .fn<AiWebRuntime['probeFrame']>()
      .mockImplementationOnce(({ signal }: { signal: AbortSignal }) => {
        firstProbeSignal = signal;
        return new Promise<void>(resolve => (finishFirstProbe = resolve));
      })
      .mockResolvedValueOnce(undefined);
    const register = vi
      .fn()
      .mockResolvedValueOnce({ code: 200, data: { openId: 'stale-user' } })
      .mockResolvedValueOnce({ code: 200, data: { openId: 'current-user' } });
    const session = createAiChatSession(
      runtime({ probeFrame, service: { registerCurrentSnailUser: register } }),
      vi.fn()
    );

    const first = session.load();
    await vi.waitFor(() => expect(probeFrame).toHaveBeenCalledOnce());
    await session.load();
    expect(firstProbeSignal.aborted).toBe(true);
    finishFirstProbe();
    await first;

    const url = new URL(session.snapshot().frameUrl, 'https://admin.example.test');
    expect(url.searchParams.get('openId')).toBe('current-user');
  });
});
