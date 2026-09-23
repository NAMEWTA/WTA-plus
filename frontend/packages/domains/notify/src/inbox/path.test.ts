import { describe, expect, it } from 'vitest';
import { inboxBusinessPath, inboxMessageId } from './path';

describe('本人收件箱链接', () => {
  it('只接受单个正 signed-64 十进制编号', () => {
    expect(inboxMessageId('9223372036854775807')).toBe('9223372036854775807');
    for (const invalid of [undefined, null, ['1'], '0', '-1', '01', '1/2', '9223372036854775808',
      Number.MAX_SAFE_INTEGER + 1]) {
      expect(inboxMessageId(invalid)).toBeUndefined();
    }
  });

  it('旧管理链接只能以当前本人消息编号转换，新深链不循环', () => {
    expect(inboxBusinessPath('/notify/notice?noticeId=88', '42'))
      .toBe('/notify/inbox?messageId=42');
    expect(inboxBusinessPath('/notify/notice?noticeId=88', '42', '42')).toBeUndefined();
    expect(inboxBusinessPath('/notify/notice?noticeId=88', '0')).toBeUndefined();
    expect(inboxBusinessPath('/notify/notice?noticeId=88&other=1', '42')).toBeUndefined();
    expect(inboxBusinessPath('/notify/inbox?messageId=42', '42')).toBe('/notify/inbox?messageId=42');
    expect(inboxBusinessPath('/notify/inbox?messageId=42', '42', '42')).toBeUndefined();
    expect(inboxBusinessPath('/notify/inbox?messageId=43', '42')).toBeUndefined();
    expect(inboxBusinessPath('/workflow/task?taskId=1', '42')).toBe('/workflow/task?taskId=1');
  });
});
