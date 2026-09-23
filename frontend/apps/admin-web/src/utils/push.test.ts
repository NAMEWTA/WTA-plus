import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({
  list: vi.fn(),
  request: vi.fn(),
  token: 'session-1' as string | undefined,
  beginLoad: vi.fn(),
  setNotices: vi.fn(),
  failLoad: vi.fn(),
  clearNotice: vi.fn(),
  notification: vi.fn(),
  start: vi.fn().mockResolvedValue(undefined),
  close: vi.fn(),
  options: undefined as
    | undefined
    | { onMessage(raw: string): void; onConnected(): void; requestTicket(): Promise<string> }
}));

vi.mock('@/application/http', () => ({ adminHttp: { request: harness.request } }));
vi.mock('@/application/services', () => ({ notificationService: { inbox: { list: harness.list } } }));
vi.mock('@/application/session', () => ({ getToken: () => harness.token }));
vi.mock('@/store/modules/notice', () => ({ useNoticeStore: () => harness }));
vi.mock('element-plus', () => ({ ElNotification: harness.notification }));
vi.mock('@/utils/push-connection', () => ({
  createPushConnection: (options: NonNullable<typeof harness.options>) => {
    harness.options = options;
    return { start: harness.start, close: harness.close };
  },
  createPushUrl: vi.fn()
}));

import { closePush, initMessageBox, initPush, refreshMessageInbox } from './push';

describe('message box synchronization', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    harness.token = 'session-1';
    harness.list.mockResolvedValue({ data: [] });
    harness.request.mockResolvedValue({ data: 'ticket' });
    vi.stubEnv('VITE_APP_MESSAGE_ENABLED', 'true');
    vi.stubEnv('VITE_APP_MESSAGE_TRANSPORT', 'sse');
    vi.stubEnv('VITE_APP_MESSAGE_PATH', '/resource/message');
    vi.stubGlobal('window', Object.assign(new EventTarget(), { location: { origin: 'http://localhost:5175' } }));
    vi.stubGlobal('document', Object.assign(new EventTarget(), { visibilityState: 'visible' }));
  });
  afterEach(() => {
    closePush();
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  it('loads the authoritative inbox fields and server read state', async () => {
    harness.list.mockResolvedValue({
      data: [
        {
          messageId: '9000000000000000001',
          category: 'notice',
          title: '公告',
          content: '内容',
          createTime: '2026-09-05 20:00:00',
          readTime: null
        }
      ]
    });
    await initMessageBox();
    expect(harness.setNotices).toHaveBeenCalledWith([
      expect.objectContaining({
        messageId: '9000000000000000001',
        category: 'notice',
        title: '公告',
        content: '内容',
        read: false
      })
    ], 'session-1');
  });

  it('reads the inbox with realtime disabled but never opens a push connection', async () => {
    vi.stubEnv('VITE_APP_MESSAGE_ENABLED', 'false');
    await initMessageBox();
    await initPush();
    expect(harness.list).toHaveBeenCalledOnce();
    expect(harness.beginLoad).toHaveBeenCalledWith('session-1');
    expect(harness.clearNotice).not.toHaveBeenCalled();
    expect(harness.start).not.toHaveBeenCalled();
  });

  it('clears the inbox only when the token is absent', async () => {
    vi.stubEnv('VITE_APP_MESSAGE_ENABLED', 'false');
    harness.token = undefined;
    await initMessageBox();
    expect(harness.clearNotice).toHaveBeenCalledOnce();
    expect(harness.list).not.toHaveBeenCalled();
  });

  it('discards an older response when an event refresh finishes first', async () => {
    let resolveOlder: (result: unknown) => void = () => undefined;
    harness.list.mockImplementationOnce(
      () =>
        new Promise(resolve => {
          resolveOlder = resolve;
        })
    );
    const first = initMessageBox();
    harness.list.mockResolvedValueOnce({ data: [{ messageId: 'new', category: 'system' }] });
    const second = refreshMessageInbox();
    resolveOlder({ data: [{ messageId: 'old', category: 'system' }] });
    await Promise.all([first, second]);
    expect(harness.setNotices).toHaveBeenCalledOnce();
    expect(harness.setNotices.mock.calls[0][0][0].messageId).toBe('new');
  });

  it('coalesces simultaneous passive loads for one session', async () => {
    let resolveList: (result: unknown) => void = () => undefined;
    harness.list.mockImplementationOnce(() => new Promise(resolve => { resolveList = resolve; }));
    const first = initMessageBox();
    const second = initMessageBox();
    expect(harness.list).toHaveBeenCalledOnce();
    resolveList({ data: [] });
    await Promise.all([first, second]);
    expect(harness.setNotices).toHaveBeenCalledOnce();
  });

  it('ignores both late success and late failure from the previous identity', async () => {
    let resolveA: (result: unknown) => void = () => undefined;
    harness.list.mockImplementationOnce(() => new Promise(resolve => { resolveA = resolve; }));
    const oldSuccess = initMessageBox();
    harness.token = 'session-2';
    harness.list.mockResolvedValueOnce({ data: [{ messageId: 'b', category: 'notice' }] });
    await initMessageBox();
    resolveA({ data: [{ messageId: 'a', category: 'notice' }] });
    await oldSuccess;
    expect(harness.setNotices).toHaveBeenCalledOnce();
    expect(harness.setNotices.mock.calls[0][0][0].messageId).toBe('b');

    let rejectA: (error: Error) => void = () => undefined;
    harness.token = 'session-3';
    harness.list.mockImplementationOnce(() => new Promise((_, reject) => { rejectA = reject; }));
    const oldFailure = initMessageBox();
    harness.token = 'session-4';
    harness.list.mockResolvedValueOnce({ data: [{ messageId: 'd', category: 'notice' }] });
    await initMessageBox();
    rejectA(new Error('old session failed'));
    await oldFailure;
    expect(harness.failLoad).not.toHaveBeenCalled();
    expect(harness.setNotices.mock.calls.at(-1)?.[0][0].messageId).toBe('d');
  });

  it('surfaces the active session failure so the box can retry', async () => {
    harness.list.mockRejectedValueOnce(new Error('network failed'));
    await initMessageBox();
    expect(harness.failLoad).toHaveBeenCalledWith('session-1');
    await initMessageBox();
    expect(harness.setNotices).toHaveBeenCalledOnce();
  });

  it('ignores a response from a session that has signed out', async () => {
    let resolveList: (result: unknown) => void = () => undefined;
    harness.list.mockImplementationOnce(
      () =>
        new Promise(resolve => {
          resolveList = resolve;
        })
    );
    const loading = initMessageBox();
    closePush();
    harness.token = undefined;
    resolveList({ data: [{ messageId: 'old', category: 'system' }] });
    await loading;
    expect(harness.setNotices).not.toHaveBeenCalled();
  });

  it('refreshes on both connection recovery and realtime event without inserting a second local record', async () => {
    const updated = vi.fn();
    window.addEventListener('notify:inbox-updated', updated);
    await initPush();
    harness.options?.onConnected();
    harness.options?.onMessage('{"type":"message","data":{"notificationId":"1","title":"公告"}}');
    await vi.waitFor(() => expect(harness.list).toHaveBeenCalledTimes(2));
    expect(updated).toHaveBeenCalledTimes(2);
    expect(harness.notification).toHaveBeenCalledWith(expect.objectContaining({ title: '公告' }));
  });

  it('requests tickets through the App HTTP adapter and stops on kicked', async () => {
    await initPush();
    expect(await harness.options?.requestTicket()).toBe('ticket');
    expect(harness.request).toHaveBeenCalledWith({ url: '/resource/message/ticket', method: 'get', timeout: 10000 });
    harness.options?.onMessage('kicked');
    expect(harness.close).toHaveBeenCalledOnce();
    expect(harness.list).not.toHaveBeenCalled();
  });

  it('uses the configured message endpoint when requesting a ticket', async () => {
    vi.stubEnv('VITE_APP_MESSAGE_PATH', '/custom/push');
    await initPush();
    await harness.options?.requestTicket();
    expect(harness.request).toHaveBeenCalledWith(expect.objectContaining({ url: '/custom/push/ticket' }));
  });
});
