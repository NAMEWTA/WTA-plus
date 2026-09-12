import { describe, expect, it } from 'vitest';
import { parsePushMessage, resolveNoticeGroup, shouldAppendNotice } from './push-message';

describe('Notify push payload', () => {
  it('maps the persisted notification identifier, title and path from realtime data', () => {
    const message = parsePushMessage(
      JSON.stringify({
        type: 'message',
        source: 'backend',
        message: '公告发布',
        data: { notificationId: '9223372036854775806', title: '维护公告', path: '/notify/notice/1' }
      })
    );
    expect(message.messageId).toBe('9223372036854775806');
    expect(message.title).toBe('维护公告');
    expect(resolveNoticeGroup(message)).toBe('notice');
  });

  it('uses the same workflow and explicit category rules as inbox items', () => {
    expect(resolveNoticeGroup(parsePushMessage('{"path":"/workflow/task/1"}'))).toBe('workflow');
    expect(resolveNoticeGroup(parsePushMessage('{"category":"system","path":"/workflow/task/1"}'))).toBe('system');
  });

  it('does not add LLM fragments and rejects unsafe numeric IDs', () => {
    expect(shouldAppendNotice(parsePushMessage('{"type":"llm","message":"partial"}'))).toBe(false);
    expect(parsePushMessage('{"messageId":9223372036854775806}').messageId).toBeUndefined();
    expect(parsePushMessage('{"data":true}').data).toBeNull();
  });
});
