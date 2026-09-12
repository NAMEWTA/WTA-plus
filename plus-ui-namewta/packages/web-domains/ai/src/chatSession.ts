import type { AiWebRuntime } from './runtime';
import { requireAiWebRuntime } from './runtime';

export interface AiChatSnapshot {
  error: string;
  frameUrl: string;
  loading: boolean;
}

export interface AiChatSession {
  dispose(): void;
  frameLoaded(sourceUrl?: string): void;
  load(): Promise<void>;
  snapshot(): AiChatSnapshot;
}

const missingCredentialMessage = '登录凭证不存在，请重新登录后再试';
const missingBaseUrlMessage = 'AI 服务地址不存在，请联系管理员';
const missingIdentityMessage = '获取 AI 用户身份失败';
const registrationFailureMessage = '加载 AI 聊天失败，请稍后重试';
const frameFailureMessage = 'AI 聊天连接中断，请重新加载';
const frameLoadTimeoutMs = 15000;
const parserOrigin = 'https://app.namewta.invalid';

function parseSameOriginBasePath(input: unknown): string | undefined {
  if (typeof input !== 'string') return undefined;
  const candidate = input.trim();
  if (!candidate.startsWith('/') || candidate.startsWith('//')) return undefined;
  try {
    const parsed = new URL(candidate, parserOrigin);
    if (parsed.origin !== parserOrigin || parsed.username || parsed.password || parsed.search || parsed.hash) {
      return undefined;
    }
    const normalized = parsed.pathname.replace(/\/+$/, '');
    return normalized || '/';
  } catch {
    return undefined;
  }
}

function createFrameUrl(basePath: string, openId: string, credential: string): string {
  const params = new URLSearchParams({ openId, trustedCredential: credential });
  const prefix = basePath === '/' ? '' : basePath;
  return `${prefix}/snail-chat/?${params.toString()}`;
}

export function createAiChatSession(
  runtimeInput: AiWebRuntime | undefined,
  onChange: (snapshot: AiChatSnapshot) => void
): AiChatSession {
  const runtime = requireAiWebRuntime(runtimeInput);
  let attempt = 0;
  let activeProbe: AbortController | undefined;
  let cancelProbeWait: (() => void) | undefined;
  let disposed = false;
  let frameTimer: ReturnType<typeof setTimeout> | undefined;
  let state: AiChatSnapshot = { error: '', frameUrl: '', loading: false };

  const snapshot = (): AiChatSnapshot => ({ ...state });
  const publish = () => onChange(snapshot());
  const replaceState = (next: AiChatSnapshot) => {
    state = next;
    publish();
  };
  const matchesFrame = (sourceUrl?: string) => !sourceUrl || sourceUrl === state.frameUrl;
  const clearFrameTimer = () => {
    if (frameTimer !== undefined) clearTimeout(frameTimer);
    frameTimer = undefined;
  };
  const cancelPending = () => {
    activeProbe?.abort();
    activeProbe = undefined;
    cancelProbeWait?.();
    cancelProbeWait = undefined;
    clearFrameTimer();
  };

  return Object.freeze({
    async load() {
      const currentAttempt = ++attempt;
      cancelPending();
      replaceState({ error: '', frameUrl: '', loading: true });
      let basePath: string | undefined;
      try {
        basePath = parseSameOriginBasePath(runtime.baseUrl());
      } catch {
        basePath = undefined;
      }
      if (!basePath) {
        replaceState({ error: missingBaseUrlMessage, frameUrl: '', loading: false });
        return;
      }
      let credential: string | null = null;
      try {
        credential = runtime.trustedCredential();
      } catch {
        credential = null;
      }
      if (typeof credential !== 'string' || credential.trim() === '') {
        replaceState({ error: missingCredentialMessage, frameUrl: '', loading: false });
        return;
      }

      try {
        const response = await runtime.service.registerCurrentSnailUser();
        if (disposed || currentAttempt !== attempt) return;
        const openId = response.data?.openId;
        if (typeof openId !== 'string' || openId.trim() === '') {
          replaceState({ error: missingIdentityMessage, frameUrl: '', loading: false });
          return;
        }
        const frameUrl = createFrameUrl(basePath, openId, credential);
        const probe = new AbortController();
        activeProbe = probe;
        let settleProbeWait!: (outcome: 'cancelled' | 'timeout') => void;
        const lifecycleTimeout = new Promise<'cancelled' | 'timeout'>(resolve => {
          settleProbeWait = resolve;
        });
        const cancelCurrentProbeWait = () => settleProbeWait('cancelled');
        cancelProbeWait = cancelCurrentProbeWait;
        frameTimer = setTimeout(() => {
          if (disposed || currentAttempt !== attempt) {
            settleProbeWait('cancelled');
            return;
          }
          if (activeProbe === probe) {
            probe.abort();
            activeProbe = undefined;
          }
          if (cancelProbeWait === cancelCurrentProbeWait) cancelProbeWait = undefined;
          ++attempt;
          replaceState({ error: frameFailureMessage, frameUrl: '', loading: false });
          frameTimer = undefined;
          settleProbeWait('timeout');
        }, frameLoadTimeoutMs);

        const probeOutcome = await Promise.race([
          Promise.resolve()
            .then(() => runtime.probeFrame({ signal: probe.signal, url: frameUrl }))
            .then(
              () => 'success' as const,
              () => 'failure' as const
            ),
          lifecycleTimeout
        ]);
        if (cancelProbeWait === cancelCurrentProbeWait) cancelProbeWait = undefined;
        if (activeProbe === probe) activeProbe = undefined;
        if (probeOutcome === 'cancelled' || probeOutcome === 'timeout') return;
        if (probeOutcome === 'failure') {
          clearFrameTimer();
          if (disposed || currentAttempt !== attempt) return;
          replaceState({ error: frameFailureMessage, frameUrl: '', loading: false });
          return;
        }
        if (disposed || currentAttempt !== attempt) {
          clearFrameTimer();
          return;
        }
        replaceState({ error: '', frameUrl, loading: true });
      } catch {
        if (disposed || currentAttempt !== attempt) return;
        replaceState({ error: registrationFailureMessage, frameUrl: '', loading: false });
      }
    },
    frameLoaded(sourceUrl) {
      if (disposed || !state.frameUrl || !matchesFrame(sourceUrl)) return;
      clearFrameTimer();
      replaceState({ ...state, loading: false });
    },
    dispose() {
      disposed = true;
      ++attempt;
      cancelPending();
      state = { error: '', frameUrl: '', loading: false };
    },
    snapshot
  });
}
