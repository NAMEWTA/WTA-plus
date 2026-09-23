import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { useNoticeStore } from './notice';

describe('消息盒子本人全局计数', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('保留服务端顶部顺序，并以全部消息未读数显示 badge', () => {
    const store = useNoticeStore();
    store.beginLoad('A');
    store.setNotices([
      { messageId: '2', message: '较新', read: true, time: '', timestamp: 1 },
      { messageId: '1', message: '较旧', read: false, time: '', timestamp: 1 }
    ], 481, 'A');
    expect(store.state.notices.map(item => item.messageId)).toEqual(['2', '1']);
    expect(store.unreadCount).toBe(481);
    store.beginLoad('B');
    expect(store.state.notices).toEqual([]);
    expect(store.unreadCount).toBe(0);
    store.setNotices([{ messageId: '3', message: 'B', read: false, time: '' }], 2, 'B');
    store.setNotices([{ messageId: 'old', message: 'A', read: false, time: '' }], 500, 'A');
    expect(store.state.notices.map(item => item.messageId)).toEqual(['3']);
    expect(store.unreadCount).toBe(2);
    store.clearNotice();
    expect(store.unreadCount).toBe(0);
  });
});
