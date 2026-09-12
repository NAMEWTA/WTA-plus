import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({
  list: vi.fn(),
  request: vi.fn(),
  token: 'session-1' as string | undefined,
  setNotices: vi.fn(),
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

import { closePush, initMessageBox, initPush } from './push';

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
    ]);
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
    await initMessageBox();
    resolveOlder({ data: [{ messageId: 'old', category: 'system' }] });
    await first;
    expect(harness.setNotices).toHaveBeenCalledOnce();
    expect(harness.setNotices.mock.calls[0][0][0].messageId).toBe('new');
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
    await Promise.resolve();
    expect(harness.list).toHaveBeenCalledTimes(2);
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
