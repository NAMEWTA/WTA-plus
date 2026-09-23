const MAX_SIGNED_LONG = '9223372036854775807';

/** 只接受一个正 signed-64 十进制消息 ID；不把数组或失精数字送到本人详情接口。 */
export function inboxMessageId(value: unknown): string | undefined {
  if (typeof value === 'number') {
    if (!Number.isSafeInteger(value) || value <= 0) return undefined;
    value = String(value);
  }
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) return undefined;
  if (value.length > MAX_SIGNED_LONG.length ||
      (value.length === MAX_SIGNED_LONG.length && value > MAX_SIGNED_LONG)) return undefined;
  return value;
}

/** 旧管理链接仅凭当前已授权消息的 ID 转为本人入口；新深链不再指向自身。 */
export function inboxBusinessPath(path: string | null | undefined, ownMessageId: unknown,
                                  currentQueryId?: string): string | undefined {
  if (!path) return undefined;
  const own = inboxMessageId(ownMessageId);
  const deep = /^\/notify\/inbox\?messageId=([1-9]\d*)$/.exec(path);
  if (path.startsWith('/notify/inbox?')) {
    return deep && own && inboxMessageId(deep[1]) === own && currentQueryId !== own
      ? `/notify/inbox?messageId=${own}` : undefined;
  }
  if (!path.startsWith('/notify/notice?')) return path;
  const legacy = /^\/notify\/notice\?noticeId=([1-9]\d*)$/.exec(path);
  return legacy && inboxMessageId(legacy[1]) && own && currentQueryId !== own
    ? `/notify/inbox?messageId=${own}` : undefined;
}
