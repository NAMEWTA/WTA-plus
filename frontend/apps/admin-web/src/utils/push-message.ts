export const PUSH_MESSAGE_TYPE = {
  MESSAGE: 'message',
  NOTICE: 'notice',
  LLM: 'llm',
  CUSTOM: 'custom'
} as const;

export const PUSH_MESSAGE_SOURCE = {
  BACKEND: 'backend',
  NOTICE: 'notice',
  WORKFLOW: 'workflow',
  LLM: 'llm',
  CLIENT: 'client'
} as const;

export const NOTICE_GROUP = {
  SYSTEM: 'system',
  NOTICE: 'notice',
  WORKFLOW: 'workflow'
} as const;

export interface PushMessagePayload {
  messageId?: string | number;
  title?: string;
  category?: string;
  type?: string;
  source?: string;
  message?: string;
  data?: Record<string, unknown> | null;
  path?: string;
  timestamp?: number;
}

const MESSAGE_CENTER_TYPES = new Set<string>([PUSH_MESSAGE_TYPE.MESSAGE, PUSH_MESSAGE_TYPE.NOTICE]);
const record = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);
const text = (value: unknown) => (typeof value === 'string' ? value : undefined);
const identifier = (value: unknown) =>
  typeof value === 'string' || (typeof value === 'number' && Number.isSafeInteger(value)) ? value : undefined;

export const parsePushMessage = (raw: string): PushMessagePayload => {
  try {
    const payload: unknown = JSON.parse(raw);
    if (!record(payload)) throw new Error('推送消息必须为对象');
    const data = record(payload.data) ? payload.data : null;
    return {
      type: text(payload.type) ?? PUSH_MESSAGE_TYPE.MESSAGE,
      source: text(payload.source) ?? PUSH_MESSAGE_SOURCE.BACKEND,
      // Notify 实时事件的 notificationId 与已持久化收件箱 messageId 相同。
      messageId: identifier(payload.messageId) ?? identifier(data?.notificationId),
      title: text(payload.title) ?? text(data?.title),
      category: text(payload.category) ?? text(data?.category),
      message: text(payload.message) ?? '',
      data,
      path: text(payload.path) ?? text(data?.path),
      timestamp:
        typeof payload.timestamp === 'number' && Number.isFinite(payload.timestamp) ? payload.timestamp : Date.now()
    };
  } catch {
    return {
      type: PUSH_MESSAGE_TYPE.MESSAGE,
      source: 'backend',
      messageId: undefined,
      message: raw,
      data: null,
      path: undefined,
      timestamp: Date.now()
    };
  }
};

export const shouldAppendNotice = (payload: PushMessagePayload) => {
  return MESSAGE_CENTER_TYPES.has(payload.type ?? PUSH_MESSAGE_TYPE.MESSAGE);
};

export const resolveNoticeGroup = (payload: PushMessagePayload) => {
  if (
    payload.category === NOTICE_GROUP.NOTICE ||
    payload.category === NOTICE_GROUP.WORKFLOW ||
    payload.category === NOTICE_GROUP.SYSTEM
  ) {
    return payload.category;
  }
  if (
    payload.type === PUSH_MESSAGE_TYPE.NOTICE ||
    payload.source === PUSH_MESSAGE_SOURCE.NOTICE ||
    payload.path?.startsWith('/notify/notice')
  ) {
    return NOTICE_GROUP.NOTICE;
  }
  if (payload.source === PUSH_MESSAGE_SOURCE.WORKFLOW || payload.path?.startsWith('/workflow')) {
    return NOTICE_GROUP.WORKFLOW;
  }
  return NOTICE_GROUP.SYSTEM;
};

export const resolveNoticeTitle = (payload: PushMessagePayload) => {
  const group = resolveNoticeGroup(payload);
  if (group === NOTICE_GROUP.NOTICE) {
    return '通知公告消息';
  }
  if (group === NOTICE_GROUP.WORKFLOW) {
    return '工作流消息';
  }
  return '系统消息';
};
