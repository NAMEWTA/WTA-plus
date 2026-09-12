export type PushTransport = 'sse' | 'websocket';

interface PushConnectionOptions {
  transport: PushTransport;
  requestTicket(): Promise<string>;
  createUrl(ticket: string): string;
  onMessage(message: string): void;
  onConnected(): void;
  onError(error: unknown): void;
}

export function createPushUrl(baseApi: string, path: string, origin: string, ticket: string, transport: PushTransport) {
  const url = new URL(`${baseApi}${path}`, origin);
  url.searchParams.set('ticket', ticket);
  if (transport === 'websocket') url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  return url.toString();
}

/** 一次性票据只能建连一次；每次断线都关闭原连接，重新申请票据后恢复。 */
export function createPushConnection(options: PushConnectionOptions) {
  let generation = 0;
  let stopped = true;
  let retryDelay = 1000;
  let retryTimer: ReturnType<typeof setTimeout> | undefined;
  let closeTransport: (() => void) | undefined;

  const active = (current: number) => !stopped && generation === current;
  const disconnect = () => {
    closeTransport?.();
    closeTransport = undefined;
  };
  const close = () => {
    stopped = true;
    generation++;
    clearTimeout(retryTimer);
    retryTimer = undefined;
    disconnect();
  };
  const retry = (current: number, error: unknown) => {
    if (!active(current) || retryTimer !== undefined) return;
    disconnect();
    options.onError(error);
    retryTimer = setTimeout(() => {
      retryTimer = undefined;
      void connect(current);
    }, retryDelay);
    retryDelay = Math.min(retryDelay * 2, 30000);
  };
  const connected = (current: number) => {
    if (!active(current)) return;
    retryDelay = 1000;
    options.onConnected();
  };

  const connect = async (current: number) => {
    try {
      const ticket = await options.requestTicket();
      // 注销或重新初始化期间返回的票据不得恢复旧会话。
      if (!active(current)) return;
      const url = options.createUrl(ticket);
      if (options.transport === 'sse') {
        const source = new EventSource(url);
        closeTransport = () => {
          source.onopen = null;
          source.onmessage = null;
          source.onerror = null;
          source.close();
        };
        source.onopen = () => connected(current);
        source.onmessage = event => {
          if (active(current)) options.onMessage(event.data);
        };
        // 禁用浏览器对旧 URL 的隐式重连，避免重复消费已失效票据。
        source.onerror = error => retry(current, error);
        return;
      }

      const socket = new WebSocket(url);
      let heartbeatTimer: ReturnType<typeof setInterval> | undefined;
      let pongTimer: ReturnType<typeof setTimeout> | undefined;
      closeTransport = () => {
        clearInterval(heartbeatTimer);
        clearTimeout(pongTimer);
        socket.onopen = null;
        socket.onmessage = null;
        socket.onclose = null;
        socket.onerror = null;
        socket.close();
      };
      socket.onopen = () => {
        connected(current);
        heartbeatTimer = setInterval(() => {
          if (socket.readyState !== WebSocket.OPEN) return;
          socket.send('ping');
          pongTimer = setTimeout(() => retry(current, new Error('推送心跳超时')), 2000);
        }, 10000);
      };
      socket.onmessage = event => {
        clearTimeout(pongTimer);
        const message = String(event.data);
        if (active(current) && message !== 'pong') options.onMessage(message);
      };
      socket.onclose = event => retry(current, event);
      socket.onerror = error => retry(current, error);
    } catch (error) {
      retry(current, error);
    }
  };

  return {
    async start() {
      close();
      stopped = false;
      retryDelay = 1000;
      await connect(generation);
    },
    close
  };
}
