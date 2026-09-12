import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPushConnection, createPushUrl } from './push-connection';

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  onopen: (() => void) | null = null;
  onmessage: ((event: { data: string }) => void) | null = null;
  onerror: ((error: unknown) => void) | null = null;
  close = vi.fn();
  constructor(readonly url: string) {
    FakeEventSource.instances.push(this);
  }
}

class FakeWebSocket {
  static OPEN = 1;
  static instances: FakeWebSocket[] = [];
  readyState = 1;
  onopen: (() => void) | null = null;
  onmessage: ((event: { data: string }) => void) | null = null;
  onclose: ((error: unknown) => void) | null = null;
  onerror: ((error: unknown) => void) | null = null;
  close = vi.fn();
  send = vi.fn();
  constructor(readonly url: string) {
    FakeWebSocket.instances.push(this);
  }
}

describe('push connection lifecycle', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    FakeEventSource.instances = [];
    FakeWebSocket.instances = [];
    vi.stubGlobal('EventSource', FakeEventSource);
    vi.stubGlobal('WebSocket', FakeWebSocket);
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  const options = () => ({
    transport: 'sse' as const,
    requestTicket: vi.fn().mockResolvedValueOnce('ticket-1').mockResolvedValue('ticket-2'),
    createUrl: (ticket: string) => `http://localhost/message?ticket=${ticket}`,
    onMessage: vi.fn(),
    onConnected: vi.fn(),
    onError: vi.fn()
  });

  it('requests a fresh ticket after SSE drops and closes the consumed URL', async () => {
    const settings = options();
    const connection = createPushConnection(settings);
    await connection.start();
    FakeEventSource.instances[0].onopen?.();
    FakeEventSource.instances[0].onerror?.(new Error('offline'));
    expect(FakeEventSource.instances[0].close).toHaveBeenCalledOnce();
    await vi.advanceTimersByTimeAsync(1000);
    expect(settings.requestTicket).toHaveBeenCalledTimes(2);
    expect(FakeEventSource.instances[1].url).toContain('ticket-2');
    FakeEventSource.instances[1].onopen?.();
    expect(settings.onConnected).toHaveBeenCalledTimes(2);
    connection.close();
  });

  it('cancels retry and ignores a ticket that arrives after logout', async () => {
    let resolveTicket: (ticket: string) => void = () => undefined;
    const connection = createPushConnection({
      ...options(),
      requestTicket: () =>
        new Promise(resolve => {
          resolveTicket = resolve;
        })
    });
    const started = connection.start();
    connection.close();
    resolveTicket('late-ticket');
    await started;
    expect(FakeEventSource.instances).toHaveLength(0);

    const settings = options();
    const retrying = createPushConnection(settings);
    await retrying.start();
    FakeEventSource.instances[0].onerror?.(new Error('offline'));
    retrying.close();
    await vi.advanceTimersByTimeAsync(30000);
    expect(settings.requestTicket).toHaveBeenCalledOnce();
  });

  it('retries a failed ticket request without opening a connection with an invalid ticket', async () => {
    const settings = options();
    settings.requestTicket.mockReset().mockRejectedValueOnce(new Error('network')).mockResolvedValue('recovered');
    const connection = createPushConnection(settings);
    await connection.start();
    expect(FakeEventSource.instances).toHaveLength(0);
    await vi.advanceTimersByTimeAsync(1000);
    expect(FakeEventSource.instances[0].url).toContain('recovered');
    connection.close();
  });

  it('renews a websocket ticket after heartbeat timeout and releases timers', async () => {
    const settings = { ...options(), transport: 'websocket' as const };
    const connection = createPushConnection(settings);
    await connection.start();
    const socket = FakeWebSocket.instances[0];
    socket.onopen?.();
    await vi.advanceTimersByTimeAsync(10000);
    expect(socket.send).toHaveBeenCalledWith('ping');
    await vi.advanceTimersByTimeAsync(3000);
    expect(socket.close).toHaveBeenCalledOnce();
    expect(FakeWebSocket.instances[1].url).toContain('ticket-2');
    connection.close();
    expect(vi.getTimerCount()).toBe(0);
  });

  it('ignores websocket pong while forwarding messages', async () => {
    const settings = { ...options(), transport: 'websocket' as const };
    const connection = createPushConnection(settings);
    await connection.start();
    FakeWebSocket.instances[0].onmessage?.({ data: 'pong' });
    FakeWebSocket.instances[0].onmessage?.({ data: '{"messageId":"1"}' });
    expect(settings.onMessage).toHaveBeenCalledExactlyOnceWith('{"messageId":"1"}');
    connection.close();
  });
});

describe('push URL', () => {
  it('builds a valid websocket URL from a relative API path', () => {
    expect(createPushUrl('/dev-api', '/resource/message', 'http://localhost:5175', 'ticket + 1', 'websocket')).toBe(
      'ws://localhost:5175/dev-api/resource/message?ticket=ticket+%2B+1'
    );
  });

  it('preserves the API origin and uses secure websocket transport for HTTPS', () => {
    expect(
      createPushUrl('https://api.example.com', '/resource/message', 'https://admin.example.com', 'ticket', 'websocket')
    ).toBe('wss://api.example.com/resource/message?ticket=ticket');
  });
});
